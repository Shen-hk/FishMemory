package com.fishmemory.app.core.utils.view

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.View
import androidx.core.view.ViewCompat

/**
 * 纯 View/Context 交互扩展：点击反馈动画、震动。使用处：ArticleAdapter 等列表交互。
 */
fun View.clickFeedback(
    scale: Float = 1.2f,
    duration: Long = 100L,
    action: (() -> Unit)? = null
) {
    if (ViewCompat.hasTransientState(this)) return
    animate()
        .scaleX(scale)
        .scaleY(scale)
        .setDuration(duration)
        .withEndAction {
            animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(duration)
                .withEndAction { action?.invoke() }
                .start()
        }
        .start()
}

fun Context.vibrate(duration: Long = 20L, amplitude: Int = 2) {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }
    if (!vibrator.hasVibrator()) return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createOneShot(duration, amplitude.coerceIn(1, 255)))
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(duration)
    }
}
