package com.fishmemory.app.ui.publish.richtext.ui.container

import android.content.Context
import android.net.Uri
import android.text.SpannableStringBuilder
import android.util.Log
import android.view.View
import android.widget.EditText
import androidx.recyclerview.widget.RecyclerView
import com.fishmemory.app.ui.publish.richtext.api.EditorCallback
import com.fishmemory.app.ui.publish.richtext.api.EditorVideoUploader
import com.fishmemory.app.ui.publish.richtext.business.format.BlockActionManager
import com.fishmemory.app.ui.publish.richtext.business.selection.FocusManager
import com.fishmemory.app.ui.publish.richtext.business.selection.SelectionManager
import com.fishmemory.app.ui.publish.richtext.business.selection.BlockEditorUiSideEffects
import com.fishmemory.app.ui.publish.richtext.core.converter.BlockDocumentConverter
import com.fishmemory.app.ui.publish.richtext.core.model.BlockIdGenerator
import com.fishmemory.app.ui.publish.richtext.core.model.EditorBlock
import com.fishmemory.app.ui.publish.richtext.core.model.EditorBlockEntity
import com.fishmemory.app.ui.publish.richtext.core.model.EditorBlockList
import com.fishmemory.app.ui.publish.richtext.ui.actions.LinkUiActions
import com.fishmemory.app.ui.publish.ai.AiAssistUiState
import com.fishmemory.app.ui.publish.richtext.ui.adapter.EditorAdapter
import com.fishmemory.app.ui.publish.richtext.ui.adapter.LinkCardViewHolder
import com.fishmemory.app.ui.publish.richtext.ui.adapter.TextBlockViewHolder

/**
 * BlockEditor 的业务/事件中枢：对外 Facade API + adapter 事件链语义。
 *
 * 约束：
 * - 行为与原 `BlockEditorRecyclerView` 内部实现保持一致；
 * - `BlockEditorRecyclerView` 只保留 RecyclerView/生命周期相关少量逻辑，其他全部委托到本类。
 */
