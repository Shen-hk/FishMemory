package com.fishmemory.app.model

import androidx.compose.runtime.Immutable

@Immutable
data class ArticleData(
    val id: Int,
    val title: String,
    val body: String,
    val userId: Int,
    val author: String = "Fish Memory Author",
    val category: String = "技术",
    val tags: List<String> = listOf("Android", "Kotlin"),
    val createdAt: String = "2024-01-01",
    val readTime: Int = 5,
    val likeCount: Int = 42,
    val commentCount: Int = 8,
    val coverImage: String? = null
)