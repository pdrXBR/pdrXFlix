package com.pdrxflix.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.pdrxflix.data.model.VideoItem
import com.pdrxflix.databinding.ItemEpisodeBinding

class EpisodeAdapter(
    private val onClick: (VideoItem) -> Unit
) : ListAdapter<VideoItem, EpisodeAdapter.VH>(Diff) {

    object Diff : DiffUtil.ItemCallback<VideoItem>() {
        override fun areItemsTheSame(oldItem: VideoItem, newItem: VideoItem) = oldItem.filePath == newItem.filePath
        override fun areContentsTheSame(oldItem: VideoItem, newItem: VideoItem) = oldItem == newItem
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemEpisodeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    inner class VH(private val binding: ItemEpisodeBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: VideoItem) {
            binding.title.text = item.displayName
            binding.subtitle.text = item.fileName
            binding.episodeIndex.text = (item.episodeIndex + 1).toString()
            binding.root.setOnClickListener { onClick(item) }
        }
    }
}
