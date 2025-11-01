package com.fishmemory.app.ui.publish.richtext.core.engine.validator

/**
 * 验证规则
 * Editor 统一的 URL 规则（校验 + 行范围解析）。
 *
 * 为什么要抽出来：
 * - 编辑态（BlockEditorRecyclerView/TextBlockViewHolder）与只读态渲染需要一致的“URL 是否可转卡片/是否为纯 URL 行”判定。
 * - 避免同一份正则在多个 UI 类里重复维护导致语义漂移。
 */
object EditorUrlRules {

    /**
     * URL 候选更宽松：
     * - 支持 `www` 开头
     * - 支持中文路径/参数
     * - 支持 http/https
     */
    private val urlRegex = Regex(
        "^((https?://)|(www\\.))([\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]|[\\u4e00-\\u9fa5])+$",
        RegexOption.IGNORE_CASE
    )

    fun isValidUrlCandidate(line: String): Boolean {
        if (line.isBlank()) return false
        return try {
            urlRegex.matches(line)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 根据光标位置，计算当前光标所在行的范围：[lineStart, lineEnd)
     */
    fun findCurrentLineRange(text: CharSequence, cursorPos: Int): Pair<Int, Int> {
        val safeCursor = cursorPos.coerceIn(0, text.length)
        val before = (safeCursor - 1).coerceAtLeast(0)
        val lineStart = text.lastIndexOf('\n', before).let { if (it < 0) 0 else it + 1 }
        val lineEnd = text.indexOf('\n', safeCursor).let { if (it < 0) text.length else it }
        return lineStart to lineEnd
    }
}

