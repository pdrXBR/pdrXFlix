package com.pdrxflix.ui.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.pdrxflix.data.model.MediaCollection
import com.pdrxflix.data.model.PlaybackRecord
import com.pdrxflix.data.model.VideoItem
import com.pdrxflix.databinding.ItemEpisodeBinding

class EpisodeAdapter(
    private val historyProvider: (String) -> PlaybackRecord?,
    private val onClick: (VideoItem) -> Unit
) : ListAdapter<EpisodeAdapter.DisplayItem, EpisodeAdapter.VH>(Diff) {

    private var currentCollection: MediaCollection? = null
    private var isShowingEpisodes = false

    sealed class DisplayItem {
        data class Season(val name: String) : DisplayItem()
        data class Episode(val video: VideoItem) : DisplayItem()
    }

    object Diff : DiffUtil.ItemCallback<DisplayItem>() {
        override fun areItemsTheSame(o: DisplayItem, n: DisplayItem) = o == n
        override fun areContentsTheSame(o: DisplayItem, n: DisplayItem) = o == n
    }

    fun updateData(collection: MediaCollection) {
        currentCollection = collection
        isShowingEpisodes = false
        submitList(collection.getSeasonNames().map { DisplayItem.Season(it) })
    }

    override fun onCreateViewHolder(p: ViewGroup, t: Int): VH {
        val b = ItemEpisodeBinding.inflate(LayoutInflater.from(p.context), p, false)
        return VH(b)
    }

    override fun onBindViewHolder(h: VH, p: Int) = h.bind(getItem(p))

    inner class VH(private val binding: ItemEpisodeBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: DisplayItem) {
            when (item) {
                is DisplayItem.Season -> {
                    // Temporadas ficam no padrão escuro
                    binding.episodeIndex.text = "S"
                    binding.title.text = item.name
                    binding.subtitle.text = "Temporada"
                    
                    updateColors(backgroundColor = "#1A1A1A", textColor = Color.WHITE)
                    binding.root.setOnClickListener { showEpisodes(item.name) }
                }
                is DisplayItem.Episode -> {
                    val v = item.video
                    val record = historyProvider(v.filePath)
                    
                    binding.title.text = v.displayName
                    binding.subtitle.text = v.fileName
                    binding.episodeIndex.text = (v.episodeIndex + 1).toString()

                    when {
                        record == null -> {
                            // Não assistido: Fundo escuro, texto branco
                            updateColors(backgroundColor = "#1A1A1A", textColor = Color.WHITE)
                        }
                        record.isFinished -> {
                            // Concluído: Amarelo Marin, TEXTO PRETO
                            updateColors(backgroundColor = "#FFD700", textColor = Color.BLACK)
                        }
                        else -> {
                            // Em curso: Rosa Marin, TEXTO PRETO
                            updateColors(backgroundColor = "#FF69B4", textColor = Color.BLACK)
                        }
                    }
                    binding.root.setOnClickListener { onClick(v) }
                }
            }
        }

        // Função auxiliar para não repetir código de cores
        private fun updateColors(backgroundColor: String, textColor: Int) {
            binding.root.setCardBackgroundColor(Color.parseColor(backgroundColor))
            binding.title.setTextColor(textColor)
            binding.subtitle.setTextColor(textColor)
            binding.episodeIndex.setTextColor(textColor)
        }
    }

    private fun showEpisodes(name: String) {
        val videos = currentCollection?.getVideosBySeason(name) ?: return
        isShowingEpisodes = true
        submitList(videos.map { DisplayItem.Episode(it) })
    }

    fun handleBackPress(): Boolean {
        if (isShowingEpisodes && currentCollection != null) {
            updateData(currentCollection!!)
            return true
        }
        return false
    }
}
