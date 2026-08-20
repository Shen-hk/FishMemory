package com.fishmemory.app.ui.publish.richtext.business.selection

/**
 * 块级结构操作的“焦点交接”结果。
 *
 * - `action layer`（如 BlockActionManager）只负责结构变更与 notify 顺序；
 * - `selection layer`（SelectionManager）负责 scroll/tryFocus/postDelayed/retry 等 UI 时序。
 *
 * 返回为 null 表示“本次按键未被结构逻辑处理”，应让系统执行默认行为。
 */
data class OperationFocusResult(
    val focusTargetDataPos: Int?,
    val focusSelection: Int = 0
)

