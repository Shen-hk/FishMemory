package com.fishmemory.app.ui.publish.ai

import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * OpenAI 兼容 Chat Completions 流式接口（DeepSeek 同形）。
 * 按行解析 SSE：`data: {...}`，从 choices[0].delta.content 取增量文本。
 */
class DeepSeekChatStreamClient(
    private val apiKey: String,
    private val baseUrl: String = DEFAULT_BASE_URL,
    private val model: String = "deepseek-chat",
) {

    private val gson = Gson()
    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.MINUTES)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * @param messages OpenAI 格式 role/content 列表，顺序与请求体一致
     */
    fun streamChat(messages: List<Pair<String, String>>): Flow<String> = callbackFlow {
        if (apiKey.isBlank()) {
            close(IllegalStateException("API key empty"))
            return@callbackFlow
        }

        val bodyJson = gson.toJson(
            ChatCompletionRequest(
                model = model,
                stream = true,
                messages = messages.map { (role, content) ->
                    ChatMessage(role = role, content = content)
                },
            )
        )
        val request = Request.Builder()
            .url(baseUrl)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(bodyJson.toByteArray(Charsets.UTF_8).toRequestBody(jsonMedia))
            .build()

        val call = client.newCall(request)
        try {
            val response = call.execute()
            if (!response.isSuccessful) {
                val errBody = response.body?.string()?.take(500)
                close(
                    IllegalStateException(
                        "HTTP ${response.code}${errBody?.let { ": $it" }.orEmpty()}"
                    )
                )
                return@callbackFlow
            }
            val source = response.body?.source() ?: run {
                close(IllegalStateException("Empty body"))
                return@callbackFlow
            }
            while (true) {
                val line = source.readUtf8Line() ?: break
                if (line.isBlank()) continue
                if (!line.startsWith("data:")) continue
                val payload = line.removePrefix("data:").trim()
                if (payload == "[DONE]") break
                val chunk = extractDeltaContent(payload) ?: continue
                if (chunk.isNotEmpty()) {
                    trySend(chunk)
                }
            }
            close()
        } catch (e: Exception) {
            close(e)
        } finally {
            call.cancel()
        }
        awaitClose { call.cancel() }
    }.flowOn(Dispatchers.IO)

    private data class ChatCompletionRequest(
        val model: String,
        val stream: Boolean,
        val messages: List<ChatMessage>,
    )

    private data class ChatMessage(
        val role: String,
        val content: String,
    )

    private fun extractDeltaContent(dataLine: String): String? {
        return try {
            val root = JsonParser.parseString(dataLine).asJsonObject
            val choices = root.getAsJsonArray("choices") ?: return null
            if (choices.size() == 0) return null
            val delta = choices[0].asJsonObject.getAsJsonObject("delta") ?: return null
            if (!delta.has("content") || delta.get("content").isJsonNull) return null
            delta.get("content").asString
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        const val DEFAULT_BASE_URL = "https://api.deepseek.com/v1/chat/completions"
    }
}
