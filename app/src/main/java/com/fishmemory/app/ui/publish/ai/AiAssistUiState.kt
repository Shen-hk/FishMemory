package com.fishmemory.app.ui.publish.ai

/**
 * 按 blockId 存放的 AI 辅助 UI 状态；不写入 [EditorBlock]，避免污染持久化模型。
 */
sealed interface AiAssistUiState {

    data object Idle : AiAssistUiState

    data class Loading(
        val style: AiPolishStyle,
    ) : AiAssistUiState

    data class Streaming(
        val style: AiPolishStyle,
        val accumulatedText: String,
    ) : AiAssistUiState

    /** 流结束，等待用户 Accept / Retry / Discard。 */
    data class Preview(
        val style: AiPolishStyle,
        val text: String,
    ) : AiAssistUiState

    data class Error(
        val style: AiPolishStyle,
        val message: String,
    ) : AiAssistUiState
}
