package com.pdrxflix.ui.player

import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.pdrxflix.R
import com.pdrxflix.PdrXFlixApp
import com.pdrxflix.data.model.PlaybackRecord
import com.pdrxflix.databinding.FragmentPlayerBinding
import com.pdrxflix.ui.AppNavigator
import com.pdrxflix.ui.util.PlayerFactory
import com.pdrxflix.ui.util.formatTime
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

class PlayerFragment : Fragment() {

    private var _binding: FragmentPlayerBinding? = null
    private val binding get() = _binding!!

    private val repository by lazy { (requireActivity().application as PdrXFlixApp).repository }

    private var player: androidx.media3.exoplayer.ExoPlayer? = null
    private var saveJob: Job? = null

    private val collectionId: Long by lazy { requireArguments().getLong(ARG_COLLECTION_ID) }
    private val videoPath: String by lazy { requireArguments().getString(ARG_VIDEO_PATH).orEmpty() }
    private val startPositionMs: Long by lazy { requireArguments().getLong(ARG_START_POSITION_MS) }
    private val episodeIndex: Int by lazy { requireArguments().getInt(ARG_EPISODE_INDEX) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlayerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Força a orientação horizontal assim que a view é criada
        enterFullscreen()
        
        binding.btnBack.setOnClickListener { closeAndSave() }
        binding.btnNext.setOnClickListener { playNextEpisodeIfAvailable() }
        
        // Volume
        binding.volumeSeek.max = 100
        binding.volumeSeek.progress = 100
        binding.volumeSeek.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                player?.volume = progress / 100f
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) = Unit
        })

        setupPlayer()
    }

    private fun setupPlayer() {
        val file = File(videoPath)
        if (!file.exists()) {
            binding.errorText.visibility = View.VISIBLE
            binding.errorText.text = getString(R.string.file_not_found)
            return
        }

        val exoPlayer = PlayerFactory.build(requireContext())
        player = exoPlayer

        binding.playerView.player = exoPlayer
        
        // Ajusta o vídeo para preencher a tela (zoom) se necessário
        binding.playerView.resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL
        
        binding.title.text = file.nameWithoutExtension

        exoPlayer.setMediaItem(androidx.media3.common.MediaItem.fromUri(Uri.fromFile(file)))
        exoPlayer.prepare()
        if (startPositionMs > 0L) exoPlayer.seekTo(startPositionMs)
        exoPlayer.playWhenReady = true

        exoPlayer.addListener(object : androidx.media3.common.Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == androidx.media3.common.Player.STATE_ENDED) {
                    saveCurrentProgress(forceComplete = true)
                    // Verifica autoplay no repository
                    playNextEpisodeIfAvailable()
                }
            }
        })

        binding.playerView.useController = true
        binding.playerView.controllerShowTimeoutMs = 3000

        saveJob = viewLifecycleOwner.lifecycleScope.launch {
            while (isActive) {
                delay(3000)
                saveCurrentProgress()
                updateTimeLabel()
            }
        }
    }

    private fun updateTimeLabel() {
        val current = player?.currentPosition ?: 0L
        val duration = player?.duration?.takeIf { it > 0L } ?: 0L
        binding.timeLabel.text = "${formatTime(current)} / ${formatTime(duration)}"
    }

    private fun saveCurrentProgress(forceComplete: Boolean = false) {
        val exo = player ?: return
        val duration = exo.duration.takeIf { it > 0L } ?: 0L
        val position = if (forceComplete) duration else exo.currentPosition
        
        val collection = repository.findCollectionById(collectionId)
        
        repository.saveProgress(
            PlaybackRecord(
                collectionId = collectionId,
                collectionTitle = collection?.title.orEmpty(),
                videoPath = videoPath,
                videoTitle = File(videoPath).nameWithoutExtension,
                coverPath = collection?.coverPath,
                episodeIndex = episodeIndex,
                lastPositionMs = position,
                durationMs = duration,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    private fun playNextEpisodeIfAvailable() {
        // Supondo que seu repository tenha essa lógica de buscar o próximo VideoItem
        val next = repository.collections.value
            .find { it.id == collectionId }
            ?.videos?.getOrNull(episodeIndex + 1)

        if (next != null) {
            saveCurrentProgress()
            releasePlayer()
            (activity as? AppNavigator)?.openPlayer(collectionId, next.filePath, 0L, next.episodeIndex)
        } else {
            closeAndSave()
        }
    }

    private fun closeAndSave() {
        saveCurrentProgress()
        exitFullscreen() // Volta a tela para "em pé" antes de sair
        releasePlayer()
        (activity as? AppNavigator)?.closeTransientScreen()
    }

    private fun releasePlayer() {
        saveJob?.cancel()
        saveJob = null
        binding.playerView.player = null
        player?.release()
        player = null
    }

    private fun enterFullscreen() {
        val window = requireActivity().window
        // 1. Esconde as barras
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        // 2. Gira a tela para Horizontal (Sensor Landscape permite girar pros dois lados deitados)
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    }

    private fun exitFullscreen() {
        val window = requireActivity().window
        // 1. Mostra as barras
        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowInsetsControllerCompat(window, window.decorView).show(WindowInsetsCompat.Type.systemBars())
        // 2. Volta a tela para Em Pé (Portrait)
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

    override fun onPause() {
        super.onPause()
        player?.pause()
    }

    override fun onStop() {
        super.onStop()
        // Garantir que saia do fullscreen se o app for minimizado
        exitFullscreen()
        releasePlayer()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "PlayerFragment"
        private const val ARG_COLLECTION_ID = "collection_id"
        private const val ARG_VIDEO_PATH = "video_path"
        private const val ARG_START_POSITION_MS = "start_position_ms"
        private const val ARG_EPISODE_INDEX = "episode_index"

        fun newInstance(
            collectionId: Long,
            videoPath: String,
            startPositionMs: Long,
            episodeIndex: Int
        ): PlayerFragment {
            return PlayerFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_COLLECTION_ID, collectionId)
                    putString(ARG_VIDEO_PATH, videoPath)
                    putLong(ARG_START_POSITION_MS, startPositionMs)
                    putInt(ARG_EPISODE_INDEX, episodeIndex)
                }
            }
        }
    }
}
