package com.fishmemory.app.compose.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleDetailScreen(
    articleId: Int? = null,                     // 可选的文章ID参数 可选参数，支持空安全
    onBack: () -> Unit = {}                     // 返回按钮回调 事件回调，默认空实现
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "文章详情",
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"                           //// 无障碍描述？？？？辅助功能
                        )
                    }
                }
            )
        }
    ) { paddingValues ->  //   Scaffold 传递的内边距参数，用于避免内容被系统栏（如状态栏、导航栏）遮挡
        Box(
            modifier = Modifier
                .fillMaxSize()           // 让 Box 占满 Scaffold 内部的全部可用空间
                .padding(paddingValues), // 应用 Scaffold 提供的内边距（例如顶部避开状态栏，底部避开导航栏）
            contentAlignment = Alignment.Center  // 将 Box 内部的内容（Text）在水平和垂直方向上居中显示
        ) {
            Text(
                text = "文章详情页面\n文章ID: ${articleId ?: "未知"}",  // 显示页面标题和文章ID；若 articleId 为 null，则显示“未知”
                style = MaterialTheme.typography.bodyLarge,           // 使用 Material Design 主题中定义的 bodyLarge 文字样式
                textAlign = TextAlign.Center                          // 文本内容在 Text 组件内部居中对齐
            )
        }
    }
}

@Preview
@Composable
fun ArticleDetailScreenPreview() {
    ArticleDetailScreen(articleId = 123, onBack = {})
}