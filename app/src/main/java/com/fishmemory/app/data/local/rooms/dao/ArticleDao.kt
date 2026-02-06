package com.fishmemory.app.data.local.rooms.dao

import androidx.room.*
import com.fishmemory.app.data.local.rooms.entity.ArticleEntity
import kotlinx.coroutines.flow.Flow

/**
 * 文章收藏状态中间模型（给 Room 用）
 */
data class ArticleCollectedState(
    val id: Int,
    val isCollected: Boolean
)

@Dao
interface ArticleDao {

    // 倒序获得已收藏文章
    @Query("""
        SELECT * FROM articles 
        WHERE isCollected = 1 
        ORDER BY collectedAt DESC
    """)
    fun getCollectedArticles(): Flow<List<ArticleEntity>>

    // 根据 id 获取单篇文章
    @Query("SELECT * FROM articles WHERE id = :id")
    suspend fun getArticleById(id: Int): ArticleEntity?

    // 插入文章
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(article: ArticleEntity)

    // 更新文章
    @Update
    suspend fun update(article: ArticleEntity)

    // 设置收藏状态
    @Query("""
        UPDATE articles 
        SET isCollected = :collected, collectedAt = :collectedAt 
        WHERE id = :id
    """)
    suspend fun setCollected(
        id: Int,
        collected: Boolean,
        collectedAt: Long? = null
    )

    // 查询某篇文章是否被收藏（单值，Boolean 是 OK 的）
    @Query("SELECT isCollected FROM articles WHERE id = :id")
    suspend fun isCollected(id: Int): Boolean?

    // 批量监听文章收藏状态（稳定写法）
    @Query("""
        SELECT id, isCollected 
        FROM articles 
        WHERE id IN (:ids)
    """)
    fun observeCollectedStates(ids: List<Int>): Flow<List<ArticleCollectedState>>
}
