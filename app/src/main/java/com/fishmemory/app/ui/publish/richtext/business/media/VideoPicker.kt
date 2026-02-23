package com.fishmemory.app.ui.publish.richtext.business.media

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * 视频选择器：封装视频选择意图、URI 解析、元数据提取与缩略图生成。
 */
object VideoPicker {

    fun createPickIntent(): Intent = Intent(
        Intent.ACTION_PICK,
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    ).apply {
        type = "video/*"
    }

    /**
     * 从 Uri 解析视频元数据（含缩略图），应在 IO 线程调用。
     */
    suspend fun parseVideoMeta(context: Context, uri: Uri): VideoMeta? =
        withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    retriever.setDataSource(context, uri)
                } else {
                    retriever.setDataSource(context, uri)
                }
                val durationStr =
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                val durationMs = durationStr?.toLongOrNull() ?: 0L
                val widthStr =
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                val heightStr =
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                val width = widthStr?.toIntOrNull() ?: 0
                val height = heightStr?.toIntOrNull() ?: 0
                val fileSize = getFileSize(context.contentResolver, uri)
                val coverPath = extractThumbnail(context, uri, retriever)
                VideoMeta(
                    localUri = uri.toString(),
                    durationMs = durationMs,
                    width = width,
                    height = height,
                    fileSize = fileSize,
                    coverPath = coverPath
                )
            } catch (e: Exception) {
                null
            } finally {
                retriever.release()
            }
        }

    private fun getFileSize(resolver: ContentResolver, uri: Uri): Long {
        return try {
            resolver.openFileDescriptor(uri, "r")?.use { it.statSize } ?: 0L
        } catch (_: Exception) {
            0L
        }
    }

    private fun extractThumbnail(context: Context, uri: Uri, retriever: MediaMetadataRetriever): String? {
        return try {
            val bitmap = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: return null
            val cacheDir = File(context.cacheDir, "video_thumbs")
            if (!cacheDir.exists()) cacheDir.mkdirs()
            val file = File(cacheDir, "thumb_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
            }
            file.absolutePath
        } catch (_: Exception) {
            null
        }
    }
}