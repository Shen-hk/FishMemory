package com.fishmemory.app.model

data class ArticleResponse(
    val userId: Int,
    val id: Int,
    val title: String,
    val body: String
)