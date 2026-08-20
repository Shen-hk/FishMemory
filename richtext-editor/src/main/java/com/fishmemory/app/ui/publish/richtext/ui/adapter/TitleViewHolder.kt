package com.fishmemory.app.ui.publish.richtext.ui.adapter

import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.recyclerview.widget.RecyclerView
import com.fishmemory.richeditor.R
import com.fishmemory.app.ui.publish.richtext.ui.actions.TitleActions

/**
 * 作为 RecyclerView 第一个 item 的标题块：
 * - 单行，最大长度 40
 * - 回车 / IME_ACTION_NEXT 时将焦点移动到正文第一个 TextBlock。
 */
class TitleViewHolder(
    private val root: View,
    private val etTitle: EditText,
    private val adapter: EditorAdapter,
    private val actions: TitleActions = TitleActions(adapter)
) : RecyclerView.ViewHolder(root) {

    private var textWatcher: TextWatcher? = null

    fun bind(
        title: String,
        maxLength: Int,
        onTitleChanged: (String) -> Unit,
        onNextRequested: () -> Unit
    ) {
        // 使用 Actions 类处理标题输入逻辑
        textWatcher?.let { etTitle.removeTextChangedListener(it) }
        textWatcher = actions.bindTitleInput(
            etTitle = etTitle,
            title = title,
            maxLength = maxLength,
            onTitleChanged = onTitleChanged,
            onNextRequested = onNextRequested
        )
    }

    companion object {
        fun create(parent: ViewGroup, adapter: EditorAdapter): TitleViewHolder {
            val root = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_title_block, parent, false)
            val etTitle: EditText = root.findViewById(R.id.etTitle)
            return TitleViewHolder(root, etTitle, adapter)
        }
    }
}

