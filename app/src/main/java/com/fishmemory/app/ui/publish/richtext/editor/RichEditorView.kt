package com.fishmemory.app.ui.publish.richtext.editor

import android.content.ClipboardManager
import android.content.Context
import android.graphics.Rect
import android.net.Uri
import android.text.Editable
import android.text.Spannable
import android.text.TextWatcher
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.KeyEvent
import android.view.MotionEvent
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.widget.NestedScrollView
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputConnectionWrapper
import com.bumptech.glide.Glide
import com.fishmemory.app.R
import com.fishmemory.app.ui.publish.richtext.model.RichBlock
import kotlin.math.max
import kotlin.math.min


/**
 * 富文本编辑器统一排版规范
 */
object RichEditorStyle {

    // ===== 正文 =====
    val TEXT_BODY = TextBlockStyle(
        textSizeSp = 16f,                     // 字号 16sp
        lineSpacingMultiplier = 1.15f,        // 行距倍数 1.15
        lineSpacingExtraDp = 2,               // 额外行间距 2dp
        blockMarginDp = 4,                    // 段间距 6dp（上下块之间间距）
        paddingDp = Padding(left = 16, top = 6, right = 16, bottom = 0) // 内边距
    )

    // ===== 标题 =====
    val TEXT_TITLE = TextBlockStyle(
        textSizeSp = 20f,                     // 字号 20sp
        lineSpacingMultiplier = 1.2f,         // 行距倍数 1.2
        lineSpacingExtraDp = 2,               // 额外行间距 2dp
        blockMarginDp = 10,                   // 段间距 10dp
        paddingDp = Padding(left = 16, top = 16, right = 16, bottom = 16) // 内边距
    )

    // ===== 引用 =====
// 引用块风格，左侧缩进更大，适用于引用文本
    val TEXT_QUOTE = TextBlockStyle(
        textSizeSp = 16f,                     // 字号 16sp
        lineSpacingMultiplier = 1.15f,        // 行距倍数 1.15
        lineSpacingExtraDp = 2,               // 额外行间距 2dp
        blockMarginDp = 6,                    // 段间距 6dp
        paddingDp = Padding(left = 20, top = 12, right = 16, bottom = 12) // 左缩进 20dp
    )

    // ===== 列表 =====
// 列表块风格，左侧缩进更大，方便区分项目符号或数字
    val TEXT_LIST = TextBlockStyle(
        textSizeSp = 16f,                     // 字号 16sp
        lineSpacingMultiplier = 1.15f,        // 行距倍数 1.15
        lineSpacingExtraDp = 2,               // 额外行间距 2dp
        blockMarginDp = 6,                    // 段间距 6dp
        paddingDp = Padding(left = 28, top = 12, right = 16, bottom = 12) // 左缩进 28dp
    )

    // ===== 图片 =====
// 图片块风格，上下间距和左右内边距
    val IMAGE_BLOCK = ImageBlockStyle(
        marginTopBottomDp = 10,               // 图片块上下间距 12dp
        paddingDp = Padding(left = 16, top = 0, right = 16, bottom = 0) // 内边距，左右 16dp
    )


    // ===== 辅助 =====
    fun dpToPx(context: Context, dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }

    data class Padding(val left: Int, val top: Int, val right: Int, val bottom: Int)

    data class TextBlockStyle(
        val textSizeSp: Float,
        val lineSpacingMultiplier: Float,
        val lineSpacingExtraDp: Int,
        val blockMarginDp: Int,
        val paddingDp: Padding
    )

    data class ImageBlockStyle(
        val marginTopBottomDp: Int,
        val paddingDp: Padding
    )
}

/**
 * 富文本编辑器容器（精确控制滚动，每行对应一行滚动）
 */
class RichEditorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    companion object {
        private const val TAG = "RichEditorView"
        private const val DEBUG = true

        // ✅ 正文单个 Block 最大字符数，用于防止一次性粘贴超长内容
        const val MAX_BODY_BLOCK_CHARS = 800

