package com.fishmemory.app.ui.publish.richtext.core.converter

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParseException

/**
 * 将标准化 JSON 解析为 StandardDocument，供详情页只读渲染使用。
 * 按 blocks 中每项的 type 字段反序列化为对应 StandardBlock 子类。
 */
object StandardJsonParser {

    private val gson = Gson()

    fun parse(json: String): StandardDocument? {
        return try {
            Log.d("StandardJsonParser", "========== 开始解析标准化 JSON ==========")
            Log.d("StandardJsonParser", "JSON 长度：${json.length}")
            
            val root = gson.fromJson(json, JsonObject::class.java) ?: return null
            val version = root.get("version")?.asString ?: "1.0.0"
            val title = root.get("title")?.asString ?: ""
            val createdAt = root.get("created_at")?.asString ?: ""
            val updatedAt = root.get("updated_at")?.asString ?: ""
            val metadata = root.get("metadata")?.let { gson.fromJson(it, DocumentMetadata::class.java) }
                ?: DocumentMetadata(0, 0, false, false, false, false, false)
            val blocksArray = root.getAsJsonArray("blocks") ?: JsonArray()
            
            Log.d("StandardJsonParser", "文档标题：$title")
            Log.d("StandardJsonParser", "版本：$version")
            Log.d("StandardJsonParser", "块数量：${blocksArray.size()}")
            
            val blocks = parseBlocks(blocksArray)
            
            Log.d("StandardJsonParser", "解析完成，成功解析 ${blocks.size} 个块")
            Log.d("StandardJsonParser", "========== 解析结束 ==========")
            
            StandardDocument(
                version = version,
                title = title,
                createdAt = createdAt,
                updatedAt = updatedAt,
                blocks = blocks,
                metadata = metadata
            )
        } catch (e: JsonParseException) {
            Log.e("StandardJsonParser", "JSON 解析失败", e)
            null
        } catch (e: Exception) {
            Log.e("StandardJsonParser", "解析过程中发生异常", e)
            null
        }
    }

    private fun parseBlocks(array: JsonArray): List<StandardBlock> {
        val list = mutableListOf<StandardBlock>()
        var imageCount = 0
        var videoCount = 0
        
        for (i in 0 until array.size()) {
            val el = array.get(i)?.asJsonObject ?: continue
            val type = el.get("type")?.asString ?: continue
            val id = el.get("id")?.asString ?: "unknown_$i"
            
            Log.d("StandardJsonParser", "[$i] 处理块类型：$type, ID: $id")
            
            val block = when (type) {
                "text" -> gson.fromJson(el, TextBlock::class.java)
                "heading" -> gson.fromJson(el, HeadingBlock::class.java)
                "list" -> gson.fromJson(el, ListBlock::class.java)
                "image" -> {
                    imageCount++
                    parseImageBlock(el, i)
                }
                "video" -> {
                    videoCount++
                    parseVideoBlock(el, i)
                }
                "code" -> gson.fromJson(el, CodeBlock::class.java)
                "link_card" -> gson.fromJson(el, LinkCardBlock::class.java)
                "hr" -> gson.fromJson(el, HrBlock::class.java)
                else -> {
                    Log.w("StandardJsonParser", "[$i] 未知块类型：$type")
                    null
                }
            }
            if (block != null) list.add(block)
        }
        
        Log.d("StandardJsonParser", "块统计：图片=$imageCount, 视频=$videoCount, 总计=${list.size}")
        return list
    }
    
    private fun parseImageBlock(el: JsonObject, index: Int): ImageBlock? {
        try {
            val id = el.get("id")?.asString ?: "unknown_image_$index"
            val dataObj = el.getAsJsonObject("data") ?: run {
                Log.e("StandardJsonParser", "[$index] 图片块缺少 data 字段，ID: $id")
                return null
            }
            
            val url = dataObj.get("url")?.asString ?: ""
            val caption = dataObj.get("caption")?.asString ?: ""
            val fileKey = dataObj.get("file_key")?.asString
            
            Log.d("StandardJsonParser", "[$index] ========== 图片块详情 ==========")
            Log.d("StandardJsonParser", "[$index] ID: $id")
            Log.d("StandardJsonParser", "[$index] URL: $url")
            Log.d("StandardJsonParser", "[$index] 说明文字：$caption")
            Log.d("StandardJsonParser", "[$index] 文件 Key: $fileKey")
            
            if (url.isBlank()) {
                Log.w("StandardJsonParser", "[$index] ⚠️ 警告：图片 URL 为空！")
            } else {
                Log.d("StandardJsonParser", "[$index] ✅ URL 有效：${url.take(50)}...")
            }
            
            return ImageBlock(
                id = id,
                type = "image",
                data = ImageData(
                    url = url,
                    thumbnail = null,
                    width = 0,
                    height = 0,
                    alignment = "center",
                    caption = caption,
                    isLocal = false,
                    uploadState = "success",
                    sizeMode = "default"
                )
            )
        } catch (e: Exception) {
            Log.e("StandardJsonParser", "[$index] 解析图片块失败", e)
            return null
        }
    }
    
    private fun parseVideoBlock(el: JsonObject, index: Int): VideoBlock? {
        try {
            val id = el.get("id")?.asString ?: "unknown_video_$index"
            val dataObj = el.getAsJsonObject("data") ?: run {
                Log.e("StandardJsonParser", "[$index] 视频块缺少 data 字段，ID: $id")
                return null
            }
            
            val url = dataObj.get("url")?.asString ?: ""
            val coverUrl = dataObj.get("cover_url")?.asString
            val durationMs = dataObj.get("duration_ms")?.asLong ?: 0L
            val fileKey = dataObj.get("file_key")?.asString
            
            Log.d("StandardJsonParser", "[$index] ========== 视频块详情 ==========")
            Log.d("StandardJsonParser", "[$index] ID: $id")
            Log.d("StandardJsonParser", "[$index] URL: $url")
            Log.d("StandardJsonParser", "[$index] 封面 URL: $coverUrl")
            Log.d("StandardJsonParser", "[$index] 时长：${durationMs}ms (${formatDurationLog(durationMs)})")
            Log.d("StandardJsonParser", "[$index] 文件 Key: $fileKey")
            
            if (url.isBlank()) {
                Log.e("StandardJsonParser", "[$index] ❌ 严重：视频 URL 为空！无法播放")
            } else {
                Log.d("StandardJsonParser", "[$index] ✅ URL 有效：${url.take(50)}...")
            }
            
            if (coverUrl.isNullOrBlank()) {
                Log.w("StandardJsonParser", "[$index] ⚠️ 警告：封面 URL 为空，将使用默认占位图")
            } else {
                Log.d("StandardJsonParser", "[$index] ✅ 封面有效：${coverUrl.take(50)}...")
            }
            
            return VideoBlock(
                id = id,
                type = "video",
                data = VideoData(
                    url = url,
                    thumbnail = null,
                    coverUrl = coverUrl,
                    durationMs = durationMs,
                    width = 0,
                    height = 0,
                    alignment = "center",
                    caption = "",
                    isLocal = false,
                    uploadState = "success"
                )
            )
        } catch (e: Exception) {
            Log.e("StandardJsonParser", "[$index] 解析视频块失败", e)
            return null
        }
    }
    
    private fun formatDurationLog(ms: Long): String {
        val sec = (ms / 1000) % 60
        val min = (ms / 60000) % 60
        val hour = ms / 3600000
        return if (hour > 0) {
            "%d:%02d:%02d".format(hour, min, sec)
        } else {
            "%d:%02d".format(min, sec)
        }
    }
}
