package com.fishmemory.app.ui.publish.richtext.ui.adapter

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.SpannableStringBuilder
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.fishmemory.app.R

import com.fishmemory.app.ui.publish.richtext.core.model.EditorBlockDisplay
import com.fishmemory.app.ui.publish.richtext.core.model.EditorBlock
import com.fishmemory.app.ui.publish.richtext.ui.actions.CodeUiActions

class CodeBlockViewHolder(
    private val root: ConstraintLayout,
    private val etCode: EditText,
    private val tvLanguage: TextView,
    private val ivCopy: ImageView,
    private val ivDelete: ImageView
) : RecyclerView.ViewHolder(root) {

    private var block: EditorBlock.CodeBlock? = null
    private var onDeleteRequested: ((String) -> Unit)? = null
    private var onClickRequested: ((Int) -> Unit)? = null
    private var onBlockChanged: (() -> Unit)? = null

    private val handler = Handler(Looper.getMainLooper())
    private var highlightRunnable: Runnable? = null
    private var internalChange = false
    private var textWatcher: TextWatcher? = null
    private val originalKeyListener = etCode.keyListener
    
    private val codeUiActions = CodeUiActions(root.context)

    fun bind(
        block: EditorBlock.CodeBlock,
        isSelected: Boolean,
        onClicked: (Int) -> Unit,
        onDelete: (String) -> Unit,
        onFocusGained: (blockId: String) -> Unit,
        onBlockChanged: (() -> Unit)? = null
    ) {
        this.block = block
        this.onDeleteRequested = onDelete
        this.onClickRequested = onClicked
        this.onBlockChanged = onBlockChanged

        root.isSelected = isSelected
        ivDelete.visibility = if (isSelected) View.VISIBLE else View.GONE
        tvLanguage.text = block.language.trim().ifEmpty { "CODE" }.uppercase()

        setEditable(isSelected)

        etCode.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                onFocusGained(block.id)
            }
        }

        // 同步文本：保持 Editable 与 model.content 指向同一份实例
        if (etCode.text !== block.content) {
            internalChange = true
            etCode.setText(block.content)
            etCode.setSelection(etCode.text.length)
            internalChange = false
        }

        // 更新 selection 状态
        block.isSelected = isSelected

        etCode.setOnClickListener {
            if (!isSelected) {
               codeUiActions.copyCodeToClipboard(block.content.toString())
                return@setOnClickListener
            }
            // 选中态：允许编辑，点击不再触发复制
        }
        root.setOnClickListener {
            onClickRequested?.invoke(bindingAdapterPosition)
        }

        ivCopy.setOnClickListener { codeUiActions.copyCodeToClipboard(block.content.toString()) }

        ivDelete.setOnClickListener {
            val id = this.block?.id ?: return@setOnClickListener
            onDeleteRequested?.invoke(id)
        }

        // TextWatcher：输入时 300ms 防抖高亮
        textWatcher?.let { etCode.removeTextChangedListener(it) }
        textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (internalChange) return
                val b = this@CodeBlockViewHolder.block ?: return
                if (s == null) return

                // 将最新 Editable 保存回 model
                b.content = SpannableStringBuilder(s)
                onBlockChanged?.invoke()

                // 防抖：取消上一次，延迟 300ms 再渲染
                highlightRunnable?.let { handler.removeCallbacks(it) }
                val runnable = Runnable {
                   codeUiActions.applyHighlight(etCode, block.content.toString(), b.language)
                }
                highlightRunnable = runnable
                handler.postDelayed(runnable, 300)
            }
        }
        etCode.addTextChangedListener(textWatcher)

        // 首次绑定也做一次高亮
        codeUiActions.applyHighlight(etCode, block.content.toString(), block.language)
    }

    fun clear() {
        highlightRunnable?.let { handler.removeCallbacks(it) }
        highlightRunnable = null
        textWatcher?.let { etCode.removeTextChangedListener(it) }
        textWatcher = null
        block = null
        onDeleteRequested = null
        onClickRequested = null
    }

    fun requestFocusForEdit() {
        etCode.requestFocus()
        etCode.setSelection(etCode.text?.length ?: 0)
        val imm = root.context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.showSoftInput(etCode, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun setEditable(editable: Boolean) {
        if (editable) {
            etCode.isEnabled = true
            etCode.isFocusable = true
            etCode.isFocusableInTouchMode = true
            etCode.isCursorVisible = true
            etCode.keyListener = originalKeyListener
        } else {
            // 只读态：禁止输入/光标，但保留显示与滚动
            etCode.isEnabled = true
            etCode.isFocusable = false
            etCode.isFocusableInTouchMode = false
            etCode.isCursorVisible = false
            etCode.keyListener = null
            etCode.clearFocus()
        }
    }

    /** 只读态：展示代码与语言，禁用编辑、隐藏删除，但保留复制功能。 */
    fun bindReadOnly(display: EditorBlockDisplay.Code) {
        block = null
        textWatcher?.let { etCode.removeTextChangedListener(it) }
        textWatcher = null
        root.isSelected = false
        ivDelete.visibility = View.GONE
        tvLanguage.text = display.language.trim().ifEmpty { "CODE" }.uppercase()

        // 配置只读态：禁输入 + 可选择 + 水平滚动
        etCode.isFocusable = false
        etCode.isFocusableInTouchMode = false
        etCode.isCursorVisible = false
        etCode.keyListener = null
        etCode.inputType = EditorInfo.TYPE_NULL
        etCode.setSingleLine(false)
        etCode.setHorizontallyScrolling(true)
        etCode.setTextIsSelectable(true)

        internalChange = true
        etCode.setText(display.code)
        internalChange = false

        codeUiActions.applyHighlight(etCode, display.code, display.language)

        val copyLogic = {
            codeUiActions.copyCodeToClipboard(display.code)
        }

        root.setOnClickListener { copyLogic() }
        ivCopy.setOnClickListener { copyLogic() }
        ivDelete.setOnClickListener(null)
    }

    companion object {
        fun create(parent: ViewGroup): CodeBlockViewHolder {
            val root = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_code_block, parent, false) as ConstraintLayout
            val etCode: EditText = root.findViewById(R.id.etCode)
            val tvLanguage: TextView = root.findViewById(R.id.tvLanguage)
            val ivCopy: ImageView = root.findViewById(R.id.ivCopy)
            val ivDelete: ImageView = root.findViewById(R.id.ivDelete)
            return CodeBlockViewHolder(root, etCode, tvLanguage, ivCopy, ivDelete)
        }
    }
}