        // ✅ 粘贴阈值：长度超过该值或包含换行时，视为“粘贴长文本”，走 Block 级拆分逻辑
        const val BULK_PASTE_MIN_LENGTH = 64
        // ✅ 整个文档的最大字符数限制（新增）
        const val MAX_TOTAL_CHARS = 10000
    }

    private var outerScroll: NestedScrollView? = null
    private var lastFocusEdit: EditText? = null

    private var lastCursorLine = 0
    private var lineHeight = 0
    private var isAdjustingScroll = false
    private var isUserScrolling = false

    // 是否正在执行回车拆块，用于关闭 TextWatcher 中的空块清理，避免拆块过程中改 View 树
    private var isSplittingByEnter: Boolean = false

    // ===== IME 遮挡高度 =====
    private var imeHeight = 0

    // ===== UX 参数（你只改这）=====
    private val startScrollLine = 3      // 前 3 行不滚
    private val triggerScrollLines = 9 // 提前 6 行触发

    init {
        orientation = VERTICAL
        addEditText()
    }

    fun bindScrollView(scrollView: NestedScrollView) {
        outerScroll = scrollView

        // 监听用户手势滚动，防止自动滚动与手动滚动打架
        scrollView.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN,
                MotionEvent.ACTION_MOVE -> {
                    // 用户正在主动滚动，暂时关闭自动跟随光标
                    isUserScrolling = true
                }
                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> {
                    // 手势结束后，短暂延迟再恢复自动滚动
                    scrollView.postDelayed({
                        isUserScrolling = false
                    }, 200)
                }
            }
            false
        }
    }

    /**
     * 提供给标题 EditText 调用：
     * 主动把焦点交给正文第一个 TextBlock，避免系统默认焦点回退到标题。
     */
    fun focusFirstTextBlock() {
        // 查找第一个 EditText，如果不存在就创建一个
        var first: EditText? = null
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child is EditText) {
                first = child
                break
            }
        }
        if (first == null) {
            first = createEditText()
            addView(first, 0)
        }

        // 🔥 显式设置焦点与光标位置，防止系统默认焦点回退到标题
        first.requestFocus()
        first.setSelection(first.text.length)
        lastFocusEdit = first
    }

    fun updateImeHeight(height: Int) {
        imeHeight = height
        if (DEBUG) Log.d(TAG, "IME 高度更新: $imeHeight")
        post { adjustScrollIfNeeded() }
    }

    private fun addEditText() {
        val et = createEditText()
        addView(et)
        et.requestFocus()
        lastFocusEdit = et
    }
// ===== 新增：富文本格式操作入口 =====

    fun toggleBold() {
        lastFocusEdit?.let { EditorSpanApplier.toggleBold(it) }
    }

    fun toggleUnderline() {
        lastFocusEdit?.let { EditorSpanApplier.toggleUnderline(it) }
    }
    private fun adjustScrollIfNeeded() {
        if (isAdjustingScroll) return
        // 用户正在主动滚动时，不触发自动滚动，避免抖动和“抢”位置
        if (isUserScrolling) return

        val scrollView = outerScroll ?: return
        val editText = lastFocusEdit ?: return
        val layout = editText.layout ?: return

        if (!scrollView.isLaidOut || !editText.isLaidOut) return

        isAdjustingScroll = true

        try {
            val cursorPos = editText.selectionStart
            if (cursorPos < 0) return

            val cursorLine = layout.getLineForOffset(cursorPos)

            if (lineHeight == 0) {
                lineHeight = editText.lineHeight
            }

            val scrollResult = calculatePreciseScroll(
                cursorLine,
                scrollView,
                editText
            )

            if (scrollResult.shouldScroll) {
                val target = scrollView.scrollY + scrollResult.scrollDistance
                scrollView.smoothScrollTo(0, target)
            }

            lastCursorLine = cursorLine
        } finally {
            isAdjustingScroll = false
        }
    }

    /**
 * 计算精确滚动位置，确保光标在可视区域内并保持舒适的显示距离。
 *
 * @param cursorLine 光标所在的行号
 * @param scrollView 包含 EditText 的 NestedScrollView 实例
 * @param editText 需要计算滚动位置的 EditText 实例
 * @return ScrollResult 包含是否需要滚动以及滚动距离的结果对象
 */
