package com.fishmemory.app.ui.publish.richtext.ui.container

import android.content.Context
import android.graphics.Rect
import android.net.Uri
import android.text.SpannableStringBuilder
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fishmemory.app.R
import com.fishmemory.app.ui.publish.richtext.business.media.VideoUploadManager
import com.fishmemory.app.ui.publish.richtext.business.format.BlockActionManager
import com.fishmemory.app.ui.publish.richtext.business.selection.BlockEditorUiSideEffects
import com.fishmemory.app.ui.publish.richtext.business.selection.FocusManager
import com.fishmemory.app.ui.publish.richtext.business.selection.OperationFocusResult
import com.fishmemory.app.ui.publish.richtext.business.selection.SelectionManager
import com.fishmemory.app.ui.publish.richtext.core.model.BlockIdGenerator
import com.fishmemory.app.ui.publish.richtext.core.model.EditorBlockList
import com.fishmemory.app.ui.publish.richtext.core.converter.BlockDocumentConverter
import com.fishmemory.app.ui.publish.richtext.core.model.EditorBlock
import com.fishmemory.app.ui.publish.richtext.core.model.EditorBlockEntity
import com.fishmemory.app.ui.publish.richtext.ui.adapter.EditorAdapter
import com.fishmemory.app.ui.publish.richtext.ui.adapter.CodeBlockViewHolder
import com.fishmemory.app.ui.publish.richtext.ui.adapter.ImageBlockViewHolder
import com.fishmemory.app.ui.publish.richtext.ui.adapter.LinkCardViewHolder
import com.fishmemory.app.ui.publish.richtext.ui.adapter.TextBlockViewHolder
import com.fishmemory.app.ui.publish.richtext.ui.actions.LinkUiActions
import com.fishmemory.app.ui.publish.richtext.business.media.VideoPlayerManager
import com.yalantis.ucrop.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * Block 编辑器：RecyclerView + EditorBlockList + EditorAdapter，负责分裂/合并回调和焦点滚动。
 * 对外提供 getBlocks/setBlocks 对接 RichDocument，focusBlockAt 供标题回车跳转。
 */
class BlockEditorRecyclerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : RecyclerView(context, attrs) {

    val blockList = EditorBlockList()
    private val adapter = EditorAdapter(blockList)

    /** 标题文本仅用于编辑态显示与 RichDocument 同步。 */
    private var titleText: String = ""

    private val uiScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var onContentChanged: (() -> Unit)? = null

    private fun notifyContentChanged() {
        onContentChanged?.invoke()
    }

    private fun adapterPosOfDataPos(dataPos: Int): Int = dataPos + 1
    private fun dataPosOfAdapterPos(adapterPos: Int): Int = adapterPos - 1

