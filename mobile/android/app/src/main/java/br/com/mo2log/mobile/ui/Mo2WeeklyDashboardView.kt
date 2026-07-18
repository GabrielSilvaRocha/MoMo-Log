package br.com.mo2log.mobile.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RectF
import android.graphics.Typeface
import android.util.TypedValue
import android.view.View
import br.com.mo2log.mobile.R
import java.util.Locale
import kotlin.math.min

data class Mo2WeeklyDashboardData(
    val strengthWorkouts: Int,
    val strengthWorkoutTarget: Int,
    val runningWorkouts: Int,
    val runningWorkoutTarget: Int,
    val completedSets: Int,
    val setTarget: Int,
    val volumeKg: Int,
    val distanceKm: Double,
    val activitySeconds: Long,
)

object Mo2WeeklyDashboardFormatter {
    fun percent(value: Int, target: Int): Int {
        if (target <= 0) return 0
        return ((value.toDouble() / target.toDouble()) * 100.0).toInt().coerceIn(0, 100)
    }

    fun distance(value: Double): String = String.format(Locale("pt", "BR"), "%.2f km", value.coerceAtLeast(0.0))

    fun activityTime(totalSeconds: Long): String {
        val minutes = totalSeconds.coerceAtLeast(0L) / 60L
        return (minutes / 60L).toString() + "h " + (minutes % 60L).toString().padStart(2, '0') + "min"
    }
}

