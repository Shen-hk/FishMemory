package com.fishmemory.app.data.repository

import com.fishmemory.app.data.local.rooms.dao.ArticleDao
import com.fishmemory.app.data.local.rooms.dao.ArticleCollectedState
import com.fishmemory.app.data.local.rooms.entity.ArticleEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ArticleRepository(private val articleDao: ArticleDao) {

    fun getCollectedArticles(): Flow<List<ArticleEntity>> =
        articleDao.getCollectedArticles()

    suspend fun toggleCollect(id: Int, title: String, originalUrl: String?): Boolean {
        val existing = articleDao.getArticleById(id)
        val newCollected: Boolean
        if (existing != null) {
            newCollected = !existing.isCollected
            articleDao.setCollected(
                id = id,
                collected = newCollected,
                collectedAt = if (newCollected) System.currentTimeMillis() else null
            )
        } else {
            newCollected = true
            articleDao.insert(ArticleEntity(
                id = id,
                title = title,
                originalUrl = originalUrl,
                isCollected = true,
                collectedAt = System.currentTimeMillis()
            ))
        }
        return newCollected
    }

    suspend fun isCollected(id: Int): Boolean {
        return articleDao.isCollected(id) ?: false
    }

    /**
     * 批量监听收藏状态（列表页 / 推荐页用）
     * Flow<List> → Flow<Map>
     */
    fun observeCollectedStateMap(
        ids: List<Int>
    ): Flow<Map<Int, Boolean>> {

        return articleDao.observeCollectedStates(ids)
            .map {
                list: List<ArticleCollectedState> ->
                list.associate { state ->
                    state.id to state.isCollected
                }

            }

    }
    suspend fun ensureArticleExists(id: Int, title: String, originalUrl: String?) {
        val existing = articleDao.getArticleById(id)
        if (existing == null) {
            // 插入未收藏的文章（用于后续收藏）
            articleDao.insert(ArticleEntity(id, title, originalUrl, isCollected = false))
        }
    }
}