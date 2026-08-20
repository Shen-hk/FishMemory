package com.fishmemory.app.ui.publish.richtext.core.converter

import com.fishmemory.app.ui.publish.richtext.core.model.Document
import com.fishmemory.app.ui.publish.richtext.core.model.EditorBlockEntity
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import java.text.SimpleDateFormat
import java.util.*

/**
 * 标准化 JSON 导出格式 v1.0.0
 * 
 * 统一的文档结构：
 * - version: 格式版本号
 * - title: 文档标题
 * - created_at: 创建时间（ISO 8601）
 * - updated_at: 更新时间（ISO 8601）
 * - blocks: 块数组
 * - metadata: 元数据信息
 */

// ==================== 标准化数据模型 ====================

data class StandardDocument(
    @SerializedName("version") val version: String = "1.0.0",
    @SerializedName("title") val title: String,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String,
    @SerializedName("blocks") val blocks: List<StandardBlock>,
    @SerializedName("metadata") val metadata: DocumentMetadata
)

data class DocumentMetadata(
    @SerializedName("word_count") val wordCount: Int,
    @SerializedName("block_count") val blockCount: Int,
    @SerializedName("has_image") val hasImage: Boolean,
    @SerializedName("has_video") val hasVideo: Boolean,
    @SerializedName("has_code") val hasCode: Boolean,
    @SerializedName("has_heading") val hasHeading: Boolean,
    @SerializedName("has_list") val hasList: Boolean
)

sealed class StandardBlock {
    abstract val id: String
    abstract val type: String
}

// 文本块
data class TextBlock(
    @SerializedName("id") override val id: String,
    @SerializedName("type") override val type: String = "text",
    @SerializedName("data") val data: TextData
) : StandardBlock()

data class TextData(
    @SerializedName("content") val content: String,
    @SerializedName("formats") val formats: List<TextFormat> = emptyList()
)

data class TextFormat(
    @SerializedName("start") val start: Int,
    @SerializedName("end") val end: Int,
    @SerializedName("type") val type: String // bold, italic, underline, etc.
)

// 标题块
data class HeadingBlock(
    @SerializedName("id") override val id: String,
    @SerializedName("type") override val type: String = "heading",
    @SerializedName("data") val data: HeadingData
) : StandardBlock()

data class HeadingData(
    @SerializedName("content") val content: String,
    @SerializedName("level") val level: Int = 2, // h1=1, h2=2, h3=3
    @SerializedName("formats") val formats: List<TextFormat> = emptyList()
)

// 列表块
data class ListBlock(
    @SerializedName("id") override val id: String,
    @SerializedName("type") override val type: String = "list",
    @SerializedName("data") val data: ListData
) : StandardBlock()

data class ListData(
    @SerializedName("list_type") val listType: String, // bullet, number
    @SerializedName("items") val items: List<ListItem>
)

data class ListItem(
    @SerializedName("content") val content: String,
    @SerializedName("order") val order: Int,
    @SerializedName("formats") val formats: List<TextFormat> = emptyList()
)

// 图片块
data class ImageBlock(
    @SerializedName("id") override val id: String,
    @SerializedName("type") override val type: String = "image",
    @SerializedName("data") val data: ImageData
) : StandardBlock()

data class ImageData(
    @SerializedName("url") val url: String,
    @SerializedName("thumbnail") val thumbnail: String?,
    @SerializedName("width") val width: Int = 0,
    @SerializedName("height") val height: Int = 0,
    @SerializedName("alignment") val alignment: String = "center",
    @SerializedName("caption") val caption: String = "",
    @SerializedName("is_local") val isLocal: Boolean = true,
    @SerializedName("upload_state") val uploadState: String = "success",
    @SerializedName("size_mode") val sizeMode: String = "default"
)

// 视频块
data class VideoBlock(
    @SerializedName("id") override val id: String,
    @SerializedName("type") override val type: String = "video",
    @SerializedName("data") val data: VideoData
) : StandardBlock()

data class VideoData(
    @SerializedName("url") val url: String,
    @SerializedName("thumbnail") val thumbnail: String?,
    @SerializedName("cover_url") val coverUrl: String?,
    @SerializedName("duration_ms") val durationMs: Long = 0L,
    @SerializedName("width") val width: Int = 0,
    @SerializedName("height") val height: Int = 0,
    @SerializedName("alignment") val alignment: String = "center",
    @SerializedName("caption") val caption: String = "",
    @SerializedName("is_local") val isLocal: Boolean = true,
    @SerializedName("upload_state") val uploadState: String = "success"
)

// 代码块
data class CodeBlock(
    @SerializedName("id") override val id: String,
    @SerializedName("type") override val type: String = "code",
    @SerializedName("data") val data: CodeData
) : StandardBlock()

data class CodeData(
    @SerializedName("code") val code: String,
    @SerializedName("language") val language: String,
    @SerializedName("show_line_numbers") val showLineNumbers: Boolean = false,
    @SerializedName("wrap_lines") val wrapLines: Boolean = true
)

