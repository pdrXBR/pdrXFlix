package com.pdrxflix.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.pdrxflix.data.model.MediaCollection
import com.pdrxflix.data.model.VideoItem
import com.pdrxflix.databinding.ItemEpisodeBinding

class EpisodeAdapter(
    private val onClick: (VideoItem) -> Unit
) : ListAdapter<EpisodeAdapter.DisplayItem, EpisodeAdapter.VH>(Diff) {

    private var currentCollection: MediaCollection? = null
    private var isShowingEpisodes = false

    // Classe auxiliar para o Adapter saber se desenha uma Temporada ou um Episódio
    sealed class DisplayItem {
        data class Season(val name: String) : DisplayItem()
        data class Episode(val video: VideoItem) : DisplayItem()
    }

    object Diff : DiffUtil.ItemCallback<DisplayItem>() {
        override fun areItemsTheSame(old: DisplayItem, new: DisplayItem): Boolean {
            return if (old is DisplayItem.Episode && new is DisplayItem.Episode) {
                old.video.filePath == new.video.filePath
            } else if (old is DisplayItem.Season && new is DisplayItem.Season) {
                old.name == new.name
            } else false
        }
        override fun areContentsTheSame(old: DisplayItem, new: DisplayItem) = old == new
    }

    // Esta é a função que o Fragment vai chamar agora
    fun updateData(collection: MediaCollection) {
        currentCollection = collection
        isShowingEpisodes = false
        val seasons = collection.getSeasonNames().map { DisplayItem.Season(it) }
        submitList(seasons)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemEpisodeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    inner class VH(private val binding: ItemEpisodeBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: DisplayItem) {
            when (item) {
                is DisplayItem.Season -> {
                    // Configura o visual para parecer uma Temporada
                    binding.episodeIndex.text = "S"
                    binding.title.text = item.name
                    binding.subtitle.text = "Clique para ver os episódios"
                    
                    binding.root.setOnClickListener {
                        showEpisodesOfSeason(item.name)
                    }
                }
                is DisplayItem.Episode -> {
                    // Configura o visual normal de episódio
                    val video = item.video
                    binding.episodeIndex.text = (video.episodeIndex + 1).toString()
                    binding.title.text = video.displayName
                    binding.subtitle.text = video.fileName
                    
                    binding.root.setOnClickListener { onClick(video) }
                }
            }
        }
    }

    private fun showEpisodesOfSeason(seasonName: String) {
        val videos = currentCollection?.getVideosBySeason(seasonName) ?: return
        isShowingEpisodes = true
        submitList(videos.map { DisplayItem.Episode(it) })
    }

    // Se o usuário apertar "Voltar" no celular, poderíamos usar isso para voltar às temporadas
    fun handleBackPress(): Boolean {
        return if (isShowingEpisodes && currentCollection != null) {
            updateData(currentCollection!!)
            true
        } else {
            false
        }
    }
}
