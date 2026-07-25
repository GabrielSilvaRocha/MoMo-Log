package br.com.mo2log.mobile.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.view.View
import kotlin.math.min

enum class Mo2ActionIcon {
    Back,
    Search,
    Filter,
    Clear,
    Heart,
    HeartFilled,
    More,
    Edit,
    Pause,
    Play,
    Plus,
    Register,
    Warning,
    ChevronRight,
    Star,
    StarFilled,
    Dumbbell,
}

@SuppressLint("ViewConstructor")
class Mo2ActionIconView(
    context: Context,
    icon: Mo2ActionIcon,
    tint: Int,
) : View(context) {
    private var currentIcon = icon
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = tint
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    fun updateIcon(icon: Mo2ActionIcon) {
        currentIcon = icon
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val size = context.mo2Dp(24)
        setMeasuredDimension(resolveSize(size, widthMeasureSpec), resolveSize(size, heightMeasureSpec))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val size = min(width, height).toFloat()
        val scale = size / 24f
        canvas.save()
        canvas.translate((width - size) / 2f, (height - size) / 2f)
        canvas.scale(scale, scale)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f

        when (currentIcon) {
            Mo2ActionIcon.Back -> drawBack(canvas)
            Mo2ActionIcon.Search -> drawSearch(canvas)
            Mo2ActionIcon.Filter -> drawFilter(canvas)
            Mo2ActionIcon.Clear -> drawClear(canvas)
            Mo2ActionIcon.Heart -> drawHeart(canvas, false)
            Mo2ActionIcon.HeartFilled -> drawHeart(canvas, true)
            Mo2ActionIcon.More -> drawMore(canvas)
            Mo2ActionIcon.Edit -> drawEdit(canvas)
            Mo2ActionIcon.Pause -> drawPause(canvas)
            Mo2ActionIcon.Play -> drawPlay(canvas)
            Mo2ActionIcon.Plus -> drawPlus(canvas)
            Mo2ActionIcon.Register -> drawRegister(canvas)
            Mo2ActionIcon.Warning -> drawWarning(canvas)
            Mo2ActionIcon.ChevronRight -> drawChevron(canvas)
            Mo2ActionIcon.Star -> drawStar(canvas, false)
            Mo2ActionIcon.StarFilled -> drawStar(canvas, true)
            Mo2ActionIcon.Dumbbell -> drawDumbbell(canvas)
        }
        canvas.restore()
    }

    private fun drawBack(canvas: Canvas) {
        canvas.drawLine(20f, 12f, 4f, 12f, paint)
        canvas.drawLine(4f, 12f, 10f, 6f, paint)
        canvas.drawLine(4f, 12f, 10f, 18f, paint)
    }

    private fun drawSearch(canvas: Canvas) {
        canvas.drawCircle(10.5f, 10.5f, 6.5f, paint)
        canvas.drawLine(15.2f, 15.2f, 21f, 21f, paint)
    }

    private fun drawFilter(canvas: Canvas) {
        canvas.drawLine(4f, 6f, 20f, 6f, paint)
        canvas.drawLine(4f, 12f, 20f, 12f, paint)
        canvas.drawLine(4f, 18f, 20f, 18f, paint)
        canvas.drawCircle(8f, 6f, 2f, paint)
        canvas.drawCircle(16f, 12f, 2f, paint)
        canvas.drawCircle(10f, 18f, 2f, paint)
    }

    private fun drawClear(canvas: Canvas) {
        canvas.drawLine(6f, 6f, 18f, 18f, paint)
        canvas.drawLine(18f, 6f, 6f, 18f, paint)
    }

    private fun drawHeart(canvas: Canvas, filled: Boolean) {
        val path = Path().apply {
            moveTo(12f, 20.5f)
            cubicTo(10f, 18.5f, 4f, 14.4f, 4f, 9.2f)
            cubicTo(4f, 5.8f, 6.4f, 3.5f, 9.4f, 3.5f)
            cubicTo(11.2f, 3.5f, 12f, 4.5f, 12f, 4.5f)
            cubicTo(12f, 4.5f, 12.8f, 3.5f, 14.6f, 3.5f)
            cubicTo(17.6f, 3.5f, 20f, 5.8f, 20f, 9.2f)
            cubicTo(20f, 14.4f, 14f, 18.5f, 12f, 20.5f)
            close()
        }
        paint.style = if (filled) Paint.Style.FILL else Paint.Style.STROKE
        canvas.drawPath(path, paint)
    }

    private fun drawMore(canvas: Canvas) {
        paint.style = Paint.Style.FILL
        canvas.drawCircle(12f, 5f, 1.6f, paint)
        canvas.drawCircle(12f, 12f, 1.6f, paint)
        canvas.drawCircle(12f, 19f, 1.6f, paint)
    }

    private fun drawEdit(canvas: Canvas) {
        val body = Path().apply {
            moveTo(4f, 20f)
            lineTo(8.5f, 19f)
            lineTo(19f, 8.5f)
            lineTo(15.5f, 5f)
            lineTo(5f, 15.5f)
            close()
        }
        canvas.drawPath(body, paint)
        canvas.drawLine(13.8f, 6.8f, 17.2f, 10.2f, paint)
    }

    private fun drawPause(canvas: Canvas) {
        paint.strokeWidth = 3f
        canvas.drawLine(9f, 6f, 9f, 18f, paint)
        canvas.drawLine(15f, 6f, 15f, 18f, paint)
    }

    private fun drawPlay(canvas: Canvas) {
        val path = Path().apply {
            moveTo(8f, 5f)
            lineTo(19f, 12f)
            lineTo(8f, 19f)
            close()
        }
        canvas.drawPath(path, paint)
    }

    private fun drawPlus(canvas: Canvas) {
        canvas.drawLine(12f, 5f, 12f, 19f, paint)
        canvas.drawLine(5f, 12f, 19f, 12f, paint)
    }

    private fun drawRegister(canvas: Canvas) {
        canvas.drawRoundRect(5f, 5f, 19f, 21f, 2f, 2f, paint)
        canvas.drawRoundRect(9f, 3f, 15f, 7f, 1f, 1f, paint)
        canvas.drawLine(9f, 12f, 11f, 14f, paint)
        canvas.drawLine(11f, 14f, 15f, 10f, paint)
        canvas.drawLine(9f, 18f, 16f, 18f, paint)
    }

    private fun drawWarning(canvas: Canvas) {
        val triangle = Path().apply {
            moveTo(12f, 3f)
            lineTo(22f, 20f)
            lineTo(2f, 20f)
            close()
        }
        canvas.drawPath(triangle, paint)
        canvas.drawLine(12f, 9f, 12f, 14f, paint)
        paint.style = Paint.Style.FILL
        canvas.drawCircle(12f, 17f, 1f, paint)
    }

    private fun drawChevron(canvas: Canvas) {
        canvas.drawLine(9f, 5f, 16f, 12f, paint)
        canvas.drawLine(16f, 12f, 9f, 19f, paint)
    }

    private fun drawStar(canvas: Canvas, filled: Boolean) {
        val path = Path().apply {
            moveTo(12f, 3f)
            lineTo(14.8f, 8.7f)
            lineTo(21f, 9.6f)
            lineTo(16.5f, 14f)
            lineTo(17.6f, 20.2f)
            lineTo(12f, 17.3f)
            lineTo(6.4f, 20.2f)
            lineTo(7.5f, 14f)
            lineTo(3f, 9.6f)
            lineTo(9.2f, 8.7f)
            close()
        }
        paint.style = if (filled) Paint.Style.FILL else Paint.Style.STROKE
        canvas.drawPath(path, paint)
    }

    private fun drawDumbbell(canvas: Canvas) {
        canvas.drawLine(5f, 12f, 19f, 12f, paint)
        canvas.drawRoundRect(2.5f, 8f, 5f, 16f, 1f, 1f, paint)
        canvas.drawRoundRect(5f, 6.5f, 7.5f, 17.5f, 1f, 1f, paint)
        canvas.drawRoundRect(16.5f, 6.5f, 19f, 17.5f, 1f, 1f, paint)
        canvas.drawRoundRect(19f, 8f, 21.5f, 16f, 1f, 1f, paint)
    }
}
