package com.fishmemory.app.ui.publish.richtext.core.converter

import android.text.SpannableStringBuilder
import com.fishmemory.app.ui.publish.richtext.core.model.BlockIdGenerator
import com.fishmemory.app.ui.publish.richtext.core.engine.span.EditorSpanApplier
import com.fishmemory.app.ui.publish.richtext.core.engine.parser.EditorSpanParser
import com.fishmemory.app.ui.publish.richtext.core.model.EditorBlock
import com.fishmemory.app.ui.publish.richtext.core.model.EditorBlockEntity

/**
 * 编辑态文档 <-> 持久化实体 的转换器。
 *
 * 设计点：
 * - 仅负责“模型转换”，不触碰 RecyclerView/Adapter。
 * - 保证 getBlocks/setBlocks 的映射语义与原实现一致，避免行为漂移。
 */
object BlockDocumentConverter {

    fun toEditorBlockEntities(editorBlocks: List<EditorBlock>): List<EditorBlockEntity> {
        return editorBlocks.mapNotNull { block ->
            when (block) {
                is EditorBlock.TextBlock -> {
                    val type = when (block.listType) {
                        EditorBlock.ListType.BULLET_LIST -> EditorBlockEntity.Text.TextBlockType.BULLET_LIST
                        EditorBlock.ListType.NUMBER_LIST -> EditorBlockEntity.Text.TextBlockType.NUMBER_LIST
                        null -> EditorBlockEntity.Text.TextBlockType.TEXT
                    }
                    val orderIndex = if (block.listType == EditorBlock.ListType.NUMBER_LIST) {
                        block.orderIndex
                    } else 0

                    EditorBlockEntity.Text(
                        content = block.text.toString(),
                        spans = EditorSpanParser.parse(block.text),
                        blockType = type,
                        orderIndex = orderIndex,
                        isHeading = block.isHeading
                    )
                }

                is EditorBlock.ImageBlock -> EditorBlockEntity.Image(
                    url = block.remoteUrl ?: block.localUri.orEmpty(),
                    caption = block.caption,
                    isLocal = block.localUri != null,
                    uploadState = block.uploadState
                )

                is EditorBlock.VideoBlock -> {
                    // 提交时优先使用远程 URL，回退到本地 URI 以便草稿恢复
                    val url = block.remoteUrl ?: block.localUri.orEmpty()
                    if (url.isBlank()) return@mapNotNull null

                    EditorBlockEntity.Video(
                        url = url,
                        coverUrl = block.coverUrl,
                        durationMs = block.durationMs,
                        uploadState = block.uploadState
                    )
                }

                is EditorBlock.HrBlock -> EditorBlockEntity.Hr

                is EditorBlock.CodeBlock -> EditorBlockEntity.Code(
                    language = block.language,
                    code = block.content.toString()
                )

                is EditorBlock.LinkCard -> EditorBlockEntity.LinkCard(
                    url = block.url,
                    title = block.title,
                    description = block.description,
                    imageUrl = block.imageUrl
                )
            }
        }
    }

    fun toEditorBlocks(richBlocks: List<EditorBlockEntity>): List<EditorBlock> {
        return richBlocks.map { rb ->
            when (rb) {
                is EditorBlockEntity.Text -> {
                    val listType = when (rb.blockType) {
                        EditorBlockEntity.Text.TextBlockType.BULLET_LIST -> EditorBlock.ListType.BULLET_LIST
                        EditorBlockEntity.Text.TextBlockType.NUMBER_LIST -> EditorBlock.ListType.NUMBER_LIST
                        else -> null
                    }
                    val order = if (listType == EditorBlock.ListType.NUMBER_LIST) rb.orderIndex else 0

                    EditorBlock.TextBlock(
                        id = BlockIdGenerator.nextId(),
                        text = SpannableStringBuilder(rb.content).apply {
                            EditorSpanApplier.applySpans(this, rb.spans)
                        },
                        isQuote = false,
                        listType = listType,
                        orderIndex = order,
                        isHeading = rb.isHeading
                    )
                }

                is EditorBlockEntity.Image -> EditorBlock.ImageBlock(
                    id = BlockIdGenerator.nextId(),
                    localUri = if (rb.isLocal) rb.url else null,
                    remoteUrl = if (rb.isLocal) null else rb.url,
                    caption = rb.caption,
                    uploadState = rb.uploadState
                )

                is EditorBlockEntity.Video -> EditorBlock.VideoBlock(
                    id = BlockIdGenerator.nextId(),
                    localUri = null, // 反序列化时通常只持有远程 URL
                    remoteUrl = rb.url,
                    coverUrl = rb.coverUrl,
                    durationMs = rb.durationMs,
                    uploadState = rb.uploadState,
                    uploadProgress = if (rb.uploadState == EditorBlockEntity.UploadState.UPLOADING) 0 else 100,
                    playState = EditorBlock.PlayState.IDLE,
                    isSelected = false
                )

                is EditorBlockEntity.Code -> EditorBlock.CodeBlock(
                    id = BlockIdGenerator.nextId(),
                    content = SpannableStringBuilder(rb.code),
                    language = rb.language
                )

                is EditorBlockEntity.Hr -> EditorBlock.HrBlock(BlockIdGenerator.nextId())

                is EditorBlockEntity.LinkCard -> EditorBlock.LinkCard(
                    id = BlockIdGenerator.nextId(),
                    url = rb.url,
                    title = rb.title,
                    description = rb.description,
                    imageUrl = rb.imageUrl,
                    isLoading = false,
                    isSelected = false
                )
            }
        }
    }
}

