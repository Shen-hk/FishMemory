package com.fishmemory.app.ui.publish.richtext.core.model

/**
 * 统一展示用 Block 模型：编辑与只读共用。
 * - 编辑态：由 EditorBlock 转出用于渲染；只读态：由 StandardBlock 解析得到。
 * - 仅承载展示所需字段，不包含 SpannableStringBuilder、选中态等编辑态状态。
 */
sealed class EditorBlockDisplay {

    abstract val id: String

    data class Text(
        override val id: String,
        val content: String,
        val isQuote: Boolean = false,
        val isHeading: Boolean = false,
        val listType: EditorBlock.ListType? = null,
        val orderIndex: Int = 0
    ) : EditorBlockDisplay()

    data class Image(
        override val id: String,
        val url: String,
        val caption: String = ""
    ) : EditorBlockDisplay()

    data class Code(
        override val id: String,
        val code: String,
        val language: String
    ) : EditorBlockDisplay()

    data class Hr(override val id: String) : EditorBlockDisplay()

    data class LinkCard(
        override val id: String,
        val url: String,
        val title: String = "",
        val description: String = "",
        val imageUrl: String? = null
    ) : EditorBlockDisplay()

    data class Video(
        override val id: String,
        val url: String,
        val coverUrl: String? = null,
        val durationMs: Long = 0L
    ) : EditorBlockDisplay()

    /** 只读列表块（编辑态中为多个带 listType 的 TextBlock） */
    data class ListBlock(
        override val id: String,
        val listType: String, // "bullet" | "number"
        val items: List<ListItem>
    ) : EditorBlockDisplay() {
        data class ListItem(val content: String, val order: Int)
    }
}
