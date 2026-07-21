package br.com.mo2log.mobile.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.min

enum class Mo2WeeklyAgendaStatus {
    Pending,
    Partial,
    Completed,
    Recovery,
}

data class Mo2WeeklyAgendaDay(
    val dayIndex: Int,
    val dayName: String,
    val shortName: String,
    val dateLabel: String,
    val title: String,
    val description: String,
    val meta: String,
    val hasStrength: Boolean,
    val strengthCompleted: Boolean,
    val hasRunning: Boolean,
    val runningCompleted: Boolean,
    val status: Mo2WeeklyAgendaStatus,
    val isToday: Boolean,
)

object Mo2WeeklyAgendaState {
    fun normalized(index: Int, size: Int): Int {
        if (size <= 0) return 0
        return ((index % size) + size) % size
    }

    fun status(
        hasStrength: Boolean,
        strengthCompleted: Boolean,
        hasRunning: Boolean,
        runningCompleted: Boolean,
    ): Mo2WeeklyAgendaStatus {
        val planned = listOf(hasStrength, hasRunning).count { it }
        if (planned == 0) return Mo2WeeklyAgendaStatus.Recovery
        val completed = listOf(
            hasStrength && strengthCompleted,
            hasRunning && runningCompleted,
        ).count { it }
        return when (completed) {
            0 -> Mo2WeeklyAgendaStatus.Pending
            planned -> Mo2WeeklyAgendaStatus.Completed
            else -> Mo2WeeklyAgendaStatus.Partial
        }
    }

    fun progressPercent(completed: Int, total: Int): Int {
        if (total <= 0) return 0
        return ((completed.toDouble() / total.toDouble()) * 100.0).toInt().coerceIn(0, 100)
    }
}

