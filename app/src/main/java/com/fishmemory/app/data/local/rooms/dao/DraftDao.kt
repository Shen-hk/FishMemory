package com.fishmemory.app.data.local.rooms.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.fishmemory.app.data.local.rooms.entity.DraftEntity
import com.fishmemory.app.data.local.rooms.entity.DraftStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface DraftDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DraftEntity)

    @Update
    suspend fun update(entity: DraftEntity)

    @Query("SELECT * FROM drafts WHERE draft_id = :draftId LIMIT 1")
    suspend fun getById(draftId: String): DraftEntity?

    @Query(
        "SELECT * FROM drafts " +
            "WHERE status IN (:visibleStatuses) " +
            "ORDER BY last_opened_at DESC, updated_at DESC " +
            "LIMIT :limit"
    )
    fun observeRecent(
        visibleStatuses: List<DraftStatus> = listOf(DraftStatus.ACTIVE, DraftStatus.SAVED),
        limit: Int = 50
    ): Flow<List<DraftEntity>>

    @Query(
        "SELECT * FROM drafts " +
            "WHERE status = :status " +
            "ORDER BY updated_at DESC " +
            "LIMIT 1"
    )
    suspend fun getLatestByStatus(status: DraftStatus = DraftStatus.ACTIVE): DraftEntity?

    @Query("UPDATE drafts SET status = :status, updated_at = :updatedAt WHERE draft_id = :draftId")
    suspend fun updateStatus(draftId: String, status: DraftStatus, updatedAt: Long)

    @Query("DELETE FROM drafts WHERE status = :status AND updated_at < :olderThan")
    suspend fun deleteByStatusOlderThan(status: DraftStatus, olderThan: Long): Int
}

