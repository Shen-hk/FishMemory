package com.fishmemory.app.ui.publish.richtext.business.selection

import android.util.Log
import com.fishmemory.app.ui.publish.richtext.core.model.EditorBlockList
import com.fishmemory.app.ui.publish.richtext.business.format.BlockActionManager
import com.fishmemory.app.ui.publish.richtext.core.model.EditorBlock

/**
 * 选中态/焦点落位管理器。
 *
 * 目标：
 * - 单一职责：只处理 HR/Code/LinkCard/Video/Image 的选中态与“删除后焦点交接”。
 * - 不承担弹窗/Intent 等 UI 语义（这类在后续 links/uiActions 迁移）。
 * - 结构性删除仍由外层执行（BlockEditorRecyclerView.removeBlock），本类只在删除后完成 clear/焦点。
 */
class SelectionManager(
    private val blockList: EditorBlockList,
    private val ui: BlockEditorUiSideEffects,
    // 用于删除后触发有序列表序号重算（保持现有交互语义）
    @Suppress("unused") private val actionManager: BlockActionManager,
) {
    private var selectedImageBlockId: String? = null
    private var selectedVideoBlockId: String? = null

    private var selectedHrPosition: Int = -1
    private var selectedCodePosition: Int = -1
    private var selectedLinkCardPosition: Int = -1

    fun clearHrSelection() {
        val old = selectedHrPosition
        if (old < 0) return
        selectedHrPosition = -1
        ui.setSelectedHrPosition(-1)
        ui.notifyItemChanged(old)
    }

    fun clearCodeSelection() {
        val old = selectedCodePosition
        if (old < 0) return
        selectedCodePosition = -1
        ui.setSelectedCodePosition(-1)
        ui.notifyItemChanged(old)
    }

    fun clearLinkCardSelection() {
        val old = selectedLinkCardPosition
        if (old < 0) return
        selectedLinkCardPosition = -1
        ui.setSelectedLinkCardPosition(-1)
        ui.notifyItemChanged(old)
    }

    fun clearVideoSelection() {
        val id = selectedVideoBlockId ?: return
        selectedVideoBlockId = null
        ui.setSelectedVideoBlockId(null)
        val pos = blockList.getBlockPosition(id)
        // 旧代码：删除视频后 id 已不存在，这里不会触发 notify（保持一致）
        if (pos >= 0) ui.notifyItemChanged(pos + 1)
    }

    fun clearImageSelection() {
        val id = selectedImageBlockId ?: return
        val block = blockList.findBlock(id) as? EditorBlock.ImageBlock ?: return
        if (!block.isSelected) {
            selectedImageBlockId = null
            return
        }
        block.isSelected = false
        selectedImageBlockId = null
        val pos = blockList.getBlockPosition(id)
        if (pos >= 0) ui.notifyItemChanged(pos + 1)
    }

    fun selectImageBlock(blockId: String) {
        if (selectedImageBlockId == blockId) return
        val previousId = selectedImageBlockId
        selectedImageBlockId = blockId

        if (previousId != null) {
            val prevBlock = blockList.findBlock(previousId) as? EditorBlock.ImageBlock
            if (prevBlock != null && prevBlock.isSelected) {
                prevBlock.isSelected = false
                val prevPos = blockList.getBlockPosition(previousId)
                if (prevPos >= 0) ui.notifyItemChanged(prevPos + 1)
            }
        }

        val current = blockList.findBlock(blockId) as? EditorBlock.ImageBlock ?: return
        current.isSelected = true
        val currentPos = blockList.getBlockPosition(blockId)
        if (currentPos >= 0) ui.notifyItemChanged(currentPos + 1)
    }

    fun onHrClicked(adapterPos: Int) {
        if (adapterPos <= 0) return // 0 为标题
        val old = selectedHrPosition
        selectedHrPosition = adapterPos
        ui.setSelectedHrPosition(selectedHrPosition)
        if (old >= 0 && old != adapterPos) ui.notifyItemChanged(old)
        ui.notifyItemChanged(adapterPos)
    }

    fun onHrDeletedAfterRemoval(originalDataPos: Int) {
        // 删除 HR 后焦点交给相邻 TextBlock
        focusNearestTextAfterDeletionDataPos(originalDataPos)
        selectedHrPosition = -1
        ui.setSelectedHrPosition(-1)
    }

    fun onCodeClicked(adapterPos: Int) {
        if (adapterPos <= 0) return
        Log.d("CodeSelect", "[onCodeClicked] adapterPos=$adapterPos, oldSelected=$selectedCodePosition")
        val old = selectedCodePosition
        selectedCodePosition = adapterPos
        ui.setSelectedCodePosition(selectedCodePosition)
        Log.d("CodeSelect", "[onCodeSelect] updated to $selectedCodePosition, notifying UI")
        if (old >= 0 && old != adapterPos) ui.notifyItemChanged(old)
        ui.notifyItemChanged(adapterPos)
    }

    /**
     * 在“插入后直接选中”场景下使用：只更新 UI 状态，不额外通知旧选中项。
     * 目的是最大化保持现有交互语义与刷新粒度一致。
     */
    fun setCodeSelectedPositionSilently(adapterPos: Int) {
        if (adapterPos <= 0) return
        selectedCodePosition = adapterPos
        ui.setSelectedCodePosition(adapterPos)
    }

    /**
     * CodeBlock 结构插入后的焦点落位语义。
     * 保持旧交互：焦点直接进入 CodeBlock 的编辑框（并尝试展示软键盘）。
     */
    fun focusAfterCodeInsert(codeAdapterPos: Int) {
        if (codeAdapterPos <= 0) return
        ui.post {
            val ok = ui.tryFocusCodeBlockByAdapterPos(codeAdapterPos)
            if (!ok) {
                ui.postDelayed(100L) {
                    ui.tryFocusCodeBlockByAdapterPos(codeAdapterPos)
                }
            }
        }
    }

    fun onCodeDeletedAfterRemoval(originalDataPos: Int) {
        focusNearestTextAfterDeletionDataPos(originalDataPos)
        selectedCodePosition = -1
        ui.setSelectedCodePosition(-1)
    }

    fun onLinkCardClicked(adapterPos: Int) {
        if (adapterPos <= 0) return // 0 为标题
        val old = selectedLinkCardPosition
        selectedLinkCardPosition = adapterPos
        ui.setSelectedLinkCardPosition(selectedLinkCardPosition)
        if (old >= 0 && old != adapterPos) ui.notifyItemChanged(old)
        ui.notifyItemChanged(adapterPos)
    }

    fun onLinkCardDeletedAfterRemoval(originalDataPos: Int) {
        // 旧代码：删除后 clearLinkCardSelection 再尝试焦点交接
        clearLinkCardSelection()
        focusNearestTextAfterDeletionDataPos(originalDataPos)
    }

    fun onVideoClicked(blockId: String) {
        if (selectedVideoBlockId == blockId) return

        selectedVideoBlockId?.let { old ->
            val oldBlock = blockList.findBlock(old) as? EditorBlock.VideoBlock
            oldBlock?.isSelected = false
            val oldPos = blockList.getBlockPosition(old)
            if (oldPos >= 0) ui.notifyItemChanged(oldPos + 1)
        }

        selectedVideoBlockId = blockId
        ui.setSelectedVideoBlockId(blockId)
        val block = blockList.findBlock(blockId) as? EditorBlock.VideoBlock
        block?.isSelected = true
        val pos = blockList.getBlockPosition(blockId)
        if (pos >= 0) ui.notifyItemChanged(pos + 1)
    }

    fun onVideoDeletedAfterRemoval(originalDataPos: Int) {
        // 旧代码：删除后先 clearVideoSelection，再尝试焦点交接
        clearVideoSelection()
        focusNearestTextAfterDeletionDataPos(originalDataPos)
    }

    private fun focusNearestTextAfterDeletionDataPos(originalDataPos: Int) {
        val blocks = blockList.getBlocks()
        val prev = blocks.getOrNull(originalDataPos - 1) as? EditorBlock.TextBlock
        val next = blocks.getOrNull(originalDataPos) as? EditorBlock.TextBlock
        val targetId = prev?.id ?: next?.id
        if (targetId == null) return
        val targetPos = blockList.getBlockPosition(targetId)
        if (targetPos < 0) return
        ui.post {
            ui.requestFocusTextBlockByAdapterPos(targetPos + 1)
        }
    }

    /**
     * 图片删除后的焦点交接（与旧逻辑保持一致）：
     * - 若目标块是 TextBlock：requestFocus（可选设置 selection）
     * - 若目标块是 ImageBlock：requestFocus 对应 caption 的 EditText
     */
    fun focusAfterImageDeletion(targetDataPos: Int, textSelection: Int? = null) {
        if (targetDataPos < 0) return
        val adapterPos = targetDataPos + 1
        val blocks = blockList.getBlocks()
        val targetBlock = blocks.getOrNull(targetDataPos)

        ui.post {
            when {
                textSelection != null && targetBlock is EditorBlock.TextBlock -> {
                    ui.tryFocusTextBlockByAdapterPos(adapterPos, textSelection)
                }
                targetBlock is EditorBlock.ImageBlock -> {
                    ui.requestFocusImageCaptionByAdapterPos(adapterPos)
                }
                else -> {
                    ui.requestFocusTextBlockByAdapterPos(adapterPos)
                }
            }
        }
    }

    /**
     * LinkCard -> TextBlock 转换后的焦点交接（保留旧语义：focus + setSelection(url.length)）。
     */
    fun focusAfterLinkCardConvertedToText(textDataPos: Int, selection: Int) {
        ui.post {
            ui.tryFocusTextBlockByAdapterPos(textDataPos + 1, selection)
        }
    }

    /**
     * Split 后的焦点落位语义（复刻旧实现）：
     * - 先由 action layer 完成结构 notify（split/insert）
     * - 这里仅在 UI 线程 post 后 tryFocus 到“新块行首（selection=0）”
     */
    fun focusAfterSplit(focusDataPos: Int, selection: Int = 0) {
        ui.post {
            ui.tryFocusTextBlockByAdapterPos(focusDataPos + 1, selection)
        }
    }

    /**
     * Merge 后的焦点落位语义（复刻旧实现）：
     * - 不做 scroll（旧代码也不做）
     * - post 后 tryFocus 到“合并后的上一个块行尾”
     */
    fun focusAfterMerge(focusDataPos: Int, selection: Int) {
        ui.post {
            ui.tryFocusTextBlockByAdapterPos(focusDataPos + 1, selection)
        }
    }

    /**
     * Backspace-at-start 降级/合并后的焦点落位语义（复刻旧实现）：
     * - 先 scrollToAdapterPos
     * - 再 postDelayed(100) 之后 tryFocus
     * - 若 tryFocus 失败：notifyItemChanged + 再 post 重试
     */
    fun focusAfterBackspaceAtStart(focusDataPos: Int, selection: Int) {
        val targetAdapterPos = focusDataPos + 1
        ui.post {
            ui.scrollToAdapterPos(targetAdapterPos)
            ui.postDelayed(100L) {
                val ok = ui.tryFocusTextBlockByAdapterPos(targetAdapterPos, selection)
                if (!ok) {
                    ui.notifyItemChanged(targetAdapterPos)
                    ui.post {
                        ui.tryFocusTextBlockByAdapterPos(targetAdapterPos, selection)
                    }
                }
            }
        }
    }

    /**
     * URL 行 -> LinkCard 插入完成后的焦点交接。
     * 保持旧语义：聚焦到“卡片后文本块行首”（selection=0），但不承担结构/notify 语义。
     */
    fun focusAfterUrlLineConvertedToLinkCard(focusTargetDataPos: Int, focusSelection: Int = 0) {
        ui.post {
            ui.tryFocusTextBlockByAdapterPos(focusTargetDataPos + 1, focusSelection)
        }
    }

    /**
     * HR / Image / Video 等“结构插入后，新插入 trailing TextBlock 行首聚焦”的统一语义。
     *
     * 为什么要抽出来：
     * - 避免在 `BlockEditorRecyclerView` 内复制 `post { findViewHolder... requestFocus + setSelection(0) }`。
     * - 保持同一份“ViewHolder 未就绪时的失败重试”策略，提高稳定性。
     */
    fun focusAfterTrailingTextInsert(trailingTextDataPos: Int, trailingSelection: Int = 0) {
        val adapterPos = trailingTextDataPos + 1
        ui.post {
            ui.ensureBlockVisibleByAdapterPos(adapterPos)
            ui.postDelayed(100L) {
                val ok = ui.tryFocusTextBlockByAdapterPos(adapterPos, trailingSelection)
                if (!ok) {
                    ui.notifyItemChanged(adapterPos)
                    ui.post {
                        ui.tryFocusTextBlockByAdapterPos(adapterPos, trailingSelection)
                    }
                }
            }
        }
    }
}

