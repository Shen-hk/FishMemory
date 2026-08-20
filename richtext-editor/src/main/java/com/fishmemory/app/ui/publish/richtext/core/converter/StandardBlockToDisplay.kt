package com.fishmemory.app.ui.publish.richtext.core.converter

import com.fishmemory.app.ui.publish.richtext.core.model.EditorBlockDisplay

/**
 * StandardBlock（JSON 解析结果）转 BlockDisplay，供只读渲染与统一 Adapter 使用。
 */
object StandardBlockToDisplay {

    fun toDisplayList(blocks: List<StandardBlock>): List<EditorBlockDisplay> =
        blocks.mapNotNull { toDisplay(it) }

    fun toDisplay(block: StandardBlock): EditorBlockDisplay? = when (block) {
        is TextBlock -> EditorBlockDisplay.Text(
            id = block.id,
            content = block.data.content,
            isQuote = false,
            isHeading = false
        )
        is HeadingBlock -> EditorBlockDisplay.Text(
            id = block.id,
            content = block.data.content,
            isQuote = false,
            isHeading = true
        )
        is ImageBlock -> EditorBlockDisplay.Image(
            id = block.id,
            url = block.data.url,
            caption = block.data.caption
        )
        is CodeBlock -> EditorBlockDisplay.Code(
            id = block.id,
            code = block.data.code,
            language = block.data.language
        )
        is HrBlock -> EditorBlockDisplay.Hr(id = block.id)
        is LinkCardBlock -> EditorBlockDisplay.LinkCard(
            id = block.id,
            url = block.data.url,
            title = block.data.title,
            description = block.data.description,
            imageUrl = block.data.imageUrl
        )
        is VideoBlock -> EditorBlockDisplay.Video(
            id = block.id,
            url = block.data.url,
            coverUrl = block.data.coverUrl,
            durationMs = block.data.durationMs
        )
        is ListBlock -> EditorBlockDisplay.ListBlock(
            id = block.id,
            listType = block.data.listType,
            items = block.data.items.map { EditorBlockDisplay.ListBlock.ListItem(it.content, it.order) }
        )
        else -> null
    }
}
