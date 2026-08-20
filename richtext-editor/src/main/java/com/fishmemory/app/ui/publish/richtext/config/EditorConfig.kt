package com.fishmemory.app.ui.publish.richtext.config

import android.graphics.Color

object EditorConfig {
    val TAG_EDIT_TEXT = "BlockEditText"
    val TAG_ADAPTER = "EditorAdapter"

    object Behavior {
        const val MAX_BLOCK_COUNT = 100
        const val AUTO_SAVE_DELAY_MS = 2000L
    }

    object Style {
        const val CURSOR_VISIBLE_POST_DELAY = 100L
    }

    /**
     * 编程语言关键字定义
     * 
     * 包含常用编程语言的核心关键字，用于代码高亮引擎的语法匹配。
     * 每个语言的关键字列表按功能分类组织，便于维护和扩展。
     */
    object LanguageKeywords {
        /**
         * Kotlin 关键字（完整列表）
         * 包含：控制流、声明、类型相关、修饰符等
         */
        val KOTLIN = setOf(
            // 控制流
            "as", "break", "continue", "do", "else", "for", "if", "in", "return", "when", "while",
            // 声明与定义
            "class", "fun", "interface", "object", "package", "typealias", "val", "var", "constructor",
            // 类型相关
            "is", "null", "this", "super", "open", "override", "abstract", "final", "sealed", "enum",
            // 可见性与修饰符
            "public", "private", "protected", "internal", "actual", "expect", "tailrec", "operator", "infix", "inline", "external",
            // 异常处理
            "throw", "try", "catch", "finally",
            // 其他
            "companion", "init", "data", "inner", "value", "suspend", "annotation", "by", "get", "set"
        )

        /**
         * Java 关键字（完整列表）
         * 包含：基础类型、控制流、修饰符、异常处理等
         */
        val JAVA = setOf(
            // 基础类型与布尔值
            "boolean", "byte", "char", "short", "int", "long", "float", "double", "void",
            "true", "false", "null",
            // 控制流
            "break", "case", "continue", "default", "do", "else", "for", "if", "return", "switch", "while",
            // 声明与定义
            "class", "enum", "extends", "implements", "import", "interface", "new", "package",
            // 访问控制与修饰符
            "public", "private", "protected", "static", "final", "abstract", "native", "strictfp",
            "transient", "volatile", "synchronized",
            // 特殊关键字
            "super", "this", "instanceof",
            // 异常处理
            "throw", "throws", "try", "catch", "finally",
            // 其他
            "assert", "const", "goto", "requires"
        )

        /**
         * JavaScript 关键字（ES6+ 完整列表）
         * 包含：ES5、ES6+ 新特性、异步编程等
         */
        val JAVASCRIPT = setOf(
            // 控制流
            "break", "case", "continue", "debugger", "default", "do", "else", "for", "if", "return", "switch", "throw", "try", "while",
            // 声明与定义
            "class", "const", "export", "extends", "import", "function", "let", "var", "yield",
            // 特殊值
            "true", "false", "null", "undefined", "NaN", "Infinity",
            // 操作符相关
            "in", "instanceof", "typeof", "void", "delete", "new", "this", "super",
            // 异步编程
            "async", "await",
            // 模块相关
            "from", "as", "export", "import",
            // 其他
            "static", "get", "with", "catch", "finally", "enum", "implements", "interface", "package", "protected", "public", "private"
        )

        /**
         * TypeScript 关键字
         * 包含：JavaScript 全部关键字 + TypeScript 特有类型系统关键字
         */
        val TYPESCRIPT = setOf(
            // 继承 JavaScript 全部关键字
            "break", "case", "catch", "class", "const", "continue", "debugger", "default", "delete",
            "do", "else", "enum", "export", "extends", "false", "finally", "for", "function", "if",
            "import", "in", "instanceof", "new", "null", "return", "super", "switch", "this", "throw",
            "true", "try", "typeof", "var", "void", "while", "with", "as", "from", "let", "yield",
            "async", "await", "of",
            // TypeScript 特有
            "namespace", "module", "declare", "interface", "type", "generic", "implements", "readonly",
            "keyof", "infer", "conditional", "mapped", "unique", "symbol", "bigint", "never", "any",
            "unknown", "asserts", "is", "satisfies"
        )

