package com.fishmemory.app.ui.publish.richtext.core.model

data class SpanData(
    val start: Int,
    val end: Int,
    val type: SpanType,
    // 仅当 type == LINK 时生效
    val url: String? = null
)


