package com.fishmemory.app.ui.publish.richtext.business.selection

import com.fishmemory.app.ui.publish.richtext.core.model.EditorBlockList
import com.fishmemory.app.ui.publish.richtext.core.model.EditorBlock

/**
 * 焦点/滚动导航管理器。
 *
 * 为什么需要它：
 * - 让 `BlockEditorRecyclerView` 只做“回调路由 + side-effects 实现”，避免把 scroll/focus 细节散落在多个函数里。
 * - 让“哪个 block 是当前聚焦源（lastFocusedBlockId）”成为单一职责的数据来源。
 */
class FocusManager(
    private val blockList: EditorBlockList,
    private val ui: BlockEditorUiSideEffects
) {
    private var lastFocusedBlockId: String? = null

    fun getLastFocusedBlockId(): String? = lastFocusedBlockId

    fun onTextBlockFocusGained(blockId: String) {
        if (lastFocusedBlockId == blockId) return
        lastFocusedBlockId = blockId
        scrollToBlock(blockId)
    }

    fun onCodeBlockFocusGained(blockId: String) {
        if (lastFocusedBlockId == blockId) return
        lastFocusedBlockId = blockId
        scrollToBlock(blockId)
    }

    fun scrollToBlock(blockId: String) {
        val dataPos = blockList.getBlockPosition(blockId)
        if (dataPos < 0) return
        ui.ensureBlockVisibleByAdapterPos(dataPos + 1)
    }

    /** 焦点到指定 TextBlock dataPos（注意：adapterPos = dataPos + 1）。 */
    fun focusBlockAtDataPos(dataPos: Int) {
        val block = blockList.getBlocks().getOrNull(dataPos) as? EditorBlock.TextBlock ?: return
        lastFocusedBlockId = block.id
        val adapterPos = dataPos + 1
        ui.ensureBlockVisibleByAdapterPos(adapterPos)
        ui.post {
            val ok = ui.requestFocusTextBlockByAdapterPos(adapterPos)
            if (!ok) {
                // ViewHolder 可能尚未创建完成：做一次延迟重试，保持旧聚焦稳定性
                ui.postDelayed(100L) {
                    ui.requestFocusTextBlockByAdapterPos(adapterPos)
                }
            }
        }
    }

    /** 焦点到第一个 TextBlock，供标题回车跳转。 */
    fun focusFirstTextBlock() {
        val blocks = blockList.getBlocks()
        val idx = blocks.indexOfFirst { it is EditorBlock.TextBlock }
        if (idx < 0) return
        focusBlockAtDataPos(idx)
    }
}