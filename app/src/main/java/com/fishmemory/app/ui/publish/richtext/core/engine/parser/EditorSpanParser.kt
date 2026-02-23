package com.fishmemory.app.ui.publish.richtext.core.engine.parser

import android.graphics.Typeface
import android.text.Editable
import android.text.Spannable
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.text.style.UnderlineSpan
import android.widget.EditText
import com.fishmemory.app.ui.publish.richtext.business.link.LinkSpan
import com.fishmemory.app.ui.publish.richtext.core.model.SpanType
import com.fishmemory.app.ui.publish.richtext.core.model.SpanData

/**
 * 富文本样式解析器：从编辑器提取样式信息并转换为可持久化的 [SpanData] 列表。
 *
 * 与 [EditorSpanApplier] 互为逆操作：
 * - `parse()`：提取样式（保存时用）
 * - `applySpans()`：应用样式（加载时用）
 */
object EditorSpanParser {

    /**
     * 从 EditText 提取所有样式
     */
    fun parse(editText: EditText): List<SpanData> = parse(editText.text)

    /**
     * 从 Editable 提取所有样式
     *
     * 处理流程：
     * 1. 遍历文本中所有 Span
     * 2. 映射到 SpanType 枚举
     * 3. 提取位置、类型、URL（链接）
     *
     * 支持的样式：
     * - StyleSpan → BOLD（仅粗体）
     * - UnderlineSpan → UNDERLINE
     * - TypefaceSpan → CODE（仅 monospace）
     * - LinkSpan → LINK
     */
    fun parse(editable: Editable): List<SpanData> {
        if (editable !is Spannable) return emptyList()

        val result = mutableListOf<SpanData>()
        val text = editable

        // 扫描所有 Span
        (text as Spannable).getSpans(0, text.length, Any::class.java).forEach { span ->
            val start = text.getSpanStart(span)
            val end = text.getSpanEnd(span)

            // 类型映射
            val type = when (span) {
                is StyleSpan -> if (span.style == Typeface.BOLD) SpanType.BOLD else null
                is UnderlineSpan -> SpanType.UNDERLINE
                is TypefaceSpan -> if (span.family == "monospace") SpanType.CODE else null
                is LinkSpan -> SpanType.LINK
                else -> null
            }

            // 收集有效样式
            type?.let {
                val url = (span as? LinkSpan)?.url
                result.add(SpanData(start, end, it, url = url))
            }
        }

        return result
    }
}