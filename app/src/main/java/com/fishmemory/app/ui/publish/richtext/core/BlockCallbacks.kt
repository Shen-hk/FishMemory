package com.fishmemory.app.ui.publish.richtext.core

import com.fishmemory.app.ui.publish.richtext.business.selection.OperationFocusResult

/**
 * 定义 BlockEditText 与外层（Adapter/Coordinator）通信的标准协议
 */
interface BlockInteractionListener {
    /** 回车分裂请求 */
    fun onSplitRequested(blockId: String, cursorPos: Int): OperationFocusResult?

    /** 回车特殊处理（如 URL 转卡片） */
    fun onEnterRequested(blockId: String, cursorPos: Int): Boolean

    /** 块首退格合并 */
    fun onMergeRequested(blockId: String): OperationFocusResult?

    /** 块首退格特殊处理（如取消引用样式） */
    fun onBackspaceAtStart(blockId: String): OperationFocusResult?

    /** 获得焦点通知 */
    fun onFocusGained(blockId: String)

    /** 内容变更通知 */
    fun onContentChanged(blockId: String)

    /** 链接点击 */
    fun onLinkClicked(blockId: String, url: String, start: Int, end: Int)
}