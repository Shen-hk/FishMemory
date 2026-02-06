package com.fishmemory.app.ui.publish.richtext.ui

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.fishmemory.app.ui.publish.richtext.model.RichBlock
import com.fishmemory.app.ui.publish.richtext.ui.*
import com.fishmemory.app.ui.publish.richtext.ui.holder.CodeViewHolder
import com.fishmemory.app.ui.publish.richtext.ui.holder.ImageViewHolder
import com.fishmemory.app.ui.publish.richtext.ui.holder.TextViewHolder

class RichContentAdapter(
    private val items: List<RichBlock>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_TEXT = 1
        private const val TYPE_IMAGE = 2
        private const val TYPE_CODE = 3
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is RichBlock.Text -> TYPE_TEXT
            is RichBlock.Image -> TYPE_IMAGE
            is RichBlock.Code -> TYPE_CODE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_TEXT -> TextViewHolder.create(parent)
            TYPE_IMAGE -> ImageViewHolder.create(parent)
            TYPE_CODE -> CodeViewHolder.create(parent)
            else -> error("Unknown type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is RichBlock.Text -> (holder as TextViewHolder).bind(item)
            is RichBlock.Image -> (holder as ImageViewHolder).bind(item)
            is RichBlock.Code -> (holder as CodeViewHolder).bind(item)
        }
    }

    override fun getItemCount(): Int = items.size
}