// 链接卡片块
data class LinkCardBlock(
    @SerializedName("id") override val id: String,
    @SerializedName("type") override val type: String = "link_card",
    @SerializedName("data") val data: LinkCardData
) : StandardBlock()

data class LinkCardData(
    @SerializedName("url") val url: String,
    @SerializedName("title") val title: String = "",
    @SerializedName("description") val description: String = "",
    @SerializedName("image_url") val imageUrl: String? = null
)

// 分割线块
data class HrBlock(
    @SerializedName("id") override val id: String,
    @SerializedName("type") override val type: String = "hr"
) : StandardBlock()

// ==================== 导出工具类 ====================

object StandardJsonExporter {
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    /**
     * 将 RichDocument 转换为标准化 JSON
     */
    fun toStandardJson(document: Document): String {
        val standardDoc = convertToStandardDocument(document)
        val gson = GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create()
        return gson.toJson(standardDoc)
    }

    /**
     * 转换为标准化文档结构
     */
    private fun convertToStandardDocument(document: Document): StandardDocument {
        val timestamp = dateFormat.format(Date())
        val rawBlocks = document.blocks.mapNotNull { block ->
            convertToStandardBlock(block)
        }

        // 将连续的列表项组合成 ListBlock
        val groupedBlocks = groupListItems(rawBlocks)

        // 过滤空文本块（但保留图片和视频）
        val filteredBlocks = filterEmptyBlocks(groupedBlocks)

        val metadata = calculateMetadata(filteredBlocks, document.title)

        return StandardDocument(
            version = "1.0.0",
            title = document.title.ifBlank { "无标题" },
            createdAt = timestamp,
            updatedAt = timestamp,
            blocks = filteredBlocks,
            metadata = metadata
        )
    }

    /**
     * 将单个 RichBlock 转换为 StandardBlock
     */
    private fun convertToStandardBlock(block: EditorBlockEntity): StandardBlock? {
        return when (block) {
            is EditorBlockEntity.Text -> {
                if (block.content.isBlank() && block.spans.isEmpty()) {
                    // 过滤完全空的文本块
                    null
                } else {
                    val blockId = generateBlockId("text")
                    val formats = block.spans.map { span ->
                        TextFormat(
                            start = span.start,
                            end = span.end,
                            type = formatEnumValue(span.type.name)
                        )
                    }

                    // 列表前缀直接烘焙进 content，保证详情页至少能看到项目符号/编号
                    val contentWithPrefix = when (block.blockType) {
                        EditorBlockEntity.Text.TextBlockType.BULLET_LIST ->
                            "• ${block.content}"
                        EditorBlockEntity.Text.TextBlockType.NUMBER_LIST ->
                            // orderIndex 从 1 开始，兜底用 1
                            "${if (block.orderIndex > 0) block.orderIndex else 1}. ${block.content}"
                        else -> block.content
                    }

                    // 根据 isHeading 判断是否为标题
                    if (block.isHeading) {
                        HeadingBlock(
                            id = blockId,
                            type = "heading",
                            data = HeadingData(
                                content = contentWithPrefix,
                                level = 2, // 默认 h2，可根据需要调整
                                formats = formats
                            )
                        )
                    } else {
                        TextBlock(
                            id = blockId,
                            type = "text",
                            data = TextData(
                                content = contentWithPrefix,
                                formats = formats
                            )
                        )
                    }
                }
            }
            is EditorBlockEntity.Image -> {
                ImageBlock(
                    id = generateBlockId("image"),
                    type = "image",
                    data = ImageData(
                        url = block.url,
                        thumbnail = null, // TODO: 可以生成缩略图
                        width = 0, // TODO: 可以从图片加载器获取
                        height = 0,
                        alignment = formatEnumValue(block.alignment),
                        caption = block.caption,
                        isLocal = block.isLocal,
                        uploadState = formatEnumValue(block.uploadState.name),
                        sizeMode = formatEnumValue(block.sizeMode)
                    )
                )
            }
            is EditorBlockEntity.Video -> {
                VideoBlock(
                    id = generateBlockId("video"),
                    type = "video",
                    data = VideoData(
                        url = block.url,
                        thumbnail = null, // TODO: 可以从 coverUrl 获取
                        coverUrl = block.coverUrl,
                        durationMs = block.durationMs,
                        width = 0, // TODO: 可以获取
                        height = 0,
                        alignment = "center",
                        caption = "",
                        isLocal = block.uploadState == EditorBlockEntity.UploadState.SUCCESS && block.url.startsWith("content://"),
                        uploadState = formatEnumValue(block.uploadState.name)
                    )
                )
            }
            is EditorBlockEntity.Code -> {
                CodeBlock(
                    id = generateBlockId("code"),
                    type = "code",
                    data = CodeData(
                        code = block.code,
                        language = block.language.lowercase(),
                        showLineNumbers = false,
                        wrapLines = true
                    )
                )
            }
            is EditorBlockEntity.LinkCard -> {
                LinkCardBlock(
                    id = generateBlockId("link"),
                    type = "link_card",
                    data = LinkCardData(
                        url = block.url,
                        title = block.title,
                        description = block.description,
                        imageUrl = block.imageUrl
                    )
                )
            }
            is EditorBlockEntity.Hr -> {
                HrBlock(
                    id = generateBlockId("hr"),
                    type = "hr"
                )
            }
        }
    }

