package com.pdrxflix.ui.adapters

import android.view.LayoutInflater
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

    sealed class DisplayItem {
        data class Season(val name: String) : DisplayItem()
        data class Episode(val video: VideoItem) : DisplayItem()
    }

    object Diff : DiffUtil.ItemCallback<DisplayItem>() {
        override fun areItemsTheSame(old: DisplayItem, new: DisplayItem) = old == new
        override fun areContentsTheSame(old: DisplayItem, new: DisplayItem) = old == new
    }

    fun updateData(collection: MediaCollection) {
        this.currentCollection = collection
        this.isShowingEpisodes = false
        
        // Buscando os nomes das temporadas da função que criamos no Models.kt
        val names = collection.getSeasonNames()
        val seasonItems = names.map { DisplayItem.Season(it) }
        submitList(seasonItems)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemEpisodeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    inner class VH(private val binding: ItemEpisodeBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: DisplayItem) {
            when (item) {
                is DisplayItem.Season -> {
                    binding.episodeIndex.text = "S"
                    binding.title.text = item.name
                    binding.subtitle.text = "Toque para abrir"
                    binding.root.setOnClickListener { showEpisodesOfSeason(item.name) }
                }
                is DisplayItem.Episode -> {
                    val v = item.video
                    binding.episodeIndex.text = (v.episodeIndex + 1).toString()
                    binding.title.text = v.displayName
                    binding.subtitle.text = v.fileName
                    binding.root.setOnClickListener { onClick(v) }
                }
            }
        }
    }

    private fun showEpisodesOfSeason(seasonName: String) {
        val collection = currentCollection ?: return
        val filteredVideos = collection.getVideosBySeason(seasonName)
        isShowingEpisodes = true
        submitList(filteredVideos.map { DisplayItem.Episode(it) })
    }

    fun handleBackPress(): Boolean {
        val collection = currentCollection
        return if (isShowingEpisodes && collection != null) {
            updateData(collection)
            true
        } else {
            false
        }
    }
}
