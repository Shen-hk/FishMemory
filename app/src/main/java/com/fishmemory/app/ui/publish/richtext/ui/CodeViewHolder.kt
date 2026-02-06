package com.fishmemory.app.ui.publish.richtext.ui.holder

import android.graphics.Typeface
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.fishmemory.app.ui.publish.richtext.model.RichBlock

class CodeViewHolder(private val textView: TextView) :
    RecyclerView.ViewHolder(textView) {

    fun bind(block: RichBlock.Code) {
        textView.text = block.code
    }

    companion object {
        fun create(parent: ViewGroup): CodeViewHolder {
            val tv = TextView(parent.context).apply {
                textSize = 14f
                typeface = Typeface.MONOSPACE
                setPadding(32, 24, 32, 24)
            }
            return CodeViewHolder(tv)
        }
    }
}
