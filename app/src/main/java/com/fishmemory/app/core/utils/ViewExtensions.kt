package com.fishmemory.app.core.utils

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.View
import androidx.core.view.ViewCompat

/**
 * 为 View 添加点击缩放反馈动画
 * @param scale 放大比例，默认 1.2f（即放大 20%）
 * @param duration 动画持续时间（毫秒），默认 100ms
 * @param action 动画完全结束后执行的回调（可选）
 */
fun View.clickFeedback(
    scale: Float = 1.2f,
    duration: Long = 100L,
    action: (() -> Unit)? = null
) {
    // 防止重复触发动画（可选增强）
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
                .withEndAction {
                    action?.invoke()
                }
                .start()
        }
        .start()
}

/**
 * 在 Context 上提供轻量震动反馈（例如点击按钮时）
 *
 * @param duration 震动时长（毫秒），默认 50ms（轻微短震）
 */
fun Context.vibrate(duration: Long = 20L, amplitude: Int = 2) {
    // 幅度范围：1 ~ 255（255 最强，1 最弱）
    // 建议值：64（很轻）、128（中等偏轻）、192（中等）
    // 注意：amplitude 必须 >= 1

    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    if (!vibrator.hasVibrator()) return

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        // 使用自定义 amplitude（轻震动）
        val safeAmplitude = amplitude.coerceIn(1, 255)
        vibrator.vibrate(VibrationEffect.createOneShot(duration, safeAmplitude))
    } else {
        // Android 7.1 及以下：无法控制强度，只能用默认震动
        @Suppress("DEPRECATION")
        vibrator.vibrate(duration)
    }
}