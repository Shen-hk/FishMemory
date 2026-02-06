package com.fishmemory.app.data.model

data class HotRankItem(
    val id: Int,
    val title: String,
    val hotValue: Int, // 热度值，如 1234
    val rank: Int      // 排名（1开始）
)