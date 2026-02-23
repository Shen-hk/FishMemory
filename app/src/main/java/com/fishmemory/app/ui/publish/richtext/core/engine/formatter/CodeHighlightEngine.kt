package com.fishmemory.app.ui.publish.richtext.core.engine.formatter

import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import com.fishmemory.app.ui.publish.richtext.config.EditorConfig
import com.fishmemory.app.ui.publish.richtext.config.EditorStyle

/**
 * 代码高亮引擎：根据编程语言关键字、注释、字符串、数字等元素，
 * 为代码文本应用前景色和加粗样式，实现语法高亮效果。
 * 
 * ## 核心功能
 * 
 * ### 1. 支持的语言
 * - Kotlin
 * - Java
 * - JavaScript
 * - 其他语言（默认使用 Kotlin 规则）
 * 
 * ### 2. 高亮规则
 * - **关键字**：如 `fun`, `class`, `if`, `return` 等，使用蓝色 + 加粗
 * - **注释**：单行注释 `// ...` 和多行注释 `/* ... */`，使用绿色
 * - **字符串**：双引号 `"..."` 和单引号 `'...'`，使用橙色
 * - **数字**：整数和小数，使用紫色
 * - **函数名**：形如 `name(` 的标识符，使用青色 + 加粗
 * 
 * ### 3. 实现原理
 * 使用正则表达式匹配各类语法元素，通过 [ForegroundColorSpan] 和 [StyleSpan]
 * 为匹配的文本区域应用颜色和样式。
 * 
 * ### 4. 防抖优化
 * 在 [CodeBlockViewHolder] 中，输入时会延迟 300ms 再调用高亮，
 * 避免频繁渲染影响性能。
 * 
 * @see com.fishmemory.app.ui.publish.richtext.model.EditorConfig.LanguageKeywords 各语言的关键字定义
 * @see com.fishmemory.app.ui.publish.richtext.model.EditorConfig.CodeColors 高亮颜色配置
 * @see CodeBlockViewHolder 代码块视图持有者，调用此引擎进行高亮
 */
object CodeHighlightEngine {

    /**
     * 代码高亮入口函数
     * 
     * @param code 待高亮的源代码字符串
     * @param language 编程语言标识，如 "Kotlin", "Java", "JavaScript"
     * @return 已应用高亮样式的 SpannableStringBuilder
     */
    fun highlight(code: String, language: String): SpannableStringBuilder {
        val builder = SpannableStringBuilder(code)
        applyHighlight(builder, language)
        return builder
    }

    /**
     * 为 SpannableStringBuilder 应用语法高亮
     * 
     * ## 处理流程
     * 1. 清理已有样式，避免重复叠加
     * 2. 根据语言类型选择关键字列表
     * 3. 依次应用注释、字符串、数字、关键字、函数名的高亮规则
     * 
     * @param spannable 待处理的 SpannableStringBuilder
     * @param language 编程语言标识
     */
    fun applyHighlight(spannable: SpannableStringBuilder, language: String) {
        // 先清理已有样式，避免重复叠加
        val spans = spannable.getSpans(0, spannable.length, Any::class.java)
        for (span in spans) {
            spannable.removeSpan(span)
        }

        val text = spannable.toString()
        val lang = language.lowercase()

        // 根据语言类型选择对应的关键字列表
        val keywords = when {
            lang.contains("kotlin") -> EditorConfig.LanguageKeywords.KOTLIN
            lang.contains("java") -> EditorConfig.LanguageKeywords.JAVA
            lang.contains("js") || lang.contains("javascript") -> EditorConfig.LanguageKeywords.JAVASCRIPT
            else -> EditorConfig.LanguageKeywords.KOTLIN // 兜底使用 Kotlin 规则
        }

        // 注释：单行 // ... 与简单的 /* ... */（不处理嵌套）
        applyRegex(
            spannable,
            Regex("//.*"),
            EditorStyle.CodeColors.COMMENT
        )
        applyRegex(
            spannable,
            Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL),
            EditorStyle.CodeColors.COMMENT
        )

        // 字符串："..." 或 '...'，支持转义字符
        applyRegex(
            spannable,
            Regex("\"([^\"\\\\]|\\\\.)*\""),
            EditorStyle.CodeColors.STRING
        )
        applyRegex(
            spannable,
            Regex("'([^'\\\\]|\\\\.)*'"),
            EditorStyle.CodeColors.STRING
        )

        // 数字：整数和小数
        applyRegex(
            spannable,
            Regex("\\b\\d+(\\.\\d+)?\\b"),
            EditorStyle.CodeColors.NUMBER
        )

        // 关键字：使用正则组合匹配多个关键字
        if (keywords.isNotEmpty()) {
            val pattern = Regex("\\b(${keywords.joinToString("|")})\\b")
            applyRegex(
                spannable,
                pattern,
                EditorStyle.CodeColors.KEYWORD,
                bold = true
            )
        }

        // 函数名：形如 name(，仅粗略匹配
        applyFunctionHighlight(spannable)
    }

    /**
     * 使用正则表达式为文本应用颜色样式
     * 
     * @param spannable 目标 SpannableStringBuilder
     * @param regex 用于匹配的正则表达式
     * @param color 前景色颜色值
     * @param bold 是否加粗，默认为 false
     */
    private fun applyRegex(
        spannable: SpannableStringBuilder,
        regex: Regex,
        color: Int,
        bold: Boolean = false
    ) {
        val text = spannable.toString()
        regex.findAll(text).forEach { match ->
            val start = match.range.first
            val end = match.range.last + 1
            // 应用前景色
            spannable.setSpan(
                ForegroundColorSpan(color),
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            // 可选加粗
            if (bold) {
                spannable.setSpan(
                    StyleSpan(Typeface.BOLD),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }
    }

    /**
     * 为函数名应用高亮样式
     * 
     * ## 匹配规则
     * - 匹配模式：`\b([A-Za-z_][A-Za-z0-9_]*)\s*\(`
     * - 示例：`println(`, `toString(`, `myFunction(`
     * 
     * ## 样式
     * - 前景色：青色 ([EditorConfig.CodeColors.FUNCTION])
     * - 加粗：是
     * 
     * @param spannable 目标 SpannableStringBuilder
     */
    private fun applyFunctionHighlight(spannable: SpannableStringBuilder) {
        val text = spannable.toString()
        val regex = Regex("\\b([A-Za-z_][A-Za-z0-9_]*)\\s*\\(")
        regex.findAll(text).forEach { match ->
            val group = match.groups[1] ?: return@forEach
            val start = group.range.first
            val end = group.range.last + 1
            // 应用函数名高亮
            spannable.setSpan(
                ForegroundColorSpan(EditorStyle.CodeColors.FUNCTION),
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            spannable.setSpan(
                StyleSpan(Typeface.BOLD),
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }


}

