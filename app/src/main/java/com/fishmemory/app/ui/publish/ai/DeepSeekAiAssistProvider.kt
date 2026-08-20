package com.fishmemory.app.ui.publish.ai

import com.fishmemory.app.ui.publish.richtext.api.AiAssistProvider
import com.fishmemory.app.ui.publish.richtext.core.model.EditorBlockList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * FishMemory's AI assist implementation backed by DeepSeek-compatible streaming chat.
 */
class DeepSeekAiAssistProvider(
    private val apiKey: String,
    private val streamClient: DeepSeekChatStreamClient = DeepSeekChatStreamClient(apiKey),
) : AiAssistProvider {

    override fun polish(
        blockList: EditorBlockList,
        blockId: String,
        style: AiPolishStyle,
    ): Flow<String> {
        if (apiKey.isBlank()) {
            return flow { throw AiAssistProvider.MissingCredentialsException() }
        }
        val messages = AiPromptBuilder.buildMessages(blockList, blockId, style)
            ?: return flow { throw IllegalArgumentException("Target text block not found") }
        return streamClient.streamChat(
            listOf("system" to messages.first, "user" to messages.second),
        )
    }
}
