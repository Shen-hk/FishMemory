package com.fishmemory.app.ui.publish.richtext.api

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fishmemory.app.ui.publish.richtext.core.model.EditorBlockDisplay
import com.fishmemory.app.ui.publish.richtext.ui.adapter.EditorAdapter
import com.fishmemory.app.ui.publish.richtext.ui.adapter.VideoBlockViewHolder

/**
 * Facade for rendering rich-text documents in read-only screens.
 *
 * Host screens should not need to know about EditorAdapter or concrete ViewHolder types.
 */
class ReadOnlyRichTextRenderer(
    private val recyclerView: RecyclerView,
) {

    private val adapter = EditorAdapter(readOnlyBlocks = emptyList())
    private val imageBlockUrls = mutableMapOf<String, String>()

    var onImagePreviewRequested: ((url: String) -> Unit)? = null

    init {
        recyclerView.layoutManager = LinearLayoutManager(recyclerView.context).apply {
            isAutoMeasureEnabled = true
        }
        recyclerView.adapter = adapter
        recyclerView.setHasFixedSize(false)
        recyclerView.isFocusable = false

        adapter.onImageBlockPreviewRequested = { blockId ->
            imageBlockUrls[blockId]?.takeIf { it.isNotBlank() }?.let { url ->
                onImagePreviewRequested?.invoke(url)
            }
        }
    }

    fun renderStandardJson(json: String): Boolean {
        val blocks = RichTextDocumentCodec.parseReadOnlyBlocks(json)
        renderBlocks(blocks)
        return blocks.isNotEmpty()
    }

    fun renderBlocks(blocks: List<EditorBlockDisplay>) {
        imageBlockUrls.clear()
        blocks.filterIsInstance<EditorBlockDisplay.Image>()
            .filter { it.url.isNotBlank() }
            .forEach { imageBlockUrls[it.id] = it.url }

        adapter.setReadOnlyBlocks(blocks)
    }

    fun scrollToTop() {
        recyclerView.scrollToPosition(0)
    }

    fun pauseVisibleVideoPlayback() {
        for (index in 0 until recyclerView.childCount) {
            val child = recyclerView.getChildAt(index)
            val viewHolder = recyclerView.getChildViewHolder(child)
            if (viewHolder is VideoBlockViewHolder) {
                viewHolder.pausePlayback()
            }
        }
    }
}
