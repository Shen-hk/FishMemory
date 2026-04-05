package com.fishmemory.app.ui.publish.richtext.ui.actions

import android.text.Editable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextWatcher
import android.widget.EditText
import com.fishmemory.app.ui.publish.richtext.business.link.LinkSpan
import com.fishmemory.app.ui.publish.richtext.core.BlockInteractionListener

import com.fishmemory.app.ui.publish.richtext.core.model.EditorBlock
import com.fishmemory.app.ui.publish.richtext.core.engine.validator.EditorUrlRules
import kotlin.math.max

/**
 * 文本块 UI 操作助手类：封装文本块的创建、配置、URL 识别等 UI 操作。
 */
class TextBlockActions {

    /**
     * 为 EditText 添加 URL 自动识别监听器
     * @param editText 目标 EditText
     * @param blockId 块 ID
     * @param onContentChanged 内容变化回调
     * @param interactionListener 交互监听器，用于处理回车分裂等操作
     * @return 返回 TextWatcher 供后续移除使用
     */
    fun attachUrlAutoDetectListener(
        editText: EditText,
        blockId: String,
        onContentChanged: (String) -> Unit,
        interactionListener: BlockInteractionListener? = null
    ): TextWatcher {
        editText.tag = blockId
        
        val textWatcher = object : TextWatcher {
            private var imeInsertedEnter = false
            
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s !is Editable) return
                if (before == 0 && count == 0) return
                
                // 检测是否是回车键插入（IME 的 commitText）
                if (count == 1 && before == 0 && s?.getOrNull(start) == '\n') {
                    imeInsertedEnter = true
                }
                
                val changeEnd = (start + max(before, count)).coerceAtMost(s.length)
                
                // 任何对链接范围内的编辑，都立刻取消链接属性
                val spans = s.getSpans(start, changeEnd, LinkSpan::class.java)
                spans.forEach { span -> s.removeSpan(span) }
                
                // 标记内容变化
                onContentChanged(blockId)
            }
            
            override fun afterTextChanged(s: Editable?) {
                if (s == null) return
                val len = s.length
                if (len == 0) return
                
                val cursor = editText.selectionStart.coerceIn(0, len)
                if (cursor <= 0) return
                
                val lastChar = s[cursor - 1]
                
                // 回车兜底：软键盘回车通常不会走 onKeyDown(KEYCODE_ENTER)，这里补一条
                if (lastChar == '\n' && cursor == len && imeInsertedEnter) {
                    val handled = handleEnterKey(editText, s, cursor, interactionListener)
                    if (handled) {
                        // 已被结构化操作消费：移除本次插入的换行，避免残留
                        val removeAt = cursor - 1
                        if (removeAt in 0 until s.length && s[removeAt] == '\n') {
                            s.delete(removeAt, removeAt + 1)
                        }
                    }
                    imeInsertedEnter = false
                }
                
                // 空格触发 URL 识别
                if (lastChar != ' ') return
                
                detectAndApplyLinkSpan(s, cursor)
            }
        }
        
        editText.addTextChangedListener(textWatcher)
        return textWatcher
    }
    
    /**
     * 处理回车键逻辑：尝试分裂块或将 URL 转为卡片
     * @return true 表示回车已被消费（需要删除换行符），false 表示允许系统默认行为
     */
    private fun handleEnterKey(
        editText: EditText,
        s: Editable,
        cursor: Int,
        interactionListener: BlockInteractionListener?
    ): Boolean {
        if (interactionListener == null) return false
        
        val blockId = editText.tag as? String ?: return false
        
        // 先尝试特殊回车处理（如 URL 行转卡片）
        val enterHandled = try {
            interactionListener.onEnterRequested(blockId, cursor)
        } catch (e: Exception) {
            false
        }
        
        if (enterHandled) {
            // URL 转卡片成功，消费事件
            return true
        }
        
        // 再尝试分裂块
        val splitResult = try {
            interactionListener.onSplitRequested(blockId, cursor)
        } catch (e: Exception) {
            null
        }
        
        if (splitResult != null) {
            // 分裂成功，消费事件
            return true
        }
        
        return false
    }

    /**
     * 检测并应用 LinkSpan
     */
    private fun detectAndApplyLinkSpan(s: Editable, cursor: Int) {
        try {
            val tokenEnd = (cursor - 1).coerceIn(0, s.length - 1)
            var tokenStart = tokenEnd - 1
            
            while (tokenStart >= 0) {
                val ch = s[tokenStart]
                if (ch == ' ' || ch == '\n' || ch == '\t' || ch == '\r') break
                tokenStart--
            }
            tokenStart++
            
            if (tokenStart >= tokenEnd) return
            
            val token = s.subSequence(tokenStart, tokenEnd).toString().trim()
            if (token.isEmpty()) return
            
            // 避免重复套 span
            val existing = s.getSpans(tokenStart, tokenEnd, LinkSpan::class.java)
            if (existing.isNotEmpty()) return
            
            // 验证 URL 前先捕获异常
            val isValidUrl = try {
                EditorUrlRules.isValidUrlCandidate(token)
            } catch (e: Exception) {
                false
            }
            
            if (!isValidUrl) return
            
            runCatching {
                s.setSpan(LinkSpan(token), tokenStart, tokenEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        } catch (e: Exception) {
            // URL 识别失败不影响输入
        }
    }
    
    /**
     * 确保文本为 SpannableStringBuilder 类型
     */
    fun ensureSpannableStringBuilder(editText: EditText, block: EditorBlock.TextBlock) {
        val editable = editText.text
        when (editable) {
            is SpannableStringBuilder -> {
                if (block.text !== editable) block.text = editable
            }
            null -> {
                val sb = SpannableStringBuilder()
                editText.setText(sb)
                block.text = sb
            }
            else -> {
                val sb = SpannableStringBuilder(editable)
                editText.setText(sb)
                block.text = sb
            }
        }
    }
}
