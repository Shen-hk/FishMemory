package com.fishmemory.app.ui.publish.richtext.ui.holder

import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.fishmemory.app.ui.publish.richtext.model.RichBlock

class ImageViewHolder(private val imageView: ImageView) :
    RecyclerView.ViewHolder(imageView) {

    fun bind(block: RichBlock.Image) {
        Glide.with(imageView)
            .load(block.url)
            .into(imageView)
    }

    companion object {
        fun create(parent: ViewGroup): ImageViewHolder {
            val iv = ImageView(parent.context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                adjustViewBounds = true
                scaleType = ImageView.ScaleType.CENTER_CROP
            }
            return ImageViewHolder(iv)
        }
    }
}
