package com.fishmemory.app.ui.publish.richtext.ui.container

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fishmemory.app.ui.publish.richtext.business.format.BlockActionManager
import com.fishmemory.app.ui.publish.richtext.business.media.VideoPlayerManager
import com.fishmemory.app.ui.publish.richtext.business.media.VideoUploadManager
import com.fishmemory.app.ui.publish.richtext.business.selection.FocusManager
import com.fishmemory.app.ui.publish.richtext.business.selection.SelectionManager
import com.fishmemory.app.ui.publish.richtext.core.model.EditorBlock
import com.fishmemory.app.ui.publish.richtext.core.model.EditorBlockEntity
import com.fishmemory.app.ui.publish.richtext.core.model.EditorBlockList
import com.fishmemory.app.ui.publish.richtext.business.selection.BlockEditorUiSideEffects
import com.fishmemory.app.ui.publish.richtext.ui.actions.LinkUiActions
import com.fishmemory.app.ui.publish.richtext.ui.adapter.EditorAdapter
import com.fishmemory.app.ui.publish.ai.AiAssistUiState
import com.fishmemory.app.ui.publish.richtext.ui.container.BlockEditorOperations
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * Block 编辑器：RecyclerView + EditorBlockList + EditorAdapter。
 *
 * 当前职责：
 * - Facade 门面：对外统一 API（全部委托给 `BlockEditorOperations`）
 * - UI 容器：RecyclerView 生命周期/触摸清空选中态
 */
class BlockEditorRecyclerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : RecyclerView(context, attrs) {

    val blockList = EditorBlockList()
    private val adapter = EditorAdapter(blockList)

    // 主线程协程作用域，管理异步 UI 操作生命周期（用于 ActionManager 内部 OGP 拉取等）
    private val uiScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var onContentChanged: (() -> Unit)? = null

    private fun notifyContentChanged() {
        onContentChanged?.invoke()
    }

    // UI 副作用实现（原匿名 object 迁移）
    private val uiSideEffects: BlockEditorUiSideEffects = BlockEditorUiSideEffectsImpl(
        recyclerView = this,
        adapter = adapter,
        notifyContentChangedDelegate = { notifyContentChanged() },
    )

    // 业务层
    private val actionManager = BlockActionManager(blockList, uiSideEffects, uiScope)
    private val selectionManager = SelectionManager(blockList, uiSideEffects, actionManager)
    private val focusManager = FocusManager(blockList, uiSideEffects)

    // 链接 UI 行为
    private val linkUiActions = LinkUiActions(
        actionManager = actionManager,
        selectionManager = selectionManager,
        onShowToast = { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        },
        onOpenUrl = { _ -> }
    )

    private val operations = BlockEditorOperations(
        recyclerView = this,
        context = context,
        blockList = blockList,
        adapter = adapter,
        uiSideEffects = uiSideEffects,
        actionManager = actionManager,
        selectionManager = selectionManager,
        focusManager = focusManager,
        linkUiActions = linkUiActions,
        notifyContentChanged = { notifyContentChanged() }
    )

    init {
        layoutManager = LinearLayoutManager(context)
        setAdapter(adapter)
        operations.bindAdapterCallbacks()
        operations.ensureOneBlockIfEmpty()
    }

    /** 设置内容变更监听，用于自动保存。 */
    fun setOnContentChangedListener(listener: (() -> Unit)?) {
        onContentChanged = listener
    }

    /** 注入视频管理器：播放器与上传器。 */
    fun setVideoManagers(
        playerManager: VideoPlayerManager?,
        uploadManager: VideoUploadManager?
    ) {
        adapter.videoPlayerManager = playerManager
        adapter.videoUploadManager = uploadManager
    }

    /** 注册图片预览回调，供外部打开大图查看。 */
    fun setOnImageBlockPreviewRequested(callback: (url: String) -> Unit) {
        operations.setOnImageBlockPreviewRequested(callback)
    }

    /** 注册图片菜单回调，供外部弹出长按菜单。 */
    fun setOnImageBlockMenuRequested(callback: (anchorView: View, blockId: String) -> Unit) {
        operations.setOnImageBlockMenuRequested(callback)
    }

    /** 供外部能力（如裁剪）获取指定图片块当前显示的源 Uri。优先 localUri，其次 remoteUrl。 */
    fun getImageBlockSourceUri(blockId: String): Uri? = operations.getImageBlockSourceUri(blockId)

    /** 当前获得焦点的 Block 的 EditText，供工具栏加粗/下划线等使用。 */
    fun getFocusedEditText(): EditText? = operations.getFocusedEditText()

    /** 切换引用块样式，刷新相邻块以实现无缝连接。 */
    fun toggleQuote(position: Int) = operations.toggleQuote(position)

    /** 切换当前焦点块的引用样式。 */
    fun toggleQuoteForFocusedBlock() = operations.toggleQuoteForFocusedBlock()

    /** 导出为 EditorBlockEntity 列表，用于持久化。 */
    fun getBlocks(): List<EditorBlockEntity> = operations.getBlocks()

    /** 获取标题文本，用于提交草稿。 */
    fun getTitleText(): String = operations.getTitleText()

    /** 设置标题文本，同步到标题 ViewHolder。 */
    fun setTitleText(text: String) = operations.setTitleText(text)

    /** 从 EditorBlockEntity 恢复编辑器状态，用于加载草稿。 */
    fun setBlocks(richBlocks: List<EditorBlockEntity>) = operations.setBlocks(richBlocks)

