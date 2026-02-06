package com.fishmemory.app.core.api

import com.fishmemory.app.data.model.ArticleResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface PostApi {
    @GET("api/dm-it")
    suspend fun getItNews(
        @Query("num") num: Int,
        @Query("apikey") apikey: String
    ): ArticleResponse
}