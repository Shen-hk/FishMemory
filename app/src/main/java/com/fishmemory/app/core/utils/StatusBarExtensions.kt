package com.fishmemory.app.core.utils
import android.graphics.Color
import android.os.Build
import android.view.View
import android.view.Window

/**
 * 一行代码设置状态栏图标颜色
 * 在任何Activity中调用：window.setStatusBarIconsWhite()
 */
fun Window.setStatusBarIconsWhite() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        // 设置白色图标
        decorView.systemUiVisibility =
            decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
    }
}

fun Window.setStatusBarIconsBlack() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        // 设置黑色图标
        decorView.systemUiVisibility =
            decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
    }
}

/**
 * 根据背景色自动设置图标颜色
 * @param backgroundColor 背景颜色
 */
fun Window.autoSetStatusBarIcons(backgroundColor: Int) {
    // 简单判断：颜色越深，图标越应该用白色
    val isDarkBackground = isColorDark(backgroundColor)

    if (isDarkBackground) {
        setStatusBarIconsWhite() // 深色背景用白色图标
    } else {
        setStatusBarIconsBlack() // 浅色背景用黑色图标
    }
}

/**
 * 判断颜色是否为深色
 */
private fun isColorDark(color: Int): Boolean {
    // 计算亮度（0-1之间）
    val darkness = 1 - (0.299 * Color.red(color) +
            0.587 * Color.green(color) +
            0.114 * Color.blue(color)) / 255
    // 亮度 > 0.5 认为是深色
    return darkness > 0.5
}