package br.com.mo2log.mobile.ui

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.animation.DecelerateInterpolator
import br.com.mo2log.mobile.R
import kotlin.math.abs
import kotlin.math.max

data class Mo2WeeklyWorkoutActivity(
    val title: String,
    val imageRes: Int,
    val destination: String,
    val planIndex: Int? = null,
    val workoutId: String? = null,
    val completed: Boolean = false,
)

data class Mo2WeeklyWorkoutSlide(
    val day: String,
    val dayIndex: Int,
    val activities: List<Mo2WeeklyWorkoutActivity>,
)

object Mo2WeeklyCarouselState {
    const val ACTIVITY_SWITCH_MILLIS = 5000L

    fun normalized(index: Int, size: Int): Int {
        if (size <= 0) return 0
        return ((index % size) + size) % size
    }

    fun next(index: Int, size: Int): Int = normalized(index + 1, size)

    fun previous(index: Int, size: Int): Int = normalized(index - 1, size)

    fun fromIsoDay(day: Int, size: Int = 7): Int = normalized(day.coerceIn(1, 7) - 1, size)

    fun initialIndexForDay(day: Int, scheduledDays: List<Int>): Int {
        if (scheduledDays.isEmpty()) return 0
        val safeDay = day.coerceIn(1, 7)
        val exact = scheduledDays.indexOf(safeDay)
        if (exact >= 0) return exact
        val next = scheduledDays.indexOfFirst { scheduledDay -> scheduledDay > safeDay }
        return if (next >= 0) next else scheduledDays.lastIndex
    }

    fun nextActivity(index: Int, size: Int): Int = normalized(index + 1, size)
}

