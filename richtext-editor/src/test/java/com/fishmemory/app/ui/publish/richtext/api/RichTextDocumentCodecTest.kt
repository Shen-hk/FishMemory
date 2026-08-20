package com.fishmemory.app.ui.publish.richtext.api

import com.fishmemory.app.ui.publish.richtext.core.model.Document
import com.fishmemory.app.ui.publish.richtext.core.model.EditorBlockDisplay
import com.fishmemory.app.ui.publish.richtext.core.model.EditorBlockEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RichTextDocumentCodecTest {

    @Test
    fun exportStandardJson_canBeParsedAsReadOnlyBlocks() {
        val document = Document(
            title = "SDK Demo",
            blocks = listOf(
                EditorBlockEntity.Text(content = "Hello SDK"),
                EditorBlockEntity.Code(language = "kotlin", code = "println(\"hi\")"),
                EditorBlockEntity.Image(
                    url = "https://example.com/image.png",
                    caption = "cover",
                    uploadState = EditorBlockEntity.UploadState.SUCCESS,
                ),
            )
        )

        val json = RichTextDocumentCodec.exportStandardJson(document)
        val parsed = RichTextDocumentCodec.parseStandardJson(json)
        val displayBlocks = RichTextDocumentCodec.parseReadOnlyBlocks(json)

        assertEquals("SDK Demo", parsed?.title)
        assertEquals(3, displayBlocks.size)
        assertTrue(displayBlocks[0] is EditorBlockDisplay.Text)
        assertTrue(displayBlocks[1] is EditorBlockDisplay.Code)
        assertTrue(displayBlocks[2] is EditorBlockDisplay.Image)
    }

    @Test
    fun exportStandardJson_canBeParsedAsEditableDocument() {
        val document = Document(
            title = "Editable",
            blocks = listOf(
                EditorBlockEntity.Text(content = "Hello SDK"),
                EditorBlockEntity.Text(content = "Heading", isHeading = true),
                EditorBlockEntity.Video(
                    url = "https://example.com/video.mp4",
                    coverUrl = "https://example.com/cover.png",
                    durationMs = 1200L,
                    uploadState = EditorBlockEntity.UploadState.SUCCESS,
                ),
                EditorBlockEntity.LinkCard(
                    url = "https://example.com",
                    title = "Example",
                    description = "Demo link",
                ),
            )
        )

        val json = RichTextDocumentCodec.exportStandardJson(document)
        val editable = RichTextDocumentCodec.parseEditableDocument(json)
        val blocks = requireNotNull(editable).blocks

        assertEquals("Editable", editable.title)
        assertEquals(4, blocks.size)
        assertEquals("Hello SDK", (blocks[0] as EditorBlockEntity.Text).content)
        assertTrue((blocks[1] as EditorBlockEntity.Text).isHeading)
        assertEquals(EditorBlockEntity.UploadState.SUCCESS, (blocks[2] as EditorBlockEntity.Video).uploadState)
        assertEquals("Example", (blocks[3] as EditorBlockEntity.LinkCard).title)
    }
}
