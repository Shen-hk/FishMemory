package com.fishmemory.app.ui.publish.draft

import com.fishmemory.app.ui.publish.richtext.core.model.EditorBlockEntity
import com.fishmemory.app.ui.publish.richtext.core.model.Document
import com.fishmemory.app.ui.publish.richtext.core.model.SpanData
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import kotlin.collections.get

/**
 * 草稿专用 JSON：
 * - 需要稳定可逆（编辑态可完整恢复）
 * - 不依赖 sealed class 的 Gson 反射推断
 */
object DraftJsonCodec {
    private const val VERSION = "1"

    private val gson: Gson = GsonBuilder()
        .disableHtmlEscaping()
        .create()

    fun encode(document: Document): String {
        val dto = DraftDocumentDto(
            version = VERSION,
            title = document.title,
            blocks = document.blocks.map { DraftBlockDto.from(it) }
        )
        return gson.toJson(dto)
    }

    fun decode(json: String): Document {
        val dto = gson.fromJson(json, DraftDocumentDto::class.java)
        val blocks = dto.blocks.mapNotNull { it.toRichBlockOrNull() }
        return Document(title = dto.title.orEmpty(), blocks = blocks)
    }
}

data class DraftDocumentDto(
    @SerializedName("version") val version: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("blocks") val blocks: List<DraftBlockDto> = emptyList()
)

data class DraftBlockDto(
    @SerializedName("type") val type: String,
    @SerializedName("data") val data: Any?
) {
    companion object {
        fun from(block: EditorBlockEntity): DraftBlockDto {
            return when (block) {
                is EditorBlockEntity.Text -> DraftBlockDto(
                    type = "text",
                    data = TextData(
                        content = block.content,
                        spans = block.spans,
                        blockType = block.blockType.name,
                        orderIndex = block.orderIndex,
                        isHeading = block.isHeading
                    )
                )
                is EditorBlockEntity.Image -> DraftBlockDto(
                    type = "image",
                    data = ImageData(
                        url = block.url,
                        caption = block.caption,
                        isLocal = block.isLocal,
                        uploadState = block.uploadState.name,
                        alignment = block.alignment,
                        sizeMode = block.sizeMode
                    )
                )
                is EditorBlockEntity.Video -> DraftBlockDto(
                    type = "video",
                    data = VideoData(
                        url = block.url,
                        coverUrl = block.coverUrl,
                        durationMs = block.durationMs,
                        uploadState = block.uploadState.name
                    )
                )
                is EditorBlockEntity.Code -> DraftBlockDto(
                    type = "code",
                    data = CodeData(language = block.language, code = block.code)
                )
                is EditorBlockEntity.LinkCard -> DraftBlockDto(
                    type = "link_card",
                    data = LinkCardData(
                        url = block.url,
                        title = block.title,
                        description = block.description,
                        imageUrl = block.imageUrl
                    )
                )
                is EditorBlockEntity.Hr -> DraftBlockDto(type = "hr", data = null)
            }
        }
    }

    fun toRichBlockOrNull(): EditorBlockEntity? {
        return when (type) {
            "text" -> {
                val m = data as? Map<*, *> ?: return null
                val content = m["content"] as? String ?: ""
                val spans = runCatching {
                    // Gson 在 Any->Map 时，spans 会是 List<LinkedTreeMap>；直接二次序列化最稳
                    val spansJson = Gson().toJson(m["spans"])
                    Gson().fromJson(spansJson, Array<SpanData>::class.java)
                        .toList()
                }.getOrDefault(emptyList())
                val blockTypeName = m["blockType"] as? String ?: EditorBlockEntity.Text.TextBlockType.TEXT.name
                val orderIndex = (m["orderIndex"] as? Number)?.toInt() ?: 0
                val isHeading = m["isHeading"] as? Boolean ?: false
                val blockType = runCatching { EditorBlockEntity.Text.TextBlockType.valueOf(blockTypeName) }
                    .getOrDefault(EditorBlockEntity.Text.TextBlockType.TEXT)
                EditorBlockEntity.Text(
                    content = content,
                    spans = spans,
                    blockType = blockType,
                    orderIndex = orderIndex,
                    isHeading = isHeading
                )
            }
            "image" -> {
                val m = data as? Map<*, *> ?: return null
                val url = m["url"] as? String ?: return null
                val caption = m["caption"] as? String ?: ""
                val isLocal = m["isLocal"] as? Boolean ?: true
                val uploadStateName = m["uploadState"] as? String ?: EditorBlockEntity.UploadState.PENDING.name
                val uploadState = runCatching { EditorBlockEntity.UploadState.valueOf(uploadStateName) }
                    .getOrDefault(EditorBlockEntity.UploadState.PENDING)
                val alignment = m["alignment"] as? String ?: "CENTER"
                val sizeMode = m["sizeMode"] as? String ?: "DEFAULT"
                EditorBlockEntity.Image(
                    url = url,
                    caption = caption,
                    isLocal = isLocal,
                    uploadState = uploadState,
                    alignment = alignment,
                    sizeMode = sizeMode
                )
            }
            "video" -> {
                val m = data as? Map<*, *> ?: return null
                val url = m["url"] as? String ?: return null
                val coverUrl = m["coverUrl"] as? String
                val durationMs = (m["durationMs"] as? Number)?.toLong() ?: 0L
                val uploadStateName = m["uploadState"] as? String ?: EditorBlockEntity.UploadState.PENDING.name
                val uploadState = runCatching { EditorBlockEntity.UploadState.valueOf(uploadStateName) }
                    .getOrDefault(EditorBlockEntity.UploadState.PENDING)
                EditorBlockEntity.Video(url = url, coverUrl = coverUrl, durationMs = durationMs, uploadState = uploadState)
            }
            "code" -> {
                val m = data as? Map<*, *> ?: return null
                val language = m["language"] as? String ?: "text"
                val code = m["code"] as? String ?: ""
                EditorBlockEntity.Code(language = language, code = code)
            }
            "link_card" -> {
                val m = data as? Map<*, *> ?: return null
                val url = m["url"] as? String ?: return null
                val title = m["title"] as? String ?: ""
                val description = m["description"] as? String ?: ""
                val imageUrl = m["imageUrl"] as? String
                EditorBlockEntity.LinkCard(url = url, title = title, description = description, imageUrl = imageUrl)
            }
            "hr" -> EditorBlockEntity.Hr
            else -> null
        }
    }
}

data class TextData(
    @SerializedName("content") val content: String,
    @SerializedName("spans") val spans: List<SpanData>,
    @SerializedName("blockType") val blockType: String,
    @SerializedName("orderIndex") val orderIndex: Int,
    @SerializedName("isHeading") val isHeading: Boolean
)

data class ImageData(
    @SerializedName("url") val url: String,
    @SerializedName("caption") val caption: String,
    @SerializedName("isLocal") val isLocal: Boolean,
    @SerializedName("uploadState") val uploadState: String,
    @SerializedName("alignment") val alignment: String,
    @SerializedName("sizeMode") val sizeMode: String
)

data class VideoData(
    @SerializedName("url") val url: String,
    @SerializedName("coverUrl") val coverUrl: String?,
    @SerializedName("durationMs") val durationMs: Long,
    @SerializedName("uploadState") val uploadState: String
)

data class CodeData(
    @SerializedName("language") val language: String,
    @SerializedName("code") val code: String
)

data class LinkCardData(
    @SerializedName("url") val url: String,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String,
    @SerializedName("imageUrl") val imageUrl: String?
)

