package com.fishmemory.app.ui.publish.richtext.ui.actions

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.Editable
import android.text.Spanned
import android.widget.EditText
import android.widget.PopupMenu
import android.view.View
import android.widget.Toast
import com.fishmemory.app.ui.publish.richtext.business.link.LinkSpan
import com.fishmemory.app.ui.publish.richtext.business.format.BlockActionManager
import com.fishmemory.app.ui.publish.richtext.business.selection.SelectionManager
import com.fishmemory.app.ui.publish.richtext.core.model.EditorBlock
import com.fishmemory.app.ui.publish.richtext.core.engine.validator.EditorUrlRules
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.net.URLDecoder

/**
 * 链接相关 UI 语义：行内链接菜单、LinkCard 菜单、转文字/复制/打开等。
 *它负责处理“链接”相关的所有 UI 语义（User Interface Semantics），具体包括：
 * 菜单展示 (PopupMenu)：长按行内链接或 LinkCard 时弹出的选项菜单。
 * 系统交互 (Clipboard, Toast)：复制链接到剪贴板、显示提示信息。
 * 外部跳转 (Intent)：安全地打开浏览器访问网页。
 * 同时，它明确地将业务逻辑（如将文字转换为链接卡片）和焦点管理交给了 BlockActionManager 和 SelectionManager 等更底层的服务类
 * 设计点：
 * - 结构性转换（如 LinkCard->TextBlock）由 [BlockActionManager] 完成；
 * - 焦点/selection 交给 [SelectionManager]，避免 View 层与焦点算法混在一起；
 * - 本类只负责“菜单/弹窗/Intent/剪贴板”等 UI 语义。
 */
class LinkUiActions(
    private val actionManager: BlockActionManager,
    private val selectionManager: SelectionManager,
    private val onShowToast: (String) -> Unit,  // 抽离 Toast，便于测试和统一样式
    private val onOpenUrl: (String) -> Unit     // 外部传入或内部实现
) {
    fun onInlineLinkClicked(
        context: Context,
        anchorEditText: EditText,
        blockId: String,
        url: String,
        start: Int,
        end: Int,
        editable: Editable
    ) {
        val popup = PopupMenu(context, anchorEditText)
        popup.menu.add(0, 1, 0, "编辑链接")
        popup.menu.add(0, 2, 1, "取消超链接")
        popup.menu.add(0, 3, 2, "切换为卡片样式")
        popup.menu.add(0, 4, 3, "复制链接")

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> {
                    val input = EditText(context).apply {
                        setText(url)
                        setSelection(text.length)
                    }
                    MaterialAlertDialogBuilder(context)
                        .setTitle("编辑链接")
                        .setView(input)
                        .setPositiveButton("确定") { _, _ ->
                            val newUrl = input.text.toString().trim()
                            if (newUrl.isNotEmpty()) {
                                editable.getSpans(start, end, LinkSpan::class.java)
                                    .forEach { editable.removeSpan(it) }
                                editable.setSpan(
                                    LinkSpan(newUrl),
                                    start,
                                    end,
                                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                                )
                            }
                        }
                        .setNegativeButton("取消", null)
                        .show()
                    true
                }

                2 -> {
                    editable.getSpans(start, end, LinkSpan::class.java)
                        .forEach { editable.removeSpan(it) }
                    true
                }

                3 -> {
                    // 仅当“当前行只有一个 URL”时允许转卡片
                    val cursor = anchorEditText.selectionStart.coerceAtLeast(0)
                    val (lineStart, lineEnd) = EditorUrlRules.findCurrentLineRange(editable, cursor)
                    val line = editable.substring(lineStart, lineEnd).trim()
                    if (line == url) {
                        val result = actionManager.transformUrlLineToLinkCard(
                            blockId = blockId,
                            url = url,
                            lineStart = lineStart,
                            lineEnd = lineEnd
                        )
                        if (result?.focusTargetDataPos != null) {
                            selectionManager.focusAfterUrlLineConvertedToLinkCard(
                                focusTargetDataPos = result.focusTargetDataPos,
                                focusSelection = result.focusSelection
                            )
                        }
                    }
                    true
                }

                4 -> {
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    cm?.setPrimaryClip(ClipData.newPlainText("url", url))
                    Toast.makeText(context, "已复制链接", Toast.LENGTH_SHORT).show()
                    true
                }

                else -> false
            }
        }

        popup.show()
    }

    fun showLinkCardMenu(
        context: Context,
        anchor: View,
        block: EditorBlock.LinkCard
    ) {
        val popup = PopupMenu(context, anchor)
        popup.menu.add(0, 1, 0, "访问链接")
        popup.menu.add(0, 2, 1, "切换为文字样式")
        popup.menu.add(0, 3, 2, "复制链接")

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> {
                    try {
                        val rawUrl = block.url.trim()
                        val decodedUrl = try {
                            URLDecoder.decode(rawUrl, "UTF-8")
                        } catch (_: Exception) {
                            rawUrl
                        }

                        val urlWithProtocol = if (
                            decodedUrl.startsWith("http://", ignoreCase = true) ||
                                decodedUrl.startsWith("https://", ignoreCase = true)
                        ) {
                            decodedUrl
                        } else {
                            "https://$decodedUrl"
                        }

                        val intent = android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse(urlWithProtocol)
                        ).apply {
                            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            setPackage(null)
                        }

                        if (intent.resolveActivity(context.packageManager) != null) {
                            context.startActivity(intent)
                            Toast.makeText(context, "正在打开链接...", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "未找到可打开链接的应用", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "无法打开链接：${e.message}", Toast.LENGTH_LONG).show()
                    }
                    true
                }

                2 -> {
                    val result = actionManager.convertLinkCardToTextStructure(block.id)
                    if (result != null) {
                        selectionManager.focusAfterLinkCardConvertedToText(
                            textDataPos = result.textDataPos,
                            selection = result.selection
                        )
                    }
                    true
                }

                3 -> {
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    cm?.setPrimaryClip(ClipData.newPlainText("url", block.url))
                    Toast.makeText(context, "已复制链接", Toast.LENGTH_SHORT).show()
                    true
                }

                else -> false
            }
        }

        popup.show()
    }
    /** 处理 LinkCard 的点击跳转（原 ViewHolder 中的逻辑） */
    fun openLinkCardUrl(context: Context, url: String) {
        if (url.isBlank()) return

        try {
            val urlWithProtocol = when {
                url.startsWith("http://", true) || url.startsWith("https://", true) -> url
                else -> "https://$url"
            }

            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(urlWithProtocol)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                onShowToast("正在打开链接...")
            } else {
                onShowToast("未找到可打开链接的应用")
            }
        } catch (e: Exception) {
            onShowToast("无法打开链接：${e.message}")
        }
    }

    /** 统一处理复制链接（消除重复代码） */
    fun copyLinkToClipboard(context: Context, url: String, label: String = "链接") {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        cm.setPrimaryClip(ClipData.newPlainText("url", url))
        onShowToast("已复制$label")
    }
}

