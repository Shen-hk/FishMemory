// App.kt
package com.fishmemory.app

import android.app.Application
import com.fishmemory.app.core.network.NetworkStateManager
import com.fishmemory.app.data.local.rooms.AppDatabase
import com.fishmemory.app.data.repository.ArticleRepository

class App : Application() {

    lateinit var articleRepository: ArticleRepository

    override fun onCreate() {
        super.onCreate()

        NetworkStateManager.init(applicationContext)

        // 初始化 Room 数据库和 Repository
        val db = AppDatabase.getInstance(this)
        articleRepository = ArticleRepository(db.articleDao())
    }
}