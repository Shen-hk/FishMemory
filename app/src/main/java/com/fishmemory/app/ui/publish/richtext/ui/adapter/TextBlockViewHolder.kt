package com.fishmemory.app.ui.publish.richtext.ui.adapter

import androidx.recyclerview.widget.RecyclerView
import android.graphics.Color
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.view.inputmethod.EditorInfo
import com.fishmemory.app.ui.publish.richtext.core.model.EditorBlockDisplay
import com.fishmemory.app.ui.publish.richtext.core.model.EditorBlock
import com.fishmemory.app.ui.publish.richtext.ui.view.TextBlockView
import com.fishmemory.app.ui.publish.richtext.ui.actions.TextBlockActions

class TextBlockViewHolder(
    val blockView: TextBlockView,
    private val actions: TextBlockActions = TextBlockActions()
) : RecyclerView.ViewHolder(blockView) {

    private var textWatcher: TextWatcher? = null
    private var currentBlock: EditorBlock.TextBlock? = null

    fun bind(block: EditorBlock.TextBlock, prevIsQuote: Boolean = false, nextIsQuote: Boolean = false) {
        currentBlock = block
        val et = blockView.editText
        et.blockId = block.id
            
        // 确保数据源同步
        if (et.text !== block.text) {
            et.setText(block.text)
        }
            
        // 确保为 SpannableStringBuilder 类型
        actions.ensureSpannableStringBuilder(et, block)

        blockView.updateQuoteStyle(
            isQuote = block.isQuote,
            prevIsQuote = prevIsQuote,
            nextIsQuote = nextIsQuote
        )
        blockView.updateHeadingStyle(block.isHeading)
        blockView.updateListStyle(block.listType, block.orderIndex)

        // 行内链接点击需要 LinkMovementMethod
        et.movementMethod = LinkMovementMethod.getInstance()
        et.highlightColor = Color.TRANSPARENT

        // 使用 Action 类添加 URL 自动识别监听器
        textWatcher?.let { et.removeTextChangedListener(it) }
        textWatcher = actions.attachUrlAutoDetectListener(
            editText = et,
            blockId = block.id,
            onContentChanged = { id ->
                blockView.editText.interactionListener?.onContentChanged(id)
            },
            interactionListener = blockView.editText.interactionListener
        )
    }

    /** 只读态：同布局展示文本，禁用输入但保留选择/复制能力。 */
    fun bindReadOnly(display: EditorBlockDisplay.Text) {
        currentBlock = null
        val et = blockView.editText
        et.blockId = display.id
        textWatcher?.let { et.removeTextChangedListener(it) }
        textWatcher = null

        // 配置只读态：禁输入 + 可选择/复制 + 多行
        et.keyListener = null
        et.inputType = EditorInfo.TYPE_NULL
        et.setSingleLine(false)
        et.isVerticalScrollBarEnabled = true
        et.setTextIsSelectable(true)
        et.isCursorVisible = false
        et.isFocusable = false
        et.isFocusableInTouchMode = false
        et.isLongClickable = true

        et.setText(display.content)

        // 样式更新
        blockView.updateQuoteStyle(display.isQuote, false, false)
        blockView.updateHeadingStyle(display.isHeading)
        blockView.updateListStyle(display.listType, display.orderIndex)
    }
}
