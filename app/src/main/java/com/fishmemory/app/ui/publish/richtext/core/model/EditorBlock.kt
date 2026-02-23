package com.fishmemory.app.ui.publish.richtext.core.model

import android.text.SpannableStringBuilder



/**
 * Block Editor 内存编辑态块类型。
 * 与 RichBlock 分离：RichBlock 用于序列化/提交，EditorBlock 用于编辑态。
 */
sealed class EditorBlock {

    abstract val id: String

    data class TextBlock(
        override val id: String,
        var text: SpannableStringBuilder,
        // 是否为整块引用样式，由 BlockEditorRecyclerView 控制；导出 RichBlock 时暂不序列化
        var isQuote: Boolean = false,
        // 是否为标题块样式，仅编辑态使用，不参与 RichBlock 序列化
        var isHeading: Boolean = false,
        // 列表类型：null 表示普通文本块，BULLET_LIST/NUMBER_LIST 表示无序/有序列表块
        var listType: ListType? = null,
        // 有序列表序号，仅在 listType == NUMBER_LIST 时生效
        var orderIndex: Int = 0
    ) : EditorBlock()

    data class ImageBlock(
        override val id: String,
        var localUri: String?,
        var remoteUrl: String?,
        var caption: String = "",
        var uploadState: EditorBlockEntity.UploadState = EditorBlockEntity.UploadState.PENDING,
        var progress: Int = 0,
        // 仅编辑态使用的 UI 状态：是否选中、是否展示下方注释框
        var isSelected: Boolean = false,
        var showCaption: Boolean = false,
        // 注释当前是否处于「可编辑」态；显示但不可编辑时为 false
        var isCaptionEditing: Boolean = false
    ) : EditorBlock()

    /**
     * 视频块：承载本地/远程视频、封面与上传/播放状态。
     * - localUri：本地选择的视频 URI
     * - remoteUrl：上传成功后的线上地址
     * - coverUrl：封面图本地路径或远程 URL
     * - durationMs：视频时长（毫秒）
     * - uploadState / uploadProgress：上传状态与进度
     * - playState：当前播放状态，仅编辑态使用
     */
    data class VideoBlock(
        override val id: String,
        var localUri: String?,
        var remoteUrl: String?,
        var coverUrl: String? = null,
        var durationMs: Long = 0L,
        var uploadState: EditorBlockEntity.UploadState = EditorBlockEntity.UploadState.PENDING,
        var uploadProgress: Int = 0,
        var playState: PlayState = PlayState.IDLE,
        var isSelected: Boolean = false
    ) : EditorBlock()

    /**
     * 水平分割线块：占据一整行，不承载文本，仅作为结构性分隔符。
     * 仅编辑态使用（RichBlock 暂不序列化 HR），支持选中态用于展示删除按钮与高亮边框。
     */
    data class HrBlock(
        override val id: String,
        var isSelected: Boolean = false
    ) : EditorBlock()

    /**
     * 代码块：使用等宽字体与语法高亮展示，可编辑。
     * content 通过 SpannableStringBuilder 保留 Span，高亮时由 CodeHighlightEngine 直接作用于 Editable。
     */
    data class CodeBlock(
        override val id: String,
        var content: SpannableStringBuilder,
        var language: String,
        val type: String = TYPE_CODE,
        var isSelected: Boolean = false
    ) : EditorBlock()

    /**
     * 链接卡片块：原子块（Atomic Block）。
     * - 点击进入选中态显示删除按钮
     * - Backspace 不删除（由 ViewHolder 拦截）
     */
    data class LinkCard(
        override val id: String,
        val url: String,
        var title: String = "",
        var description: String = "",
        var imageUrl: String? = null,
        var isLoading: Boolean = true,
        var isSelected: Boolean = false
    ) : EditorBlock()

    companion object {
        const val TYPE_CODE: String = "TYPE_CODE"
    }

    /**
     * 文本块的列表类型定义。
     * - BULLET_LIST：无序列表
     * - NUMBER_LIST：有序列表
     */
    enum class ListType {
        BULLET_LIST,
        NUMBER_LIST
    }

    /**
     * 视频播放状态，仅用于编辑态控制 UI。
     */
    enum class PlayState {
        IDLE,
        PLAYING,
        PAUSED,
        ERROR
    }
}
