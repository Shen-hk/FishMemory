package com.fishmemory.app.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.ArticleList.route
    ) {
        // 文章列表页面
        composable(route = Screen.ArticleList.route) {
            ArticleListScreen(
                onArticleClick = { articleId ->
                    // 导航到文章详情页
                    navController.navigate(Screen.ArticleDetail.createRoute(articleId))
                }
            )
        }

        // 文章详情页面
        composable(
            route = Screen.ArticleDetail.route,
            arguments = listOf(
                navArgument("articleId") {
                    type = NavType.IntType  // 直接使用 NavType，不需要 androidx.navigation. 前缀
                }
            )
        ) { backStackEntry ->
            val articleId = backStackEntry.arguments?.getInt("articleId")
            ArticleDetailScreen(
                articleId = articleId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}