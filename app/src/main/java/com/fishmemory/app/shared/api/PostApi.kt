package com.fishmemory.app.shared.api

import android.R
import com.fishmemory.app.shared.model.ArticleResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface PostApi {
    @GET("api/dm-it")
    suspend fun getItNews(
        @Query("num") num: Int ,
        @Query("apikey") apikey: String
    ): ArticleResponse
}