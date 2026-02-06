package com.fishmemory.app.data.local.rooms.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "articles")
data class ArticleEntity(
    @PrimaryKey val id: Int,
    val title: String,
    val originalUrl: String?,
    val isCollected: Boolean = false, // 默认未收藏
    val collectedAt: Long? = null // 收藏时间（可选）
)