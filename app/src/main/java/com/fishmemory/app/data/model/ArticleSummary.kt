package com.fishmemory.app.data.model

/**
 * 首页/推荐流使用的统一文章摘要模型：
 * - 兼容 API 文章与本地原创文章
 * - 不关心 blocks 细节，只承载列表展示和路由所需字段
 */
data class ArticleSummary(
    val id: String,
    val title: String,
    val authorName: String,
    val publishTimeMs: Long,
    val summary: String,
    val coverUrl: String?,
    val readCount: Long,
    val likeCount: Long,
    val commentCount: Long,
    val sourceType: SourceType,
    val apiUrl: String? = null,
    val localId: String? = null
) {
    enum class SourceType {
        API,
        LOCAL
    }
}