    private val uiSideEffects = object : BlockEditorUiSideEffects {
        override fun notifyItemChanged(adapterPos: Int) {
            adapter.notifyItemChanged(adapterPos)
        }

        override fun notifyItemInserted(adapterPos: Int) {
            adapter.notifyItemInserted(adapterPos)
        }

        override fun notifyItemRemoved(adapterPos: Int) {
            adapter.notifyItemRemoved(adapterPos)
        }

        override fun notifyItemRangeInserted(startAdapterPos: Int, itemCount: Int) {
            adapter.notifyItemRangeInserted(startAdapterPos, itemCount)
        }

        override fun notifyItemRangeChanged(startAdapterPos: Int, itemCount: Int) {
            adapter.notifyItemRangeChanged(startAdapterPos, itemCount)
        }

        override fun notifyDataSetChanged() {
            adapter.notifyDataSetChanged()
        }

        override fun scrollToAdapterPos(adapterPos: Int) {
            scrollToPosition(adapterPos)
        }

        override fun ensureBlockVisibleByAdapterPos(adapterPos: Int) {
            fun requestVisible() {
                val vh = findViewHolderForAdapterPosition(adapterPos) ?: return
                val item = vh.itemView ?: return
                val rect = Rect(0, 0, item.width, item.height)
                item.requestRectangleOnScreen(rect, true)
            }

            post {
                val vh = findViewHolderForAdapterPosition(adapterPos)
                if (vh != null) {
                    requestVisible()
                } else {
                    scrollToPosition(adapterPos)
                    post { requestVisible() }
                }
            }
        }

        override fun post(action: () -> Unit) {
            this@BlockEditorRecyclerView.post(action)
        }

        override fun postDelayed(delayMs: Long, action: () -> Unit) {
            this@BlockEditorRecyclerView.postDelayed({ action() }, delayMs)
        }

        override fun tryFocusTextBlockByAdapterPos(adapterPos: Int, selection: Int): Boolean {
            val vh = findViewHolderForAdapterPosition(adapterPos) ?: return false
            val editText = (vh as? TextBlockViewHolder)?.blockView?.editText ?: return false
            val safeSelection = selection.coerceAtLeast(0).coerceAtMost(editText.text?.length ?: 0)
            editText.requestFocus()
            editText.setSelection(safeSelection)
            return true
        }

        override fun tryFocusCodeBlockByAdapterPos(adapterPos: Int): Boolean {
            val vh = findViewHolderForAdapterPosition(adapterPos) ?: return false
            val codeVh = vh as? CodeBlockViewHolder ?: return false
            codeVh.requestFocusForEdit()
            return true
        }

        override fun requestFocusTextBlockByAdapterPos(adapterPos: Int): Boolean {
            val vh = findViewHolderForAdapterPosition(adapterPos) ?: return false
            val editText = (vh as? TextBlockViewHolder)?.blockView?.editText ?: return false
            editText.requestFocus()
            return true
        }

        override fun setSelectedHrPosition(position: Int) {
            adapter.selectedHrPosition = position
        }

        override fun setSelectedCodePosition(position: Int) {
            adapter.selectedCodePosition = position
        }

        override fun setSelectedLinkCardPosition(position: Int) {
            adapter.selectedLinkCardPosition = position
        }

        override fun setSelectedVideoBlockId(blockId: String?) {
            adapter.selectedVideoBlockId = blockId
        }

        override fun requestFocusImageCaptionByAdapterPos(adapterPos: Int): Boolean {
            val vh = findViewHolderForAdapterPosition(adapterPos) ?: return false
            val imageVh = vh as? ImageBlockViewHolder ?: return false
            val captionEt = imageVh.itemView.findViewById<EditText>(R.id.etCaption)
            captionEt.requestFocus()
            return true
        }

        override fun requestFocusImageCaptionEndByAdapterPos(adapterPos: Int): Boolean {
            val vh = findViewHolderForAdapterPosition(adapterPos) ?: return false
            val imageVh = vh as? ImageBlockViewHolder ?: return false
            val captionEt = imageVh.itemView.findViewById<EditText>(R.id.etCaption)
            captionEt.isEnabled = true
            captionEt.isFocusableInTouchMode = true
            captionEt.isFocusable = true
            captionEt.isCursorVisible = true
            captionEt.requestFocus()
            val end = captionEt.text?.length ?: 0
            captionEt.setSelection(end)
            return true
        }

        override fun notifyContentChanged() {
            // 复用 BlockEditorRecyclerView 内部的 dirty/auto-save 回调
            this@BlockEditorRecyclerView.notifyContentChanged()
        }
    }

    private val actionManager = BlockActionManager(blockList, uiSideEffects, uiScope)
    private val selectionManager = SelectionManager(blockList, uiSideEffects, actionManager)
    private val focusManager = FocusManager(blockList, uiSideEffects)
    private val linkUiActions = LinkUiActions(
        actionManager = actionManager,
        selectionManager = selectionManager,
        onShowToast = { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        },
        // 当前实现中 LinkUiActions 内部并未直接使用 onOpenUrl；先用空实现补齐签名。
        onOpenUrl = { _ -> }
    )

