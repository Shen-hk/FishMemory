package com.fishmemory.app.ui.publish.richtext.business.selection

import android.text.Spannable
import android.view.inputmethod.BaseInputConnection
import android.widget.EditText

/**
 * 选择逻辑的辅助工具
 * 输入法 composing 与光标位置判断，供 Block 级编辑（回车分裂、删除合并）前使用。
 */
object EditorSelectionHelper {

    /** 存在未上屏的 composing 文本时不做块级编辑。 */
    fun hasComposingText(et: EditText): Boolean {
        val text = et.text
        if (text !is Spannable) return false
        val start = BaseInputConnection.getComposingSpanStart(text)
        val end = BaseInputConnection.getComposingSpanEnd(text)
        return start != -1 && end != -1 && start != end
    }

    fun isCursorAtStart(et: EditText): Boolean =
        et.selectionStart == 0 && et.selectionEnd == 0
}