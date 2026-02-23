package com.fishmemory.app.ui.common

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.sqrt

/**
 * 底部导航栏弧形背景自定义 View，与 FAB 配合使用。
 * 职责：根据 FAB 位移绘制弧形描边与填充，不处理点击与业务逻辑。
 */
class BottomNavWithFab @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        private const val DEFAULT_DISTANCE = 50f
        private const val RADIUS_CORNER = 50f
    }

    private val paint by lazy {
        Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#ffcecece")
            strokeWidth = 2f
            style = Paint.Style.STROKE
        }
    }

    private val fillPaint by lazy {
        Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            style = Paint.Style.FILL
        }
    }

    private var distance = DEFAULT_DISTANCE

    private val radiusCentral: Float
        get() = RADIUS_CORNER + 2 * distance

    fun updateDistance(newDistance: Float) {
        distance = (DEFAULT_DISTANCE - newDistance)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val leftCenterX = centerX - sqrt(3f) * (RADIUS_CORNER + distance)
        val rightCenterX = centerX + sqrt(3f) * (RADIUS_CORNER + distance)

        val bgPath = Path().apply {
            moveTo(0f, 0f)
            if (distance >= -10f) {
                lineTo(leftCenterX, 0f)
                arcTo(
                    leftCenterX - RADIUS_CORNER,
                    0f,
                    leftCenterX + RADIUS_CORNER,
                    2 * RADIUS_CORNER,
                    -90f,
                    60f,
                    false
                )
                arcTo(
                    centerX - radiusCentral,
                    -distance - radiusCentral,
                    centerX + radiusCentral,
                    -distance + radiusCentral,
                    150f,
                    -120f,
                    false
                )
                arcTo(
                    rightCenterX - RADIUS_CORNER,
                    0f,
                    rightCenterX + RADIUS_CORNER,
                    2 * RADIUS_CORNER,
                    -150f,
                    60f,
                    false
                )
                lineTo(width.toFloat(), 0f)
            } else {
                lineTo(width.toFloat(), 0f)
            }
            lineTo(width.toFloat(), height.toFloat())
            lineTo(0f, height.toFloat())
            close()
        }

        canvas.drawPath(bgPath, fillPaint)
        canvas.drawPath(bgPath, paint)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(80.dpToPx(context), MeasureSpec.EXACTLY))
    }

    private fun Int.dpToPx(context: Context): Int {
        return (this * context.resources.displayMetrics.density).toInt()
    }
}
