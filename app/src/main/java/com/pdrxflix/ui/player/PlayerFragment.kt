package com.pdrxflix.ui.player

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
        binding.btnBack.setOnClickListener { closeAndSave() }
        binding.btnNext.setOnClickListener { playNextEpisodeIfAvailable() }
        binding.btnResume.setOnClickListener {
            player?.seekTo(startPositionMs)
            player?.play()
        }

        binding.volumeSeek.max = 100
        binding.volumeSeek.progress = 100
        binding.volumeSeek.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                player?.volume = progress / 100f
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) = Unit
        })

        enterFullscreen()
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
        binding.title.text = file.nameWithoutExtension

        exoPlayer.setMediaItem(androidx.media3.common.MediaItem.fromUri(Uri.fromFile(file)))
        exoPlayer.prepare()
        if (startPositionMs > 0L) exoPlayer.seekTo(startPositionMs)
        exoPlayer.playWhenReady = true

        exoPlayer.addListener(object : androidx.media3.common.Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == androidx.media3.common.Player.STATE_ENDED) {
                    saveCurrentProgress(forceComplete = true)
                    if (repository.getAutoPlay()) {
                        playNextEpisodeIfAvailable()
                    } else {
                        closeAndSave()
                    }
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
        repository.saveProgress(
            PlaybackRecord(
                collectionId = collectionId,
                collectionTitle = repository.getCollection(collectionId)?.title.orEmpty(),
                videoPath = videoPath,
                videoTitle = File(videoPath).nameWithoutExtension,
                coverPath = repository.getCollection(collectionId)?.coverPath,
                episodeIndex = episodeIndex,
                lastPositionMs = position,
                durationMs = duration,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    private fun playNextEpisodeIfAvailable() {
        val next = repository.resolveNextVideo(collectionId, episodeIndex) ?: run {
            closeAndSave()
            return
        }
        closeAndSave()
        (activity as? AppNavigator)?.openPlayer(collectionId, next.filePath, 0L, next.episodeIndex)
    }

    private fun closeAndSave() {
        saveCurrentProgress()
        releasePlayer()
        (activity as? AppNavigator)?.closeTransientScreen()
    }

    private fun releasePlayer() {
        saveJob?.cancel()
        saveJob = null
        binding.playerView.player = null
        player?.release()
        player = null
        exitFullscreen()
    }

    private fun enterFullscreen() {
        val window = requireActivity().window
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).hide(WindowInsetsCompat.Type.systemBars())
    }

    private fun exitFullscreen() {
        val window = requireActivity().window
        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowInsetsControllerCompat(window, window.decorView).show(WindowInsetsCompat.Type.systemBars())
    }

    override fun onPause() {
        super.onPause()
        saveCurrentProgress()
    }

    override fun onStop() {
        super.onStop()
        saveCurrentProgress()
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
