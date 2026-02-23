package com.fishmemory.app.ui.publish.richtext.business.format

import android.text.SpannableStringBuilder
import com.fishmemory.app.ui.publish.richtext.core.model.BlockIdGenerator
import com.fishmemory.app.ui.publish.richtext.core.model.EditorBlockList
import com.fishmemory.app.ui.publish.richtext.business.link.LinkMetaFetcher
import com.fishmemory.app.ui.publish.richtext.business.link.LinkSpan
import com.fishmemory.app.ui.publish.richtext.core.model.EditorBlock
import com.fishmemory.app.ui.publish.richtext.core.model.EditorBlockEntity
import android.text.Spanned
import com.fishmemory.app.ui.publish.richtext.core.engine.validator.EditorUrlRules
import com.fishmemory.app.ui.publish.richtext.business.selection.BlockEditorUiSideEffects
import com.fishmemory.app.ui.publish.richtext.business.selection.OperationFocusResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * 编辑态块级结构操作的业务处理器（split/merge/backspace/结构退化与列表序号重算）。
 *
 * 约束：
 * - 只操作 `EditorBlockList` 中的数据；
 * - 所有 RecyclerView 更新与焦点落位通过 [BlockEditorUiSideEffects] 触发。
 */
class BlockActionManager(
    private val blockList: EditorBlockList,
    private val sideEffects: BlockEditorUiSideEffects,
    private val uiScope: CoroutineScope
) {

    data class ImageDeletionResult(
        val focusTargetDataPos: Int,
        // 仅当 focus 需要设置 selection 时才提供；否则保持默认语义（requestFocus only）
        val textSelection: Int? = null
    )

    data class LinkCardToTextResult(
        val textDataPos: Int,
        val selection: Int
    )

    data class UrlLineToLinkCardResult(
        val focusTargetDataPos: Int?,
        val focusSelection: Int = 0
    )

    data class CodeBlockInsertResult(
        val codeAdapterPos: Int
    )

    data class TrailingTextInsertResult(
        val trailingTextBlockId: String,
        val focus: OperationFocusResult
    )

    /**
     * CodeBlock 结构插入（结构+notify），不负责焦点与软键盘时序。
     */
    fun insertCodeBlockAtCursor(
        textBlockId: String,
        cursorPos: Int,
        language: String
    ): CodeBlockInsertResult? {
        val current = blockList.findBlock(textBlockId) as? EditorBlock.TextBlock ?: return null
        val text = current.text
        val safeCursor = cursorPos.coerceIn(0, text.length)

        val rightText = SpannableStringBuilder(text, safeCursor, text.length)
        text.delete(safeCursor, text.length)

        val codeBlock = EditorBlock.CodeBlock(
            id = BlockIdGenerator.nextId(),
            content = SpannableStringBuilder(),
            language = language
        )
        val newTextBlock = EditorBlock.TextBlock(
            id = BlockIdGenerator.nextId(),
            text = rightText,
            isQuote = current.isQuote,
            listType = current.listType
        )

        val insertBlocks = listOf<EditorBlock>(codeBlock, newTextBlock)
        blockList.insertBlocksAfter(textBlockId, insertBlocks)

        val codePos = blockList.getBlockPosition(codeBlock.id)
        if (codePos < 0) return null

        val codeAdapterPos = codePos + 1
        sideEffects.notifyItemRangeInserted(codeAdapterPos, insertBlocks.size)
        sideEffects.notifyContentChanged()
        return CodeBlockInsertResult(codeAdapterPos = codeAdapterPos)
    }

    fun insertCodeBlockAfterAnchor(
        anchorTextBlockId: String,
        language: String
    ): CodeBlockInsertResult? {
        val anchor = blockList.findBlock(anchorTextBlockId) as? EditorBlock.TextBlock ?: return null

        val codeBlock = EditorBlock.CodeBlock(
            id = BlockIdGenerator.nextId(),
            content = SpannableStringBuilder(),
            language = language
        )
        val newTextBlock = EditorBlock.TextBlock(
            id = BlockIdGenerator.nextId(),
            text = SpannableStringBuilder(),
            isQuote = anchor.isQuote,
            listType = anchor.listType
        )

        val insertBlocks = listOf<EditorBlock>(codeBlock, newTextBlock)
        blockList.insertBlocksAfter(anchorTextBlockId, insertBlocks)

        val codePos = blockList.getBlockPosition(codeBlock.id)
        if (codePos < 0) return null

        val codeAdapterPos = codePos + 1
        sideEffects.notifyItemRangeInserted(codeAdapterPos, insertBlocks.size)
        sideEffects.notifyContentChanged()
        return CodeBlockInsertResult(codeAdapterPos = codeAdapterPos)
    }

    /**
     * HrBlock 结构插入（光标分裂版本）。
     * 返回 trailing TextBlock 的 focus 目标给上层 selection/focus 模块。
     */
    fun insertHorizontalRuleAtCursor(
        textBlockId: String,
        cursorPos: Int
    ): OperationFocusResult? {
        val current = blockList.findBlock(textBlockId) as? EditorBlock.TextBlock ?: return null
        val text = current.text
        val safeCursor = cursorPos.coerceIn(0, text.length)

        val rightText = SpannableStringBuilder(text, safeCursor, text.length)
        text.delete(safeCursor, text.length)

        val hrBlock = EditorBlock.HrBlock(id = BlockIdGenerator.nextId())
        val newTextBlock = EditorBlock.TextBlock(
            id = BlockIdGenerator.nextId(),
            text = rightText,
            isQuote = current.isQuote,
            listType = current.listType
        )

        val insertBlocks = listOf<EditorBlock>(hrBlock, newTextBlock)
        blockList.insertBlocksAfter(textBlockId, insertBlocks)

        val hrPos = blockList.getBlockPosition(hrBlock.id)
        if (hrPos >= 0) sideEffects.notifyItemRangeInserted(hrPos + 1, insertBlocks.size)

        val newTextPos = blockList.getBlockPosition(newTextBlock.id)
        sideEffects.notifyContentChanged()
        return if (newTextPos >= 0) {
            OperationFocusResult(focusTargetDataPos = newTextPos, focusSelection = 0)
        } else null
    }

    fun insertHorizontalRuleAfterAnchor(anchorTextBlockId: String): OperationFocusResult? {
        val anchor = blockList.findBlock(anchorTextBlockId) as? EditorBlock.TextBlock ?: return null

        val hrBlock = EditorBlock.HrBlock(id = BlockIdGenerator.nextId())
        val newTextBlock = EditorBlock.TextBlock(
            id = BlockIdGenerator.nextId(),
            text = SpannableStringBuilder(),
            isQuote = anchor.isQuote,
            listType = anchor.listType
        )

        val insertBlocks = listOf<EditorBlock>(hrBlock, newTextBlock)
        blockList.insertBlocksAfter(anchorTextBlockId, insertBlocks)

        val hrPos = blockList.getBlockPosition(hrBlock.id)
        if (hrPos >= 0) sideEffects.notifyItemRangeInserted(hrPos + 1, insertBlocks.size)

        val newTextPos = blockList.getBlockPosition(newTextBlock.id)
        sideEffects.notifyContentChanged()
        return if (newTextPos >= 0) {
            OperationFocusResult(focusTargetDataPos = newTextPos, focusSelection = 0)
        } else null
    }

    fun insertImageAtCursor(
        textBlockId: String,
        cursorPos: Int,
        localUri: String
    ): TrailingTextInsertResult? {
        val current = blockList.findBlock(textBlockId) as? EditorBlock.TextBlock ?: return null
        val text = current.text
        val safeCursor = cursorPos.coerceIn(0, text.length)

        val rightText = SpannableStringBuilder(text, safeCursor, text.length)
        text.delete(safeCursor, text.length)

        val imageBlock = EditorBlock.ImageBlock(
            id = BlockIdGenerator.nextId(),
            localUri = localUri,
            remoteUrl = null,
            caption = "",
            uploadState = EditorBlockEntity.UploadState.SUCCESS
        )
        val newTextBlock = EditorBlock.TextBlock(
            id = BlockIdGenerator.nextId(),
            text = rightText,
            isQuote = current.isQuote,
            listType = current.listType
        )

        val insertBlocks = listOf<EditorBlock>(imageBlock, newTextBlock)
        blockList.insertBlocksAfter(textBlockId, insertBlocks)

        val imagePos = blockList.getBlockPosition(imageBlock.id)
        if (imagePos >= 0) sideEffects.notifyItemRangeInserted(imagePos + 1, insertBlocks.size)

        val newTextPos = blockList.getBlockPosition(newTextBlock.id)
        sideEffects.notifyContentChanged()

        return if (newTextPos >= 0) {
            TrailingTextInsertResult(
                trailingTextBlockId = newTextBlock.id,
                focus = OperationFocusResult(focusTargetDataPos = newTextPos, focusSelection = 0)
            )
        } else null
    }

    fun insertVideoAtCursor(
        textBlockId: String,
        cursorPos: Int,
        videoBlock: EditorBlock.VideoBlock
    ): TrailingTextInsertResult? {
        val current = blockList.findBlock(textBlockId) as? EditorBlock.TextBlock ?: return null
        val text = current.text
        val safeCursor = cursorPos.coerceIn(0, text.length)

        val rightText = SpannableStringBuilder(text, safeCursor, text.length)
        text.delete(safeCursor, text.length)

        val newTextBlock = EditorBlock.TextBlock(
            id = BlockIdGenerator.nextId(),
            text = rightText,
            isQuote = current.isQuote,
            listType = current.listType
        )

        val insertBlocks = listOf<EditorBlock>(videoBlock, newTextBlock)
        blockList.insertBlocksAfter(textBlockId, insertBlocks)

        val videoPos = blockList.getBlockPosition(videoBlock.id)
        if (videoPos >= 0) sideEffects.notifyItemRangeInserted(videoPos + 1, insertBlocks.size)

        val newTextPos = blockList.getBlockPosition(newTextBlock.id)
        sideEffects.notifyContentChanged()

        return if (newTextPos >= 0) {
            TrailingTextInsertResult(
                trailingTextBlockId = newTextBlock.id,
                focus = OperationFocusResult(focusTargetDataPos = newTextPos, focusSelection = 0)
            )
        } else null
    }

    fun addImageBlocksAfter(anchorBlockId: String, blocks: List<EditorBlock.ImageBlock>) {
        if (blocks.isEmpty()) return
        blockList.insertBlocksAfter(anchorBlockId, blocks)
        val startPos = blockList.getBlockPosition(blocks.first().id)
        if (startPos >= 0) sideEffects.notifyItemRangeInserted(startPos + 1, blocks.size)
        sideEffects.notifyContentChanged()
    }

    fun addVideoBlocksAfter(anchorBlockId: String, blocks: List<EditorBlock.VideoBlock>) {
        if (blocks.isEmpty()) return
        blockList.insertBlocksAfter(anchorBlockId, blocks)
        val startPos = blockList.getBlockPosition(blocks.first().id)
        if (startPos >= 0) sideEffects.notifyItemRangeInserted(startPos + 1, blocks.size)
        sideEffects.notifyContentChanged()
    }

    fun handleSplit(blockId: String, cursorPos: Int): OperationFocusResult? {
        val block = blockList.findBlock(blockId) as? EditorBlock.TextBlock ?: return null
        val text = block.text
        if (cursorPos < 0 || cursorPos > text.length) return null

        // 列表块且内容为空时，回车降级为普通文本块
        if (text.isEmpty() && block.listType != null) {
            block.listType = null
            block.orderIndex = 0
            val pos = blockList.getBlockPosition(blockId)
            if (pos >= 0) sideEffects.notifyItemChanged(pos + 1)
            recalculateNumberListOrderIndexes()
            return OperationFocusResult(focusTargetDataPos = null)
        }

        val right = SpannableStringBuilder(text, cursorPos, text.length)
        text.delete(cursorPos, text.length)

        val newBlock = EditorBlock.TextBlock(
            id = BlockIdGenerator.nextId(),
            text = right,
            isQuote = block.isQuote,
            listType = block.listType
        )
        blockList.insertBlockAfter(blockId, newBlock)

        val newPos = blockList.getBlockPosition(newBlock.id)
        val oldPos = blockList.getBlockPosition(blockId)
        if (oldPos >= 0) sideEffects.notifyItemChanged(oldPos + 1) // 当前块 nextIsQuote 可能变化
        if (newPos >= 0) sideEffects.notifyItemInserted(newPos + 1)

        recalculateNumberListOrderIndexes()
        return if (newPos >= 0) {
            OperationFocusResult(focusTargetDataPos = newPos, focusSelection = 0)
        } else {
            OperationFocusResult(focusTargetDataPos = null)
        }
    }

    fun handleMerge(blockId: String): OperationFocusResult? {
        val pos = blockList.getBlockPosition(blockId)
        if (pos <= 0) return null // 第一个块无法合并

        val blocks = blockList.getBlocks()
        val current = blocks.getOrNull(pos) as? EditorBlock.TextBlock
        val prev = blocks.getOrNull(pos - 1) as? EditorBlock.TextBlock
        if (current == null || prev == null) return null

        val prevTextLength = prev.text.length
        val currentText = current.text.toString()

        // 合并内容
        if (currentText.isNotEmpty()) {
            prev.text.append(current.text)
        }

        // 删除当前块
        blockList.removeBlock(blockId)
        sideEffects.notifyItemRemoved(pos + 1)

        // 更新上一个块
        sideEffects.notifyItemChanged(pos) // pos 是上一个块的新位置

        recalculateNumberListOrderIndexes()
        val newPrevPos = blockList.getBlockPosition(prev.id)
        val selection = prevTextLength + (if (currentText.isNotEmpty()) currentText.length else 0)
        return if (newPrevPos >= 0) {
            OperationFocusResult(focusTargetDataPos = newPrevPos, focusSelection = selection)
        } else {
            OperationFocusResult(focusTargetDataPos = null)
        }
    }

    fun handleBackspaceAtStart(blockId: String): OperationFocusResult? {
        val block = blockList.findBlock(blockId) as? EditorBlock.TextBlock ?: return null

        // 列表块在行首且内容为空时，退格降级为普通文本块
        if (block.listType != null && block.text.isEmpty()) {
            block.listType = null
            block.orderIndex = 0
            val pos = blockList.getBlockPosition(blockId)
            if (pos >= 0) sideEffects.notifyItemChanged(pos + 1)
            recalculateNumberListOrderIndexes()
            return OperationFocusResult(focusTargetDataPos = null)
        }

        // 处理引用块
        if (block.isQuote) {
            block.isQuote = false
            val pos = blockList.getBlockPosition(blockId)
            if (pos >= 0) sideEffects.notifyItemChanged(pos + 1)
            return OperationFocusResult(focusTargetDataPos = null)
        }

        // 获取当前块的位置
        val currentPos = blockList.getBlockPosition(blockId)
        if (currentPos <= 0) return null

        val blocks = blockList.getBlocks()
        val prevBlock = blocks.getOrNull(currentPos - 1) as? EditorBlock.TextBlock ?: return null

        val prevTextLength = prevBlock.text.length
        val currentText = block.text.toString()

        // 先合并内容
        prevBlock.text.append(block.text)

        // 从数据源中删除当前块
        blockList.removeBlock(blockId)

        // currentPos 是数据位置，adapter 位置需要 +1（因为标题占位置0）
        val currentAdapterPos = currentPos + 1
        sideEffects.notifyItemRemoved(currentAdapterPos)

        // 通知上一个块更新：上一个块的数据位置是 currentPos-1，adapter 位置是 currentPos
        sideEffects.notifyItemChanged(currentPos) // currentPos 就是上一个块的 adapter 位置
        sideEffects.notifyDataSetChanged()

        recalculateNumberListOrderIndexes()
        val newPrevPos = blockList.getBlockPosition(prevBlock.id)
        val selection = prevTextLength + currentText.length
        return if (newPrevPos >= 0) {
            OperationFocusResult(focusTargetDataPos = newPrevPos, focusSelection = selection)
        } else {
            OperationFocusResult(focusTargetDataPos = null)
        }
    }

    /**
     * Enter 回车请求处理：
     * - 标题块跳过；
     * - 若光标所在行是“纯 URL”，转换为 LinkCard，并返回可能的焦点交接信息。
     */
    fun handleEnterRequested(blockId: String, cursorPos: Int): UrlLineToLinkCardResult? {
        val block = blockList.findBlock(blockId) as? EditorBlock.TextBlock ?: return null
        val text = block.text
        if (cursorPos < 0 || cursorPos > text.length) return null

        // 标题块不做 URL 行转卡片，避免误触
        if (block.isHeading) return null

        val (lineStart, lineEnd) = EditorUrlRules.findCurrentLineRange(text, cursorPos)
        val line = text.substring(lineStart, lineEnd).trim()
        if (!EditorUrlRules.isValidUrlCandidate(line)) return null

        return transformUrlLineToLinkCard(
            blockId = blockId,
            url = line,
            lineStart = lineStart,
            lineEnd = lineEnd
        )
    }

    /**
     * 将“当前行仅 URL”转换为 LinkCard 块，并异步补全 OGP。
     *
     * 说明：
     * - 结构性插入/删除完全由本方法完成；
     * - OGP 拉取与 card 字段更新通过 [uiScope] + [sideEffects] 完成。
     */
    fun transformUrlLineToLinkCard(
        blockId: String,
        url: String,
        lineStart: Int,
        lineEnd: Int
    ): UrlLineToLinkCardResult? {
        val pos = blockList.getBlockPosition(blockId)
        if (pos < 0) return null

        val oldBlock = blockList.findBlock(blockId) as? EditorBlock.TextBlock ?: return null
        val text = oldBlock.text
        if (lineStart < 0 || lineEnd > text.length || lineStart > lineEnd) return null

        // left：保留 URL 行之前的内容
        val left = SpannableStringBuilder(text, 0, lineStart)

        // right：跳过 URL 行末尾的换行符（若有）
        var rightStart = lineEnd
        if (rightStart < text.length && text[rightStart] == '\n') rightStart++
        val right = SpannableStringBuilder(text, rightStart, text.length)

        val card = EditorBlock.LinkCard(
            id = BlockIdGenerator.nextId(),
            url = url,
            title = "正在解析链接...",
            description = "",
            imageUrl = null,
            isLoading = true,
            isSelected = false
        )

        // 1) 处理原 TextBlock（保留 left 或移除）
        var insertIndex = pos
        if (left.isNotEmpty()) {
            oldBlock.text = left
            sideEffects.notifyItemChanged(pos + 1)
            insertIndex = pos + 1
        } else {
            blockList.removeBlock(blockId)
            sideEffects.notifyItemRemoved(pos + 1)
        }

        // 2) 插入卡片块
        blockList.insertAt(insertIndex, card)
        sideEffects.notifyItemInserted(insertIndex + 1)

        // 3) 卡片前后保证都有可编辑的 TextBlock
        val currentBlocks = blockList.getBlocks()
        val hasPrevTextBlock = insertIndex > 0 && currentBlocks.getOrNull(insertIndex - 1) is EditorBlock.TextBlock
        val hasNextTextBlock = insertIndex + 1 < currentBlocks.size && currentBlocks.getOrNull(insertIndex + 1) is EditorBlock.TextBlock

        // 如果前面没有 TextBlock，在卡片前插入一个空 TextBlock
        if (!hasPrevTextBlock) {
            val beforeText = EditorBlock.TextBlock(
                id = BlockIdGenerator.nextId(),
                text = SpannableStringBuilder(),
                isQuote = false,
                isHeading = false,
                listType = null,
                orderIndex = 0
            )
            blockList.insertAt(insertIndex, beforeText)
            sideEffects.notifyItemInserted(insertIndex + 1)
            insertIndex++ // 更新卡片的插入位置
        }

        // 如果后面没有 TextBlock，在卡片后插入一个 TextBlock（包含剩余的文本）
        var focusAfterCardDataPos: Int? = null
        if (!hasNextTextBlock) {
            val afterText = EditorBlock.TextBlock(
                id = BlockIdGenerator.nextId(),
                text = right,
                isQuote = oldBlock.isQuote,
                isHeading = false,
                listType = null,
                orderIndex = 0
            )
            blockList.insertAt(insertIndex + 1, afterText)
            sideEffects.notifyItemInserted(insertIndex + 2)
            // 焦点交给 selection 模块；这里只返回“应该聚焦的 dataPos”
            val afterPos = blockList.getBlockPosition(afterText.id)
            if (afterPos >= 0) focusAfterCardDataPos = afterPos
        } else if (right.isNotEmpty()) {
            // 特殊情况：后面已有 TextBlock 但当前 URL 行后面还有文本
            val nextBlock = blockList.getBlocks().getOrNull(insertIndex + 1) as? EditorBlock.TextBlock
            if (nextBlock != null) {
                nextBlock.text.insert(0, right)
                nextBlock.text.insert(right.length, "\n")
                val nextPos = blockList.getBlockPosition(nextBlock.id)
                if (nextPos >= 0) {
                    sideEffects.notifyItemChanged(nextPos + 1)
                }
            }
        }

        // 4) 异步抓取 OGP 信息并刷新卡片
        uiScope.launch {
            val meta = LinkMetaFetcher.fetch(url)
            val cardPos = blockList.getBlockPosition(card.id)
            val current = blockList.findBlock(card.id) as? EditorBlock.LinkCard ?: return@launch

            current.title = meta.title.ifBlank { current.title }
            current.description = meta.description
            current.imageUrl = meta.imageUrl
            current.isLoading = false

            if (cardPos >= 0) sideEffects.notifyItemChanged(cardPos + 1)
        }
        return UrlLineToLinkCardResult(
            focusTargetDataPos = focusAfterCardDataPos,
            focusSelection = 0
        )
    }

    /**
     * 图片块删除的结构性处理（merge 前后 TextBlock / 仅删除自身）：
     * - 只修改 `EditorBlockList` 与触发 RecyclerView 数据更新（notifyItemRemoved / 序号重算 / dirty 通知）
     * - 不做具体 ViewHolder 的焦点设置（交给 SelectionManager / side-effects 完成）
     *
     * 返回值用于描述“删除后应该把焦点交给哪个块”，以保持旧交互语义。
     */
    fun handleImageBlockDeleteStructure(blockId: String): ImageDeletionResult? {
        val pos = blockList.getBlockPosition(blockId)
        if (pos < 0) return null

        val blocks = blockList.getBlocks()
        val prev = blocks.getOrNull(pos - 1)
        val next = blocks.getOrNull(pos + 1)

        val prevText = prev as? EditorBlock.TextBlock
        val nextText = next as? EditorBlock.TextBlock

        if (prevText != null && nextText != null) {
            // 上下都是文本块：删除图片块并合并前后文本
            prevText.text.append(nextText.text)

            // 保持旧实现的 notify 顺序（先移除 nextText，再通过结构删除移除 image，
            // 最后手动补齐 nextText/image 的 notifyItemRemoved 调用）。
            blockList.removeBlock(nextText.id)

            // 等价于旧的 removeBlock(blockId) 内部：notifyItemRemoved(pos + 1) + 序号重算/dirty
            val removedImage = blockList.removeBlock(blockId)
            if (!removedImage) return null
            sideEffects.notifyItemRemoved(pos + 1)
            recalculateNumberListOrderIndexes()
            sideEffects.notifyContentChanged()

            // 等价于旧实现中后续的两次手动移除通知
            sideEffects.notifyItemRemoved(pos + 2) // nextText 原位置
            sideEffects.notifyItemRemoved(pos + 1) // image 原位置

            val prevPos = blockList.getBlockPosition(prevText.id)
            if (prevPos < 0) return null
            return ImageDeletionResult(
                focusTargetDataPos = prevPos,
                textSelection = prevText.text.length
            )
        }

        // 否则：仅删除图片块，焦点交给它的前一个块（保持旧语义）
        val removed = blockList.removeBlock(blockId)
        if (!removed) return null
        sideEffects.notifyItemRemoved(pos + 1)

        recalculateNumberListOrderIndexes()
        sideEffects.notifyContentChanged()

        if (pos <= 0) return null
        return ImageDeletionResult(
            focusTargetDataPos = pos - 1,
            textSelection = null
        )
    }

    /**
     * 将 LinkCard 转换为“可点击 URL”的 TextBlock（保留 span）。
     *
     * 注意：
     * - 这是结构性数据变更（EditorBlockList + notify），不包含菜单/对话框等 UI 语义。
     * - 返回用于焦点交接的结果，让上层 selection 模块处理 requestFocus/setSelection。
     */
    fun convertLinkCardToTextStructure(cardId: String): LinkCardToTextResult? {
        val pos = blockList.getBlockPosition(cardId)
        if (pos < 0) return null
        val card = blockList.findBlock(cardId) as? EditorBlock.LinkCard ?: return null

        val urlText = SpannableStringBuilder(card.url)
        val linkSpan = LinkSpan(card.url)
        urlText.setSpan(linkSpan, 0, card.url.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        val textBlock = EditorBlock.TextBlock(
            id = BlockIdGenerator.nextId(),
            text = urlText,
            isQuote = false,
            isHeading = false,
            listType = null,
            orderIndex = 0
        )

        // 与旧实现保持一致：先删除 card（adapterPos = pos+1），再在相同位置插入 text（adapterPos = pos+1）
        val removed = blockList.removeBlock(cardId)
        if (!removed) return null
        sideEffects.notifyItemRemoved(pos + 1)

        blockList.insertAt(pos, textBlock)
        sideEffects.notifyItemInserted(pos + 1)

        return LinkCardToTextResult(textDataPos = pos, selection = urlText.length)
    }

    fun recalculateNumberListOrderIndexes() {
        val blocks = blockList.getBlocks()
        var currentIndex = 0
        blocks.forEach { block ->
            if (block is EditorBlock.TextBlock && block.listType == EditorBlock.ListType.NUMBER_LIST) {
                if (currentIndex == 0) currentIndex = 1
                block.orderIndex = currentIndex++
            } else {
                currentIndex = 0
            }
        }
        if (blocks.isNotEmpty()) {
            sideEffects.notifyItemRangeChanged(startAdapterPos = 1, itemCount = blocks.size)
        }
    }
}
