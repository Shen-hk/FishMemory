package com.fishmemory.app.ui.publish.richtext.ui.view

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.util.Log
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import androidx.appcompat.widget.AppCompatEditText
import com.fishmemory.app.ui.publish.richtext.config.EditorConfig
import com.fishmemory.app.ui.publish.richtext.core.BlockInteractionListener
import com.fishmemory.app.ui.publish.richtext.business.selection.EditorSelectionHelper

/**
 * Block 内 EditText：富文本编辑器的核心输入组件。
 *
 * ## 核心职责
 *
 * ### 1. 文本输入与编辑
 * - 支持多行文本输入、选择、复制、粘贴
 * - 保留 SpannableStringBuilder，支持富文本样式（粗体、斜体、下划线、链接等）
 * - 所有文本修改通过 Editable 接口，禁止调用 setText() 重建
 * - 实时监听文本变化，通过 [onTextChanged] 通知草稿自动保存
 *
 * ### 2. 用户交互事件处理
 * - **回车键**：根据上下文执行不同逻辑
 *   - 优先：URL 行转换为链接卡片（[com.fishmemory.app.ui.publish.richtext.core.BlockInteractionListener.onEnterRequested]）
 *   - 其次：分裂当前文本块为两个独立块（[com.fishmemory.app.ui.publish.richtext.core.BlockInteractionListener.onSplitRequested]）
 * - **退格键（在块首）**：
 *   - 优先：取消引用等特殊样式（[com.fishmemory.app.ui.publish.richtext.core.BlockInteractionListener.onBackspaceAtStart]）
 *   - 其次：与前一个文本块合并（[com.fishmemory.app.ui.publish.richtext.core.BlockInteractionListener.onMergeRequested]）
 *   - 兜底：空块时允许删除
 * - **光标移动**：延迟 [com.fishmemory.app.ui.publish.richtext.model.EditorConfig.Style.CURSOR_VISIBLE_POST_DELAY] ms 后自动滚动确保光标可见
 * - **焦点变化**：通知外层容器滚动到该块位置（[com.fishmemory.app.ui.publish.richtext.core.BlockInteractionListener.onFocusGained]）
 * - **链接点击**：拦截行内超链接点击事件，弹出操作菜单（编辑态）
 *
 * ### 3. 委托模式设计
 * - 通过 [com.fishmemory.app.ui.publish.richtext.core.BlockInteractionListener] 接口将所有块级操作委托给外部处理
 * - 自身仅负责文本输入，不关心块级逻辑（分裂、合并、样式变更等）
 * - 优势：
 *   - **职责分离**：输入框专注输入，外层专注块管理
 *   - **可测试性**：回调逻辑可以独立单元测试
 *   - **可扩展性**：新增块类型或交互只需扩展接口实现
 *
 * ### 4. 性能优化
 * - 使用 [postDelayed] 而非 [post] 处理光标滚动，避免频繁布局
 * - 延迟时间由 [com.fishmemory.app.ui.publish.richtext.model.EditorConfig.Style.CURSOR_VISIBLE_POST_DELAY] 统一配置
 * - 按键事件前置检查（输入法组合文本），避免误判
 * 虽然 postDelayed 更好用，但它有两个小坑，你在面试时可以提到：
 *
 * 内存泄漏： 如果 Activity 已经销毁了，但延迟的 Runnable 还在队列里，可能会导致内存泄漏。
 *
 * 对策： 在 onDetachedFromWindow 中调用 removeCallbacks。
 *
 * 用户感知： 延迟不能太长（建议 30ms - 100ms）。如果设为 500ms，用户会感觉到光标跳动，体验很差。
 *
 * ## 架构关系
  *
 * @see com.fishmemory.app.ui.publish.richtext.core.BlockInteractionListener 交互监听器接口，定义所有块级回调
 * @see TextBlockView 文本块的视图容器
 * @see com.fishmemory.app.ui.publish.richtext.ui.container.BlockEditorRecyclerView 块级操作的实际执行者，实现 [com.fishmemory.app.ui.publish.richtext.core.BlockInteractionListener]
 * @see EditorBlock.TextBlock 数据模型
 * @see com.fishmemory.app.ui.publish.richtext.model.EditorConfig 全局配置常量（延迟时间、日志标签等）
 */
class BlockEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AppCompatEditText(context, attrs) {
    init {
        // 部分输入法回车不会分发 KEYCODE_ENTER，而是走 editor action。
        // 这里做兜底转发，保证“回车转卡片/分裂”语义一致。
        setOnEditorActionListener { _, actionId, event ->
            if (event != null) return@setOnEditorActionListener false
            if (EditorSelectionHelper.hasComposingText(this)) return@setOnEditorActionListener false

            val isImeEnterLike =
                actionId == EditorInfo.IME_NULL ||
                    actionId == EditorInfo.IME_ACTION_DONE ||
                    actionId == EditorInfo.IME_ACTION_GO ||
                    actionId == EditorInfo.IME_ACTION_SEND

            if (!isImeEnterLike) return@setOnEditorActionListener false
            handleEnterAtCurrentCursor()
        }
    }


    /** 唯一标识符，用于定位具体的文本块 */
    var blockId: String = ""

    /** 交互监听器：将所有块级操作委托给外部处理 */
    var interactionListener: BlockInteractionListener? = null