    /** 聚焦指定位置的块，用于初始化或跳转。 */
    fun focusBlockAt(position: Int) = operations.focusBlockAt(position)

    /** 焦点到第一个 TextBlock，供标题回车跳转。 */
    fun focusFirstTextBlock() = operations.focusFirstTextBlock()

    /** 设置底部 IME 留白，避免键盘遮挡。 */
    fun setImeBottomPadding(padding: Int) {
        setPadding(paddingLeft, paddingTop, paddingRight, padding)
    }

    // 触摸事件清除选中态，避免残留高亮
    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (e.action == MotionEvent.ACTION_DOWN) {
            operations.clearImageSelection()
            selectionManager.clearLinkCardSelection()
            selectionManager.clearVideoSelection()
        }
        return super.onTouchEvent(e)
    }

    // 窗口分离时取消协程，防止内存泄漏
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        uiScope.cancel()
    }

    /** 在当前光标位置插入代码块。 */
    fun insertCodeBlock(language: String) = operations.insertCodeBlock(language)

    /** 在指定块之后插入多个 ImageBlock。 */
    fun addImageBlocksAfter(anchorBlockId: String, blocks: List<EditorBlock.ImageBlock>) {
        operations.addImageBlocksAfter(anchorBlockId, blocks)
    }

    /** 在指定块之后插入多个 VideoBlock。 */
    fun addVideoBlocksAfter(anchorBlockId: String, blocks: List<EditorBlock.VideoBlock>) {
        operations.addVideoBlocksAfter(anchorBlockId, blocks)
    }

    /**
     * 在当前光标位置插入视频块，并在视频后自动插入新的 TextBlock。
     * @return 新插入的 TextBlock id，供后续插入使用；失败返回 null
     */
    fun insertVideoAtCursor(
        videoBlock: EditorBlock.VideoBlock,
        currentBlockId: String,
        cursorPosition: Int
    ): String? = operations.insertVideoAtCursor(videoBlock, currentBlockId, cursorPosition)

    fun getAnchorBlockIdForInsert(): String = operations.getAnchorBlockIdForInsert()

    fun getFocusedTextCursorInfo(): Pair<String, Int>? = operations.getFocusedTextCursorInfo()

    fun insertImageAtCursor(
        uri: Uri,
        currentBlockId: String,
        cursorPosition: Int
    ): String? = operations.insertImageAtCursor(uri, currentBlockId, cursorPosition)

    fun insertHorizontalRule() = operations.insertHorizontalRule()

    fun toggleHeadingForFocusedBlock() = operations.toggleHeadingForFocusedBlock()

    fun toggleListForFocusedBlock(targetType: EditorBlock.ListType?) =
        operations.toggleListForFocusedBlock(targetType)

    /** 对外暴露：显示指定图片块的注释输入框并请求焦点。 */
    fun showCaptionForImageBlock(blockId: String) = operations.showCaptionForImageBlock(blockId)

    /** 替换 ImageBlock 时回调，由 PublishActivity 设置并弹出选择器。 */
    fun setOnImageBlockReplaceRequested(callback: (blockId: String) -> Unit) =
        operations.setOnImageBlockReplaceRequested(callback)

    /** 更新 ImageBlock（如 caption、alignment 变更）后通知刷新。 */
    fun notifyImageBlockChanged(blockId: String) = operations.notifyImageBlockChanged(blockId)

    /** 删除指定块。 */
    fun removeBlock(blockId: String): Boolean = operations.removeBlock(blockId)

    /** 替换 ImageBlock 的图片源（相册/拍照/URL 选择后调用）。 */
    fun updateImageBlock(blockId: String, localUri: String?, remoteUrl: String?) =
        operations.updateImageBlock(blockId, localUri, remoteUrl)

    /** 更新 VideoBlock 上传进度，供 PublishActivity 视频选择后上传回调使用。 */
    fun updateVideoBlockProgress(blockId: String, progress: Int) =
        operations.updateVideoBlockProgress(blockId, progress)

    /** 更新 VideoBlock 远程地址，上传成功时调用。 */
    fun updateVideoBlockRemoteUrl(blockId: String, remoteUrl: String) =
        operations.updateVideoBlockRemoteUrl(blockId, remoteUrl)

    /** 更新 VideoBlock 为上传失败态。 */
    fun updateVideoBlockFailed(blockId: String) = operations.updateVideoBlockFailed(blockId)

    /** 点击空白区域时，清除图片选中态。 */
    fun clearImageSelection() = operations.clearImageSelection()

    /** 刷新视频块 UI（上传/播放状态变更）。 */
    fun notifyVideoBlockChanged(blockId: String) = operations.notifyVideoBlockChanged(blockId)

    fun syncAiAssistStates(map: Map<String, AiAssistUiState>) = operations.syncAiAssistStates(map)

    fun notifyAiAssistForBlock(blockId: String) = operations.notifyAiAssistForBlock(blockId)

    fun configureAiAssistCallbacks(
        onSparkle: (String) -> Unit,
        onAccept: (String) -> Unit,
        onRetry: (String) -> Unit,
        onDiscard: (String) -> Unit,
    ) = operations.configureAiAssistCallbacks(onSparkle, onAccept, onRetry, onDiscard)

    fun applyAiPolishToTextBlock(blockId: String, plainText: String) =
        operations.applyAiPolishToTextBlock(blockId, plainText)
}

