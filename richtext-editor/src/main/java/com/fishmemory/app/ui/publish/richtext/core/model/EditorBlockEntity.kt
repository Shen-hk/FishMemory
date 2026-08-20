package com.fishmemory.app.ui.publish.richtext.core.model

// RichBlock.kt
import com.google.gson.annotations.SerializedName

sealed class EditorBlockEntity {

    data class Text(
        @SerializedName("content")
        val content: String,
        @SerializedName("spans")
        val spans: List<SpanData> = emptyList(),
        // 文本块类型：普通文本 / 无序列表 / 有序列表
        @SerializedName("blockType")
        val blockType: TextBlockType = TextBlockType.TEXT,
        // 有序列表序号，仅在 blockType == NUMBER_LIST 时有效
        @SerializedName("orderIndex")
        val orderIndex: Int = 0,
        // 是否为标题块
        @SerializedName("isHeading")
        val isHeading: Boolean = false
    ) : EditorBlockEntity() {

        enum class TextBlockType {
            @SerializedName("text")
            TEXT,

            @SerializedName("bullet_list")
            BULLET_LIST,

            @SerializedName("number_list")
            NUMBER_LIST
        }
    }

    data class Image(
        @SerializedName("url")
        val url: String,
        @SerializedName("caption")
        val caption: String = "",
        @SerializedName("isLocal")
        val isLocal: Boolean = true,
        @SerializedName("uploadState")
        val uploadState: UploadState = UploadState.PENDING,
        @SerializedName("alignment")
        val alignment: String = "CENTER",
        @SerializedName("sizeMode")
        val sizeMode: String = "DEFAULT"
    ) : EditorBlockEntity()

    /**
     * 视频块：用于持久化/提交的结构化视频信息。
     * - url：视频远程地址（提交时应为线上 URL）
     * - coverUrl：封面图 URL 或路径
     * - durationMs：视频时长（毫秒）
     * - uploadState：上传状态
     */
    data class Video(
        @SerializedName("url")
        val url: String,
        @SerializedName("coverUrl")
        val coverUrl: String? = null,
        @SerializedName("durationMs")
        val durationMs: Long = 0L,
        @SerializedName("uploadState")
        val uploadState: UploadState = UploadState.PENDING
    ) : EditorBlockEntity()

    data class Code(
        @SerializedName("language")
        val language: String,
        @SerializedName("code")
        val code: String
    ) : EditorBlockEntity()

    data class LinkCard(
        @SerializedName("url")
        val url: String,
        @SerializedName("title")
        val title: String = "",
        @SerializedName("description")
        val description: String = "",
        @SerializedName("imageUrl")
        val imageUrl: String? = null
    ) : EditorBlockEntity()
    object Hr : EditorBlockEntity()
    enum class UploadState {
        @SerializedName("pending")
        PENDING,
        @SerializedName("uploading")
        UPLOADING,
        @SerializedName("success")
        SUCCESS,
        @SerializedName("failed")
        FAILED
    }
}