        /**
         * Python 关键字（Python 3.x）
         * 包含：控制流、函数定义、类、异常处理、导入等
         */
        val PYTHON = setOf(
            // 控制流
            "if", "elif", "else", "for", "while", "break", "continue", "pass", "return",
            // 函数与类
            "def", "class", "lambda", "yield", "async", "await",
            // 导入与模块
            "import", "from", "as", "global", "nonlocal",
            // 逻辑操作
            "and", "or", "not", "is", "in",
            // 异常处理
            "try", "except", "finally", "raise", "assert",
            // 特殊值
            "True", "False", "None",
            // 其他
            "with", "del", "exec", "print", "match", "case"
        )

        /**
         * Go 语言关键字
         * 包含：控制流、函数、类型、并发等
         */
        val GO = setOf(
            // 控制流
            "break", "case", "continue", "default", "else", "fallthrough", "for", "go", "goto", "if", "range", "return", "select", "switch",
            // 声明
            "func", "interface", "map", "package", "struct", "type", "const", "var", "import",
            // 特殊关键字
            "chan", "defer", "make", "new", "nil", "iota",
            // 返回值
            "bool", "byte", "complex64", "complex128", "float32", "float64", "int", "int8", "int16", "int32", "int64", "rune", "string", "uint", "uint8", "uint16", "uint32", "uint64", "uintptr"
        )

        /**
         * Rust 关键字
         * 包含：所有权系统、模式匹配、宏等
         */
        val RUST = setOf(
            // 控制流
            "break", "continue", "else", "if", "loop", "match", "return", "while", "for", "in",
            // 声明
            "as", "const", "crate", "dyn", "enum", "extern", "fn", "impl", "let", "mod", "mut", "pub", "ref", "static", "struct", "super", "trait", "type", "union", "unsafe", "use", "where",
            // 特殊
            "async", "await", "box", "macro", "move", "raw", "try", "Self", "self", "offsetof", "sizeof", "alignof", "typeof", "yield"
        )

        /**
         * C++ 关键字（C++11/14/17/20）
         * 包含：C 兼容特性、面向对象、泛型编程、现代 C++ 特性
         */
        val CPP = setOf(
            // 基础类型
            "bool", "char", "double", "float", "int", "long", "short", "signed", "unsigned", "void", "wchar_t", "nullptr", "true", "false",
            // 控制流
            "break", "case", "catch", "continue", "default", "do", "else", "for", "goto", "if", "return", "switch", "throw", "try", "while",
            // 声明与定义
            "class", "const", "constexpr", "consteval", "constinit", "enum", "explicit", "export", "extern", "friend", "inline", "namespace", "new", "delete", "operator", "template", "typename", "typedef", "using", "virtual",
            // 修饰符
            "auto", "mutable", "register", "static", "volatile", "thread_local",
            // 访问控制
            "private", "protected", "public",
            // 特殊
            "this", "decltype", "noexcept", "nullptr", "static_assert", "static_cast", "dynamic_cast", "reinterpret_cast", "const_cast", "typeid", "alignas", "alignof", "concept", "requires"
        )

        /**
         * C# 关键字
         * 包含：.NET 特性、LINQ、异步编程等
         */
        val CSHARP = setOf(
            // 类型
            "bool", "byte", "char", "decimal", "double", "float", "int", "long", "object", "sbyte", "short", "string", "uint", "ulong", "ushort", "void",
            // 控制流
            "break", "case", "catch", "continue", "default", "do", "else", "finally", "for", "foreach", "goto", "if", "return", "switch", "throw", "try", "while",
            // 声明
            "class", "delegate", "enum", "event", "explicit", "extern", "implicit", "interface", "namespace", "new", "operator", "override", "params", "partial", "readonly", "sealed", "stackalloc", "static", "struct", "unsafe", "virtual", "volatile", "abstract", "async", "await",
            // 访问修饰符
            "private", "protected", "public", "internal",
            // LINQ 与查询
            "from", "where", "select", "group", "into", "orderby", "join", "let", "on", "equals", "by", "ascending", "descending",
            // 其他
            "as", "base", "checked", "unchecked", "context", "fixed", "in", "is", "lock", "nameof", "null", "out", "ref", "sizeof", "this", "typeof", "unchecked", "using", "var", "value", "when", "while", "yield"
        )

