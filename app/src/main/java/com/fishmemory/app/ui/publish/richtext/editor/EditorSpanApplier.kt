package com.fishmemory.app.ui.publish.richtext.editor

import android.graphics.Typeface
import android.text.Editable
import android.text.Spanned
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.widget.EditText
import com.fishmemory.app.ui.publish.richtext.model.SpanType

object EditorSpanApplier {

    fun toggleBold(editText: EditText) {
        toggleStyle(editText, SpanType.BOLD)
    }

    fun toggleUnderline(editText: EditText) {
        toggleStyle(editText, SpanType.UNDERLINE)
    }

    private fun toggleStyle(editText: EditText, type: SpanType) {
        val start = editText.selectionStart
        val end = editText.selectionEnd
        if (start >= end) return

        val editable = editText.text

        when (type) {
            SpanType.BOLD -> applyOrRemoveSpan(
                editable, start, end, StyleSpan(Typeface.BOLD)
            )
            SpanType.UNDERLINE -> applyOrRemoveSpan(
                editable, start, end, UnderlineSpan()
            )
            else -> {}
        }
    }

    private fun applyOrRemoveSpan(
        editable: Editable,
        start: Int,
        end: Int,
        spanPrototype: Any
    ) {
        // 获取所有与 [start, end) 区间有交集的同类 Span
        val spans = editable.getSpans(start, end, spanPrototype::class.java)

        if (spans.isEmpty()) {
            // 没有重叠 Span → 直接添加新格式
            editable.setSpan(
                cloneSpan(spanPrototype),
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            return
        }
        //span粒度控制
        // 遍历每个重叠的 Span，进行智能拆分
        for (span in spans) {
            val spanStart = editable.getSpanStart(span)
            val spanEnd = editable.getSpanEnd(span)
            val flags = editable.getSpanFlags(span)

            // 先移除原始 Span
            editable.removeSpan(span)

            // 如果当前操作区间完全覆盖了原 Span → 不重建（即整体取消）
            if (start <= spanStart && end >= spanEnd) {
                continue // 删除，不恢复
            }

            // 保留左侧未选中部分（如果有）
            if (spanStart < start) {
                editable.setSpan(cloneSpan(spanPrototype), spanStart, start, flags)
            }

            // 保留右侧未选中部分（如果有）
            if (spanEnd > end) {
                editable.setSpan(cloneSpan(spanPrototype), end, spanEnd, flags)
            }
        }

        // 每次样式编辑后尝试做一次 Span 归一化，合并相邻且连续的同类 Span，防止长文中 Span 片段无限碎片化
        normalizeSpans(editable, spanPrototype)
    }

    /**
     * 对 Editable 中的同类 Span 做一次简单的“合并相邻”操作，减少碎片数量。
     * 这里不追求绝对最少，只需控制在“线性增长”而不是“编辑次数级”即可。
     */
    private fun normalizeSpans(editable: Editable, sampleSpan: Any) {
        val spanClass = sampleSpan::class.java
        val spans = editable.getSpans(0, editable.length, spanClass)
        if (spans.size <= 1) return

        // 按起始位置排序，依次尝试合并相邻区间
        val sorted = spans.sortedBy { editable.getSpanStart(it) }
        var i = 0
        while (i < sorted.size - 1) {
            val current = sorted[i]
            val next = sorted[i + 1]

            val curStart = editable.getSpanStart(current)
            val curEnd = editable.getSpanEnd(current)
            val nextStart = editable.getSpanStart(next)
            val nextEnd = editable.getSpanEnd(next)
            val curFlags = editable.getSpanFlags(current)

            // 仅在“前一个刚好结束在后一个开始处”时合并，避免跨越未加粗区域
            if (curEnd == nextStart) {
                editable.removeSpan(current)
                editable.removeSpan(next)
                // 使用现有 Span 作为模板，重新构建一个覆盖更大区间的 Span
                val merged = cloneSpan(current)
                editable.setSpan(merged, curStart, nextEnd, curFlags)
                // 合并后不自增 i，继续检查当前位置是否还能与后面合并
                continue
            }

            i++
        }
    }

    // 在 object EditorSpanApplier 内部添加：
    private fun cloneSpan(span: Any): Any {
        return when (span) {
            is StyleSpan -> StyleSpan(span.style)
            is UnderlineSpan -> UnderlineSpan()
            // 未来可扩展：ColorSpan, TypefaceSpan 等
            else -> throw IllegalArgumentException("Unsupported span type: ${span::class.java.simpleName}")
        }
    }
}
