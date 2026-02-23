package com.fishmemory.app.ui.publish.richtext.config

import android.content.Context
import android.graphics.Color
import android.util.TypedValue

/**
 * 富文本编辑器统一排版规范（Block Editor 与旧版共用）。
 * 遵循知乎风格的排版标准：
 * 正文字号：16px，行高：1.7，段间距：1em(16px)
 * 标题字号：20px-24px，加粗处理
 */
object EditorStyle {

    val TEXT_BODY = TextBlockStyle(
        textSizeSp = 16f,                    // 正文字号 16px
        lineSpacingMultiplier = 1.2f,         // 行高 1.5 (font-size * 1.5)
        lineSpacingExtraDp = 0,               // 不额外增加行间距
        blockMarginDp = 12,                   // 段间距 1em (12px)
        paddingDp = Padding(left = 16, top = 0, right = 16, bottom = 0)
    )

    val TEXT_TITLE = TextBlockStyle(
        textSizeSp = 22f,                     // 标题字号 22px (20-24px 范围内)
        lineSpacingMultiplier = 1.5f,         // 标题行高稍小
        lineSpacingExtraDp = 0,
        blockMarginDp = 20,                   // 标题段间距稍大
        paddingDp = Padding(left = 16, top = 16, right = 16, bottom = 16)
    )

    val TEXT_QUOTE = TextBlockStyle(
        textSizeSp = 16f,
        // 引用块行间距与正文保持一致（1.2 倍），避免视觉上过疏
        lineSpacingMultiplier = 1.2f,
        lineSpacingExtraDp = 0,
        blockMarginDp = 16,
        paddingDp = Padding(left = 20, top = 12, right = 16, bottom = 12)
    )

    val TEXT_LIST = TextBlockStyle(
        textSizeSp = 16f,
        lineSpacingMultiplier = 1.2f,
        lineSpacingExtraDp = 0,
        blockMarginDp = 12,
        // 列表块的缩进主要由前缀容器控制，这里左 padding 置 0，避免重复缩进
        paddingDp = Padding(left = 0, top = 0, right = 16, bottom = 0)
    )

    val IMAGE_BLOCK = ImageBlockStyle(
        marginTopBottomDp = 16,               // 图片上下间距 16px
        paddingDp = Padding(left = 16, top = 0, right = 16, bottom = 0)
    )

    fun dpToPx(context: Context, dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }

    data class Padding(val left: Int, val top: Int, val right: Int, val bottom: Int)

    data class TextBlockStyle(
        val textSizeSp: Float,
        val lineSpacingMultiplier: Float,
        val lineSpacingExtraDp: Int,
        val blockMarginDp: Int,
        val paddingDp: Padding
    )

    data class ImageBlockStyle(
        val marginTopBottomDp: Int,
        val paddingDp: Padding
    )
    /**
     * 代码高亮配色：对齐 GitHub Light 风格。
     */
    object CodeColors {

        val KEYWORD: Int = Color.parseColor("#D73A49")
        val FUNCTION: Int = Color.parseColor("#6F42C1")
        val STRING: Int = Color.parseColor("#032F62")
        val COMMENT: Int = Color.parseColor("#6A737D")
        val NUMBER: Int = Color.parseColor("#005CC5")

        // 代码块选中时使用的紫色边框主题色
        val SELECTED_PURPLE: Int = Color.parseColor("#6200EE")
    }
}