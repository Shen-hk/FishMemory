package com.fishmemory.app.ui.publish.usecase

import android.content.Context
import android.os.Environment
import android.util.Log
import com.fishmemory.app.R
import com.fishmemory.app.data.local.rooms.AppDatabase
import com.fishmemory.app.data.local.rooms.entity.LocalArticleEntity
import com.fishmemory.app.ui.publish.richtext.core.model.Document
import com.fishmemory.app.ui.publish.richtext.core.model.EditorBlockEntity
import com.fishmemory.app.ui.publish.richtext.api.RichTextDocumentCodec
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

/**
 * 发布业务：将 [Document] 导出标准 JSON、写摘要与封面、异步落库 [com.fishmemory.app.data.local.rooms.entity.LocalArticleEntity]、写入应用外部文档目录。
 * 不包含 [android.app.Activity.startActivity]；分享与 Toast 由界面层根据 [Outcome] 处理。
 */
class PublishArticleUseCase(
    private val appContext: Context,
    private val ioScope: CoroutineScope,
) {

    sealed class Outcome {
        data class Success(val jsonFile: File) : Outcome()
        data object FailureCreateFile : Outcome()
        data object FailureExport : Outcome()
    }

    /**
     * 与原先发布流程一致的线程模型：JSON 构建与写文件在调用线程（主线程），[ioScope] 上启动 IO 落库。
     */
    fun execute(document: Document): Outcome {
        return try {
            val json = RichTextDocumentCodec.exportStandardJson(document)
            Log.d(TAG, "Standard JSON exported, length=${json.length}")
            val (summary, coverUrl) = extractSummaryAndCover(document)
            val localId = "local_${System.currentTimeMillis()}"
            val publishTimeMs = System.currentTimeMillis()
            val authorName = appContext.getString(R.string.local_article_author)
            val entity = LocalArticleEntity(
                localId = localId,
                title = document.title.ifBlank { appContext.getString(R.string.untitled_article) },
                authorName = authorName,
                publishTimeMs = publishTimeMs,
                coverUrl = coverUrl.takeIf { !it.isNullOrBlank() },
                summary = summary.ifBlank { null },
                blocksJson = json,
                readCount = 0L,
                likeCount = 0L,
                commentCount = 0L,
                status = LocalArticleEntity.Companion.STATUS_PUBLISHED
            )
            ioScope.launch(Dispatchers.IO) {
                runCatching {
                    AppDatabase.Companion.getInstance(appContext).localArticleDao().upsert(entity)
                }.onFailure { e -> Log.e(TAG, "save local article failed", e) }
            }
            val file = createJsonFile(json) ?: return Outcome.FailureCreateFile
            Outcome.Success(file)
        } catch (e: Exception) {
            Log.e(TAG, "export json failed", e)
            Outcome.FailureExport
        }
    }

    /** 从 Document 提取列表摘要（前 200 字）与封面图 URL。封面优先正文首图，无图时用首张链接卡片图。 */
    private fun extractSummaryAndCover(document: Document): Pair<String, String?> {
        val summaryBuilder = StringBuilder()
        var firstImageUrl: String? = null
        var firstLinkCardImageUrl: String? = null
        for (block in document.blocks) {
            when (block) {
                is EditorBlockEntity.Text -> {
                    if (block.content.isNotBlank()) summaryBuilder.append(block.content)
                }
                is EditorBlockEntity.Code -> {
                    if (block.code.isNotBlank()) summaryBuilder.append(block.code)
                }
                is EditorBlockEntity.LinkCard -> {
                    if (block.title.isNotBlank()) summaryBuilder.append(block.title)
                    if (block.description.isNotBlank()) summaryBuilder.append(block.description)
                    if (firstLinkCardImageUrl == null && !block.imageUrl.isNullOrBlank()) firstLinkCardImageUrl = block.imageUrl
                }
                is EditorBlockEntity.Image -> {
                    if (firstImageUrl == null && block.url.isNotBlank()) firstImageUrl = block.url
                }
                else -> {}
            }
            if (summaryBuilder.length >= 200) break
        }
        val summary = summaryBuilder.toString().take(200).trim()
        val coverUrl = firstImageUrl ?: firstLinkCardImageUrl
        return (summary.ifBlank { document.title }) to coverUrl
    }

    private fun createJsonFile(json: String): File? {
        return try {
            val dir = appContext.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            if (dir == null) return null
            val articlesDir = File(dir, "articles")
            if (!articlesDir.exists() && !articlesDir.mkdirs()) {
                return null
            }
            val fileName = "article_${System.currentTimeMillis()}.json"
            val file = File(articlesDir, fileName)
            FileOutputStream(file).use { out ->
                out.write(json.toByteArray(Charsets.UTF_8))
            }
            file
        } catch (e: Exception) {
            Log.e(TAG, "createJsonFile failed", e)
            null
        }
    }

    private companion object {
        private const val TAG = "PublishArticleUseCase"
    }
}