        /**
         * Ruby 关键字
         */
        val RUBY = setOf(
            "BEGIN", "END", "alias", "and", "begin", "break", "case", "class", "def", "defined?", "do", "else", "elsif", "end", "ensure", "false", "for", "if", "in", "module", "next", "nil", "not", "or", "redo", "rescue", "retry", "return", "self", "super", "then", "true", "undef", "unless", "until", "when", "while", "yield", "__FILE__", "__LINE__", "__ENCODING__"
        )

        /**
         * PHP 关键字（PHP 7+）
         */
        val PHP = setOf(
            "abstract", "and", "array", "as", "break", "callable", "case", "catch", "class", "clone", "const", "continue", "declare", "default", "die", "do", "echo", "else", "elseif", "empty", "enddeclare", "endfor", "endforeach", "endif", "endswitch", "endwhile", "eval", "exit", "extends", "final", "finally", "fn", "for", "foreach", "function", "global", "goto", "if", "implements", "include", "include_once", "instanceof", "insteadof", "interface", "isset", "list", "match", "namespace", "new", "or", "print", "private", "protected", "public", "require", "require_once", "return", "static", "switch", "throw", "trait", "try", "unset", "use", "var", "while", "xor", "yield", "from", "readonly"
        )

        /**
         * Swift 关键字
         */
        val SWIFT = setOf(
            "associatedtype", "as", "break", "case", "catch", "class", "continue", "convenience", "default", "defer", "deinit", "didSet", "do", "dynamic", "else", "enum", "extension", "fallthrough", "fileprivate", "final", "for", "func", "guard", "if", "import", "in", "indirect", "inout", "internal", "is", "lazy", "let", "mutating", "namespace", "none", "nonmutating", "open", "operator", "optional", "override", "postfix", "precedencegroup", "prefix", "private", "protocol", "public", "repeat", "required", "rethrows", "return", "self", "Self", "set", "some", "static", "struct", "subscript", "super", "switch", "throw", "throws", "try", "Type", "typealias", "unowned", "var", "weak", "where", "while", "willSet"
        )

        /**
         * SQL 关键字（通用）
         */
        val SQL = setOf(
            "SELECT", "FROM", "WHERE", "INSERT", "UPDATE", "DELETE", "CREATE", "ALTER", "DROP", "TABLE", "INDEX", "VIEW", "TRIGGER", "PROCEDURE", "FUNCTION", "DATABASE", "SCHEMA",
            "JOIN", "INNER", "LEFT", "RIGHT", "OUTER", "FULL", "CROSS", "ON", "UNION", "ALL",
            "GROUP", "BY", "HAVING", "ORDER", "ASC", "DESC",
            "DISTINCT", "LIMIT", "OFFSET", "TOP",
            "AND", "OR", "NOT", "IN", "BETWEEN", "LIKE", "EXISTS", "IS", "NULL",
            "VALUES", "SET", "DEFAULT", "PRIMARY", "KEY", "FOREIGN", "REFERENCES", "CONSTRAINT", "UNIQUE", "CHECK",
            "CASE", "WHEN", "THEN", "ELSE", "END",
            "AS", "CAST", "CONVERT", "INTO",
            "EXECUTE", "EXEC", "CALL", "RETURN",
            "BEGIN", "COMMIT", "ROLLBACK", "TRANSACTION", "SAVEPOINT",
            "GRANT", "REVOKE", "DENY",
            "CURSOR", "FETCH", "OPEN", "CLOSE",
            "DECLARE", "IF", "ELSEIF", "LOOP", "WHILE", "FOR", "BREAK", "CONTINUE",
            "TRUE", "FALSE", "UNKNOWN"
        )

        /**
         * Shell/Bash 关键字
         */
        val SHELL = setOf(
            "if", "then", "else", "elif", "fi", "case", "esac", "for", "do", "done", "while", "until", "select", "in",
            "function", "return", "exit", "break", "continue",
            "declare", "local", "export", "readonly", "unset",
            "source", "alias", "unalias",
            "trap", "wait",
            "eval", "exec", "command",
            "true", "false",
            "null", "EOF"
        )

