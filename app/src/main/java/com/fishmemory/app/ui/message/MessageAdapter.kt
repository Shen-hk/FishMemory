package com.fishmemory.app.ui.message

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.fishmemory.app.databinding.ItemMessageBinding
import com.fishmemory.app.data.model.MessageItem

class MessageAdapter(
    private val onItemClick: (MessageItem) -> Unit
) : ListAdapter<MessageItem, MessageAdapter.MessageViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val binding = ItemMessageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MessageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MessageViewHolder(
        private val binding: ItemMessageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MessageItem) {
            binding.tvName.text = item.title
            binding.tvLastMessage.text = item.lastMessage
            binding.tvTime.text = item.time

            binding.root.setOnClickListener {
                onItemClick(item)
            }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<MessageItem>() {
            override fun areItemsTheSame(oldItem: MessageItem, newItem: MessageItem): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: MessageItem, newItem: MessageItem): Boolean {
                return oldItem == newItem
            }
        }
    }
}
