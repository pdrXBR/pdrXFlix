package com.pdrxflix.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.pdrxflix.R
import com.pdrxflix.data.model.PlaybackRecord
import com.pdrxflix.databinding.ItemContinueWatchBinding

class ContinueAdapter(
    private val onClick: (PlaybackRecord) -> Unit
) : ListAdapter<PlaybackRecord, ContinueAdapter.VH>(Diff) {

    object Diff : DiffUtil.ItemCallback<PlaybackRecord>() {
        override fun areItemsTheSame(oldItem: PlaybackRecord, newItem: PlaybackRecord) = oldItem.videoPath == newItem.videoPath
        override fun areContentsTheSame(oldItem: PlaybackRecord, newItem: PlaybackRecord) = oldItem == newItem
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemContinueWatchBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    inner class VH(private val binding: ItemContinueWatchBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: PlaybackRecord) {
            binding.title.text = item.collectionTitle
            binding.subtitle.text = item.videoTitle
            binding.progressBar.progress = (item.progress * 100).toInt().coerceIn(0, 100)
            Glide.with(binding.cover)
                .load(item.coverPath ?: R.drawable.ic_placeholder_cover)
                .placeholder(R.drawable.ic_placeholder_cover)
                .centerCrop()
                .into(binding.cover)

            binding.root.setOnClickListener { onClick(item) }
        }
    }
}
