package com.fishmemory.app.ui.publish.richtext.business.media

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import com.fishmemory.app.ui.publish.richtext.api.EditorVideoUploader
import com.google.gson.JsonParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * 视频上传管理器：基于 OkHttp 构建 multipart 请求，管理上传任务与进度回调。
 * 上传地址需根据实际后端配置，此处使用占位 URL。
 */
class VideoUploadManager(
    private val context: Context,
    private val uploadUrl: String = "https://api.qqsuu.cn/upload/video",
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()
) : EditorVideoUploader {

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val jobs = ConcurrentHashMap<String, Job>()
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun enqueueUpload(blockId: String, localUri: String, callback: EditorVideoUploader.Callback) {
        jobs[blockId]?.cancel()
        jobs[blockId] = scope.launch {
            try {
                val file = copyUriToTempFile(localUri) ?: run {
                    mainHandler.post { callback.onError(blockId, IllegalArgumentException("无法读取视频文件")) }
                    return@launch
                }
                val progressBody =
                    ProgressRequestBody(file, "video/*".toMediaType(), blockId) { id, p ->
                        mainHandler.post { callback.onProgress(id, p) }
                    }
                val body = MultipartBody.Builder()
                    .setType(MultipartBody.Companion.FORM)
                    .addFormDataPart("video", file.name, progressBody)
                    .build()
                val request = Request.Builder()
                    .url(uploadUrl)
                    .post(body)
                    .build()
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string().orEmpty()
                    val url = parseRemoteUrl(bodyStr)
                    mainHandler.post {
                        callback.onProgress(blockId, 100)
                        callback.onSuccess(blockId, url.ifEmpty { localUri })
                    }
                } else {
                    mainHandler.post { callback.onError(blockId, RuntimeException("上传失败: ${response.code}")) }
                }
                file.delete()
            } catch (e: Exception) {
                mainHandler.post { callback.onError(blockId, e) }
            } finally {
                jobs.remove(blockId)
            }
        }
    }

    override fun cancelUpload(blockId: String) {
        jobs[blockId]?.cancel()
        jobs.remove(blockId)
    }

    private suspend fun copyUriToTempFile(uriString: String): File? = withContext(Dispatchers.IO) {
        try {
            val uri = Uri.parse(uriString)
            val input = context.contentResolver.openInputStream(uri) ?: return@withContext null
            val file = File(context.cacheDir, "upload_video_${System.currentTimeMillis()}.mp4")
            FileOutputStream(file).use { out ->
                input.copyTo(out)
            }
            file
        } catch (_: Exception) {
            null
        }
    }

    private fun parseRemoteUrl(responseBody: String): String {
        return try {
            val json = JsonParser.parseString(responseBody).asJsonObject
            json.get("url")?.asString
                ?: json.get("data")?.asJsonObject?.get("url")?.asString
                ?: ""
        } catch (_: Exception) {
            ""
        }
    }
}
