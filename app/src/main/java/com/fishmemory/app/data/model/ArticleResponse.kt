package com.fishmemory.app.data.model

import androidx.compose.runtime.Immutable

/** 网络接口返回的列表包装（code/msg/data）。 */
@Immutable
data class ArticleResponse(
    val code: Int,
    val msg: String,
    val data: ResultData
)

/** 列表 data 节点（分页 + 列表）。 */
@Immutable
data class ResultData(
    val curpage: Int,
    val allnum: Int,
    val newslist: List<DmItArticle>
)

/** 单条文章网络 DTO（接口字段与命名保持与后端一致）。 */
@Immutable
data class DmItArticle(
    val id: String,
    val ctime: String,        // 发布时间，如 "2021-02-04 19:09"
    val title: String,
    val description: String,  // 摘要
    val source: String,       // 来源，如 "网易IT"
    val picUrl: String,       // 封面图 URL ✅
    val url: String           // 原文链接 ✅
)