@SuppressLint("ViewConstructor")
class Mo2WeeklyDashboardView(
    context: Context,
    private val data: Mo2WeeklyDashboardData,
) : View(context) {
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Mo2Colors.TextPrimary
        textAlign = Paint.Align.CENTER
    }
    private val runner = BitmapFactory.decodeResource(resources, R.drawable.ic_running_sprint)
    private val runnerPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
        colorFilter = PorterDuffColorFilter(Mo2Colors.Running, PorterDuff.Mode.SRC_IN)
    }

    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        isFocusable = true
        contentDescription = buildString {
            append("Resumo desta semana. Musculacao ")
            append(data.strengthWorkouts).append(" de ").append(data.strengthWorkoutTarget).append(" treinos. ")
            append("Corrida ").append(data.runningWorkouts).append(" de ").append(data.runningWorkoutTarget).append(" treinos. ")
            append(data.completedSets).append(" de ").append(data.setTarget).append(" series. ")
            append(data.volumeKg).append(" quilos de volume. ")
            append(Mo2WeeklyDashboardFormatter.distance(data.distanceKm)).append(" de corrida. ")
            append(Mo2WeeklyDashboardFormatter.activityTime(data.activitySeconds)).append(" de atividade.")
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(
            resolveSize(suggestedMinimumWidth, widthMeasureSpec),
            resolveSize(context.mo2Dp(252), heightMeasureSpec),
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width <= 0 || height <= 0) return

        val borderInset = context.mo2Dp(1).toFloat()
        val card = RectF(borderInset, borderInset, width - borderInset, height - borderInset)
        fillPaint.color = Mo2Colors.SurfaceElevated
        canvas.drawRoundRect(card, context.mo2Dp(16).toFloat(), context.mo2Dp(16).toFloat(), fillPaint)
        strokePaint.color = Mo2Colors.Border
        strokePaint.strokeWidth = context.mo2Dp(1).toFloat()
        canvas.drawRoundRect(card, context.mo2Dp(16).toFloat(), context.mo2Dp(16).toFloat(), strokePaint)

        val leftWidth = min(width * 0.38f, context.mo2Dp(132).toFloat())
        val verticalGap = context.mo2Dp(12).toFloat()
        val outerDiameter = min(leftWidth - context.mo2Dp(14), (height - verticalGap * 3f) / 2f)
        val ringCenterX = leftWidth / 2f
        val topCenterY = verticalGap + outerDiameter / 2f
        val bottomCenterY = verticalGap * 2f + outerDiameter * 1.5f

        drawProgressRing(
            canvas,
            ringCenterX,
            topCenterY,
            outerDiameter,
            Mo2WeeklyDashboardFormatter.percent(data.strengthWorkouts, data.strengthWorkoutTarget),
            data.strengthWorkouts.toString() + "/" + data.strengthWorkoutTarget,
            Mo2Colors.Primary,
            false,
        )
        drawProgressRing(
            canvas,
            ringCenterX,
            bottomCenterY,
            outerDiameter,
            Mo2WeeklyDashboardFormatter.percent(data.runningWorkouts, data.runningWorkoutTarget),
            data.runningWorkouts.toString() + "/" + data.runningWorkoutTarget,
            Mo2Colors.Running,
            true,
        )

        val metricWidth = width - leftWidth
        val columnWidth = metricWidth / 2f
        val firstX = leftWidth + columnWidth / 2f
        val secondX = leftWidth + columnWidth * 1.5f
        val topIconY = height * 0.205f
        val topValueY = height * 0.392f
        val bottomIconY = height * 0.66f
        val bottomValueY = height * 0.875f

        drawMetric(canvas, firstX, topIconY, topValueY, data.completedSets.toString() + " / " + data.setTarget, MetricIcon.Sets, Mo2Colors.Primary, Color.rgb(20, 83, 45), columnWidth)
        drawMetric(canvas, secondX, topIconY, topValueY, data.volumeKg.toString() + " kg", MetricIcon.Volume, Mo2Colors.Warning, Color.rgb(112, 80, 0), columnWidth)
        drawMetric(canvas, firstX, bottomIconY, bottomValueY, Mo2WeeklyDashboardFormatter.distance(data.distanceKm), MetricIcon.Distance, Mo2Colors.Running, Color.rgb(7, 89, 133), columnWidth)
        drawMetric(canvas, secondX, bottomIconY, bottomValueY, Mo2WeeklyDashboardFormatter.activityTime(data.activitySeconds), MetricIcon.Time, Color.rgb(45, 212, 191), Color.rgb(17, 94, 89), columnWidth)
    }

    private fun drawProgressRing(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        diameter: Float,
        percent: Int,
        count: String,
        accent: Int,
        running: Boolean,
    ) {
        val stroke = context.mo2Dp(8).toFloat()
        val radius = diameter / 2f - stroke / 2f
        val ring = RectF(centerX - radius, centerY - radius, centerX + radius, centerY + radius)
        strokePaint.strokeWidth = stroke
        strokePaint.color = Color.rgb(74, 96, 120)
        canvas.drawOval(ring, strokePaint)
        if (percent > 0) {
            strokePaint.color = accent
            canvas.drawArc(ring, -90f, 360f * percent / 100f, false, strokePaint)
        }

        val iconCenterY = centerY - context.mo2Dp(29)
        if (running) drawRunner(canvas, centerX, iconCenterY) else drawDumbbell(canvas, centerX, iconCenterY, accent, context.mo2Dp(24).toFloat())
        drawCenteredText(canvas, percent.toString() + "%", centerX, centerY + context.mo2Dp(1), 22f, Mo2Colors.TextPrimary, true, diameter - stroke * 2f)
        drawCenteredText(canvas, count, centerX, centerY + context.mo2Dp(29), 12f, Mo2Colors.TextSecondary, false, diameter - stroke * 2f)
    }

    private fun drawMetric(
        canvas: Canvas,
        centerX: Float,
        iconCenterY: Float,
        valueCenterY: Float,
        value: String,
        icon: MetricIcon,
        accent: Int,
        tileColor: Int,
        availableWidth: Float,
    ) {
        val tileSize = context.mo2Dp(50).toFloat()
        fillPaint.color = tileColor
        val tile = RectF(centerX - tileSize / 2f, iconCenterY - tileSize / 2f, centerX + tileSize / 2f, iconCenterY + tileSize / 2f)
        canvas.drawRoundRect(tile, context.mo2Dp(10).toFloat(), context.mo2Dp(10).toFloat(), fillPaint)
        when (icon) {
            MetricIcon.Sets -> drawChecklist(canvas, centerX, iconCenterY, accent)
            MetricIcon.Volume -> drawDumbbell(canvas, centerX, iconCenterY, accent, context.mo2Dp(31).toFloat())
            MetricIcon.Distance -> drawPin(canvas, centerX, iconCenterY, accent)
            MetricIcon.Time -> drawClock(canvas, centerX, iconCenterY, accent)
        }
        drawCenteredText(canvas, value, centerX, valueCenterY, 18f, Mo2Colors.TextPrimary, false, availableWidth - context.mo2Dp(8))
    }

    private fun drawRunner(canvas: Canvas, centerX: Float, centerY: Float) {
        val size = context.mo2Dp(24).toFloat()
        canvas.drawBitmap(runner, null, RectF(centerX - size / 2f, centerY - size / 2f, centerX + size / 2f, centerY + size / 2f), runnerPaint)
    }

    private fun drawDumbbell(canvas: Canvas, centerX: Float, centerY: Float, color: Int, size: Float) {
        strokePaint.color = color
        strokePaint.strokeWidth = context.mo2Dp(3).toFloat()
        val halfWidth = size / 2f
        val innerX = size * 0.27f
        val outerX = size * 0.43f
        val innerY = size * 0.38f
        val outerY = size * 0.30f
        canvas.drawLine(centerX - halfWidth, centerY, centerX + halfWidth, centerY, strokePaint)
        canvas.drawLine(centerX - innerX, centerY - innerY, centerX - innerX, centerY + innerY, strokePaint)
        canvas.drawLine(centerX + innerX, centerY - innerY, centerX + innerX, centerY + innerY, strokePaint)
        canvas.drawLine(centerX - outerX, centerY - outerY, centerX - outerX, centerY + outerY, strokePaint)
        canvas.drawLine(centerX + outerX, centerY - outerY, centerX + outerX, centerY + outerY, strokePaint)
    }

    private fun drawChecklist(canvas: Canvas, centerX: Float, centerY: Float, color: Int) {
        strokePaint.color = color
        strokePaint.strokeWidth = context.mo2Dp(3).toFloat()
        val gap = context.mo2Dp(10).toFloat()
        for (offset in floatArrayOf(-gap, 0f, gap)) {
            val y = centerY + offset
            canvas.drawLine(centerX - context.mo2Dp(15), y, centerX - context.mo2Dp(11), y + context.mo2Dp(4), strokePaint)
            canvas.drawLine(centerX - context.mo2Dp(11), y + context.mo2Dp(4), centerX - context.mo2Dp(5), y - context.mo2Dp(4), strokePaint)
            canvas.drawLine(centerX + context.mo2Dp(1), y, centerX + context.mo2Dp(15), y, strokePaint)
        }
    }

    private fun drawPin(canvas: Canvas, centerX: Float, centerY: Float, color: Int) {
        strokePaint.color = color
        strokePaint.strokeWidth = context.mo2Dp(3).toFloat()
        val scale = context.mo2Dp(1).toFloat()
        val path = Path().apply {
            moveTo(centerX, centerY + 17f * scale)
            cubicTo(centerX - 4f * scale, centerY + 10f * scale, centerX - 11f * scale, centerY + 1f * scale, centerX - 11f * scale, centerY - 7f * scale)
            cubicTo(centerX - 11f * scale, centerY - 16f * scale, centerX - 6f * scale, centerY - 20f * scale, centerX, centerY - 20f * scale)
            cubicTo(centerX + 6f * scale, centerY - 20f * scale, centerX + 11f * scale, centerY - 16f * scale, centerX + 11f * scale, centerY - 7f * scale)
            cubicTo(centerX + 11f * scale, centerY + 1f * scale, centerX + 4f * scale, centerY + 10f * scale, centerX, centerY + 17f * scale)
            close()
        }
        canvas.drawPath(path, strokePaint)
        canvas.drawCircle(centerX, centerY - context.mo2Dp(7), context.mo2Dp(4).toFloat(), strokePaint)
    }

    private fun drawClock(canvas: Canvas, centerX: Float, centerY: Float, color: Int) {
        strokePaint.color = color
        strokePaint.strokeWidth = context.mo2Dp(3).toFloat()
        val radius = context.mo2Dp(16).toFloat()
        canvas.drawCircle(centerX, centerY, radius, strokePaint)
        canvas.drawLine(centerX, centerY - context.mo2Dp(10), centerX, centerY + context.mo2Dp(1), strokePaint)
        canvas.drawLine(centerX, centerY + context.mo2Dp(1), centerX + context.mo2Dp(9), centerY + context.mo2Dp(7), strokePaint)
    }

    private fun drawCenteredText(
        canvas: Canvas,
        text: String,
        centerX: Float,
        centerY: Float,
        sizeSp: Float,
        color: Int,
        bold: Boolean,
        maxWidth: Float,
    ) {
        textPaint.color = color
        textPaint.typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        textPaint.textSize = sp(sizeSp)
        while (textPaint.measureText(text) > maxWidth && textPaint.textSize > sp(12f)) {
            textPaint.textSize -= sp(0.5f)
        }
        val metrics = textPaint.fontMetrics
        canvas.drawText(text, centerX, centerY - (metrics.ascent + metrics.descent) / 2f, textPaint)
    }

    private fun sp(value: Float): Float = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP,
        value,
        resources.displayMetrics,
    )

    private enum class MetricIcon { Sets, Volume, Distance, Time }
}