private fun calculatePreciseScroll(
    cursorLine: Int,
    scrollView: NestedScrollView,
    editText: EditText
): ScrollResult {

    // 如果光标所在行小于起始滚动行，则无需滚动
    if (cursorLine < startScrollLine) {
        return ScrollResult(false, 0)
    }

    val layout = editText.layout ?: return ScrollResult(false, 0)

    // 获取光标所在行的边界矩形
    val lineRect = Rect()
    layout.getLineBounds(cursorLine, lineRect)

    // 计算光标底部相对于 EditText 的绝对 Y 坐标
    val cursorBottomInEditText =
        editText.paddingTop + lineRect.bottom

    // 获取 EditText 和 ScrollView 在窗口中的位置
    val editLoc = IntArray(2)
    val scrollLoc = IntArray(2)
    editText.getLocationInWindow(editLoc)
    scrollView.getLocationInWindow(scrollLoc)

    // 计算光标在屏幕上的绝对 Y 坐标及其在 ScrollView 中的可见 Y 坐标
    val cursorAbsY = editLoc[1] + cursorBottomInEditText
    val cursorVisibleY =
        cursorAbsY - scrollLoc[1] + scrollView.scrollY

    // 计算真实的可见高度（减去输入法高度）
    val realVisibleHeight =
        scrollView.height - imeHeight

    // 获取当前视图在窗口中的位置，并计算其底部在 ScrollView 中的位置
    val editorLoc = IntArray(2)
    this.getLocationInWindow(editorLoc)

    val editorBottomInScroll =
        editorLoc[1] - scrollLoc[1] + scrollView.scrollY + height

    // 计算光标到编辑器底部的距离
    val distanceToEditorBottom =
        editorBottomInScroll - cursorVisibleY

    // 定义一个舒适的滚动触发距离（以行为单位）
    val comfortableDistance =
        triggerScrollLines * lineHeight

    if (DEBUG) {
        Log.d(TAG, "cursorLine=$cursorLine dist=$distanceToEditorBottom visH=$realVisibleHeight")
    }

    // 如果光标到编辑器底部的距离小于舒适距离，则需要滚动
    if (distanceToEditorBottom < comfortableDistance) {

        val need = comfortableDistance - distanceToEditorBottom
        val lines =
            kotlin.math.ceil(need / lineHeight.toFloat()).toInt()

        return ScrollResult(
            true,
            lines * lineHeight
        )
    }

    return ScrollResult(false, 0)
}


    private data class ScrollResult(
        val shouldScroll: Boolean,
        val scrollDistance: Int
    )



    private val imageContainers = mutableMapOf<FrameLayout, RichBlock.Image>()

    // 图片块回调
    interface OnImageBlockListener {
        fun onImageAdded(imageBlock: RichBlock.Image)
        fun onImageClicked(imageBlock: RichBlock.Image)
        fun onImageRemoved(imageBlock: RichBlock.Image)
    }

    private var imageBlockListener: OnImageBlockListener? = null

    fun setOnImageBlockListener(listener: OnImageBlockListener) {
        imageBlockListener = listener
    }

    // 添加图片块
    fun addLocalImage(imageUri: Uri, caption: String = "") {
        val et = lastFocusEdit ?: return
        val cursor = et.selectionStart

        val fullText = et.text.toString()
        val left = fullText.substring(0, cursor)
        val right = fullText.substring(cursor)

        val index = indexOfChild(et)

        // 1️⃣ 当前 EditText 保留左半部分
        et.setText(left)
        et.setSelection(left.length)

        // 2️⃣ 创建 ImageBlock
        val imageBlock = RichBlock.Image(
            url = imageUri.toString(),
            caption = caption,
            isLocal = true
        )

        val imageContainer = createImageContainer(imageBlock)
        imageContainers[imageContainer] = imageBlock

        // 3️⃣ 插入 ImageBlock
        addView(imageContainer, index + 1)

        // 4️⃣ Image 后必须有 TextBlock
        val nextEt = createEditText()

        if (right.isNotEmpty()) {
            nextEt.setText(right)
        }

        addView(nextEt, index + 2)

        // 5️⃣ 光标一定落在图片后
        nextEt.requestFocus()
        nextEt.setSelection(0)

        imageBlockListener?.onImageAdded(imageBlock)
    }



    private fun createImageContainer(imageBlock: RichBlock.Image,style: RichEditorStyle.ImageBlockStyle = RichEditorStyle.IMAGE_BLOCK): FrameLayout {
        return FrameLayout(context).apply {
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = RichEditorStyle.dpToPx(context, style.marginTopBottomDp)
                bottomMargin = RichEditorStyle.dpToPx(context, style.marginTopBottomDp)
            }
            setPadding(
                RichEditorStyle.dpToPx(context, style.paddingDp.left),
                RichEditorStyle.dpToPx(context, style.paddingDp.top),
                RichEditorStyle.dpToPx(context, style.paddingDp.right),
                RichEditorStyle.dpToPx(context, style.paddingDp.bottom)
            )

            // 图片
            val imageView = ImageView(context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.WRAP_CONTENT
                )
                scaleType = ImageView.ScaleType.FIT_CENTER
                adjustViewBounds = true
                background = null

                // 加载图片
                Glide.with(context)
                    .load(Uri.parse(imageBlock.url))
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_image_error)
                    .into(this)

                // 点击预览
                setOnClickListener {
                    imageBlockListener?.onImageClicked(imageBlock)
                }
            }

            addView(imageView)

            // 保存图片信息到Tag
            tag = imageBlock
        }
    }

    private fun createEditText(style: RichEditorStyle.TextBlockStyle = RichEditorStyle.TEXT_BODY): EditText {
        return BodyEditText(context).apply {
            textSize = style.textSizeSp
            background = null
            // ✅ 文本左右内缩（只留左右）
            setPadding(
                RichEditorStyle.dpToPx(context, style.paddingDp.left),
                RichEditorStyle.dpToPx(context, style.paddingDp.top),
                RichEditorStyle.dpToPx(context, style.paddingDp.right),
                RichEditorStyle.dpToPx(context, style.paddingDp.bottom)
            )

            // ✅ 行内行间距（同一段内）
            setLineSpacing(
                RichEditorStyle.dpToPx(context, style.lineSpacingExtraDp).toFloat(),
                style.lineSpacingMultiplier
            )

            // ✅ Block 之间的段落间距
            layoutParams = LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = RichEditorStyle.dpToPx(context, style.blockMarginDp)
                bottomMargin = RichEditorStyle.dpToPx(context, style.blockMarginDp)
            }

            setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    lastFocusEdit = this
                }
            }

            // 监听键盘删除键和回车键（分裂textblock）
            setOnKeyListener { _, keyCode, event ->
                // 🔒 处于输入法 composing 状态时，不做结构性编辑，全部交给输入法处理
                if (hasComposingText(this)) {
                    return@setOnKeyListener false
                }
                if (keyCode == KeyEvent.KEYCODE_ENTER &&
                    event.action == KeyEvent.ACTION_DOWN &&
                    !event.isShiftPressed
                ) {
                    return@setOnKeyListener handleEnterSplit(this)
                }

                if (keyCode == KeyEvent.KEYCODE_DEL &&
                    event.action == KeyEvent.ACTION_DOWN
                ) {
                    return@setOnKeyListener handleBackspace(this)
                }

                false
            }

            // ✅ 将「粘贴式输入」统一回调给 RichEditorView，在 Block 级别处理
            onBulkInsert = { pasted ->
                // 这里只处理“长文本 / 多行”粘贴，普通短文本交给系统默认逻辑
                handlePasteIntoBodyBlock(this, pasted.toString())
                true // 返回 true 表示已经处理，不再交给系统
            }


            addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    adjustScrollIfNeeded()
                    // 回车拆块过程中不做结构性清理
                }
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })
        }
    }

    /**
     * 粘贴长文本 → 规范化为多个正文 Block。
     *
     * 设计要点：
     * 1. 在这里统一区分「输入」与「粘贴」：
     *    - 普通按键：一个字符一个 commit，不会走这里。
     *    - 粘贴/剪贴板：一次性大量文本，走 onBulkInsert 回调。
     * 2. 按换行拆段，每个非空段映射为一个 TextBlock。
     * 3. 单个段落过长时再拆成多个 Block，避免单个 EditText 过大。
     */

    private fun handlePasteIntoBodyBlock(target: EditText, raw: String) {
        if (raw.isEmpty()) return

        // 1️⃣ 统一换行符，防止 Windows / Mac 混用 \r\n
        val normalized = raw
            .replace("\r\n", "\n")
            .replace('\r', '\n')

        val index = indexOfChild(target)
        if (index == -1) return

        val original = target.text.toString()

        // 2️⃣ 先按当前选择区间切分正文
        val selStart = target.selectionStart.coerceAtLeast(0)
        val selEnd = target.selectionEnd.coerceAtLeast(0)
        val rangeStart = min(selStart, selEnd)
        val rangeEnd = max(selStart, selEnd)

        val before = original.substring(0, rangeStart)
        val after = original.substring(rangeEnd)

        // 2.1️⃣ 计算当前文档总字符数，为整篇文档设置一个上限，防止极端粘贴把编辑器撑爆
        var totalChars = 0
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child is EditText) {
                totalChars += child.text.length
            }
        }
        // 本次粘贴会先替换掉 [rangeStart, rangeEnd) 这一段
        val currentEffectiveChars = totalChars - (rangeEnd - rangeStart)
        var remainingBudget = MAX_TOTAL_CHARS - currentEffectiveChars
        if (remainingBudget <= 0) {
            // 已经达到全局上限，直接丢弃本次粘贴
            return
        }

        // 3️⃣ 按换行拆成段落，并过滤掉纯空行
        val paragraphs = normalized
            .split('\n')
            .map { it.trimEnd() }
            .filter { it.isNotEmpty() }

        if (paragraphs.isEmpty()) return

        // 4️⃣ 段落级 + 长度级拆分，生成最终要插入的「块」序列
        val segments = mutableListOf<String>();
        paragraphs.forEach { para ->
            var remain = para
            while (remain.isNotEmpty() && remainingBudget > 0) {
                val take = minOf(remain.length, MAX_BODY_BLOCK_CHARS, remainingBudget)
                segments.add(remain.substring(0, take))
                remainingBudget -= take
                remain = remain.substring(take)
            }
            if (remainingBudget <= 0) return@forEach
        }

        if (segments.isEmpty()) return

        // 5️⃣ 第一段仍然塞回当前 EditText，避免无意义的新 View 抖动
        val firstInserted = segments.first()
        val newCurrentText = before + firstInserted
        target.setText(newCurrentText)
        target.setSelection(newCurrentText.length)

        var lastEt: EditText = target
        val remainingSegments = segments.drop(1)

        // 6️⃣ 其余段落依次在当前块后面插入新的 TextBlock
        remainingSegments.forEachIndexed { segIndex, segment ->
            val newEt = createEditText()
            val textForThis =
                // 最后一段需要把原来的「after」拼接上，保持语义顺序
                if (segIndex == remainingSegments.lastIndex) segment + after else segment

            newEt.setText(textForThis)

            val insertPos = indexOfChild(lastEt) + 1
            addView(newEt, insertPos)

            lastEt = newEt
        }

        // 7️⃣ 最终把光标落在「粘贴内容的末尾」（在原 after 之前）
        lastEt.requestFocus()
        val cursorPosInLast = (lastEt.text.length - after.length)
            .coerceIn(0, lastEt.text.length)
        lastEt.setSelection(cursorPosInLast)

        // 8️⃣ 粘贴后也做一次滚动校正，保证插入位置在可视区内
        adjustScrollIfNeeded()
    }

    /**
     * 尝试删除光标前的图片
     * 规则：如果光标在EditText的开头，且前面是图片，则删除该图片
     */
    private fun tryDeletePreviousImage(): Boolean {
        val current = lastFocusEdit ?: return false

        // 光标必须在起始位置
        if (!isCursorAtStart(current)) return false

        val currentIndex = indexOfChild(current)
        if (currentIndex <= 0) return false

        val imageView = getChildAt(currentIndex - 1)
        if (imageView !is FrameLayout) return false

        val imageBlock =
            imageContainers[imageView]
                ?: imageView.tag as? RichBlock.Image
                ?: return false

        // 找图片前的 TextBlock
        val prevIndex = currentIndex - 2
        val prevText = if (prevIndex >= 0) getChildAt(prevIndex) as? EditText else null

        // 1️⃣ 删除图片 Block
        removeView(imageView)
        imageContainers.remove(imageView)
        imageBlockListener?.onImageRemoved(imageBlock)

        // 2️⃣ 合并文本 Block
        if (prevText != null) {
            val cursorPos=mergeTextWithNewLine(
                prevText.text,
                current.text
            )

            // 删除当前 TextBlock
            removeView(current)

            // 光标落在拼接点
            prevText.requestFocus()
            prevText.setSelection(cursorPos)
        } else {
            // 前面没有 TextBlock（图片在最前）
            current.requestFocus()
            current.setSelection(0)
        }

        // 3️⃣ 清理多余空块
       removeConsecutiveEmptyTextBlocks()

        return true
    }



    // ====== 文档块访问 API ======

    /**
     * 获取当前正文部分的所有 Block 结构。
     * 外部（如 PublishActivity）应通过文档模型 RichDocument 来持有标题 + blocks。
     */
    fun getBlocks(): List<RichBlock> {
        val blocks = mutableListOf<RichBlock>()

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            when (child) {
                is EditText -> {
                    blocks.add(RichBlock.Text(
                        content = child.text.toString(),
                        spans = EditorSpanParser.parse(child)
                    ))
                }
                is FrameLayout -> {
                    (imageContainers[child] ?: child.tag as? RichBlock.Image)?.let { imageBlock ->
                        blocks.add(imageBlock)
                    }
                }
            }
        }

        return blocks
    }

    /**
     * 兼容旧 API：getAllBlocks 已经等价于 getBlocks。
     * 建议新代码使用 getBlocks，以强调这是正文 Block 列表。
     */
    fun getAllBlocks(): List<RichBlock> = getBlocks()

    /**
     * 根据传入的 RichBlock 列表重建正文视图结构。
     * - 标题不在这里处理，由 PublishActivity 单独维护。
     * - 若列表为空，则保证至少存在一个可编辑的 TextBlock。
     */
    fun setBlocks(blocks: List<RichBlock>) {
        // 清空现有视图与图片缓存
        removeAllViews()
        imageContainers.clear()
        lastFocusEdit = null

        // 逐个 Block 重建视图
        blocks.forEach { block ->
            when (block) {
                is RichBlock.Text -> {
                    val et = createEditText()
                    et.setText(block.content)
                    addView(et)
                }
                is RichBlock.Image -> {
                    val container = createImageContainer(block)
                    imageContainers[container] = block
                    addView(container)
                }
                is RichBlock.Code -> {
                    // 简化处理：代码块按普通文本块渲染，样式由预览层负责
                    val et = createEditText()
                    et.setText(block.code)
                    addView(et)
                }
            }
        }

        // 若没有任何 Block，保证至少存在一个可编辑正文块
        if (childCount == 0) {
            addEditText()
        }

        // 默认将焦点放在最后一个 TextBlock，便于继续编辑
        var lastText: EditText? = null
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child is EditText) {
                lastText = child
            }
        }
        lastText?.let { et ->
            et.requestFocus()
            et.setSelection(et.text.length)
            lastFocusEdit = et
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    /**
     * 判断当前 EditText 是否存在输入法的 composing 文本（拼音未上屏状态）。
     * 在这种状态下不做任何块级编辑（拆块、合并、删除），交给输入法自己管理。
     */
    private fun hasComposingText(et: EditText): Boolean {
        val text = et.text
        if (text !is Spannable) return false
        val start = BaseInputConnection.getComposingSpanStart(text)
        val end = BaseInputConnection.getComposingSpanEnd(text)
        return start != -1 && end != -1 && start != end
    }
    //判空这里不是没有字而是光标在最开始
    private fun isCursorAtStart(et: EditText): Boolean {
        return et.selectionStart == 0 && et.selectionEnd == 0
    }
    //合并换行
    private fun mergeTextWithNewLine(
        left: Editable,
        right: Editable
    ): Int {
        val leftLen = left.length

        val needNewLine =
            leftLen > 0 &&
                    !left.endsWith("\n") &&
                    right.isNotEmpty() &&
                    !right.startsWith("\n")

        if (needNewLine) {
            left.append("\n")
        }

        left.append(right)
        return leftLen + if (needNewLine) 1 else 0
    }
   //回车分裂textblock
    private fun handleEnterSplit(et: EditText): Boolean {
        val cursor = et.selectionStart
        if (cursor < 0) return false

        // 标记：从这里开始属于“拆块中”，禁止 TextWatcher 里再做结构性清理
        if (isSplittingByEnter) {
            // 理论上不会嵌套，这里防御一下，直接交给系统处理这次回车
            return false
        }
        isSplittingByEnter = true

        try {
            val fullText = et.text.toString()
            val left = fullText.substring(0, cursor)
            val right = fullText.substring(cursor)

            // 先拿到当前 et 在 RichEditorView 中的位置
            val originalIndex = indexOfChild(et)
            if (originalIndex == -1) {
                // 当前 EditText 已不在 View 树中，说明状态异常，这次拆块直接放弃
                return false
            }

            // 1️⃣ 当前 TextBlock 保留左半
            et.setText(left)
            et.setSelection(left.length)

            // 2️⃣ 创建新的 TextBlock
            val nextEt = createEditText()
            nextEt.setText(right)

            // 3️⃣ 计算安全的插入位置：
            //    - 正常情况下 originalIndex+1 一定 <= childCount
            //    - 为防御未知情况，这里再用 coerceIn 做一次保护
            val currentCount = childCount
            val safeInsertPos = (originalIndex + 1).coerceIn(0, currentCount)
            addView(nextEt, safeInsertPos)

            // 4️⃣ 光标进入新块
            nextEt.requestFocus()
            nextEt.setSelection(0)


            return true // 消耗 Enter

        } finally {
            // 无论拆块是否成功，都要恢复状态
            isSplittingByEnter = false
        }
    }

    private fun handleBackspace(current: EditText): Boolean {
        // 1️⃣ 光标不在行首：交给系统
        if (!isCursorAtStart(current)) return false

        val index = indexOfChild(current)
        if (index <= 0) return false

        val prev = getChildAt(index - 1)

        return when (prev) {
            is FrameLayout -> {
                // ImageBlock
                tryDeletePreviousImage()
            }
            is EditText -> {
                // TextBlock
                mergeWithPreviousTextBlock(current, prev)
            }
            else -> false
        }
    }
    //文字的合并逻辑
    private fun mergeWithPreviousTextBlock(
        current: EditText,
        prev: EditText
    ): Boolean {

        val prevText = prev.text.toString()
        val currText = current.text.toString()

        val merged = prevText + currText

        // 1️⃣ 合并文本
        prev.setText(merged)
        prev.setSelection(prevText.length)

        // 2️⃣ 删除当前块
        removeView(current)

        // 3️⃣ 光标回到 prev
        prev.requestFocus()

        // 4️⃣ 清理多余空块
        cleanupEmptyTextBlocks()

        return true
    }
    //空白回收（全量扫描）
    private fun cleanupEmptyTextBlocks() {
        // 至少保留一个 TextBlock
        var hasNonEmpty = false

        for (i in 0 until childCount) {
            val v = getChildAt(i)
            if (v is EditText && v.text.isNotEmpty()) {
                hasNonEmpty = true
                break
            }
        }

        var i = 0
        while (i < childCount) {
            val v = getChildAt(i)

            if (v is EditText && v.text.isEmpty()) {
                if (hasNonEmpty) {
                    removeView(v)
                    continue
                }
            }
            i++
        }

        // 如果一个都没有，补一个
        if (!hasNonEmpty) {
            val et = createEditText()
            addView(et)
            et.requestFocus()
        }
    }


    //空块清理器（相邻节点）
    private fun removeConsecutiveEmptyTextBlocks() {
        var i = 0
        while (i < childCount - 1) {
            val current = getChildAt(i) as? EditText
            val next = getChildAt(i + 1) as? EditText

            if (current != null && next != null &&
                current.text.isEmpty() && next.text.isEmpty()
            ) {
                // ✅ 只把「真正没有内容」的连续空块压缩成一个
                // 为避免系统把焦点抢走，优先保留当前有焦点的那个
                val removeTarget = when {
                    next.isFocused -> current
                    else -> next
                }
                val keepTarget = if (removeTarget === current) next!! else current
                val needMoveFocus = removeTarget.isFocused

                removeView(removeTarget)

                // 🔥 如果被删的是当前有焦点的块，显式把焦点交给相邻块
                if (needMoveFocus) {
                    keepTarget.requestFocus()
                    keepTarget.setSelection(keepTarget.text.length)
                    lastFocusEdit = keepTarget
                }

                continue // 当前位置内容发生变化，继续检查当前位置
            }
            i++
        }
    }

}

