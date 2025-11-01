package com.fishmemory.app.api

import com.fishmemory.app.model.ArticleResponse
import retrofit2.http.GET

interface PostApi {
    @GET("posts")
    suspend fun getPosts(): List<ArticleResponse>

    @GET("posts/1")
    suspend fun getPostDetail(): ArticleResponse
}