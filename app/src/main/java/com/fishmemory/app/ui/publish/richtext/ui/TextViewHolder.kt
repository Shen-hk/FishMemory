package com.fishmemory.app.ui.publish.richtext.ui.holder

import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.*
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.fishmemory.app.ui.publish.richtext.model.*

class TextViewHolder(private val textView: TextView) :
    RecyclerView.ViewHolder(textView) {

    fun bind(block: RichBlock.Text) {
        val spannable = SpannableString(block.content)

        block.spans.forEach { span ->
            val androidSpan = when (span.type) {
                SpanType.BOLD -> StyleSpan(Typeface.BOLD)
                SpanType.ITALIC -> StyleSpan(Typeface.ITALIC)
                SpanType.UNDERLINE -> UnderlineSpan()
                SpanType.STRIKE -> StrikethroughSpan()
                SpanType.CODE -> TypefaceSpan("monospace")
            }

            spannable.setSpan(
                androidSpan,
                span.start,
                span.end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        textView.text = spannable
    }

    companion object {
        fun create(parent: ViewGroup): TextViewHolder {
            val tv = TextView(parent.context).apply {
                textSize = 16f
                setPadding(32, 24, 32, 24)
            }
            return TextViewHolder(tv)
        }
    }
}
