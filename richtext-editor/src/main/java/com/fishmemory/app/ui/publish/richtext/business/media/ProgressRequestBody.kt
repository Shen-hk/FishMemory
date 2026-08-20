package com.fishmemory.app.ui.publish.richtext.business.media

import okhttp3.MediaType
import okhttp3.RequestBody
import okio.BufferedSink
import java.io.File
import java.io.FileInputStream

/**
 * 带进度回调的 RequestBody，用于上传大文件时报告进度。
 */
class ProgressRequestBody(
    private val file: File,
    private val contentType: MediaType?,
    private val blockId: String,
    private val onProgress: (String, Int) -> Unit
) : RequestBody() {

    override fun contentLength(): Long = file.length()

    override fun contentType(): MediaType? = contentType

    override fun writeTo(sink: BufferedSink) {
        val length = file.length()
        var uploaded = 0L
        FileInputStream(file).use { input ->
            val buffer = ByteArray(8192)
            var read: Int
            while (input.read(buffer).also { read = it } != -1) {
                sink.write(buffer, 0, read)
                uploaded += read
                val progress = if (length > 0) ((uploaded * 100) / length).toInt().coerceIn(0, 99) else 0
                onProgress(blockId, progress)
            }
        }
        onProgress(blockId, 100)
    }
}
