package com.fishmemory.app.compose.navigation

sealed class Screen(val route: String) {
    object ArticleList : Screen("article_list")
    object ArticleDetail : Screen("article_detail/{articleId}") {
        fun createRoute(articleId: Int) = "article_detail/$articleId"
    }
}