@SuppressLint("ViewConstructor")
class Mo2WeeklyWorkoutCarouselView(
    context: Context,
    private val slides: List<Mo2WeeklyWorkoutSlide>,
    initialIndex: Int,
    private val reduceMotion: Boolean,
    private val onSlideChanged: (Int) -> Unit,
    private val onSlideClick: (Mo2WeeklyWorkoutSlide, Mo2WeeklyWorkoutActivity) -> Unit,
) : View(context) {
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }
    private val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val regularTypeface = resources.getFont(R.font.be_vietnam_pro_regular)
    private val strongTypeface = Typeface.create(regularTypeface, Typeface.BOLD)
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val bitmaps: Map<Int, Bitmap?> = slides
        .flatMap { slide -> slide.activities }
        .map { activity -> activity.imageRes }
        .distinct()
        .associateWith { imageRes -> BitmapFactory.decodeResource(resources, imageRes) }
    private val autoSwitchHandler = Handler(Looper.getMainLooper())

    private var currentIndex = Mo2WeeklyCarouselState.normalized(initialIndex, slides.size)
    private var previousIndex = currentIndex
    private var currentActivityIndex = 0
    private var previousActivityIndex = 0
    private var transitionProgress = 1f
    private var animator: ValueAnimator? = null
    private var downX = 0f
    private var downY = 0f
    private val autoSwitchRunnable = object : Runnable {
        override fun run() {
            val activities = slides.getOrNull(currentIndex)?.activities.orEmpty()
            if (activities.size > 1) {
                changeActivity(Mo2WeeklyCarouselState.nextActivity(currentActivityIndex, activities.size))
            }
        }
    }

    init {
        isClickable = true
        isFocusable = true
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        updateContentDescription()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val measuredWidth = resolveSize(suggestedMinimumWidth, widthMeasureSpec)
        val desiredHeight = (measuredWidth * 0.90f).toInt()
        setMeasuredDimension(measuredWidth, resolveSize(desiredHeight, heightMeasureSpec))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (slides.isEmpty() || width <= 0 || height <= 0) return

        val borderInset = context.mo2Dp(2).toFloat()
        val card = RectF(borderInset, borderInset, width - borderInset, height - borderInset)
        val radius = context.mo2Dp(Mo2Radius.Md).toFloat()
        val clipPath = Path().apply { addRoundRect(card, radius, radius, Path.Direction.CW) }

        canvas.save()
        canvas.clipPath(clipPath)
        if (transitionProgress < 1f &&
            (previousIndex != currentIndex || previousActivityIndex != currentActivityIndex)) {
            drawSlideLayer(canvas, card, previousIndex, previousActivityIndex, ((1f - transitionProgress) * 255f).toInt())
            drawSlideLayer(canvas, card, currentIndex, currentActivityIndex, (transitionProgress * 255f).toInt())
        } else {
            drawSlide(canvas, card, currentIndex, currentActivityIndex)
        }
        canvas.restore()

        val completed = currentActivity()?.completed == true
        strokePaint.color = if (completed) Mo2Colors.Primary else Mo2Colors.Border
        strokePaint.strokeWidth = context.mo2Dp(if (completed) 3 else 1).toFloat()
        canvas.drawRoundRect(card, radius, radius, strokePaint)
    }

    private fun drawSlideLayer(canvas: Canvas, card: RectF, index: Int, activityIndex: Int, alpha: Int) {
        if (alpha <= 0) return
        val checkpoint = canvas.saveLayerAlpha(card, alpha.coerceIn(0, 255))
        drawSlide(canvas, card, index, activityIndex)
        canvas.restoreToCount(checkpoint)
    }

    private fun drawSlide(canvas: Canvas, card: RectF, index: Int, activityIndex: Int) {
        val slide = slides[index]
        val activity = slide.activities[Mo2WeeklyCarouselState.normalized(activityIndex, slide.activities.size)]
        val bitmap = bitmaps[activity.imageRes]
        if (bitmap != null) {
            canvas.drawBitmap(bitmap, sourceRect(bitmap, card), card, imagePaint)
        } else {
            fillPaint.color = Mo2Colors.SurfaceElevated
            canvas.drawRect(card, fillPaint)
        }

        val bandTop = card.bottom - card.height() * 0.31f
        fillPaint.color = Color.rgb(12, 24, 40)
        canvas.drawRect(card.left, bandTop, card.right, card.bottom, fillPaint)

        val horizontalPadding = context.mo2Dp(Mo2Spacing.Lg).toFloat()
        val dayBaseline = bandTop + context.mo2Dp(28)
        drawText(
            canvas = canvas,
            text = slide.day.uppercase(),
            x = card.left + horizontalPadding,
            baseline = dayBaseline,
            sizeSp = 13f,
            color = Mo2Colors.Primary,
            typeface = strongTypeface,
            align = Paint.Align.LEFT,
        )

        drawText(
            canvas = canvas,
            text = buildString {
                append(index + 1).append('/').append(slides.size)
                if (slide.activities.size > 1) {
                    append(" | ").append(Mo2WeeklyCarouselState.normalized(activityIndex, slide.activities.size) + 1)
                        .append('/').append(slide.activities.size)
                }
            },
            x = card.right - horizontalPadding,
            baseline = dayBaseline,
            sizeSp = 13f,
            color = Mo2Colors.TextSecondary,
            typeface = regularTypeface,
            align = Paint.Align.RIGHT,
        )

        drawFittedText(
            canvas = canvas,
            text = activity.title,
            x = card.left + horizontalPadding,
            baseline = bandTop + context.mo2Dp(65),
            maxWidth = card.width() - horizontalPadding * 2f - context.mo2Dp(34),
        )
        drawChevron(canvas, card.right - horizontalPadding, bandTop + context.mo2Dp(59))
        drawDots(canvas, card, index)
    }

    private fun sourceRect(bitmap: Bitmap, target: RectF): android.graphics.Rect {
        val targetRatio = target.width() / target.height()
        val bitmapRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
        return if (bitmapRatio > targetRatio) {
            val sourceWidth = (bitmap.height * targetRatio).toInt()
            val left = (bitmap.width - sourceWidth) / 2
            android.graphics.Rect(left, 0, left + sourceWidth, bitmap.height)
        } else {
            val sourceHeight = (bitmap.width / targetRatio).toInt()
            val top = (bitmap.height - sourceHeight) / 2
            android.graphics.Rect(0, top, bitmap.width, top + sourceHeight)
        }
    }

    private fun drawFittedText(canvas: Canvas, text: String, x: Float, baseline: Float, maxWidth: Float) {
        textPaint.typeface = regularTypeface
        textPaint.textSize = sp(24f)
        textPaint.letterSpacing = 0f
        while (textPaint.measureText(text) > maxWidth && textPaint.textSize > sp(17f)) {
            textPaint.textSize -= sp(0.5f)
        }
        val fittedText = if (textPaint.measureText(text) <= maxWidth) {
            text
        } else {
            val suffix = "..."
            val availableWidth = (maxWidth - textPaint.measureText(suffix)).coerceAtLeast(0f)
            val count = textPaint.breakText(text, true, availableWidth, null)
            text.take(count).trimEnd() + suffix
        }
        textPaint.color = Mo2Colors.TextPrimary
        textPaint.textAlign = Paint.Align.LEFT
        canvas.drawText(fittedText, x, baseline, textPaint)
    }

    private fun drawText(
        canvas: Canvas,
        text: String,
        x: Float,
        baseline: Float,
        sizeSp: Float,
        color: Int,
        typeface: Typeface,
        align: Paint.Align,
    ) {
        textPaint.color = color
        textPaint.textSize = sp(sizeSp)
        textPaint.typeface = typeface
        textPaint.textAlign = align
        textPaint.letterSpacing = 0f
        canvas.drawText(text, x, baseline, textPaint)
    }

    private fun drawChevron(canvas: Canvas, centerX: Float, centerY: Float) {
        strokePaint.color = Mo2Colors.TextPrimary
        strokePaint.strokeWidth = context.mo2Dp(2).toFloat()
        val size = context.mo2Dp(8).toFloat()
        canvas.drawLine(centerX - size / 2f, centerY - size, centerX + size / 2f, centerY, strokePaint)
        canvas.drawLine(centerX + size / 2f, centerY, centerX - size / 2f, centerY + size, strokePaint)
    }

    private fun drawDots(canvas: Canvas, card: RectF, selectedIndex: Int) {
        val radius = context.mo2Dp(3).toFloat()
        val gap = context.mo2Dp(12).toFloat()
        val totalWidth = gap * (slides.size - 1)
        val startX = card.centerX() - totalWidth / 2f
        val centerY = card.bottom - context.mo2Dp(18)
        slides.indices.forEach { index ->
            fillPaint.color = if (index == selectedIndex) Mo2Colors.Primary else Mo2Colors.TextMuted
            canvas.drawCircle(startX + index * gap, centerY, if (index == selectedIndex) radius + context.mo2Dp(1) else radius, fillPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (slides.isEmpty()) return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                parent?.requestDisallowInterceptTouchEvent(false)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val horizontal = abs(event.x - downX)
                val vertical = abs(event.y - downY)
                if (horizontal > touchSlop && horizontal > vertical) {
                    parent?.requestDisallowInterceptTouchEvent(true)
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                val deltaX = event.x - downX
                val deltaY = event.y - downY
                val swipeThreshold = max(touchSlop * 3f, width * 0.12f)
                if (abs(deltaX) >= swipeThreshold && abs(deltaX) > abs(deltaY)) {
                    if (deltaX < 0f) showNext() else showPrevious()
                } else if (abs(deltaX) < touchSlop * 2f && abs(deltaY) < touchSlop * 2f) {
                    val arrowArea = context.mo2Dp(66)
                    if (event.x >= width - arrowArea) showNext() else performClick()
                }
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        super.performClick()
        slides.getOrNull(currentIndex)?.let { slide ->
            currentActivity()?.let { activity -> onSlideClick(slide, activity) }
        }
        return true
    }

    private fun showNext() = changeSlide(Mo2WeeklyCarouselState.next(currentIndex, slides.size))

    private fun showPrevious() = changeSlide(Mo2WeeklyCarouselState.previous(currentIndex, slides.size))

    private fun changeSlide(targetIndex: Int) {
        val target = Mo2WeeklyCarouselState.normalized(targetIndex, slides.size)
        if (target == currentIndex) return
        animator?.cancel()
        previousIndex = currentIndex
        previousActivityIndex = currentActivityIndex
        currentIndex = target
        currentActivityIndex = 0
        onSlideChanged(currentIndex)
        updateContentDescription()
        scheduleAutoSwitch()
        if (reduceMotion) {
            transitionProgress = 1f
            invalidate()
            announceForAccessibility(contentDescription)
            return
        }

        transitionProgress = 0f
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 180L
            interpolator = DecelerateInterpolator()
            addUpdateListener { animation ->
                transitionProgress = animation.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun changeActivity(targetIndex: Int) {
        val activities = slides.getOrNull(currentIndex)?.activities.orEmpty()
        if (activities.size <= 1) return
        val target = Mo2WeeklyCarouselState.normalized(targetIndex, activities.size)
        if (target == currentActivityIndex) return
        animator?.cancel()
        previousIndex = currentIndex
        previousActivityIndex = currentActivityIndex
        currentActivityIndex = target
        updateContentDescription()
        scheduleAutoSwitch()
        if (reduceMotion) {
            transitionProgress = 1f
            invalidate()
            return
        }
        transitionProgress = 0f
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 220L
            interpolator = DecelerateInterpolator()
            addUpdateListener { animation ->
                transitionProgress = animation.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun currentActivity(): Mo2WeeklyWorkoutActivity? {
        val activities = slides.getOrNull(currentIndex)?.activities.orEmpty()
        if (activities.isEmpty()) return null
        return activities[Mo2WeeklyCarouselState.normalized(currentActivityIndex, activities.size)]
    }

    private fun scheduleAutoSwitch() {
        autoSwitchHandler.removeCallbacks(autoSwitchRunnable)
        if (isAttachedToWindow && slides.getOrNull(currentIndex)?.activities.orEmpty().size > 1) {
            autoSwitchHandler.postDelayed(autoSwitchRunnable, Mo2WeeklyCarouselState.ACTIVITY_SWITCH_MILLIS)
        }
    }

    private fun updateContentDescription() {
        contentDescription = slides.getOrNull(currentIndex)?.let { slide ->
            val activity = currentActivity()
            slide.day + ", " + activity?.title.orEmpty() +
                (if (activity?.completed == true) ", concluido" else ", pendente") +
                ". Dia " + (currentIndex + 1) + " de " + slides.size + ". Deslize para trocar."
        }.orEmpty()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        scheduleAutoSwitch()
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        autoSwitchHandler.removeCallbacks(autoSwitchRunnable)
        super.onDetachedFromWindow()
    }

    private fun sp(value: Float): Float = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP,
        value,
        resources.displayMetrics,
    )
}
