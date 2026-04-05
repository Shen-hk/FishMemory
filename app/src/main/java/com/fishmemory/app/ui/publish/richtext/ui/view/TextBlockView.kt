package com.fishmemory.app.ui.publish.richtext.ui.view

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import com.fishmemory.app.R
import com.fishmemory.app.ui.publish.ai.AiAssistUiState
import com.fishmemory.app.ui.publish.richtext.config.EditorStyle
import com.fishmemory.app.ui.publish.richtext.ui.view.BlockEditText
import com.fishmemory.app.ui.publish.richtext.core.model.EditorBlock

/**
 * 单块容器：内嵌 BlockEditText，应用正文块样式（padding、行距、块间距）。
 * 字体设置：优先使用思源黑体(Source Han Sans)， fallback 到系统默认黑体
 */
class TextBlockView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    val editText: BlockEditText = BlockEditText(context).apply {
        textSize = EditorStyle.TEXT_BODY.textSizeSp
        background = null

        // 设置字体族：思源黑体 > 系统默认黑体
        try {
            typeface = Typeface.createFromAsset(context.assets, "fonts/SourceHanSansCN-Regular.ttf")
        } catch (e: Exception) {
            // 如果思源黑体不可用，使用系统默认字体
            typeface = Typeface.DEFAULT
        }
    }

    /**
     * 当前是否为引用模式，供 BlockEditText 判断回车行为。
     */
    var isQuoteMode: Boolean = false
        private set

    /**
     * 当前是否为标题样式，由 BlockEditorRecyclerView 控制。
     */
    private var isHeadingMode: Boolean = false

    private var currentListType: EditorBlock.ListType? = null

    private val quoteIndicator: View = View(context).apply {
        // 使用更粗的灰色竖线，让引用标识更明显
        val width = EditorStyle.dpToPx(context, 4)
        layoutParams = LinearLayout.LayoutParams(
            width,
            LayoutParams.MATCH_PARENT
        ).apply {
            // 减少右边距，让竖线更靠近文字
            rightMargin = EditorStyle.dpToPx(context, 1)
        }
        setBackgroundColor(ContextCompat.getColor(context, R.color.gray))
        visibility = GONE
    }

    // 列表前缀容器：仅列表块时赋予宽度与间距，非列表时宽度为 0 避免占位缩进
    private val prefixContainer: LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_HORIZONTAL
        layoutParams = LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT)
    }

    private val listPrefixWidthPx: Int = EditorStyle.dpToPx(context, 34)
    private val listPrefixGapPx: Int = EditorStyle.dpToPx(context, 2)

    // 无序列表圆点
    private val bulletView: View = View(context).apply {
        val size = EditorStyle.dpToPx(context, 6)
        layoutParams = LinearLayout.LayoutParams(size, size).apply {
            topMargin = EditorStyle.dpToPx(context, 8)
        }

        // 使用纯代码绘制小圆点，避免额外资源文件
        background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(ContextCompat.getColor(context, R.color.app_primary))
        }
        visibility = GONE
    }

    // 有序列表序号文本，右对齐展示 "1." "2." 等
    private val numberView: TextView = TextView(context).apply {
        textSize = EditorStyle.TEXT_BODY.textSizeSp + 2f
        setTextColor(ContextCompat.getColor(context, R.color.app_primary))
        gravity = Gravity.END
        visibility = GONE
    }

    private val container: LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.TOP
    }

    /** 垂直承载正文行与 AI 预览区，便于在块内展示预览而不打断 RecyclerView 单块结构。 */
    private val mainColumn: LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
    }

    private val aiSection: LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        visibility = GONE
        setPadding(
            EditorStyle.dpToPx(context, 12),
            EditorStyle.dpToPx(context, 8),
            EditorStyle.dpToPx(context, 12),
            EditorStyle.dpToPx(context, 4)
        )
        background = GradientDrawable().apply {
            setColor(Color.parseColor("#0D000000"))
            cornerRadius = EditorStyle.dpToPx(context, 8).toFloat()
        }
    }

    private val aiProgress: ProgressBar = ProgressBar(context).apply {
        isIndeterminate = true
        visibility = GONE
    }

    private val aiPreviewLabel: TextView = TextView(context).apply {
        textSize = 12f
        setTextColor(ContextCompat.getColor(context, R.color.gray))
        setText(R.string.ai_polish_preview_label)
        visibility = GONE
    }

    private val aiPreviewText: TextView = TextView(context).apply {
        textSize = EditorStyle.TEXT_BODY.textSizeSp
        setTextColor(ContextCompat.getColor(context, R.color.app_primary))
        visibility = GONE
    }

    private val aiErrorText: TextView = TextView(context).apply {
        textSize = 14f
        setTextColor(Color.parseColor("#B00020"))
        visibility = GONE
    }

    private val aiButtonRow: LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        visibility = GONE
    }

    // 使用 AppCompatButton：避免在代码里用 Material attr 构造 MaterialButton 时因 Context/主题解析异常导致闪退
    private val btnAiAccept: AppCompatButton = AppCompatButton(context).apply {
        text = context.getString(R.string.ai_polish_accept)
        textSize = 13f
        visibility = GONE
    }

    private val btnAiRetry: AppCompatButton = AppCompatButton(context).apply {
        text = context.getString(R.string.ai_polish_retry)
        textSize = 13f
        visibility = GONE
    }

    private val btnAiDiscard: AppCompatButton = AppCompatButton(context).apply {
        text = context.getString(R.string.ai_polish_discard)
        textSize = 13f
        visibility = GONE
    }

    /** 焦点且非空、且当前无进行中的 AI 会话时展示；避免与软键盘 idle 防抖抢交互。 */
    private val sparkleFab: TextView = TextView(context).apply {
        text = "✨"
        textSize = 18f
        contentDescription = context.getString(R.string.ai_polish_sparkle_cd)
        visibility = GONE
        setPadding(
            EditorStyle.dpToPx(context, 6),
            EditorStyle.dpToPx(context, 4),
            EditorStyle.dpToPx(context, 6),
            EditorStyle.dpToPx(context, 4)
        )
        background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.parseColor("#E8F4FD"))
        }
    }

    private val normalTextColor: Int
    private val quoteTextColor: Int = ContextCompat.getColor(context, R.color.richtext_quote_text)

    init {
        applyTextStyle(EditorStyle.TEXT_BODY)

        normalTextColor = editText.currentTextColor

        // 设置引用指示器的布局参数，使其高度填满父容器
        quoteIndicator.layoutParams = LinearLayout.LayoutParams(
            EditorStyle.dpToPx(context, 4),
            LayoutParams.MATCH_PARENT
        ).apply {
            rightMargin = EditorStyle.dpToPx(context, 1)
        }

        // 列表前缀容器内仅展示一个前缀视图：圆点或序号
        prefixContainer.addView(bulletView)
        prefixContainer.addView(numberView)

        container.addView(quoteIndicator)
        container.addView(prefixContainer)
        container.addView(
            editText,
            LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            )
        )

        val containerLp = LinearLayout.LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = EditorStyle.dpToPx(context, EditorStyle.TEXT_BODY.blockMarginDp)
            bottomMargin = EditorStyle.dpToPx(context, EditorStyle.TEXT_BODY.blockMarginDp)
        }
        mainColumn.addView(container, containerLp)

        aiSection.addView(aiProgress, LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
        aiSection.addView(aiPreviewLabel, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        aiSection.addView(aiPreviewText, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        aiSection.addView(aiErrorText, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

        val btnLp = LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply {
            marginEnd = EditorStyle.dpToPx(context, 4)
        }
        aiButtonRow.addView(btnAiAccept, LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply {
            marginEnd = EditorStyle.dpToPx(context, 4)
        })
        aiButtonRow.addView(btnAiRetry, btnLp)
        aiButtonRow.addView(btnAiDiscard, LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        aiSection.addView(aiButtonRow, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            topMargin = EditorStyle.dpToPx(context, 8)
        })

        mainColumn.addView(aiSection, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

        addView(
            mainColumn,
            FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        )
        addView(
            sparkleFab,
            FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.BOTTOM or Gravity.END
                marginEnd = EditorStyle.dpToPx(context, 4)
                bottomMargin = EditorStyle.dpToPx(context, 4)
            }
        )
    }

    /**
     * 只读态或解绑时关闭 AI 控件，避免复用时残留。
     */
    fun resetAiAssistUi() {
        sparkleFab.visibility = GONE
        aiSection.visibility = GONE
        aiProgress.visibility = GONE
        aiPreviewLabel.visibility = GONE
        aiPreviewText.visibility = GONE
        aiErrorText.visibility = GONE
        aiButtonRow.visibility = GONE
        btnAiAccept.visibility = GONE
        btnAiRetry.visibility = GONE
        btnAiDiscard.visibility = GONE
    }

    /**
     * @param showSparkle 当前块获得焦点且正文非空、且 AI 为 Idle 时显示入口。
     */
    fun bindAiAssistUi(
        showSparkle: Boolean,
        state: AiAssistUiState,
        onSparkleClick: () -> Unit,
        onAccept: () -> Unit,
        onRetry: () -> Unit,
        onDiscard: () -> Unit,
    ) {
        sparkleFab.setOnClickListener { onSparkleClick() }
        btnAiAccept.setOnClickListener { onAccept() }
        btnAiRetry.setOnClickListener { onRetry() }
        btnAiDiscard.setOnClickListener { onDiscard() }

        sparkleFab.visibility = if (showSparkle) VISIBLE else GONE

        when (state) {
            is AiAssistUiState.Idle -> {
                aiSection.visibility = GONE
                aiProgress.visibility = GONE
                aiPreviewLabel.visibility = GONE
                aiPreviewText.visibility = GONE
                aiErrorText.visibility = GONE
                aiButtonRow.visibility = GONE
            }
            is AiAssistUiState.Loading -> {
                aiSection.visibility = VISIBLE
                aiProgress.visibility = VISIBLE
                aiPreviewLabel.visibility = GONE
                aiPreviewText.visibility = GONE
                aiErrorText.visibility = GONE
                aiButtonRow.visibility = GONE
            }
            is AiAssistUiState.Streaming -> {
                aiSection.visibility = VISIBLE
                aiProgress.visibility = GONE
                aiPreviewLabel.visibility = VISIBLE
                aiPreviewText.visibility = VISIBLE
                aiPreviewText.text = state.accumulatedText
                aiErrorText.visibility = GONE
                aiButtonRow.visibility = GONE
            }
            is AiAssistUiState.Preview -> {
                aiSection.visibility = VISIBLE
                aiProgress.visibility = GONE
                aiPreviewLabel.visibility = VISIBLE
                aiPreviewText.visibility = VISIBLE
                aiPreviewText.text = state.text
                aiErrorText.visibility = GONE
                aiButtonRow.visibility = VISIBLE
                btnAiAccept.visibility = VISIBLE
                btnAiRetry.visibility = VISIBLE
                btnAiDiscard.visibility = VISIBLE
            }
            is AiAssistUiState.Error -> {
                aiSection.visibility = VISIBLE
                aiProgress.visibility = GONE
                aiPreviewLabel.visibility = GONE
                aiPreviewText.visibility = GONE
                aiErrorText.visibility = VISIBLE
                aiErrorText.text = state.message
                aiButtonRow.visibility = VISIBLE
                btnAiAccept.visibility = GONE
                btnAiRetry.visibility = VISIBLE
                btnAiDiscard.visibility = VISIBLE
            }
        }
    }

    /**
     * 更新引用样式。连续引用块之间 top/bottom margin 为 0，让灰色竖线视觉上连成一线。
     * @param prevIsQuote 上一个块是否为引用块
     * @param nextIsQuote 下一个块是否为引用块
     */
    fun updateQuoteStyle(isQuote: Boolean, prevIsQuote: Boolean = false, nextIsQuote: Boolean = false) {
        isQuoteMode = isQuote

        if (isQuote) {
            applyTextStyle(EditorStyle.TEXT_QUOTE)
            editText.setTextColor(quoteTextColor)
            quoteIndicator.visibility = VISIBLE

            // 仅用 container 的 margin 控制间距，避免与根 layoutParams 重复导致间距翻倍
            val quoteMargin = EditorStyle.dpToPx(context, EditorStyle.TEXT_QUOTE.blockMarginDp)
            (container.layoutParams as? MarginLayoutParams)?.apply {
                leftMargin = EditorStyle.dpToPx(context, 25)
                topMargin = if (prevIsQuote) 0 else quoteMargin
                bottomMargin = if (nextIsQuote) 0 else quoteMargin
            }

            requestLayout()
        } else {
            applyTextStyle(
                if (currentListType == null) EditorStyle.TEXT_BODY else EditorStyle.TEXT_LIST
            )
            editText.setTextColor(normalTextColor)
            quoteIndicator.visibility = GONE

            val bodyMargin = EditorStyle.dpToPx(context, EditorStyle.TEXT_BODY.blockMarginDp)
            (container.layoutParams as? MarginLayoutParams)?.apply {
                leftMargin = 0
                topMargin = bodyMargin
                bottomMargin = bodyMargin
            }

            requestLayout()
        }
    }

    fun updateListStyle(listType: EditorBlock.ListType?, orderIndex: Int) {
        // 标题块不展示列表前缀与缩进，直接忽略列表样式
        if (isHeadingMode) {
            bulletView.visibility = GONE
            numberView.visibility = GONE
            val params = prefixContainer.layoutParams as? LinearLayout.LayoutParams
            if (params != null) {
                params.width = 0
                params.rightMargin = 0
                prefixContainer.layoutParams = params
            }
            return
        }

        currentListType = listType
        // 仅列表块时给前缀容器占位（缩进 + 与文字间距），非列表时宽度为 0 不缩进
        val params = prefixContainer.layoutParams as? LinearLayout.LayoutParams
        if (params != null) {
            if (listType != null) {
                params.width = listPrefixWidthPx
                params.rightMargin = listPrefixGapPx
            } else {
                params.width = 0
                params.rightMargin = 0
            }
            prefixContainer.layoutParams = params
        }

        when (listType) {
            EditorBlock.ListType.BULLET_LIST -> {
                bulletView.visibility = VISIBLE
                numberView.visibility = GONE
                if (!isQuoteMode) {
                    applyTextStyle(EditorStyle.TEXT_LIST)
                }
            }
            EditorBlock.ListType.NUMBER_LIST -> {
                bulletView.visibility = GONE
                numberView.visibility = VISIBLE
                numberView.text = if (orderIndex > 0) "$orderIndex." else "1."
                if (!isQuoteMode) {
                    applyTextStyle(EditorStyle.TEXT_LIST)
                }
            }
            null -> {
                bulletView.visibility = GONE
                numberView.visibility = GONE
                if (!isQuoteMode) {
                    applyTextStyle(EditorStyle.TEXT_BODY)
                }
            }
        }
    }

    /**
     * 标题样式开关：
     * - 开启时：使用标题字号 22sp、加粗、上下 12dp padding，深色文字，并强制隐藏列表前缀与缩进
     * - 关闭时：恢复为正文/列表当前样式（由 updateListStyle + updateQuoteStyle 决定）
     */
    fun updateHeadingStyle(isHeading: Boolean) {
        isHeadingMode = isHeading
        val params = prefixContainer.layoutParams as? LinearLayout.LayoutParams

        if (isHeading) {
            // 标题不展示任何列表前缀与缩进
            bulletView.visibility = GONE
            numberView.visibility = GONE
            if (params != null) {
                params.width = 0
                params.rightMargin = 0
                prefixContainer.layoutParams = params
            }

            // 应用标题排版与颜色
            editText.textSize = EditorStyle.TEXT_TITLE.textSizeSp
            editText.setTypeface(editText.typeface, Typeface.BOLD)
            editText.setTextColor(Color.parseColor("#1A1A1A"))
            editText.hint = "请输入标题..."
            editText.setHintTextColor(Color.parseColor("#661A1A1A"))
            editText.background = null

            val style = EditorStyle.TEXT_TITLE
            editText.setPadding(
                EditorStyle.dpToPx(context, style.paddingDp.left),
                EditorStyle.dpToPx(context, 12),
                EditorStyle.dpToPx(context, style.paddingDp.right),
                EditorStyle.dpToPx(context, 12)
            )
            editText.setLineSpacing(
                EditorStyle.dpToPx(context, style.lineSpacingExtraDp).toFloat(),
                style.lineSpacingMultiplier
            )
        } else {
            // 关闭标题样式后，交回给引用/列表逻辑控制外观
            // 使用正文字号与行距，颜色恢复为普通文本色
            val body = EditorStyle.TEXT_BODY
            editText.hint = null
            editText.textSize = body.textSizeSp
            editText.typeface = Typeface.DEFAULT
            editText.setTextColor(normalTextColor)
            editText.setLineSpacing(
                EditorStyle.dpToPx(context, body.lineSpacingExtraDp).toFloat(),
                body.lineSpacingMultiplier
            )
            // padding 由 updateQuoteStyle / updateListStyle 再次调用 applyTextStyle 时覆盖
        }
    }

    private fun applyTextStyle(style: EditorStyle.TextBlockStyle) {
        editText.setPadding(
            EditorStyle.dpToPx(context, style.paddingDp.left),
            EditorStyle.dpToPx(context, style.paddingDp.top),
            EditorStyle.dpToPx(context, style.paddingDp.right),
            EditorStyle.dpToPx(context, style.paddingDp.bottom)
        )
        editText.setLineSpacing(
            EditorStyle.dpToPx(context, style.lineSpacingExtraDp).toFloat(),
            style.lineSpacingMultiplier
        )
    }
}