@SuppressLint("ViewConstructor")
class Mo2WeeklyAgendaView(
    context: Context,
    private val days: List<Mo2WeeklyAgendaDay>,
    initialIndex: Int,
    private val weekLabel: String,
    private val completedActivities: Int,
    private val totalActivities: Int,
    private val reduceMotion: Boolean,
    private val onDayChanged: (Int) -> Unit,
    private val onOpenStrength: (Mo2WeeklyAgendaDay) -> Unit,
    private val onOpenRunning: (Mo2WeeklyAgendaDay) -> Unit,
    private val onOpenRecovery: (Mo2WeeklyAgendaDay) -> Unit,
    private val onOpenFullPlan: () -> Unit,
) : LinearLayout(context) {
    private var selectedIndex = Mo2WeeklyAgendaState.normalized(initialIndex, days.size)
    private val dayButtons = mutableListOf<LinearLayout>()
    private val detail = LinearLayout(context)

    init {
        require(days.isNotEmpty()) { "A agenda semanal precisa de pelo menos um dia." }
        orientation = VERTICAL
        setPadding(
            context.mo2Dp(Mo2Spacing.Lg),
            context.mo2Dp(Mo2Spacing.Lg),
            context.mo2Dp(Mo2Spacing.Lg),
            context.mo2Dp(Mo2Spacing.Lg),
        )
        background = Mo2Drawables.rounded(context, Mo2Colors.Surface, Mo2Radius.Lg, Mo2Colors.Border)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            setMargins(0, context.mo2Dp(Mo2Spacing.Sm), 0, context.mo2Dp(Mo2Spacing.Sm))
        }

        addView(buildHeader())
        addView(buildProgressBar())
        addView(buildDaySelector())

        val divider = View(context)
        divider.setBackgroundColor(Mo2Colors.Border)
        addView(divider, LayoutParams(LayoutParams.MATCH_PARENT, context.mo2Dp(1)).apply {
            setMargins(0, context.mo2Dp(Mo2Spacing.Lg), 0, 0)
        })

        detail.orientation = VERTICAL
        addView(detail, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        updateSelection(animate = false)
    }

    private fun buildHeader(): View {
        val row = LinearLayout(context)
        row.orientation = HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL

        val titles = LinearLayout(context)
        titles.orientation = VERTICAL
        titles.addView(Mo2Components.label(context, "Agenda da semana", Mo2Colors.TextPrimary, 22f, true))
        titles.addView(Mo2Components.label(context, weekLabel, Mo2Colors.TextSecondary, Mo2Type.Label, false))
        row.addView(titles, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))

        val progress = Mo2Components.label(
            context,
            "$completedActivities / $totalActivities",
            Mo2Colors.Background,
            Mo2Type.BodySmall,
            true,
        )
        progress.gravity = Gravity.CENTER
        progress.includeFontPadding = false
        progress.minWidth = context.mo2Dp(58)
        progress.minHeight = context.mo2Dp(32)
        progress.setPadding(context.mo2Dp(Mo2Spacing.Md), 0, context.mo2Dp(Mo2Spacing.Md), 0)
        progress.background = Mo2Drawables.rounded(
            context,
            Mo2Colors.Primary,
            Mo2Radius.Pill,
            Mo2Colors.Primary,
        )
        progress.contentDescription = "$completedActivities de $totalActivities atividades concluidas"
        row.addView(progress)
        return row
    }

    private fun buildProgressBar(): View {
        val progress = Mo2Components.progressBar(
            context,
            Mo2WeeklyAgendaState.progressPercent(completedActivities, totalActivities),
            Mo2Colors.Primary,
        )
        progress.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, context.mo2Dp(5)).apply {
            setMargins(0, context.mo2Dp(Mo2Spacing.Md), 0, 0)
        }
        return progress
    }

    private fun buildDaySelector(): View {
        val row = LinearLayout(context)
        row.orientation = HORIZONTAL
        row.gravity = Gravity.CENTER
        row.setPadding(0, context.mo2Dp(Mo2Spacing.Lg), 0, 0)
        days.forEachIndexed { index, day ->
            val button = buildDayButton(day, index)
            dayButtons.add(button)
            row.addView(button, LayoutParams(0, context.mo2Dp(72), 1f).apply {
                val gap = context.mo2Dp(2)
                setMargins(if (index == 0) 0 else gap, 0, if (index == days.lastIndex) 0 else gap, 0)
            })
        }
        return row
    }

    private fun buildDayButton(day: Mo2WeeklyAgendaDay, index: Int): LinearLayout {
        val button = LinearLayout(context)
        button.orientation = VERTICAL
        button.gravity = Gravity.CENTER
        button.isClickable = true
        button.isFocusable = true
        button.minimumHeight = context.mo2Dp(72)

        val name = Mo2Components.label(context, day.shortName, Mo2Colors.TextSecondary, 10f, true)
        name.gravity = Gravity.CENTER
        name.includeFontPadding = false
        button.addView(name, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

        val date = Mo2Components.label(context, day.dateLabel, Mo2Colors.TextPrimary, 17f, true)
        date.gravity = Gravity.CENTER
        date.includeFontPadding = false
        button.addView(date, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            setMargins(0, context.mo2Dp(3), 0, context.mo2Dp(5))
        })

        button.addView(
            Mo2AgendaStatusDotsView(
                context,
                day.hasStrength,
                day.strengthCompleted,
                day.hasRunning,
                day.runningCompleted,
            ),
            LayoutParams(context.mo2Dp(28), context.mo2Dp(8)),
        )
        button.setOnClickListener {
            if (selectedIndex == index) return@setOnClickListener
            selectedIndex = index
            performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            onDayChanged(index)
            updateSelection(animate = true)
        }
        return button
    }

    private fun updateSelection(animate: Boolean) {
        dayButtons.forEachIndexed { index, button ->
            val selected = index == selectedIndex
            val day = days[index]
            val accent = accentFor(day)
            button.isSelected = selected
            button.background = Mo2Drawables.rounded(
                context,
                if (selected) Mo2Colors.SurfaceElevated else Color.TRANSPARENT,
                Mo2Radius.Md,
                if (selected) accent else Color.TRANSPARENT,
            )
            button.contentDescription = buildString {
                append(day.dayName).append(", dia ").append(day.dateLabel).append(", ").append(day.title)
                append(", ").append(statusLabel(day.status))
                if (selected) append(", selecionado")
            }
            val name = button.getChildAt(0) as TextView
            val date = button.getChildAt(1) as TextView
            name.setTextColor(if (selected) accent else Mo2Colors.TextSecondary)
            date.setTextColor(if (selected || day.isToday) Mo2Colors.TextPrimary else Mo2Colors.TextSecondary)
        }
        renderDetail(days[selectedIndex], animate)
    }

    private fun renderDetail(day: Mo2WeeklyAgendaDay, animate: Boolean) {
        detail.removeAllViews()
        detail.alpha = if (animate && !reduceMotion) 0f else 1f
        detail.translationY = if (animate && !reduceMotion) context.mo2Dp(Mo2Spacing.Sm).toFloat() else 0f

        val top = LinearLayout(context)
        top.orientation = HORIZONTAL
        top.gravity = Gravity.CENTER_VERTICAL
        top.setPadding(0, context.mo2Dp(Mo2Spacing.Lg), 0, 0)

        val icon = Mo2AgendaActivityIconView(context, day.hasStrength, day.hasRunning)
        top.addView(icon, LayoutParams(context.mo2Dp(52), context.mo2Dp(52)))

        val text = LinearLayout(context)
        text.orientation = VERTICAL
        text.setPadding(context.mo2Dp(Mo2Spacing.Md), 0, context.mo2Dp(Mo2Spacing.Sm), 0)
        text.addView(Mo2Components.label(context, day.dayName.uppercase(), accentFor(day), Mo2Type.Label, true))
        text.addView(Mo2Components.label(context, day.title, Mo2Colors.TextPrimary, 20f, true))
        top.addView(text, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        top.addView(statusBadge(day.status))
        detail.addView(top)

        detail.addView(Mo2Components.label(context, day.description, Mo2Colors.TextSecondary, Mo2Type.BodySmall, false).apply {
            setPadding(0, context.mo2Dp(Mo2Spacing.Md), 0, 0)
        })
        detail.addView(Mo2Components.label(context, day.meta, Mo2Colors.TextPrimary, Mo2Type.BodySmall, true).apply {
            setPadding(0, context.mo2Dp(Mo2Spacing.Sm), 0, 0)
        })
        detail.addView(buildActionRow(day))

        val fullPlan = Mo2Components.actionButton(context, "Planejamento completo", Mo2Colors.SurfaceAlt, Mo2Colors.Primary)
        fullPlan.setOnClickListener { onOpenFullPlan() }
        detail.addView(fullPlan, LayoutParams(LayoutParams.MATCH_PARENT, context.mo2Dp(48)).apply {
            setMargins(0, context.mo2Dp(Mo2Spacing.Sm), 0, 0)
        })

        if (animate && !reduceMotion) {
            detail.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(180L)
                .start()
        }
    }

    private fun buildActionRow(day: Mo2WeeklyAgendaDay): View {
        val row = LinearLayout(context)
        row.orientation = HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        row.setPadding(0, context.mo2Dp(Mo2Spacing.Lg), 0, 0)

        if (day.hasStrength) {
            val strength = Mo2Components.actionButton(context, "Abrir treino", Mo2Colors.Primary, Mo2Colors.Background)
            strength.setOnClickListener { onOpenStrength(day) }
            row.addView(strength, LayoutParams(0, context.mo2Dp(48), 1f))
        }
        if (day.hasRunning) {
            val running = Mo2Components.actionButton(
                context,
                "Abrir corrida",
                if (day.hasStrength) Mo2Colors.SurfaceAlt else Mo2Colors.Running,
                if (day.hasStrength) Mo2Colors.Running else Mo2Colors.Background,
            )
            running.setOnClickListener { onOpenRunning(day) }
            row.addView(running, LayoutParams(0, context.mo2Dp(48), 1f).apply {
                if (day.hasStrength) setMargins(context.mo2Dp(Mo2Spacing.Sm), 0, 0, 0)
            })
        }
        if (!day.hasStrength && !day.hasRunning) {
            val recovery = Mo2Components.actionButton(context, "Abrir coach", Mo2Colors.SurfaceAlt, Mo2Colors.Primary)
            recovery.setOnClickListener { onOpenRecovery(day) }
            row.addView(recovery, LayoutParams(LayoutParams.MATCH_PARENT, context.mo2Dp(48)))
        }
        return row
    }

    private fun statusBadge(status: Mo2WeeklyAgendaStatus): TextView {
        val (text, foreground, background) = when (status) {
            Mo2WeeklyAgendaStatus.Completed -> Triple("Feito", Mo2Colors.Background, Mo2Colors.Primary)
            Mo2WeeklyAgendaStatus.Partial -> Triple("Parcial", Mo2Colors.Background, Mo2Colors.Warning)
            Mo2WeeklyAgendaStatus.Pending -> Triple("Pendente", Mo2Colors.TextSecondary, Mo2Colors.SurfaceAlt)
            Mo2WeeklyAgendaStatus.Recovery -> Triple("Recuperar", Mo2Colors.Running, Mo2Colors.SurfaceAlt)
        }
        return Mo2Components.label(context, text.uppercase(), foreground, 10f, true).apply {
            gravity = Gravity.CENTER
            includeFontPadding = false
            minHeight = context.mo2Dp(28)
            setPadding(context.mo2Dp(Mo2Spacing.Sm), 0, context.mo2Dp(Mo2Spacing.Sm), 0)
            this.background = Mo2Drawables.rounded(context, background, Mo2Radius.Pill, background)
        }
    }

    private fun accentFor(day: Mo2WeeklyAgendaDay): Int = when {
        day.hasStrength -> Mo2Colors.Primary
        day.hasRunning -> Mo2Colors.Running
        else -> Color.rgb(45, 212, 191)
    }

    private fun statusLabel(status: Mo2WeeklyAgendaStatus): String = when (status) {
        Mo2WeeklyAgendaStatus.Completed -> "concluido"
        Mo2WeeklyAgendaStatus.Partial -> "parcialmente concluido"
        Mo2WeeklyAgendaStatus.Pending -> "pendente"
        Mo2WeeklyAgendaStatus.Recovery -> "recuperacao"
    }
}

