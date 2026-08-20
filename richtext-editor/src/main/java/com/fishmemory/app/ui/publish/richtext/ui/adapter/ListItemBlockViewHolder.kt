package com.fishmemory.app.ui.publish.richtext.ui.adapter

import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import com.fishmemory.richeditor.R
import com.fishmemory.app.ui.publish.richtext.core.model.EditorBlockDisplay
import com.fishmemory.app.ui.publish.richtext.ui.actions.ListBlockActions

/**
 * 只读列表块：使用与编辑态一致的 TextBlockView 展示列表项。
 * 编辑态下列表由多个 TextBlock 的 listType 表示，故仅只读模式使用本 ViewHolder。
 */
class ListItemBlockViewHolder(
    private val container: LinearLayout,
    private val actions: ListBlockActions
) : RecyclerView.ViewHolder(container) {

    fun bindReadOnly(display: EditorBlockDisplay.ListBlock) {
        actions.bindListBlock(container, display)
    }

    companion object {
        fun create(parent: ViewGroup, actions: ListBlockActions): ListItemBlockViewHolder {
            val root = LinearLayout(parent.context).apply {
                id = R.id.containerList
                orientation = LinearLayout.VERTICAL
            }
            return ListItemBlockViewHolder(root, actions)
        }
    }
}
