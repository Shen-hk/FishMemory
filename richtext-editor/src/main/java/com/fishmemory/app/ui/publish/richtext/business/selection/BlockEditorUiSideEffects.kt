package com.fishmemory.app.ui.publish.richtext.business.selection

/**
 * UI 层对外暴露的“副作用”抽象层。
 *
 * 为什么需要它：
 * - 业务层（ActionManager/Coordinator）只关心数据与编辑语义；
 * - 但 split/merge/backspace 等操作必须更新 RecyclerView 并进行焦点落位，这些是 UI 副作用。
 *
 * 约束：
 * - 只暴露必要的原子操作，避免业务层依赖 RecyclerView/ViewHolder 实现细节。
 */
interface BlockEditorUiSideEffects {
    fun notifyItemChanged(adapterPos: Int)
    fun notifyItemInserted(adapterPos: Int)
    fun notifyItemRemoved(adapterPos: Int)
    fun notifyItemRangeInserted(startAdapterPos: Int, itemCount: Int)
    fun notifyItemRangeChanged(startAdapterPos: Int, itemCount: Int)
    fun notifyDataSetChanged()

    fun scrollToAdapterPos(adapterPos: Int)

    /**
     * 确保指定 adapterPos 对应的 Item 已经进入“可见区域”，避免 focus 时 ViewHolder 仍未创建/布局未完成。
     * 内部允许使用 findViewHolder + requestRectangleOnScreen + scrollToPosition + post 组合实现。
     */
    fun ensureBlockVisibleByAdapterPos(adapterPos: Int)

    fun post(action: () -> Unit)
    fun postDelayed(delayMs: Long, action: () -> Unit)

    /**
     * 尝试在指定 adapterPos 上把 TextBlock 的 EditText 获取焦点并设置 selection。
     * @return 是否成功（ViewHolder 尚未创建时可能返回 false）
     */
    fun tryFocusTextBlockByAdapterPos(adapterPos: Int, selection: Int): Boolean

    /**
     * 尝试在指定 adapterPos 上把 CodeBlock 的编辑框获取焦点（并展示软键盘）。
     * @return 是否成功（ViewHolder 尚未创建时可能返回 false）
     */
    fun tryFocusCodeBlockByAdapterPos(adapterPos: Int): Boolean

    /**
     * 请求焦点到指定 adapterPos 的 TextBlock（不改变 selection）。
     * 用于保持删除/切换后的历史交互语义。
     */
    fun requestFocusTextBlockByAdapterPos(adapterPos: Int): Boolean

    // -------- 结构块选中态（由 Adapter 持有的 UI 状态）--------
    fun setSelectedHrPosition(position: Int)
    fun setSelectedCodePosition(position: Int)
    fun setSelectedLinkCardPosition(position: Int)
    fun setSelectedVideoBlockId(blockId: String?)

    /**
     * 请求焦点到指定 adapterPos 的 ImageBlock 注释输入框（etCaption）。
     * 用于保持删除/切换后“焦点落在 caption”的既有交互语义。
     */
    fun requestFocusImageCaptionByAdapterPos(adapterPos: Int): Boolean

    /**
     * 请求焦点到指定 adapterPos 的 ImageBlock 注释输入框，并把光标移动到末尾。
     * 用于“显示 caption 并开始输入”的交互语义（保证默认光标在文本末端）。
     */
    fun requestFocusImageCaptionEndByAdapterPos(adapterPos: Int): Boolean

    /** 通知外层“内容已变更”（用于草稿 dirty/自动保存等）。 */
    fun notifyContentChanged()
}