@SuppressLint("ViewConstructor")
private class Mo2AgendaStatusDotsView(
    context: Context,
    private val hasStrength: Boolean,
    private val strengthCompleted: Boolean,
    private val hasRunning: Boolean,
    private val runningCompleted: Boolean,
) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val activities = mutableListOf<Pair<Int, Boolean>>()
        if (hasStrength) activities.add(Mo2Colors.Primary to strengthCompleted)
        if (hasRunning) activities.add(Mo2Colors.Running to runningCompleted)
        if (activities.isEmpty()) activities.add(Color.rgb(45, 212, 191) to false)
        val gap = context.mo2Dp(5).toFloat()
        val radius = context.mo2Dp(3).toFloat()
        val totalWidth = activities.size * radius * 2f + (activities.size - 1) * gap
        var x = (width - totalWidth) / 2f + radius
        activities.forEach { (color, completed) ->
            paint.color = color
            paint.style = if (completed) Paint.Style.FILL else Paint.Style.STROKE
            paint.strokeWidth = context.mo2Dp(1).toFloat()
            canvas.drawCircle(x, height / 2f, radius, paint)
            x += radius * 2f + gap
        }
    }
}

@SuppressLint("ViewConstructor")
private class Mo2AgendaActivityIconView(
    context: Context,
    private val hasStrength: Boolean,
    private val hasRunning: Boolean,
) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    init {
        background = Mo2Drawables.rounded(context, Mo2Colors.SurfaceAlt, Mo2Radius.Md, Mo2Colors.Border)
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        when {
            hasStrength && hasRunning -> {
                drawStrength(canvas, 4f, 16f, 22f, Mo2Colors.Primary)
                drawRunning(canvas, 28f, 16f, 20f, Mo2Colors.Running)
            }
            hasStrength -> drawStrength(canvas, 12f, 12f, 28f, Mo2Colors.Primary)
            hasRunning -> drawRunning(canvas, 12f, 11f, 29f, Mo2Colors.Running)
            else -> drawRecovery(canvas)
        }
    }

    private fun drawStrength(canvas: Canvas, leftDp: Float, topDp: Float, sizeDp: Float, color: Int) {
        val left = context.mo2Dp(leftDp.toInt()).toFloat()
        val top = context.mo2Dp(topDp.toInt()).toFloat()
        val size = context.mo2Dp(sizeDp.toInt()).toFloat()
        val scale = size / 24f
        canvas.save()
        canvas.translate(left, top)
        canvas.scale(scale, scale)
        paint.color = color
        paint.strokeWidth = 2.2f
        canvas.drawLine(5f, 12f, 19f, 12f, paint)
        canvas.drawRoundRect(2.5f, 8f, 5f, 16f, 1f, 1f, paint)
        canvas.drawRoundRect(5f, 6.5f, 7.5f, 17.5f, 1f, 1f, paint)
        canvas.drawRoundRect(16.5f, 6.5f, 19f, 17.5f, 1f, 1f, paint)
        canvas.drawRoundRect(19f, 8f, 21.5f, 16f, 1f, 1f, paint)
        canvas.restore()
    }

    private fun drawRunning(canvas: Canvas, leftDp: Float, topDp: Float, sizeDp: Float, color: Int) {
        val left = context.mo2Dp(leftDp.toInt()).toFloat()
        val top = context.mo2Dp(topDp.toInt()).toFloat()
        val size = context.mo2Dp(sizeDp.toInt()).toFloat()
        val scale = size / 24f
        canvas.save()
        canvas.translate(left, top)
        canvas.scale(scale, scale)
        paint.color = color
        paint.strokeWidth = 2.2f
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
        canvas.restore()
    }

    private fun drawRecovery(canvas: Canvas) {
        val size = min(width, height).toFloat()
        val scale = size / 52f
        canvas.save()
        canvas.scale(scale, scale)
        paint.color = Color.rgb(45, 212, 191)
        paint.strokeWidth = 2.4f
        val leaf = Path().apply {
            moveTo(13f, 31f)
            cubicTo(15f, 16f, 28f, 11f, 39f, 13f)
            cubicTo(40f, 25f, 32f, 38f, 20f, 39f)
            cubicTo(16f, 38f, 14f, 35f, 13f, 31f)
            moveTo(15f, 36f)
            cubicTo(22f, 29f, 28f, 24f, 36f, 19f)
        }
        canvas.drawPath(leaf, paint)
        canvas.restore()
    }
}
