package com.fishmemory.app.ui.publish.richtext.config

import android.graphics.Color

object EditorConfig {
    // 日志标签统一管理
    const val TAG_EDIT_TEXT = "BlockEditText"
    const val TAG_ADAPTER = "EditorAdapter"

    // 交互行为常量
    object Behavior {
        const val MAX_BLOCK_COUNT = 100
        const val AUTO_SAVE_DELAY_MS = 2000L
    }

    // 样式与动画配置
    object Style {
        const val CURSOR_VISIBLE_POST_DELAY = 100L
    }


    /**
     * 编程语言关键字定义
     */
    object LanguageKeywords {
        val KOTLIN = setOf(
            "as", "break", "class", "continue", "do", "else", "false", "for", "fun",
            "if", "in", "interface", "is", "null", "object", "package", "return",
            "super", "this", "throw", "true", "try", "typealias", "val", "var",
            "when", "while"
        )

        val JAVA = setOf(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char",
            "class", "const", "continue", "default", "do", "double", "else", "enum",
            "extends", "final", "finally", "float", "for", "goto", "if", "implements",
            "import", "instanceof", "int", "interface", "long", "native", "new",
            "package", "private", "protected", "public", "return", "short", "static",
            "strictfp", "super", "switch", "synchronized", "this", "throw", "throws",
            "transient", "try", "void", "volatile", "while"
        )

        val JAVASCRIPT = setOf(
            "break", "case", "catch", "class", "const", "continue", "debugger", "default",
            "delete", "do", "else", "export", "extends", "finally", "for", "function",
            "if", "import", "in", "instanceof", "new", "return", "super", "switch",
            "this", "throw", "try", "typeof", "var", "void", "while", "with", "yield",
            "let"
        )
    }


}