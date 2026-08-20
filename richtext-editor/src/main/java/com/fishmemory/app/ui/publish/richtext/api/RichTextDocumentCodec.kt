package com.fishmemory.app.ui.publish.richtext.api

import com.fishmemory.app.ui.publish.richtext.core.converter.CodeBlock
import com.fishmemory.app.ui.publish.richtext.core.converter.HeadingBlock
import com.fishmemory.app.ui.publish.richtext.core.converter.HrBlock
import com.fishmemory.app.ui.publish.richtext.core.converter.ImageBlock
import com.fishmemory.app.ui.publish.richtext.core.converter.LinkCardBlock
import com.fishmemory.app.ui.publish.richtext.core.converter.ListBlock
import com.fishmemory.app.ui.publish.richtext.core.converter.StandardBlockToDisplay
import com.fishmemory.app.ui.publish.richtext.core.converter.StandardDocument
import com.fishmemory.app.ui.publish.richtext.core.converter.StandardJsonExporter
import com.fishmemory.app.ui.publish.richtext.core.converter.StandardJsonParser
import com.fishmemory.app.ui.publish.richtext.core.converter.TextBlock
import com.fishmemory.app.ui.publish.richtext.core.converter.TextFormat
import com.fishmemory.app.ui.publish.richtext.core.converter.VideoBlock
import com.fishmemory.app.ui.publish.richtext.core.model.Document
import com.fishmemory.app.ui.publish.richtext.core.model.EditorBlockDisplay
import com.fishmemory.app.ui.publish.richtext.core.model.EditorBlockEntity
import com.fishmemory.app.ui.publish.richtext.core.model.SpanData
import com.fishmemory.app.ui.publish.richtext.core.model.SpanType

/**
 * Public JSON facade for SDK consumers.
 *
 * Keeps parser/exporter implementation details behind a small stable entry point.
 */
object RichTextDocumentCodec {

    fun exportStandardJson(document: Document): String {
        return StandardJsonExporter.toStandardJson(document)
    }

    fun parseStandardJson(json: String): StandardDocument? {
        return StandardJsonParser.parse(json)
    }

    fun parseEditableDocument(json: String): Document? {
        val document = parseStandardJson(json) ?: return null
        return Document(
            title = document.title,
            blocks = document.blocks.mapNotNull { block ->
                when (block) {
                    is TextBlock -> EditorBlockEntity.Text(
                        content = block.data.content,
                        spans = block.data.formats.toSpanData(),
                    )

                    is HeadingBlock -> EditorBlockEntity.Text(
                        content = block.data.content,
                        spans = block.data.formats.toSpanData(),
                        isHeading = true,
                    )

                    is ImageBlock -> EditorBlockEntity.Image(
                        url = block.data.url,
                        caption = block.data.caption,
                        isLocal = block.data.isLocal,
                        uploadState = block.data.uploadState.toUploadState(),
                        alignment = block.data.alignment.uppercase(),
                        sizeMode = block.data.sizeMode.uppercase(),
                    )

                    is VideoBlock -> EditorBlockEntity.Video(
                        url = block.data.url,
                        coverUrl = block.data.coverUrl ?: block.data.thumbnail,
                        durationMs = block.data.durationMs,
                        uploadState = block.data.uploadState.toUploadState(),
                    )

                    is CodeBlock -> EditorBlockEntity.Code(
                        language = block.data.language,
                        code = block.data.code,
                    )

                    is LinkCardBlock -> EditorBlockEntity.LinkCard(
                        url = block.data.url,
                        title = block.data.title,
                        description = block.data.description,
                        imageUrl = block.data.imageUrl,
                    )

                    is ListBlock -> EditorBlockEntity.Text(
                        content = block.data.items.joinToString(separator = "\n") { item ->
                            if (block.data.listType == "number") {
                                "${item.order}. ${item.content}"
                            } else {
                                "• ${item.content}"
                            }
                        },
                    )

                    is HrBlock -> EditorBlockEntity.Hr
                }
            }
        )
    }

    fun parseReadOnlyBlocks(json: String): List<EditorBlockDisplay> {
        val document = parseStandardJson(json) ?: return emptyList()
        return StandardBlockToDisplay.toDisplayList(document.blocks)
    }

    private fun List<TextFormat>.toSpanData(): List<SpanData> {
        return mapNotNull { format ->
            val type = when (format.type.lowercase()) {
                "bold" -> SpanType.BOLD
                "underline" -> SpanType.UNDERLINE
                "code" -> SpanType.CODE
                "link" -> SpanType.LINK
                else -> null
            } ?: return@mapNotNull null
            SpanData(
                start = format.start,
                end = format.end,
                type = type,
            )
        }
    }

    private fun String.toUploadState(): EditorBlockEntity.UploadState {
        return when (lowercase()) {
            "uploading" -> EditorBlockEntity.UploadState.UPLOADING
            "success" -> EditorBlockEntity.UploadState.SUCCESS
            "failed" -> EditorBlockEntity.UploadState.FAILED
            else -> EditorBlockEntity.UploadState.PENDING
        }
    }
}
