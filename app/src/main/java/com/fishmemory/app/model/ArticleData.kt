package com.fishmemory.app.model

import androidx.compose.runtime.Immutable

@Immutable
data class ArticleData(
    // 基础标识字段
    val id: Int,
    val title: String,
    val body: String,
    val userId: Int,

    // 扩展信息字段（带默认值）
    val author: String = "Fish Memory Author",
    val category: String = "技术",
    val tags: List<String> = listOf("Android", "Kotlin"),
    val createdAt: String = "2024-01-01",

    // 交互统计字段
    val readTime: Int = 5,
    val likeCount: Int = 42,
    val commentCount: Int = 8,

    // 可选字段
    val coverImage: String? = null,
    val isFavorite: Boolean = false
)