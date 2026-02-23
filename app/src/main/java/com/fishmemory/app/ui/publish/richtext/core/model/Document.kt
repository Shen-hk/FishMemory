package com.fishmemory.app.ui.publish.richtext.core.model

/**
 * 统一的文档模型：
 * - 标题：单独字段，受标题输入规则约束
 * - blocks：正文部分的结构化 RichBlock 列表
 *
 * 说明：
 * - PublishActivity 持有整个 RichDocument，用于提交、预览等业务逻辑
 * - RichEditorView 只负责维护和编辑 blocks，不关心标题本身
 */
data class Document(
    val title: String,
    val blocks: List<EditorBlockEntity>
)

