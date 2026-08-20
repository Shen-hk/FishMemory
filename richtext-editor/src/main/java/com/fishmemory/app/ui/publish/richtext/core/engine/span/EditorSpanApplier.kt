package com.fishmemory.app.ui.publish.richtext.core.engine.span

import android.graphics.Typeface
import android.text.Editable
import android.text.Spanned
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.widget.EditText
import com.fishmemory.app.ui.publish.richtext.business.link.LinkSpan
import com.fishmemory.app.ui.publish.richtext.core.model.SpanType
import com.fishmemory.app.ui.publish.richtext.core.model.SpanData

/**
 * 富文本样式管理器：负责在编辑器中应用、切换和管理文本样式（如粗体、下划线等）。
 *
 * ## 核心功能
 *
 * ### 1. 样式持久化与恢复
 * - 将 [SpanData] 列表应用到 [Editable]，用于从数据库加载草稿时恢复格式
 * - 支持粗体、下划线、链接等多种样式类型
 *
 * ### 2. 样式切换（Toggle）
 * - 对选中文本应用或取消样式（如点击加粗按钮）
 * - 智能处理样式重叠和拆分，避免样式碎片化
 *
 * ### 3. Span 归一化
 * - 自动合并相邻的同类 Span，减少碎片数量
 * - 防止长文本编辑过程中 Span 片段无限增长，影响性能
 *
 * ## 设计亮点
 *
 * ### ✅ 智能拆分逻辑
 * 当用户选中部分已加粗的文本并再次点击加粗时：
 * - 完全覆盖：取消该区域的加粗样式
 * - 部分重叠：保留未选中部分的样式，仅取消选中部分
 *
 * ### ✅ 防碎片化机制
 * 每次样式编辑后调用 [normalizeSpans]，合并相邻的同类 Span：
 * - 例如：`[0-5]` 和 `[5-10]` 两个粗体 Span 会合并为 `[0-10]`
 * - 避免多次编辑后产生大量细小 Span，导致性能下降
 *
 * @see SpanData 持久化的样式数据结构
 * @see SpanType 样式类型枚举（粗体、下划线、链接等）
 */
object EditorSpanApplier {

    /**
     * 将持久化的 TextSpan 列表应用到 Editable（如 setBlocks 时恢复格式）。
     *
     * ## 使用场景
     * - 从数据库加载草稿时，恢复用户之前设置的样式
     * - 撤销/重做操作后重新应用样式
     *
     * @param editable 目标可编辑文本
     * @param spans 待应用的样式列表
     */
    fun applySpans(editable: Editable, spans: List<SpanData>) {
        spans.forEach { ts ->
            val span = when (ts.type) {
                SpanType.BOLD -> StyleSpan(Typeface.BOLD)
                SpanType.UNDERLINE -> UnderlineSpan()
                SpanType.LINK -> {
                    val url = ts.url ?: return@forEach
                    LinkSpan(url)
                }
                else -> return@forEach
            }
            editable.setSpan(span, ts.start, ts.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    /**
     * 切换粗体样式
     *
     * @param editText 目标编辑器
     */
    fun toggleBold(editText: EditText) {
        toggleStyle(editText, SpanType.BOLD)
    }

    /**
     * 切换下划线样式
     *
     * @param editText 目标编辑器
     */
    fun toggleUnderline(editText: EditText) {
        toggleStyle(editText, SpanType.UNDERLINE)
    }

    /**
     * 通用样式切换逻辑
     *
     * ## 处理流程
     * 1. 获取用户选区范围 [start, end)
     * 2. 检查选区是否有效（start < end）
     * 3. 根据样式类型调用 [applyOrRemoveSpan] 执行智能应用/取消
     *
     * @param editText 目标编辑器
     * @param type 样式类型
     */
    private fun toggleStyle(editText: EditText, type: SpanType) {
        val start = editText.selectionStart
        val end = editText.selectionEnd
        if (start >= end) return  // 无效选区，直接返回

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

    /**
     * 核心逻辑：智能应用或移除 Span 样式
     *
     * ## 处理策略
     *
     * ### 情况 1：无重叠 Span
     * 直接在选区上添加新样式
     *
     * ### 情况 2：有重叠 Span
     * 遍历每个重叠的 Span，进行精细拆分：
     * - **完全覆盖**（操作区间包含 Span）：删除该 Span（即取消样式）
     * - **左侧未选中**：保留左侧部分 `[spanStart, start)`
     * - **右侧未选中**：保留右侧部分 `[end, spanEnd)`
     */

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

        // span 粒度控制：遍历每个重叠的 Span，进行智能拆分
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
     * Span 归一化：合并相邻的同类 Span，减少碎片数量
     * ## 性能优化
     * - 时间复杂度：O(n log n)，主要是排序开销
     * - 空间复杂度：O(n)，创建临时列表
     * - 提前退出：如果 Span 数量 ≤ 1，直接返回
     */
    private fun normalizeSpans(editable: Editable, sampleSpan: Any) {
        val spanClass = sampleSpan::class.java
        val spans = editable.getSpans(0, editable.length, spanClass)
        if (spans.size <= 1) return  // 无需合并

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

            // 仅在"前一个刚好结束在后一个开始处"时合并，避免跨越未加粗区域
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

    /**
     * 克隆 Span 对象：创建一个新的相同类型的 Span 实例
     *
     * ## 为什么要克隆？
     *
     * Android 的 Span 对象是有状态的，记录了它作用的起始和结束位置。
     * 同一个 Span 不能同时作用于多个不连续的区域，必须创建副本。
     *
     * ## 支持的 Span 类型
     * - [StyleSpan]：粗体/斜体样式，保留 `style` 参数
     * - [UnderlineSpan]：下划线样式，无参构造
     * - （未来可扩展：ColorSpan, TypefaceSpan 等）
     *
     * @param span 待克隆的 Span 对象
     * @return 新的 Span 实例
     * @throws IllegalArgumentException 如果遇到不支持的 Span 类型
     */
    private fun cloneSpan(span: Any): Any {
        return when (span) {
            is StyleSpan -> StyleSpan(span.style)  // 保留原有的 style（BOLD/ITALIC 等）
            is UnderlineSpan -> UnderlineSpan()    // 无参构造，下划线样式固定
            // 未来可扩展：ForegroundColorSpan, BackgroundColorSpan, TypefaceSpan 等
            else -> throw IllegalArgumentException("Unsupported span type: ${span::class.java.simpleName}")
        }
    }
}
