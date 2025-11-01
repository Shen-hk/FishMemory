package com.fishmemory.app.ui.publish.richtext.ui.actions

import android.text.method.ArrowKeyMovementMethod
import android.widget.EditText

/**
 * 只读态 EditText 配置器：保留长按复制/选择能力，同时禁用输入与焦点编辑。
 */
object ReadOnlyEditTextConfigurator {

    fun configureMultilineSelectable(editText: EditText) {
        editText.isEnabled = true
        editText.isFocusable = false
        editText.isFocusableInTouchMode = false
        editText.isCursorVisible = false
        editText.isLongClickable = true
        editText.setTextIsSelectable(true)
        editText.movementMethod = ArrowKeyMovementMethod.getInstance()
        editText.keyListener = null
    }
}