class BlockEditorOperations(
    private val recyclerView: RecyclerView,
    private val context: Context,
    private val blockList: EditorBlockList,
    private val adapter: EditorAdapter,
    private val uiSideEffects: BlockEditorUiSideEffects,
    private val actionManager: BlockActionManager,
    private val selectionManager: SelectionManager,
    private val focusManager: FocusManager,
    private val linkUiActions: LinkUiActions,
    private val notifyContentChanged: () -> Unit,
) {
    /** 标题文本仅用于编辑态显示与 RichDocument 同步。 */
    private var titleText: String = ""

    // 外部回调：图片预览、菜单弹出
    private var editorCallback: EditorCallback? = null
    private var onPreviewRequested: ((url: String) -> Unit)? = null
    private var onImageBlockMenuRequested: ((anchorView: View, blockId: String) -> Unit)? = null
    private var onImageBlockReplaceRequested: ((blockId: String) -> Unit)? = null
    private var onAiSparkle: ((String) -> Unit)? = null
    private var onAiAccept: ((String) -> Unit)? = null
    private var onAiRetry: ((String) -> Unit)? = null
    private var onAiDiscard: ((String) -> Unit)? = null

    // Adapter 位置与数据位置转换（Adapter 第 0 位是标题）
    private fun adapterPosOfDataPos(dataPos: Int): Int = dataPos + 1
    private fun dataPosOfAdapterPos(adapterPos: Int): Int = adapterPos - 1

    /** 初始化 adapter 的事件回调（包含原来的 setupBlockCallbacks 全部语义）。 */
    fun bindAdapterCallbacks() {
        adapter.onSplitRequested = { blockId, cursorPos ->
            val result = actionManager.handleSplit(blockId, cursorPos)
            if (result?.focusTargetDataPos != null) {
                selectionManager.focusAfterSplit(
                    focusDataPos = result.focusTargetDataPos,
                    selection = result.focusSelection
                )
            }
            result
        }
        adapter.onEnterRequested = { blockId, cursorPos -> handleEnterRequested(blockId, cursorPos) }
        adapter.onMergeRequested = { blockId ->
            val result = actionManager.handleMerge(blockId)
            if (result?.focusTargetDataPos != null) {
                selectionManager.focusAfterMerge(
                    focusDataPos = result.focusTargetDataPos,
                    selection = result.focusSelection
                )
            }
            result
        }
        adapter.onBackspaceAtStart = { blockId ->
            val result = actionManager.handleBackspaceAtStart(blockId)
            if (result?.focusTargetDataPos != null) {
                selectionManager.focusAfterBackspaceAtStart(
                    focusDataPos = result.focusTargetDataPos,
                    selection = result.focusSelection
                )
            }
            result
        }
        adapter.onLinkClicked = { blockId, url, start, end ->
            handleInlineLinkClicked(blockId, url, start, end)
        }
        adapter.onFocusGained = { blockId ->
            // 文本块获得焦点时，清空结构块选中态，避免视觉干扰
            selectionManager.clearHrSelection()
            selectionManager.clearCodeSelection()
            selectionManager.clearLinkCardSelection()
            selectionManager.clearVideoSelection()
            setFocusedTextBlockForAi(blockId)
            focusManager.onTextBlockFocusGained(blockId)
        }
        adapter.onFocusLost = { blockId ->
            if (adapter.focusedTextBlockId == blockId) {
                setFocusedTextBlockForAi(null)
            }
        }
        adapter.onImageBlockDeleteRequested = { blockId ->
            handleImageBlockDelete(blockId)
        }
        adapter.onImageBlockReplaceRequested = { blockId ->
            editorCallback?.onImageReplaceRequested(blockId)
            onImageBlockReplaceRequested?.invoke(blockId)
        }
        adapter.onImageBlockPreviewRequested = { blockId ->
            handleImageBlockPreview(blockId)
        }
        adapter.onImageBlockChanged = { blockId ->
            notifyImageBlockChanged(blockId)
            notifyContentChanged()
        }
        adapter.onImageBlockMenuRequested = { anchorView, blockId ->
            selectionManager.selectImageBlock(blockId)
            editorCallback?.onImageMenuRequested(anchorView, blockId)
            onImageBlockMenuRequested?.invoke(anchorView, blockId)
        }
        adapter.onHrBlockClicked = { position -> selectionManager.onHrClicked(position) }
        adapter.onHrBlockDeleteRequested = { position -> handleHrBlockDelete(position) }
        adapter.onCodeBlockClicked = { position -> handleCodeBlockClicked(position) }
        adapter.onCodeBlockDeleteRequested = { position -> handleCodeBlockDelete(position) }
        adapter.onCodeBlockFocusGained = { blockId ->
            // 代码块聚焦时取消「正文块 AI 入口」的焦点归属，避免 sparkle 仍挂在旧文本块上
            setFocusedTextBlockForAi(null)
            focusManager.onCodeBlockFocusGained(blockId)
        }
        adapter.onLinkCardClicked = { position -> handleLinkCardClicked(position) }
        adapter.onLinkCardDeleteRequested = { blockId -> handleLinkCardDelete(blockId) }
        adapter.onVideoBlockDeleteRequested = { blockId -> handleVideoBlockDelete(blockId) }
        adapter.onVideoBlockRetryRequested = { blockId -> handleVideoBlockRetry(blockId) }
        adapter.onVideoBlockChanged = { blockId -> notifyVideoBlockChanged(blockId) }
        adapter.onVideoBlockClicked = { blockId -> handleVideoBlockClicked(blockId) }
        adapter.onTitleChanged = { text ->
            titleText = text
            notifyContentChanged()
        }
        adapter.onTitleNextRequested = { focusFirstTextBlock() }
        adapter.onContentChanged = { notifyContentChanged() }
        adapter.onAiSparkleClick = { blockId ->
            editorCallback?.onAiPolishRequested(blockId)
            onAiSparkle?.invoke(blockId)
        }
        adapter.onAiAccept = { blockId ->
            editorCallback?.onAiPreviewAccepted(blockId)
            onAiAccept?.invoke(blockId)
        }
        adapter.onAiRetry = { blockId ->
            editorCallback?.onAiRetryRequested(blockId)
            onAiRetry?.invoke(blockId)
        }
        adapter.onAiDiscard = { blockId ->
            editorCallback?.onAiPreviewDiscarded(blockId)
            onAiDiscard?.invoke(blockId)
        }
    }

    fun ensureOneBlockIfEmpty() {
        if (blockList.isEmpty()) {
            blockList.insertAt(
                0,
                EditorBlock.TextBlock(
                    id = BlockIdGenerator.nextId(),
                    text = SpannableStringBuilder(),
                    isQuote = false
                )
            )
            adapter.notifyItemInserted(1)
        }
    }

    /** 设置内容变更监听，用于自动保存。 */
    // 保持在外层 BlockEditorRecyclerView 负责监听，Operations 只调用 notifyContentChanged()

    fun setEditorCallback(callback: EditorCallback?) {
        editorCallback = callback
    }

    fun setOnImageBlockPreviewRequested(callback: (url: String) -> Unit) {
        onPreviewRequested = callback
    }

    fun setOnImageBlockMenuRequested(callback: (anchorView: View, blockId: String) -> Unit) {
        onImageBlockMenuRequested = callback
    }

    fun setOnImageBlockReplaceRequested(callback: (blockId: String) -> Unit) {
        onImageBlockReplaceRequested = callback
    }

    /** 供外部能力（如裁剪）获取指定图片块当前显示的源 Uri。优先 localUri，其次 remoteUrl。 */
    fun getImageBlockSourceUri(blockId: String): Uri? {
        val block = blockList.findBlock(blockId) as? EditorBlock.ImageBlock ?: return null
        val url = block.localUri ?: block.remoteUrl ?: return null
        return try {
            Uri.parse(url)
        } catch (_: Exception) {
            null
        }
    }

    /** 当前获得焦点的 Block 的 EditText，供工具栏加粗/下划线等使用。 */
    fun getFocusedEditText(): EditText? {
        val id = focusManager.getLastFocusedBlockId() ?: return null
        val pos = blockList.getBlockPosition(id)
        if (pos < 0) return null
        return (recyclerView.findViewHolderForAdapterPosition(adapterPosOfDataPos(pos)) as? TextBlockViewHolder)
            ?.blockView?.editText
    }

    // 切换引用块样式，刷新相邻块以实现无缝连接
    fun toggleQuote(position: Int) {
        val blocks = blockList.getBlocks()
        val block = blocks.getOrNull(position) as? EditorBlock.TextBlock ?: return
        block.isQuote = !block.isQuote
        // 相邻块需刷新以更新 prevIsQuote/nextIsQuote，实现连续引用块无缝连接
        if (position > 0) adapter.notifyItemChanged(position)
        adapter.notifyItemChanged(position + 1)
        if (position + 1 < blocks.size) adapter.notifyItemChanged(position + 2)
    }

    /** 切换当前焦点块的引用样式。 */
    fun toggleQuoteForFocusedBlock() {
        val id = focusManager.getLastFocusedBlockId() ?: return
        val pos = blockList.getBlockPosition(id)
        if (pos >= 0) toggleQuote(pos)
    }

    // region adapter 事件链语义（从原 BlockEditorRecyclerView 迁移）

    // Enter 键处理：URL 行转 LinkCard
    private fun handleEnterRequested(blockId: String, cursorPos: Int): Boolean {
        Log.d("URLEnter", "[handleEnterRequested] blockId=$blockId, cursorPos=$cursorPos")
        val result = actionManager.handleEnterRequested(blockId, cursorPos)
        Log.d("URLEnter", "[handleEnterRequested] actionManager returned: $result")
        if (result != null && result.focusTargetDataPos != null) {
            selectionManager.focusAfterUrlLineConvertedToLinkCard(
                focusTargetDataPos = result.focusTargetDataPos,
                focusSelection = result.focusSelection
            )
        }
        return result != null
    }

    // 行内链接点击，弹出菜单或打开
    private fun handleInlineLinkClicked(blockId: String, url: String, start: Int, end: Int) {
        if (editorCallback?.onInlineLinkClicked(blockId, url, start, end) == true) return
        val pos = blockList.getBlockPosition(blockId)
        if (pos < 0) return
        val vh = recyclerView.findViewHolderForAdapterPosition(pos + 1) as? TextBlockViewHolder ?: return
        val et = vh.blockView.editText
        val editable = et.text ?: return
        linkUiActions.onInlineLinkClicked(
            context = context,
            anchorEditText = et,
            blockId = blockId,
            url = url,
            start = start,
            end = end,
            editable = editable
        )
    }

    // LinkCard 点击与菜单展示
    private fun handleLinkCardClicked(position: Int) {
        if (position <= 0) return // 0 为标题
        selectionManager.onLinkCardClicked(position)

        val vh = recyclerView.findViewHolderForAdapterPosition(position) as? LinkCardViewHolder ?: return
        val dataPos = dataPosOfAdapterPos(position)
        val block = blockList.getBlocks().getOrNull(dataPos) as? EditorBlock.LinkCard ?: return
        showLinkCardMenu(anchor = vh.itemView, block = block)
    }

    private fun showLinkCardMenu(anchor: View, block: EditorBlock.LinkCard) {
        linkUiActions.showLinkCardMenu(
            context = context,
            anchor = anchor,
            block = block
        )
    }

    // 删除 LinkCard 并恢复焦点
    private fun handleLinkCardDelete(blockId: String) {
        val dataPos = blockList.getBlockPosition(blockId)
        if (dataPos < 0) return
        removeBlock(blockId)
        selectionManager.onLinkCardDeletedAfterRemoval(dataPos)
    }

    // 视频块事件处理：点击、删除、重试上传
    private fun handleVideoBlockClicked(blockId: String) {
        editorCallback?.onVideoClicked(blockId)
        selectionManager.onVideoClicked(blockId)
    }

    private fun handleVideoBlockDelete(blockId: String) {
        val dataPos = blockList.getBlockPosition(blockId)
        if (dataPos < 0) return
        adapter.videoPlayerManager?.release(blockId)
        removeBlock(blockId)
        selectionManager.onVideoDeletedAfterRemoval(dataPos)
    }

    private fun handleVideoBlockRetry(blockId: String) {
        val block = blockList.findBlock(blockId) as? EditorBlock.VideoBlock ?: return
        val localUri = block.localUri ?: return
        block.uploadState = EditorBlockEntity.UploadState.UPLOADING
        block.uploadProgress = 0
        notifyVideoBlockChanged(blockId)

        adapter.videoUploadManager?.enqueueUpload(
            blockId,
            localUri,
            object : EditorVideoUploader.Callback {
                override fun onProgress(blockId: String, progress: Int) {
                    val b = blockList.findBlock(blockId) as? EditorBlock.VideoBlock ?: return
                    b.uploadProgress = progress
                    notifyVideoBlockChanged(blockId)
                }

                override fun onSuccess(blockId: String, remoteUrl: String) {
                    val b = blockList.findBlock(blockId) as? EditorBlock.VideoBlock ?: return
                    b.remoteUrl = remoteUrl
                    b.uploadState = EditorBlockEntity.UploadState.SUCCESS
                    b.uploadProgress = 100
                    notifyVideoBlockChanged(blockId)
                }

                override fun onError(blockId: String, throwable: Throwable) {
                    val b = blockList.findBlock(blockId) as? EditorBlock.VideoBlock ?: return
                    b.uploadState = EditorBlockEntity.UploadState.FAILED
                    notifyVideoBlockChanged(blockId)
                }
            }
        )
    }

    /** 刷新视频块 UI（上传/播放状态变更）。 */
    fun notifyVideoBlockChanged(blockId: String) {
        val pos = blockList.getBlockPosition(blockId)
        if (pos >= 0) adapter.notifyItemChanged(pos + 1)
    }

    // 图片/HR/Code 删除处理，委托给 Manager 统一逻辑
    private fun handleImageBlockDelete(blockId: String) {
        val result = actionManager.handleImageBlockDeleteStructure(blockId) ?: return
        selectionManager.focusAfterImageDeletion(
            targetDataPos = result.focusTargetDataPos,
            textSelection = result.textSelection
        )
    }

    private fun handleImageBlockPreview(blockId: String) {
        val block = blockList.findBlock(blockId) as? EditorBlock.ImageBlock
        val url = block?.remoteUrl ?: block?.localUri
        if (url != null) {
            editorCallback?.onImagePreviewRequested(url)
            onPreviewRequested?.invoke(url)
        }
    }

    private fun handleHrBlockDelete(position: Int) {
        if (position <= 0) return
        val dataPos = dataPosOfAdapterPos(position)
        val blocks = blockList.getBlocks()
        val block = blocks.getOrNull(dataPos) as? EditorBlock.HrBlock ?: return
        val removed = removeBlock(block.id)
        if (!removed) return
        selectionManager.onHrDeletedAfterRemoval(dataPos)
    }

    private fun handleCodeBlockClicked(position: Int) {
        if (position <= 0) return
        Log.d("CodeClick", "[handleCodeBlockClicked] position=$position")
        selectionManager.onCodeClicked(position)
        val dataPos = dataPosOfAdapterPos(position)
        val block = blockList.getBlocks().getOrNull(dataPos) as? EditorBlock.CodeBlock ?: return
        focusManager.scrollToBlock(block.id)
        Log.d("CodeClick", "[handleCodeBlockClicked] completed, blockId=${block.id}")
    }

    private fun handleCodeBlockDelete(position: Int) {
        if (position <= 0) return
        val dataPos = dataPosOfAdapterPos(position)
        val blocks = blockList.getBlocks()
        val block = blocks.getOrNull(dataPos) as? EditorBlock.CodeBlock ?: return
        val removed = removeBlock(block.id)
        if (!removed) return
        selectionManager.onCodeDeletedAfterRemoval(dataPos)
    }

    // endregion

    // region 对外 API：blocks/标题/焦点

    fun getBlocks(): List<EditorBlockEntity> {
        return BlockDocumentConverter.toEditorBlockEntities(blockList.getBlocks())
    }

    fun getTitleText(): String = titleText

    fun setTitleText(text: String) {
        Log.d("TitleDebug", "[setTitleText] text=[$text], oldTitle=[$titleText]")
        titleText = text
        adapter.titleText = text
        adapter.notifyItemChanged(0)
    }

    fun setBlocks(richBlocks: List<EditorBlockEntity>) {
        Log.d("TitleDebug", "[setBlocks] 开始恢复，blocks=${richBlocks.size}")
        val newBlocks = BlockDocumentConverter.toEditorBlocks(richBlocks)
        val oldSize = blockList.getBlocks().size
        blockList.replaceAll(newBlocks)
        if (oldSize > 0) adapter.notifyItemRangeRemoved(1, oldSize)
        if (newBlocks.isNotEmpty()) adapter.notifyItemRangeInserted(1, newBlocks.size)
        ensureOneBlockIfEmpty()
    }

    fun focusBlockAt(position: Int) {
        focusManager.focusBlockAtDataPos(position)
    }

    fun focusFirstTextBlock() {
        focusManager.focusFirstTextBlock()
    }

    // endregion

    // region 对外 API：插入/切换

    // 返回当前获得焦点的 TextBlock 以及其光标位置。
    fun getFocusedTextCursorInfo(): Pair<String, Int>? {
        val id = focusManager.getLastFocusedBlockId() ?: return null
        val pos = blockList.getBlockPosition(id)
        if (pos < 0) return null
        val vh = recyclerView.findViewHolderForAdapterPosition(pos + 1) as? TextBlockViewHolder ?: return null
        val editText = vh.blockView.editText
        return id to editText.selectionStart
    }

    /** 获取用于插入的锚点 blockId：当前焦点 TextBlock，若无则最后一个 TextBlock，若无则第一个块。 */
    fun getAnchorBlockIdForInsert(): String {
        val blocks = blockList.getBlocks()
        val focusedId = focusManager.getLastFocusedBlockId()
        if (focusedId != null) {
            val block = blockList.findBlock(focusedId)
            if (block != null) return block.id
        }
        val lastText = blocks.lastOrNull { it is EditorBlock.TextBlock }
        if (lastText != null) return lastText.id
        return blocks.firstOrNull()?.id ?: ""
    }

    fun insertCodeBlock(language: String) {
        val cursorInfo = getFocusedTextCursorInfo()
        if (cursorInfo != null) {
            val (currentBlockId, cursorPosition) = cursorInfo
            val result = actionManager.insertCodeBlockAtCursor(
                textBlockId = currentBlockId,
                cursorPos = cursorPosition,
                language = language
            ) ?: return
            selectionManager.setCodeSelectedPositionSilently(result.codeAdapterPos)
            selectionManager.focusAfterCodeInsert(result.codeAdapterPos)
            return
        }

        val anchorId = getAnchorBlockIdForInsert()
        if (anchorId.isEmpty()) return
        val result = actionManager.insertCodeBlockAfterAnchor(
            anchorTextBlockId = anchorId,
            language = language
        ) ?: return
        selectionManager.setCodeSelectedPositionSilently(result.codeAdapterPos)
        selectionManager.focusAfterCodeInsert(result.codeAdapterPos)
    }

    /** 在指定块之后插入多个 ImageBlock，供相册/拍照/粘贴 URL 使用。 */
    fun addImageBlocksAfter(anchorBlockId: String, blocks: List<EditorBlock.ImageBlock>) {
        actionManager.addImageBlocksAfter(anchorBlockId, blocks)
    }

    /** 在指定块之后插入多个 VideoBlock。 */
    fun addVideoBlocksAfter(anchorBlockId: String, blocks: List<EditorBlock.VideoBlock>) {
        actionManager.addVideoBlocksAfter(anchorBlockId, blocks)
    }

    /**
     * 在当前光标位置插入视频块，并在视频后自动插入新的 TextBlock。
     */
    fun insertVideoAtCursor(
        videoBlock: EditorBlock.VideoBlock,
        currentBlockId: String,
        cursorPosition: Int
    ): String? {
        val result = actionManager.insertVideoAtCursor(
            textBlockId = currentBlockId,
            cursorPos = cursorPosition,
            videoBlock = videoBlock
        ) ?: return null

        val focus = result.focus
        val trailingPos = focus.focusTargetDataPos ?: return result.trailingTextBlockId
        selectionManager.focusAfterTrailingTextInsert(
            trailingTextDataPos = trailingPos,
            trailingSelection = focus.focusSelection
        )
        return result.trailingTextBlockId
    }

    fun insertImageAtCursor(uri: Uri, currentBlockId: String, cursorPosition: Int): String? {
        val result = actionManager.insertImageAtCursor(
            textBlockId = currentBlockId,
            cursorPos = cursorPosition,
            localUri = uri.toString()
        ) ?: return null

        val focus = result.focus
        val trailingPos = focus.focusTargetDataPos ?: return result.trailingTextBlockId
        selectionManager.focusAfterTrailingTextInsert(
            trailingTextDataPos = trailingPos,
            trailingSelection = focus.focusSelection
        )
        return result.trailingTextBlockId
    }

    fun insertHorizontalRule() {
        val cursorInfo = getFocusedTextCursorInfo()
        val focus = if (cursorInfo != null) {
            val (currentBlockId, cursorPosition) = cursorInfo
            actionManager.insertHorizontalRuleAtCursor(
                textBlockId = currentBlockId,
                cursorPos = cursorPosition
            )
        } else {
            val anchorId = getAnchorBlockIdForInsert()
            if (anchorId.isEmpty()) null else actionManager.insertHorizontalRuleAfterAnchor(anchorId)
        } ?: return

        val trailingPos = focus.focusTargetDataPos ?: return
        selectionManager.focusAfterTrailingTextInsert(
            trailingTextDataPos = trailingPos,
            trailingSelection = focus.focusSelection
        )
    }

    fun toggleHeadingForFocusedBlock() {
        val id = focusManager.getLastFocusedBlockId() ?: return
        val pos = blockList.getBlockPosition(id)
        if (pos < 0) return
        val blocks = blockList.getBlocks()
        val block = blocks.getOrNull(pos) as? EditorBlock.TextBlock ?: return

        val newHeading = !block.isHeading
        block.isHeading = newHeading

        if (newHeading && block.listType != null) {
            block.listType = null
            block.orderIndex = 0
            actionManager.recalculateNumberListOrderIndexes()
        }

        adapter.notifyItemChanged(pos + 1)
        notifyContentChanged()
    }

    fun toggleListForFocusedBlock(targetType: EditorBlock.ListType?) {
        val id = focusManager.getLastFocusedBlockId() ?: return
        val pos = blockList.getBlockPosition(id)
        if (pos < 0) return
        val blocks = blockList.getBlocks()
        val block = blocks.getOrNull(pos) as? EditorBlock.TextBlock ?: return

        val current = block.listType
        val newType = when {
            targetType == null -> null
            current == targetType -> null
            else -> targetType
        }

        block.listType = newType
        block.orderIndex = if (newType == EditorBlock.ListType.NUMBER_LIST) block.orderIndex else 0

        adapter.notifyItemChanged(pos + 1)
        actionManager.recalculateNumberListOrderIndexes()
        notifyContentChanged()
    }

    fun showCaptionForImageBlock(blockId: String) {
        val block = blockList.findBlock(blockId) as? EditorBlock.ImageBlock ?: return
        block.showCaption = true
        block.isCaptionEditing = true
        notifyImageBlockChanged(blockId)

        val pos = blockList.getBlockPosition(blockId)
        if (pos < 0) return
        val adapterPos = pos + 1

        uiSideEffects.post {
            val ok = uiSideEffects.requestFocusImageCaptionEndByAdapterPos(adapterPos)
            if (!ok) {
                uiSideEffects.postDelayed(100L) {
                    uiSideEffects.requestFocusImageCaptionEndByAdapterPos(adapterPos)
                }
            }
        }
        notifyContentChanged()
    }

    // endregion

    // region 对外 API：更新/删除

    fun notifyImageBlockChanged(blockId: String) {
        val pos = blockList.getBlockPosition(blockId)
        if (pos >= 0) adapter.notifyItemChanged(pos + 1)
    }

    fun removeBlock(blockId: String): Boolean {
        val pos = blockList.getBlockPosition(blockId)
        if (pos < 0) return false
        val removed = blockList.removeBlock(blockId)
        if (removed) {
            adapter.notifyItemRemoved(pos + 1)
            actionManager.recalculateNumberListOrderIndexes()
            notifyContentChanged()
        }
        return removed
    }

    fun updateImageBlock(blockId: String, localUri: String?, remoteUrl: String?) {
        val block = blockList.findBlock(blockId) as? EditorBlock.ImageBlock ?: return
        block.localUri = localUri
        block.remoteUrl = remoteUrl
        block.uploadState = EditorBlockEntity.UploadState.SUCCESS
        notifyImageBlockChanged(blockId)
        notifyContentChanged()
    }

    fun updateVideoBlockProgress(blockId: String, progress: Int) {
        val block = blockList.findBlock(blockId) as? EditorBlock.VideoBlock ?: return
        block.uploadState = EditorBlockEntity.UploadState.UPLOADING
        block.uploadProgress = progress
        notifyVideoBlockChangedInternal(blockId)
    }

    fun updateVideoBlockRemoteUrl(blockId: String, remoteUrl: String) {
        val block = blockList.findBlock(blockId) as? EditorBlock.VideoBlock ?: return
        block.remoteUrl = remoteUrl
        block.uploadState = EditorBlockEntity.UploadState.SUCCESS
        block.uploadProgress = 100
        notifyVideoBlockChangedInternal(blockId)
    }

    fun updateVideoBlockFailed(blockId: String) {
        val block = blockList.findBlock(blockId) as? EditorBlock.VideoBlock ?: return
        block.uploadState = EditorBlockEntity.UploadState.FAILED
        notifyVideoBlockChangedInternal(blockId)
    }

    private fun notifyVideoBlockChangedInternal(blockId: String) {
        val pos = blockList.getBlockPosition(blockId)
        if (pos >= 0) adapter.notifyItemChanged(pos + 1)
    }

    // endregion

    // region ai_assist

    private fun setFocusedTextBlockForAi(blockId: String?) {
        val old = adapter.focusedTextBlockId
        if (old == blockId) return
        adapter.focusedTextBlockId = blockId
        old?.let { notifyAiAssistForBlock(it) }
        blockId?.let { notifyAiAssistForBlock(it) }
        editorCallback?.onTextBlockFocusChanged(blockId)
    }

    fun notifyAiAssistForBlock(blockId: String) {
        val pos = blockList.getBlockPosition(blockId)
        if (pos >= 0) {
            adapter.notifyItemChanged(adapterPosOfDataPos(pos), EditorAdapter.PAYLOAD_AI_ASSIST)
        }
    }

    fun syncAiAssistStates(map: Map<String, AiAssistUiState>) {
        adapter.aiAssistStates = map.toMap()
    }

    fun configureAiAssistCallbacks(
        onSparkle: (String) -> Unit,
        onAccept: (String) -> Unit,
        onRetry: (String) -> Unit,
        onDiscard: (String) -> Unit,
    ) {
        onAiSparkle = onSparkle
        onAiAccept = onAccept
        onAiRetry = onRetry
        onAiDiscard = onDiscard
    }

    /**
     * 用户确认采用 AI 预览：仅此处写回 [EditorBlock.TextBlock.text]；首版纯文本替换，不保留 Span。
     */
    fun applyAiPolishToTextBlock(blockId: String, plainText: String) {
        val block = blockList.findBlock(blockId) as? EditorBlock.TextBlock ?: return
        block.text.clear()
        block.text.append(plainText)
        val pos = blockList.getBlockPosition(blockId)
        if (pos >= 0) {
            adapter.notifyItemChanged(adapterPosOfDataPos(pos))
        }
        notifyContentChanged()
    }

    // endregion ai_assist

    fun clearImageSelection() {
        selectionManager.clearImageSelection()
    }

    // 调试方法：如需保留，可迁移到独立 logger
    private fun debugPrintBlocks(tag: String) {
        // 原文件保留默认不触发
        // 如需启用，可在对应位置手动调用
        val blocks = blockList.getBlocks()
        Log.d("BlockEditor", "=== $tag === blocks=${blocks.size}")
    }
}

