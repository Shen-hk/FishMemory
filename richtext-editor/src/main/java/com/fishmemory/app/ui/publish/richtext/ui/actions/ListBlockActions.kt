package com.fishmemory.app.ui.publish.richtext.ui.actions

import android.widget.LinearLayout
import com.fishmemory.app.ui.publish.richtext.ui.view.TextBlockView
import com.fishmemory.app.ui.publish.richtext.core.model.EditorBlock
import com.fishmemory.app.ui.publish.richtext.core.model.EditorBlockDisplay

/**
 * 列表块 UI 操作助手类：封装列表项的创建、样式配置等 UI 操作。
 */
class ListBlockActions {

    /**
     * 创建并配置列表项视图
     */
    fun createListItemView(
        container: LinearLayout,
        item: EditorBlockDisplay.ListBlock.ListItem,
        index: Int,
        listType: String?
    ): TextBlockView {
        val textBlockView = TextBlockView(container.context)
        
        // 配置为只读模式
        val et = textBlockView.editText
        ReadOnlyEditTextConfigurator.configureMultilineSelectable(et)
        
        // 设置文本内容
        et.setText(item.content)
        
        // 应用列表样式
        val editorListType = when (listType) {
            "bullet" -> EditorBlock.ListType.BULLET_LIST
            "number" -> EditorBlock.ListType.NUMBER_LIST
            else -> null
        }
        
        // 先设置列表类型，再调用 updateQuoteStyle 让它根据 currentListType 应用正确的样式
        textBlockView.updateListStyle(editorListType, index + 1)
        textBlockView.updateQuoteStyle(false, false, false)
        
        return textBlockView
    }

    /**
     * 绑定列表块到容器
     */
    fun bindListBlock(
        container: LinearLayout,
        display: EditorBlockDisplay.ListBlock
    ) {
        container.removeAllViews()
        val sorted = display.items.sortedBy { listItem: EditorBlockDisplay.ListBlock.ListItem -> 
            listItem.order 
        }
        
        sorted.forEachIndexed { index: Int, item: EditorBlockDisplay.ListBlock.ListItem ->
            val listItemView = createListItemView(container, item, index, display.listType)
            container.addView(listItemView)
        }
    }
}
