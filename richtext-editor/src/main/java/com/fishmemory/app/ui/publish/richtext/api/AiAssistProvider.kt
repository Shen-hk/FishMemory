package com.fishmemory.app.ui.publish.richtext.api

import com.fishmemory.app.ui.publish.ai.AiPolishStyle
import com.fishmemory.app.ui.publish.richtext.core.model.EditorBlockList
import kotlinx.coroutines.flow.Flow

/**
 * AI writing assistance extension point for host apps.
 *
 * The editor owns the block model and UI states, while concrete model provider,
 * credentials, prompts, network protocol, and retry policy belong to the host app.
 */
interface AiAssistProvider {

    fun polish(
        blockList: EditorBlockList,
        blockId: String,
        style: AiPolishStyle,
    ): Flow<String>

    class MissingCredentialsException(
        message: String = "AI credentials missing",
    ) : IllegalStateException(message)
}
