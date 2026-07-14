package br.com.mo2log.mobile.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.view.View
import kotlin.math.min

enum class Mo2NavIcon {
    Home,
    Strength,
    Running,
    More,
}

@SuppressLint("ViewConstructor")
class Mo2NavIconView(
    context: Context,
    private val icon: Mo2NavIcon,
    tint: Int,
) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = tint
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val size = context.mo2Dp(24)
        setMeasuredDimension(resolveSize(size, widthMeasureSpec), resolveSize(size, heightMeasureSpec))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val size = min(width, height).toFloat()
        val scale = size / 24f
        val left = (width - size) / 2f
        val top = (height - size) / 2f

        canvas.save()
        canvas.translate(left, top)
        canvas.scale(scale, scale)
        paint.strokeWidth = 2f

        when (icon) {
            Mo2NavIcon.Home -> drawHome(canvas)
            Mo2NavIcon.Strength -> drawStrength(canvas)
            Mo2NavIcon.Running -> drawRunning(canvas)
            Mo2NavIcon.More -> drawMore(canvas)
        }
        canvas.restore()
    }

    private fun drawHome(canvas: Canvas) {
        val roof = Path().apply {
            moveTo(3f, 10.5f)
            lineTo(12f, 3.5f)
            lineTo(21f, 10.5f)
        }
        canvas.drawPath(roof, paint)

        val house = Path().apply {
            moveTo(5.5f, 9f)
            lineTo(5.5f, 20.5f)
            lineTo(18.5f, 20.5f)
            lineTo(18.5f, 9f)
            moveTo(9.5f, 20.5f)
            lineTo(9.5f, 14f)
            lineTo(14.5f, 14f)
            lineTo(14.5f, 20.5f)
        }
        canvas.drawPath(house, paint)
    }

    private fun drawStrength(canvas: Canvas) {
        canvas.drawLine(5f, 12f, 19f, 12f, paint)
        canvas.drawRoundRect(2.5f, 8f, 5f, 16f, 1f, 1f, paint)
        canvas.drawRoundRect(5f, 6.5f, 7.5f, 17.5f, 1f, 1f, paint)
        canvas.drawRoundRect(16.5f, 6.5f, 19f, 17.5f, 1f, 1f, paint)
        canvas.drawRoundRect(19f, 8f, 21.5f, 16f, 1f, 1f, paint)
    }

    private fun drawRunning(canvas: Canvas) {
        canvas.drawCircle(14.5f, 4.5f, 2f, paint)
        val body = Path().apply {
            moveTo(13f, 7f)
            lineTo(10f, 11.5f)
            lineTo(13.5f, 14f)
            lineTo(17.5f, 20.5f)
            moveTo(10f, 11.5f)
            lineTo(6f, 15.5f)
            moveTo(11.5f, 9f)
            lineTo(16f, 10.5f)
            lineTo(19.5f, 8.5f)
            moveTo(13.5f, 14f)
            lineTo(9f, 20.5f)
        }
        canvas.drawPath(body, paint)
    }

    private fun drawMore(canvas: Canvas) {
        canvas.drawRoundRect(3.5f, 3.5f, 10f, 10f, 1.5f, 1.5f, paint)
        canvas.drawRoundRect(14f, 3.5f, 20.5f, 10f, 1.5f, 1.5f, paint)
        canvas.drawRoundRect(3.5f, 14f, 10f, 20.5f, 1.5f, 1.5f, paint)
        canvas.drawRoundRect(14f, 14f, 20.5f, 20.5f, 1.5f, 1.5f, paint)
    }
}
