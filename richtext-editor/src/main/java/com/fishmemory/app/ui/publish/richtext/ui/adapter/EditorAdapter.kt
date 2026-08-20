package com.fishmemory.app.ui.publish.richtext.ui.adapter

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.fishmemory.app.ui.publish.richtext.api.EditorVideoUploader
import com.fishmemory.app.ui.publish.richtext.business.selection.OperationFocusResult
import com.fishmemory.app.ui.publish.richtext.core.BlockInteractionListener
import com.fishmemory.app.ui.publish.richtext.core.model.EditorBlockList
import com.fishmemory.app.ui.publish.richtext.core.model.EditorBlock
import com.fishmemory.app.ui.publish.richtext.core.model.EditorBlockDisplay
import com.fishmemory.app.ui.publish.richtext.ui.actions.ImageBlockActions
import com.fishmemory.app.ui.publish.richtext.ui.actions.LinkUiActions
import com.fishmemory.app.ui.publish.richtext.ui.actions.ListBlockActions
import com.fishmemory.app.ui.publish.richtext.ui.view.TextBlockView
import com.fishmemory.app.ui.publish.ai.AiAssistUiState
import com.fishmemory.app.ui.publish.richtext.business.media.VideoPlayerManager

/**
 * Block 列表适配器：编辑态用 EditorBlockList，只读态用 List<BlockDisplay>。
 * 通过 isReadOnly + readOnlyBlocks 区分；只读时复用同一套布局与 ViewHolder，仅禁用编辑回调和编辑控件。
 */
