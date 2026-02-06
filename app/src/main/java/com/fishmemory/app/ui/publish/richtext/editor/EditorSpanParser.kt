package com.fishmemory.app.ui.publish.richtext.editor

import android.graphics.Typeface
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.text.style.UnderlineSpan
import android.widget.EditText
import com.fishmemory.app.ui.publish.richtext.model.SpanType
import com.fishmemory.app.ui.publish.richtext.model.TextSpan

object EditorSpanParser {

    fun parse(editText: EditText): List<TextSpan> {
        val result = mutableListOf<TextSpan>()
        val text = editText.text

        text.getSpans(0, text.length, Any::class.java).forEach { span ->
            val start = text.getSpanStart(span)
            val end = text.getSpanEnd(span)

            val type = when (span) {
                is StyleSpan -> {
                    if (span.style == Typeface.BOLD) SpanType.BOLD else null
                }
                is UnderlineSpan -> SpanType.UNDERLINE
                is StrikethroughSpan -> SpanType.STRIKE
                is TypefaceSpan -> {
                    if (span.family == "monospace") SpanType.CODE else null
                }
                else -> null
            }

            type?.let {
                result.add(TextSpan(start, end, it))
            }
        }

        return result
    }
}