    /**
     * 将连续的列表项组合成 ListBlock
     */
    private fun groupListItems(blocks: List<StandardBlock>): List<StandardBlock> {
        val result = mutableListOf<StandardBlock>()
        var currentListItems = mutableListOf<ListItemData>()
        var currentListType: String? = null

        blocks.forEach { block ->
            if (block is TextBlock && isListItem(block)) {
                val listType = getListType(block)
                val order = getOrderIndex(block)
                
                if (currentListType == listType) {
                    // 继续当前列表
                    currentListItems.add(
                        ListItemData(
                            content = block.data.content,
                            order = order,
                            formats = block.data.formats
                        )
                    )
                } else {
                    // 列表类型改变，先保存之前的列表
                    if (currentListItems.isNotEmpty() && currentListType != null) {
                        result.add(createListBlock(currentListType!!, currentListItems))
                    }
                    // 开始新列表
                    currentListType = listType
                    currentListItems.clear()
                    currentListItems.add(
                        ListItemData(
                            content = block.data.content,
                            order = order,
                            formats = block.data.formats
                        )
                    )
                }
            } else {
                // 非列表块，先保存之前的列表
                if (currentListItems.isNotEmpty() && currentListType != null) {
                    result.add(createListBlock(currentListType!!, currentListItems))
                    currentListItems.clear()
                    currentListType = null
                }
                result.add(block)
            }
        }

        // 处理末尾的列表
        if (currentListItems.isNotEmpty() && currentListType != null) {
            result.add(createListBlock(currentListType!!, currentListItems))
        }

        return result
    }

    data class ListItemData(
        val content: String,
        val order: Int,
        val formats: List<TextFormat>
    )

    private fun isListItem(block: TextBlock): Boolean {
        // TODO: 需要在 TextBlock 中添加 listType 信息
        // 临时实现：可以根据内容或其他特征判断
        return false
    }

    private fun getListType(block: TextBlock): String {
        return "bullet" // 默认
    }

    private fun getOrderIndex(block: TextBlock): Int {
        return 0 // 临时实现
    }

    private fun createListBlock(listType: String, items: List<ListItemData>): ListBlock {
        return ListBlock(
            id = generateBlockId("list"),
            type = "list",
            data = ListData(
                listType = listType,
                items = items.mapIndexed { index, item ->
                    ListItem(
                        content = item.content,
                        order = item.order,
                        formats = item.formats
                    )
                }
            )
        )
    }

    /**
     * 过滤空块
     */
    private fun filterEmptyBlocks(blocks: List<StandardBlock>): List<StandardBlock> {
        return blocks.filter { block ->
            when (block) {
                is TextBlock -> block.data.content.isNotBlank()
                is HeadingBlock -> block.data.content.isNotBlank()
                is CodeBlock -> block.data.code.isNotBlank()
                else -> true // 图片、视频、链接卡片等即使空也保留
            }
        }
    }

    /**
     * 计算文档元数据
     */
    private fun calculateMetadata(blocks: List<StandardBlock>, title: String): DocumentMetadata {
        var wordCount = title.length
        var hasImage = false
        var hasVideo = false
        var hasCode = false
        var hasHeading = false
        var hasList = false

        blocks.forEach { block ->
            when (block) {
                is TextBlock -> wordCount += block.data.content.length
                is HeadingBlock -> {
                    wordCount += block.data.content.length
                    hasHeading = true
                }
                is ListBlock -> {
                    block.data.items.forEach { wordCount += it.content.length }
                    hasList = true
                }
                is ImageBlock -> hasImage = true
                is VideoBlock -> hasVideo = true
                is CodeBlock -> hasCode = true
                else -> {}
            }
        }

        return DocumentMetadata(
            wordCount = wordCount,
            blockCount = blocks.size,
            hasImage = hasImage,
            hasVideo = hasVideo,
            hasCode = hasCode,
            hasHeading = hasHeading,
            hasList = hasList
        )
    }

    /**
     * 生成块 ID
     */
    fun generateBlockId(type: String): String {
        val timestamp = System.currentTimeMillis()
        val random = UUID.randomUUID().toString().substring(0, 4)
        return "${type}_${timestamp}_${random}"
    }

    /**
     * 格式化枚举值（统一转小写）
     */
    private fun formatEnumValue(value: String): String {
        return value.lowercase()
    }

    /**
     * 判断是否为标题块（通过 spans 或其他方式判断）
     * TODO: 需要在 RichBlock.Text 中添加 isHeading 字段
     */
    private fun isHeadingBlock(block: EditorBlockEntity.Text): Boolean {
        // 临时实现：可以根据内容或其他特征判断
        // 更好的方式是在 EditorBlock.TextBlock 中增加 isHeading 字段
        return false
    }
}