    init {
        layoutManager = LinearLayoutManager(context)
        setAdapter(adapter)
        setupBlockCallbacks()
        ensureOneBlockIfEmpty()
    }

    private var onPreviewRequested: ((url: String) -> Unit)? = null
    private var onImageBlockMenuRequested: ((anchorView: View, blockId: String) -> Unit)? = null

    private fun setupBlockCallbacks() {
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
        adapter.onLinkClicked = { blockId, url, start, end -> handleInlineLinkClicked(blockId, url, start, end) }
        adapter.onFocusGained = { blockId ->
            // 文本块获得焦点时，清空结构块选中态，避免视觉干扰
            selectionManager.clearHrSelection()
            selectionManager.clearCodeSelection()
            selectionManager.clearLinkCardSelection()
            selectionManager.clearVideoSelection()
            focusManager.onTextBlockFocusGained(blockId)
        }
        adapter.onImageBlockDeleteRequested = { blockId ->
            handleImageBlockDelete(blockId)
        }
        adapter.onImageBlockPreviewRequested = { blockId ->
            handleImageBlockPreview(blockId)
        }
        adapter.onImageBlockChanged = { blockId ->
            notifyImageBlockChanged(blockId)
            onContentChanged?.invoke()
        }
        adapter.onImageBlockMenuRequested = { anchorView, blockId ->
            selectionManager.selectImageBlock(blockId)
            onImageBlockMenuRequested?.invoke(anchorView, blockId)
        }
        adapter.onHrBlockClicked = { position -> selectionManager.onHrClicked(position) }
        adapter.onHrBlockDeleteRequested = { position -> handleHrBlockDelete(position) }
        adapter.onCodeBlockClicked = { position -> handleCodeBlockClicked(position) }
        adapter.onCodeBlockDeleteRequested = { position -> handleCodeBlockDelete(position) }
        adapter.onCodeBlockFocusGained = { blockId ->
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
            onContentChanged?.invoke()
        }
        adapter.onTitleNextRequested = { focusFirstTextBlock() }
        adapter.onContentChanged = { onContentChanged?.invoke() }
    }

    fun setOnContentChangedListener(listener: (() -> Unit)?) {
        onContentChanged = listener
    }


    fun setVideoManagers(
        playerManager: VideoPlayerManager?,
        uploadManager: VideoUploadManager?
    ) {
        adapter.videoPlayerManager = playerManager
        adapter.videoUploadManager = uploadManager
    }

    fun setOnImageBlockPreviewRequested(callback: (url: String) -> Unit) {
        onPreviewRequested = callback
    }

    fun setOnImageBlockMenuRequested(callback: (anchorView: View, blockId: String) -> Unit) {
        onImageBlockMenuRequested = callback
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
        return (findViewHolderForAdapterPosition(adapterPosOfDataPos(pos)) as? TextBlockViewHolder)?.blockView?.editText
    }

    private fun handleSplit(blockId: String, cursorPos: Int): OperationFocusResult? {
        return actionManager.handleSplit(blockId, cursorPos)
    }

    private fun handleMerge(blockId: String): OperationFocusResult? {
        return actionManager.handleMerge(blockId)
    }


    private fun handleBackspaceAtStart(blockId: String): OperationFocusResult? {
        return actionManager.handleBackspaceAtStart(blockId)
    }
    fun toggleQuote(position: Int) {
        val blocks = blockList.getBlocks()
        val block = blocks.getOrNull(position) as? EditorBlock.TextBlock ?: return
        block.isQuote = !block.isQuote
        // 相邻块需刷新以更新 prevIsQuote/nextIsQuote，实现连续引用块无缝连接
        if (position > 0) adapter.notifyItemChanged(position)
        adapter.notifyItemChanged(position + 1)
        if (position + 1 < blocks.size) adapter.notifyItemChanged(position + 2)
    }