        /**
         * HTML 标签名（用于高亮）
         */
        val HTML_TAGS = setOf(
            "html", "head", "body", "title", "meta", "link", "style", "script",
            "header", "footer", "nav", "main", "section", "article", "aside", "div", "span",
            "h1", "h2", "h3", "h4", "h5", "h6", "p", "br", "hr",
            "a", "img", "video", "audio", "source", "track",
            "ul", "ol", "li", "dl", "dt", "dd",
            "table", "thead", "tbody", "tfoot", "tr", "th", "td", "caption",
            "form", "input", "textarea", "button", "select", "option", "label", "fieldset", "legend",
            "iframe", "canvas", "svg", "path", "circle", "rect", "line", "polyline", "polygon",
            "details", "summary", "dialog", "menu", "menuitem",
            "template", "slot", "shadow"
        )

        /**
         * CSS 属性与关键字
         */
        val CSS_KEYWORDS = setOf(
            // 布局
            "display", "position", "top", "right", "bottom", "left", "float", "clear", "z-index",
            "flex", "flex-direction", "flex-wrap", "flex-flow", "justify-content", "align-items", "align-content", "align-self",
            "grid", "grid-template", "grid-template-rows", "grid-template-columns", "grid-template-areas", "grid-auto-rows", "grid-auto-columns", "grid-auto-flow", "grid-column", "grid-row", "grid-area",
            // 盒模型
            "width", "height", "min-width", "max-width", "min-height", "max-height",
            "margin", "margin-top", "margin-right", "margin-bottom", "margin-left",
            "padding", "padding-top", "padding-right", "padding-bottom", "padding-left",
            "border", "border-width", "border-style", "border-color", "border-radius", "border-top", "border-right", "border-bottom", "border-left",
            "box-sizing", "overflow", "overflow-x", "overflow-y",
            // 文本样式
            "font", "font-family", "font-size", "font-weight", "font-style", "line-height",
            "color", "text-align", "text-decoration", "text-transform", "text-indent", "letter-spacing", "word-spacing", "white-space",
            // 背景
            "background", "background-color", "background-image", "background-repeat", "background-position", "background-size", "background-attachment",
            // 动画与变换
            "animation", "animation-name", "animation-duration", "animation-timing-function", "animation-delay", "animation-iteration-count", "animation-direction", "animation-fill-mode",
            "transition", "transition-property", "transition-duration", "transition-timing-function", "transition-delay",
            "transform", "transform-origin", "perspective", "perspective-origin",
            // 其他
            "opacity", "visibility", "cursor", "pointer-events", "user-select",
            "content", "counter-increment", "counter-reset",
            "list-style", "list-style-type", "list-style-position", "list-style-image",
            "vertical-align", "object-fit", "object-position"
        )

        /**
         * 根据语言名称获取对应的关键字集合
         * @param language 语言标识（不区分大小写）
         * @return 对应的关键字集合，若不支持则返回空集合
         */
        fun getKeywordsForLanguage(language: String): Set<String> {
            val lang = language.lowercase()
            return when {
                lang.contains("kotlin") -> KOTLIN
                lang.contains("java") && !lang.contains("javascript") -> JAVA
                lang.contains("javascript") || lang.contains("js") -> JAVASCRIPT
                lang.contains("typescript") || lang.contains("ts") -> TYPESCRIPT
                lang.contains("python") || lang.contains("py") -> PYTHON
                lang.contains("go") || lang.contains("golang") -> GO
                lang.contains("rust") -> RUST
                lang.contains("c++") || lang.contains("cpp") -> CPP
                lang.contains("c#") || lang.contains("csharp") -> CSHARP
                lang.contains("ruby") -> RUBY
                lang.contains("php") -> PHP
                lang.contains("swift") -> SWIFT
                lang.contains("sql") -> SQL
                lang.contains("shell") || lang.contains("bash") || lang.contains("sh") -> SHELL
                lang.contains("html") -> HTML_TAGS
                lang.contains("css") -> CSS_KEYWORDS
                else -> KOTLIN
            }
        }
    }
}
