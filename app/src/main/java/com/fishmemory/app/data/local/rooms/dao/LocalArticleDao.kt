package com.fishmemory.app.data.local.rooms.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fishmemory.app.data.local.rooms.entity.LocalArticleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LocalArticleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: LocalArticleEntity)

    @Query("SELECT * FROM local_articles WHERE localId = :localId LIMIT 1")
    suspend fun getById(localId: String): LocalArticleEntity?

    @Query(
        "SELECT * FROM local_articles " +
            "WHERE status = :status " +
            "ORDER BY publishTimeMs DESC"
    )
    fun observeByStatus(status: String = LocalArticleEntity.STATUS_PUBLISHED): Flow<List<LocalArticleEntity>>

    /** 一次性拉取已发表列表，供首页合并 API 列表使用 */
    @Query(
        "SELECT * FROM local_articles " +
            "WHERE status = :status " +
            "ORDER BY publishTimeMs DESC"
    )
    suspend fun getByStatus(status: String = LocalArticleEntity.STATUS_PUBLISHED): List<LocalArticleEntity>

    /** 详情页打开时本地阅读数 +1 */
    @Query("UPDATE local_articles SET readCount = readCount + 1 WHERE localId = :localId")
    suspend fun incrementReadCount(localId: String)

    /** 观察所有本地文章（不分状态），用于管理界面 */
    @Query("SELECT * FROM local_articles ORDER BY publishTimeMs DESC")
    fun observeAll(): Flow<List<LocalArticleEntity>>

    /** 根据 ID 删除本地文章 */
    @Query("DELETE FROM local_articles WHERE localId = :localId")
    suspend fun deleteByLocalId(localId: String)
}