/**
 * 正文用 EditText：
 * - 统一拦截「粘贴式输入」：
 *   - 系统菜单 paste → onTextContextMenuItem + ClipData
 *   - 输入法一次 commitText 大段文本 → InputConnectionWrapper
 * - 然后通过 onBulkInsert 回调交给 RichEditorView 做 Block 级拆分。
 */
private class BodyEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AppCompatEditText(context, attrs) {

    /**
     * 当判断为「粘贴长文本」时回调：
     * 返回 true 表示已经处理，BodyEditText 不再按默认方式插入。
     */
    var onBulkInsert: ((CharSequence) -> Boolean)? = null

    override fun onTextContextMenuItem(id: Int): Boolean {
        if (id == android.R.id.paste || id == android.R.id.pasteAsPlainText) {
            // ✅ 使用 ClipData 读取系统剪贴板内容
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = cm?.primaryClip
            val item = clip?.getItemAt(0)
            val text = item?.coerceToText(context)

            if (!text.isNullOrEmpty()) {
                // 如果外部希望接管粘贴（例如长文本/多行），就交给外部处理
                val handled = onBulkInsert?.invoke(text) ?: false
                if (handled) {
                    return true // 已处理完毕，阻止默认插入
                }
            }
        }
        return super.onTextContextMenuItem(id)
    }

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection {
        val base = super.onCreateInputConnection(outAttrs)
        // ✅ 通过 InputConnectionWrapper 拦截「一次性提交的大段文本」
        return object : InputConnectionWrapper(base, false) {
            override fun commitText(text: CharSequence?, newCursorPosition: Int): Boolean {
                val bulkHandler = onBulkInsert
                if (text != null && bulkHandler != null && shouldTreatAsBulk(text)) {
                    // 把「类似粘贴」的大段文本交给 RichEditorView 统一处理
                    if (bulkHandler.invoke(text)) {
                        return true
                    }
                }
                return super.commitText(text, newCursorPosition)
            }
        }
    }

    /**
     * 判定本次 commitText 是否属于「粘贴式输入」：
     * - 含有换行，或者
     * - 长度超过一定阈值
     *
     * 这样既能区分普通键入（单字符/短语），又能覆盖输入法里的“粘贴整段文字”。
     */
    private fun shouldTreatAsBulk(text: CharSequence): Boolean {
        return text.length >= RichEditorView.BULK_PASTE_MIN_LENGTH || text.contains('\n')
    }
}
