package com.fishmemory.app.ui.publish.richtext.business.media

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream

/**
 * 将外部 URI 拷贝到应用内部存储目录，返回 `file://绝对路径`。
 * 用于避免部分机型/Provider 在返回后不可读导致的图片/视频加载失败。
 */
object InternalMediaCopier {

    enum class MediaKind {
        IMAGE, // filesDir/images/，后缀 .jpg
        VIDEO, // filesDir/videos/，后缀 .mp4
    }

    private fun params(kind: MediaKind): Triple<String, String, String> {
        return when (kind) {
            MediaKind.IMAGE -> Triple("images", "IMG_", ".jpg")
            MediaKind.VIDEO -> Triple("videos", "VID_", ".mp4")
        }
    }

    fun copyToInternalFilesDir(
        context: Context,
        sourceUri: Uri,
        kind: MediaKind,
        logTag: String,
    ): String? {
        val (subdir, prefix, extension) = params(kind)
        return try {
            val fileName =
                "${prefix}${System.currentTimeMillis()}_${(Math.random() * 10000).toInt()}${extension}"
            val internalFile = File(context.filesDir, "${subdir}/$fileName")
            internalFile.parentFile?.mkdirs()

            Log.d(logTag, "开始复制${kind.name}... source=$sourceUri dest=${internalFile.absolutePath}")

            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                FileOutputStream(internalFile).use { outputStream ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                    }
                    outputStream.flush()
                }
            } ?: run {
                Log.e(logTag, "无法打开输入流：$sourceUri")
                return null
            }

            Log.d(logTag, "✅ 复制成功：${internalFile.absolutePath}")
            "file://${internalFile.absolutePath}"
        } catch (e: Exception) {
            Log.e(logTag, "❌ 复制失败", e)
            Toast.makeText(context, "保存失败：${e.message}", Toast.LENGTH_SHORT).show()
            null
        }
    }
}

