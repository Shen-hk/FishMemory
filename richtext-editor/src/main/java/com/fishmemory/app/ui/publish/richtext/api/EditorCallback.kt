package com.fishmemory.app.ui.publish.richtext.api

import android.view.View

/**
 * Host callbacks for editor events.
 *
 * All methods have default implementations so host apps can override only the
 * events they need. Returning true from [onInlineLinkClicked] means the host has
 * handled the link event and the editor should skip its default link menu.
 */
interface EditorCallback {

    fun onContentChanged() = Unit

    fun onTextBlockFocusChanged(blockId: String?) = Unit

    fun onInlineLinkClicked(
        blockId: String,
        url: String,
        start: Int,
        end: Int,
    ): Boolean = false

    fun onImagePreviewRequested(url: String) = Unit

    fun onImageMenuRequested(anchorView: View, blockId: String) = Unit

    fun onImageReplaceRequested(blockId: String) = Unit

    fun onVideoClicked(blockId: String) = Unit

    fun onAiPolishRequested(blockId: String) = Unit

    fun onAiPreviewAccepted(blockId: String) = Unit

    fun onAiRetryRequested(blockId: String) = Unit

    fun onAiPreviewDiscarded(blockId: String) = Unit
}