class EditorAdapter(
    private val blockList: EditorBlockList? = null,
    private var readOnlyBlocks: List<EditorBlockDisplay>? = null,
    private val linkUiActions: LinkUiActions? = null,  // 注入 LinkUiActions
    private val imageBlockActions: ImageBlockActions? = null,  // 注入 ImageBlockActions
    private val listBlockActions: ListBlockActions? = null  // 注入 ListBlockActions
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val mainHandler = Handler(Looper.getMainLooper())

    private val isReadOnly: Boolean get() = readOnlyBlocks != null

    /**
     * bind 中 [setText] 会同步触发 TextWatcher → [onContentChanged]；若在此时 [notifyItemChanged]
     * 会抛出 IllegalStateException（RecyclerView 正在 layout）。排到下一消息再刷新 AI 条。
     */
    private fun scheduleAiAssistItemRefresh(blockId: String) {
        mainHandler.post {
            val idx = blockList?.getBlockPosition(blockId) ?: -1
            if (idx >= 0) {
                notifyItemChanged(idx + 1, PAYLOAD_AI_ASSIST)
            }
        }
    }

    fun setReadOnlyBlocks(blocks: List<EditorBlockDisplay>?) {
        readOnlyBlocks = blocks
        notifyDataSetChanged()
    }

    var onSplitRequested: ((blockId: String, cursorPos: Int) -> OperationFocusResult?)? = null
    var onEnterRequested: ((blockId: String, cursorPos: Int) -> Boolean)? = null
    var onMergeRequested: ((blockId: String) -> OperationFocusResult?)? = null
    var onBackspaceAtStart: ((blockId: String) -> OperationFocusResult?)? = null
    var onFocusGained: ((blockId: String) -> Unit)? = null
    var onFocusLost: ((blockId: String) -> Unit)? = null
    var onLinkClicked: ((blockId: String, url: String, start: Int, end: Int) -> Unit)? = null
    var onImageBlockDeleteRequested: ((blockId: String) -> Unit)? = null
    var onImageBlockReplaceRequested: ((blockId: String) -> Unit)? = null
    var onImageBlockPreviewRequested: ((blockId: String) -> Unit)? = null
    var onImageBlockChanged: ((blockId: String) -> Unit)? = null
    var onImageBlockMenuRequested: ((anchorView: View, blockId: String) -> Unit)? = null

    /** HR Block 选中/删除回调由外层（如 BlockEditorRecyclerView）处理，避免 Adapter 持有过多业务。 */
    var onHrBlockClicked: ((position: Int) -> Unit)? = null
    var onHrBlockDeleteRequested: ((position: Int) -> Unit)? = null

    /** 当前选中的 HR Block 位置，仅用于 UI，同步到 EditorBlock.HrBlock.isSelected。 */
    var selectedHrPosition: Int = -1

    /** Code Block 选中/删除回调。 */
    var onCodeBlockClicked: ((position: Int) -> Unit)? = null
    var onCodeBlockDeleteRequested: ((position: Int) -> Unit)? = null
    var onCodeBlockFocusGained: ((blockId: String) -> Unit)? = null

    /** 当前选中的 Code Block 位置，仅用于 UI，同步到 EditorBlock.CodeBlock.isSelected。 */
    var selectedCodePosition: Int = -1

    /** LinkCard Block 选中/删除回调。 */
    var onLinkCardClicked: ((position: Int) -> Unit)? = null
    var onLinkCardDeleteRequested: ((blockId: String) -> Unit)? = null
    var selectedLinkCardPosition: Int = -1

    /** Video Block 回调与选中态。 */
    var onVideoBlockDeleteRequested: ((blockId: String) -> Unit)? = null
    var onVideoBlockRetryRequested: ((blockId: String) -> Unit)? = null
    var onVideoBlockChanged: ((blockId: String) -> Unit)? = null
    var onVideoBlockClicked: ((blockId: String) -> Unit)? = null
    var selectedVideoBlockId: String? = null
    var videoPlayerManager: VideoPlayerManager? = null
    var videoUploadManager: EditorVideoUploader? = null

    /** 标题文本与回调，由 BlockEditorRecyclerView 负责与 RichDocument 同步。 */
    var titleText: String = ""
    var onTitleChanged: ((String) -> Unit)? = null
    var onTitleNextRequested: (() -> Unit)? = null

    /** 任意编辑内容变更（标题/正文/结构块）统一回调。 */
    var onContentChanged: (() -> Unit)? = null

    /** 当前聚焦的正文块 id，用于展示 ✨；与 [onFocusGained] 同步。 */
    var focusedTextBlockId: String? = null

    /** 与 ViewModel 会话 map 同步的镜像，payload 刷新时读取。 */
    var aiAssistStates: Map<String, AiAssistUiState> = emptyMap()

    var onAiSparkleClick: ((blockId: String) -> Unit)? = null
    var onAiAccept: ((blockId: String) -> Unit)? = null
    var onAiRetry: ((blockId: String) -> Unit)? = null
    var onAiDiscard: ((blockId: String) -> Unit)? = null

    init {
        setHasStableIds(true)
    }

    override fun getItemCount(): Int {
        if (isReadOnly) return (readOnlyBlocks?.size ?: 0)
        return 1 + (blockList?.getBlocks()?.size ?: 0)
    }

    override fun getItemId(position: Int): Long {
        if (isReadOnly) {
            val block = readOnlyBlocks?.getOrNull(position) ?: return RecyclerView.NO_ID
            return block.id.hashCode().toLong()
        }
        if (position == 0) return Long.MIN_VALUE
        val index = position - 1
        val blocks = blockList!!.getBlocks()
        if (index !in blocks.indices) return RecyclerView.NO_ID
        return blocks[index].id.hashCode().toLong()
    }

    override fun getItemViewType(position: Int): Int {
        if (isReadOnly) {
            val block = readOnlyBlocks?.getOrNull(position) ?: return VIEW_TYPE_TEXT
            return when (block) {
                is EditorBlockDisplay.Text -> VIEW_TYPE_TEXT
                is EditorBlockDisplay.Image -> VIEW_TYPE_IMAGE
                is EditorBlockDisplay.Code -> VIEW_TYPE_CODE
                is EditorBlockDisplay.Hr -> VIEW_TYPE_HR
                is EditorBlockDisplay.LinkCard -> VIEW_TYPE_LINK_CARD
                is EditorBlockDisplay.Video -> VIEW_TYPE_VIDEO
                is EditorBlockDisplay.ListBlock -> VIEW_TYPE_LIST
            }
        }
        if (position == 0) return VIEW_TYPE_TITLE
        val index = position - 1
        val block = blockList!!.getBlocks().getOrNull(index) ?: return VIEW_TYPE_TEXT
        return when (block) {
            is EditorBlock.TextBlock -> VIEW_TYPE_TEXT
            is EditorBlock.ImageBlock -> VIEW_TYPE_IMAGE
            is EditorBlock.HrBlock -> VIEW_TYPE_HR
            is EditorBlock.CodeBlock -> VIEW_TYPE_CODE
            is EditorBlock.LinkCard -> VIEW_TYPE_LINK_CARD
            is EditorBlock.VideoBlock -> VIEW_TYPE_VIDEO
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_TITLE -> TitleViewHolder.create(parent, this)
            VIEW_TYPE_IMAGE -> {
                val actions = imageBlockActions ?: run {
                    val activity = parent.context as? FragmentActivity
                        ?: throw IllegalStateException("ImageBlockActions 构造失败：context 不是 FragmentActivity")
                    ImageBlockActions(activity)
                }
                ImageBlockViewHolder.create(parent, actions)
            }
            VIEW_TYPE_HR -> HrBlockViewHolder.create(parent)
            VIEW_TYPE_CODE -> CodeBlockViewHolder.create(parent)
            VIEW_TYPE_LINK_CARD -> LinkCardViewHolder.create(parent)
            VIEW_TYPE_VIDEO -> VideoBlockViewHolder.create(parent)
            VIEW_TYPE_LIST -> {
                val actions = listBlockActions ?: ListBlockActions()
                ListItemBlockViewHolder.create(parent, actions)
            }
            else -> {
                val view = TextBlockView(parent.context)
                view.layoutParams = RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                TextBlockViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val adapterPosition = holder.adapterPosition
        if (adapterPosition == RecyclerView.NO_POSITION) return
        if (isReadOnly) {
            val display = readOnlyBlocks?.getOrNull(position) ?: return
            when (holder) {
                is TextBlockViewHolder -> (display as? EditorBlockDisplay.Text)?.let { holder.bindReadOnly(it) }
                is ImageBlockViewHolder -> (display as? EditorBlockDisplay.Image)?.let { holder.bindReadOnly(it, onImageBlockPreviewRequested) }
                is HrBlockViewHolder -> (display as? EditorBlockDisplay.Hr)?.let { holder.bindReadOnly() }
                is CodeBlockViewHolder -> (display as? EditorBlockDisplay.Code)?.let { holder.bindReadOnly(it) }
                is LinkCardViewHolder -> (display as? EditorBlockDisplay.LinkCard)?.let { 
                    holder.bindReadOnly(it, linkUiActions) 
                }
                is VideoBlockViewHolder -> (display as? EditorBlockDisplay.Video)?.let { holder.bindReadOnly(it) }
                is ListItemBlockViewHolder -> (display as? EditorBlockDisplay.ListBlock)?.let { holder.bindReadOnly(it) }
                else -> {}
            }
            return
        }
        if (position == 0) {
            val vh = holder as? TitleViewHolder ?: return
            Log.d("TitleDebug", "[onBindViewHolder] title position, titleText=[$titleText]")
            vh.bind(
                title = titleText,
                maxLength = 40,
                onTitleChanged = { text ->
                    Log.d("TitleDebug", "[onBindViewHolder] onTitleChanged callback: [$text]")
                    onTitleChanged?.invoke(text)
                },
                onNextRequested = {
                    Log.d("TitleDebug", "[onBindViewHolder] onNextRequested called")
                    onTitleNextRequested?.invoke()
                }
            )
            return
        }
        val index = position - 1
        val block = blockList!!.getBlocks().getOrNull(index) ?: return
        when (holder) {
            is TextBlockViewHolder -> {
                val textBlock = block as? EditorBlock.TextBlock ?: return
                val blocks = blockList!!.getBlocks()
                val prevIsQuote = (blocks.getOrNull(index - 1) as? EditorBlock.TextBlock)?.isQuote == true
                val nextIsQuote = (blocks.getOrNull(index + 1) as? EditorBlock.TextBlock)?.isQuote == true
                holder.blockView.editText.interactionListener = object : BlockInteractionListener {
                    override fun onSplitRequested(blockId: String, cursorPos: Int): OperationFocusResult? {
                        return this@EditorAdapter.onSplitRequested?.invoke(blockId, cursorPos)
                    }

                    override fun onEnterRequested(blockId: String, cursorPos: Int): Boolean {
                        val result = this@EditorAdapter.onEnterRequested?.invoke(blockId, cursorPos)
                        return result == true
                    }

                    override fun onMergeRequested(blockId: String): OperationFocusResult? {
                        return this@EditorAdapter.onMergeRequested?.invoke(blockId)
                    }

                    override fun onBackspaceAtStart(blockId: String): OperationFocusResult? {
                        return this@EditorAdapter.onBackspaceAtStart?.invoke(blockId)
                    }

                    override fun onFocusGained(blockId: String) {
                        this@EditorAdapter.onFocusGained?.invoke(blockId)
                    }

                    override fun onFocusLost(blockId: String) {
                        if (focusedTextBlockId != blockId) return
                        this@EditorAdapter.onFocusLost?.invoke(blockId)
                    }

                    override fun onContentChanged(blockId: String) {
                        this@EditorAdapter.onContentChanged?.invoke()
                        // 输入导致空/非空变化时更新 ✨；必须 post，避免 bind 内 setText 同步回调时 notify
                        if (!isReadOnly) {
                            scheduleAiAssistItemRefresh(blockId)
                        }
                    }

                    override fun onLinkClicked(blockId: String, url: String, start: Int, end: Int) {
                        this@EditorAdapter.onLinkClicked?.invoke(blockId, url, start, end)
                    }
                }
                holder.bind(textBlock, prevIsQuote, nextIsQuote)
                holder.bindAiAssist(
                    block = textBlock,
                    focusedTextBlockId = focusedTextBlockId,
                    state = aiAssistStates[textBlock.id] ?: AiAssistUiState.Idle,
                    onSparkleClick = { onAiSparkleClick?.invoke(textBlock.id) },
                    onAccept = { onAiAccept?.invoke(textBlock.id) },
                    onRetry = { onAiRetry?.invoke(textBlock.id) },
                    onDiscard = { onAiDiscard?.invoke(textBlock.id) },
                )
            }
            is ImageBlockViewHolder -> {
                val imageBlock = block as? EditorBlock.ImageBlock ?: return
                holder.bind(
                    imageBlock,
                    onDeleteRequested = onImageBlockDeleteRequested,
                    onReplaceRequested = onImageBlockReplaceRequested,
                    onPreviewRequested = onImageBlockPreviewRequested,
                    onBlockChanged = onImageBlockChanged,
                    onMenuRequested = onImageBlockMenuRequested
                )
            }
            is HrBlockViewHolder -> {
                val hrBlock = block as? EditorBlock.HrBlock ?: return
                val isSelected = position == selectedHrPosition
                hrBlock.isSelected = isSelected
                holder.bind(
                    block = hrBlock,
                    isSelected = isSelected,
                    onClick = { pos -> onHrBlockClicked?.invoke(pos) },
                    onDelete = { pos -> onHrBlockDeleteRequested?.invoke(pos) }
                )
            }
            is CodeBlockViewHolder -> {
                val codeBlock = block as? EditorBlock.CodeBlock ?: return
                val isSelected = position == selectedCodePosition
                codeBlock.isSelected = isSelected
                Log.d("CodeBind", "[onBindViewHolder] position=$position, selectedCodePosition=$selectedCodePosition, isSelected=$isSelected")
                holder.bind(
                    block = codeBlock,
                    isSelected = isSelected,
                    onClicked = { pos -> onCodeBlockClicked?.invoke(pos) },
                    onDelete = { _ ->
                        val adapterPos = holder.bindingAdapterPosition
                        if (adapterPos != RecyclerView.NO_POSITION) {
                            onCodeBlockDeleteRequested?.invoke(adapterPos)
                        }
                    },
                    onFocusGained = { id -> onCodeBlockFocusGained?.invoke(id) },
                    onBlockChanged = { onContentChanged?.invoke() }
                )
            }
            is LinkCardViewHolder -> {
                val cardBlock = block as? EditorBlock.LinkCard ?: return
                val isSelected = position == selectedLinkCardPosition
                cardBlock.isSelected = isSelected
                holder.bind(
                    block = cardBlock,
                    isSelected = isSelected,
                    onClicked = { pos -> onLinkCardClicked?.invoke(pos) },
                    onDelete = { id -> onLinkCardDeleteRequested?.invoke(id) },
                    linkUiActions = linkUiActions  // 新增：传入 LinkUiActions
                )
            }
            is VideoBlockViewHolder -> {
                val videoBlock = block as? EditorBlock.VideoBlock ?: return
                videoBlock.isSelected = videoBlock.id == selectedVideoBlockId
                holder.bind(
                    block = videoBlock,
                    playerManager = videoPlayerManager,
                    uploadManager = videoUploadManager,
                    onDeleteRequested = onVideoBlockDeleteRequested,
                    onRetryRequested = onVideoBlockRetryRequested,
                    onBlockChanged = onVideoBlockChanged,
                    onPlayStateChanged = { _, _ -> },
                    onVideoClicked = onVideoBlockClicked
                )
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position)
            return
        }
        if (
            holder is TextBlockViewHolder &&
            !isReadOnly &&
            position > 0 &&
            payloads.contains(PAYLOAD_AI_ASSIST)
        ) {
            val textBlock = blockList!!.getBlocks().getOrNull(position - 1) as? EditorBlock.TextBlock
                ?: return
            holder.bindAiAssist(
                block = textBlock,
                focusedTextBlockId = focusedTextBlockId,
                state = aiAssistStates[textBlock.id] ?: AiAssistUiState.Idle,
                onSparkleClick = { onAiSparkleClick?.invoke(textBlock.id) },
                onAccept = { onAiAccept?.invoke(textBlock.id) },
                onRetry = { onAiRetry?.invoke(textBlock.id) },
                onDiscard = { onAiDiscard?.invoke(textBlock.id) },
            )
            return
        }
        onBindViewHolder(holder, position)
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        if (holder is ImageBlockViewHolder) holder.clearGlide()
        if (holder is CodeBlockViewHolder) holder.clear()
        if (holder is LinkCardViewHolder) holder.clear()
        if (holder is VideoBlockViewHolder) holder.clear()
    }

    companion object {
        /** 局部刷新：仅更新 AI 辅助条，避免重绑 EditText。 */
        const val PAYLOAD_AI_ASSIST = "ai_assist"

        private const val VIEW_TYPE_TITLE = 0
        private const val VIEW_TYPE_TEXT = 1
        private const val VIEW_TYPE_IMAGE = 2
        private const val VIEW_TYPE_HR = 3
        private const val VIEW_TYPE_CODE = 4
        private const val VIEW_TYPE_LINK_CARD = 5
        private const val VIEW_TYPE_VIDEO = 6
        private const val VIEW_TYPE_LIST = 7
    }
}
