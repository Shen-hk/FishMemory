package com.fishmemory.app.ui.publish.localarticlelist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.fishmemory.app.R
import com.fishmemory.app.data.local.rooms.entity.LocalArticleEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LocalArticleListAdapter(
    private val onClick: (LocalArticleEntity) -> Unit,
    private val onDelete: (LocalArticleEntity) -> Unit
) : ListAdapter<LocalArticleEntity, LocalArticleListAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_local_article, parent, false)
        return VH(v, onClick, onDelete)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    class VH(
        itemView: View,
        private val onClick: (LocalArticleEntity) -> Unit,
        private val onDelete: (LocalArticleEntity) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        private val tvSummary: TextView = itemView.findViewById(R.id.tvSummary)
        private val tvMeta: TextView = itemView.findViewById(R.id.tvMeta)
        private val ivCover: ImageView = itemView.findViewById(R.id.ivCover)
        private val btnDelete: ImageView = itemView.findViewById(R.id.btnDelete)

        private val df = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

        fun bind(entity: LocalArticleEntity) {
            tvTitle.text = entity.title.ifBlank { "无标题" }
            tvSummary.text = entity.summary?.take(50)?.ifBlank { "（无摘要）" } ?: "（无摘要）"
            tvMeta.text = "${df.format(Date(entity.publishTimeMs))}  ·  阅读 ${entity.readCount}"
            
            // 加载封面图
            if (!entity.coverUrl.isNullOrBlank()) {
                try {
                    Glide.with(ivCover)
                        .load(entity.coverUrl)
                        .placeholder(R.drawable.ic_image_placeholder)
                        .error(R.drawable.ic_image_error)
                        .centerCrop()
                        .into(ivCover)
                } catch (e: Exception) {
                    ivCover.setImageResource(R.drawable.ic_image_placeholder)
                }
            } else {
                ivCover.setImageResource(R.drawable.ic_image_placeholder)
            }
            
            itemView.setOnClickListener { onClick(entity) }
            btnDelete.setOnClickListener { onDelete(entity) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<LocalArticleEntity>() {
            override fun areItemsTheSame(oldItem: LocalArticleEntity, newItem: LocalArticleEntity): Boolean =
                oldItem.localId == newItem.localId

            override fun areContentsTheSame(oldItem: LocalArticleEntity, newItem: LocalArticleEntity): Boolean =
                oldItem == newItem
        }
    }
}
