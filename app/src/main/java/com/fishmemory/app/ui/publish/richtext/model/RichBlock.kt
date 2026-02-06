package com.fishmemory.app.ui.publish.richtext.model

// RichBlock.kt
import com.google.gson.annotations.SerializedName

sealed class RichBlock {

    data class Text(
        @SerializedName("content")
        val content: String,
        @SerializedName("spans")
        val spans: List<TextSpan> = emptyList()
    ) : RichBlock()

    data class Image(
        @SerializedName("url")
        val url: String,
        @SerializedName("caption")
        val caption: String = "",
        @SerializedName("isLocal")
        val isLocal: Boolean = true,
        @SerializedName("uploadState")
        val uploadState: UploadState = UploadState.PENDING
    ) : RichBlock()

    data class Code(
        @SerializedName("language")
        val language: String,
        @SerializedName("code")
        val code: String
    ) : RichBlock()

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