package com.fishmemory.app.ui.publish.draftlist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.fishmemory.app.R
import com.fishmemory.app.data.local.rooms.entity.DraftEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DraftListAdapter(
    private val onClick: (DraftEntity) -> Unit,
    private val onDelete: (DraftEntity) -> Unit
) : ListAdapter<DraftEntity, DraftListAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_draft, parent, false)
        return VH(v, onClick, onDelete)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    class VH(
        itemView: View,
        private val onClick: (DraftEntity) -> Unit,
        private val onDelete: (DraftEntity) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        private val tvPreview: TextView = itemView.findViewById(R.id.tvPreview)
        private val tvMeta: TextView = itemView.findViewById(R.id.tvMeta)
        private val btnDelete: ImageView = itemView.findViewById(R.id.btnDelete)

        private val df = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())

        fun bind(entity: DraftEntity) {
            tvTitle.text = entity.title.ifBlank { "无标题" }
            tvPreview.text = entity.preview.ifBlank { "（无预览）" }
            tvMeta.text = "${df.format(Date(entity.updatedAt))}  ·  ${entity.wordCount}字"
            itemView.setOnClickListener { onClick(entity) }
            btnDelete.setOnClickListener { onDelete(entity) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<DraftEntity>() {
            override fun areItemsTheSame(oldItem: DraftEntity, newItem: DraftEntity): Boolean =
                oldItem.draftId == newItem.draftId

            override fun areContentsTheSame(oldItem: DraftEntity, newItem: DraftEntity): Boolean =
                oldItem == newItem
        }
    }
}

