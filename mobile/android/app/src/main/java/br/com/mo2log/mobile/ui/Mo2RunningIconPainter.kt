package br.com.mo2log.mobile.ui

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RectF
import br.com.mo2log.mobile.R

internal class Mo2RunningIconPainter(
    context: Context,
    tint: Int,
) {
    private val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.ic_running_sprint)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
        colorFilter = PorterDuffColorFilter(tint, PorterDuff.Mode.SRC_IN)
    }

    fun draw(canvas: Canvas, left: Float, top: Float, size: Float) {
        canvas.drawBitmap(bitmap, null, RectF(left, top, left + size, top + size), paint)
    }

    fun drawCentered(canvas: Canvas, centerX: Float, centerY: Float, size: Float) {
        draw(canvas, centerX - size / 2f, centerY - size / 2f, size)
    }
}
