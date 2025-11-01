package com.fishmemory.app.ui.publish.ai

import com.fishmemory.app.ui.publish.richtext.core.model.EditorBlock
import com.fishmemory.app.ui.publish.richtext.core.model.EditorBlockList

/**
 * 组装 DeepSeek / OpenAI 兼容 Chat 消息：前文若干块作背景，仅改写目标块纯文本。
 *
 * 边界：目标与上下文均使用 [CharSequence.toString]，不尝试保留 Span；Accept 时由 UI 写回纯文本。
 */
object AiPromptBuilder {

    private const val CONTEXT_BLOCK_LIMIT = 3

    private fun styleInstruction(style: AiPolishStyle): String = when (style) {
        AiPolishStyle.MORE_FORMAL -> "将目标段落改写得更正式、书面。"
        AiPolishStyle.SHORTER -> "将目标段落压缩得更简短，保留核心信息。"
        AiPolishStyle.EXPAND -> "在保持原意的前提下适当扩写目标段落，使表达更完整。"
    }

    /**
     * @param blockId 当前要润色的 TextBlock id
     * @return system + user 两条消息
     */
    fun buildMessages(
        blockList: EditorBlockList,
        blockId: String,
        style: AiPolishStyle,
    ): Pair<String, String>? {
        val blocks = blockList.getBlocks()
        val targetIndex = blocks.indexOfFirst { it.id == blockId }
        val targetBlock = blocks.getOrNull(targetIndex) as? EditorBlock.TextBlock
            ?: return null

        val contextSnippets = mutableListOf<String>()
        if (targetIndex > 0) {
            var i = targetIndex - 1
            while (i >= 0 && contextSnippets.size < CONTEXT_BLOCK_LIMIT) {
                when (val b = blocks[i]) {
                    is EditorBlock.TextBlock -> {
                        val t = b.text.toString().trim()
                        if (t.isNotEmpty()) contextSnippets.add(0, t)
                    }
                    else -> { /* 非文本块跳过，不强行当上下文 */ }
                }
                i--
            }
        }

        val targetPlain = targetBlock.text.toString()

        val system = buildString {
            append("你是中文写作助手。")
            append(styleInstruction(style))
            append("用户会提供「上下文背景」与「目标段落」。")
            append("你必须结合上下文理解语气与主题，但只改写目标段落。")
            append("输出中只返回改写后的目标段落正文，不要输出解释、引号或 Markdown 代码块。")
        }

        val user = buildString {
            if (contextSnippets.isNotEmpty()) {
                append("【上下文背景】\n")
                contextSnippets.forEachIndexed { idx, s ->
                    append("(${idx + 1}) ")
                    append(s)
                    append('\n')
                }
                append('\n')
            }
            append("【目标段落】\n")
            append(targetPlain)
        }

        return system to user
    }
}
