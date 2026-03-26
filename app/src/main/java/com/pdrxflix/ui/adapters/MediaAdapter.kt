package com.pdrxflix.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.pdrxflix.R
import com.pdrxflix.data.model.MediaCollection
import com.pdrxflix.databinding.ItemMediaCardBinding

class MediaAdapter(
    private val onClick: (MediaCollection) -> Unit
) : ListAdapter<MediaCollection, MediaAdapter.VH>(Diff) {

    object Diff : DiffUtil.ItemCallback<MediaCollection>() {
        override fun areItemsTheSame(oldItem: MediaCollection, newItem: MediaCollection) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: MediaCollection, newItem: MediaCollection) = oldItem == newItem
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemMediaCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    inner class VH(private val binding: ItemMediaCardBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: MediaCollection) {
            binding.title.text = item.title
            binding.subtitle.text = binding.root.context.getString(R.string.item_count_format, item.itemCount)
            Glide.with(binding.cover)
                .load(item.coverPath ?: R.drawable.ic_placeholder_cover)
                .placeholder(R.drawable.ic_placeholder_cover)
                .centerCrop()
                .into(binding.cover)

            binding.root.setOnClickListener { onClick(item) }
        }
    }
}