    /**
     * 光标选择变化处理
     *
     * 延迟 [com.fishmemory.app.ui.publish.richtext.model.EditorConfig.Style.CURSOR_VISIBLE_POST_DELAY] ms 后执行滚动，确保：
     * 1. 光标位置在可视范围内
     * 2. 父级滚动容器（如 NestedScrollView）将当前行滚动到可见区域
     *
     * 使用延迟而非立即执行的原因：
     * - 等待布局完成，避免获取错误的光标位置
     * - 减少频繁滚动带来的性能开销
     */
    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        super.onSelectionChanged(selStart, selEnd)
        // 使用常量配置延迟时间
        postDelayed({
            val currentSelection = selectionStart
            if (currentSelection >= 0) {
                bringPointIntoView(currentSelection)
                val r = Rect()
                getFocusedRect(r)
                requestRectangleOnScreen(r, true)
            }
        }, EditorConfig.Style.CURSOR_VISIBLE_POST_DELAY)
    }

    /**
     * 焦点变化处理
     *
     * 当获得焦点时，通过 [interactionListener] 通知外层容器滚动到该块位置
     *
     * @param focused 是否获得焦点
     * @param direction 焦点移动方向
     * @param previouslyFocusedRect 之前获得焦点的矩形区域
     */
    override fun onFocusChanged(
        focused: Boolean,
        direction: Int,
        previouslyFocusedRect: Rect?
    ) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect)
        if (blockId.isEmpty()) return
        if (focused) {
            interactionListener?.onFocusGained(blockId)
        } else {
            interactionListener?.onFocusLost(blockId)
        }
    }

    /**
     * 物理按键按下事件处理
     *
     * ## 处理逻辑
     *
     * ### 回车键 (KEYCODE_ENTER)
     * 1. 检查是否为普通按下（非 Shift+Enter）
     * 2. 优先尝试特殊处理：URL 行转卡片 ([BlockInteractionListener.onEnterRequested])
     * 3. 其次尝试分块逻辑：分裂为两个文本块 ([BlockInteractionListener.onSplitRequested])
     * 4. 如果都未处理，执行系统默认回车行为
     *
     * ### 退格键 (KEYCODE_DEL)
     * 1. 检查光标是否在块首 ([com.fishmemory.app.ui.publish.richtext.utils.EditorSelectionHelper.isCursorAtStart])
     * 2. 优先尝试特殊处理：取消引用等 ([BlockInteractionListener.onBackspaceAtStart])
     * 3. 其次尝试合并：与前一块合并 ([BlockInteractionListener.onMergeRequested])
     * 4. 如果都未处理：
     *    - 空块：允许系统删除
     *    - 非空块：阻止删除（返回 true）
     *
     * ### 优化检查
     * - 事件为 null 时直接返回系统默认
     * - 输入法组合文本时（拼音等）直接返回系统默认，避免误判
     *
     * @return true 表示已消费该按键事件，false 表示交给系统处理
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (event == null || EditorSelectionHelper.hasComposingText(this)) return super.onKeyDown(keyCode, event)

        when (keyCode) {
            KeyEvent.KEYCODE_ENTER -> {
                if (event.action == KeyEvent.ACTION_DOWN && !event.isShiftPressed) {
                    if (handleEnterAtCurrentCursor()) return true
                }
            }
            KeyEvent.KEYCODE_DEL -> {
                if (event.action == KeyEvent.ACTION_DOWN && EditorSelectionHelper.isCursorAtStart(this)) {
                    // 先尝试回调处理
                    if (interactionListener?.onBackspaceAtStart(blockId) != null ||
                        interactionListener?.onMergeRequested(blockId) != null) {
                        return true
                    }

                    // 回调未处理，检查空块情况
                    if (text?.isEmpty() == true) {
                        return false  // 允许系统删除空块
                    }

                    return true  // 非空块且未处理，阻止删除
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun handleEnterAtCurrentCursor(): Boolean {
        val cursor = selectionStart.coerceAtLeast(0)
        Log.d(EditorConfig.TAG_EDIT_TEXT, "Enter pressed at cursor=$cursor, textLength=${text?.length}")

        // 逻辑转发：优先尝试 URL 转卡片，其次尝试分裂
        val enterResult = interactionListener?.onEnterRequested(blockId, cursor)
        Log.d(EditorConfig.TAG_EDIT_TEXT, "onEnterRequested returned: $enterResult")
        if (enterResult == true) {
            Log.d(EditorConfig.TAG_EDIT_TEXT, "Enter handled by onEnterRequested.")
            return true
        }

        val splitResult = interactionListener?.onSplitRequested(blockId, cursor)
        Log.d(EditorConfig.TAG_EDIT_TEXT, "onSplitRequested returned: $splitResult")
        if (splitResult != null) {
            Log.d(EditorConfig.TAG_EDIT_TEXT, "Enter handled by onSplitRequested.")
            return true
        }

        Log.d(EditorConfig.TAG_EDIT_TEXT, "Enter not handled, will use default behavior.")
        return false
    }

    /**
     * 文本内容变化监听
     *
     * 任意文本修改（输入、删除、粘贴等）都会触发此方法
     * 通过 [interactionListener] 通知外层标记草稿为"dirty"状态
     *
     * @param text 当前文本内容
     * @param start 变化起始位置
     * @param lengthBefore 变化前长度
     * @param lengthAfter 变化后长度
     */
    override fun onTextChanged(text: CharSequence?, start: Int, lengthBefore: Int, lengthAfter: Int) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter)
        interactionListener?.onContentChanged(blockId)
    }
}