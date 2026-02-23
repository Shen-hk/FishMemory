package com.fishmemory.app.data.model

import androidx.compose.runtime.Immutable

/** 列表/详情页使用的文章领域模型，可来自网络 DTO 转换或本地合并收藏状态。 */
@Immutable
data class ArticleData(
    // 基础标识字段
    val id: Int,
    val title: String,
    val body: String,
    val userId: Int=0,

    // 扩展信息字段（带默认值）
    val author: String = "Fish Memory Author",
    val category: String = "技术",
    val tags: List<String> = listOf("Android", "Kotlin"),
    val createdAt: String = "2024-01-01",

    // 交互统计字段
    val readTime: Int = 5,
    var likeCount: Int = 42,
    val commentCount: Int = 8,

    // 可选字段
    val coverImage: String? = null,
    var isFavorite: Boolean = false,
    var isCollect: Boolean=false,
    val originalUrl: String? = null
)