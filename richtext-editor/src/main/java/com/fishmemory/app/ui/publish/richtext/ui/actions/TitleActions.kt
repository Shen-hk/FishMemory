package com.fishmemory.app.ui.publish.richtext.ui.actions

import android.text.Editable
import android.text.InputFilter
import android.text.Spanned
import android.text.TextWatcher
import android.view.KeyEvent
import android.widget.EditText
import android.view.inputmethod.EditorInfo
import com.fishmemory.app.ui.publish.richtext.ui.adapter.EditorAdapter

/**
 * 标题块的行为逻辑封装
 */
class TitleActions(
    private val adapter: EditorAdapter
) {
    /**
     * 为标题 EditText 绑定输入处理逻辑
     * @param etTitle 标题输入框
     * @param title 当前标题
     * @param maxLength 最大长度限制
     * @param onTitleChanged 标题变化回调
     * @param onNextRequested 下一个焦点请求回调
     * @return 返回 TextWatcher 供后续移除使用
     */
    fun bindTitleInput(
        etTitle: EditText,
        title: String,
        maxLength: Int,
        onTitleChanged: (String) -> Unit,
        onNextRequested: () -> Unit
    ): TextWatcher {
        // 初始化标题文本
        initializeTitleText(etTitle, title)
        
        // 设置单行和长度限制的过滤器
        setupTitleFilters(etTitle, maxLength)
        
        // 添加文本变化监听器
        val textWatcher = attachTitleChangeListener(
            editText = etTitle,
            onTitleChanged = onTitleChanged
        )
        
        // 设置 IME 动作（回车/Next）
        setupImeAction(etTitle, onNextRequested)
        
        return textWatcher
    }
    
    /**
     * 初始化标题文本，避免重复设置相同值
     */
    private fun initializeTitleText(etTitle: EditText, title: String) {
        val currentText = etTitle.text?.toString() ?: ""
        if (currentText != title) {
            etTitle.setText(title)
            etTitle.setSelection(etTitle.text.length)
        }
    }
    
    /**
     * 设置标题输入过滤器：单行 + 最大长度限制
     */
    private fun setupTitleFilters(etTitle: EditText, maxLength: Int) {
        etTitle.filters = arrayOf(object : InputFilter {
            override fun filter(
                source: CharSequence?,
                start: Int,
                end: Int,
                dest: Spanned?,
                dstart: Int,
                dend: Int
            ): CharSequence? {
                if (source.isNullOrEmpty()) return null
                
                // 移除换行符
                val builder = StringBuilder()
                for (i in start until end) {
                    val ch = source[i]
                    if (ch == '\n' || ch == '\r') continue
                    builder.append(ch)
                }
                
                val sanitized = builder.toString()
                if (sanitized.isEmpty()) return ""
                
                // 检查长度限制
                val destLength = dest?.length ?: 0
                val keep = maxLength - (destLength - (dend - dstart))
                if (keep <= 0) return ""
                
                return if (sanitized.length <= keep) {
                    sanitized
                } else {
                    sanitized.substring(0, keep)
                }
            }
        })
    }
    
    /**
     * 附加标题变化监听器
     */
    private fun attachTitleChangeListener(
        editText: EditText,
        onTitleChanged: (String) -> Unit
    ): TextWatcher {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            
            override fun afterTextChanged(s: Editable?) {
                val newText = s?.toString().orEmpty()
                // 同步更新 adapter.titleText，避免 RecyclerView 刷新时使用旧值
                adapter.titleText = newText
                onTitleChanged(newText)
            }
        }
        
        editText.addTextChangedListener(textWatcher)
        return textWatcher
    }
    
    /**
     * 设置 IME 动作：回车或 Next 时将焦点移动到正文
     */
    private fun setupImeAction(etTitle: EditText, onNextRequested: () -> Unit) {
        etTitle.imeOptions = EditorInfo.IME_ACTION_NEXT
        etTitle.setOnEditorActionListener { _, actionId, event ->
            val isEnterKey = event?.keyCode == KeyEvent.KEYCODE_ENTER &&
                    event.action == KeyEvent.ACTION_DOWN
            val isNextAction = actionId == EditorInfo.IME_ACTION_NEXT
            
            if (isEnterKey || isNextAction) {
                onNextRequested()
                true
            } else {
                false
            }
        }
    }
}
