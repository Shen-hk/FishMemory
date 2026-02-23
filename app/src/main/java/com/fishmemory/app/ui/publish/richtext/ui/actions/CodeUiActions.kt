package com.fishmemory.app.ui.publish.richtext.ui.actions

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.annotation.UiThread
import com.fishmemory.app.ui.publish.richtext.core.engine.formatter.CodeHighlightEngine
import android.widget.EditText

/**
 * 代码块相关 UI 语义的协调者：代码高亮、复制到剪贴板等。
 *
 * 设计原则：
 * - 不持有任何 View 或 Model 的引用，只接收输入并执行操作。
 * - 将 [CodeBlockViewHolder] 中与 UI 交互相关的逻辑集中管理，提高复用性和可测试性。
 */
class CodeUiActions(
    private val context: Context
) {

    private val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager

    /**
     * 对指定的 EditText 内容应用语法高亮。
     * @param editText 要应用高亮的文本编辑框。
     * @param code 原始代码字符串。
     * @param language 编程语言。
     */
    @UiThread
    fun applyHighlight(editText: EditText, code: String, language: String) {
        val builder = CodeHighlightEngine.highlight(code, language)
        val selectionStart = editText.selectionStart.coerceAtLeast(0)
        val selectionEnd = editText.selectionEnd.coerceAtLeast(0)

        // 应用高亮后的文本
        editText.setText(builder)

        // 恢复光标位置
        val len = editText.text.length
        val start = selectionStart.coerceAtMost(len)
        val end = selectionEnd.coerceAtMost(len)
        if (start <= end) {
            editText.setSelection(start, end)
        } else {
            editText.setSelection(len)
        }

        // 请求滚动到光标位置
        val r = android.graphics.Rect()
        editText.getFocusedRect(r)
        editText.requestRectangleOnScreen(r, true)
    }

    /**
     * 复制代码到系统剪贴板。
     * @param code 要复制的代码内容。
     */
    fun copyCodeToClipboard(code: String) {
        clipboardManager?.let { cm ->
            val clip = ClipData.newPlainText("code", code)
            cm.setPrimaryClip(clip)
            Toast.makeText(context, "已复制代码", Toast.LENGTH_SHORT).show()
        }
    }
}
