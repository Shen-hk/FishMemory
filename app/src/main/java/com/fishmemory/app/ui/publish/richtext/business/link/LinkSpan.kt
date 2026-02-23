package com.fishmemory.app.ui.publish.richtext.business.link

import android.graphics.Color
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.view.View
import com.fishmemory.app.ui.publish.richtext.ui.view.BlockEditText

/**
 * 行内链接 Span：
 * - 自绘制为蓝色+下划线
 * - 点击后通过 BlockEditText 回调给外层，避免 Span 持有 Activity/RecyclerView 引用
 */
class LinkSpan(
    val url: String
) : ClickableSpan() {

    override fun updateDrawState(ds: TextPaint) {
        super.updateDrawState(ds)
        ds.color = Color.parseColor("#1E6FFF")
        ds.isUnderlineText = true
    }
//    这里的“内化”技术点：解耦艺术
//    这是这段代码最专业的地方。
//
//    A. 拒绝持有外部引用
//    普通做法：新手往往会在 LinkSpan 的构造函数里传入一个 Context 或者 Callback。
//
//    致命后果：Span 是存在于 Editable（内存数据）里的。如果你退出了 Activity，但这个数据还在缓存里，那么它持有的 Activity 引用就会导致内存泄漏。
//
//    你的做法：利用 onClick(widget: View) 里的 widget 参数。
//
//    B. 向上寻址与回调
//
//    val et = widget as? BlockEditText ?: return
//    et.interactionListener?.onLinkClicked(...)
//    原理：既然点击事件传回了当前的 View，那么我们就把 View 强转为你的自定义控件 BlockEditText。
//
//    逻辑：通过控件自带的 interactionListener 把事件“抛”出去。
//
//    好处：LinkSpan 变得非常纯净，它只知道自己是一个链接，不知道外面是谁。这种设计叫 “事件冒泡” 的变体。
    override fun onClick(widget: View) {
        val et = widget as? BlockEditText ?: return
        val editable = et.text ?: return
        val start = editable.getSpanStart(this)
        val end = editable.getSpanEnd(this)
        if (start < 0 || end < 0 || start >= end) return
        et.interactionListener?.onLinkClicked(et.blockId, url, start, end)
    }
}