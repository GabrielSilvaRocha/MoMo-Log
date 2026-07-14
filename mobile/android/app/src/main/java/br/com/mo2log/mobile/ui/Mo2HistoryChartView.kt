package br.com.mo2log.mobile.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.util.TypedValue
import android.view.View
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class Mo2HistoryPoint(
    val label: String,
    val strengthVolume: Double,
    val runningDistance: Double,
)

@SuppressLint("ViewConstructor")
class Mo2HistoryChartView(
    context: Context,
    private val points: List<Mo2HistoryPoint>,
) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Mo2Colors.TextSecondary
        textAlign = Paint.Align.CENTER
        textSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            9f,
            resources.displayMetrics,
        )
        typeface = Typeface.DEFAULT_BOLD
    }

    init {
        minimumHeight = context.mo2Dp(210)
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        isFocusable = true
        contentDescription = points.joinToString(", ", "Evolucao em oito semanas: ") { point ->
            point.label + ", " + point.strengthVolume.roundToInt() + " kg e " +
                String.format(Locale("pt", "BR"), "%.2f km", point.runningDistance)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredHeight = context.mo2Dp(210)
        setMeasuredDimension(
            resolveSize(suggestedMinimumWidth, widthMeasureSpec),
            resolveSize(desiredHeight, heightMeasureSpec),
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (points.isEmpty()) return

        val left = context.mo2Dp(8).toFloat()
        val right = width - context.mo2Dp(8).toFloat()
        val top = context.mo2Dp(12).toFloat()
        val bottom = height - context.mo2Dp(30).toFloat()
        val chartHeight = (bottom - top).coerceAtLeast(1f)

        paint.strokeWidth = context.mo2Dp(1).toFloat()
        paint.color = Mo2Colors.Border
        repeat(4) { index ->
            val y = top + chartHeight * index / 3f
            canvas.drawLine(left, y, right, y, paint)
        }

        val maxStrength = points.maxOfOrNull { it.strengthVolume } ?: 0.0
        val maxDistance = points.maxOfOrNull { it.runningDistance } ?: 0.0
        if (maxStrength <= 0.0 && maxDistance <= 0.0) {
            labelPaint.textSize = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                13f,
                resources.displayMetrics,
            )
            canvas.drawText("Sem atividades no periodo", width / 2f, top + chartHeight / 2f, labelPaint)
            return
        }

        val columnWidth = (right - left) / max(1, points.size)
        val barWidth = min(context.mo2Dp(12).toFloat(), columnWidth * 0.28f).coerceAtLeast(2f)
        val radius = context.mo2Dp(3).toFloat()

        points.forEachIndexed { index, point ->
            val center = left + columnWidth * index + columnWidth / 2f
            val strengthHeight = if (maxStrength <= 0.0) 0f else {
                (chartHeight * (point.strengthVolume / maxStrength)).toFloat()
            }
            val distanceHeight = if (maxDistance <= 0.0) 0f else {
                (chartHeight * (point.runningDistance / maxDistance)).toFloat()
            }

            if (strengthHeight > 0f) {
                paint.color = Mo2Colors.Primary
                canvas.drawRoundRect(
                    center - barWidth - context.mo2Dp(1),
                    bottom - strengthHeight,
                    center - context.mo2Dp(1),
                    bottom,
                    radius,
                    radius,
                    paint,
                )
            }
            if (distanceHeight > 0f) {
                paint.color = Mo2Colors.Running
                canvas.drawRoundRect(
                    center + context.mo2Dp(1),
                    bottom - distanceHeight,
                    center + barWidth + context.mo2Dp(1),
                    bottom,
                    radius,
                    radius,
                    paint,
                )
            }
            canvas.drawText(point.label, center, height - context.mo2Dp(8).toFloat(), labelPaint)
        }
    }
}
