package com.fishmemory.app.ui.publish.richtext.business.media

/**
 * 视频元数据：选择本地视频后解析得到的信息。
 */
data class VideoMeta(
    val localUri: String,
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val fileSize: Long,
    val coverPath: String?
)
