package com.fishmemory.app.ui.publish.ai

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fishmemory.app.BuildConfig
import com.fishmemory.app.R
import com.fishmemory.app.ui.publish.richtext.core.model.EditorBlockList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 块状编辑器 AI 润色：会话态在 ViewModel，不写入 [com.fishmemory.app.ui.publish.richtext.core.model.EditorBlock]。
 * 视觉刷新通过 [onSessionVisualUpdate] 通知外层对单条 notifyItemChanged(payload)，避免整表刷新。
 */
class BlockAiAssistViewModel(
    application: Application,
    apiKey: String,
    private val streamClient: DeepSeekChatStreamClient = DeepSeekChatStreamClient(apiKey),
) : AndroidViewModel(application) {

    /** 由 Activity 设置：某 block 的 AI UI 变化时刷新对应 ViewHolder。 */
    var onSessionVisualUpdate: ((blockId: String) -> Unit)? = null

    private val _sessions = MutableStateFlow<Map<String, AiAssistUiState>>(emptyMap())
    val sessions: StateFlow<Map<String, AiAssistUiState>> = _sessions.asStateFlow()

    private val _effects = Channel<AiAssistEffect>(capacity = Channel.BUFFERED)
    val effects: Flow<AiAssistEffect> = _effects.receiveAsFlow()

    private val streamJobs = mutableMapOf<String, Job>()

    private var lastStyleByBlock = mutableMapOf<String, AiPolishStyle>()

    fun startPolish(blockList: EditorBlockList, blockId: String, style: AiPolishStyle) {
        // 先于网络校验写入，便于缺 Key 时 Retry 仍能复现同一润色意图
        lastStyleByBlock[blockId] = style
        if (BuildConfig.DEEPSEEK_API_KEY.isBlank()) {
            putSession(
                blockId,
                AiAssistUiState.Error(
                    style = style,
                    message = getApplication<Application>().getString(R.string.ai_polish_missing_api_key),
                ),
            )
            return
        }
        val messages = AiPromptBuilder.buildMessages(blockList, blockId, style) ?: return
        streamJobs[blockId]?.cancel()
        streamJobs[blockId] = viewModelScope.launch {
            putSession(blockId, AiAssistUiState.Loading(style))
            var accumulated = ""
            try {
                streamClient.streamChat(
                    listOf("system" to messages.first, "user" to messages.second),
                ).collect { delta ->
                    accumulated += delta
                    putSession(
                        blockId,
                        AiAssistUiState.Streaming(style, accumulatedText = accumulated),
                    )
                }
                putSession(
                    blockId,
                    AiAssistUiState.Preview(style, text = accumulated.trim()),
                )
            } catch (_: CancellationException) {
                putSession(blockId, AiAssistUiState.Idle)
            } catch (e: Exception) {
                putSession(
                    blockId,
                    AiAssistUiState.Error(
                        style = style,
                        message = e.message?.take(200) ?: "request failed",
                    ),
                )
            } finally {
                streamJobs.remove(blockId)
            }
        }
    }

    fun retry(blockList: EditorBlockList, blockId: String) {
        val style = lastStyleByBlock[blockId] ?: return
        startPolish(blockList, blockId, style)
    }

    fun discard(blockId: String) {
        streamJobs[blockId]?.cancel()
        streamJobs.remove(blockId)
        removeSession(blockId)
    }

    fun accept(blockId: String) {
        val preview = _sessions.value[blockId] as? AiAssistUiState.Preview ?: return
        viewModelScope.launch {
            _effects.send(AiAssistEffect.ApplyAcceptedText(blockId, preview.text))
            removeSession(blockId)
        }
    }

    private fun putSession(blockId: String, state: AiAssistUiState) {
        _sessions.update { curr ->
            val next = curr.toMutableMap()
            if (state === AiAssistUiState.Idle) {
                next.remove(blockId)
            } else {
                next[blockId] = state
            }
            next
        }
        onSessionVisualUpdate?.invoke(blockId)
    }

    private fun removeSession(blockId: String) {
        _sessions.update { curr ->
            val next = curr.toMutableMap()
            next.remove(blockId)
            next
        }
        onSessionVisualUpdate?.invoke(blockId)
    }

    override fun onCleared() {
        super.onCleared()
        streamJobs.values.forEach { it.cancel() }
        streamJobs.clear()
        onSessionVisualUpdate = null
    }
}

class BlockAiAssistViewModelFactory(
    private val application: Application,
    private val apiKey: String,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return BlockAiAssistViewModel(application, apiKey) as T
    }
}

sealed class AiAssistEffect {
    data class ApplyAcceptedText(val blockId: String, val plainText: String) : AiAssistEffect()
}
