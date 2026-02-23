package com.fishmemory.app.ui.publish.draft

import com.fishmemory.app.data.local.rooms.dao.DraftDao
import com.fishmemory.app.data.local.rooms.entity.DraftEntity
import com.fishmemory.app.data.local.rooms.entity.DraftStatus
import com.fishmemory.app.ui.publish.richtext.core.model.EditorBlockEntity
import com.fishmemory.app.ui.publish.richtext.core.model.Document
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class DraftManager(
    private val draftDao: DraftDao
) {
    data class DraftSnapshot(
        val draftId: String,
        val document: Document
    )

    suspend fun loadLatestActiveOrNull(): DraftSnapshot? = withContext(Dispatchers.IO) {
        val entity = draftDao.getLatestByStatus(DraftStatus.ACTIVE) ?: return@withContext null
        DraftSnapshot(draftId = entity.draftId, document = DraftJsonCodec.decode(entity.contentJson))
    }

    suspend fun loadByIdOrNull(draftId: String): DraftSnapshot? = withContext(Dispatchers.IO) {
        val entity = draftDao.getById(draftId) ?: return@withContext null
        DraftSnapshot(draftId = entity.draftId, document = DraftJsonCodec.decode(entity.contentJson))
    }

    suspend fun createNewEmptyDraft(): DraftSnapshot = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val draftId = UUID.randomUUID().toString()
        val document = Document(title = "", blocks = emptyList())
        val json = DraftJsonCodec.encode(document)
        val summary = summarize(document)
        draftDao.upsert(
            DraftEntity(
                draftId = draftId,
                title = document.title,
                contentJson = json,
                status = DraftStatus.ACTIVE,
                createdAt = now,
                updatedAt = now,
                lastOpenedAt = now,
                wordCount = summary.wordCount,
                preview = summary.preview
            )
        )
        DraftSnapshot(draftId = draftId, document = document)
    }

    suspend fun markOpened(draftId: String) = withContext(Dispatchers.IO) {
        val entity = draftDao.getById(draftId) ?: return@withContext
        val now = System.currentTimeMillis()
        draftDao.upsert(entity.copy(lastOpenedAt = now, updatedAt = now))
    }

    suspend fun save(
        draftId: String,
        document: Document,
        status: DraftStatus = DraftStatus.ACTIVE
    ) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val json = DraftJsonCodec.encode(document)
        val summary = summarize(document)
        val existing = draftDao.getById(draftId)
        val createdAt = existing?.createdAt ?: now
        draftDao.upsert(
            DraftEntity(
                draftId = draftId,
                title = document.title,
                contentJson = json,
                status = status,
                createdAt = createdAt,
                updatedAt = now,
                lastOpenedAt = now,
                wordCount = summary.wordCount,
                preview = summary.preview
            )
        )
    }

    private data class Summary(val wordCount: Int, val preview: String)

    private fun summarize(document: Document): Summary {
        val sb = StringBuilder()
        var wordCount = document.title.length
        document.blocks.forEach { block ->
            when (block) {
                is EditorBlockEntity.Text -> {
                    wordCount += block.content.length
                    if (sb.length < 80 && block.content.isNotBlank()) {
                        if (sb.isNotEmpty()) sb.append(" ")
                        sb.append(block.content.trim().replace('\n', ' '))
                    }
                }
                is EditorBlockEntity.Code -> {
                    wordCount += block.code.length
                    if (sb.length < 80 && block.code.isNotBlank()) {
                        if (sb.isNotEmpty()) sb.append(" ")
                        sb.append(block.code.trim().replace('\n', ' '))
                    }
                }
                is EditorBlockEntity.Image -> {
                    // 图片不计入字数
                }
                is EditorBlockEntity.Video -> {}
                is EditorBlockEntity.LinkCard -> {
                    wordCount += block.title.length + block.description.length
                }
                is EditorBlockEntity.Hr -> {}
            }
        }
        val preview = sb.toString().take(80)
        return Summary(wordCount = wordCount, preview = preview)
    }
}

