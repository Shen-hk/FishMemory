package com.fishmemory.app.data.local.rooms.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 本地已发表原创文章表：
 * - 用于首页列表混合展示与原生详情渲染
 * - 不与收藏表 ArticleEntity 混用
 */
@Entity(tableName = "local_articles")
data class LocalArticleEntity(
    @PrimaryKey
    val localId: String,
    val title: String,
    val authorName: String,
    val publishTimeMs: Long,
    val coverUrl: String?,
    val summary: String?,
    val blocksJson: String,
    val readCount: Long = 0L,
    val likeCount: Long = 0L,
    val commentCount: Long = 0L,
    val status: String = STATUS_PUBLISHED
) {
    companion object {
        const val STATUS_PUBLISHED = "PUBLISHED"
        const val STATUS_DRAFT = "DRAFT"
    }
}

