package com.fishmemory.app.core.utils.view

import android.graphics.Color
import android.os.Build
import android.view.View
import android.view.Window

/**
 * 状态栏图标颜色扩展（Window）。使用处：MainActivity 等需统一状态栏样式的页面。
 */
fun Window.setStatusBarIconsWhite() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        decorView.systemUiVisibility =
            decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
    }
}

fun Window.setStatusBarIconsBlack() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        decorView.systemUiVisibility =
            decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
    }
}

fun Window.autoSetStatusBarIcons(backgroundColor: Int) {
    if (isColorDark(backgroundColor)) setStatusBarIconsWhite()
    else setStatusBarIconsBlack()
}

private fun isColorDark(color: Int): Boolean {
    val darkness = 1 - (0.299 * Color.red(color) +
            0.587 * Color.green(color) +
            0.114 * Color.blue(color)) / 255
    return darkness > 0.5
}