    fun toggleQuoteForFocusedBlock() {
        val id = focusManager.getLastFocusedBlockId() ?: return
        val pos = blockList.getBlockPosition(id)
        if (pos >= 0) {
            toggleQuote(pos)
        }
    }
    private fun handleEnterRequested(blockId: String, cursorPos: Int): Boolean {
        // 当前行仅 URL → 转为 LinkCard 块（消费 Enter）
        val result = actionManager.handleEnterRequested(blockId, cursorPos)
        if (result != null && result.focusTargetDataPos != null) {
            selectionManager.focusAfterUrlLineConvertedToLinkCard(
                focusTargetDataPos = result.focusTargetDataPos,
                focusSelection = result.focusSelection
            )
        }
        return result != null
    }

    private fun handleInlineLinkClicked(blockId: String, url: String, start: Int, end: Int) {
        val pos = blockList.getBlockPosition(blockId)
        if (pos < 0) return
        val vh = findViewHolderForAdapterPosition(pos + 1) as? TextBlockViewHolder ?: return
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

    private fun handleLinkCardClicked(position: Int) {
        if (position <= 0) return // 0 为标题
        selectionManager.onLinkCardClicked(position)

        val vh = findViewHolderForAdapterPosition(position) as? LinkCardViewHolder ?: return
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

    private fun convertLinkCardToText(cardId: String) {
        val result = actionManager.convertLinkCardToTextStructure(cardId) ?: return
        selectionManager.focusAfterLinkCardConvertedToText(
            textDataPos = result.textDataPos,
            selection = result.selection
        )
    }


// ... existing code ...

// ... existing code ...


    private fun handleLinkCardDelete(blockId: String) {
        val dataPos = blockList.getBlockPosition(blockId)
        if (dataPos < 0) return
        removeBlock(blockId)
        selectionManager.onLinkCardDeletedAfterRemoval(dataPos)
    }

    private fun handleVideoBlockClicked(blockId: String) {
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
        adapter.videoUploadManager?.enqueueUpload(blockId, localUri, object : VideoUploadManager.UploadCallback {
            override fun onProgress(id: String, progress: Int) {
                val b = blockList.findBlock(id) as? EditorBlock.VideoBlock ?: return
                b.uploadProgress = progress
                notifyVideoBlockChanged(id)
            }
            override fun onSuccess(id: String, remoteUrl: String) {
                val b = blockList.findBlock(id) as? EditorBlock.VideoBlock ?: return
                b.remoteUrl = remoteUrl
                b.uploadState = EditorBlockEntity.UploadState.SUCCESS
                b.uploadProgress = 100
                notifyVideoBlockChanged(id)
            }
            override fun onError(id: String, throwable: Throwable) {
                val b = blockList.findBlock(id) as? EditorBlock.VideoBlock ?: return
                b.uploadState = EditorBlockEntity.UploadState.FAILED
                notifyVideoBlockChanged(id)
            }
        })
    }

    fun notifyVideoBlockChanged(blockId: String) {
        val pos = blockList.getBlockPosition(blockId)
        if (pos >= 0) adapter.notifyItemChanged(pos + 1)
    }

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
        if (url != null) onPreviewRequested?.invoke(url)
    }

    private fun handleImageBlockSelection(blockId: String) {
        selectionManager.selectImageBlock(blockId)
    }

    /** HR Block 点击：切换选中位置，并刷新前一个与当前 Item 的选中 UI。 */
    private fun handleHrBlockClicked(position: Int) {
        selectionManager.onHrClicked(position)
    }

    /** HR Block 删除：移除块并把焦点交给相邻的 TextBlock，同时重置选中态。 */
    private fun handleHrBlockDelete(position: Int) {
        if (position <= 0) return
        val dataPos = dataPosOfAdapterPos(position)
        val blocks = blockList.getBlocks()
        val block = blocks.getOrNull(dataPos) as? EditorBlock.HrBlock ?: return
        val removed = removeBlock(block.id)
        if (!removed) return
        selectionManager.onHrDeletedAfterRemoval(dataPos)
    }

    /** Code Block 点击：切换选中位置，并刷新前一个与当前 Item 的选中 UI。 */
    private fun handleCodeBlockClicked(position: Int) {
        if (position <= 0) return
        selectionManager.onCodeClicked(position)
        val dataPos = dataPosOfAdapterPos(position)
        val block = blockList.getBlocks().getOrNull(dataPos) as? EditorBlock.CodeBlock ?: return
        focusManager.scrollToBlock(block.id)
    }

    /** Code Block 删除：移除块并把焦点交给相邻的 TextBlock，同时重置选中态。 */
    private fun handleCodeBlockDelete(position: Int) {
        if (position <= 0) return
        val dataPos = dataPosOfAdapterPos(position)
        val blocks = blockList.getBlocks()
        val block = blocks.getOrNull(dataPos) as? EditorBlock.CodeBlock ?: return
        val removed = removeBlock(block.id)
        if (!removed) return
        selectionManager.onCodeDeletedAfterRemoval(dataPos)
    }

    /** 点击空白区域时，清除图片选中态。 */
    fun clearImageSelection() {
        selectionManager.clearImageSelection()
    }

    /** 当文本块获得焦点或用户开始编辑时，可调用以清除 HR 选中态。 */
    private fun clearHrSelection() {
        selectionManager.clearHrSelection()
    }

    private fun clearCodeSelection() {
        selectionManager.clearCodeSelection()
    }

    // scrollToBlock 行为已迁移到 FocusManager（通过 BlockEditorUiSideEffects.ensureBlockVisibleByAdapterPos）

    fun getBlocks(): List<EditorBlockEntity> {
        return BlockDocumentConverter
            .toEditorBlockEntities(blockList.getBlocks())
    }

    fun getTitleText(): String {
        Log.d("TitleDebug", "[getTitleText] title=[$titleText]")
        return titleText
    }

    fun setTitleText(text: String) {
        Log.d("TitleDebug", "[setTitleText] text=[$text], oldTitle=[$titleText]")
        titleText = text
        adapter.titleText = text
        adapter.notifyItemChanged(0)
    }

    fun setBlocks(richBlocks: List<EditorBlockEntity>) {
        Log.d("TitleDebug", "[setBlocks] 开始恢复，blocks=${richBlocks.size}")
        val newBlocks = BlockDocumentConverter
            .toEditorBlocks(richBlocks)
        val oldSize = blockList.getBlocks().size
        blockList.replaceAll(newBlocks)
        if (oldSize > 0) adapter.notifyItemRangeRemoved(1, oldSize)
        if (newBlocks.isNotEmpty()) adapter.notifyItemRangeInserted(1, newBlocks.size)
        ensureOneBlockIfEmpty()
    }

    private fun ensureOneBlockIfEmpty() {
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

    fun focusBlockAt(position: Int) {
        focusManager.focusBlockAtDataPos(position)
    }

    /** 焦点到第一个 TextBlock，供标题回车跳转。 */
    fun focusFirstTextBlock() {
        focusManager.focusFirstTextBlock()
    }

    fun setImeBottomPadding(padding: Int) {
        setPadding(paddingLeft, paddingTop, paddingRight, padding)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (e.action == MotionEvent.ACTION_DOWN) {
            clearImageSelection()
            selectionManager.clearLinkCardSelection()
            selectionManager.clearVideoSelection()
        }
        return super.onTouchEvent(e)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        uiScope.cancel()
    }

    /**
     * 在当前光标位置插入代码块：
     * - 若当前聚焦在 TextBlock，则将其拆为「上半部分 + CodeBlock + 下半部分 TextBlock」；
     * - 若无有效聚焦信息，则在锚点 TextBlock 之后插入「CodeBlock + 新空 TextBlock」。
     */
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
     * @return 新插入的 TextBlock id，供后续插入使用；失败返回 null
     */
    fun insertVideoAtCursor(videoBlock: EditorBlock.VideoBlock, currentBlockId: String, cursorPosition: Int): String? {
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

    /** 返回当前获得焦点的 TextBlock 以及其光标位置。 */
    fun getFocusedTextCursorInfo(): Pair<String, Int>? {
        val id = focusManager.getLastFocusedBlockId() ?: return null
        val pos = blockList.getBlockPosition(id)
        if (pos < 0) return null
        val vh = findViewHolderForAdapterPosition(pos + 1) as? TextBlockViewHolder ?: return null
        val editText = vh.blockView.editText
        return id to editText.selectionStart
    }

    /**
     * 在当前文本块的光标位置插入图片，并在图片后自动插入一个新的 TextBlock。
     * 这对应「文字中间插入图片」的 Split 逻辑。
     * @return 新插入的图片块 id，供多图时在其后追加；失败返回 null
     */
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

    /**
     * 在当前光标位置插入水平分割线（HrBlock）：
     * - 若当前聚焦在某个 TextBlock，则将该块在光标处分裂为「上半部分 + HR + 下半部分文本块」；
     * - 若无法获取有效光标信息，则退化为在锚点 TextBlock 后插入「HR + 新空 TextBlock」。
     */
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



    /**
     * 将当前获得焦点的文本块在「普通正文 / 标题块」之间切换。
     * - 仅修改 EditorBlock.TextBlock.isHeading，不改动文本内容与 Span。
     * - 开启标题时，为避免视觉冲突，清除列表类型（listType/reset orderIndex）。
     */
    fun toggleHeadingForFocusedBlock() {
        val id = focusManager.getLastFocusedBlockId() ?: return
        val pos = blockList.getBlockPosition(id)
        if (pos < 0) return
        val blocks = blockList.getBlocks()
        val block = blocks.getOrNull(pos) as? EditorBlock.TextBlock ?: return

        val newHeading = !block.isHeading
        block.isHeading = newHeading

        // 标题块不参与列表逻辑，关闭列表状态并重排有序列表序号
        if (newHeading && block.listType != null) {
            block.listType = null
            block.orderIndex = 0
            actionManager.recalculateNumberListOrderIndexes()
        }

        adapter.notifyItemChanged(pos + 1)
        notifyContentChanged()
    }

    /**
     * 将当前获得焦点的文本块在「普通文本 / 列表」之间切换。
     * - targetType == BULLET_LIST / NUMBER_LIST：开启或切换到对应列表类型；再次点击同一类型则关闭列表（恢复普通文本）
     * - targetType == null：强制关闭列表，恢复普通文本
     * 切换时仅影响块级元数据，不改动文本内容和 Span。
     */
    fun toggleListForFocusedBlock(targetType: EditorBlock.ListType?) {
        val id = focusManager.getLastFocusedBlockId() ?: return
        val pos = blockList.getBlockPosition(id)
        if (pos < 0) return
        val blocks = blockList.getBlocks()
        val block = blocks.getOrNull(pos) as? EditorBlock.TextBlock ?: return

        val current = block.listType
        val newType = when {
            targetType == null -> null
            current == targetType -> null // 再次点击同一类型，视为关闭列表
            else -> targetType
        }

        block.listType = newType
        // 仅有序列表需要序号，其他情况清零
        block.orderIndex = if (newType == EditorBlock.ListType.NUMBER_LIST) block.orderIndex else 0

        adapter.notifyItemChanged(pos + 1)
        actionManager.recalculateNumberListOrderIndexes()
        notifyContentChanged()
    }

    /** 对外暴露：显示指定图片块的注释输入框并请求焦点。 */
    fun showCaptionForImageBlock(blockId: String) {
        val block = blockList.findBlock(blockId) as? EditorBlock.ImageBlock ?: return
        // 显示注释并切换到「可编辑」态
        block.showCaption = true
        block.isCaptionEditing = true
        notifyImageBlockChanged(blockId)
        val pos = blockList.getBlockPosition(blockId)
        if (pos < 0) return
        val adapterPos = pos + 1
        post {
            val ok = uiSideEffects.requestFocusImageCaptionEndByAdapterPos(adapterPos)
            if (!ok) {
                uiSideEffects.postDelayed(100L) {
                    uiSideEffects.requestFocusImageCaptionEndByAdapterPos(adapterPos)
                }
            }
        }
        notifyContentChanged()
    }

    /** 替换 ImageBlock 时回调，由 PublishActivity 设置并弹出选择器。 */
    fun setOnImageBlockReplaceRequested(callback: (blockId: String) -> Unit) {
        adapter.onImageBlockReplaceRequested = callback
    }

    /** 更新 ImageBlock（如 caption、alignment 变更）后通知刷新。 */
    fun notifyImageBlockChanged(blockId: String) {
        val pos = blockList.getBlockPosition(blockId)
        if (pos >= 0) adapter.notifyItemChanged(pos + 1)
    }

    /** 删除指定块。 */
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

    /** 替换 ImageBlock 的图片源（相册/拍照/URL 选择后调用）。 */
    fun updateImageBlock(blockId: String, localUri: String?, remoteUrl: String?) {
        val block = blockList.findBlock(blockId) as? EditorBlock.ImageBlock ?: return
        block.localUri = localUri
        block.remoteUrl = remoteUrl
        block.uploadState = EditorBlockEntity.UploadState.SUCCESS
        notifyImageBlockChanged(blockId)
        notifyContentChanged()
    }

    /** 更新 VideoBlock 上传进度，供 PublishActivity 视频选择后上传回调使用。 */
    fun updateVideoBlockProgress(blockId: String, progress: Int) {
        val block = blockList.findBlock(blockId) as? EditorBlock.VideoBlock ?: return
        block.uploadState = EditorBlockEntity.UploadState.UPLOADING
        block.uploadProgress = progress
        notifyVideoBlockChanged(blockId)
        // 上传进度属于高频状态，不进入撤销栈
    }

    /** 更新 VideoBlock 远程地址，上传成功时调用。 */
    fun updateVideoBlockRemoteUrl(blockId: String, remoteUrl: String) {
        val block = blockList.findBlock(blockId) as? EditorBlock.VideoBlock ?: return
        block.remoteUrl = remoteUrl
        block.uploadState = EditorBlockEntity.UploadState.SUCCESS
        block.uploadProgress = 100
        notifyVideoBlockChanged(blockId)
    }

    /** 更新 VideoBlock 为上传失败态。 */
    fun updateVideoBlockFailed(blockId: String) {
        val block = blockList.findBlock(blockId) as? EditorBlock.VideoBlock ?: return
        block.uploadState = EditorBlockEntity.UploadState.FAILED
        notifyVideoBlockChanged(blockId)
        // 失败状态属于上传过程，不进入撤销栈
    }

    // 添加调试方法，在需要时调用
    private fun debugPrintBlocks(tag: String) {
        if (!BuildConfig.DEBUG) return

        val blocks = blockList.getBlocks()
        Log.d("BlockEditor", "=== $tag ===")
        blocks.forEachIndexed { index, block ->
            when (block) {
                is EditorBlock.TextBlock -> {
                    val preview = block.text.toString().take(20).replace("\n", "\\n")
                    Log.d("BlockEditor", "[$index] TextBlock id=${block.id} text='$preview'")
                }
                is EditorBlock.LinkCard -> {
                    Log.d("BlockEditor", "[$index] LinkCard id=${block.id} url=${block.url}")
                }
                else -> {
                    Log.d("BlockEditor", "[$index] ${block::class.simpleName} id=${block.id}")
                }
            }
        }
        Log.d("BlockEditor", "================")
    }
}