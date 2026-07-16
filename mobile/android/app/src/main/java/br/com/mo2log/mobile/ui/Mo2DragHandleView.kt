package br.com.mo2log.mobile.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View

class Mo2DragHandleView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Mo2Colors.TextSecondary
        strokeWidth = context.mo2Dp(2).toFloat()
        strokeCap = Paint.Cap.ROUND
    }

    init {
        minimumWidth = context.mo2Dp(32)
        minimumHeight = context.mo2Dp(40)
        contentDescription = "Reordenar exercicio"
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val halfLine = context.mo2Dp(8).toFloat()
        val centerX = width / 2f
        val centerY = height / 2f
        val gap = context.mo2Dp(6).toFloat()
        canvas.drawLine(centerX - halfLine, centerY - gap, centerX + halfLine, centerY - gap, paint)
        canvas.drawLine(centerX - halfLine, centerY, centerX + halfLine, centerY, paint)
        canvas.drawLine(centerX - halfLine, centerY + gap, centerX + halfLine, centerY + gap, paint)
    }
}
