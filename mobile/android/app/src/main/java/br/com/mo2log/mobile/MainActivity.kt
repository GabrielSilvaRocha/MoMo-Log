package br.com.mo2log.mobile

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.GradientDrawable
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.TypedValue
import android.view.DragEvent
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowInsets
import android.view.WindowManager
import android.view.animation.AnimationSet
import android.view.animation.AlphaAnimation
import android.view.animation.TranslateAnimation
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import br.com.mo2log.mobile.ui.Mo2Colors
import br.com.mo2log.mobile.ui.Mo2Components
import br.com.mo2log.mobile.ui.Mo2DragHandleView
import br.com.mo2log.mobile.ui.Mo2Drawables
import br.com.mo2log.mobile.ui.Mo2HistoryChartView
import br.com.mo2log.mobile.ui.Mo2HistoryPoint
import br.com.mo2log.mobile.ui.Mo2NavIcon
import br.com.mo2log.mobile.ui.Mo2Radius
import br.com.mo2log.mobile.ui.Mo2Spacing
import br.com.mo2log.mobile.ui.Mo2WeeklyDashboardData
import br.com.mo2log.mobile.ui.Mo2WeeklyDashboardView
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.text.Normalizer
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.net.URL
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt

data class ExercisePlan(
    val name: String,
    val target: String,
    val rest: String,
    val notes: String,
)

data class WorkoutPlan(
    val id: String,
    val title: String,
    val focus: String,
    val exercises: List<ExercisePlan>,
)

data class RunningStage(
    val title: String,
    val distanceKm: Double,
    val speedKmh: Double,
    val note: String,
)

data class RunningWorkout(
    val id: String,
    val week: Int,
    val dayName: String,
    val dayIndex: Int,
    val title: String,
    val focus: String,
    val description: String,
    val stages: List<RunningStage>,
)

data class NavItem(
    val id: String,
    val title: String,
)

data class CatalogExercise(
    val id: String,
    val name: String,
    val slug: String,
    val muscle: String,
    val subgroup: String,
    val movement: String,
    val type: String,
    val level: String,
    val primary: String,
    val secondary: String,
    val equipment: String,
    val alternatives: String,
    val description: String,
    val technicalCare: String,
    val source: String,
    val sourceId: String,
    val status: String,
    val review: String,
    val links: List<String>,
)

data class StrengthAdjustment(
    val nextLoad: Double,
    val suggestedSetCount: Int,
    val loadReason: String,
    val volumeReason: String,
)

data class RunningAdjustment(
    val speedOffset: Double,
    val distanceScale: Double,
    val headline: String,
    val reason: String,
)

data class RunningForecast(
    val predictedSeconds: Long,
    val goalSeconds: Long,
    val sampleCount: Int,
    val confidence: String,
    val source: String,
    val trendSeconds: Long?,
    val usesMeasuredRuns: Boolean,
)

data class HistoryActivity(
    val type: String,
    val item: JSONObject,
    val strengthSets: List<JSONObject> = emptyList(),
    val completion: JSONObject? = null,
)

data class HistoryCalendarSummary(
    var strengthWorkouts: Int = 0,
    var strengthSets: Int = 0,
    var runs: Int = 0,
    var runningDistance: Double = 0.0,
)

class RemoteExerciseMediaView(context: Context, private val links: List<String>) : LinearLayout(context) {
    private val handler = Handler(Looper.getMainLooper())
    private val image = ImageView(context)
    private val status = TextView(context)
    private val frames = mutableListOf<Bitmap>()
    private var frameIndex = 0

    private val frameLoop = object : Runnable {
        override fun run() {
            if (frames.isNotEmpty()) {
                image.setImageBitmap(frames[frameIndex % frames.size])
                frameIndex += 1
            }
            handler.postDelayed(this, 700L)
        }
    }

    init {
        orientation = VERTICAL
        setPadding(0, 10, 0, 10)
        setBackgroundColor(Mo2Colors.Surface)

        image.scaleType = ImageView.ScaleType.FIT_CENTER
        image.adjustViewBounds = false
        addView(image, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))

        status.text = "Carregando frames de execucao..."
        status.setTextColor(Mo2Colors.TextSecondary)
        status.textSize = 12f
        status.gravity = Gravity.CENTER
        addView(status, LayoutParams(LayoutParams.MATCH_PARENT, 34))

        if (links.isEmpty()) {
            status.text = "Midia indisponivel para este exercicio"
        } else {
            loadFrames()
        }
    }

    private fun loadFrames() {
        Thread {
            val loaded = links.mapNotNull { link -> loadFrameWithCache(link) }
            val cachedCount = loaded.count { it.second }

            post {
                frames.clear()
                frames.addAll(loaded.map { it.first })
                if (frames.isNotEmpty()) {
                    image.setImageBitmap(frames.first())
                    status.text = when {
                        cachedCount == frames.size -> "Fonte: cache local"
                        cachedCount > 0 -> "Fonte: Free Exercise DB + cache"
                        else -> "Fonte: Free Exercise DB"
                    }
                } else {
                    status.text = "Nao foi possivel carregar a midia agora"
                }
            }
        }.start()
    }

    private fun loadFrameWithCache(link: String): Pair<Bitmap, Boolean>? {
        return try {
            val dir = File(context.cacheDir, "exercise_media")
            dir.mkdirs()
            val file = File(dir, Integer.toHexString(link.hashCode()) + ".img")
            if (file.exists() && file.length() > 0L) {
                val cached = BitmapFactory.decodeFile(file.absolutePath)
                if (cached != null) return Pair(cached, true)
            }

            val connection = URL(link).openConnection()
            connection.connectTimeout = 7000
            connection.readTimeout = 9000
            val bytes = connection.getInputStream().use { input -> input.readBytes() }
            val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            if (decoded != null) {
                file.writeBytes(bytes)
                Pair(decoded, false)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        handler.post(frameLoop)
    }

    override fun onDetachedFromWindow() {
        handler.removeCallbacks(frameLoop)
        super.onDetachedFromWindow()
    }
}

class MainActivity : Activity() {
    private val versionName = BuildConfig.VERSION_NAME
    // Change only when the bundled plan changes so visual releases never reset an active workout.
    private val trainingPlanVersion = "12.1.0"
    private val backupSource = "mo2log_native_android"
    private val backupSchema = "personal_backup_v2"
    private val supportedBackupSchemas = setOf("personal_backup_v1", backupSchema)
    private val preImportBackupKey = "pre_import_backup_snapshot"
    private val bg = Mo2Colors.Background
    private val surface = Mo2Colors.Surface
    private val surface2 = Mo2Colors.SurfaceAlt
    private val surface3 = Mo2Colors.SurfaceElevated
    private val border = Mo2Colors.Border
    private val green = Mo2Colors.Primary
    private val muted = Mo2Colors.TextSecondary
    private val white = Mo2Colors.TextPrimary
    private val danger = Mo2Colors.Error
    private val amber = Mo2Colors.Warning

    private val prefs by lazy { getSharedPreferences("mo2log_native", Context.MODE_PRIVATE) }
    private val plans: List<WorkoutPlan>
        get() = buildWorkoutPlans()
    private val catalog by lazy { loadExerciseCatalog() }
    private val runningPlan: List<RunningWorkout>
        get() = buildRunningPlan()
    private val primaryNavItems = listOf(
        NavItem("home", "Inicio"),
        NavItem("workout", "Treino"),
        NavItem("running", "Corrida"),
        NavItem("more", "Mais"),
    )
    private val secondaryNavItems = listOf(
        NavItem("plan_editor", "Plano"),
        NavItem("exercises", "Exercicios"),
        NavItem("history", "Historico"),
        NavItem("stats", "Stats"),
        NavItem("goals", "Metas"),
        NavItem("coach", "Coach"),
        NavItem("profile", "Perfil"),
    )
    private val navItems = primaryNavItems + secondaryNavItems

    private var currentTab = "home"
    private var selectedPlanIndex = 0
    private var selectedExerciseIndex = 0
    private var selectedRunId = ""
    private var voiceCoach: TextToSpeech? = null
    private var voiceCoachReady = false
    private val restTimerHandler = Handler(Looper.getMainLooper())
    private val runningSessionHandler = Handler(Looper.getMainLooper())
    private val audioManager by lazy { getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    private val restAudioFocusListener = AudioManager.OnAudioFocusChangeListener { }
    private var restToneGenerator: ToneGenerator? = null
    private var restAudioFocusRequest: AudioFocusRequest? = null
    private var activeRestUtteranceId: String? = null
    private var restTimerText: TextView? = null
    private var runningCountdownText: TextView? = null
    private var runningCountdownCueText: TextView? = null
    private var runningSessionTitleText: TextView? = null
    private var runningSessionSubtitleText: TextView? = null
    private var runningStageText: TextView? = null
    private var runningRemainingText: TextView? = null
    private var runningDistanceText: TextView? = null
    private var runningPaceText: TextView? = null
    private var runningElapsedText: TextView? = null
    private var runningSpeedText: TextView? = null
    private var runningStageProgressText: TextView? = null
    private var runningNextText: TextView? = null
    private var runningTreadmillText: TextView? = null
    private var runningCountdownPanel: View? = null
    private var runningActiveContent: View? = null
    private var runningPauseButton: Button? = null
    private var pendingRunSummary: JSONObject? = null
    private var requestedSection = ""
    private var pageScrollTarget: View? = null
    private var pageScrollView: ScrollView? = null
    private val restTimerRunnable = object : Runnable {
        override fun run() {
            restTimerText?.let { view ->
                view.text = restTimerDisplay()
                val remaining = restTimerRemainingSeconds()
                if (remaining > 0L) {
                    restTimerHandler.postDelayed(this, 1000L)
                } else {
                    notifyRestTimerFinishedIfNeeded()
                    if (currentTab == "workout") view.post { renderWorkoutInPlace() }
                }
            }
        }
    }
    private val runningSessionRunnable = object : Runnable {
        override fun run() {
            val completed = updateActiveRunProgress()
            if (completed) {
                render()
                return
            }
            refreshRunningSessionViews()
            if (activeRunningWorkout() != null) runningSessionHandler.postDelayed(this, 1000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        syncTrainingPlanVersion()
        restorePersistedState(intent.getStringExtra("tab"))
        requestedSection = intent.getStringExtra("section").orEmpty()
        render()
        window.decorView.post { initVoiceCoach() }
    }

    override fun onDestroy() {
        restTimerHandler.removeCallbacksAndMessages(null)
        runningSessionHandler.removeCallbacks(runningSessionRunnable)
        stopRestCompletionAlert()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        voiceCoach?.stop()
        voiceCoach?.shutdown()
        super.onDestroy()
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        if (currentTab == "running" && activeRunningWorkout() != null) {
            showCancelRunningWorkoutDialog()
            return
        }
        when {
            secondaryNavItems.any { it.id == currentTab } -> switchTab("more")
            currentTab != "home" -> switchTab("home")
            else -> super.onBackPressed()
        }
    }

    private fun syncTrainingPlanVersion() {
        if (prefs.getString("training_plan_version", "") == trainingPlanVersion) return
        migrateCombinedWorkoutExercises()
        val editor = prefs.edit()
            .putString("training_plan_version", trainingPlanVersion)
            .putInt("selected_plan", todayPlanIndex())
            .putInt("selected_exercise", 0)
            .putString("selected_run_id", todayRunningWorkout()?.id ?: "w1-mon")
        if (!prefs.contains("run_distance")) editor.putString("run_distance", "5.0")
        if (!prefs.contains("run_speed")) editor.putString("run_speed", "8.0")
        if (!prefs.contains("running_plan_start_day")) editor.putString("running_plan_start_day", dayKey())
        editor.apply()
    }

    private fun migrateCombinedWorkoutExercises() {
        val customPlans = customWorkoutPlans()
        val legacyNamesByPlan = if (customPlans != null) {
            customPlans.associate { plan -> plan.id to plan.exercises.map { exercise -> exercise.name } }
        } else {
            mapOf(
                "a" to listOf(
                    "Supino reto ou maquina peitoral",
                    "Supino inclinado com halteres",
                    "Desenvolvimento de ombros",
                    "Elevacao lateral",
                    "Triceps corda",
                    "Prancha",
                ),
                "b" to listOf(
                    "Leg press",
                    "Agachamento livre ou guiado",
                    "Cadeira extensora",
                    "Mesa flexora",
                    "Stiff",
                    "Panturrilha",
                    "Abdominal ou prancha",
                ),
                "c" to listOf(
                    "Puxada frente",
                    "Remada baixa",
                    "Remada unilateral",
                    "Face pull",
                    "Rosca direta",
                    "Rosca martelo",
                ),
            )
        }
        val hasCombinedExercise = legacyNamesByPlan.values.flatten().any { name -> splitCombinedExerciseNames(name).size > 1 }
        if (!hasCombinedExercise) return

        val editor = prefs.edit()
        legacyNamesByPlan.forEach { (planId, names) -> migratePlannedSetIndexesForSplit(planId, names, editor) }
        if (customPlans != null) {
            val migrated = customPlans.map { plan ->
                plan.copy(exercises = plan.exercises.flatMap { exercise -> splitCombinedExercise(exercise) })
            }
            editor.putString("custom_workout_plans", workoutPlansToJson(migrated).toString())
        }
        editor.apply()
    }

    private fun migratePlannedSetIndexesForSplit(planId: String, exerciseNames: List<String>, editor: SharedPreferences.Editor) {
        val newIndexByOldIndex = mutableMapOf<Int, Int>()
        var nextIndex = 0
        exerciseNames.forEachIndexed { oldIndex, name ->
            newIndexByOldIndex[oldIndex] = nextIndex
            nextIndex += splitCombinedExerciseNames(name).size
        }
        if (nextIndex == exerciseNames.size) return

        val groups = prefs.all.keys
            .filter { key ->
                key.startsWith("planned_sets_") &&
                    key.substringAfterLast('_').toIntOrNull() != null &&
                    key.substringBeforeLast('_').endsWith("_" + planId)
            }
            .groupBy { key -> key.substringBeforeLast('_') }
        groups.forEach { (baseKey, keys) ->
            val valuesByOldIndex = keys.associate { key ->
                Pair(key.substringAfterLast('_').toInt(), prefs.getString(key, "[]") ?: "[]")
            }
            keys.forEach { key -> editor.remove(key) }
            valuesByOldIndex.forEach { (oldIndex, raw) ->
                newIndexByOldIndex[oldIndex]?.let { newIndex -> editor.putString(baseKey + "_" + newIndex, raw) }
            }
        }
    }

    private fun splitCombinedExercise(exercise: ExercisePlan): List<ExercisePlan> {
        return splitCombinedExerciseNames(exercise.name).map { name -> exercise.copy(name = name) }
    }

    private fun splitCombinedExerciseNames(name: String): List<String> {
        return when (normalized(name)) {
            normalized("Supino reto ou maquina peitoral") -> listOf("Supino reto", "Maquina peitoral")
            normalized("Agachamento livre ou guiado") -> listOf("Agachamento livre", "Agachamento guiado")
            normalized("Abdominal ou prancha") -> listOf("Abdominal", "Prancha")
            else -> {
                val parts = Regex("\\s+ou\\s+", RegexOption.IGNORE_CASE)
                    .split(name)
                    .map { part -> part.trim() }
                    .filter { part -> part.isNotBlank() }
                if (parts.size <= 1) listOf(name) else parts.map { part ->
                    part.replaceFirstChar { first -> if (first.isLowerCase()) first.titlecase(Locale("pt", "BR")) else first.toString() }
                }
            }
        }
    }

    private fun restorePersistedState(preferredTab: String? = null) {
        val persistedTab = prefs.getString("current_tab", "home").orEmpty()
        currentTab = listOfNotNull(preferredTab, persistedTab, "home")
            .first { candidate -> navItems.any { it.id == candidate } }

        val availablePlans = plans
        selectedPlanIndex = prefs.getInt("selected_plan", todayPlanIndex())
            .coerceIn(availablePlans.indices)
        selectedExerciseIndex = prefs.getInt("selected_exercise", 0)
            .coerceIn(availablePlans[selectedPlanIndex].exercises.indices)

        val selectedRun = prefs.getString("selected_run_id", "").orEmpty()
        val fallbackRun = todayRunningWorkout()?.id
            ?: currentRunningWeekWorkouts().firstOrNull()?.id
            ?: runningPlan.firstOrNull()?.id
            ?: ""
        selectedRunId = selectedRun.takeIf { runningWorkoutById(it) != null } ?: fallbackRun

        val editor = prefs.edit()
            .putString("current_tab", currentTab)
            .putInt("selected_plan", selectedPlanIndex)
            .putInt("selected_exercise", selectedExerciseIndex)
            .putString("selected_run_id", selectedRunId)
        val activeRunId = prefs.getString("running_active_id", "").orEmpty()
        if (activeRunId.isNotBlank() && runningWorkoutById(activeRunId) == null) {
            clearActiveRunState(editor)
        }
        editor.apply()
    }

    private fun render(preserveWorkoutScroll: Boolean = false) {
        val previousWorkoutScroll = if (preserveWorkoutScroll && currentTab == "workout") {
            pageScrollView?.scrollY
        } else {
            null
        }
        restTimerHandler.removeCallbacks(restTimerRunnable)
        runningSessionHandler.removeCallbacks(runningSessionRunnable)
        restTimerText = null
        runningCountdownText = null
        runningCountdownCueText = null
        runningSessionTitleText = null
        runningSessionSubtitleText = null
        runningStageText = null
        runningRemainingText = null
        runningDistanceText = null
        runningPaceText = null
        runningElapsedText = null
        runningSpeedText = null
        runningNextText = null
        runningTreadmillText = null
        runningCountdownPanel = null
        runningActiveContent = null
        runningPauseButton = null
        pageScrollTarget = null
        updateSessionWakeLock()

        val page = LinearLayout(this)
        page.orientation = LinearLayout.VERTICAL
        page.setBackgroundColor(bg)

        val scroll = ScrollView(this)
        scroll.setBackgroundColor(bg)
        pageScrollView = scroll

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(dp(Mo2Spacing.Xxl), dp(Mo2Spacing.Xxl), dp(Mo2Spacing.Xxl), dp(Mo2Spacing.Xxl))
        scroll.addView(root)

        val focusedRunningSession = currentTab == "running" && activeRunningWorkout() != null
        val focusedStrengthSession = currentTab == "workout"
        if (!focusedRunningSession && !focusedStrengthSession) {
            if (currentTab !in setOf("home", "running", "more")) {
                root.addView(header())
            }
            root.addView(pageIntro())
        }

        when (currentTab) {
            "home" -> renderHome(root)
            "workout" -> renderWorkout(root)
            "running" -> renderRunning(root)
            "more" -> renderMore(root)
            "plan_editor" -> renderPlanEditor(root)
            "history" -> renderHistory(root)
            "stats" -> renderStats(root)
            "exercises" -> renderExercises(root)
            "goals" -> renderGoals(root)
            "coach" -> renderCoach(root)
            "profile" -> renderProfile(root)
            else -> renderHome(root)
        }

        root.isFocusableInTouchMode = true
        root.requestFocus()

        page.addView(scroll, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        val navigation = if (focusedRunningSession) null else bottomNav()
        if (navigation != null) page.addView(navigation)
        applySystemBarInsets(page, root, navigation)
        setContentView(page)

        pageScrollTarget?.let { target ->
            scrollPageToTarget(root, target)
        } ?: previousWorkoutScroll?.let { scrollY ->
            scroll.post {
                val maxScroll = (root.height - scroll.height).coerceAtLeast(0)
                scroll.scrollTo(0, scrollY.coerceIn(0, maxScroll))
            }
        }

        pendingRunSummary?.let { summary ->
            pendingRunSummary = null
            page.post { showRunningWorkoutSummary(summary) }
        }
    }

    private fun renderWorkoutInPlace() {
        render(preserveWorkoutScroll = true)
    }

    private fun scrollPageToTarget(root: LinearLayout, target: View) {
        val scroll = pageScrollView ?: return
        scroll.post {
            var targetTop = target.top
            var parent = target.parent
            while (parent is View && parent !== root) {
                targetTop += parent.top
                parent = parent.parent
            }
            scroll.scrollTo(0, (targetTop - dp(Mo2Spacing.Md)).coerceAtLeast(0))
            requestedSection = ""
        }
    }

    private fun updateSessionWakeLock() {
        val keepScreenOn = currentTab == "workout" || activeRunningWorkout() != null || restTimerRemainingSeconds() > 0L
        if (keepScreenOn) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun header(): View {
        val stats = computeStats(allLogs())
        val box = card(surface3)
        box.orientation = LinearLayout.VERTICAL

        val top = LinearLayout(this)
        top.gravity = Gravity.CENTER_VERTICAL
        top.orientation = LinearLayout.HORIZONTAL

        val logo = TextView(this)
        logo.text = "M2"
        logo.gravity = Gravity.CENTER
        logo.setTextColor(bg)
        logo.textSize = 18f
        logo.typeface = Typeface.DEFAULT_BOLD
        logo.background = rounded(green, dp(8))
        top.addView(logo, LinearLayout.LayoutParams(dp(50), dp(50)))

        val titleBox = LinearLayout(this)
        titleBox.orientation = LinearLayout.VERTICAL
        titleBox.setPadding(dp(14), 0, 0, 0)
        titleBox.addView(label("MO2 LOG", green, 13f, true))
        titleBox.addView(label("Treino pessoal", white, 25f, true))
        titleBox.addView(label("Android nativo | v" + versionName, muted, 14f, false))
        top.addView(titleBox, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        top.addView(statusChip("Local"))
        box.addView(top)

        val summary = LinearLayout(this)
        summary.orientation = LinearLayout.HORIZONTAL
        summary.setPadding(0, dp(14), 0, 0)
        summary.addView(compactMetric("Hoje", todayLogs().length().toString() + " series"), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        summary.addView(compactMetric("Semana", stats.optInt("week_sets").toString() + " series"), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        summary.addView(compactMetric("Volume", stats.optInt("today_volume").toString() + " kg"), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        box.addView(summary)

        return box
    }

    private fun pageIntro(): View {
        val wrap = LinearLayout(this)
        wrap.orientation = LinearLayout.VERTICAL
        wrap.setPadding(0, dp(18), 0, if (currentTab == "home") dp(10) else dp(6))

        val title = label(
            currentSectionTitle(),
            white,
            if (currentTab == "home") 40f else 26f,
            currentTab != "home",
        )
        if (currentTab == "home") {
            title.typeface = resources.getFont(R.font.be_vietnam_pro_regular)
            title.includeFontPadding = false
            title.setLineSpacing(0f, 1f)
            title.setAutoSizeTextTypeUniformWithConfiguration(30, 40, 1, TypedValue.COMPLEX_UNIT_SP)
        }
        wrap.addView(title)
        val subtitle = currentSectionSubtitle()
        if (subtitle.isNotBlank()) wrap.addView(label(subtitle, muted, 14f, false))
        return wrap
    }

    private fun bottomNav(): View {
        val wrap = LinearLayout(this)
        wrap.orientation = LinearLayout.VERTICAL
        wrap.setBackgroundColor(surface)

        val divider = View(this)
        divider.setBackgroundColor(border)
        wrap.addView(divider, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1)))

        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        row.setPadding(dp(Mo2Spacing.Sm), dp(Mo2Spacing.Xs), dp(Mo2Spacing.Sm), dp(Mo2Spacing.Xs))
        primaryNavItems.forEach { item ->
            val active = isBottomNavActive(item.id)
            val icon = when (item.id) {
                "home" -> Mo2NavIcon.Home
                "workout" -> Mo2NavIcon.Strength
                "running" -> Mo2NavIcon.Running
                else -> Mo2NavIcon.More
            }
            val button = Mo2Components.bottomNavigationItem(
                context = this,
                title = item.title,
                icon = icon,
                active = active,
                onClick = { switchTab(item.id) },
            )
            val params = LinearLayout.LayoutParams(0, dp(64), 1f)
            params.setMargins(dp(2), 0, dp(2), 0)
            row.addView(button, params)
        }
        wrap.addView(row, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(72)))
        return wrap
    }

    @Suppress("DEPRECATION")
    private fun applySystemBarInsets(page: View, content: LinearLayout, navigation: View?) {
        page.setOnApplyWindowInsetsListener { _, insets ->
            val topInset = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                insets.getInsets(WindowInsets.Type.statusBars()).top
            } else {
                insets.systemWindowInsetTop
            }
            val bottomInset = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                insets.getInsets(WindowInsets.Type.navigationBars()).bottom
            } else {
                insets.systemWindowInsetBottom
            }
            val enforcedEdgeToEdge = Build.VERSION.SDK_INT >= 35
            page.setPadding(0, if (enforcedEdgeToEdge) topInset else 0, 0, 0)
            content.setPadding(
                dp(Mo2Spacing.Xxl),
                dp(Mo2Spacing.Xxl) + if (enforcedEdgeToEdge) 0 else topInset,
                dp(Mo2Spacing.Xxl),
                dp(Mo2Spacing.Xxl) + if (navigation == null) bottomInset else 0,
            )
            navigation?.setPadding(0, 0, 0, bottomInset)
            insets
        }
        page.requestApplyInsets()
    }

    private fun isBottomNavActive(id: String): Boolean {
        return if (id == "more") {
            currentTab == "more" || secondaryNavItems.any { it.id == currentTab }
        } else {
            currentTab == id
        }
    }

    private fun renderMore(root: LinearLayout) {
        val hub = card(surface3)
        hub.orientation = LinearLayout.VERTICAL
        hub.addView(label("CENTRAL DO APP", green, 13f, true))
        hub.addView(label("Ferramentas extras em um lugar so", white, 25f, true))
        hub.addView(label("Plano, historico, estatisticas, catalogo, metas, coach e perfil ficam aqui para deixar o uso diario mais direto.", muted, 15f, false))
        root.addView(hub)

        val shortcuts = listOf(
            Triple("plan_editor", "Plano", "Editar treinos, exercicios e ajustes da corrida."),
            Triple("exercises", "Exercicios", "Catalogo com midia, favoritos e alternativas."),
            Triple("history", "Historico", "Series e corridas registradas no celular."),
            Triple("stats", "Stats", "Volume, cargas, semana e melhores marcas."),
            Triple("goals", "Metas", "Objetivos semanais de treino e corrida."),
            Triple("coach", "Coach", "Insights simples para ajustar a rotina."),
            Triple("profile", "Perfil", "Dados locais, versao e exportacao."),
        )

        shortcuts.chunked(2).forEach { rowItems ->
            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            rowItems.forEachIndexed { index, item ->
                val shortcut = moreShortcut(item.first, item.second, item.third)
                val params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                params.setMargins(if (index == 0) 0 else dp(8), dp(8), 0, 0)
                row.addView(shortcut, params)
            }
            if (rowItems.size == 1) {
                val spacer = View(this)
                row.addView(spacer, LinearLayout.LayoutParams(0, 1, 1f))
            }
            root.addView(row)
        }
        root.addView(heroCard("Proxima tela", "Treino sempre a um toque", "Use a barra inferior para voltar rapido ao treino ou registrar corrida sem atravessar menus."))
    }

    private fun moreShortcut(id: String, title: String, subtitle: String): View {
        val box = card(surface2)
        box.orientation = LinearLayout.VERTICAL
        box.minimumHeight = dp(116)
        box.setOnClickListener { switchTab(id) }
        box.addView(label(title, white, 18f, true))
        box.addView(label(subtitle, muted, 13f, false))
        return box
    }

    private fun renderHome(root: LinearLayout) {
        val stats = computeStats(allLogs())

        root.addView(weeklyDashboardPanel())
        root.addView(stablePersonalDashboardPanel())
        root.addView(todayDashboardPanel())
        root.addView(quickActionsPanel())
        root.addView(weeklyAgendaDashboardPanel())
        root.addView(dailyCommandPanel())
        root.addView(readinessCheckInPanel())
        root.addView(consistencyChecklistPanel())

        val favorites = favoriteCatalogExercises()
        if (favorites.isNotEmpty()) {
            val favBox = card(surface)
            favBox.orientation = LinearLayout.VERTICAL
            favBox.addView(label("ATALHOS FAVORITOS", green, 13f, true))
            favBox.addView(label("Abra seus exercicios mais usados com um toque.", muted, 15f, false))
            favorites.take(3).forEach { exercise ->
                val shortcut = actionButton(exercise.name, surface2, white)
                shortcut.setOnClickListener {
                    prefs.edit()
                        .putString("catalog_muscle", "Todos")
                        .putString("catalog_selected", exercise.id)
                        .apply()
                    switchTab("exercises")
                }
                favBox.addView(buttonParams(shortcut))
            }
            root.addView(favBox)
        }

        root.addView(metricGrid(listOf(
            Pair("Semana", stats.optInt("week_sets").toString() + " series"),
            Pair("Volume total", stats.optInt("total_volume").toString() + " kg"),
            Pair("Melhor carga", stats.optString("best_load")),
            Pair("Corridas", runLogs().length().toString()),
        )))

        val insightBox = card(surface)
        insightBox.orientation = LinearLayout.VERTICAL
        insightBox.addView(label("INSIGHTS", green, 13f, true))
        localInsights().forEach { insight -> insightBox.addView(label("- " + insight, white, 15f, false)) }
        root.addView(insightBox)
    }

    private fun stablePersonalDashboardPanel(): View {
        val stats = computeStats(allLogs())
        val weekTarget = prefs.getString("goal_week_sets", "60")?.toIntOrNull() ?: 60
        val weekSets = stats.optInt("week_sets")
        val weekRuns = currentRunningWeekWorkouts()
        val runningDone = weekRuns.count { isRunWorkoutCompleted(it) }
        val strengthToday = todayLogs().length() > 0 || hasCompletedStrengthSession(dayKey())
        val runToday = todayRunCount() > 0
        val checkInDone = readinessStatus().isNotBlank()
        val backupDone = prefs.getString("last_backup_day", "") == dayKey()
        val doneCount = listOf(strengthToday, runToday, checkInDone, backupDone).count { it }
        val weeklyScore = ((progressPercent(weekSets, weekTarget).coerceAtMost(100) + progressPercent(runningDone, weekRuns.size).coerceAtMost(100)) / 2)

        val box = card(surface3)
        box.orientation = LinearLayout.VERTICAL
        box.addView(label("MO2 LOG V12", green, 13f, true))
        box.addView(label("Central pessoal integrada", white, 26f, true))
        box.addView(label("Offline, local e pronto para academia: treino, corrida, historico e backup no mesmo fluxo.", muted, 14f, false))

        val metrics = LinearLayout(this)
        metrics.orientation = LinearLayout.HORIZONTAL
        metrics.addView(compactMetric("Semana", weeklyScore.toString() + "%"), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        metrics.addView(compactMetric("Hoje", doneCount.toString() + "/4"), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        metrics.addView(compactMetric("Dados", localDataHealthLabel()), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        box.addView(spacedRow(metrics))

        box.addView(label("Proximo passo: " + stableNextActionLine(), white, 15f, true))
        box.addView(label("Ultima atividade: " + latestActivitySummary(), muted, 13f, false))
        box.addView(label(dataSafetyLine(), if (backupDone) green else amber, 13f, true))
        return box
    }

    private fun quickActionsPanel(): View {
        val box = card(surface2)
        box.orientation = LinearLayout.VERTICAL
        box.addView(label("ACOES RAPIDAS", green, 13f, true))
        box.addView(label("Comece pelo que voce vai usar agora.", white, 20f, true))

        val firstRow = LinearLayout(this)
        firstRow.orientation = LinearLayout.HORIZONTAL
        val workout = actionButton("Iniciar treino", green, bg)
        workout.setOnClickListener { switchTab("workout") }
        firstRow.addView(workout, LinearLayout.LayoutParams(0, dp(50), 1f))
        val running = actionButton("Iniciar corrida", surface3, Mo2Colors.Running)
        running.setOnClickListener { switchTab("running") }
        firstRow.addView(running, LinearLayout.LayoutParams(0, dp(50), 1f))
        box.addView(spacedRow(firstRow))

        val secondRow = LinearLayout(this)
        secondRow.orientation = LinearLayout.HORIZONTAL
        val history = actionButton("Historico", surface3, white)
        history.setOnClickListener { switchTab("history") }
        secondRow.addView(history, LinearLayout.LayoutParams(0, dp(50), 1f))
        val backup = actionButton("Backup", surface3, green)
        backup.setOnClickListener { exportToClipboard() }
        secondRow.addView(backup, LinearLayout.LayoutParams(0, dp(50), 1f))
        box.addView(spacedRow(secondRow))
        return box
    }

    private fun weeklyDashboardPanel(): View {
        val stats = computeStats(allLogs())
        val weekTarget = prefs.getString("goal_week_sets", "60")?.toIntOrNull() ?: 60
        val weekRuns = currentRunningWeekWorkouts()
        val runningDone = weekRuns.count { isRunWorkoutCompleted(it) }
        return Mo2WeeklyDashboardView(
            this,
            Mo2WeeklyDashboardData(
                strengthWorkouts = currentWeekStrengthWorkoutCount(),
                strengthWorkoutTarget = 3,
                runningWorkouts = runningDone,
                runningWorkoutTarget = weekRuns.size.coerceAtLeast(1),
                completedSets = stats.optInt("week_sets"),
                setTarget = weekTarget.coerceAtLeast(1),
                volumeKg = stats.optInt("week_volume"),
                distanceKm = currentWeekRunDistance(),
                activitySeconds = currentWeekActivitySeconds(),
            ),
        ).apply {
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(252),
            )
            params.setMargins(0, dp(Mo2Spacing.Sm), 0, dp(Mo2Spacing.Sm))
            layoutParams = params
        }
    }

    private fun todayDashboardPanel(): View {
        val stats = computeStats(allLogs())
        val mission = todayMission()
        val plan = currentPlan()
        val run = todayRunningWorkout()
        val strengthText = if (hasCompletedStrengthSession(dayKey())) {
            plan.title + " concluido. Exercicios nao realizados foram registrados como pulados."
        } else if (isStrengthDayToday()) {
            plan.title + " - " + plan.focus + " | " + plan.exercises.size + " exercicios"
        } else {
            "Sem musculacao principal hoje. Preserve energia para a corrida ou recuperacao."
        }
        val runText = if (run != null) {
            run.dayName + " - " + run.title + " | " + formatKm(totalRunDistance(run)) + " | " + formatDuration(estimatedWorkoutSeconds(run))
        } else {
            "Sem corrida planejada para hoje."
        }

        val box = card(surface2)
        box.orientation = LinearLayout.VERTICAL
        box.addView(label("HOJE", green, 13f, true))
        box.addView(label(mission.first, white, 24f, true))
        box.addView(label(mission.second, muted, 14f, false))
        box.addView(label("Musculacao: " + strengthText, white, 15f, true))
        box.addView(label("Corrida: " + runText, muted, 14f, false))
        box.addView(label(todayLogs().length().toString() + " series hoje | volume " + stats.optInt("today_volume") + " kg | corridas " + todayRunCount(), muted, 13f, false))

        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        val workout = actionButton("Treino", if (isStrengthDayToday()) green else surface2, if (isStrengthDayToday()) bg else white)
        workout.setOnClickListener { switchTab("workout") }
        row.addView(workout, LinearLayout.LayoutParams(0, dp(50), 1f))
        val running = actionButton("Corrida", if (missionButtonTab() == "running") green else surface2, if (missionButtonTab() == "running") bg else white)
        running.setOnClickListener { switchTab("running") }
        row.addView(running, LinearLayout.LayoutParams(0, dp(50), 1f))
        box.addView(spacedRow(row))
        return box
    }

    private fun weeklyAgendaDashboardPanel(): View {
        val workoutsByDay = currentRunningWeekWorkouts().associateBy { it.dayIndex }
        val box = card(surface)
        box.orientation = LinearLayout.VERTICAL
        box.addView(label("AGENDA DA SEMANA", green, 13f, true))
        box.addView(label("Treinos planejados ate a prova de 5 km", white, 22f, true))
        hybridWeekLines().forEachIndexed { index, line ->
            val dayIndex = index + 1
            val runDone = workoutsByDay[dayIndex]?.let { isRunWorkoutCompleted(it) } ?: false
            val strengthDone = isStrengthWeekDayCompleted(dayIndex)
            val marker = if (runDone || strengthDone) "[x] " else "[ ] "
            val color = if (runDone || strengthDone) green else white
            box.addView(label(marker + line, color, 14f, false))
        }
        val open = actionButton("Planejamento completo", surface2, green)
        open.setOnClickListener { switchTab("running") }
        box.addView(buttonParams(open))
        return box
    }

    private fun dashboardProgressLine(title: String, detail: String, percent: Int, color: Int): View {
        val wrap = LinearLayout(this)
        wrap.orientation = LinearLayout.VERTICAL
        wrap.setPadding(0, dp(14), 0, 0)

        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        row.addView(label(title, white, 15f, true), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        row.addView(label(percent.coerceAtMost(999).toString() + "%", color, 15f, true))
        wrap.addView(row)
        wrap.addView(label(detail, muted, 13f, false))

        val bar = Mo2Components.progressBar(this, percent.coerceIn(0, 100), color)
        val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(10))
        params.setMargins(0, dp(8), 0, 0)
        wrap.addView(bar, params)
        return wrap
    }

    private fun currentWeekRunDistance(): Double {
        val runs = runLogs()
        var total = 0.0
        for (i in 0 until runs.length()) {
            val item = runs.getJSONObject(i)
            if (item.optString("week") == weekKey()) total += item.optDouble("distance")
        }
        return roundKm(total)
    }

    private fun currentWeekStrengthWorkoutCount(): Int {
        val completed = linkedSetOf<String>()
        val sessions = strengthSessionLogs()
        for (index in 0 until sessions.length()) {
            val item = sessions.getJSONObject(index)
            val day = item.optString("day")
            if (isValidDayKey(day) && item.optString("status", "completed") == "completed" && weekKeyForDay(day) == weekKey()) {
                completed.add(day + "::" + item.optString("plan_id", item.optString("plan_title", "Treino")))
            }
        }
        return completed.size
    }

    private fun currentWeekActivitySeconds(): Long {
        var total = 0L
        val runs = runLogs()
        for (index in 0 until runs.length()) {
            val item = runs.getJSONObject(index)
            if (!isValidDayKey(item.optString("day")) || weekKeyForDay(item.optString("day")) != weekKey()) continue
            total += item.optLong("duration_seconds", parseHistoryDurationSeconds(item.optString("duration"))).coerceAtLeast(0L)
        }

        val sessions = strengthSessionLogs()
        for (index in 0 until sessions.length()) {
            val item = sessions.getJSONObject(index)
            if (!isValidDayKey(item.optString("day")) || item.optString("status", "completed") != "completed" || weekKeyForDay(item.optString("day")) != weekKey()) continue
            total += item.optLong("duration_seconds", 0L)
                .takeIf { it > 0L }
                ?: estimateStrengthSessionSeconds(item.optString("day"), item.optString("plan_title", "Treino"))
        }
        return total
    }

    private fun estimateStrengthSessionSeconds(day: String, planTitle: String): Long {
        val times = mutableListOf<Int>()
        var setCount = 0
        val logs = allLogs()
        for (index in 0 until logs.length()) {
            val item = logs.getJSONObject(index)
            if (item.optString("day") != day || item.optString("plan", "Treino") != planTitle) continue
            setCount += 1
            val parts = item.optString("time").split(":")
            val hour = parts.getOrNull(0)?.toIntOrNull()
            val minute = parts.getOrNull(1)?.toIntOrNull()
            if (hour != null && minute != null && hour in 0..23 && minute in 0..59) times.add(hour * 60 + minute)
        }
        if (setCount <= 0) return 0L
        val measured = if (times.size >= 2) ((times.maxOrNull()!! - times.minOrNull()!!).coerceAtLeast(0) * 60L) else 0L
        val estimated = setCount * 120L
        return max(measured + 60L, estimated).coerceAtMost(4L * 3600L)
    }

    private fun stableNextActionLine(): String {
        val mission = todayMission()
        val strengthDone = todayLogs().length() > 0 || hasCompletedStrengthSession(dayKey())
        val runDone = todayRunCount() > 0
        val checkInDone = readinessStatus().isNotBlank()
        val backupDone = prefs.getString("last_backup_day", "") == dayKey()
        val run = todayRunningWorkout()
        return when {
            !checkInDone -> "fazer check-in rapido antes de decidir intensidade."
            isStrengthDayToday() && !strengthDone -> "abrir " + currentPlan().title + " e registrar as series."
            run != null && !runDone -> "abrir corrida: " + run.title + "."
            !backupDone -> "copiar backup local depois do treino."
            else -> mission.first.lowercase(Locale("pt", "BR")) + " concluido ou em dia; revisar historico se quiser ajustar."
        }
    }

    private fun localDataHealthLabel(): String {
        val total = allLogs().length() + runLogs().length() + strengthSessionLogs().length()
        if (total <= 0) return "novo"
        val backupDay = prefs.getString("last_backup_day", "").orEmpty()
        val age = if (isValidDayKey(backupDay)) daysBetween(backupDay, dayKey()) else null
        return when (Mo2ProgressEngine.dataHealth(invalidLocalCollectionCount(), age, total).status) {
            "Integro" -> "ok"
            "Revisar" -> "revisar"
            else -> "backup"
        }
    }

    private fun dataSafetyLine(): String {
        val backupDay = prefs.getString("last_backup_day", "").orEmpty()
        return if (backupDay == dayKey()) {
            "Backup de hoje ja foi copiado."
        } else if (backupDay.isBlank()) {
            "Backup ainda nao feito neste aparelho. Use Acoes Rapidas > Backup."
        } else {
            "Ultimo backup: " + backupDay + ". Copie novamente depois de registrar novos treinos."
        }
    }

    private fun latestActivitySummary(): String {
        var latestKey = ""
        var latestText = "nenhum registro local ainda."
        val sets = allLogs()
        for (index in 0 until sets.length()) {
            val item = sets.getJSONObject(index)
            val key = item.optString("day") + item.optString("time")
            if (key >= latestKey) {
                latestKey = key
                latestText = item.optString("day") + " " + item.optString("time") + " | " +
                    item.optString("exercise", "Serie") + " " + item.optInt("reps") + " reps"
            }
        }
        val runs = runLogs()
        for (index in 0 until runs.length()) {
            val item = runs.getJSONObject(index)
            val key = item.optString("day") + item.optString("time")
            if (key >= latestKey) {
                latestKey = key
                latestText = item.optString("day") + " " + item.optString("time") + " | " +
                    item.optString("workout_title", "Corrida") + " " + formatKm(item.optDouble("distance"))
            }
        }
        val sessions = strengthSessionLogs()
        for (index in 0 until sessions.length()) {
            val item = sessions.getJSONObject(index)
            val key = item.optString("day") + item.optString("time")
            if (key >= latestKey) {
                latestKey = key
                latestText = item.optString("day") + " " + item.optString("time") + " | " +
                    item.optString("plan_title", "Treino") + " concluido"
            }
        }
        return latestText
    }

    private fun isStrengthDayToday(): Boolean {
        val day = SimpleDateFormat("u", Locale.US).format(Date()).toIntOrNull() ?: 1
        return day == 2 || day == 4 || day == 6
    }

    private fun isStrengthWeekDayCompleted(dayIndex: Int): Boolean {
        val sessions = strengthSessionLogs()
        for (i in 0 until sessions.length()) {
            val item = sessions.getJSONObject(i)
            if (item.optString("week") == weekKey() &&
                item.optString("status", "completed") == "completed" &&
                dayIndexFor(item.optString("day")) == dayIndex) return true
        }
        val logs = allLogs()
        for (i in 0 until logs.length()) {
            val item = logs.getJSONObject(i)
            if (item.optString("week") == weekKey() && dayIndexFor(item.optString("day")) == dayIndex) return true
        }
        return false
    }

    private fun dayIndexFor(day: String): Int? {
        return try {
            val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(day) ?: return null
            SimpleDateFormat("u", Locale.US).format(parsed).toIntOrNull()
        } catch (_: Exception) {
            null
        }
    }

    private fun dailyCommandPanel(): View {
        val stats = computeStats(allLogs())
        val weekTarget = prefs.getString("goal_week_sets", "60")?.toIntOrNull() ?: 60
        val weekSets = stats.optInt("week_sets")
        val weekPercent = progressPercent(weekSets, weekTarget)
        val runningDone = currentRunningWeekWorkouts().count { isRunWorkoutCompleted(it) }
        val runningTotal = currentRunningWeekWorkouts().size
        val mission = todayMission()

        val box = card(surface2)
        box.orientation = LinearLayout.VERTICAL
        box.addView(label("COCKPIT V12", green, 13f, true))
        box.addView(label(mission.first, white, 24f, true))
        box.addView(label(mission.second, muted, 14f, false))
        box.addView(label("Semana: " + weekSets + "/" + weekTarget + " series (" + weekPercent + "%) | Corridas " + runningDone + "/" + runningTotal, white, 15f, true))
        box.addView(label(readinessLine(stats), muted, 14f, false))

        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        val primary = actionButton(missionButtonLabel(), green, bg)
        primary.setOnClickListener { switchTab(missionButtonTab()) }
        row.addView(primary, LinearLayout.LayoutParams(0, dp(50), 1f))

        val backup = actionButton("Backup", surface2, green)
        backup.setOnClickListener { exportToClipboard() }
        row.addView(backup, LinearLayout.LayoutParams(0, dp(50), 1f))
        box.addView(spacedRow(row))
        return box
    }

    private fun consistencyChecklistPanel(): View {
        val strengthDone = todayLogs().length() > 0 || hasCompletedStrengthSession(dayKey())
        val runDone = todayRunCount() > 0
        val backupDone = prefs.getString("last_backup_day", "") == dayKey()
        val checkInDone = readinessStatus().isNotBlank()
        val weekTarget = prefs.getString("goal_week_sets", "60")?.toIntOrNull() ?: 60
        val weekSets = computeStats(allLogs()).optInt("week_sets")

        val box = card(surface2)
        box.orientation = LinearLayout.VERTICAL
        box.addView(label("CHECKLIST DE CONTINUIDADE", green, 13f, true))
        listOf(
            checklistLine(strengthDone, "Musculacao registrada hoje"),
            checklistLine(runDone, "Corrida registrada hoje"),
            checklistLine(checkInDone, "Check-in de prontidao feito"),
            checklistLine(backupDone, "Backup copiado hoje"),
            checklistLine(weekSets >= weekTarget, "Meta semanal de series em dia"),
        ).forEach { line -> box.addView(label(line, white, 15f, false)) }
        box.addView(label("Use o checklist como manutencao do app pessoal: registrar, correr, proteger dados e fechar semana.", muted, 13f, false))
        return box
    }

    private fun readinessCheckInPanel(): View {
        val status = readinessStatus()
        val box = card(surface)
        box.orientation = LinearLayout.VERTICAL
        box.addView(label("CHECK-IN RAPIDO", green, 13f, true))
        box.addView(label(if (status.isBlank()) "Como voce esta hoje?" else "Hoje: " + readinessTitle(status), white, 22f, true))
        box.addView(label(readinessGuidance(status), muted, 14f, false))

        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        listOf(
            Pair("green", "Verde"),
            Pair("yellow", "Amarelo"),
            Pair("red", "Vermelho"),
        ).forEach { item ->
            val active = status == item.first
            val button = actionButton(item.second, if (active) green else surface2, if (active) bg else white)
            button.setOnClickListener { saveReadinessStatus(item.first) }
            row.addView(button, LinearLayout.LayoutParams(0, dp(50), 1f))
        }
        box.addView(spacedRow(row))
        return box
    }

    private fun todayMission(): Pair<String, String> {
        val day = SimpleDateFormat("u", Locale.US).format(Date()).toIntOrNull() ?: 1
        return when (day) {
            1 -> Pair("Corrida principal", "Segunda sem musculacao: faca o treino forte de corrida e proteja a recuperacao.")
            2 -> Pair("Treino A + leve", "Musculacao primeiro. Depois, corrida leve se estiver inteiro.")
            3 -> Pair("Recuperacao", "Dia para mobilidade, caminhada leve ou descanso sem culpa.")
            4 -> Pair("Treino B controlado", "Pernas e core primeiro. Corrida curta so para soltar.")
            5 -> Pair("Preparar o sabado", "Descanso, sono e alimentacao para chegar bem ao Treino C.")
            6 -> Pair("Treino C + ritmo", "Costas e biceps, depois bloco de ritmo sem sprintar.")
            7 -> Pair("Longo leve", "Corrida facil para construir base aerobica e fechar a semana.")
            else -> Pair("Treino pessoal", "Abra o cockpit e siga o proximo bloco planejado.")
        }
    }

    private fun missionButtonLabel(): String {
        return when (missionButtonTab()) {
            "running" -> "Abrir corrida"
            "coach" -> "Abrir coach"
            else -> "Abrir treino"
        }
    }

    private fun missionButtonTab(): String {
        val day = SimpleDateFormat("u", Locale.US).format(Date()).toIntOrNull() ?: 1
        return when (day) {
            1, 7 -> "running"
            3, 5 -> "coach"
            else -> "workout"
        }
    }

    private fun readinessLine(stats: JSONObject): String {
        val status = readinessStatus()
        val avgRpe = stats.optString("avg_rpe").replace(',', '.').toDoubleOrNull()
        val strengthFinished = hasCompletedStrengthSession(dayKey())
        return when {
            status == "red" -> "Prontidao: vermelha. Reduza carga, corte extras e priorize recuperar."
            status == "yellow" -> "Prontidao: amarela. Mantenha o plano, mas sem buscar recorde hoje."
            status == "green" -> "Prontidao: verde. Pode seguir o plano com progressao controlada."
            restTimerRemainingSeconds() > 0L -> "Prontidao: em intervalo. Respire e volte quando o descanso terminar."
            avgRpe != null && avgRpe >= 9.0 -> "Prontidao: cautela. RPE recente alto, mantenha tecnica e evite extras."
            strengthFinished && todayRunCount() > 0 -> "Prontidao: dia completo registrado. Agora a prioridade e recuperar."
            strengthFinished -> "Prontidao: musculacao encerrada. O app preservou feitos, parciais e pulados."
            todayLogs().length() > 0 -> "Prontidao: musculacao ja entrou. Corrida leve so se combinar com o dia."
            todayRunCount() > 0 -> "Prontidao: corrida registrada. Musculacao so se estiver no plano do dia."
            else -> "Prontidao: comece pelo bloco principal do dia e registre tudo no app."
        }
    }

    private fun progressPercent(value: Int, target: Int): Int {
        if (target <= 0) return 0
        return ((value.toDouble() / target.toDouble()) * 100.0).roundToInt().coerceIn(0, 999)
    }

    private fun todayRunCount(): Int {
        val runs = runLogs()
        var count = 0
        for (i in 0 until runs.length()) {
            if (runs.getJSONObject(i).optString("day") == dayKey()) count += 1
        }
        return count
    }

    private fun checklistLine(done: Boolean, text: String): String {
        return (if (done) "[x] " else "[ ] ") + text
    }

    private fun readinessStatus(): String {
        if (prefs.getString("readiness_day", "") != dayKey()) return ""
        return prefs.getString("readiness_status", "") ?: ""
    }

    private fun saveReadinessStatus(status: String) {
        prefs.edit()
            .putString("readiness_day", dayKey())
            .putString("readiness_status", status)
            .apply()
        Toast.makeText(this, "Check-in salvo: " + readinessTitle(status), Toast.LENGTH_SHORT).show()
        render()
    }

    private fun readinessTitle(status: String): String {
        return when (status) {
            "green" -> "Verde - pronto para treinar"
            "yellow" -> "Amarelo - moderar"
            "red" -> "Vermelho - recuperar"
            else -> "Sem check-in"
        }
    }

    private fun readinessGuidance(status: String): String {
        return when (status) {
            "green" -> "Siga o plano e use as sugestoes de carga com progressao limpa."
            "yellow" -> "Treine conservador: mantenha carga, controle descanso e evite volume extra."
            "red" -> "Use o dia para recuperar ou fazer leve. Se treinar, reduza intensidade."
            else -> "Toque em uma cor antes do treino. Isso ajusta a leitura de prontidao do Cockpit."
        }
    }

    private fun weeklyHybridPlanPanel(): View {
        val box = card(surface3)
        box.orientation = LinearLayout.VERTICAL
        box.addView(label("PLANO HIBRIDO DA SEMANA", green, 13f, true))
        box.addView(label("Musculacao 3x/semana + corrida para 5 km", white, 22f, true))
        hybridWeekLines().forEach { line -> box.addView(label(line, white, 14f, false)) }
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        val workout = actionButton("Treino", green, bg)
        workout.setOnClickListener { switchTab("workout") }
        row.addView(workout, LinearLayout.LayoutParams(0, dp(50), 1f))
        val running = actionButton("Corrida 5 km", surface2, white)
        running.setOnClickListener { switchTab("running") }
        row.addView(running, LinearLayout.LayoutParams(0, dp(50), 1f))
        box.addView(spacedRow(row))
        return box
    }

    private fun hybridWeekLines(): List<String> = listOf(
        "Segunda: corrida forte 5 km, sem musculacao.",
        "Terca: Treino A + corrida leve 15-25 min.",
        "Quarta: descanso ou mobilidade.",
        "Quinta: Treino B pernas/core + corrida leve 10-15 min.",
        "Sexta: descanso.",
        "Sabado: Treino C costas/biceps + corrida ritmo.",
        "Domingo: corrida longa leve 35-50 min.",
    )

    private fun runningWeekLines(): List<String> = listOf(
        "Segunda: 10 min leve + 6x400 m forte + 8-10 min leve.",
        "Terca: depois do Treino A, 15-25 min bem confortavel.",
        "Quinta: depois de pernas, 10-15 min leve para soltar.",
        "Sabado: 10 min leve + 15-20 min ritmo RPE 7 + 5 min leve.",
        "Domingo: 35-50 min leve para construir base aerobica.",
        "Quarta e sexta: descanso, mobilidade ou caminhada leve.",
    )

    private fun renderWorkout(root: LinearLayout) {
        val plan = currentPlan()
        root.addView(workoutHeader(plan))
        root.addView(sectionTitle("Planos"))
        root.addView(planSelector())
        val completion = currentStrengthSessionCompletion()
        if (completion != null) {
            root.addView(completedStrengthWorkoutPanel(completion))
            root.addView(sectionTitle("Exercicios do treino"))
            val completedList = exerciseList()
            if (requestedSection == "workout_exercises") pageScrollTarget = completedList
            root.addView(completedList)
            return
        }
        ensureStrengthSessionStarted()
        root.addView(workoutProgressPanel())
        val register = registerPanel()
        if (requestedSection == "workout_current") pageScrollTarget = register
        root.addView(register)
        root.addView(smartStrengthCoachPanel())
        val deferredSection = requestedSection
        val deferred = LinearLayout(this)
        deferred.orientation = LinearLayout.VERTICAL
        root.addView(deferred)
        root.postDelayed({
            if (!root.isAttachedToWindow || currentTab != "workout") return@postDelayed
            deferred.addView(sectionTitle("Exercicios do treino"))
            val exerciseList = exerciseList()
            deferred.addView(exerciseList)
            deferred.addView(sectionTitle("Apoio da sessao"))
            val support = gymModePanel()
            deferred.addView(support)

            val target = when (deferredSection) {
                "workout_media" -> register
                "workout_exercises" -> exerciseList
                "workout_support" -> support
                else -> null
            }
            target?.let { view ->
                view.postDelayed({ scrollPageToTarget(root, view) }, 80L)
            }
        }, 120L)
    }

    private fun workoutHeader(plan: WorkoutPlan): View {
        val totalSets = plan.exercises.sumOf { exercise -> defaultSetCountFor(exercise.target) }
        val averageRest = plan.exercises
            .map { exercise -> restSecondsForPlanExercise(exercise) }
            .filter { seconds -> seconds > 0 }
            .average()
            .takeIf { value -> !value.isNaN() }
            ?.roundToInt()
            ?: 0
        val completed = completedExerciseCount()

        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.setPadding(0, dp(Mo2Spacing.Sm), 0, dp(Mo2Spacing.Sm))
        box.addView(label("TREINO DE HOJE", green, 13f, true))

        val titleRow = LinearLayout(this)
        titleRow.orientation = LinearLayout.HORIZONTAL
        titleRow.gravity = Gravity.CENTER_VERTICAL
        val title = LinearLayout(this)
        title.orientation = LinearLayout.VERTICAL
        title.addView(label(plan.title, white, 28f, true))
        title.addView(label(plan.focus, muted, 15f, false))
        titleRow.addView(title, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        titleRow.addView(statusChip(if (completed >= plan.exercises.size) "CONCLUIDO" else "EM ANDAMENTO"))
        box.addView(titleRow)
        box.addView(label(
            plan.exercises.size.toString() + " exercicios | " + totalSets + " series | descanso medio " + averageRest + "s",
            muted,
            14f,
            false,
        ))
        return box
    }

    private fun completedStrengthWorkoutPanel(completion: JSONObject): View {
        val completed = jsonStringList(completion.optJSONArray("completed_exercises"))
        val partial = jsonStringList(completion.optJSONArray("partial_exercises"))
        val skipped = jsonStringList(completion.optJSONArray("skipped_exercises"))
        val box = card(surface3)
        box.orientation = LinearLayout.VERTICAL
        box.addView(label("TREINO CONCLUIDO", green, 13f, true))
        box.addView(label(completion.optString("plan_title", currentPlan().title), white, 24f, true))
        box.addView(label(
            completion.optInt("completed_sets").toString() + " series registradas | " +
                completed.size + " completos | " + partial.size + " parciais | " + skipped.size + " pulados",
            muted,
            14f,
            false,
        ))
        if (partial.isNotEmpty()) box.addView(label("Parciais: " + partial.joinToString(", "), amber, 14f, true))
        if (skipped.isNotEmpty()) box.addView(label("Pulados: " + skipped.joinToString(", "), muted, 14f, false))
        if (partial.isEmpty() && skipped.isEmpty()) {
            box.addView(label("Todos os exercicios planejados foram concluidos.", green, 14f, true))
        }
        val totalExercises = (completed.size + partial.size + skipped.size).coerceAtLeast(1)
        box.addView(dashboardProgressLine(
            "Exercicios realizados",
            completed.size.toString() + " completos de " + totalExercises,
            progressPercent(completed.size, totalExercises),
            green,
        ))

        val actions = LinearLayout(this)
        actions.orientation = LinearLayout.HORIZONTAL
        val history = actionButton("Ver historico", green, bg)
        history.setOnClickListener { switchTab("history") }
        actions.addView(history, LinearLayout.LayoutParams(0, dp(50), 1f))
        val reopen = actionButton("Reabrir treino", surface2, white)
        reopen.setOnClickListener { reopenCurrentStrengthWorkout() }
        actions.addView(reopen, LinearLayout.LayoutParams(0, dp(50), 1f))
        box.addView(spacedRow(actions))
        return box
    }

    private fun workoutProgressPanel(): View {
        val plan = currentPlan()
        val exercise = currentExercise()
        val logs = allLogs()
        var planSetsToday = 0
        var planVolumeToday = 0.0
        for (i in 0 until logs.length()) {
            val item = logs.getJSONObject(i)
            if (item.optString("day") == dayKey() && item.optString("plan") == plan.title) {
                planSetsToday += 1
                planVolumeToday += item.optDouble("load") * item.optInt("reps")
            }
        }

        val sets = plannedSetsForCurrentExercise()
        val doneSets = countDonePlannedSets(sets)
        val completedExercises = completedExerciseCount()
        val planPercent = progressPercent(completedExercises, plan.exercises.size)

        val box = card(surface3)
        box.orientation = LinearLayout.VERTICAL
        box.addView(label("SESSAO DE HOJE", green, 13f, true))
        box.addView(label(completedExercises.toString() + " de " + plan.exercises.size + " exercicios concluidos", white, 22f, true))
        box.addView(label("Agora: " + exercise.name, muted, 14f, false))
        box.addView(workoutMetricStrip(listOf(
            Pair("SERIES", planSetsToday.toString()),
            Pair("VOLUME", planVolumeToday.roundToInt().toString() + " kg"),
            Pair("ATUAL", doneSets.toString() + "/" + sets.length()),
        )))
        box.addView(dashboardProgressLine(
            "Avanco do treino",
            completedExercises.toString() + " de " + plan.exercises.size + " exercicios concluidos",
            planPercent,
            green,
        ))

        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        val previous = actionButton("Anterior", surface2, white)
        previous.setOnClickListener {
            if (selectedExerciseIndex > 0) {
                selectedExerciseIndex -= 1
                prefs.edit().putInt("selected_exercise", selectedExerciseIndex).apply()
                renderWorkoutInPlace()
            }
        }
        row.addView(previous, LinearLayout.LayoutParams(0, dp(50), 1f))
        val next = actionButton("Proximo", green, bg)
        next.setOnClickListener {
            if (selectedExerciseIndex < plan.exercises.lastIndex) {
                selectedExerciseIndex += 1
                prefs.edit().putInt("selected_exercise", selectedExerciseIndex).apply()
                renderWorkoutInPlace()
            }
        }
        row.addView(next, LinearLayout.LayoutParams(0, dp(50), 1f))
        box.addView(spacedRow(row))
        return box
    }

    private fun workoutMetricStrip(metrics: List<Pair<String, String>>): View {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.setPadding(0, dp(Mo2Spacing.Md), 0, 0)
        metrics.forEachIndexed { index, metric ->
            val item = LinearLayout(this)
            item.orientation = LinearLayout.VERTICAL
            item.setPadding(if (index == 0) 0 else dp(Mo2Spacing.Md), 0, dp(Mo2Spacing.Sm), 0)
            item.addView(label(metric.first, muted, 11f, true))
            item.addView(label(metric.second, white, 17f, true))
            row.addView(item, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        }
        return row
    }

    private fun gymModePanel(): View {
        val exercise = currentExercise()
        val matched = catalogMatchForWorkoutExercise(exercise.name)
        val sets = plannedSetsForCurrentExercise()
        val doneCount = countDonePlannedSets(sets)
        val pendingLine = nextPendingSetLine(sets)
        val nextExercise = nextExerciseName()
        val restLine = if (restTimerRemainingSeconds() > 0L) {
            "Descanso ativo: " + restTimerDisplay()
        } else {
            "Descanso pronto: " + formatDuration(restSecondsFor(exercise.name).toLong())
        }
        val equipmentLine = matched?.equipment
            ?.takeIf { it.isNotBlank() }
            ?.let { equipment -> if (isEquipmentUnavailable(equipment)) "Equipamento indisponivel: " + equipment else "Equipamento: " + equipment }
            ?: "Equipamento: variavel"

        val box = card(surface3)
        box.orientation = LinearLayout.VERTICAL
        box.addView(label("MODO ACADEMIA", green, 13f, true))
        box.addView(label("Agora: " + exercise.name, white, 21f, true))
        box.addView(label(doneCount.toString() + "/" + sets.length() + " series | " + pendingLine, white, 15f, true))
        box.addView(label(restLine + " | Proximo: " + nextExercise, muted, 14f, false))
        box.addView(label(equipmentLine, if (matched?.equipment?.let { isEquipmentUnavailable(it) } == true) danger else muted, 13f, false))
        box.addView(label("Tela mantida ativa enquanto voce estiver na aba Treino.", muted, 13f, false))

        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        val load = actionButton("Carga", green, bg)
        load.setOnClickListener { applySmartLoadToPendingSets() }
        row.addView(load, LinearLayout.LayoutParams(0, dp(50), 1f))

        val rest = actionButton("Descanso", surface2, green)
        rest.setOnClickListener { startRestTimerForCurrentExercise() }
        row.addView(rest, LinearLayout.LayoutParams(0, dp(50), 1f))
        val swap = actionButton("Trocar", surface2, amber)
        swap.setOnClickListener { matched?.let { showEquipmentUnavailableDialog(it) } ?: swapCurrentExerciseForRecommended() }
        row.addView(swap, LinearLayout.LayoutParams(0, dp(50), 1f))
        box.addView(spacedRow(row))
        return box
    }

    private fun smartStrengthCoachPanel(): View {
        val exercise = currentExercise()
        val adjustment = strengthAdjustmentFor(exercise)
        val sets = plannedSetsForCurrentExercise()
        val doneCount = countDonePlannedSets(sets)

        val box = card(surface3)
        box.orientation = LinearLayout.VERTICAL
        box.addView(label("AJUSTE INTELIGENTE", green, 13f, true))
        box.addView(label("Proxima carga: " + formatLoad(adjustment.nextLoad), white, 24f, true))
        box.addView(label(adjustment.loadReason, muted, 14f, false))
        box.addView(label("Volume sugerido: " + adjustment.suggestedSetCount + " series", white, 18f, true))
        box.addView(label(doneCount.toString() + "/" + sets.length() + " series feitas agora. " + adjustment.volumeReason, muted, 14f, false))
        val availableWeights = availableWeightsFor(exercise, adjustment.nextLoad)
        val weightMode = if (customAvailableWeightsFor(exercise) == null) "Sugestoes padrao" else "Pesos personalizados"
        box.addView(label(weightMode + ": " + availableWeightSummary(availableWeights), muted, 13f, false))

        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        val applyLoad = actionButton("Aplicar carga", green, bg)
        applyLoad.setOnClickListener { applySmartLoadToPendingSets() }
        row.addView(applyLoad, LinearLayout.LayoutParams(0, dp(50), 1f))

        val applyVolume = actionButton("Ajustar series", surface2, green)
        applyVolume.setOnClickListener { applySmartVolumeToCurrentExercise() }
        row.addView(applyVolume, LinearLayout.LayoutParams(0, dp(50), 1f))
        box.addView(spacedRow(row))

        val available = actionButton("Pesos disponiveis", surface2, white)
        available.setOnClickListener { showAvailableWeightsDialog(exercise) }
        box.addView(buttonParams(available))
        return box
    }

    private fun showAvailableWeightsDialog(exercise: ExercisePlan) {
        val adjustment = strengthAdjustmentFor(exercise)
        val weights = availableWeightsFor(exercise, adjustment.nextLoad)
        val customized = customAvailableWeightsFor(exercise) != null
        val content = LinearLayout(this)
        content.orientation = LinearLayout.VERTICAL
        content.setPadding(dp(18), dp(18), dp(18), dp(12))
        content.background = rounded(surface, dp(Mo2Radius.Modal), border)
        content.addView(label("PESOS DISPONIVEIS", green, 13f, true))
        content.addView(label(exercise.name, white, 22f, true))
        content.addView(label(if (customized) "LISTA PERSONALIZADA" else "SUGESTOES PADRAO", if (customized) amber else muted, 12f, true))

        lateinit var dialog: AlertDialog
        var reopening = false
        weights.forEach { weight ->
            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            row.gravity = Gravity.CENTER_VERTICAL
            row.setPadding(dp(Mo2Spacing.Md), dp(Mo2Spacing.Xs), dp(Mo2Spacing.Xs), dp(Mo2Spacing.Xs))
            row.background = rounded(surface2, dp(Mo2Radius.Md), border)
            row.addView(label(formatLoad(weight), white, 16f, true), LinearLayout.LayoutParams(0, dp(48), 1f))
            val remove = actionButton("x", surface2, danger)
            remove.textSize = accessibleTextSize(22f)
            remove.contentDescription = "Remover " + formatLoad(weight)
            remove.setOnClickListener {
                saveAvailableWeights(exercise, weights.filterNot { value -> kotlin.math.abs(value - weight) < 0.001 })
                reopening = true
                dialog.dismiss()
                showAvailableWeightsDialog(exercise)
            }
            row.addView(remove, LinearLayout.LayoutParams(dp(52), dp(48)))
            val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            params.setMargins(0, dp(Mo2Spacing.Sm), 0, 0)
            content.addView(row, params)
        }
        if (weights.isEmpty()) {
            content.addView(label("Nenhum peso configurado", muted, 14f, false))
        }

        val newWeight = input("Peso em kg (ex.: 17,5)", "")
        newWeight.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        content.addView(newWeight)
        val add = actionButton("Adicionar peso", green, bg)
        add.setOnClickListener {
            val value = newWeight.textValue().replace(',', '.').toDoubleOrNull()
            if (value == null || value <= 0.0) {
                Toast.makeText(this, "Informe um peso maior que zero.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            saveAvailableWeights(exercise, weights + value)
            reopening = true
            dialog.dismiss()
            showAvailableWeightsDialog(exercise)
        }
        content.addView(buttonParams(add))

        val restore = actionButton("Restaurar sugestoes padrao", surface2, green)
        restore.setOnClickListener {
            clearAvailableWeights(exercise)
            reopening = true
            dialog.dismiss()
            showAvailableWeightsDialog(exercise)
        }
        content.addView(buttonParams(restore))
        val close = actionButton("Fechar", surface2, white)
        close.setOnClickListener { dialog.dismiss() }
        content.addView(buttonParams(close))

        val scroll = ScrollView(this)
        scroll.isFillViewport = true
        scroll.isVerticalScrollBarEnabled = true
        scroll.addView(content)
        dialog = AlertDialog.Builder(this)
            .setView(scroll)
            .create()
        dialog.setOnShowListener {
            content.startAnimation(smoothPopupAnimation())
            dialog.window?.setLayout(
                (resources.displayMetrics.widthPixels * 0.94f).roundToInt(),
                (resources.displayMetrics.heightPixels * 0.82f).roundToInt(),
            )
        }
        dialog.setOnDismissListener {
            if (!reopening && currentTab == "workout") renderWorkoutInPlace()
        }
        dialog.show()
    }

    private fun restTimerPanel(): View {
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.setPadding(0, dp(Mo2Spacing.Lg), 0, dp(Mo2Spacing.Sm))
        val active = restTimerRemainingSeconds() > 0L
        val heading = LinearLayout(this)
        heading.orientation = LinearLayout.HORIZONTAL
        heading.gravity = Gravity.CENTER_VERTICAL
        heading.addView(label("DESCANSO", green, 13f, true), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        heading.addView(label(if (active) "ATIVO" else "PRONTO", if (active) green else muted, 11f, true))
        box.addView(heading)

        val controls = LinearLayout(this)
        controls.orientation = LinearLayout.HORIZONTAL
        controls.gravity = Gravity.CENTER_VERTICAL
        val reduce = actionButton("-30", surface2, white)
        reduce.contentDescription = "Reduzir descanso em 30 segundos"
        reduce.isEnabled = active
        reduce.setOnClickListener { adjustRestTime(-30) }
        controls.addView(reduce, LinearLayout.LayoutParams(dp(64), dp(52)))

        val timer = label(restTimerDisplay(), white, 34f, true)
        timer.gravity = Gravity.CENTER
        restTimerText = timer
        val timerParams = LinearLayout.LayoutParams(0, dp(58), 1f)
        timerParams.setMargins(dp(Mo2Spacing.Sm), 0, dp(Mo2Spacing.Sm), 0)
        controls.addView(timer, timerParams)

        val add = actionButton("+30", surface2, white)
        add.contentDescription = "Adicionar 30 segundos ao descanso"
        add.setOnClickListener { adjustRestTime(30) }
        controls.addView(add, LinearLayout.LayoutParams(dp(64), dp(52)))
        box.addView(controls)
        box.addView(label(restTimerSubtitle(), muted, 14f, false))
        if (active) restTimerHandler.post(restTimerRunnable)
        else notifyRestTimerFinishedIfNeeded()

        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        val start = actionButton(if (active) "Reiniciar" else "Iniciar", green, bg)
        start.setOnClickListener { startRestTimerForCurrentExercise() }
        row.addView(start, LinearLayout.LayoutParams(0, dp(50), 1f))
        val stop = actionButton("Parar", surface2, danger)
        stop.isEnabled = active
        stop.setOnClickListener { clearRestTimer() }
        row.addView(stop, LinearLayout.LayoutParams(0, dp(50), 1f))
        box.addView(spacedRow(row))
        return box
    }

    private fun renderRunning(root: LinearLayout) {
        updateActiveRunProgress()
        activeRunningWorkout()?.let { workout ->
            root.addView(activeRunPanel(workout))
            return
        }

        root.addView(runningThisWeekPanel())
        root.addView(runningFullPlanButton())
        if (prefs.getBoolean("running_full_plan_open", false)) root.addView(runningFullPlanPanel())
        val treadmillPanel = treadmillModePanel()
        root.addView(treadmillPanel)
        if (requestedSection == "treadmill") pageScrollTarget = treadmillPanel
        val coachPanel = runningSmartCoachPanel()
        root.addView(coachPanel)
        if (requestedSection == "coach") pageScrollTarget = coachPanel
        val voicePanel = runningVoiceSettingsPanel()
        root.addView(voicePanel)
        if (requestedSection == "voice") pageScrollTarget = voicePanel
        root.addView(runningHistoryPanel())
    }

    private fun runningSmartCoachPanel(): View {
        val adjustment = smartRunningAdjustment()
        val currentSpeedOffset = prefs.getString("running_speed_offset", "0.0")?.toDoubleOrNull() ?: 0.0
        val currentDistanceScale = prefs.getString("running_distance_scale", "1.0")?.toDoubleOrNull() ?: 1.0
        val box = card(surface3)
        box.orientation = LinearLayout.VERTICAL
        box.addView(label("COACH DE RITMO", Mo2Colors.Running, 13f, true))
        box.addView(label(adjustment.headline, white, 22f, true))
        box.addView(label(adjustment.reason, muted, 14f, false))
        box.addView(label("Ajuste atual", muted, 12f, true))
        box.addView(label(signedSpeedOffset(currentSpeedOffset) + " | distancia x" + String.format(Locale.US, "%.2f", currentDistanceScale), white, 15f, true))
        box.addView(label("Sugestao: " + signedSpeedOffset(adjustment.speedOffset) + " | distancia x" + String.format(Locale.US, "%.2f", adjustment.distanceScale), Mo2Colors.Running, 15f, true))

        val apply = actionButton("Aplicar ajuste sugerido", green, bg)
        apply.setOnClickListener { applySmartRunningAdjustment(adjustment) }
        box.addView(buttonParams(apply))
        return box
    }

    private fun runningFiveKmForecastPanel(): View {
        val forecast = runningFiveKmForecast()
        val paceSeconds = (forecast.predictedSeconds.toDouble() / 5.0).roundToInt().toLong()
        val predictedSpeed = 3600.0 / paceSeconds.coerceAtLeast(1L).toDouble()
        val goalDelta = forecast.predictedSeconds - forecast.goalSeconds
        val comparison = when {
            kotlin.math.abs(goalDelta) <= 10L -> Pair("Projecao alinhada com a meta", green)
            goalDelta > 0L -> Pair("Faltam " + formatDuration(goalDelta) + " para a meta", amber)
            else -> Pair("Projecao " + formatDuration(-goalDelta) + " mais rapida que a meta", green)
        }
        val trend = forecast.trendSeconds?.let { seconds ->
            when {
                seconds <= -10L -> "Tendencia: melhora de " + formatDuration(-seconds)
                seconds >= 10L -> "Tendencia: piora de " + formatDuration(seconds)
                else -> "Tendencia: estavel"
            }
        } ?: "Tendencia: aguardando mais corridas validas"

        val box = card(surface3)
        box.orientation = LinearLayout.VERTICAL
        box.addView(label("PREVISAO 5 KM", Mo2Colors.Running, 13f, true))
        box.addView(label(formatDuration(forecast.predictedSeconds), white, 34f, true))
        box.addView(label(forecast.source, muted, 13f, false))

        val metrics = LinearLayout(this)
        metrics.orientation = LinearLayout.HORIZONTAL
        metrics.addView(compactMetric("Pace", formatPaceSeconds(paceSeconds)), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        metrics.addView(compactMetric("Velocidade", String.format(Locale("pt", "BR"), "%.1f", predictedSpeed)), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        metrics.addView(compactMetric("Confianca", forecast.confidence), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        box.addView(spacedRow(metrics))

        box.addView(label("Meta: " + formatDuration(forecast.goalSeconds), white, 14f, true))
        box.addView(label(comparison.first, comparison.second, 14f, true))
        box.addView(label(trend, if ((forecast.trendSeconds ?: 0L) <= 0L) green else amber, 13f, false))

        val goal = actionButton("Ajustar meta de 5 km", surface2, Mo2Colors.Running)
        goal.setOnClickListener { showRunningGoalDialog() }
        box.addView(buttonParams(goal))
        return box
    }

    private fun showRunningGoalDialog() {
        val currentGoal = prefs.getLong("running_goal_5k_seconds", 1800L).coerceIn(900L, 7200L)
        val content = LinearLayout(this)
        content.orientation = LinearLayout.VERTICAL
        content.setPadding(dp(14), dp(14), dp(14), dp(8))
        content.addView(label("META DE 5 KM", Mo2Colors.Running, 13f, true))
        content.addView(label("Tempo alvo", white, 23f, true))
        val target = input("mm:ss ou hh:mm:ss", formatDuration(currentGoal))
        content.addView(target)

        val dialog = AlertDialog.Builder(this)
            .setView(content)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Salvar", null)
            .create()
        dialog.setOnShowListener {
            styleHistoryDialog(dialog, content)
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val seconds = parseHistoryDurationSeconds(target.textValue())
                if (seconds !in 900L..7200L) {
                    Toast.makeText(this, "Defina uma meta entre 15 minutos e 2 horas.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                prefs.edit().putLong("running_goal_5k_seconds", seconds).apply()
                dialog.dismiss()
                render()
            }
        }
        dialog.show()
    }

    private fun runningVoiceSettingsPanel(): View {
        val box = card(surface3)
        box.orientation = LinearLayout.VERTICAL
        box.addView(label("COMANDOS DE VOZ", Mo2Colors.Running, 13f, true))
        box.addView(label("Coach na esteira", white, 22f, true))

        val tenSecondCue = runningVoiceCheckBox(
            "Aviso adicional aos 10 segundos",
            prefs.getBoolean("running_voice_10_seconds", true),
        ) { checked ->
            prefs.edit().putBoolean("running_voice_10_seconds", checked).apply()
        }
        val enabled = runningVoiceCheckBox(
            "Avisos de voz durante a corrida",
            isRunningVoiceEnabled(),
        ) { checked ->
            prefs.edit().putBoolean("running_voice_enabled", checked).apply()
            tenSecondCue.isEnabled = checked
            tenSecondCue.alpha = if (checked) 1f else 0.45f
            if (!checked) voiceCoach?.stop()
        }
        tenSecondCue.isEnabled = enabled.isChecked
        tenSecondCue.alpha = if (enabled.isChecked) 1f else 0.45f
        box.addView(enabled)
        box.addView(tenSecondCue)

        val test = actionButton("Testar voz", surface2, Mo2Colors.Running)
        test.setOnClickListener {
            when {
                !isRunningVoiceEnabled() -> Toast.makeText(this, "Ative os avisos de voz primeiro.", Toast.LENGTH_SHORT).show()
                !voiceCoachReady -> Toast.makeText(this, "A voz do Android ainda nao esta pronta.", Toast.LENGTH_SHORT).show()
                else -> speakRunCue("Comandos de voz ativos. Bom treino.", flush = true)
            }
        }
        box.addView(buttonParams(test))
        return box
    }

    private fun runningVoiceCheckBox(
        text: String,
        checked: Boolean,
        onChanged: (Boolean) -> Unit,
    ): CheckBox {
        val checkBox = CheckBox(this)
        checkBox.text = text
        checkBox.isChecked = checked
        checkBox.setTextColor(white)
        checkBox.textSize = 15f
        checkBox.gravity = Gravity.CENTER_VERTICAL
        checkBox.minHeight = dp(48)
        checkBox.buttonTintList = ColorStateList(
            arrayOf(
                intArrayOf(-android.R.attr.state_enabled),
                intArrayOf(android.R.attr.state_checked),
                intArrayOf(),
            ),
            intArrayOf(muted, Mo2Colors.Running, border),
        )
        checkBox.setOnCheckedChangeListener { _, isChecked -> onChanged(isChecked) }
        return checkBox
    }

    private fun treadmillModePanel(): View {
        val workouts = currentRunningWeekWorkouts()
        val workout = activeRunningWorkout()
            ?: workouts.firstOrNull { it.id == selectedRunId }
            ?: todayRunningWorkout()
            ?: workouts.first()
        val box = card(surface3)
        box.orientation = LinearLayout.VERTICAL
        box.addView(label("PRONTO PARA A ESTEIRA", Mo2Colors.Running, 13f, true))
        box.addView(label(workout.dayName + " | " + workout.title, white, 22f, true))
        box.addView(label(formatKm(totalRunDistance(workout)) + " | " + formatDuration(estimatedWorkoutSeconds(workout)), muted, 14f, false))
        box.addView(label("Plano de velocidade", muted, 12f, true))
        box.addView(label(treadmillSpeedPlan(workout), white, 15f, true))
        box.addView(label("Use inclinacao de 1% se estiver confortavel. A tela fica ligada durante toda a sessao.", muted, 13f, false))

        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        val select = actionButton("Ver detalhes", surface2, Mo2Colors.Running)
        select.setOnClickListener {
            selectedRunId = workout.id
            prefs.edit().putString("selected_run_id", workout.id).apply()
            render()
        }
        row.addView(select, LinearLayout.LayoutParams(0, dp(50), 1f))

        val runningNow = activeRunningWorkout()?.id == workout.id
        val start = actionButton(if (runningNow) "Em andamento" else "Iniciar corrida", green, bg)
        start.setOnClickListener {
            if (runningNow) Toast.makeText(this, "Corrida ja esta em andamento.", Toast.LENGTH_SHORT).show()
            else startRunningWorkout(workout)
        }
        row.addView(start, LinearLayout.LayoutParams(0, dp(50), 1f))
        box.addView(spacedRow(row))
        return box
    }

    private fun runningThisWeekPanel(): View {
        val week = currentRunningPlanWeek()
        val workouts = currentRunningWeekWorkouts()
        val selected = workouts.firstOrNull { it.id == selectedRunId } ?: todayRunningWorkout() ?: workouts.first()
        selectedRunId = selected.id
        val completed = workouts.count { isRunWorkoutCompleted(it) }
        val distanceDone = completedRunningDistance(workouts)

        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL

        val overview = card(surface3)
        overview.orientation = LinearLayout.VERTICAL
        overview.addView(label("ESSA SEMANA", green, 13f, true))
        overview.addView(label("Semana " + week + " de 6", white, 28f, true))
        overview.addView(label("Meta: completar os 5 treinos e evoluir com controle ate os 5 km.", muted, 14f, false))

        val metrics = LinearLayout(this)
        metrics.orientation = LinearLayout.HORIZONTAL
        metrics.addView(compactMetric("Treinos", completed.toString() + "/" + workouts.size), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        metrics.addView(compactMetric("Distancia", formatKm(distanceDone)), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        metrics.addView(compactMetric("Faltam", (workouts.size - completed).coerceAtLeast(0).toString()), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        overview.addView(spacedRow(metrics))

        val progress = Mo2Components.progressBar(this, progressPercent(completed, workouts.size), Mo2Colors.Running)
        val progressParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(8))
        progressParams.setMargins(0, dp(14), 0, 0)
        overview.addView(progress, progressParams)
        box.addView(overview)
        val forecastPanel = runningFiveKmForecastPanel()
        box.addView(forecastPanel)
        if (requestedSection == "forecast") pageScrollTarget = forecastPanel

        val listHeader = LinearLayout(this)
        listHeader.orientation = LinearLayout.VERTICAL
        listHeader.setPadding(0, dp(12), 0, dp(4))
        listHeader.addView(label("TREINOS DA SEMANA", white, 18f, true))
        listHeader.addView(label("Toque para abrir detalhes ou registre um treino feito sem o app.", muted, 13f, false))
        box.addView(listHeader)

        workouts.forEach { workout ->
            box.addView(runningWeekWorkoutItem(workout, workout.id == selected.id))
        }
        return box
    }

    private fun runningWeekWorkoutItem(workout: RunningWorkout, expanded: Boolean): View {
        val completed = isRunWorkoutCompleted(workout)
        val active = activeRunningWorkout()?.id == workout.id
        val scheduledDay = scheduledDayFor(workout)
        val overdue = !completed && scheduledDay < dayKey()
        val item = card(if (expanded) surface3 else surface)
        item.orientation = LinearLayout.VERTICAL
        item.setOnClickListener {
            selectedRunId = workout.id
            prefs.edit().putString("selected_run_id", workout.id).apply()
            render()
        }

        val top = LinearLayout(this)
        top.orientation = LinearLayout.HORIZONTAL
        top.gravity = Gravity.CENTER_VERTICAL
        top.addView(runningWorkoutCheck(completed, active), LinearLayout.LayoutParams(dp(44), dp(44)))
        val titleBox = LinearLayout(this)
        titleBox.orientation = LinearLayout.VERTICAL
        titleBox.addView(label(workout.dayName + " | " + workout.title, if (completed) green else white, 17f, true))
        titleBox.addView(label(formatKm(totalRunDistance(workout)) + " | " + formatDuration(estimatedWorkoutSeconds(workout)) + " | " + runningStageSummary(workout), muted, 13f, false))
        titleBox.addView(label("Agendado: " + formatDayForDisplay(scheduledDay) + if (overdue) " | atrasado" else "", if (overdue) amber else muted, 12f, overdue))
        top.addView(titleBox, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        top.addView(Mo2Components.badge(this, if (completed) "Feito" else if (active) "Rodando" else if (expanded) "Aberto" else "Pendente", completed || active))
        item.addView(top)

        if (expanded) {
            item.addView(label(workout.focus, Mo2Colors.Running, 14f, true))
            item.addView(label(workout.description, white, 14f, false))
            workout.stages.forEachIndexed { index, stage ->
                val stageRow = LinearLayout(this)
                stageRow.orientation = LinearLayout.VERTICAL
                stageRow.setPadding(dp(12), dp(10), dp(12), dp(10))
                stageRow.background = rounded(surface2, dp(Mo2Radius.Md), border)
                stageRow.addView(label("ETAPA " + (index + 1) + " | " + stage.title, white, 13f, true))
                stageRow.addView(label(formatKm(stage.distanceKm) + " a " + formatSpeed(stage.speedKmh) + " | " + stage.note, muted, 12f, false))
                val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                params.setMargins(0, dp(8), 0, 0)
                item.addView(stageRow, params)
            }

            val scheduleRow = LinearLayout(this)
            scheduleRow.orientation = LinearLayout.HORIZONTAL
            val reschedule = actionButton("Reagendar", surface2, Mo2Colors.Running)
            reschedule.setOnClickListener { showRescheduleRunningWorkoutDialog(workout) }
            scheduleRow.addView(reschedule, LinearLayout.LayoutParams(0, dp(48), 1f))
            val recover = actionButton(if (scheduledDay == dayKey()) "Agendado hoje" else "Recuperar hoje", surface2, if (overdue) amber else white)
            recover.isEnabled = !completed && scheduledDay != dayKey()
            recover.alpha = if (recover.isEnabled) 1f else 0.55f
            recover.setOnClickListener { rescheduleRunningWorkout(workout, dayKey()) }
            scheduleRow.addView(recover, LinearLayout.LayoutParams(0, dp(48), 1f))
            item.addView(spacedRow(scheduleRow))

            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            val start = actionButton(if (active) "Em andamento" else "Iniciar", green, bg)
            start.isEnabled = !completed
            start.alpha = if (completed) 0.45f else 1f
            start.setOnClickListener { if (!completed) startRunningWorkout(workout) }
            row.addView(start, LinearLayout.LayoutParams(0, dp(50), 1f))

            val done = actionButton(if (completed) "Concluido" else "Marcar concluido", surface2, if (completed) green else white)
            done.isEnabled = !completed
            done.alpha = if (completed) 0.65f else 1f
            done.setOnClickListener { if (!completed) saveRunCompletion(workout, true) }
            row.addView(done, LinearLayout.LayoutParams(0, dp(50), 1f))
            item.addView(spacedRow(row))
        }
        return item
    }

    private fun showRescheduleRunningWorkoutDialog(workout: RunningWorkout) {
        val content = LinearLayout(this)
        content.orientation = LinearLayout.VERTICAL
        content.setPadding(dp(14), dp(14), dp(14), dp(8))
        content.addView(label("REAGENDAR CORRIDA", Mo2Colors.Running, 13f, true))
        content.addView(label(workout.title, white, 22f, true))
        content.addView(label("Informe a nova data no formato ano-mes-dia.", muted, 14f, false))
        val day = input("yyyy-mm-dd", scheduledDayFor(workout))
        content.addView(day)

        val dialog = AlertDialog.Builder(this)
            .setView(content)
            .setNegativeButton("Cancelar", null)
            .setNeutralButton("Plano original", null)
            .setPositiveButton("Salvar", null)
            .create()
        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawable(Mo2Drawables.rounded(this, surface, Mo2Radius.Modal, border))
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(green)
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(Mo2Colors.Running)
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(muted)
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val value = day.textValue().trim()
                if (!isValidDayKey(value)) {
                    Toast.makeText(this, "Use uma data valida no formato yyyy-mm-dd.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                rescheduleRunningWorkout(workout, value)
                dialog.dismiss()
            }
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                clearRunningWorkoutSchedule(workout)
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun rescheduleRunningWorkout(workout: RunningWorkout, day: String) {
        val overrides = safeObject("running_schedule_overrides")
        overrides.put(workout.id, day)
        selectedRunId = workout.id
        prefs.edit()
            .putString("running_schedule_overrides", overrides.toString())
            .putString("selected_run_id", workout.id)
            .apply()
        Toast.makeText(this, "Corrida reagendada para " + formatDayForDisplay(day) + ".", Toast.LENGTH_SHORT).show()
        render()
    }

    private fun clearRunningWorkoutSchedule(workout: RunningWorkout) {
        val overrides = safeObject("running_schedule_overrides")
        overrides.remove(workout.id)
        prefs.edit().putString("running_schedule_overrides", overrides.toString()).apply()
        Toast.makeText(this, "Data original do plano restaurada.", Toast.LENGTH_SHORT).show()
        render()
    }

    private fun runningWorkoutCheck(completed: Boolean, active: Boolean): CheckBox {
        val check = CheckBox(this)
        check.isChecked = completed
        check.isEnabled = false
        check.alpha = if (completed || active) 1f else 0.72f
        check.contentDescription = if (completed) "Treino de corrida concluido" else "Treino de corrida pendente"
        check.buttonTintList = ColorStateList(
            arrayOf(
                intArrayOf(-android.R.attr.state_enabled, android.R.attr.state_checked),
                intArrayOf(-android.R.attr.state_enabled),
                intArrayOf(),
            ),
            intArrayOf(green, if (active) Mo2Colors.Running else border, border),
        )
        return check
    }

    private fun runningStageSummary(workout: RunningWorkout): String {
        val hardStages = workout.stages.count { stage ->
            stage.title.contains("Tiro", true) || stage.title.contains("Ritmo", true)
        }
        return workout.stages.size.toString() + " etapas" + if (hardStages > 0) " | " + hardStages + " fortes" else ""
    }

    private fun runningFullPlanButton(): View {
        val open = prefs.getBoolean("running_full_plan_open", false)
        val button = actionButton(if (open) "Ocultar planejamento completo" else "Planejamento completo", surface2, green)
        button.setOnClickListener {
            prefs.edit().putBoolean("running_full_plan_open", !open).apply()
            render()
        }
        return buttonParams(button)
    }

    private fun runningFullPlanPanel(): View {
        val box = card()
        box.orientation = LinearLayout.VERTICAL
        box.addView(label("PLANEJAMENTO COMPLETO", green, 13f, true))
        box.addView(label("6 semanas progressivas para prova de 5 km", white, 22f, true))
        runningPlan.groupBy { it.week }.forEach { entry ->
            box.addView(label("Semana " + entry.key, amber, 16f, true))
            entry.value.forEach { workout ->
                val marker = if (isRunWorkoutCompleted(workout)) "[x] " else "[ ] "
                box.addView(label(marker + formatDayForDisplay(scheduledDayFor(workout)) + " | " + workout.dayName + ": " + workout.title + " | " + formatKm(totalRunDistance(workout)) + " | " + formatDuration(estimatedWorkoutSeconds(workout)), white, 13f, false))
            }
        }
        return box
    }

    private fun runningHistoryPanel(): View {
        val history = card()
        history.orientation = LinearLayout.VERTICAL
        history.addView(label("CORRIDAS RECENTES", green, 13f, true))
        val runs = runLogs()
        if (runs.length() == 0) history.addView(label("Nenhuma corrida salva ainda.", muted, 15f, false))
        for (i in runs.length() - 1 downTo max(0, runs.length() - 6)) {
            val item = runs.getJSONObject(i)
            val title = item.optString("workout_title", "Corrida")
            val source = if (item.optBoolean("manual", false)) "manual" else "app"
            history.addView(label(item.optString("day") + " " + item.optString("time") + " | " + title + " | " + item.optDouble("distance") + " km | " + item.optString("duration") + " | " + source, white, 14f, false))
        }
        return history
    }

    private fun activeRunPanel(workout: RunningWorkout): View {
        val screen = LinearLayout(this)
        screen.orientation = LinearLayout.VERTICAL

        val header = LinearLayout(this)
        header.orientation = LinearLayout.VERTICAL
        header.setPadding(0, dp(8), 0, dp(12))
        runningSessionTitleText = label(workout.title, white, 25f, true)
        runningSessionSubtitleText = label("", muted, 14f, false)
        header.addView(runningSessionTitleText)
        header.addView(runningSessionSubtitleText)
        screen.addView(header)

        val countdownGroup = LinearLayout(this)
        countdownGroup.orientation = LinearLayout.VERTICAL
        val countdownCard = card(surface2)
        countdownCard.orientation = LinearLayout.VERTICAL
        countdownCard.gravity = Gravity.CENTER
        countdownCard.setPadding(dp(18), dp(36), dp(18), dp(36))
        countdownCard.addView(label("INICIANDO EM", muted, 13f, true))
        runningCountdownText = label("", green, 92f, true)
        runningCountdownText?.gravity = Gravity.CENTER
        countdownCard.addView(runningCountdownText)
        runningCountdownCueText = label("", muted, 14f, false)
        runningCountdownCueText?.gravity = Gravity.CENTER
        countdownCard.addView(runningCountdownCueText)
        countdownGroup.addView(countdownCard)

        val tips = card(surface3)
        tips.orientation = LinearLayout.VERTICAL
        tips.addView(label("DICAS ANTES DE INICIAR", muted, 12f, true))
        tips.addView(label("- Ajuste a esteira na velocidade inicial", white, 14f, false))
        tips.addView(label("- Confira velocidade, timer e inclinacao", white, 14f, false))
        countdownGroup.addView(tips)

        val cancel = actionButton("Cancelar treino", bg, white)
        cancel.setOnClickListener { showCancelRunningWorkoutDialog() }
        countdownGroup.addView(buttonParams(cancel))
        runningCountdownPanel = countdownGroup
        screen.addView(countdownGroup)

        val activeContent = LinearLayout(this)
        activeContent.orientation = LinearLayout.VERTICAL

        runningStageText = label("", Mo2Colors.Running, 15f, true)
        activeContent.addView(runningStageText)
        runningStageProgressText = label("", muted, 13f, false)
        activeContent.addView(runningStageProgressText)
        activeContent.addView(activeRunVoiceControls(workout))

        val timeCard = card(surface2)
        timeCard.orientation = LinearLayout.VERTICAL
        timeCard.addView(label("TEMPO RESTANTE", muted, 12f, true))
        runningRemainingText = label("", white, 48f, true)
        timeCard.addView(runningRemainingText)
        runningElapsedText = label("", muted, 13f, false)
        timeCard.addView(runningElapsedText)
        activeContent.addView(timeCard)

        val distanceCard = card(surface2)
        distanceCard.orientation = LinearLayout.VERTICAL
        distanceCard.addView(label("DISTANCIA RESTANTE", muted, 12f, true))
        runningDistanceText = label("", white, 32f, true)
        distanceCard.addView(runningDistanceText)
        activeContent.addView(distanceCard)

        val paceCard = card(surface2)
        paceCard.orientation = LinearLayout.VERTICAL
        paceCard.addView(label("PACE ALVO", muted, 12f, true))
        runningPaceText = label("", white, 24f, true)
        paceCard.addView(runningPaceText)
        activeContent.addView(paceCard)

        val speedCard = card(surface2)
        speedCard.orientation = LinearLayout.VERTICAL
        speedCard.addView(label("VELOCIDADE", muted, 12f, true))
        val speedRow = LinearLayout(this)
        speedRow.orientation = LinearLayout.HORIZONTAL
        speedRow.gravity = Gravity.CENTER_VERTICAL
        val minus = actionButton("-", Mo2Colors.Border, white)
        minus.contentDescription = "Diminuir velocidade"
        minus.setOnClickListener { adjustActiveRunSpeed(-0.2) }
        speedRow.addView(minus, LinearLayout.LayoutParams(dp(58), dp(58)))

        val speedValue = LinearLayout(this)
        speedValue.orientation = LinearLayout.VERTICAL
        speedValue.gravity = Gravity.CENTER
        runningSpeedText = label("", white, 32f, true)
        runningSpeedText?.gravity = Gravity.CENTER
        speedValue.addView(runningSpeedText)
        val unit = label("km/h", muted, 12f, false)
        unit.gravity = Gravity.CENTER
        speedValue.addView(unit)
        speedRow.addView(speedValue, LinearLayout.LayoutParams(0, dp(70), 1f))

        val plus = actionButton("+", Mo2Colors.Border, white)
        plus.contentDescription = "Aumentar velocidade"
        plus.setOnClickListener { adjustActiveRunSpeed(0.2) }
        speedRow.addView(plus, LinearLayout.LayoutParams(dp(58), dp(58)))
        speedCard.addView(speedRow)
        activeContent.addView(speedCard)

        val nextCard = card(surface3)
        nextCard.orientation = LinearLayout.VERTICAL
        nextCard.addView(label("PROXIMA ETAPA", muted, 12f, true))
        runningNextText = label("", white, 15f, false)
        nextCard.addView(runningNextText)
        activeContent.addView(nextCard)

        runningTreadmillText = label("", amber, 14f, true)
        activeContent.addView(runningTreadmillText)

        val actions = LinearLayout(this)
        actions.orientation = LinearLayout.HORIZONTAL
        val pause = actionButton("Pausar", bg, white)
        pause.setOnClickListener { toggleActiveRunPause() }
        runningPauseButton = pause
        actions.addView(pause, LinearLayout.LayoutParams(0, dp(52), 1f))
        val finishWorkout = actionButton("Concluir", green, bg)
        finishWorkout.setOnClickListener { finishActiveRunningWorkout(workout) }
        actions.addView(finishWorkout, LinearLayout.LayoutParams(0, dp(52), 1f))
        activeContent.addView(spacedRow(actions))

        val finishStage = actionButton("Finalizar etapa atual", surface2, white)
        finishStage.setOnClickListener { finishCurrentRunStage() }
        activeContent.addView(buttonParams(finishStage))

        runningActiveContent = activeContent
        screen.addView(activeContent)

        refreshRunningSessionViews()
        runningSessionHandler.post(runningSessionRunnable)
        return screen
    }

    private fun activeRunVoiceControls(workout: RunningWorkout): View {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        val enabled = runningVoiceCheckBox("Voz", isRunningVoiceEnabled()) { checked ->
            prefs.edit().putBoolean("running_voice_enabled", checked).apply()
            if (!checked) voiceCoach?.stop()
        }
        row.addView(enabled, LinearLayout.LayoutParams(0, dp(50), 1f))
        val repeat = actionButton("Repetir instrucao", surface2, Mo2Colors.Running)
        repeat.textSize = 14f
        repeat.isSingleLine = true
        repeat.setPadding(dp(4), 0, dp(4), 0)
        repeat.setOnClickListener { repeatCurrentRunCue(workout) }
        row.addView(repeat, LinearLayout.LayoutParams(0, dp(50), 1f))
        return spacedRow(row)
    }

    private fun refreshRunningSessionViews() {
        val workout = activeRunningWorkout() ?: return
        val countdown = activeRunCountdownSeconds()
        if (countdown > 0L) {
            runningCountdownPanel?.visibility = View.VISIBLE
            runningActiveContent?.visibility = View.GONE
            runningSessionTitleText?.text = "Preparar corrida"
            runningSessionSubtitleText?.text = workout.title
            runningCountdownText?.text = countdown.toString()
            runningCountdownCueText?.text = "Primeira etapa: " + stageCue(workout.stages.firstOrNull())
            return
        }

        val stageIndex = prefs.getInt("running_active_stage", 0).coerceIn(workout.stages.indices)
        val stage = workout.stages[stageIndex]
        val remainingKm = activeRunStageRemainingKm(workout)
        val completedKm = (stage.distanceKm - remainingKm).coerceIn(0.0, stage.distanceKm)
        val stagePercent = if (stage.distanceKm <= 0.0) 0 else ((completedKm / stage.distanceKm) * 100.0).roundToInt().coerceIn(0, 100)
        val speed = currentActiveRunSpeed(workout)
        val remainingSeconds = if (speed <= 0.0) 0L else ((remainingKm / speed) * 3600.0).roundToInt().toLong()
        val paused = isActiveRunPaused()

        runningCountdownPanel?.visibility = View.GONE
        runningActiveContent?.visibility = View.VISIBLE
        runningSessionTitleText?.text = workout.title
        runningSessionSubtitleText?.text = "Etapa " + (stageIndex + 1) + " de " + workout.stages.size + " | Esteira"
        runningStageText?.text = (if (paused) "PAUSADO | " else "ETAPA ATUAL | ") + stage.title
        runningStageProgressText?.text = "Avanco da etapa: " + stagePercent + "% | feito " + formatKm(completedKm) + " de " + formatKm(stage.distanceKm)
        runningRemainingText?.text = formatDuration(remainingSeconds)
        runningDistanceText?.text = formatKm(remainingKm)
        runningPaceText?.text = formatPaceForSpeed(speed)
        runningElapsedText?.text = "Tempo de treino: " + formatDuration(activeRunElapsedSeconds())
        runningSpeedText?.text = String.format(Locale("pt", "BR"), "%.1f", speed)
        runningTreadmillText?.text = if (paused) {
            "Sessao pausada. A distancia nao esta avancando."
        } else {
            "Esteira agora: " + formatSpeed(speed) + " | tela mantida ativa"
        }
        runningPauseButton?.text = if (paused) "Continuar" else "Pausar"
        runningPauseButton?.setTextColor(if (paused) bg else white)
        runningPauseButton?.background = Mo2Drawables.pressed(
            this,
            if (paused) green else bg,
            surface2,
            Mo2Radius.Md,
            if (paused) green else border,
        )
        runningNextText?.text = if (stageIndex < workout.stages.lastIndex) {
            stageCue(workout.stages[stageIndex + 1])
        } else {
            "Ultima etapa do treino."
        }
        if (!paused) announceRunTransitionIfNeeded(workout, stageIndex, remainingSeconds)
    }

    private fun renderPlanEditor(root: LinearLayout) {
        root.addView(heroCard("Editor local", "Plano pessoal", "Ajuste musculacao e corrida direto no celular. Tudo fica salvo localmente."))
        root.addView(planEditorSummary())
        root.addView(planEditorSelector())
        root.addView(planDetailsEditor())
        root.addView(exerciseDetailsEditor())
        root.addView(runningPlanEditor())
        root.addView(planEditorResetPanel())
    }

    private fun planEditorSummary(): View {
        val box = card(surface3)
        box.orientation = LinearLayout.VERTICAL
        val custom = prefs.contains("custom_workout_plans")
        box.addView(label("STATUS DO PLANO", green, 13f, true))
        box.addView(label(if (custom) "Plano personalizado ativo" else "Plano padrao ativo", white, 23f, true))
        box.addView(label(plans.size.toString() + " treinos de musculacao | " + runningPlan.size + " corridas planejadas", muted, 15f, false))
        box.addView(label("Edicoes feitas aqui aparecem imediatamente na aba Treino e nos atalhos da Home.", muted, 14f, false))
        return box
    }

    private fun planEditorSelector(): View {
        val box = card()
        box.orientation = LinearLayout.VERTICAL
        box.addView(label("TREINOS", green, 13f, true))
        val selected = editorPlanIndex()
        plans.forEachIndexed { index, plan ->
            val item = card(if (index == selected) surface3 else surface)
            item.orientation = LinearLayout.VERTICAL
            item.setOnClickListener {
                prefs.edit().putInt("editor_plan", index).putInt("editor_exercise", 0).apply()
                render()
            }
            item.addView(label(plan.title + " - " + plan.focus, white, 17f, true))
            item.addView(label(plan.exercises.size.toString() + " exercicios", muted, 13f, false))
            box.addView(item)
        }

        val add = actionButton("Adicionar treino", surface2, green)
        add.setOnClickListener {
            val updated = plans.toMutableList()
            updated.add(WorkoutPlan("custom-" + System.currentTimeMillis(), "Novo treino", "Dia e foco", listOf(
                ExercisePlan("Novo exercicio", "3 x 10", "60s", "Edite este exercicio antes de usar."),
            )))
            saveWorkoutPlans(updated)
            prefs.edit().putInt("editor_plan", updated.lastIndex).putInt("editor_exercise", 0).apply()
            Toast.makeText(this, "Treino adicionado.", Toast.LENGTH_SHORT).show()
            render()
        }
        box.addView(buttonParams(add))
        return box
    }

    private fun planDetailsEditor(): View {
        val planIndex = editorPlanIndex()
        val plan = plans[planIndex]
        val box = card()
        box.orientation = LinearLayout.VERTICAL
        box.addView(label("EDITAR TREINO", green, 13f, true))
        val title = input("Nome do treino", plan.title)
        val focus = input("Dia e foco", plan.focus)
        box.addView(title)
        box.addView(focus)

        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        val save = actionButton("Salvar treino", green, bg)
        save.setOnClickListener {
            saveWorkoutPlanDetails(planIndex, title.textValue(), focus.textValue())
        }
        row.addView(save, LinearLayout.LayoutParams(0, dp(50), 1f))
        val duplicate = actionButton("Duplicar", surface2, white)
        duplicate.setOnClickListener {
            val updated = plans.toMutableList()
            val copy = plan.copy(id = "custom-" + System.currentTimeMillis(), title = plan.title + " copia")
            updated.add(copy)
            saveWorkoutPlans(updated)
            prefs.edit().putInt("editor_plan", updated.lastIndex).putInt("editor_exercise", 0).apply()
            Toast.makeText(this, "Treino duplicado.", Toast.LENGTH_SHORT).show()
            render()
        }
        row.addView(duplicate, LinearLayout.LayoutParams(0, dp(50), 1f))
        box.addView(spacedRow(row))

        val remove = actionButton("Remover treino", surface2, danger)
        remove.setOnClickListener {
            if (plans.size <= 1) {
                Toast.makeText(this, "Mantenha pelo menos um treino.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val updated = plans.toMutableList()
            updated.removeAt(planIndex)
            saveWorkoutPlans(updated)
            prefs.edit().putInt("editor_plan", 0).putInt("editor_exercise", 0).putInt("selected_plan", 0).apply()
            Toast.makeText(this, "Treino removido.", Toast.LENGTH_SHORT).show()
            render()
        }
        box.addView(buttonParams(remove))
        return box
    }

    private fun exerciseDetailsEditor(): View {
        val planIndex = editorPlanIndex()
        val plan = plans[planIndex]
        val exerciseIndex = editorExerciseIndex(plan)
        val exercise = plan.exercises[exerciseIndex]
        val box = card()
        box.orientation = LinearLayout.VERTICAL
        box.addView(label("EXERCICIOS DO TREINO", green, 13f, true))

        plan.exercises.forEachIndexed { index, item ->
            val active = index == exerciseIndex
            val row = card(if (active) surface3 else surface)
            row.orientation = LinearLayout.VERTICAL
            row.setOnClickListener {
                prefs.edit().putInt("editor_exercise", index).apply()
                render()
            }
            row.addView(label((index + 1).toString() + ". " + item.name, white, 16f, true))
            row.addView(label(item.target + " | descanso " + item.rest, muted, 13f, false))
            box.addView(row)
        }

        val name = input("Exercicio", exercise.name)
        val target = input("Series/reps", exercise.target)
        val rest = input("Descanso", exercise.rest)
        val notes = input("Notas", exercise.notes)
        box.addView(name)
        box.addView(target)
        box.addView(rest)
        box.addView(notes)

        val save = actionButton("Salvar exercicio", green, bg)
        save.setOnClickListener {
            saveExerciseDetails(planIndex, exerciseIndex, name.textValue(), target.textValue(), rest.textValue(), notes.textValue())
        }
        box.addView(buttonParams(save))

        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        val add = actionButton("Adicionar", surface2, green)
        add.setOnClickListener {
            val exercises = plan.exercises.toMutableList()
            exercises.add(ExercisePlan("Novo exercicio", "3 x 10", "60s", "Edite antes de usar no treino."))
            replacePlanExercises(planIndex, exercises)
            prefs.edit().putInt("editor_exercise", exercises.lastIndex).apply()
            Toast.makeText(this, "Exercicio adicionado.", Toast.LENGTH_SHORT).show()
            render()
        }
        row.addView(add, LinearLayout.LayoutParams(0, dp(50), 1f))
        val remove = actionButton("Remover", surface2, danger)
        remove.setOnClickListener {
            if (plan.exercises.size <= 1) {
                Toast.makeText(this, "Mantenha pelo menos um exercicio.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val exercises = plan.exercises.toMutableList()
            exercises.removeAt(exerciseIndex)
            replacePlanExercises(planIndex, exercises)
            prefs.edit().putInt("editor_exercise", exerciseIndex.coerceAtMost(exercises.lastIndex)).apply()
            Toast.makeText(this, "Exercicio removido.", Toast.LENGTH_SHORT).show()
            render()
        }
        row.addView(remove, LinearLayout.LayoutParams(0, dp(50), 1f))
        box.addView(spacedRow(row))
        return box
    }

    private fun runningPlanEditor(): View {
        val box = card()
        box.orientation = LinearLayout.VERTICAL
        box.addView(label("AJUSTES DA CORRIDA", green, 13f, true))
        box.addView(label("Os treinos de 5 km continuam estruturados, mas voce pode calibrar tudo para sua esteira.", muted, 14f, false))
        val speed = input("Ajuste de velocidade km/h", prefs.getString("running_speed_offset", "0.0") ?: "0.0")
        val scale = input("Multiplicador de distancia", prefs.getString("running_distance_scale", "1.00") ?: "1.00")
        val start = input("Inicio do ciclo yyyy-mm-dd", prefs.getString("running_plan_start_day", dayKey()) ?: dayKey())
        box.addView(speed)
        box.addView(scale)
        box.addView(start)
        box.addView(label("Exemplo: velocidade 0.3 deixa todas as fases 0,3 km/h mais rapidas; distancia 0.90 reduz os blocos para 90%.", muted, 13f, false))

        val save = actionButton("Salvar ajustes da corrida", green, bg)
        save.setOnClickListener {
            val speedValue = speed.textValue().replace(',', '.').toDoubleOrNull() ?: 0.0
            val scaleValue = (scale.textValue().replace(',', '.').toDoubleOrNull() ?: 1.0).coerceIn(0.60, 1.40)
            prefs.edit()
                .putString("running_speed_offset", speedValue.coerceIn(-2.0, 2.0).toString())
                .putString("running_distance_scale", scaleValue.toString())
                .putString("running_plan_start_day", start.textValue().ifBlank { dayKey() })
                .remove("selected_run_id")
                .apply()
            clearActiveRun()
            Toast.makeText(this, "Corrida ajustada.", Toast.LENGTH_SHORT).show()
            render()
        }
        box.addView(buttonParams(save))
        return box
    }

    private fun planEditorResetPanel(): View {
        val box = card()
        box.orientation = LinearLayout.VERTICAL
        box.addView(label("RESTAURAR PADRAO", green, 13f, true))
        box.addView(label("Use se quiser voltar ao plano A/B/C e corrida original da v12.1.0.", muted, 14f, false))
        val reset = actionButton("Restaurar plano padrao", surface2, danger)
        reset.setOnClickListener {
            prefs.edit()
                .remove("custom_workout_plans")
                .remove("running_speed_offset")
                .remove("running_distance_scale")
                .putString("running_plan_start_day", dayKey())
                .putInt("editor_plan", todayPlanIndex())
                .putInt("editor_exercise", 0)
                .putInt("selected_plan", todayPlanIndex())
                .putInt("selected_exercise", 0)
                .apply()
            clearActiveRun()
            Toast.makeText(this, "Plano padrao restaurado.", Toast.LENGTH_SHORT).show()
            render()
        }
        box.addView(buttonParams(reset))
        return box
    }

    private fun renderHistory(root: LinearLayout) {
        val strengthLogs = filteredStrengthHistory()
        val strengthSessions = filteredStrengthSessionHistory()
        val strengthActivities = strengthHistoryActivities(strengthLogs, strengthSessions)
        val runs = filteredRunHistory()
        root.addView(historyStableSummaryPanel(strengthLogs, strengthActivities, runs))
        root.addView(historyTypeSelectorPanel())
        root.addView(historyCalendarPanel())
        root.addView(historyAdvancedFiltersButton())
        if (prefs.getBoolean("history_filters_open", false)) root.addView(historyFilterPanel())
        root.addView(historyMetricsPanel(strengthLogs, strengthActivities, runs))
        val trendPanel = historyTrendPanel(strengthLogs, runs)
        root.addView(trendPanel)
        if (requestedSection == "trend") pageScrollTarget = trendPanel
        val recordsPanel = historyPersonalRecordsPanel()
        root.addView(recordsPanel)
        if (requestedSection == "records") pageScrollTarget = recordsPanel
        val activityPanel = historyActivityPanel(strengthActivities, runs)
        root.addView(activityPanel)
        if (requestedSection == "activity") pageScrollTarget = activityPanel
        if (strengthLogs.isNotEmpty()) root.addView(exerciseEvolutionPanel(strengthLogs))

        val export = actionButton("Copiar exportacao completa", green, bg)
        export.setOnClickListener { exportToClipboard() }
        root.addView(buttonParams(export))
    }

    private fun historyStableSummaryPanel(
        strengthLogs: List<JSONObject>,
        strengthActivities: List<HistoryActivity>,
        runs: List<JSONObject>,
    ): View {
        val volume = strengthLogs.sumOf { it.optDouble("load") * it.optInt("reps") }.roundToInt()
        val distance = roundKm(runs.sumOf { it.optDouble("distance") })
        val box = card(surface3)
        box.orientation = LinearLayout.VERTICAL
        box.addView(label("HISTORICO V12", green, 13f, true))
        box.addView(label("Registros locais editaveis", white, 24f, true))
        box.addView(label("Use esta tela para revisar, corrigir ou excluir o que ficou salvo no celular.", muted, 14f, false))

        val metrics = LinearLayout(this)
        metrics.orientation = LinearLayout.HORIZONTAL
        metrics.addView(compactMetric("Treinos", strengthActivities.size.toString()), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        metrics.addView(compactMetric("Volume", volume.toString() + " kg"), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        metrics.addView(compactMetric("Corrida", formatKm(distance)), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        box.addView(spacedRow(metrics))
        box.addView(label("Filtro atual: " + historyFilterSummary(), muted, 13f, false))

        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        val activity = actionButton("Atividades", surface2, white)
        activity.setOnClickListener {
            requestedSection = "activity"
            render()
        }
        row.addView(activity, LinearLayout.LayoutParams(0, dp(50), 1f))
        val export = actionButton("Backup", surface2, green)
        export.setOnClickListener { exportToClipboard() }
        row.addView(export, LinearLayout.LayoutParams(0, dp(50), 1f))
        box.addView(spacedRow(row))
        return box
    }

    private fun historyFilterSummary(): String {
        val type = when (prefs.getString("history_type", "all") ?: "all") {
            "run" -> "corrida"
            "strength" -> "musculacao"
            else -> "todos"
        }
        val from = prefs.getString("history_from", "").orEmpty()
        val to = prefs.getString("history_to", "").orEmpty()
        val query = prefs.getString("history_query", "").orEmpty()
        val period = when {
            from.isNotBlank() && to.isNotBlank() -> from + " ate " + to
            from.isNotBlank() -> "desde " + from
            to.isNotBlank() -> "ate " + to
            else -> "sem periodo fixo"
        }
        return type + " | " + period + if (query.isNotBlank()) " | busca: " + query else ""
    }

    private fun historyTypeSelectorPanel(): View {
        val selected = prefs.getString("history_type", "all") ?: "all"
        val box = card(surface2)
        box.orientation = LinearLayout.HORIZONTAL
        listOf(
            Pair("all", "Todos"),
            Pair("run", "Corrida"),
            Pair("strength", "Musculacao"),
        ).forEachIndexed { index, item ->
            val active = selected == item.first
            val button = actionButton(item.second, if (active) green else surface2, if (active) bg else white)
            button.textSize = 14f
            button.isSingleLine = true
            button.setPadding(dp(4), 0, dp(4), 0)
            button.setOnClickListener {
                prefs.edit().putString("history_type", item.first).apply()
                render()
            }
            val params = LinearLayout.LayoutParams(0, dp(48), 1f)
            params.setMargins(if (index == 0) 0 else dp(6), 0, 0, 0)
            box.addView(button, params)
        }
        return box
    }

    private fun historyCalendarPanel(): View {
        val month = historyCalendarMonth()
        val monthKey = SimpleDateFormat("yyyy-MM", Locale.US).format(month.time)
        val locale = Locale("pt", "BR")
        val rawTitle = SimpleDateFormat("MMMM yyyy", locale).format(month.time)
        val monthTitle = rawTitle.substring(0, 1).uppercase(locale) + rawTitle.substring(1)
        val selectedDay = prefs.getString("history_calendar_day", "").orEmpty()
        val today = dayKey()
        val activities = historyCalendarActivities()

        val box = card(surface3)
        box.orientation = LinearLayout.VERTICAL
        box.addView(label("CALENDARIO DE ATIVIDADES", green, 13f, true))

        val header = LinearLayout(this)
        header.orientation = LinearLayout.HORIZONTAL
        header.gravity = Gravity.CENTER_VERTICAL
        val previous = actionButton("<", surface, white)
        previous.contentDescription = "Mes anterior"
        previous.setOnClickListener { shiftHistoryCalendarMonth(-1) }
        header.addView(previous, LinearLayout.LayoutParams(dp(48), dp(48)))
        val title = label(monthTitle, white, 20f, true)
        title.gravity = Gravity.CENTER
        header.addView(title, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        val next = actionButton(">", surface, white)
        next.contentDescription = "Proximo mes"
        next.setOnClickListener { shiftHistoryCalendarMonth(1) }
        header.addView(next, LinearLayout.LayoutParams(dp(48), dp(48)))
        box.addView(spacedRow(header))

        val weekdays = LinearLayout(this)
        weekdays.orientation = LinearLayout.HORIZONTAL
        listOf("Seg", "Ter", "Qua", "Qui", "Sex", "Sab", "Dom").forEach { weekday ->
            val view = label(weekday, muted, 11f, true)
            view.gravity = Gravity.CENTER
            weekdays.addView(view, LinearLayout.LayoutParams(0, dp(30), 1f))
        }
        box.addView(weekdays)

        val firstDayOffset = (month.get(Calendar.DAY_OF_WEEK) + 5) % 7
        val daysInMonth = month.getActualMaximum(Calendar.DAY_OF_MONTH)
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        repeat(6) { rowIndex ->
            val week = LinearLayout(this)
            week.orientation = LinearLayout.HORIZONTAL
            repeat(7) { columnIndex ->
                val dayNumber = rowIndex * 7 + columnIndex - firstDayOffset + 1
                if (dayNumber in 1..daysInMonth) {
                    val dayCalendar = month.clone() as Calendar
                    dayCalendar.set(Calendar.DAY_OF_MONTH, dayNumber)
                    val dateKey = formatter.format(dayCalendar.time)
                    val cell = historyCalendarDayCell(
                        dayNumber = dayNumber,
                        dateKey = dateKey,
                        summary = activities[dateKey] ?: HistoryCalendarSummary(),
                        selected = selectedDay == dateKey,
                        today = today == dateKey,
                    )
                    week.addView(cell, LinearLayout.LayoutParams(0, dp(52), 1f))
                } else {
                    week.addView(View(this), LinearLayout.LayoutParams(0, dp(52), 1f))
                }
            }
            box.addView(week)
        }

        val monthActivities = activities.filterKeys { it.startsWith(monthKey) }.values
        val monthMetrics = LinearLayout(this)
        monthMetrics.orientation = LinearLayout.HORIZONTAL
        monthMetrics.addView(
            compactMetric("Treinos", monthActivities.sumOf { it.strengthWorkouts }.toString()),
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f),
        )
        monthMetrics.addView(
            compactMetric("Corridas", monthActivities.sumOf { it.runs }.toString()),
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f),
        )
        monthMetrics.addView(
            compactMetric("Distancia", formatKm(monthActivities.sumOf { it.runningDistance })),
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f),
        )
        box.addView(spacedRow(monthMetrics))

        box.addView(label("PERIODO RAPIDO", muted, 12f, true))
        val periods = LinearLayout(this)
        periods.orientation = LinearLayout.HORIZONTAL
        listOf(Pair(30, "30 dias"), Pair(90, "90 dias"), Pair(0, "Tudo")).forEachIndexed { index, period ->
            val days = period.first.takeIf { it > 0 }
            val active = historyQuickPeriodActive(days)
            val button = actionButton(period.second, if (active) green else surface, if (active) bg else white)
            button.setOnClickListener { applyHistoryQuickPeriod(days) }
            val params = LinearLayout.LayoutParams(0, dp(48), 1f)
            params.setMargins(if (index == 0) 0 else dp(6), dp(8), 0, 0)
            periods.addView(button, params)
        }
        box.addView(periods)

        val calendarActions = LinearLayout(this)
        calendarActions.orientation = LinearLayout.HORIZONTAL
        val currentMonth = actionButton("Mes atual", surface, white)
        currentMonth.setOnClickListener { showCurrentHistoryMonth() }
        calendarActions.addView(currentMonth, LinearLayout.LayoutParams(0, dp(48), 1f))
        if (selectedDay.isNotBlank()) {
            val clearDay = actionButton("Limpar dia", surface, muted)
            clearDay.setOnClickListener { clearHistoryCalendarDay() }
            calendarActions.addView(clearDay, LinearLayout.LayoutParams(0, dp(48), 1f))
            box.addView(label("Dia selecionado: " + selectedDay, green, 13f, true))
        }
        box.addView(spacedRow(calendarActions))
        box.addView(label("Verde indica musculacao; azul indica corrida. Toque em um dia para filtrar.", muted, 12f, false))
        return box
    }

    private fun historyCalendarDayCell(
        dayNumber: Int,
        dateKey: String,
        summary: HistoryCalendarSummary,
        selected: Boolean,
        today: Boolean,
    ): View {
        val cell = LinearLayout(this)
        cell.orientation = LinearLayout.VERTICAL
        cell.gravity = Gravity.CENTER
        cell.setPadding(dp(2), dp(3), dp(2), dp(3))
        cell.background = rounded(
            if (selected) surface else if (today) surface2 else surface3,
            dp(Mo2Radius.Sm),
            if (selected) green else if (today) border else null,
        )
        cell.isClickable = true
        cell.isFocusable = true

        val number = label(dayNumber.toString(), if (selected || today) green else white, 13f, selected || today)
        number.gravity = Gravity.CENTER
        cell.addView(number, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))

        val markers = LinearLayout(this)
        markers.orientation = LinearLayout.HORIZONTAL
        markers.gravity = Gravity.CENTER
        if (summary.strengthWorkouts > 0) {
            val marker = View(this)
            marker.background = rounded(green, dp(Mo2Radius.Pill))
            markers.addView(marker, LinearLayout.LayoutParams(dp(6), dp(6)))
        }
        if (summary.runs > 0) {
            val marker = View(this)
            marker.background = rounded(Mo2Colors.Running, dp(Mo2Radius.Pill))
            val params = LinearLayout.LayoutParams(dp(6), dp(6))
            params.setMargins(if (summary.strengthWorkouts > 0) dp(3) else 0, 0, 0, 0)
            markers.addView(marker, params)
        }
        cell.addView(markers, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(9)))

        cell.contentDescription = dateKey + ", " + summary.strengthWorkouts + " treinos de musculacao, " +
            summary.runs + " corridas"
        cell.setOnClickListener { selectHistoryCalendarDay(dateKey) }
        return cell
    }

    private fun historyCalendarActivities(): Map<String, HistoryCalendarSummary> {
        val selectedType = prefs.getString("history_type", "all") ?: "all"
        val result = linkedMapOf<String, HistoryCalendarSummary>()
        if (selectedType != "run") {
            val workoutKeys = linkedSetOf<String>()
            val sets = allLogs()
            for (index in 0 until sets.length()) {
                val item = sets.getJSONObject(index)
                val day = item.optString("day")
                if (!isValidHistoryDay(day)) continue
                val summary = result.getOrPut(day) { HistoryCalendarSummary() }
                summary.strengthSets += 1
                workoutKeys.add(strengthHistoryKey(day, item.optString("plan", "Treino")))
            }
            val sessions = strengthSessionLogs()
            for (index in 0 until sessions.length()) {
                val item = sessions.getJSONObject(index)
                val day = item.optString("day")
                if (!isValidHistoryDay(day)) continue
                workoutKeys.add(strengthHistoryKey(day, item.optString("plan_title", "Treino")))
            }
            workoutKeys.forEach { key ->
                val day = key.substringBefore("::")
                result.getOrPut(day) { HistoryCalendarSummary() }.strengthWorkouts += 1
            }
        }
        if (selectedType != "strength") {
            val runs = runLogs()
            for (index in 0 until runs.length()) {
                val item = runs.getJSONObject(index)
                val day = item.optString("day")
                if (!isValidHistoryDay(day)) continue
                val summary = result.getOrPut(day) { HistoryCalendarSummary() }
                summary.runs += 1
                summary.runningDistance += item.optDouble("distance")
            }
        }
        return result
    }

    private fun historyCalendarMonth(): Calendar {
        val stored = prefs.getString("history_calendar_month", "").orEmpty()
        val parser = SimpleDateFormat("yyyy-MM", Locale.US)
        parser.isLenient = false
        val calendar = Calendar.getInstance()
        val parsed = try {
            if (stored.isBlank()) null else parser.parse(stored)
        } catch (_: Exception) {
            null
        }
        if (parsed != null) calendar.time = parsed
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar
    }

    private fun shiftHistoryCalendarMonth(delta: Int) {
        val month = historyCalendarMonth()
        month.add(Calendar.MONTH, delta)
        prefs.edit()
            .putString("history_calendar_month", SimpleDateFormat("yyyy-MM", Locale.US).format(month.time))
            .apply()
        render()
    }

    private fun showCurrentHistoryMonth() {
        prefs.edit()
            .putString("history_calendar_month", SimpleDateFormat("yyyy-MM", Locale.US).format(Date()))
            .apply()
        render()
    }

    private fun selectHistoryCalendarDay(day: String) {
        prefs.edit()
            .putString("history_calendar_day", day)
            .putString("history_calendar_month", day.take(7))
            .putString("history_from", day)
            .putString("history_to", day)
            .apply()
        render()
    }

    private fun clearHistoryCalendarDay() {
        val selected = prefs.getString("history_calendar_day", "").orEmpty()
        val editor = prefs.edit().remove("history_calendar_day")
        if (prefs.getString("history_from", "") == selected && prefs.getString("history_to", "") == selected) {
            editor.remove("history_from").remove("history_to")
        }
        editor.apply()
        render()
    }

    private fun historyQuickPeriodActive(days: Int?): Boolean {
        if (prefs.getString("history_calendar_day", "").orEmpty().isNotBlank()) return false
        val from = prefs.getString("history_from", "").orEmpty()
        val to = prefs.getString("history_to", "").orEmpty()
        if (days == null) return from.isBlank() && to.isBlank()
        return from == historyDateOffset(-(days - 1)) && to == dayKey()
    }

    private fun applyHistoryQuickPeriod(days: Int?) {
        val editor = prefs.edit().remove("history_calendar_day")
        if (days == null) {
            editor.remove("history_from").remove("history_to")
        } else {
            editor.putString("history_from", historyDateOffset(-(days - 1))).putString("history_to", dayKey())
        }
        editor.apply()
        render()
    }

    private fun historyDateOffset(days: Int): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, days)
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)
    }

    private fun historyAdvancedFiltersButton(): View {
        val open = prefs.getBoolean("history_filters_open", false)
        val button = actionButton(if (open) "Ocultar filtros avancados" else "Filtros avancados", surface, muted)
        button.setOnClickListener {
            prefs.edit().putBoolean("history_filters_open", !open).apply()
            render()
        }
        return buttonParams(button)
    }

    private fun historyFilterPanel(): View {
        val box = card(surface)
        box.orientation = LinearLayout.VERTICAL
        box.addView(label("FILTROS AVANCADOS", green, 13f, true))
        val from = input("De yyyy-mm-dd", prefs.getString("history_from", "") ?: "")
        val to = input("Ate yyyy-mm-dd", prefs.getString("history_to", "") ?: "")
        val query = input("Buscar exercicio, treino ou observacao", prefs.getString("history_query", "") ?: "")
        box.addView(from)
        box.addView(to)
        box.addView(query)

        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        val apply = actionButton("Aplicar", green, bg)
        apply.setOnClickListener {
            hideKeyboard()
            val fromValue = from.textValue().trim()
            val toValue = to.textValue().trim()
            if ((fromValue.isNotBlank() && !isValidHistoryDay(fromValue)) ||
                (toValue.isNotBlank() && !isValidHistoryDay(toValue))) {
                Toast.makeText(this, "Use datas validas no formato yyyy-mm-dd.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (fromValue.isNotBlank() && toValue.isNotBlank() && fromValue > toValue) {
                Toast.makeText(this, "A data inicial deve vir antes da data final.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            prefs.edit()
                .putString("history_from", fromValue)
                .putString("history_to", toValue)
                .putString("history_query", query.textValue().trim())
                .remove("history_calendar_day")
                .apply()
            render()
        }
        row.addView(apply, LinearLayout.LayoutParams(0, dp(50), 1f))
        val clear = actionButton("Limpar", surface2, white)
        clear.setOnClickListener {
            hideKeyboard()
            prefs.edit()
                .remove("history_from")
                .remove("history_to")
                .remove("history_query")
                .remove("history_calendar_day")
                .putString("history_type", "all")
                .apply()
            render()
        }
        row.addView(clear, LinearLayout.LayoutParams(0, dp(50), 1f))
        box.addView(spacedRow(row))
        return box
    }

    private fun historyMetricsPanel(
        strengthLogs: List<JSONObject>,
        strengthActivities: List<HistoryActivity>,
        runs: List<JSONObject>,
    ): View {
        val volume = strengthLogs.sumOf { it.optDouble("load") * it.optInt("reps") }.roundToInt()
        val runDistance = runs.sumOf { it.optDouble("distance") }
        val bestLoad = strengthLogs.maxOfOrNull { it.optDouble("load") } ?: 0.0
        val activities = strengthActivities.size + runs.size
        val activeDays = (strengthActivities.map { it.item.optString("day") } + runs.map { it.optString("day") })
            .filter { it.isNotBlank() }
            .toSet()
            .size
        val box = card(surface3)
        box.orientation = LinearLayout.VERTICAL
        box.addView(label("RESUMO DO FILTRO", green, 13f, true))
        box.addView(label(activities.toString() + " atividades", white, 26f, true))
        box.addView(label(activeDays.toString() + " dias com registro", muted, 14f, false))

        val metrics = LinearLayout(this)
        metrics.orientation = LinearLayout.HORIZONTAL
        metrics.addView(compactMetric("Series", strengthLogs.size.toString()), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        metrics.addView(compactMetric("Volume", volume.toString() + " kg"), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        metrics.addView(compactMetric("Corrida", formatKm(runDistance)), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        box.addView(spacedRow(metrics))
        if (bestLoad > 0.0) box.addView(label("Maior carga no filtro: " + formatLoad(bestLoad), muted, 13f, false))
        return box
    }

    private fun historyTrendPanel(
        strengthLogs: List<JSONObject>,
        runs: List<JSONObject>,
    ): View {
        val points = historyWeekPoints(strengthLogs, runs)
        val selectedType = prefs.getString("history_type", "all") ?: "all"
        val box = card(surface2)
        box.orientation = LinearLayout.VERTICAL
        box.addView(label("EVOLUCAO EM 8 SEMANAS", green, 13f, true))
        box.addView(label("Volume e distancia acompanham os filtros ativos.", muted, 13f, false))

        val legend = LinearLayout(this)
        legend.orientation = LinearLayout.HORIZONTAL
        if (selectedType != "run") {
            legend.addView(
                historyLegendItem("Volume de musculacao", green),
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f),
            )
        }
        if (selectedType != "strength") {
            legend.addView(
                historyLegendItem("Distancia corrida", Mo2Colors.Running),
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f),
            )
        }
        box.addView(spacedRow(legend))

        val chart = Mo2HistoryChartView(this, points)
        val chartParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(210))
        chartParams.setMargins(0, dp(8), 0, dp(8))
        box.addView(chart, chartParams)

        val maxVolume = points.maxOfOrNull { it.strengthVolume } ?: 0.0
        val maxDistance = points.maxOfOrNull { it.runningDistance } ?: 0.0
        val scaleParts = mutableListOf<String>()
        if (selectedType != "run") scaleParts.add("volume ate " + maxVolume.roundToInt() + " kg")
        if (selectedType != "strength") scaleParts.add("corrida ate " + formatKm(maxDistance))
        box.addView(label("Escalas independentes: " + scaleParts.joinToString(" | "), muted, 12f, false))

        val current = points.lastOrNull()
        val previous = points.getOrNull(points.lastIndex - 1)
        if (current != null && previous != null) {
            if (selectedType != "run") {
                box.addView(historyTrendComparison("Musculacao", current.strengthVolume, previous.strengthVolume, true, green))
            }
            if (selectedType != "strength") {
                box.addView(historyTrendComparison("Corrida", current.runningDistance, previous.runningDistance, false, Mo2Colors.Running))
            }
        }
        return box
    }

    private fun historyLegendItem(text: String, color: Int): View {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        val swatch = View(this)
        swatch.background = rounded(color, dp(Mo2Radius.Pill))
        row.addView(swatch, LinearLayout.LayoutParams(dp(10), dp(10)))
        val title = label(text, white, 12f, true)
        title.setPadding(dp(6), 0, 0, 0)
        row.addView(title, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        return row
    }

    private fun historyTrendComparison(
        title: String,
        current: Double,
        previous: Double,
        volume: Boolean,
        color: Int,
    ): TextView {
        val currentText = if (volume) current.roundToInt().toString() + " kg" else formatKm(current)
        val change = when {
            current <= 0.0 && previous <= 0.0 -> "sem registro nesta semana"
            previous <= 0.0 -> "novo registro nesta semana"
            else -> {
                val percent = (((current - previous) / previous) * 100.0).roundToInt()
                (if (percent > 0) "+" else "") + percent + "% contra a semana anterior"
            }
        }
        return label(title + ": " + currentText + " | " + change, color, 13f, true)
    }

    private fun historyWeekPoints(
        strengthLogs: List<JSONObject>,
        runs: List<JSONObject>,
    ): List<Mo2HistoryPoint> {
        val monday = Calendar.getInstance()
        monday.set(Calendar.HOUR_OF_DAY, 0)
        monday.set(Calendar.MINUTE, 0)
        monday.set(Calendar.SECOND, 0)
        monday.set(Calendar.MILLISECOND, 0)
        monday.add(Calendar.DAY_OF_YEAR, -((monday.get(Calendar.DAY_OF_WEEK) + 5) % 7))
        val dayFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val labelFormatter = SimpleDateFormat("dd/MM", Locale("pt", "BR"))

        return (7 downTo 0).map { weeksAgo ->
            val start = monday.clone() as Calendar
            start.add(Calendar.DAY_OF_YEAR, -weeksAgo * 7)
            val end = start.clone() as Calendar
            end.add(Calendar.DAY_OF_YEAR, 6)
            val from = dayFormatter.format(start.time)
            val to = dayFormatter.format(end.time)
            val volume = strengthLogs
                    .filter { item ->
                        item.optString("day").let { day -> day >= from && day <= to }
                    }
                .sumOf { it.optDouble("load") * it.optInt("reps") }
            val distance = runs
                .filter { it.optString("day") in from..to }
                .sumOf { it.optDouble("distance") }
            Mo2HistoryPoint(labelFormatter.format(start.time), volume, distance)
        }
    }

    private fun historyPersonalRecordsPanel(): View {
        val selectedType = prefs.getString("history_type", "all") ?: "all"
        val strengthLogs = historyJsonItems(allLogs()).filter { it.optDouble("load") > 0.0 && it.optInt("reps") > 0 }
        val runs = historyJsonItems(runLogs()).filter { it.optDouble("distance") > 0.0 }
        val box = card(surface3)
        box.orientation = LinearLayout.VERTICAL
        box.addView(label("RECORDES PESSOAIS", green, 13f, true))
        box.addView(label("Melhores marcas de todo o historico, sem limitar pelo periodo selecionado.", muted, 13f, false))

        var hasRecord = false
        if (selectedType != "run" && strengthLogs.isNotEmpty()) {
            hasRecord = true
            val maxLoad = strengthLogs.maxByOrNull { it.optDouble("load") }
            val bestEstimated = strengthLogs.maxByOrNull { estimatedOneRepMax(it) }
            if (maxLoad != null) {
                box.addView(historyRecordRow(
                    title = "Maior carga",
                    value = formatLoad(maxLoad.optDouble("load")),
                    detail = maxLoad.optString("exercise", "Exercicio") + " | " + maxLoad.optInt("reps") + " reps | " + maxLoad.optString("day"),
                    color = green,
                ))
            }
            if (bestEstimated != null) {
                box.addView(historyRecordRow(
                    title = "Melhor e1RM estimado",
                    value = formatLoad(estimatedOneRepMax(bestEstimated)),
                    detail = bestEstimated.optString("exercise", "Exercicio") + " | formula de Epley pela melhor serie",
                    color = green,
                ))
            }

            box.addView(label("MELHORES POR EXERCICIO", muted, 12f, true))
            strengthLogs
                .groupBy { it.optString("exercise", "Exercicio") }
                .mapValues { entry -> entry.value.maxByOrNull { estimatedOneRepMax(it) }!! }
                .entries
                .sortedByDescending { estimatedOneRepMax(it.value) }
                .take(5)
                .forEach { entry ->
                    val item = entry.value
                    box.addView(label(
                        entry.key + ": e1RM " + formatLoad(estimatedOneRepMax(item)) +
                            " | serie " + formatLoad(item.optDouble("load")) + " x " + item.optInt("reps"),
                        white,
                        13f,
                        true,
                    ))
                }
        }

        if (selectedType != "strength" && runs.isNotEmpty()) {
            hasRecord = true
            val longest = runs.maxByOrNull { it.optDouble("distance") }
            val fastest = runs
                .filter { it.optDouble("distance") >= 1.0 && historyRunPaceSeconds(it) > 0L }
                .minByOrNull { historyRunPaceSeconds(it) }
            if (longest != null) {
                box.addView(historyRecordRow(
                    title = "Corrida mais longa",
                    value = formatKm(longest.optDouble("distance")),
                    detail = longest.optString("workout_title", "Corrida") + " | " + longest.optString("day"),
                    color = Mo2Colors.Running,
                ))
            }
            if (fastest != null) {
                box.addView(historyRecordRow(
                    title = "Melhor ritmo (minimo 1 km)",
                    value = formatPaceSeconds(historyRunPaceSeconds(fastest)),
                    detail = formatKm(fastest.optDouble("distance")) + " | " + fastest.optString("day"),
                    color = Mo2Colors.Running,
                ))
            } else if (runs.any { it.optDouble("distance") >= 1.0 }) {
                box.addView(label(
                    "Melhor ritmo indisponivel: o registro precisa ter duracao ou velocidade plausivel.",
                    muted,
                    13f,
                    false,
                ))
            }
        }

        if (!hasRecord) {
            box.addView(label("Registre cargas ou corridas para liberar seus recordes pessoais.", muted, 14f, false))
        }
        return box
    }

    private fun historyRecordRow(
        title: String,
        value: String,
        detail: String,
        color: Int,
    ): View {
        val wrap = LinearLayout(this)
        wrap.orientation = LinearLayout.VERTICAL
        wrap.setPadding(0, dp(10), 0, dp(8))
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        val marker = View(this)
        marker.background = rounded(color, dp(Mo2Radius.Pill))
        row.addView(marker, LinearLayout.LayoutParams(dp(4), dp(54)))
        val text = LinearLayout(this)
        text.orientation = LinearLayout.VERTICAL
        text.setPadding(dp(10), 0, dp(8), 0)
        text.addView(label(title, white, 15f, true))
        text.addView(label(detail, muted, 12f, false))
        row.addView(text, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        val valueView = label(value, color, 18f, true)
        valueView.gravity = Gravity.END or Gravity.CENTER_VERTICAL
        row.addView(valueView)
        wrap.addView(row)
        val divider = View(this)
        divider.setBackgroundColor(border)
        val dividerParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1))
        dividerParams.setMargins(dp(14), dp(8), 0, 0)
        wrap.addView(divider, dividerParams)
        return wrap
    }

    private fun historyJsonItems(items: JSONArray): List<JSONObject> {
        return (0 until items.length()).map { index -> items.getJSONObject(index) }
    }

    private fun estimatedOneRepMax(item: JSONObject): Double {
        val load = item.optDouble("load")
        val reps = item.optInt("reps").coerceIn(1, 30)
        return load * (1.0 + reps / 30.0)
    }

    private fun historyRunPaceSeconds(item: JSONObject): Long {
        val stored = item.optLong("pace_seconds_per_km", 0L)
        if (isPlausibleRunningPace(stored)) return stored
        val distance = item.optDouble("distance")
        val seconds = item.optLong("duration_seconds", 0L).takeIf { it > 0L }
            ?: parseHistoryDurationSeconds(item.optString("duration"))
        if (distance > 0.0 && seconds > 0L) {
            val calculated = (seconds.toDouble() / distance).roundToInt().toLong()
            if (isPlausibleRunningPace(calculated)) return calculated
        }
        val speed = item.optDouble("speed")
        val speedPace = if (speed > 0.0) (3600.0 / speed).roundToInt().toLong() else 0L
        return speedPace.takeIf(::isPlausibleRunningPace) ?: 0L
    }

    private fun isPlausibleRunningPace(secondsPerKm: Long): Boolean = secondsPerKm in 120L..1800L

    private fun exerciseEvolutionPanel(strengthLogs: List<JSONObject>): View {
        val box = card(surface2)
        box.orientation = LinearLayout.VERTICAL
        box.addView(label("EVOLUCAO POR EXERCICIO", green, 13f, true))
        if (strengthLogs.isEmpty()) {
            box.addView(label("Nenhuma serie no filtro atual.", muted, 15f, false))
            return box
        }

        val grouped = linkedMapOf<String, MutableList<JSONObject>>()
        strengthLogs.forEach { item ->
            val key = item.optString("exercise").ifBlank { "Exercicio" }
            grouped.getOrPut(key) { mutableListOf() }.add(item)
        }
        grouped.entries
            .sortedByDescending { entry -> entry.value.sumOf { it.optDouble("load") * it.optInt("reps") } }
            .take(8)
            .forEach { entry ->
                val items = entry.value.sortedBy { it.optString("day") + it.optString("time") }
                val first = items.first()
                val last = items.last()
                val best = items.maxOf { it.optDouble("load") }
                val volume = items.sumOf { it.optDouble("load") * it.optInt("reps") }.roundToInt()
                val delta = last.optDouble("load") - first.optDouble("load")
                val sign = if (delta > 0.0) "+" else ""
                box.addView(label(entry.key, white, 17f, true))
                box.addView(label(items.size.toString() + " series | volume " + volume + " kg | melhor " + formatLoad(best) + " | carga " + sign + formatLoad(delta), muted, 13f, false))
            }
        return box
    }

    private fun strengthHistoryActivities(
        strengthLogs: List<JSONObject>,
        filteredSessions: List<JSONObject>,
    ): List<HistoryActivity> {
        val setsByKey = linkedMapOf<String, MutableList<JSONObject>>()
        strengthLogs.forEach { item ->
            val key = strengthHistoryKey(item.optString("day"), item.optString("plan", "Treino"))
            setsByKey.getOrPut(key) { mutableListOf() }.add(item)
        }

        val allSessionsByKey = linkedMapOf<String, JSONObject>()
        val allSessions = strengthSessionLogs()
        for (index in 0 until allSessions.length()) {
            val session = allSessions.getJSONObject(index)
            val key = strengthHistoryKey(session.optString("day"), session.optString("plan_title", "Treino"))
            allSessionsByKey[key] = session
        }

        val eligibleKeys = linkedSetOf<String>()
        eligibleKeys.addAll(setsByKey.keys)
        filteredSessions.forEach { session ->
            eligibleKeys.add(strengthHistoryKey(session.optString("day"), session.optString("plan_title", "Treino")))
        }

        val completeSetsByKey = linkedMapOf<String, MutableList<JSONObject>>()
        val allSetLogs = allLogs()
        for (index in 0 until allSetLogs.length()) {
            val item = allSetLogs.getJSONObject(index)
            val key = strengthHistoryKey(item.optString("day"), item.optString("plan", "Treino"))
            if (eligibleKeys.contains(key)) completeSetsByKey.getOrPut(key) { mutableListOf() }.add(item)
        }

        return eligibleKeys.map { key ->
            val sets = completeSetsByKey[key].orEmpty().sortedBy { it.optString("time") }
            val completion = allSessionsByKey[key]
            val firstSet = sets.firstOrNull()
            val latestSet = sets.maxByOrNull { it.optString("time") }
            val day = completion?.optString("day")?.takeIf { it.isNotBlank() } ?: firstSet?.optString("day").orEmpty()
            val plan = completion?.optString("plan_title")?.takeIf { it.isNotBlank() }
                ?: firstSet?.optString("plan", "Treino")
                ?: "Treino"
            val time = completion?.optString("time")?.takeIf { it.isNotBlank() }
                ?: latestSet?.optString("time").orEmpty()
            val item = JSONObject()
                .put("id", "strength:" + strengthHistoryKey(day, plan))
                .put("day", day)
                .put("time", time)
                .put("plan", plan)
            HistoryActivity("strength", item, sets, completion)
        }
    }

    private fun strengthHistoryKey(day: String, plan: String): String = day + "::" + plan

    private fun historyActivityPanel(strengthActivities: List<HistoryActivity>, runs: List<JSONObject>): View {
        val activities = (strengthActivities + runs.map { HistoryActivity("run", it) })
            .sortedByDescending { activity -> activity.item.optString("day") + activity.item.optString("time") }

        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        val header = LinearLayout(this)
        header.orientation = LinearLayout.VERTICAL
        header.setPadding(0, dp(14), 0, dp(4))
        header.addView(label("ATIVIDADES RECENTES", white, 20f, true))
        header.addView(label("Edite ou exclua registros salvos neste celular.", muted, 13f, false))
        box.addView(header)

        if (activities.isEmpty()) {
            val empty = card(surface2)
            empty.orientation = LinearLayout.VERTICAL
            empty.addView(label("Nenhuma atividade encontrada", white, 18f, true))
            empty.addView(label("Ajuste os filtros ou registre um novo treino.", muted, 14f, false))
            box.addView(empty)
            return box
        }

        activities.take(60).forEach { activity -> box.addView(historyActivityCard(activity)) }
        if (activities.size > 60) {
            box.addView(label("Mostrando 60 de " + activities.size + ". Use os filtros avancados para refinar.", amber, 14f, true))
        }
        return box
    }

    private fun historyActivityCard(activity: HistoryActivity): View {
        return if (activity.type == "run") runHistoryActivityCard(activity) else strengthHistoryActivityCard(activity)
    }

    private fun strengthHistoryActivityCard(activity: HistoryActivity): View {
        val item = activity.item
        val sets = activity.strengthSets
        val completion = activity.completion
        val day = item.optString("day")
        val plan = item.optString("plan", "Treino")
        val expansionKey = strengthHistoryKey(day, plan)
        val expanded = prefs.getString("history_expanded_strength", "") == expansionKey
        val exercises = sets.map { it.optString("exercise", "Exercicio") }.distinct()
        val volume = sets.sumOf { it.optDouble("load") * it.optInt("reps") }.roundToInt()
        val partial = jsonStringList(completion?.optJSONArray("partial_exercises"))
        val skipped = jsonStringList(completion?.optJSONArray("skipped_exercises"))

        val box = card(surface2)
        box.orientation = LinearLayout.VERTICAL
        val top = LinearLayout(this)
        top.orientation = LinearLayout.HORIZONTAL
        top.gravity = Gravity.CENTER_VERTICAL
        val titleBox = LinearLayout(this)
        titleBox.orientation = LinearLayout.VERTICAL
        titleBox.addView(label(plan, white, 19f, true))
        titleBox.addView(label(day + " | " + item.optString("time"), muted, 12f, false))
        top.addView(titleBox, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        top.addView(historyTypeBadge(if (completion != null) "CONCLUIDO" else "MUSCULACAO", green))
        box.addView(top)
        box.addView(label(
            sets.size.toString() + " series | " + exercises.size + " exercicios | volume " + volume + " kg",
            green,
            15f,
            true,
        ))
        if (partial.isNotEmpty()) box.addView(label("Parciais: " + partial.joinToString(", "), amber, 13f, true))
        if (skipped.isNotEmpty()) box.addView(label("Pulados: " + skipped.joinToString(", "), muted, 13f, false))

        if (expanded) {
            if (sets.isEmpty()) {
                box.addView(label("Treino concluido sem series registradas.", muted, 14f, false))
            } else {
                sets.groupBy { it.optString("exercise", "Exercicio") }.forEach { entry ->
                    box.addView(label(entry.key.uppercase(Locale("pt", "BR")), white, 14f, true))
                    entry.value.forEachIndexed { index, set -> box.addView(strengthHistorySetRow(set, index + 1)) }
                }
            }
            val deleteWorkout = actionButton("Excluir treino do historico", surface, danger)
            deleteWorkout.setOnClickListener { showDeleteStrengthHistorySessionDialog(activity) }
            box.addView(buttonParams(deleteWorkout))
        }
        val expand = actionButton(if (expanded) "Ocultar series" else "Ver todas as series", surface, if (expanded) muted else white)
        expand.setOnClickListener { toggleHistoryExpansion("history_expanded_strength", expansionKey, expanded) }
        box.addView(buttonParams(expand))
        box.setOnClickListener { toggleHistoryExpansion("history_expanded_strength", expansionKey, expanded) }
        return box
    }

    private fun strengthHistorySetRow(item: JSONObject, number: Int): View {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.VERTICAL
        row.setPadding(0, dp(10), 0, dp(6))
        val divider = View(this)
        divider.setBackgroundColor(border)
        row.addView(divider, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1)))
        val rpe = item.optString("rpe").ifBlank { "-" }
        row.addView(label(
            "Serie " + number + ": " + item.optInt("reps") + " reps x " + formatLoad(item.optDouble("load")) + " | RPE " + rpe,
            white,
            14f,
            true,
        ))
        val notes = item.optString("notes")
        if (notes.isNotBlank()) row.addView(label(notes, muted, 12f, false))
        val actions = LinearLayout(this)
        actions.orientation = LinearLayout.HORIZONTAL
        val edit = actionButton("Editar serie", surface, white)
        edit.setOnClickListener { showEditStrengthHistoryDialog(item) }
        actions.addView(edit, LinearLayout.LayoutParams(0, dp(44), 1f))
        val delete = actionButton("Excluir", surface, danger)
        delete.setOnClickListener { showDeleteHistoryActivityDialog(HistoryActivity("strength", item)) }
        actions.addView(delete, LinearLayout.LayoutParams(0, dp(44), 1f))
        row.addView(spacedRow(actions))
        return row
    }

    private fun runHistoryActivityCard(activity: HistoryActivity): View {
        val item = activity.item
        val stages = runStagesForLog(item)
        val expansionKey = item.optString("id").ifBlank {
            item.optString("day") + "::" + item.optString("time") + "::" + item.optString("workout_title")
        }
        val expanded = prefs.getString("history_expanded_run", "") == expansionKey
        val box = card(surface2)
        box.orientation = LinearLayout.VERTICAL

        val top = LinearLayout(this)
        top.orientation = LinearLayout.HORIZONTAL
        top.gravity = Gravity.CENTER_VERTICAL
        val titleBox = LinearLayout(this)
        titleBox.orientation = LinearLayout.VERTICAL
        titleBox.addView(label(item.optString("workout_title", "Corrida"), white, 19f, true))
        titleBox.addView(label(item.optString("day") + " | " + item.optString("time"), muted, 12f, false))
        top.addView(titleBox, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        top.addView(historyTypeBadge("CORRIDA", Mo2Colors.Running))
        box.addView(top)

        val paceSeconds = historyRunPaceSeconds(item)
        val pace = if (paceSeconds > 0L) formatPaceSeconds(paceSeconds) else "ritmo indisponivel"
        box.addView(label(
            formatKm(item.optDouble("distance")) + " | " + item.optString("duration", "-") + " | " + pace,
            Mo2Colors.Running,
            15f,
            true,
        ))
        if (stages.length() > 0) {
            val completedStages = item.optInt("completed_stages", stages.length()).coerceIn(0, stages.length())
            box.addView(label(completedStages.toString() + "/" + stages.length() + " etapas concluidas", muted, 13f, false))
        }
        val notes = item.optString("feedback_notes").ifBlank { item.optString("notes") }
        if (notes.isNotBlank()) box.addView(label(notes, muted, 13f, false))

        if (expanded) {
            for (index in 0 until stages.length()) {
                box.addView(runHistoryStageRow(item, stages.getJSONObject(index), index))
            }
        }

        if (stages.length() > 0) {
            val expand = actionButton(if (expanded) "Ocultar etapas" else "Ver e editar etapas", surface, if (expanded) muted else Mo2Colors.Running)
            expand.setOnClickListener { toggleHistoryExpansion("history_expanded_run", expansionKey, expanded) }
            box.addView(buttonParams(expand))
        }
        val actions = LinearLayout(this)
        actions.orientation = LinearLayout.HORIZONTAL
        val edit = actionButton("Editar corrida", surface, white)
        edit.setOnClickListener { showEditRunHistoryDialog(item) }
        actions.addView(edit, LinearLayout.LayoutParams(0, dp(46), 1f))
        val delete = actionButton("Excluir", surface, danger)
        delete.setOnClickListener { showDeleteHistoryActivityDialog(activity) }
        actions.addView(delete, LinearLayout.LayoutParams(0, dp(46), 1f))
        box.addView(spacedRow(actions))
        box.setOnClickListener {
            if (stages.length() > 0) toggleHistoryExpansion("history_expanded_run", expansionKey, expanded)
        }
        return box
    }

    private fun historyTypeBadge(text: String, color: Int): TextView {
        val badge = label(text, color, 11f, true)
        badge.gravity = Gravity.CENTER
        badge.setPadding(dp(10), dp(5), dp(10), dp(5))
        badge.background = rounded(surface, dp(Mo2Radius.Pill), border)
        return badge
    }

    private fun runHistoryStageRow(run: JSONObject, stage: JSONObject, index: Int): View {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.VERTICAL
        row.setPadding(0, dp(10), 0, dp(6))
        val divider = View(this)
        divider.setBackgroundColor(border)
        row.addView(divider, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1)))
        row.addView(label("ETAPA " + (index + 1) + " | " + stage.optString("title", "Etapa"), white, 14f, true))
        val distance = stage.optDouble("distance")
        val speed = stage.optDouble("speed")
        val durationSeconds = if (distance > 0.0 && speed > 0.0) ((distance / speed) * 3600.0).roundToInt().toLong() else 0L
        row.addView(label(
            formatKm(distance) + " | " + formatSpeed(speed) + if (durationSeconds > 0L) " | " + formatDuration(durationSeconds) else "",
            Mo2Colors.Running,
            14f,
            true,
        ))
        val note = stage.optString("note")
        if (note.isNotBlank()) row.addView(label(note, muted, 12f, false))
        val plannedSpeed = stage.optDouble("planned_speed", speed)
        if (plannedSpeed > 0.0 && kotlin.math.abs(plannedSpeed - speed) >= 0.05) {
            row.addView(label("Planejado: " + formatSpeed(plannedSpeed), muted, 12f, false))
        }
        val edit = actionButton("Editar etapa", surface, Mo2Colors.Running)
        edit.setOnClickListener { showEditRunStageDialog(run, index) }
        row.addView(buttonParams(edit))
        return row
    }

    private fun toggleHistoryExpansion(preference: String, key: String, expanded: Boolean) {
        prefs.edit().putString(preference, if (expanded) "" else key).apply()
        render()
    }

    private fun showEditStrengthHistoryDialog(item: JSONObject) {
        val content = LinearLayout(this)
        content.orientation = LinearLayout.VERTICAL
        content.setPadding(dp(14), dp(14), dp(14), dp(8))
        content.addView(label("EDITAR MUSCULACAO", green, 13f, true))
        content.addView(label(item.optString("exercise", "Exercicio"), white, 22f, true))

        val day = input("Data yyyy-mm-dd", item.optString("day"))
        val time = input("Hora hh:mm", item.optString("time"))
        content.addView(historyEditorRow(day, time))
        val plan = input("Treino", item.optString("plan"))
        val exercise = input("Exercicio", item.optString("exercise"))
        content.addView(plan)
        content.addView(exercise)

        val reps = input("Repeticoes", item.optString("reps"))
        reps.inputType = InputType.TYPE_CLASS_NUMBER
        val load = input("Carga kg", item.optString("load"))
        load.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        content.addView(historyEditorRow(reps, load))

        val rir = input("RIR", item.optString("rir"))
        rir.inputType = InputType.TYPE_CLASS_NUMBER
        val rpe = input("RPE 1-10", item.optString("rpe"))
        rpe.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        content.addView(historyEditorRow(rir, rpe))
        val notes = textArea("Observacoes", item.optString("notes"))
        content.addView(notes)

        val scroll = ScrollView(this)
        scroll.addView(content)
        val dialog = AlertDialog.Builder(this)
            .setView(scroll)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Salvar", null)
            .create()
        dialog.setOnShowListener {
            styleHistoryDialog(dialog, content)
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                if (updateStrengthHistoryItem(
                        item,
                        day.textValue(),
                        time.textValue(),
                        plan.textValue(),
                        exercise.textValue(),
                        reps.textValue(),
                        load.textValue(),
                        rir.textValue(),
                        rpe.textValue(),
                        notes.textValue(),
                    )) {
                    dialog.dismiss()
                    render()
                }
            }
        }
        dialog.show()
    }

    private fun showEditRunHistoryDialog(item: JSONObject) {
        val content = LinearLayout(this)
        content.orientation = LinearLayout.VERTICAL
        content.setPadding(dp(14), dp(14), dp(14), dp(8))
        content.addView(label("EDITAR CORRIDA", Mo2Colors.Running, 13f, true))
        content.addView(label(item.optString("workout_title", "Corrida"), white, 22f, true))

        val day = input("Data yyyy-mm-dd", item.optString("day"))
        val time = input("Hora hh:mm", item.optString("time"))
        content.addView(historyEditorRow(day, time))
        val title = input("Nome da atividade", item.optString("workout_title", "Corrida"))
        content.addView(title)

        val distance = input("Distancia km", item.optString("distance"))
        distance.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        val speed = input("Velocidade km/h", item.optString("speed"))
        speed.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        content.addView(historyEditorRow(distance, speed))

        val duration = input("Duracao mm:ss", item.optString("duration"))
        val rpe = input("RPE 1-10", item.optString("rpe"))
        rpe.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        content.addView(historyEditorRow(duration, rpe))
        val notes = textArea("Como foi a corrida?", item.optString("feedback_notes"))
        content.addView(notes)

        val scroll = ScrollView(this)
        scroll.addView(content)
        val dialog = AlertDialog.Builder(this)
            .setView(scroll)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Salvar", null)
            .create()
        dialog.setOnShowListener {
            styleHistoryDialog(dialog, content)
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                if (updateRunHistoryItem(
                        item,
                        day.textValue(),
                        time.textValue(),
                        title.textValue(),
                        distance.textValue(),
                        speed.textValue(),
                        duration.textValue(),
                        rpe.textValue(),
                        notes.textValue(),
                    )) {
                    dialog.dismiss()
                    render()
                }
            }
        }
        dialog.show()
    }

    private fun runStagesForLog(item: JSONObject): JSONArray {
        val stored = item.optJSONArray("stages")
        if (stored != null && stored.length() > 0) return JSONArray(stored.toString())
        val workout = runningWorkoutById(item.optString("run_workout_id"))
        if (workout != null) return runStageSnapshot(workout, false)
        val distance = item.optDouble("distance")
        val speed = item.optDouble("speed")
        if (distance <= 0.0 && speed <= 0.0) return JSONArray()
        return JSONArray().put(JSONObject()
            .put("title", item.optString("workout_title", "Corrida"))
            .put("distance", roundKm(distance))
            .put("speed", roundSpeed(speed))
            .put("note", "Registro sem etapas originais."))
    }

    private fun showEditRunStageDialog(item: JSONObject, stageIndex: Int) {
        val stages = runStagesForLog(item)
        if (stageIndex !in 0 until stages.length()) {
            Toast.makeText(this, "Etapa nao encontrada.", Toast.LENGTH_SHORT).show()
            return
        }
        val stage = stages.getJSONObject(stageIndex)
        val content = LinearLayout(this)
        content.orientation = LinearLayout.VERTICAL
        content.setPadding(dp(14), dp(14), dp(14), dp(8))
        content.addView(label("EDITAR ETAPA " + (stageIndex + 1), Mo2Colors.Running, 13f, true))
        content.addView(label(stage.optString("title", "Etapa"), white, 22f, true))
        val title = input("Nome da etapa", stage.optString("title", "Etapa"))
        content.addView(title)
        val distance = input("Distancia km", stage.optString("distance"))
        distance.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        val speed = input("Velocidade km/h", stage.optString("speed"))
        speed.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        content.addView(historyEditorRow(distance, speed))
        val note = textArea("Observacao da etapa", stage.optString("note"))
        content.addView(note)
        content.addView(label("A alteracao recalcula o total da corrida a partir das etapas salvas.", muted, 13f, false))

        val dialog = AlertDialog.Builder(this)
            .setView(content)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Salvar", null)
            .create()
        dialog.setOnShowListener {
            styleHistoryDialog(dialog, content)
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                if (updateRunStageDetails(item, stageIndex, title.textValue(), distance.textValue(), speed.textValue(), note.textValue())) {
                    dialog.dismiss()
                    render()
                }
            }
        }
        dialog.show()
    }

    private fun updateRunStageDetails(
        target: JSONObject,
        stageIndex: Int,
        titleRaw: String,
        distanceRaw: String,
        speedRaw: String,
        noteRaw: String,
    ): Boolean {
        val distance = distanceRaw.replace(',', '.').toDoubleOrNull()
        val speed = speedRaw.replace(',', '.').toDoubleOrNull()
        if (titleRaw.isBlank()) {
            Toast.makeText(this, "Informe o nome da etapa.", Toast.LENGTH_SHORT).show()
            return false
        }
        if (distance == null || distance <= 0.0) {
            Toast.makeText(this, "Informe uma distancia maior que zero.", Toast.LENGTH_SHORT).show()
            return false
        }
        if (speed == null || speed !in 1.0..30.0) {
            Toast.makeText(this, "Informe uma velocidade entre 1 e 30 km/h.", Toast.LENGTH_SHORT).show()
            return false
        }
        val logs = runLogs()
        val index = findHistoryRecordIndex(logs, target, "run")
        if (index < 0) {
            Toast.makeText(this, "Corrida nao encontrada no historico.", Toast.LENGTH_SHORT).show()
            return false
        }
        val updated = logs.getJSONObject(index)
        val stages = runStagesForLog(updated)
        if (stageIndex !in 0 until stages.length()) {
            Toast.makeText(this, "Etapa nao encontrada.", Toast.LENGTH_SHORT).show()
            return false
        }
        val stage = stages.getJSONObject(stageIndex)
            .put("title", titleRaw.trim())
            .put("distance", roundKm(distance))
            .put("speed", roundSpeed(speed))
            .put("note", noteRaw.trim())
            .put("edited_at", timestamp())
        stages.put(stageIndex, stage)
        applyRunStageTotals(updated, stages)
        updated.put("stages", stages).put("edited_at", timestamp())
        logs.put(index, updated)
        prefs.edit().putString("run_logs", logs.toString()).apply()
        Toast.makeText(this, "Etapa atualizada.", Toast.LENGTH_SHORT).show()
        return true
    }

    private fun applyRunStageTotals(run: JSONObject, stages: JSONArray) {
        var distance = 0.0
        var durationSeconds = 0L
        for (index in 0 until stages.length()) {
            val stage = stages.getJSONObject(index)
            val stageDistance = stage.optDouble("distance").coerceAtLeast(0.0)
            val stageSpeed = stage.optDouble("speed").coerceAtLeast(0.0)
            distance += stageDistance
            if (stageDistance > 0.0 && stageSpeed > 0.0) {
                durationSeconds += ((stageDistance / stageSpeed) * 3600.0).roundToInt().toLong().coerceAtLeast(1L)
            }
        }
        val safeDistance = roundKm(distance)
        val safeDuration = durationSeconds.coerceAtLeast(1L)
        val avgSpeed = if (safeDistance <= 0.0) 0.0 else safeDistance / (safeDuration.toDouble() / 3600.0)
        run
            .put("distance", safeDistance)
            .put("speed", roundSpeed(avgSpeed))
            .put("duration", formatDuration(safeDuration))
            .put("duration_seconds", safeDuration)
            .put("pace_seconds_per_km", if (safeDistance <= 0.0) 0L else (safeDuration.toDouble() / safeDistance).roundToInt().toLong())
            .put("total_stages", stages.length())
            .put("completed_stages", run.optInt("completed_stages", stages.length()).coerceIn(0, stages.length()))
    }

    private fun historyEditorRow(first: EditText, second: EditText): View {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.addView(first, LinearLayout.LayoutParams(0, dp(54), 1f))
        row.addView(second, LinearLayout.LayoutParams(0, dp(54), 1f))
        return spacedRow(row)
    }

    private fun styleHistoryDialog(dialog: AlertDialog, content: View) {
        dialog.window?.setBackgroundDrawable(Mo2Drawables.rounded(this, surface, Mo2Radius.Modal, border))
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(green)
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(muted)
        content.startAnimation(smoothPopupAnimation())
    }

    private fun updateStrengthHistoryItem(
        target: JSONObject,
        dayRaw: String,
        timeRaw: String,
        planRaw: String,
        exerciseRaw: String,
        repsRaw: String,
        loadRaw: String,
        rirRaw: String,
        rpeRaw: String,
        notesRaw: String,
    ): Boolean {
        val originalDay = target.optString("day")
        val originalPlan = target.optString("plan", "Treino")
        val day = dayRaw.trim()
        val reps = repsRaw.trim().toIntOrNull()
        val load = loadRaw.replace(',', '.').toDoubleOrNull()
        val rir = rirRaw.trim().toIntOrNull()
        val rpe = rpeRaw.replace(',', '.').toDoubleOrNull()
        if (!isValidHistoryDay(day)) {
            Toast.makeText(this, "Use a data no formato yyyy-mm-dd.", Toast.LENGTH_SHORT).show()
            return false
        }
        if (exerciseRaw.isBlank() || reps == null || reps <= 0 || load == null || load < 0.0) {
            Toast.makeText(this, "Revise exercicio, repeticoes e carga.", Toast.LENGTH_SHORT).show()
            return false
        }
        if (rirRaw.isNotBlank() && (rir == null || rir !in 0..10)) {
            Toast.makeText(this, "RIR precisa estar entre 0 e 10.", Toast.LENGTH_SHORT).show()
            return false
        }
        if (rpeRaw.isNotBlank() && (rpe == null || rpe !in 1.0..10.0)) {
            Toast.makeText(this, "RPE precisa estar entre 1 e 10.", Toast.LENGTH_SHORT).show()
            return false
        }

        val logs = allLogs()
        val index = findHistoryRecordIndex(logs, target, "strength")
        if (index < 0) {
            Toast.makeText(this, "Serie nao encontrada no historico.", Toast.LENGTH_SHORT).show()
            return false
        }
        val updated = logs.getJSONObject(index)
        if (updated.optString("id").isBlank()) updated.put("id", UUID.randomUUID().toString())
        updated
            .put("day", day)
            .put("week", weekKeyForDay(day))
            .put("time", timeRaw.trim().ifBlank { "00:00" })
            .put("plan", planRaw.trim().ifBlank { "Treino" })
            .put("exercise", exerciseRaw.trim())
            .put("reps", reps)
            .put("load", load)
            .put("notes", notesRaw.trim())
            .put("edited_at", timestamp())
        if (rir != null) updated.put("rir", rir) else updated.remove("rir")
        if (rpe != null) updated.put("rpe", rpe) else updated.remove("rpe")
        logs.put(index, updated)
        prefs.edit().putString("set_logs", logs.toString()).apply()
        refreshStrengthSessionSummary(originalDay, originalPlan)
        val updatedPlan = updated.optString("plan", "Treino")
        if (day != originalDay || updatedPlan != originalPlan) refreshStrengthSessionSummary(day, updatedPlan)
        Toast.makeText(this, "Serie atualizada.", Toast.LENGTH_SHORT).show()
        return true
    }

    private fun updateRunHistoryItem(
        target: JSONObject,
        dayRaw: String,
        timeRaw: String,
        titleRaw: String,
        distanceRaw: String,
        speedRaw: String,
        durationRaw: String,
        rpeRaw: String,
        notesRaw: String,
    ): Boolean {
        val day = dayRaw.trim()
        val distance = distanceRaw.replace(',', '.').toDoubleOrNull()
        val speedInput = speedRaw.replace(',', '.').toDoubleOrNull()
        val durationInput = parseHistoryDurationSeconds(durationRaw)
        val rpe = rpeRaw.replace(',', '.').toDoubleOrNull()
        if (!isValidHistoryDay(day)) {
            Toast.makeText(this, "Use a data no formato yyyy-mm-dd.", Toast.LENGTH_SHORT).show()
            return false
        }
        if (distance == null || distance <= 0.0) {
            Toast.makeText(this, "Informe uma distancia maior que zero.", Toast.LENGTH_SHORT).show()
            return false
        }
        if ((speedInput == null || speedInput <= 0.0) && durationInput <= 0L) {
            Toast.makeText(this, "Informe velocidade ou duracao valida.", Toast.LENGTH_SHORT).show()
            return false
        }
        if (durationInput <= 0L && (speedInput == null || speedInput !in 1.0..30.0)) {
            Toast.makeText(this, "Velocidade precisa estar entre 1 e 30 km/h.", Toast.LENGTH_SHORT).show()
            return false
        }
        if (rpeRaw.isNotBlank() && (rpe == null || rpe !in 1.0..10.0)) {
            Toast.makeText(this, "RPE precisa estar entre 1 e 10.", Toast.LENGTH_SHORT).show()
            return false
        }

        val durationSeconds = if (durationInput > 0L) {
            durationInput
        } else {
            ((distance / speedInput!!) * 3600.0).roundToInt().toLong().coerceAtLeast(1L)
        }
        val speed = if (durationInput > 0L) {
            distance / (durationSeconds.toDouble() / 3600.0)
        } else {
            speedInput!!
        }
        val paceSeconds = (durationSeconds.toDouble() / distance).roundToInt().toLong()
        if (!isPlausibleRunningPace(paceSeconds)) {
            Toast.makeText(this, "A duracao precisa equivaler a um ritmo entre 2:00 e 30:00 por km.", Toast.LENGTH_SHORT).show()
            return false
        }
        val logs = runLogs()
        val index = findHistoryRecordIndex(logs, target, "run")
        if (index < 0) {
            Toast.makeText(this, "Corrida nao encontrada no historico.", Toast.LENGTH_SHORT).show()
            return false
        }
        val updated = logs.getJSONObject(index)
        if (updated.optString("id").isBlank()) updated.put("id", UUID.randomUUID().toString())
        updated
            .put("day", day)
            .put("week", weekKeyForDay(day))
            .put("time", timeRaw.trim().ifBlank { "00:00" })
            .put("workout_title", titleRaw.trim().ifBlank { "Corrida" })
            .put("distance", roundKm(distance))
            .put("speed", roundSpeed(speed))
            .put("duration", formatDuration(durationSeconds))
            .put("duration_seconds", durationSeconds)
            .put("pace_seconds_per_km", paceSeconds)
            .put("feedback_notes", notesRaw.trim())
            .put("edited_at", timestamp())
        if (rpe != null) updated.put("rpe", rpe) else updated.remove("rpe")
        logs.put(index, updated)
        prefs.edit().putString("run_logs", logs.toString()).apply()
        Toast.makeText(this, "Corrida atualizada.", Toast.LENGTH_SHORT).show()
        return true
    }

    private fun showDeleteStrengthHistorySessionDialog(activity: HistoryActivity) {
        val plan = activity.item.optString("plan", "Treino")
        val dialog = AlertDialog.Builder(this)
            .setTitle("Excluir treino completo?")
            .setMessage(plan + " e todas as suas series deste dia serao removidos do historico local.")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Excluir treino") { _, _ -> deleteStrengthHistorySession(activity) }
            .create()
        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawable(Mo2Drawables.rounded(this, surface, Mo2Radius.Modal, border))
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(danger)
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(muted)
        }
        dialog.show()
    }

    private fun deleteStrengthHistorySession(activity: HistoryActivity) {
        val day = activity.item.optString("day")
        val plan = activity.item.optString("plan", "Treino")
        val setLogs = allLogs()
        for (index in setLogs.length() - 1 downTo 0) {
            val item = setLogs.getJSONObject(index)
            if (item.optString("day") == day && item.optString("plan") == plan) setLogs.remove(index)
        }
        val sessions = strengthSessionLogs()
        for (index in sessions.length() - 1 downTo 0) {
            val item = sessions.getJSONObject(index)
            if (item.optString("day") == day && item.optString("plan_title") == plan) sessions.remove(index)
        }
        val editor = prefs.edit()
            .putString("set_logs", setLogs.toString())
            .putString("strength_session_logs", sessions.toString())
            .remove("history_expanded_strength")
        if (day == dayKey() && prefs.getString("last_finished_day", "") == day) editor.remove("last_finished_day")
        editor.apply()
        Toast.makeText(this, "Treino excluido do historico.", Toast.LENGTH_SHORT).show()
        render()
    }

    private fun showDeleteHistoryActivityDialog(activity: HistoryActivity) {
        val item = activity.item
        val title = if (activity.type == "run") item.optString("workout_title", "Corrida") else item.optString("exercise", "Serie")
        val dialog = AlertDialog.Builder(this)
            .setTitle("Excluir atividade?")
            .setMessage(title + " sera removido definitivamente do historico local.")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Excluir") { _, _ -> deleteHistoryActivity(activity) }
            .create()
        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawable(Mo2Drawables.rounded(this, surface, Mo2Radius.Modal, border))
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(danger)
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(muted)
        }
        dialog.show()
    }

    private fun deleteHistoryActivity(activity: HistoryActivity) {
        val isRun = activity.type == "run"
        val strengthDay = activity.item.optString("day")
        val strengthPlan = activity.item.optString("plan", "Treino")
        val logs = if (isRun) runLogs() else allLogs()
        val index = findHistoryRecordIndex(logs, activity.item, activity.type)
        if (index < 0) {
            Toast.makeText(this, "Atividade nao encontrada.", Toast.LENGTH_SHORT).show()
            return
        }
        logs.remove(index)
        prefs.edit().putString(if (isRun) "run_logs" else "set_logs", logs.toString()).apply()
        if (!isRun) refreshStrengthSessionSummary(strengthDay, strengthPlan)
        Toast.makeText(this, "Atividade excluida.", Toast.LENGTH_SHORT).show()
        render()
    }

    private fun findHistoryRecordIndex(logs: JSONArray, target: JSONObject, type: String): Int {
        val id = target.optString("id")
        for (index in 0 until logs.length()) {
            val candidate = logs.getJSONObject(index)
            if (id.isNotBlank() && candidate.optString("id") == id) return index
            val sameBase = candidate.optString("day") == target.optString("day") &&
                candidate.optString("time") == target.optString("time")
            if (!sameBase) continue
            if (type == "run") {
                if (candidate.optString("workout_title") == target.optString("workout_title") &&
                    candidate.optDouble("distance") == target.optDouble("distance")) return index
            } else {
                if (candidate.optString("plan") == target.optString("plan") &&
                    candidate.optString("exercise") == target.optString("exercise") &&
                    candidate.optInt("reps") == target.optInt("reps") &&
                    candidate.optDouble("load") == target.optDouble("load")) return index
            }
        }
        return -1
    }

    private fun isValidHistoryDay(day: String): Boolean {
        if (!Regex("\\d{4}-\\d{2}-\\d{2}").matches(day)) return false
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            parser.isLenient = false
            parser.parse(day) != null
        } catch (_: Exception) {
            false
        }
    }

    private fun weekKeyForDay(day: String): String {
        return try {
            val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(day) ?: return weekKey()
            SimpleDateFormat("yyyy-ww", Locale.US).format(parsed)
        } catch (_: Exception) {
            weekKey()
        }
    }

    private fun parseHistoryDurationSeconds(raw: String): Long {
        val value = raw.trim().lowercase(Locale.US)
        if (value.isBlank()) return 0L
        if (value.contains(":")) {
            val parts = value.split(":").mapNotNull { it.trim().toLongOrNull() }
            return when (parts.size) {
                2 -> parts[0] * 60L + parts[1]
                3 -> parts[0] * 3600L + parts[1] * 60L + parts[2]
                else -> 0L
            }
        }
        val numbers = Regex("\\d+").findAll(value).mapNotNull { it.value.toLongOrNull() }.toList()
        if (numbers.isEmpty()) return 0L
        return when {
            value.contains("h") -> numbers[0] * 3600L + (numbers.getOrNull(1) ?: 0L) * 60L
            value.contains("seg") -> numbers[0]
            else -> numbers[0] * 60L
        }
    }

    private fun filteredStrengthSessionHistory(): List<JSONObject> {
        val type = prefs.getString("history_type", "all") ?: "all"
        if (type == "run") return emptyList()
        val sessions = strengthSessionLogs()
        val items = mutableListOf<JSONObject>()
        for (index in sessions.length() - 1 downTo 0) {
            val item = sessions.getJSONObject(index)
            val haystack = listOf(
                item.optString("plan_title"),
                item.optString("plan_focus"),
                jsonStringList(item.optJSONArray("completed_exercises")).joinToString(" "),
                jsonStringList(item.optJSONArray("partial_exercises")).joinToString(" "),
                jsonStringList(item.optJSONArray("skipped_exercises")).joinToString(" "),
            ).joinToString(" ")
            if (matchesHistoryFilters(item.optString("day"), haystack)) items.add(item)
        }
        return items
    }

    private fun filteredStrengthHistory(): List<JSONObject> {
        val type = prefs.getString("history_type", "all") ?: "all"
        if (type == "run") return emptyList()
        val logs = allLogs()
        val items = mutableListOf<JSONObject>()
        for (i in logs.length() - 1 downTo 0) {
            val item = logs.getJSONObject(i)
            val haystack = listOf(
                item.optString("plan"),
                item.optString("exercise"),
                item.optString("notes"),
            ).joinToString(" ")
            if (matchesHistoryFilters(item.optString("day"), haystack)) items.add(item)
        }
        return items
    }

    private fun filteredRunHistory(): List<JSONObject> {
        val type = prefs.getString("history_type", "all") ?: "all"
        if (type == "strength") return emptyList()
        val logs = runLogs()
        val items = mutableListOf<JSONObject>()
        for (i in logs.length() - 1 downTo 0) {
            val item = logs.getJSONObject(i)
            val haystack = listOf(
                item.optString("workout_title"),
                item.optString("notes"),
                item.optString("feedback_notes"),
                item.optString("duration"),
                item.optString("rpe"),
                item.optJSONArray("stages")?.toString().orEmpty(),
            ).joinToString(" ")
            if (matchesHistoryFilters(item.optString("day"), haystack)) items.add(item)
        }
        return items
    }

    private fun matchesHistoryFilters(day: String, haystack: String): Boolean {
        val from = prefs.getString("history_from", "") ?: ""
        val to = prefs.getString("history_to", "") ?: ""
        val query = prefs.getString("history_query", "") ?: ""
        if (from.isNotBlank() && day < from) return false
        if (to.isNotBlank() && day > to) return false
        if (query.isBlank()) return true
        return normalized(haystack).contains(normalized(query))
    }

    private fun filteredHistoryDays(strengthLogs: List<JSONObject>, runs: List<JSONObject>): Int {
        val days = mutableSetOf<String>()
        strengthLogs.forEach { days.add(it.optString("day")) }
        runs.forEach { days.add(it.optString("day")) }
        return days.count { it.isNotBlank() }
    }

    private fun formatLoad(value: Double): String {
        return if (value % 1.0 == 0.0) value.toInt().toString() + " kg" else String.format(Locale("pt", "BR"), "%.1f kg", value)
    }

    private fun renderStats(root: LinearLayout) {
        val stats = computeStats(allLogs())
        root.addView(sectionTitle("Central de evolucao"))
        root.addView(v12EvolutionPanel())
        root.addView(v12MuscleVolumePanel())
        root.addView(v12CurrentProgressionPanel())
        root.addView(metricGrid(listOf(
            Pair("Series totais", stats.optInt("total_sets").toString()),
            Pair("Volume total", stats.optInt("total_volume").toString() + " kg"),
            Pair("Series semana", stats.optInt("week_sets").toString()),
            Pair("Volume hoje", stats.optInt("today_volume").toString() + " kg"),
            Pair("RPE medio", stats.optString("avg_rpe")),
            Pair("Melhor carga", stats.optString("best_load")),
        )))

        val coach = card(surface3)
        coach.orientation = LinearLayout.VERTICAL
        coach.addView(label("COACH INTELIGENTE", green, 13f, true))
        smartCoachLines().forEach { line -> coach.addView(label(line, white, 15f, false)) }
        root.addView(coach)

        val prs = card()
        prs.orientation = LinearLayout.VERTICAL
        prs.addView(label("TOP EXERCICIOS POR CARGA", green, 13f, true))
        bestLoadsByExercise().forEach { line -> prs.addView(label(line, white, 15f, false)) }
        root.addView(prs)
    }

    private fun v12EvolutionPanel(): View {
        val current = periodSummary(historyDateOffset(-6), dayKey())
        val previous = periodSummary(historyDateOffset(-13), historyDateOffset(-7))
        val setTrend = Mo2ProgressEngine.trend(current.optDouble("sets"), previous.optDouble("sets"))
        val volumeTrend = Mo2ProgressEngine.trend(current.optDouble("volume"), previous.optDouble("volume"))
        val runTrend = Mo2ProgressEngine.trend(current.optDouble("run_distance"), previous.optDouble("run_distance"))
        val consistency = Mo2ProgressEngine.consistencyScore(
            current.optInt("strength_sessions") + current.optInt("runs"),
            8,
            current.optInt("active_days"),
            5,
        )

        val box = card(surface3)
        box.orientation = LinearLayout.VERTICAL
        box.addView(label("EVOLUCAO V12", green, 13f, true))
        box.addView(label("Ultimos 7 dias", white, 27f, true))
        box.addView(label("Comparacao com os 7 dias anteriores usando somente registros salvos no aparelho.", muted, 14f, false))

        val metrics = LinearLayout(this)
        metrics.orientation = LinearLayout.HORIZONTAL
        metrics.addView(compactMetric("Consistencia", consistency.toString() + "%"), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        metrics.addView(compactMetric("Treinos", (current.optInt("strength_sessions") + current.optInt("runs")).toString()), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        metrics.addView(compactMetric("Dias ativos", current.optInt("active_days").toString()), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        box.addView(spacedRow(metrics))

        box.addView(v12TrendLine("Series", current.optInt("sets").toString(), setTrend))
        box.addView(v12TrendLine("Volume", current.optInt("volume").toString() + " kg", volumeTrend))
        box.addView(v12TrendLine("Corrida", formatKm(current.optDouble("run_distance")), runTrend))

        val copy = actionButton("Copiar relatorio V12", surface2, green)
        copy.setOnClickListener { copyV12Report(current, previous, consistency) }
        box.addView(buttonParams(copy))
        return box
    }

    private fun v12TrendLine(title: String, value: String, trend: Mo2Trend): View {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        row.setPadding(0, dp(12), 0, 0)
        row.addView(label(title + ": " + value, white, 15f, true), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        val trendText = trend.percent?.let { percent ->
            (if (percent > 0) "+" else "") + percent + "%"
        } ?: trend.direction
        val color = when (trend.direction) {
            "subiu" -> green
            "caiu" -> amber
            else -> muted
        }
        row.addView(label(trendText, color, 14f, true))
        return row
    }

    private fun periodSummary(from: String, to: String): JSONObject {
        var sets = 0
        var volume = 0.0
        var strengthSessions = 0
        var runs = 0
        var runDistance = 0.0
        val activeDays = mutableSetOf<String>()

        val setLogs = allLogs()
        for (index in 0 until setLogs.length()) {
            val item = setLogs.getJSONObject(index)
            val day = item.optString("day")
            if (day !in from..to) continue
            sets += 1
            volume += item.optDouble("load") * item.optInt("reps")
            if (day.isNotBlank()) activeDays.add(day)
        }
        val sessions = strengthSessionLogs()
        for (index in 0 until sessions.length()) {
            val item = sessions.getJSONObject(index)
            val day = item.optString("day")
            if (day in from..to && item.optString("status", "completed") == "completed") strengthSessions += 1
        }
        val runItems = runLogs()
        for (index in 0 until runItems.length()) {
            val item = runItems.getJSONObject(index)
            val day = item.optString("day")
            if (day !in from..to) continue
            runs += 1
            runDistance += item.optDouble("distance")
            if (day.isNotBlank()) activeDays.add(day)
        }
        return JSONObject()
            .put("sets", sets)
            .put("volume", volume.roundToInt())
            .put("strength_sessions", strengthSessions)
            .put("runs", runs)
            .put("run_distance", roundKm(runDistance))
            .put("active_days", activeDays.size)
    }

    private fun v12MuscleVolumePanel(): View {
        val volumes = muscleVolumes(historyDateOffset(-27), dayKey())
        val box = card(surface)
        box.orientation = LinearLayout.VERTICAL
        box.addView(label("VOLUME POR GRUPO", green, 13f, true))
        box.addView(label("Ultimos 28 dias", white, 22f, true))
        if (volumes.isEmpty()) {
            box.addView(label("Registre series com carga para liberar a comparacao muscular.", muted, 14f, false))
            return box
        }
        val maximum = volumes.values.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
        volumes.entries.sortedByDescending { it.value }.take(7).forEach { entry ->
            val percent = ((entry.value / maximum) * 100.0).roundToInt()
            box.addView(dashboardProgressLine(entry.key, entry.value.roundToInt().toString() + " kg", percent, green))
        }
        box.addView(label(Mo2ProgressEngine.volumeBalance(volumes), muted, 14f, false))
        return box
    }

    private fun muscleVolumes(from: String, to: String): Map<String, Double> {
        val result = mutableMapOf<String, Double>()
        val muscleByExercise = mutableMapOf<String, String>()
        val logs = allLogs()
        for (index in 0 until logs.length()) {
            val item = logs.getJSONObject(index)
            val day = item.optString("day")
            if (day !in from..to) continue
            val exercise = item.optString("exercise", "Outro")
            val muscle = muscleByExercise.getOrPut(exercise) {
                catalogMatchForWorkoutExercise(exercise)?.muscle ?: "Outros"
            }
            val volume = item.optDouble("load") * item.optInt("reps")
            result[muscle] = (result[muscle] ?: 0.0) + volume
        }
        return result.filterValues { it > 0.0 }
    }

    private fun v12CurrentProgressionPanel(): View {
        val exercise = currentExercise()
        val adjustment = strengthAdjustmentFor(exercise)
        val box = card(surface2)
        box.orientation = LinearLayout.VERTICAL
        box.addView(label("PROXIMA PROGRESSAO", green, 13f, true))
        box.addView(label(exercise.name, white, 22f, true))
        box.addView(label("Carga sugerida: " + formatLoad(adjustment.nextLoad), white, 16f, true))
        box.addView(label("Volume sugerido: " + adjustment.suggestedSetCount + " series", white, 15f, true))
        box.addView(label(adjustment.loadReason, muted, 14f, false))
        box.addView(label(adjustment.volumeReason, muted, 14f, false))
        val open = actionButton("Abrir exercicio no treino", green, bg)
        open.setOnClickListener { switchTab("workout") }
        box.addView(buttonParams(open))
        return box
    }

    private fun copyV12Report(current: JSONObject, previous: JSONObject, consistency: Int) {
        val report = listOf(
            "Mo2 LOG v" + versionName + " - Relatorio pessoal",
            "Periodo: " + historyDateOffset(-6) + " a " + dayKey(),
            "Consistencia: " + consistency + "%",
            "Series: " + current.optInt("sets") + " (anterior " + previous.optInt("sets") + ")",
            "Volume: " + current.optInt("volume") + " kg (anterior " + previous.optInt("volume") + " kg)",
            "Corrida: " + formatKm(current.optDouble("run_distance")) + " (anterior " + formatKm(previous.optDouble("run_distance")) + ")",
            "Previsao 5 km: " + formatDuration(runningFiveKmForecast().predictedSeconds),
        ).joinToString("\n")
        copyTextToClipboard("Mo2 LOG relatorio V12", report, "Relatorio V12 copiado.")
    }

    private fun renderExercises(root: LinearLayout) {
        syncCatalogPrefs()
        root.addView(sectionTitle("Catalogo de exercicios"))

        val catalogItems = catalog
        if (catalogItems.isEmpty()) {
            val empty = card()
            empty.orientation = LinearLayout.VERTICAL
            empty.addView(label("Catalogo indisponivel", white, 20f, true))
            empty.addView(label("Nao foi possivel carregar o asset local de exercicios.", muted, 15f, false))
            root.addView(empty)
            return
        }

        val summary = card(surface3)
        summary.orientation = LinearLayout.VERTICAL
        summary.addView(label("BIBLIOTECA DO TREINO", green, 13f, true))
        val favoriteIds = favoriteCatalogIds()
        val hiddenIds = hiddenCatalogIds()
        summary.addView(label(catalogItems.size.toString() + " exercicios", white, 25f, true))
        summary.addView(label(favoriteIds.size.toString() + " favoritos | " + hiddenIds.size + " ocultos. Busque por nome, musculo ou equipamento.", muted, 15f, false))
        summary.addView(label(mediaCacheFileCount().toString() + " frames | " + mediaCacheSizeLabel() + " em cache local.", muted, 14f, false))
        val cacheRow = LinearLayout(this)
        cacheRow.orientation = LinearLayout.HORIZONTAL
        val preload = actionButton("Preparar treino", green, bg)
        preload.setOnClickListener { prefetchCurrentWorkoutMedia() }
        cacheRow.addView(preload, LinearLayout.LayoutParams(0, dp(50), 1f))
        val clearCache = actionButton("Limpar cache", surface2, white)
        clearCache.setOnClickListener { clearMediaCache() }
        cacheRow.addView(clearCache, LinearLayout.LayoutParams(0, dp(50), 1f))
        summary.addView(spacedRow(cacheRow))
        root.addView(summary)

        val muscleOptions = listOf("Todos", "Favoritos", "Ocultos") + catalogItems.map { it.muscle }.distinct().sorted()
        val savedMuscle = prefs.getString("catalog_muscle", "Todos") ?: "Todos"
        val selectedMuscle = if (muscleOptions.contains(savedMuscle)) savedMuscle else "Todos"
        val currentQuery = prefs.getString("catalog_query", "") ?: ""
        val search = input("Buscar por nome, musculo ou equipamento", currentQuery)
        root.addView(search)

        val searchRow = LinearLayout(this)
        searchRow.orientation = LinearLayout.HORIZONTAL
        val applySearch = actionButton("Buscar", green, bg)
        applySearch.setOnClickListener {
            prefs.edit()
                .putString("catalog_query", search.textValue())
                .putInt("catalog_result_limit", 30)
                .remove("catalog_selected")
                .apply()
            hideKeyboard()
            render()
        }
        searchRow.addView(applySearch, LinearLayout.LayoutParams(0, dp(50), 1f))
        val clearSearch = actionButton("Limpar", surface2, white)
        clearSearch.setOnClickListener {
            prefs.edit()
                .remove("catalog_query")
                .remove("catalog_selected")
                .putInt("catalog_result_limit", 30)
                .apply()
            hideKeyboard()
            render()
        }
        searchRow.addView(clearSearch, LinearLayout.LayoutParams(0, dp(50), 1f))
        root.addView(searchRow)

        val muscles = muscleOptions
        val muscleScroll = HorizontalScrollView(this)
        muscleScroll.isHorizontalScrollBarEnabled = false
        val muscleRow = LinearLayout(this)
        muscleRow.orientation = LinearLayout.HORIZONTAL
        muscles.forEach { muscle ->
            val active = selectedMuscle == muscle
            val button = pill(muscle, active, 178, 50)
            button.setOnClickListener {
                prefs.edit()
                    .putString("catalog_muscle", muscle)
                    .putInt("catalog_result_limit", 30)
                    .remove("catalog_selected")
                    .apply()
                render()
            }
            muscleRow.addView(button)
        }
        muscleScroll.addView(muscleRow)
        root.addView(muscleScroll)

        val filtered = catalogItems
            .filter {
                when (selectedMuscle) {
                    "Todos" -> !hiddenIds.contains(it.id)
                    "Favoritos" -> favoriteIds.contains(it.id) && !hiddenIds.contains(it.id)
                    "Ocultos" -> hiddenIds.contains(it.id)
                    else -> it.muscle == selectedMuscle
                }
            }
            .filter { selectedMuscle == "Ocultos" || !hiddenIds.contains(it.id) }
            .filter { matchesCatalogQuery(it, currentQuery) }

        if (filtered.isEmpty()) {
            val empty = card()
            empty.orientation = LinearLayout.VERTICAL
            empty.addView(label("Nenhum exercicio encontrado", white, 20f, true))
            empty.addView(label("Tente outro grupo muscular ou ajuste a busca.", muted, 15f, false))
            root.addView(empty)
            return
        }

        val selectedId = prefs.getString("catalog_selected", null)
        val selected = filtered.firstOrNull { it.id == selectedId } ?: filtered.first()

        val detail = card()
        detail.orientation = LinearLayout.VERTICAL
        detail.addView(label("MIDIA DE EXECUCAO POR LINK", green, 13f, true))
        detail.addView(label(selected.name, white, 25f, true))
        detail.addView(label(exerciseMeta(selected), muted, 14f, false))
        detail.addView(label(mediaHealthLabel(selected), muted, 13f, false))
        val favorite = favoriteIds.contains(selected.id)
        val favoriteButton = actionButton(if (favorite) "Remover dos favoritos" else "Adicionar aos favoritos", if (favorite) amber else surface2, if (favorite) bg else white)
        favoriteButton.setOnClickListener { toggleFavorite(selected) }
        detail.addView(buttonParams(favoriteButton))
        val hidden = hiddenIds.contains(selected.id)
        val hideButton = actionButton(if (hidden) "Restaurar no catalogo" else "Ocultar exercicio/midia", surface2, if (hidden) green else amber)
        hideButton.setOnClickListener { toggleHiddenCatalogExercise(selected) }
        detail.addView(buttonParams(hideButton))

        val media = RemoteExerciseMediaView(this, selected.links)
        val mediaParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(230))
        mediaParams.setMargins(0, dp(12), 0, dp(12))
        detail.addView(media, mediaParams)

        detail.addView(label("Descricao", green, 14f, true))
        detail.addView(label(selected.description.ifBlank { "Exercicio catalogado para " + selected.muscle + " com foco em " + selected.primary + "." }, white, 15f, false))

        if (selected.technicalCare.isNotBlank()) {
            detail.addView(label("Cuidados", green, 14f, true))
            detail.addView(label(selected.technicalCare, white, 15f, false))
        }

        detail.addView(label("Detalhes", green, 14f, true))
        detail.addView(label("Primario: " + selected.primary.ifBlank { "-" }, white, 14f, false))
        detail.addView(label("Secundarios: " + selected.secondary.ifBlank { "-" }, muted, 14f, false))
        detail.addView(label("Nivel: " + selected.level.ifBlank { "-" } + " | Tipo: " + selected.type.ifBlank { "-" }, muted, 14f, false))

        val preferred = preferredAlternativeFor(selected)
        detail.addView(label("Alternativos para o mesmo musculo", green, 14f, true))
        if (preferred != null) {
            detail.addView(label("Preferido: " + preferred.name, amber, 14f, true))
        }
        alternativesFor(selected).forEach { alternative ->
            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            val activePreferred = preferred?.id == alternative.id
            val alt = pill((if (activePreferred) "[pref] " else "") + alternative.name, false, 220, 48)
            alt.setOnClickListener {
                prefs.edit().putString("catalog_selected", alternative.id).apply()
                render()
            }
            row.addView(alt, LinearLayout.LayoutParams(0, dp(48), 1f))
            val prefer = actionButton("Preferir", surface2, if (activePreferred) amber else white)
            prefer.setOnClickListener { setPreferredAlternative(selected, alternative) }
            row.addView(prefer, LinearLayout.LayoutParams(dp(108), dp(48)))
            detail.addView(spacedRow(row))
        }

        val reps = input("Reps", "10")
        val load = input("Carga kg", lastLoadFor(selected.name))
        detail.addView(reps)
        detail.addView(load)
        val save = actionButton("Registrar este exercicio", green, bg)
        save.setOnClickListener {
            saveSet(selected.name, reps.textValue(), load.textValue(), "2", "8", "Catalogo: " + selected.muscle)
        }
        detail.addView(buttonParams(save))
        root.addView(detail)

        val list = card()
        list.orientation = LinearLayout.VERTICAL
        list.addView(label("EXERCICIOS DISPONIVEIS (" + filtered.size + ")", green, 13f, true))
        val resultLimit = prefs.getInt("catalog_result_limit", 30).coerceIn(30, 120)
        filtered.take(resultLimit).forEach { exercise ->
            val item = card(if (exercise.id == selected.id) surface2 else surface)
            item.orientation = LinearLayout.VERTICAL
            item.setOnClickListener {
                prefs.edit().putString("catalog_selected", exercise.id).apply()
                render()
            }
            val prefix = (if (favoriteIds.contains(exercise.id)) "[fav] " else "") + (if (hiddenIds.contains(exercise.id)) "[oculto] " else "")
            item.addView(label(prefix + exercise.name, white, 17f, true))
            item.addView(label(exerciseMeta(exercise), muted, 13f, false))
            list.addView(item)
        }
        if (filtered.size > resultLimit) {
            list.addView(label("Mostrando " + resultLimit + " de " + filtered.size + ". Use a busca ou carregue mais.", muted, 14f, false))
            if (resultLimit < 120) {
                val more = actionButton("Mostrar mais 30", surface2, green)
                more.setOnClickListener {
                    prefs.edit().putInt("catalog_result_limit", (resultLimit + 30).coerceAtMost(120)).apply()
                    render()
                }
                list.addView(buttonParams(more))
            } else {
                list.addView(label("Limite de 120 itens na tela. Use a busca para acessar os demais.", amber, 14f, true))
            }
        }
        root.addView(list)
    }

    private fun syncCatalogPrefs() {
        val catalogVersion = "8.5.0"
        if (prefs.getString("catalog_source_version", "") != catalogVersion) {
            prefs.edit()
                .putString("catalog_source_version", catalogVersion)
                .remove("catalog_muscle")
                .remove("catalog_selected")
                .remove("catalog_query")
                .apply()
        }
    }

    private fun loadExerciseCatalog(): List<CatalogExercise> {
        return try {
            val raw = assets.open("exercise_catalog.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
            val array = JSONArray(raw)
            (0 until array.length()).map { index ->
                val item = array.getJSONObject(index)
                CatalogExercise(
                    id = item.optString("id"),
                    name = item.optString("name"),
                    slug = item.optString("slug"),
                    muscle = item.optString("muscle"),
                    subgroup = item.optString("subgroup"),
                    movement = item.optString("movement"),
                    type = item.optString("type"),
                    level = item.optString("level"),
                    primary = item.optString("primary"),
                    secondary = item.optString("secondary"),
                    equipment = item.optString("equipment"),
                    alternatives = item.optString("alternatives"),
                    description = item.optString("description"),
                    technicalCare = item.optString("technicalCare"),
                    source = item.optString("source"),
                    sourceId = item.optString("sourceId"),
                    status = item.optString("status"),
                    review = item.optString("review"),
                    links = jsonStringList(item.optJSONArray("links")),
                )
            }.filter { it.name.isNotBlank() && it.muscle.isNotBlank() }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun jsonStringList(array: JSONArray?): List<String> {
        if (array == null) return emptyList()
        return (0 until array.length()).mapNotNull { index ->
            array.optString(index).takeIf { it.isNotBlank() }
        }
    }

    private fun alternativesFor(exercise: CatalogExercise): List<CatalogExercise> {
        val wanted = exercise.alternatives
            .split(";")
            .map { normalized(it) }
            .filter { it.length > 2 }

        val direct = catalog.filter { candidate ->
            candidate.id != exercise.id && wanted.any { token ->
                normalized(candidate.name).contains(token) ||
                    normalized(candidate.slug).contains(token) ||
                    normalized(candidate.primary).contains(token)
            }
        }

        val sameMuscle = catalog.filter { candidate ->
            candidate.id != exercise.id && candidate.muscle == exercise.muscle
        }

        return (direct + sameMuscle).distinctBy { it.id }.take(8)
    }

    private fun matchesCatalogQuery(exercise: CatalogExercise, query: String): Boolean {
        val tokens = normalized(query).split(" ").filter { it.isNotBlank() }
        if (tokens.isEmpty()) return true
        val haystack = normalized(listOf(
            exercise.name,
            exercise.slug,
            exercise.muscle,
            exercise.subgroup,
            exercise.primary,
            exercise.secondary,
            exercise.equipment,
            exercise.movement,
        ).joinToString(" "))
        return tokens.all { haystack.contains(it) }
    }

    private fun catalogMatchForWorkoutExercise(name: String): CatalogExercise? {
        val hidden = hiddenCatalogIds()
        val available = catalog.filter { !hidden.contains(it.id) }
        val aliases = workoutCatalogAliases(name)
        return bestCatalogMatch(aliases, available.filter { it.links.isNotEmpty() })
            ?: bestCatalogMatch(aliases, available)
    }

    private fun bestCatalogMatch(aliases: List<String>, candidates: List<CatalogExercise>): CatalogExercise? {
        var best: CatalogExercise? = null
        var bestScore = 0
        aliases.forEach { alias ->
            val aliasNorm = normalized(alias)
            val tokens = aliasNorm.split(" ").filter { it.length > 2 && it !in listOf("com", "para", "sem") }
            candidates.forEach { candidate ->
                val nameNorm = normalized(candidate.name)
                val haystack = normalized(listOf(
                    candidate.name,
                    candidate.slug,
                    candidate.muscle,
                    candidate.subgroup,
                    candidate.primary,
                    candidate.secondary,
                    candidate.equipment,
                    candidate.movement,
                ).joinToString(" "))
                var score = 0
                if (nameNorm == aliasNorm) score += 120
                if (nameNorm.contains(aliasNorm) || aliasNorm.contains(nameNorm)) score += 70
                score += tokens.count { haystack.contains(it) } * 14
                if (candidate.links.isNotEmpty()) score += 4
                if (score > bestScore) {
                    bestScore = score
                    best = candidate
                }
            }
        }
        return best.takeIf { bestScore >= 28 }
    }

    private fun workoutCatalogAliases(name: String): List<String> {
        val key = normalized(name)
        val manual = mapOf(
            normalized("Supino reto ou maquina peitoral") to listOf("Supino reto na maquina", "Supino reto com barra", "Supino reto com halteres", "Chest press horizontal"),
            normalized("Supino maquina") to listOf("Supino reto na maquina", "Chest press horizontal", "Supino reto com barra"),
            normalized("Supino inclinado com halteres") to listOf("Supino inclinado com halteres", "Supino inclinado com barra", "Supino inclinado na maquina"),
            normalized("Desenvolvimento de ombros") to listOf("Desenvolvimento na maquina", "Desenvolvimento com halteres", "Desenvolvimento militar com barra"),
            normalized("Elevacao lateral") to listOf("Elevacao lateral com halteres", "Elevacao lateral na maquina", "Elevacao lateral na polia"),
            normalized("Triceps corda") to listOf("Triceps corda na polia", "Triceps barra V na polia", "Triceps barra reta na polia"),
            normalized("Prancha") to listOf("Prancha frontal", "Prancha com toque no ombro", "Prancha lateral"),
            normalized("Puxada frente") to listOf("Puxada frente pegada aberta", "Puxada frente pegada neutra", "Puxada articulada na maquina"),
            normalized("Puxada ou remada") to listOf("Puxada frente pegada aberta", "Remada baixa com triangulo", "Remada sentado na maquina"),
            normalized("Remada baixa") to listOf("Remada baixa com triangulo", "Remada baixa com barra reta", "Remada sentado na maquina"),
            normalized("Remada unilateral") to listOf("Remada unilateral com halter", "Remada baixa unilateral", "Remada serrote"),
            normalized("Face pull") to listOf("Face pull", "Crucifixo inverso na polia", "Remada alta para posterior de ombro"),
            normalized("Rosca direta") to listOf("Rosca direta com barra reta", "Rosca direta com barra W", "Rosca direta com halteres"),
            normalized("Rosca martelo") to listOf("Rosca martelo com halteres", "Rosca martelo na corda", "Rosca martelo cruzada"),
            normalized("Leg press") to listOf("Leg press 45", "Leg press horizontal", "Leg press vertical"),
            normalized("Leg press leve") to listOf("Leg press 45", "Leg press horizontal", "Leg press vertical"),
            normalized("Agachamento livre ou guiado") to listOf("Agachamento livre com barra", "Agachamento no Smith", "Agachamento hack"),
            normalized("Cadeira extensora") to listOf("Cadeira extensora bilateral", "Cadeira extensora com pausa", "Cadeira extensora unilateral"),
            normalized("Mesa flexora") to listOf("Mesa flexora", "Cadeira flexora", "Flexora unilateral"),
            normalized("Stiff") to listOf("Stiff com barra", "Stiff com halteres", "Levantamento terra romeno"),
            normalized("Panturrilha") to listOf("Panturrilha em pe na maquina", "Panturrilha no leg press", "Panturrilha sentada na maquina"),
            normalized("Abdominal ou prancha") to listOf("Abdominal crunch no solo", "Prancha frontal", "Abdominal remador"),
            normalized("Mobilidade final") to listOf("Bird dog", "Dead bug", "Pallof press"),
        )

        val aliases = mutableListOf<String>()
        aliases.addAll(manual[key].orEmpty())
        aliases.add(name)
        name.split(" ou ", ignoreCase = true).map { it.trim() }.filter { it.length > 3 }.forEach { aliases.add(it) }
        return aliases.distinctBy { normalized(it) }
    }

    private fun exerciseMeta(exercise: CatalogExercise): String {
        val equipment = exercise.equipment.ifBlank { "equipamento variavel" }
        val primary = exercise.primary.ifBlank { exercise.subgroup.ifBlank { exercise.muscle } }
        return exercise.muscle + " | " + equipment + " | " + primary
    }

    private fun mediaHealthLabel(exercise: CatalogExercise): String {
        val frames = if (exercise.links.isEmpty()) "sem frames remotos" else exercise.links.size.toString() + " frames remotos"
        val status = exercise.status.ifBlank { "fonte externa" }
        return "Midia: " + frames + " | " + status
    }

    private fun normalized(text: String): String {
        return Normalizer.normalize(text.lowercase(Locale("pt", "BR")), Normalizer.Form.NFD)
            .replace("\\p{Mn}+".toRegex(), "")
            .replace("[^a-z0-9]+".toRegex(), " ")
            .trim()
    }

    private fun renderGoals(root: LinearLayout) {
        val weeklyTarget = input("Meta semanal de series", prefs.getString("goal_week_sets", "60") ?: "60")
        val volumeTarget = input("Meta semanal de volume kg", prefs.getString("goal_week_volume", "12000") ?: "12000")
        val weight = input("Peso corporal", prefs.getString("body_weight", "") ?: "")
        val stats = computeStats(allLogs())

        val box = card()
        box.orientation = LinearLayout.VERTICAL
        box.addView(label("METAS PESSOAIS", green, 13f, true))
        box.addView(progressLine("Series semana", stats.optInt("week_sets"), weeklyTarget.textValue().toIntOrNull() ?: 60))
        box.addView(progressLine("Volume semana", stats.optInt("week_volume"), volumeTarget.textValue().toIntOrNull() ?: 12000))
        box.addView(weeklyTarget)
        box.addView(volumeTarget)
        box.addView(weight)
        val save = actionButton("Salvar metas", green, bg)
        save.setOnClickListener {
            prefs.edit()
                .putString("goal_week_sets", weeklyTarget.textValue())
                .putString("goal_week_volume", volumeTarget.textValue())
                .putString("body_weight", weight.textValue())
                .apply()
            Toast.makeText(this, "Metas salvas.", Toast.LENGTH_SHORT).show()
            render()
        }
        box.addView(buttonParams(save))
        root.addView(box)
    }

    private fun renderCoach(root: LinearLayout) {
        val box = card()
        box.orientation = LinearLayout.VERTICAL
        box.addView(label("COACH V12", green, 13f, true))
        box.addView(label("Musculacao e corrida no mesmo contexto", white, 24f, true))
        localInsights().forEach { insight -> box.addView(label("- " + insight, white, 15f, false)) }
        root.addView(box)
        root.addView(v12CoachDecisionPanel())
        root.addView(runningFiveKmForecastPanel())

        val report = card()
        report.orientation = LinearLayout.VERTICAL
        val stats = computeStats(allLogs())
        report.addView(label("RELATORIO RAPIDO", green, 13f, true))
        report.addView(label("Series: " + stats.optInt("total_sets") + " | Volume: " + stats.optInt("total_volume") + " kg", white, 16f, true))
        report.addView(label("Consistencia semanal: " + stats.optInt("week_sets") + " series.", muted, 15f, false))
        report.addView(label("Se a academia estiver cheia, troque por exercicio do mesmo padrao: empurrar, puxar, pernas ou core.", muted, 15f, false))
        root.addView(report)
    }

    private fun v12CoachDecisionPanel(): View {
        val strength = strengthAdjustmentFor(currentExercise())
        val running = smartRunningAdjustment()
        val box = card(surface3)
        box.orientation = LinearLayout.VERTICAL
        box.addView(label("PROXIMAS DECISOES", green, 13f, true))
        box.addView(label("Musculacao", white, 18f, true))
        box.addView(label(currentExercise().name + ": " + formatLoad(strength.nextLoad) + " em " + strength.suggestedSetCount + " series.", muted, 14f, false))
        box.addView(label("Corrida", Mo2Colors.Running, 18f, true))
        box.addView(label(running.headline + ". " + running.reason, muted, 14f, false))
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        val workout = actionButton("Abrir treino", green, bg)
        workout.setOnClickListener { switchTab("workout") }
        row.addView(workout, LinearLayout.LayoutParams(0, dp(50), 1f))
        val run = actionButton("Abrir corrida", surface2, Mo2Colors.Running)
        run.setOnClickListener { switchTab("running") }
        row.addView(run, LinearLayout.LayoutParams(0, dp(50), 1f))
        box.addView(spacedRow(row))
        return box
    }

    private fun renderProfile(root: LinearLayout) {
        val overview = card(surface3)
        overview.orientation = LinearLayout.VERTICAL
        val identity = LinearLayout(this)
        identity.orientation = LinearLayout.HORIZONTAL
        identity.gravity = Gravity.CENTER_VERTICAL
        val logo = label("M2", bg, 18f, true)
        logo.gravity = Gravity.CENTER
        logo.background = rounded(green, dp(Mo2Radius.Md))
        identity.addView(logo, LinearLayout.LayoutParams(dp(54), dp(54)))
        val identityText = LinearLayout(this)
        identityText.orientation = LinearLayout.VERTICAL
        identityText.setPadding(dp(14), 0, 0, 0)
        identityText.addView(label("PERFIL LOCAL", green, 12f, true))
        identityText.addView(label("Mo2 LOG pessoal", white, 24f, true))
        identityText.addView(label("Android nativo | v" + versionName, muted, 13f, false))
        identity.addView(identityText, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        identity.addView(statusChip("Local"))
        overview.addView(identity)
        overview.addView(label("Seus treinos, corridas e preferencias ficam armazenados neste celular.", muted, 14f, false))
        root.addView(overview)

        val data = card(surface2)
        data.orientation = LinearLayout.VERTICAL
        data.addView(label("SEUS DADOS", green, 13f, true))
        data.addView(label("Resumo local", white, 22f, true))
        val dataMetrics = LinearLayout(this)
        dataMetrics.orientation = LinearLayout.HORIZONTAL
        dataMetrics.addView(compactMetric("Series", allLogs().length().toString()), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        dataMetrics.addView(compactMetric("Corridas", runLogs().length().toString()), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        dataMetrics.addView(compactMetric("Favoritos", favoriteCatalogIds().size.toString()), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        data.addView(spacedRow(dataMetrics))
        val lastBackup = prefs.getString("last_backup_day", "").orEmpty()
        data.addView(label(
            mediaCacheFileCount().toString() + " imagens em cache | " +
                if (lastBackup.isBlank()) "backup ainda nao criado" else "ultimo backup em " + lastBackup,
            muted,
            13f,
            false,
        ))
        root.addView(data)
        root.addView(v12DataHealthPanel())
        root.addView(v12AccessibilityPanel())

        val planning = card(surface)
        planning.orientation = LinearLayout.VERTICAL
        planning.addView(label("PLANEJAMENTO", green, 13f, true))
        planning.addView(label("Preferencias de treino", white, 22f, true))
        planning.addView(label(
            "Plano base " + trainingPlanVersion + " | corrida semana " + currentRunningPlanWeek() + " de 6",
            muted,
            14f,
            false,
        ))
        val readiness = readinessStatus()
        planning.addView(label(
            if (readiness.isBlank()) "Check-in de hoje ainda nao preenchido." else "Check-in de hoje: " + readinessTitle(readiness) + ".",
            muted,
            13f,
            false,
        ))
        val planningActions = LinearLayout(this)
        planningActions.orientation = LinearLayout.HORIZONTAL
        val editPlan = actionButton("Editar plano", surface2, white)
        editPlan.setOnClickListener { switchTab("plan_editor") }
        planningActions.addView(editPlan, LinearLayout.LayoutParams(0, dp(48), 1f))
        val editGoals = actionButton("Metas", surface2, green)
        editGoals.setOnClickListener { switchTab("goals") }
        planningActions.addView(editGoals, LinearLayout.LayoutParams(0, dp(48), 1f))
        planning.addView(spacedRow(planningActions))
        root.addView(planning)

        val backup = card(surface2)
        backup.orientation = LinearLayout.VERTICAL
        backup.addView(label("BACKUP PESSOAL", green, 13f, true))
        backup.addView(label("Exportacao e importacao JSON", white, 22f, true))
        backup.addView(label("Copia todos os dados locais: series, corridas, plano editado, favoritos, ocultos, substitutos, metas e ajustes.", muted, 14f, false))

        val copy = actionButton("Copiar backup JSON", green, bg)
        copy.setOnClickListener { exportToClipboard() }
        backup.addView(buttonParams(copy))

        val paste = textArea("Cole aqui um backup JSON para importar", "")
        backup.addView(paste)

        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        val importPasted = actionButton("Importar colado", surface2, green)
        importPasted.setOnClickListener {
            hideKeyboard()
            importBackupJson(paste.textValue())
        }
        row.addView(importPasted, LinearLayout.LayoutParams(0, dp(50), 1f))
        val importClipboard = actionButton("Do clipboard", surface2, white)
        importClipboard.setOnClickListener {
            hideKeyboard()
            importBackupJson(readClipboardText())
        }
        row.addView(importClipboard, LinearLayout.LayoutParams(0, dp(50), 1f))
        backup.addView(spacedRow(row))
        if (prefs.contains(preImportBackupKey)) {
            val undoImport = actionButton("Desfazer ultima importacao", surface, amber)
            undoImport.setOnClickListener { showUndoBackupImportDialog() }
            backup.addView(buttonParams(undoImport))
            backup.addView(label("Uma copia automatica dos dados anteriores esta disponivel neste aparelho.", muted, 12f, false))
        }
        root.addView(backup)

        val dangerBox = card(surface)
        dangerBox.orientation = LinearLayout.VERTICAL
        dangerBox.addView(label("DADOS LOCAIS", green, 13f, true))
        dangerBox.addView(label("Apagar remove historico de series e corridas deste celular.", muted, 14f, false))
        val clear = actionButton("Apagar dados locais", surface2, danger)
        clear.setOnClickListener { showClearLocalDataDialog() }
        dangerBox.addView(buttonParams(clear))
        root.addView(dangerBox)
    }

    private fun v12DataHealthPanel(): View {
        val recordCount = allLogs().length() + strengthSessionLogs().length() + runLogs().length()
        val invalidCollections = invalidLocalCollectionCount()
        val backupDay = prefs.getString("last_backup_day", "").orEmpty()
        val backupAge = if (isValidDayKey(backupDay)) daysBetween(backupDay, dayKey()) else null
        val health = Mo2ProgressEngine.dataHealth(invalidCollections, backupAge, recordCount)
        val box = card(surface3)
        box.orientation = LinearLayout.VERTICAL
        box.addView(label("INTEGRIDADE V12", green, 13f, true))
        box.addView(label(health.status, if (health.status == "Integro") green else amber, 24f, true))
        val detail = if (health.issues.isEmpty()) {
            "Colecoes locais legiveis e backup dentro do periodo recomendado."
        } else {
            health.issues.joinToString(" | ")
        }
        box.addView(label(detail, muted, 14f, false))
        box.addView(label(recordCount.toString() + " registros | cache " + mediaCacheSizeLabel(), white, 14f, true))
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        val verify = actionButton("Verificar", surface2, white)
        verify.setOnClickListener {
            Toast.makeText(this, "Integridade: " + health.status + ". " + detail, Toast.LENGTH_LONG).show()
        }
        row.addView(verify, LinearLayout.LayoutParams(0, dp(48), 1f))
        val backup = actionButton("Criar backup", green, bg)
        backup.setOnClickListener { exportToClipboard() }
        row.addView(backup, LinearLayout.LayoutParams(0, dp(48), 1f))
        box.addView(spacedRow(row))
        return box
    }

    private fun v12AccessibilityPanel(): View {
        val box = card(surface)
        box.orientation = LinearLayout.VERTICAL
        box.addView(label("ACESSIBILIDADE", green, 13f, true))
        box.addView(label("Leitura e movimento", white, 22f, true))
        box.addView(label("Preferencias aplicadas localmente em todas as proximas aberturas.", muted, 14f, false))
        val largeText = runningVoiceCheckBox(
            "Texto ampliado",
            prefs.getBoolean("accessibility_large_text", false),
        ) { checked ->
            prefs.edit().putBoolean("accessibility_large_text", checked).apply()
            render()
        }
        val reducedMotion = runningVoiceCheckBox(
            "Movimento reduzido em pop-ups",
            prefs.getBoolean("accessibility_reduce_motion", false),
        ) { checked ->
            prefs.edit().putBoolean("accessibility_reduce_motion", checked).apply()
        }
        box.addView(largeText)
        box.addView(reducedMotion)
        return box
    }

    private fun invalidLocalCollectionCount(): Int {
        val arrayKeys = setOf(
            "set_logs",
            "strength_session_logs",
            "run_logs",
            "favorite_catalog_ids",
            "hidden_catalog_ids",
            "running_active_stage_speeds",
            "custom_workout_plans",
        )
        val objectKeys = setOf(
            "unavailable_equipment",
            "preferred_catalog_alternatives",
            "running_schedule_overrides",
            "exercise_available_weights",
        )
        var invalid = 0
        prefs.all.forEach { entry ->
            val raw = entry.value as? String ?: return@forEach
            val expectsArray = entry.key in arrayKeys || entry.key.startsWith("planned_sets_")
            val expectsObject = entry.key in objectKeys
            if (!expectsArray && !expectsObject) return@forEach
            try {
                if (expectsArray) JSONArray(raw) else JSONObject(raw)
            } catch (_: Exception) {
                invalid += 1
            }
        }
        return invalid
    }

    private fun daysBetween(from: String, to: String): Int? {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        parser.isLenient = false
        return try {
            val start = parser.parse(from)?.time ?: return null
            val end = parser.parse(to)?.time ?: return null
            max(0L, (end - start) / 86400000L).toInt()
        } catch (_: Exception) {
            null
        }
    }

    private fun showClearLocalDataDialog() {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Apagar historico local?")
            .setMessage("Todas as series e corridas salvas neste celular serao removidas. Plano, metas e favoritos serao mantidos.")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Apagar") { _, _ ->
                prefs.edit().remove("set_logs").remove("run_logs").remove("strength_session_logs").apply()
                Toast.makeText(this, "Historico local apagado.", Toast.LENGTH_SHORT).show()
                render()
            }
            .create()
        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawable(Mo2Drawables.rounded(this, surface, Mo2Radius.Modal, border))
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(danger)
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(muted)
        }
        dialog.show()
    }

    private fun planSelector(): View {
        val scroll = HorizontalScrollView(this)
        scroll.isHorizontalScrollBarEnabled = false
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        plans.forEachIndexed { index, plan ->
            val active = selectedPlanIndex == index
            val button = pill(plan.title + "\n" + plan.focus, active, 168, 72)
            button.setOnClickListener {
                selectedPlanIndex = index
                selectedExerciseIndex = 0
                prefs.edit().putInt("selected_plan", index).putInt("selected_exercise", 0).apply()
                render()
            }
            row.addView(button)
        }
        scroll.addView(row)
        return scroll
    }

    private fun exerciseList(): View {
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        currentPlan().exercises.forEachIndexed { index, exercise ->
            val active = selectedExerciseIndex == index
            val sets = plannedSetsForExercise(index)
            val doneSets = countDonePlannedSets(sets)
            val completed = sets.length() > 0 && doneSets >= sets.length()
            val itemColor = when {
                completed -> Mo2Colors.PrimarySoft
                active -> surface3
                else -> surface
            }
            val itemBorder = when {
                completed -> green
                active -> Mo2Colors.Running
                else -> border
            }
            val item = card(itemColor)
            item.orientation = LinearLayout.VERTICAL
            item.background = rounded(itemColor, dp(Mo2Radius.Lg), itemBorder)
            item.setOnClickListener {
                selectedExerciseIndex = index
                prefs.edit().putInt("selected_exercise", index).apply()
                requestedSection = "workout_current"
                render()
            }

            val header = LinearLayout(this)
            header.orientation = LinearLayout.HORIZONTAL
            header.gravity = Gravity.CENTER_VERTICAL
            val indexBadge = label((index + 1).toString(), if (completed || active) bg else muted, 13f, true)
            indexBadge.gravity = Gravity.CENTER
            indexBadge.background = rounded(if (completed || active) green else surface2, dp(Mo2Radius.Pill), if (completed || active) green else border)
            header.addView(indexBadge, LinearLayout.LayoutParams(dp(32), dp(32)))

            val title = LinearLayout(this)
            title.orientation = LinearLayout.VERTICAL
            title.setPadding(dp(Mo2Spacing.Md), 0, dp(Mo2Spacing.Sm), 0)
            title.addView(label(exercise.name, if (completed) green else white, 18f, true))
            title.addView(label(
                doneSets.toString() + "/" + sets.length() + " series" + if (active) " | atual" else "",
                if (completed) green else muted,
                13f,
                completed,
            ))
            header.addView(title, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            header.addView(Mo2DragHandleView(this), LinearLayout.LayoutParams(dp(36), dp(44)))
            item.addView(header)
            item.addView(label(exercise.target + " | descanso " + exercise.rest, if (completed || active) white else muted, 14f, false))
            item.addView(label(exercise.notes, muted, 13f, false))
            item.addView(seriesProgressDots(sets))

            attachExerciseDragGesture(item, index)
            item.setOnDragListener { view, event ->
                when (event.action) {
                    DragEvent.ACTION_DRAG_STARTED -> event.localState is Int
                    DragEvent.ACTION_DRAG_ENTERED -> {
                        view.scaleX = 1.02f
                        view.scaleY = 1.02f
                        view.background = rounded(itemColor, dp(Mo2Radius.Lg), green)
                        true
                    }
                    DragEvent.ACTION_DRAG_EXITED -> {
                        view.scaleX = 1f
                        view.scaleY = 1f
                        view.background = rounded(itemColor, dp(Mo2Radius.Lg), itemBorder)
                        true
                    }
                    DragEvent.ACTION_DROP -> {
                        val fromIndex = event.localState as? Int ?: return@setOnDragListener false
                        if (fromIndex != index) reorderCurrentPlanExercise(fromIndex, index)
                        true
                    }
                    DragEvent.ACTION_DRAG_ENDED -> {
                        view.scaleX = 1f
                        view.scaleY = 1f
                        view.background = rounded(itemColor, dp(Mo2Radius.Lg), itemBorder)
                        true
                    }
                    else -> true
                }
            }
            box.addView(item)
        }
        box.setOnDragListener { _, event ->
            if (event.action == DragEvent.ACTION_DRAG_LOCATION) {
                val edge = dp(72)
                when {
                    event.y < edge -> pageScrollView?.smoothScrollBy(0, -dp(28))
                    event.y > box.height - edge -> pageScrollView?.smoothScrollBy(0, dp(28))
                }
            }
            event.localState is Int
        }
        return box
    }

    private fun seriesProgressDots(sets: JSONArray): View {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        row.setPadding(0, dp(Mo2Spacing.Md), 0, 0)
        val caption = label("SERIES", muted, 11f, true)
        row.addView(caption, LinearLayout.LayoutParams(dp(58), LinearLayout.LayoutParams.WRAP_CONTENT))
        for (index in 0 until sets.length()) {
            val done = sets.getJSONObject(index).optBoolean("done", false)
            val dot = label((index + 1).toString(), if (done) bg else green, 12f, true)
            dot.gravity = Gravity.CENTER
            dot.background = rounded(if (done) green else itemTransparentColor(), dp(Mo2Radius.Pill), green)
            val params = LinearLayout.LayoutParams(dp(28), dp(28))
            params.setMargins(0, 0, dp(Mo2Spacing.Sm), 0)
            row.addView(dot, params)
        }
        return row
    }

    private fun itemTransparentColor(): Int = android.graphics.Color.TRANSPARENT

    private fun attachExerciseDragGesture(item: View, index: Int) {
        val handler = Handler(Looper.getMainLooper())
        val touchSlop = ViewConfiguration.get(this).scaledTouchSlop.toFloat()
        var downX = 0f
        var downY = 0f
        var dragStarted = false
        val beginDrag = Runnable {
            dragStarted = item.startDragAndDrop(
                ClipData.newPlainText("exercise_index", index.toString()),
                View.DragShadowBuilder(item),
                index,
                0,
            )
            if (dragStarted) {
                item.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                Toast.makeText(this, "Arraste o exercicio para a nova posicao.", Toast.LENGTH_SHORT).show()
            }
        }
        item.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    dragStarted = false
                    downX = event.rawX
                    downY = event.rawY
                    handler.postDelayed(beginDrag, 1000L)
                }
                MotionEvent.ACTION_MOVE -> {
                    if (!dragStarted && (kotlin.math.abs(event.rawX - downX) > touchSlop || kotlin.math.abs(event.rawY - downY) > touchSlop)) {
                        handler.removeCallbacks(beginDrag)
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> handler.removeCallbacks(beginDrag)
            }
            false
        }
    }

    private fun registerPanel(): View {
        val exercise = currentExercise()
        val matched = catalogMatchForWorkoutExercise(exercise.name)
        val lastSet = lastSetFor(exercise.name)
        val sets = plannedSetsForCurrentExercise()
        val doneCount = countDonePlannedSets(sets)
        val box = card(surface3)
        box.orientation = LinearLayout.VERTICAL
        val heading = LinearLayout(this)
        heading.orientation = LinearLayout.HORIZONTAL
        heading.gravity = Gravity.CENTER_VERTICAL
        val headingText = LinearLayout(this)
        headingText.orientation = LinearLayout.VERTICAL
        headingText.addView(label("EXERCICIO ATUAL", green, 13f, true))
        headingText.addView(label(exercise.name, white, 23f, true))
        heading.addView(headingText, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        heading.addView(statusChip(if (doneCount >= sets.length()) "CONCLUIDO" else "EM CURSO"))
        box.addView(heading)
        box.addView(label(exercise.target + " | descanso " + exercise.rest, muted, 14f, false))
        box.addView(label(exercise.notes, muted, 13f, false))
        if (lastSet != null) {
            box.addView(label("Ultima concluida: " + lastSet.optInt("reps") + " reps | " + lastSet.optDouble("load") + " kg", muted, 13f, false))
        }
        box.addView(currentExerciseGuidancePanel(exercise, matched))
        box.addView(dashboardProgressLine("Series", doneCount.toString() + " de " + sets.length() + " concluidas", progressPercent(doneCount, sets.length()), green))
        box.addView(restTimerPanel())

        val seriesHeader = LinearLayout(this)
        seriesHeader.orientation = LinearLayout.HORIZONTAL
        seriesHeader.gravity = Gravity.CENTER_VERTICAL
        seriesHeader.setPadding(0, dp(Mo2Spacing.Lg), 0, dp(Mo2Spacing.Xs))
        seriesHeader.addView(label("SERIES", white, 16f, true), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        seriesHeader.addView(label(doneCount.toString() + "/" + sets.length(), if (doneCount >= sets.length()) green else muted, 14f, true))
        box.addView(seriesHeader)
        box.addView(plannedSetColumnHeader())

        for (index in 0 until sets.length()) {
            box.addView(plannedSetRow(index, sets.getJSONObject(index)))
        }

        val add = actionButton("+", surface2, green)
        add.textSize = 22f
        add.contentDescription = "Adicionar serie"
        add.setOnClickListener { addPlannedSetForCurrentExercise() }
        box.addView(buttonParams(add))

        val actions = LinearLayout(this)
        actions.orientation = LinearLayout.HORIZONTAL
        val swap = actionButton("Trocar exercicio", surface2, green)
        swap.setOnClickListener { swapCurrentExerciseForRecommended() }
        actions.addView(swap, LinearLayout.LayoutParams(0, dp(54), 1f))
        val finish = actionButton("Concluir treino", green, bg)
        finish.setOnClickListener { finishStrengthWorkout() }
        actions.addView(finish, LinearLayout.LayoutParams(0, dp(54), 1f))
        box.addView(spacedRow(actions))
        return box
    }

    private fun currentExerciseGuidancePanel(exercise: ExercisePlan, matched: CatalogExercise?): View {
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.setPadding(0, dp(Mo2Spacing.Md), 0, dp(Mo2Spacing.Sm))

        if (matched == null) {
            box.addView(label("Midia do catalogo ainda nao vinculada a este exercicio.", amber, 14f, true))
            box.addView(label("Use Trocar exercicio ou a aba Exercicios se quiser buscar uma alternativa com GIF.", muted, 13f, false))
            return box
        }

        val unavailable = isEquipmentUnavailable(matched.equipment)
        box.addView(label("EXECUCAO", green, 13f, true))
        box.addView(label(
            exerciseMeta(matched) + " | " + mediaHealthLabel(matched),
            muted,
            13f,
            false,
        ))
        if (unavailable) {
            box.addView(label("Equipamento marcado como indisponivel: " + matched.equipment, danger, 14f, true))
        }

        if (matched.links.isNotEmpty()) {
            val media = RemoteExerciseMediaView(this, matched.links)
            val mediaParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(220))
            mediaParams.setMargins(0, dp(Mo2Spacing.Sm), 0, dp(Mo2Spacing.Sm))
            box.addView(media, mediaParams)
        } else {
            box.addView(label("Este exercicio existe no catalogo, mas ainda nao tem GIF remoto.", amber, 14f, true))
        }

        val description = matched.description.ifBlank {
            "Exercicio do catalogo para " + matched.muscle + " com foco em " + matched.primary.ifBlank { matched.subgroup } + "."
        }
        box.addView(label(description, white, 14f, false))
        if (matched.technicalCare.isNotBlank()) {
            box.addView(label("Cuidado: " + matched.technicalCare, muted, 13f, false))
        }
        box.addView(label(
            matched.muscle + " | " + matched.equipment.ifBlank { "equipamento variavel" } + " | nivel " + matched.level.ifBlank { "-" },
            muted,
            13f,
            false,
        ))

        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        val unavailableButton = actionButton(if (unavailable) "Rever equipamento" else "Equipamento indisponivel", surface2, if (unavailable) danger else amber)
        unavailableButton.setOnClickListener { showEquipmentUnavailableDialog(matched) }
        row.addView(unavailableButton, LinearLayout.LayoutParams(0, dp(50), 1f))
        val open = actionButton("Abrir catalogo", surface2, green)
        open.setOnClickListener {
            prefs.edit()
                .putString("catalog_muscle", matched.muscle)
                .putString("catalog_selected", matched.id)
                .apply()
            switchTab("exercises")
        }
        row.addView(open, LinearLayout.LayoutParams(0, dp(50), 1f))
        box.addView(spacedRow(row))
        return box
    }

    private fun plannedSetColumnHeader(): View {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        row.setPadding(dp(Mo2Spacing.Sm), 0, dp(Mo2Spacing.Sm), 0)
        val spacerParams = LinearLayout.LayoutParams(dp(40), LinearLayout.LayoutParams.WRAP_CONTENT)
        row.addView(label("", muted, 11f, true), spacerParams)
        val load = label("KG", muted, 11f, true)
        load.gravity = Gravity.CENTER
        val loadParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        loadParams.setMargins(0, 0, dp(Mo2Spacing.Sm), 0)
        row.addView(load, loadParams)
        val reps = label("REPS", muted, 11f, true)
        reps.gravity = Gravity.CENTER
        val repsParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        repsParams.setMargins(0, 0, dp(Mo2Spacing.Sm), 0)
        row.addView(reps, repsParams)
        val done = label("FEITA", muted, 10f, true)
        done.gravity = Gravity.CENTER
        row.addView(done, LinearLayout.LayoutParams(dp(48), LinearLayout.LayoutParams.WRAP_CONTENT))
        return row
    }

    private fun plannedSetRow(index: Int, item: JSONObject): View {
        val done = item.optBoolean("done", false)
        val row = LinearLayout(this)
        row.orientation = LinearLayout.VERTICAL
        row.setPadding(dp(Mo2Spacing.Sm), dp(Mo2Spacing.Sm), dp(Mo2Spacing.Sm), dp(Mo2Spacing.Sm))
        val rowParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        rowParams.setMargins(0, dp(Mo2Spacing.Xs), 0, dp(Mo2Spacing.Xs))
        row.layoutParams = rowParams
        row.background = plannedSetRowBackground(done)

        val content = LinearLayout(this)
        content.orientation = LinearLayout.HORIZONTAL
        content.gravity = Gravity.CENTER_VERTICAL

        val number = label((index + 1).toString(), if (done) bg else muted, 13f, true)
        number.gravity = Gravity.CENTER
        number.background = rounded(if (done) green else surface2, dp(Mo2Radius.Pill), if (done) green else border)
        val numberParams = LinearLayout.LayoutParams(dp(32), dp(32))
        numberParams.setMargins(0, 0, dp(Mo2Spacing.Sm), 0)
        content.addView(number, numberParams)

        val storedLoad = item.optString("load", lastLoadFor(currentExercise().name))
        val load = input("kg", editableLoadText(storedLoad))
        load.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        load.gravity = Gravity.CENTER
        val loadParams = LinearLayout.LayoutParams(0, dp(52), 1f)
        loadParams.setMargins(0, 0, dp(Mo2Spacing.Sm), 0)
        content.addView(load, loadParams)

        val reps = input("reps", item.optString("reps", defaultRepsFor(currentExercise().target)))
        reps.inputType = InputType.TYPE_CLASS_NUMBER
        reps.gravity = Gravity.CENTER
        val repsParams = LinearLayout.LayoutParams(0, dp(52), 1f)
        repsParams.setMargins(0, 0, dp(Mo2Spacing.Sm), 0)
        content.addView(reps, repsParams)

        val check = CheckBox(this)
        check.isChecked = done
        check.gravity = Gravity.CENTER
        check.buttonTintList = ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
            intArrayOf(green, muted),
        )
        check.contentDescription = if (done) "Desmarcar serie " + (index + 1) else "Concluir serie " + (index + 1)
        content.addView(check, LinearLayout.LayoutParams(dp(48), dp(52)))
        row.addView(content)

        val status = label(if (done) "CONCLUIDA" else "PENDENTE", if (done) green else muted, 11f, true)
        status.setPadding(dp(40), dp(Mo2Spacing.Xs), 0, 0)
        row.addView(status)
        check.setOnClickListener {
            if (done) {
                uncompletePlannedSet(index, load.textValue(), reps.textValue())
            } else {
                completePlannedSet(index, load.textValue(), reps.textValue())
            }
        }
        if (done) {
            val watcher = object : TextWatcher {
                override fun beforeTextChanged(value: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(value: CharSequence?, start: Int, before: Int, count: Int) = Unit
                override fun afterTextChanged(value: Editable?) {
                    syncCompletedPlannedSetValues(index, load.textValue(), reps.textValue())
                }
            }
            load.addTextChangedListener(watcher)
            reps.addTextChangedListener(watcher)
        }

        var startX = 0f
        row.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = event.rawX
                    false
                }
                MotionEvent.ACTION_MOVE -> {
                    val delta = event.rawX - startX
                    if (delta < -dp(24)) view.background = rounded(danger, dp(Mo2Radius.Sm), danger)
                    false
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    val delta = event.rawX - startX
                    if (delta < -dp(96)) {
                        deletePlannedSet(index)
                        true
                    } else {
                        view.background = plannedSetRowBackground(done)
                        false
                    }
                }
                else -> false
            }
        }
        return row
    }

    private fun plannedSetRowBackground(done: Boolean): GradientDrawable {
        return rounded(if (done) Mo2Colors.PrimarySoft else surface, dp(Mo2Radius.Sm), if (done) green else border)
    }

    private fun plannedSetKey(): String {
        return plannedSetKey(dayKey(), currentPlan().id, selectedExerciseIndex)
    }

    private fun plannedSetKey(day: String, planId: String, exerciseIndex: Int): String {
        return "planned_sets_" + day + "_" + planId + "_" + exerciseIndex
    }

    private fun plannedSetsForCurrentExercise(): JSONArray {
        return plannedSetsForExercise(selectedExerciseIndex)
    }

    private fun plannedSetsForExercise(exerciseIndex: Int): JSONArray {
        val plan = currentPlan()
        val safeIndex = exerciseIndex.coerceIn(plan.exercises.indices)
        val exercise = plan.exercises[safeIndex]
        val key = plannedSetKey(dayKey(), plan.id, safeIndex)
        val stored = safeArray(key)
        if (stored.length() > 0) return stored

        val defaults = JSONArray()
        val count = defaultSetCountFor(exercise.target)
        val reps = defaultRepsFor(exercise.target)
        val load = lastLoadFor(exercise.name)
        for (index in 0 until count) {
            defaults.put(JSONObject()
                .put("id", UUID.randomUUID().toString())
                .put("load", load)
                .put("reps", reps)
                .put("done", false))
        }
        prefs.edit().putString(key, defaults.toString()).apply()
        return defaults
    }

    private fun savePlannedSets(sets: JSONArray) {
        prefs.edit().putString(plannedSetKey(), sets.toString()).apply()
    }

    private fun savePlannedSetsForExercise(exerciseIndex: Int, sets: JSONArray) {
        prefs.edit().putString(plannedSetKey(dayKey(), currentPlan().id, exerciseIndex), sets.toString()).apply()
    }

    private fun completePlannedSet(index: Int, loadRaw: String, repsRaw: String) {
        hideKeyboard()
        val sets = plannedSetsForCurrentExercise()
        val item = sets.getJSONObject(index)
        if (item.optBoolean("done", false)) return

        val loadValue = loadRaw.ifBlank { "0" }
        val repsValue = repsRaw.ifBlank { defaultRepsFor(currentExercise().target) }
        val logId = saveSet(currentExercise().name, repsValue, loadValue, "2", "8", "Checklist do treino", false, false)
        item
            .put("load", loadValue)
            .put("reps", repsValue)
            .put("done", true)
            .put("log_id", logId)
        sets.put(index, item)
        savePlannedSets(sets)

        if (countDonePlannedSets(sets) >= sets.length()) {
            moveAfterExerciseCompleted()
        } else {
            Toast.makeText(this, "Serie concluida. Descanso iniciado.", Toast.LENGTH_SHORT).show()
            renderWorkoutInPlace()
        }
    }

    private fun uncompletePlannedSet(index: Int, loadRaw: String, repsRaw: String) {
        hideKeyboard()
        syncCompletedPlannedSetValues(index, loadRaw, repsRaw)
        val sets = plannedSetsForCurrentExercise()
        if (index !in 0 until sets.length()) return
        val item = sets.getJSONObject(index)
        val logId = item.optString("log_id")
        item
            .put("load", loadRaw.ifBlank { item.optString("load", "0") })
            .put("reps", repsRaw.ifBlank { item.optString("reps", defaultRepsFor(currentExercise().target)) })
            .put("done", false)
        item.remove("log_id")
        sets.put(index, item)
        savePlannedSets(sets)
        if (logId.isNotBlank()) removeSetLogById(logId)
        Toast.makeText(this, "Serie desmarcada e removida do historico.", Toast.LENGTH_SHORT).show()
        renderWorkoutInPlace()
    }

    private fun syncCompletedPlannedSetValues(index: Int, loadRaw: String, repsRaw: String) {
        val sets = plannedSetsForCurrentExercise()
        if (index !in 0 until sets.length()) return
        val item = sets.getJSONObject(index)
        if (!item.optBoolean("done", false)) return

        val parsedLoad = loadRaw.trim().replace(',', '.').toDoubleOrNull()
        val parsedReps = repsRaw.trim().toIntOrNull()
        if (parsedLoad == null && parsedReps == null) return
        val loadValue = if (parsedLoad != null) loadRaw.trim() else item.optString("load", "0")
        val repsValue = if (parsedReps != null) repsRaw.trim() else item.optString("reps", defaultRepsFor(currentExercise().target))
        if (item.optString("load") == loadValue && item.optString("reps") == repsValue) return

        item.put("load", loadValue).put("reps", repsValue)
        sets.put(index, item)
        savePlannedSets(sets)
        val logId = item.optString("log_id")
        if (logId.isNotBlank()) updateSetLogValues(logId, loadValue, repsValue)
    }

    private fun updateSetLogValues(logId: String, loadRaw: String, repsRaw: String) {
        val load = loadRaw.replace(',', '.').toDoubleOrNull() ?: return
        val reps = repsRaw.toIntOrNull() ?: return
        val logs = allLogs()
        for (index in 0 until logs.length()) {
            val item = logs.getJSONObject(index)
            if (item.optString("id") == logId) {
                item
                    .put("load", load)
                    .put("reps", reps)
                    .put("edited_at", timestamp())
                logs.put(index, item)
                prefs.edit().putString("set_logs", logs.toString()).apply()
                return
            }
        }
    }

    private fun removeSetLogById(logId: String) {
        val logs = allLogs()
        for (index in logs.length() - 1 downTo 0) {
            if (logs.getJSONObject(index).optString("id") == logId) logs.remove(index)
        }
        prefs.edit().putString("set_logs", logs.toString()).apply()
    }

    private fun addPlannedSetForCurrentExercise() {
        val sets = plannedSetsForCurrentExercise()
        val adjustment = strengthAdjustmentFor(currentExercise())
        sets.put(JSONObject()
            .put("id", UUID.randomUUID().toString())
            .put("load", loadInputText(adjustment.nextLoad))
            .put("reps", defaultRepsFor(currentExercise().target))
            .put("done", false))
        savePlannedSets(sets)
        renderWorkoutInPlace()
    }

    private fun deletePlannedSet(index: Int) {
        val sets = plannedSetsForCurrentExercise()
        if (sets.length() <= 1) {
            Toast.makeText(this, "Mantenha pelo menos uma serie.", Toast.LENGTH_SHORT).show()
            renderWorkoutInPlace()
            return
        }
        val removed = sets.getJSONObject(index)
        sets.remove(index)
        savePlannedSets(sets)
        removed.optString("log_id").takeIf { it.isNotBlank() }?.let { logId -> removeSetLogById(logId) }
        Toast.makeText(this, "Serie removida.", Toast.LENGTH_SHORT).show()
        renderWorkoutInPlace()
    }

    private fun countDonePlannedSets(sets: JSONArray): Int {
        var count = 0
        for (i in 0 until sets.length()) {
            if (sets.getJSONObject(i).optBoolean("done", false)) count += 1
        }
        return count
    }

    private fun isExerciseCompleted(exerciseIndex: Int): Boolean {
        val sets = plannedSetsForExercise(exerciseIndex)
        return sets.length() > 0 && countDonePlannedSets(sets) >= sets.length()
    }

    private fun completedExerciseCount(): Int {
        return currentPlan().exercises.indices.count { exerciseIndex -> isExerciseCompleted(exerciseIndex) }
    }

    private fun firstIncompleteExerciseIndex(excludeCurrent: Boolean = false): Int? {
        return currentPlan().exercises.indices.firstOrNull { exerciseIndex ->
            (!excludeCurrent || exerciseIndex != selectedExerciseIndex) && !isExerciseCompleted(exerciseIndex)
        }
    }

    private fun reorderCurrentPlanExercise(fromIndex: Int, toIndex: Int) {
        val plan = currentPlan()
        if (fromIndex !in plan.exercises.indices || toIndex !in plan.exercises.indices || fromIndex == toIndex) return

        val oldOrder = plan.exercises.indices.toMutableList()
        val moved = oldOrder.removeAt(fromIndex)
        oldOrder.add(toIndex, moved)
        val reordered = oldOrder.map { oldIndex -> plan.exercises[oldIndex] }

        val storedGroups = prefs.all.keys
            .filter { key ->
                key.startsWith("planned_sets_") &&
                    key.substringAfterLast('_').toIntOrNull() != null &&
                    key.substringBeforeLast('_').endsWith("_" + plan.id)
            }
            .groupBy { key -> key.substringBeforeLast('_') }
        val editor = prefs.edit()
        storedGroups.forEach { (baseKey, keys) ->
            val valuesByOldIndex = keys.associate { key ->
                Pair(key.substringAfterLast('_').toInt(), prefs.getString(key, "[]") ?: "[]")
            }
            keys.forEach { key -> editor.remove(key) }
            oldOrder.forEachIndexed { newIndex, oldIndex ->
                valuesByOldIndex[oldIndex]?.let { raw -> editor.putString(baseKey + "_" + newIndex, raw) }
            }
        }
        editor.apply()

        selectedExerciseIndex = oldOrder.indexOf(selectedExerciseIndex).coerceAtLeast(0)
        replacePlanExercises(selectedPlanIndex.coerceIn(plans.indices), reordered)
        prefs.edit().putInt("selected_exercise", selectedExerciseIndex).apply()
        Toast.makeText(this, "Ordem dos exercicios atualizada.", Toast.LENGTH_SHORT).show()
        renderWorkoutInPlace()
    }

    private fun finishStrengthWorkout() {
        hideKeyboard()
        val completion = saveStrengthSessionCompletion()
        prefs.edit()
            .putString("last_finished_day", dayKey())
            .remove("strength_session_started_at_ms")
            .remove("strength_session_identity")
            .remove("rest_timer_end_at")
            .remove("rest_timer_duration_secs")
            .remove("rest_timer_exercise")
            .remove("rest_timer_notified")
            .apply()
        render()
        showWorkoutSummaryPopup(completion)
    }

    private fun saveStrengthSessionCompletion(): JSONObject {
        val plan = currentPlan()
        val day = dayKey()
        val planSetCounts = mutableMapOf<String, Int>()
        val setLogs = allLogs()
        for (index in 0 until setLogs.length()) {
            val item = setLogs.getJSONObject(index)
            if (item.optString("day") == day && item.optString("plan") == plan.title) {
                val exercise = item.optString("exercise")
                planSetCounts[exercise] = (planSetCounts[exercise] ?: 0) + 1
            }
        }

        val completed = mutableListOf<String>()
        val partial = mutableListOf<String>()
        val skipped = mutableListOf<String>()
        var totalPlannedSets = 0
        plan.exercises.forEachIndexed { exerciseIndex, exercise ->
            val stored = safeArray(plannedSetKey(day, plan.id, exerciseIndex))
            val plannedCount = if (stored.length() > 0) stored.length() else defaultSetCountFor(exercise.target)
            val checkedCount = countDonePlannedSets(stored)
            val loggedCount = planSetCounts[exercise.name] ?: 0
            val doneCount = max(checkedCount, loggedCount)
            totalPlannedSets += plannedCount
            when {
                doneCount <= 0 -> skipped.add(exercise.name)
                doneCount >= plannedCount -> completed.add(exercise.name)
                else -> partial.add(exercise.name)
            }
        }

        val sessions = strengthSessionLogs()
        val existingIndex = findStrengthSessionIndex(sessions, day, plan.id, plan.title)
        val existing = if (existingIndex >= 0) sessions.getJSONObject(existingIndex) else null
        val startedAt = prefs.getLong("strength_session_started_at_ms", 0L)
        val recordedDuration = if (startedAt > 0L) {
            ((System.currentTimeMillis() - startedAt) / 1000L).coerceIn(60L, 4L * 3600L)
        } else {
            estimateStrengthSessionSeconds(day, plan.title)
        }
        val session = JSONObject()
            .put("id", existing?.optString("id")?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString())
            .put("day", day)
            .put("week", weekKey())
            .put("time", timeKey())
            .put("plan_id", plan.id)
            .put("plan_title", plan.title)
            .put("plan_focus", plan.focus)
            .put("status", "completed")
            .put("completed_sets", planSetCounts.values.sum())
            .put("total_planned_sets", totalPlannedSets)
            .put("completed_exercises", jsonStringArray(completed))
            .put("partial_exercises", jsonStringArray(partial))
            .put("skipped_exercises", jsonStringArray(skipped))
            .put("finished_with_skips", partial.isNotEmpty() || skipped.isNotEmpty())
            .put("duration_seconds", recordedDuration)
            .put("updated_at", timestamp())
        if (existingIndex >= 0) sessions.put(existingIndex, session) else sessions.put(session)
        prefs.edit().putString("strength_session_logs", sessions.toString()).apply()
        return session
    }

    private fun ensureStrengthSessionStarted() {
        val plan = currentPlan()
        val identity = dayKey() + "::" + plan.id
        if (prefs.getString("strength_session_identity", "") == identity && prefs.getLong("strength_session_started_at_ms", 0L) > 0L) return
        prefs.edit()
            .putString("strength_session_identity", identity)
            .putLong("strength_session_started_at_ms", System.currentTimeMillis())
            .apply()
    }

    private fun currentStrengthSessionCompletion(): JSONObject? {
        val plan = currentPlan()
        val sessions = strengthSessionLogs()
        val index = findStrengthSessionIndex(sessions, dayKey(), plan.id, plan.title)
        return if (index >= 0) sessions.getJSONObject(index) else null
    }

    private fun reopenCurrentStrengthWorkout() {
        val plan = currentPlan()
        val sessions = strengthSessionLogs()
        val index = findStrengthSessionIndex(sessions, dayKey(), plan.id, plan.title)
        if (index >= 0) sessions.remove(index)
        val editor = prefs.edit().putString("strength_session_logs", sessions.toString())
        if (prefs.getString("last_finished_day", "") == dayKey()) editor.remove("last_finished_day")
        editor.apply()
        Toast.makeText(this, "Treino reaberto com as series ja registradas.", Toast.LENGTH_SHORT).show()
        render()
    }

    private fun findStrengthSessionIndex(sessions: JSONArray, day: String, planId: String, planTitle: String): Int {
        for (index in 0 until sessions.length()) {
            val item = sessions.getJSONObject(index)
            val samePlan = item.optString("plan_id") == planId || item.optString("plan_title") == planTitle
            if (item.optString("day") == day && samePlan && item.optString("status", "completed") == "completed") return index
        }
        return -1
    }

    private fun refreshStrengthSessionSummary(day: String, planTitle: String) {
        val sessions = strengthSessionLogs()
        var sessionIndex = -1
        for (index in 0 until sessions.length()) {
            val item = sessions.getJSONObject(index)
            if (item.optString("day") == day && item.optString("plan_title") == planTitle) {
                sessionIndex = index
                break
            }
        }
        if (sessionIndex < 0) return

        val session = sessions.getJSONObject(sessionIndex)
        val plan = plans.firstOrNull { it.title == planTitle }
        val counts = linkedMapOf<String, Int>()
        val logs = allLogs()
        for (index in 0 until logs.length()) {
            val item = logs.getJSONObject(index)
            if (item.optString("day") == day && item.optString("plan") == planTitle) {
                val exercise = item.optString("exercise", "Exercicio")
                counts[exercise] = (counts[exercise] ?: 0) + 1
            }
        }
        val names = if (plan != null) {
            plan.exercises.map { it.name }
        } else {
            (jsonStringList(session.optJSONArray("completed_exercises")) +
                jsonStringList(session.optJSONArray("partial_exercises")) +
                jsonStringList(session.optJSONArray("skipped_exercises")) +
                counts.keys).distinct()
        }
        val completed = mutableListOf<String>()
        val partial = mutableListOf<String>()
        val skipped = mutableListOf<String>()
        var totalPlannedSets = 0
        names.forEachIndexed { exerciseIndex, exerciseName ->
            val exercise = plan?.exercises?.getOrNull(exerciseIndex)
            val stored = if (plan != null) safeArray(plannedSetKey(day, plan.id, exerciseIndex)) else JSONArray()
            val plannedCount = when {
                stored.length() > 0 -> stored.length()
                exercise != null -> defaultSetCountFor(exercise.target)
                else -> 1
            }
            val doneCount = counts[exerciseName] ?: 0
            totalPlannedSets += plannedCount
            when {
                doneCount <= 0 -> skipped.add(exerciseName)
                doneCount >= plannedCount -> completed.add(exerciseName)
                else -> partial.add(exerciseName)
            }
        }
        session
            .put("completed_sets", counts.values.sum())
            .put("total_planned_sets", totalPlannedSets)
            .put("completed_exercises", jsonStringArray(completed))
            .put("partial_exercises", jsonStringArray(partial))
            .put("skipped_exercises", jsonStringArray(skipped))
            .put("finished_with_skips", partial.isNotEmpty() || skipped.isNotEmpty())
            .put("updated_at", timestamp())
        sessions.put(sessionIndex, session)
        prefs.edit().putString("strength_session_logs", sessions.toString()).apply()
    }

    private fun jsonStringArray(values: List<String>): JSONArray {
        val array = JSONArray()
        values.forEach { value -> array.put(value) }
        return array
    }

    private fun moveAfterExerciseCompleted() {
        val nextIncomplete = firstIncompleteExerciseIndex()
        if (nextIncomplete == null) {
            finishStrengthWorkout()
            return
        }
        selectedExerciseIndex = nextIncomplete
        prefs.edit().putInt("selected_exercise", selectedExerciseIndex).apply()
        Toast.makeText(this, "Exercicio concluido. Primeiro pendente aberto.", Toast.LENGTH_SHORT).show()
        renderWorkoutInPlace()
    }

    private fun defaultSetCountFor(target: String): Int {
        val first = Regex("(\\d+)").find(target)?.value?.toIntOrNull() ?: 3
        return first.coerceIn(1, 8)
    }

    private fun defaultRepsFor(target: String): String {
        val afterX = Regex("[xX]\\s*(\\d+)").find(target)?.groupValues?.getOrNull(1)
        if (!afterX.isNullOrBlank()) return afterX
        return if (target.contains("s", ignoreCase = true)) "10" else "10"
    }

    private fun nextPendingSetLine(sets: JSONArray): String {
        for (index in 0 until sets.length()) {
            val item = sets.getJSONObject(index)
            if (!item.optBoolean("done", false)) {
                val load = item.optString("load", lastLoadFor(currentExercise().name))
                val reps = item.optString("reps", defaultRepsFor(currentExercise().target))
                return "proxima " + (index + 1) + ": " + load + " kg x " + reps
            }
        }
        return "todas as series deste exercicio foram feitas"
    }

    private fun nextExerciseName(): String {
        val plan = currentPlan()
        val nextIncomplete = firstIncompleteExerciseIndex(excludeCurrent = true)
        return nextIncomplete?.let { index -> plan.exercises[index].name } ?: "concluir treino"
    }

    private fun strengthAdjustmentFor(exercise: ExercisePlan): StrengthAdjustment {
        val recent = recentSetsForExercise(exercise.name, 6)
        val targetSets = defaultSetCountFor(exercise.target)
        val plannedSets = plannedSetsForCurrentExercise()
        val completedSets = countDonePlannedSets(plannedSets)
        if (recent.isEmpty()) {
            val baseLoad = lastLoadFor(exercise.name).replace(',', '.').toDoubleOrNull() ?: 0.0
            return constrainAdjustmentToAvailableWeights(exercise, StrengthAdjustment(
                nextLoad = baseLoad,
                suggestedSetCount = targetSets.coerceAtLeast(completedSets),
                loadReason = if (baseLoad <= 0.0) "Sem historico para este exercicio. Use a primeira serie para registrar uma carga real." else "Use a ultima carga conhecida como ponto de partida.",
                volumeReason = "Comece pelo volume planejado e deixe o app ajustar depois dos registros.",
            ))
        }

        val recentUsed = recent.take(3)
        val repBounds = targetRepBounds(exercise.target)
        val avgReps = recentUsed.map { it.optInt("reps") }.average()
        val rpeValues = recentUsed
            .map { it.optDouble("rpe", -1.0) }
            .filter { it >= 0.0 }
        val avgRpe = if (rpeValues.isEmpty()) null else rpeValues.average()
        val baseLoad = recentUsed.first().optDouble("load")
        val step = smartLoadStep(baseLoad)

        var nextLoad = baseLoad
        val loadReason = when {
            baseLoad <= 0.0 -> {
                nextLoad = 0.0
                "A ultima serie nao tem carga registrada. Preencha a carga real antes de progredir."
            }
            avgRpe == null -> "Sem RPE recente suficiente. Repita a ultima carga e registre RPE para calibrar melhor."
            avgRpe <= 7.5 && avgReps >= repBounds.second -> {
                nextLoad = roundLoad(baseLoad + step)
                "RPE medio baixo e reps no topo da faixa. Boa hora para subir " + formatLoad(step) + "."
            }
            avgRpe >= 9.0 || avgReps < repBounds.first -> {
                nextLoad = roundLoad((baseLoad - step).coerceAtLeast(0.0))
                "RPE alto ou reps abaixo da faixa. Reduza um passo e preserve tecnica."
            }
            avgRpe <= 8.5 -> "Zona boa de trabalho. Repita a carga e tente fechar todas as series limpas."
            else -> "Fadiga um pouco alta. Mantenha carga e evite series extras hoje."
        }

        val rawSuggestedSets = when {
            avgRpe != null && avgRpe <= 7.0 && completedSets >= targetSets -> targetSets + 1
            avgRpe != null && avgRpe >= 9.0 -> targetSets - 1
            else -> targetSets
        }
        val suggestedSets = rawSuggestedSets.coerceIn(1, 8).coerceAtLeast(completedSets)
        val volumeReason = when {
            suggestedSets > targetSets -> "Voce ja fechou o planejado com folga. Uma serie extra leve e opcional."
            suggestedSets < targetSets -> "Volume reduzido para hoje por sinal de fadiga recente."
            completedSets < targetSets -> "Faltam " + (targetSets - completedSets).coerceAtLeast(0) + " series para o volume base."
            else -> "Volume do exercicio esta dentro do planejado."
        }

        return constrainAdjustmentToAvailableWeights(exercise, StrengthAdjustment(
            nextLoad = nextLoad,
            suggestedSetCount = suggestedSets,
            loadReason = loadReason,
            volumeReason = volumeReason,
        ))
    }

    private fun applySmartLoadToPendingSets() {
        val exercise = currentExercise()
        val customWeights = customAvailableWeightsFor(exercise)
        if (customWeights != null && customWeights.isEmpty()) {
            Toast.makeText(this, "Adicione ao menos um peso disponivel para este exercicio.", Toast.LENGTH_SHORT).show()
            return
        }
        val adjustment = strengthAdjustmentFor(exercise)
        if (adjustment.nextLoad <= 0.0) {
            Toast.makeText(this, "Preencha uma carga real antes de aplicar a sugestao.", Toast.LENGTH_SHORT).show()
            return
        }
        val sets = plannedSetsForCurrentExercise()
        var changed = 0
        for (i in 0 until sets.length()) {
            val item = sets.getJSONObject(i)
            if (!item.optBoolean("done", false)) {
                item.put("load", loadInputText(adjustment.nextLoad))
                sets.put(i, item)
                changed += 1
            }
        }
        if (changed == 0) {
            Toast.makeText(this, "Todas as series ja foram concluidas.", Toast.LENGTH_SHORT).show()
            return
        }
        savePlannedSets(sets)
        Toast.makeText(this, "Carga sugerida aplicada nas series pendentes.", Toast.LENGTH_SHORT).show()
        render()
    }

    private fun applySmartVolumeToCurrentExercise() {
        val adjustment = strengthAdjustmentFor(currentExercise())
        val sets = plannedSetsForCurrentExercise()
        val desired = adjustment.suggestedSetCount.coerceIn(1, 8)
        var changed = false

        while (sets.length() < desired) {
            sets.put(JSONObject()
                .put("id", UUID.randomUUID().toString())
                .put("load", loadInputText(adjustment.nextLoad))
                .put("reps", defaultRepsFor(currentExercise().target))
                .put("done", false))
            changed = true
        }

        while (sets.length() > desired) {
            var removed = false
            for (i in sets.length() - 1 downTo 0) {
                if (!sets.getJSONObject(i).optBoolean("done", false)) {
                    sets.remove(i)
                    removed = true
                    changed = true
                    break
                }
            }
            if (!removed) break
        }

        if (!changed) {
            Toast.makeText(this, "Volume ja esta no ponto sugerido.", Toast.LENGTH_SHORT).show()
            return
        }
        savePlannedSets(sets)
        Toast.makeText(this, "Series ajustadas pelo coach.", Toast.LENGTH_SHORT).show()
        render()
    }

    private fun recentSetsForExercise(exercise: String, limit: Int): List<JSONObject> {
        val logs = allLogs()
        val result = mutableListOf<JSONObject>()
        for (i in logs.length() - 1 downTo 0) {
            val item = logs.getJSONObject(i)
            if (item.optString("exercise") == exercise) {
                result.add(item)
                if (result.size >= limit) break
            }
        }
        return result
    }

    private fun targetRepBounds(target: String): Pair<Int, Int> {
        val match = Regex("[xX]\\s*(\\d+)(?:\\s*-\\s*(\\d+))?").find(target)
        val first = match?.groupValues?.getOrNull(1)?.toIntOrNull() ?: defaultRepsFor(target).toIntOrNull() ?: 10
        val second = match?.groupValues?.getOrNull(2)?.toIntOrNull() ?: first
        return Pair(first.coerceAtLeast(1), second.coerceAtLeast(first))
    }

    private fun smartLoadStep(load: Double): Double {
        return when {
            load <= 0.0 -> 0.0
            load < 12.0 -> 1.0
            load < 40.0 -> 2.5
            else -> 5.0
        }
    }

    private fun roundLoad(value: Double): Double = (value * 2.0).roundToInt().toDouble() / 2.0

    private fun loadInputText(value: Double): String {
        return if (value % 1.0 == 0.0) value.toInt().toString() else String.format(Locale.US, "%.1f", value)
    }

    private fun editableLoadText(value: String): String {
        val parsed = value.trim().replace(',', '.').toDoubleOrNull()
        return if (parsed == 0.0) "" else value
    }

    private fun exerciseWeightKey(exercise: ExercisePlan): String = normalized(exercise.name)

    private fun customAvailableWeightsFor(exercise: ExercisePlan): List<Double>? {
        val map = safeObject("exercise_available_weights")
        val key = exerciseWeightKey(exercise)
        if (key.isBlank() || !map.has(key)) return null
        val values = map.optJSONArray(key) ?: return emptyList()
        val result = mutableListOf<Double>()
        for (index in 0 until values.length()) {
            values.optDouble(index, Double.NaN).takeIf { value -> value.isFinite() }?.let { value -> result.add(value) }
        }
        return Mo2ProgressEngine.normalizeAvailableWeights(result)
    }

    private fun defaultAvailableWeightsFor(exercise: ExercisePlan, suggestedLoad: Double): List<Double> {
        val lastLoad = lastLoadFor(exercise.name).replace(',', '.').toDoubleOrNull() ?: 0.0
        val anchor = suggestedLoad.takeIf { value -> value > 0.0 } ?: lastLoad
        if (anchor <= 0.0) {
            return listOf(1.0, 2.0, 3.0, 4.0, 5.0, 7.5, 10.0, 12.5, 15.0, 17.5, 20.0)
        }
        val step = smartLoadStep(anchor).takeIf { value -> value > 0.0 } ?: 2.5
        val values = (-5..6).map { offset -> roundLoad(anchor + step * offset) }
        return Mo2ProgressEngine.normalizeAvailableWeights(values + anchor + suggestedLoad)
    }

    private fun availableWeightsFor(exercise: ExercisePlan, suggestedLoad: Double): List<Double> {
        return customAvailableWeightsFor(exercise) ?: defaultAvailableWeightsFor(exercise, suggestedLoad)
    }

    private fun constrainAdjustmentToAvailableWeights(exercise: ExercisePlan, adjustment: StrengthAdjustment): StrengthAdjustment {
        val custom = customAvailableWeightsFor(exercise) ?: return adjustment
        if (custom.isEmpty() || adjustment.nextLoad <= 0.0) {
            val reason = if (custom.isEmpty()) adjustment.loadReason + " Adicione pelo menos um peso disponivel." else adjustment.loadReason
            return adjustment.copy(loadReason = reason)
        }
        val constrained = Mo2ProgressEngine.nearestAvailableWeight(adjustment.nextLoad, custom)
        if (kotlin.math.abs(constrained - adjustment.nextLoad) < 0.001) return adjustment
        return adjustment.copy(
            nextLoad = constrained,
            loadReason = adjustment.loadReason + " Ajustada para " + formatLoad(constrained) + ", disponivel neste exercicio.",
        )
    }

    private fun saveAvailableWeights(exercise: ExercisePlan, values: List<Double>) {
        val normalizedWeights = Mo2ProgressEngine.normalizeAvailableWeights(values)
        val array = JSONArray()
        normalizedWeights.forEach { value -> array.put(value) }
        val map = safeObject("exercise_available_weights")
        map.put(exerciseWeightKey(exercise), array)
        prefs.edit().putString("exercise_available_weights", map.toString()).apply()
    }

    private fun clearAvailableWeights(exercise: ExercisePlan) {
        val map = safeObject("exercise_available_weights")
        map.remove(exerciseWeightKey(exercise))
        val editor = prefs.edit()
        if (map.length() == 0) editor.remove("exercise_available_weights") else editor.putString("exercise_available_weights", map.toString())
        editor.apply()
    }

    private fun availableWeightSummary(weights: List<Double>): String {
        if (weights.isEmpty()) return "nenhum"
        val visible = weights.take(6).joinToString(", ") { value -> formatLoad(value) }
        return visible + if (weights.size > 6) " +" + (weights.size - 6) else ""
    }

    private fun swapCurrentExerciseForRecommended() {
        val matched = catalogMatchForWorkoutExercise(currentExercise().name)
        if (matched == null) {
            Toast.makeText(this, "Nenhuma alternativa recomendada encontrada.", Toast.LENGTH_SHORT).show()
            return
        }
        val preferred = preferredAlternativeFor(matched)
        val reason = prefs.getString("swap_reason_filter", "occupied") ?: "occupied"
        val options = (listOfNotNull(preferred).filter { equipmentAvailableForSuggestion(it) } + recommendedSwapOptions(matched, reason))
            .distinctBy { it.id }
            .take(8)
        if (options.isEmpty()) {
            Toast.makeText(this, "Nenhuma alternativa recomendada encontrada.", Toast.LENGTH_SHORT).show()
            return
        }

        showRecommendedExerciseDialog(matched, options, preferred?.id, reason)
    }

    private fun recommendedSwapOptions(current: CatalogExercise, reason: String): List<CatalogExercise> {
        val hiddenIds = hiddenCatalogIds()
        val base = alternativesFor(current)
            .filter { it.id != current.id }
            .filter { !hiddenIds.contains(it.id) }
            .filter { equipmentAvailableForSuggestion(it) }
        val filtered = when (reason) {
            "same_level" -> base.filter { it.level.isBlank() || current.level.isBlank() || normalized(it.level) == normalized(current.level) }
            "same_muscle" -> base.filter { normalized(it.muscle) == normalized(current.muscle) }
            else -> base
        }
        return filtered.ifEmpty { base }
    }

    private fun equipmentAvailableForSuggestion(exercise: CatalogExercise): Boolean {
        return !isEquipmentUnavailable(exercise.equipment)
    }

    private fun unavailableEquipmentMap(): JSONObject = safeObject("unavailable_equipment")

    private fun equipmentKey(equipment: String): String = normalized(equipment)

    private fun isEquipmentUnavailable(equipment: String): Boolean {
        val key = equipmentKey(equipment)
        if (key.isBlank()) return false
        return unavailableEquipmentMap().has(key)
    }

    private fun markEquipmentUnavailable(equipment: String, reason: String) {
        val key = equipmentKey(equipment)
        if (key.isBlank()) {
            Toast.makeText(this, "Equipamento variavel; use a troca manual.", Toast.LENGTH_SHORT).show()
            return
        }
        val map = unavailableEquipmentMap()
        map.put(key, JSONObject()
            .put("label", equipment)
            .put("reason", reason)
            .put("day", dayKey()))
        prefs.edit().putString("unavailable_equipment", map.toString()).apply()
    }

    private fun clearUnavailableEquipment(equipment: String) {
        val key = equipmentKey(equipment)
        if (key.isBlank()) return
        val map = unavailableEquipmentMap()
        map.remove(key)
        prefs.edit().putString("unavailable_equipment", map.toString()).apply()
    }

    private fun showEquipmentUnavailableDialog(exercise: CatalogExercise) {
        val content = LinearLayout(this)
        content.orientation = LinearLayout.VERTICAL
        content.setPadding(dp(18), dp(18), dp(18), dp(12))
        content.background = rounded(surface, dp(Mo2Radius.Modal), border)
        content.addView(label("EQUIPAMENTO INDISPONIVEL", danger, 13f, true))
        content.addView(label(exercise.equipment.ifBlank { "Equipamento variavel" }, white, 22f, true))
        content.addView(label("Marcar aqui tira esse equipamento das sugestoes automaticas. Os exercicios continuam disponiveis manualmente no catalogo.", muted, 14f, false))

        lateinit var dialog: AlertDialog
        val occupied = actionButton("Equipamento ocupado hoje", surface2, amber)
        occupied.setOnClickListener {
            markEquipmentUnavailable(exercise.equipment, "ocupado")
            dialog.dismiss()
            Toast.makeText(this, "Equipamento oculto das sugestoes automaticas.", Toast.LENGTH_SHORT).show()
            swapCurrentExerciseForRecommended()
        }
        content.addView(buttonParams(occupied))

        val missing = actionButton("Nao existe nesta academia", surface2, danger)
        missing.setOnClickListener {
            markEquipmentUnavailable(exercise.equipment, "inexistente")
            dialog.dismiss()
            Toast.makeText(this, "Equipamento removido das sugestoes automaticas.", Toast.LENGTH_SHORT).show()
            swapCurrentExerciseForRecommended()
        }
        content.addView(buttonParams(missing))

        if (isEquipmentUnavailable(exercise.equipment)) {
            val restore = actionButton("Liberar equipamento", surface2, green)
            restore.setOnClickListener {
                clearUnavailableEquipment(exercise.equipment)
                dialog.dismiss()
                Toast.makeText(this, "Equipamento liberado nas sugestoes.", Toast.LENGTH_SHORT).show()
                render()
            }
            content.addView(buttonParams(restore))
        }

        val cancel = actionButton("Cancelar", surface2, white)
        cancel.setOnClickListener { dialog.dismiss() }
        content.addView(buttonParams(cancel))

        dialog = AlertDialog.Builder(this)
            .setView(content)
            .create()
        dialog.setOnShowListener { content.startAnimation(smoothPopupAnimation()) }
        dialog.show()
    }

    private fun showRecommendedExerciseDialog(current: CatalogExercise, options: List<CatalogExercise>, preferredId: String?, reason: String) {
        val content = LinearLayout(this)
        content.orientation = LinearLayout.VERTICAL
        content.setPadding(dp(18), dp(18), dp(18), dp(12))
        content.background = rounded(surface, dp(Mo2Radius.Modal), border)
        content.addView(label("TROCAR EXERCICIO", green, 13f, true))
        content.addView(label(currentExercise().name, white, 22f, true))
        content.addView(label("Escolha uma alternativa para " + current.muscle + ". O plano sera atualizado localmente.", muted, 14f, false))

        lateinit var dialog: AlertDialog
        val reasons = listOf(
            Pair("occupied", "Equip. ocupado"),
            Pair("missing", "Equip. inexistente"),
            Pair("same_muscle", "Mesmo musculo"),
            Pair("same_level", "Nivel similar"),
        )
        val reasonGrid = LinearLayout(this)
        reasonGrid.orientation = LinearLayout.VERTICAL
        reasons.chunked(2).forEach { rowItems ->
            val reasonRow = LinearLayout(this)
            reasonRow.orientation = LinearLayout.HORIZONTAL
            rowItems.forEach { item ->
                val active = item.first == reason
                val button = actionButton(item.second, if (active) green else surface2, if (active) bg else white)
                button.textSize = accessibleTextSize(13f)
                button.maxLines = 1
                button.setOnClickListener {
                    prefs.edit().putString("swap_reason_filter", item.first).apply()
                    val refreshed = recommendedSwapOptions(current, item.first)
                        .let { listOfNotNull(preferredAlternativeFor(current)).filter { pref -> equipmentAvailableForSuggestion(pref) } + it }
                        .distinctBy { option -> option.id }
                        .take(8)
                    dialog.dismiss()
                    if (refreshed.isEmpty()) Toast.makeText(this, "Nenhuma alternativa livre para esse filtro.", Toast.LENGTH_SHORT).show()
                    else showRecommendedExerciseDialog(current, refreshed, preferredAlternativeFor(current)?.id, item.first)
                }
                reasonRow.addView(button, LinearLayout.LayoutParams(0, dp(50), 1f))
            }
            reasonGrid.addView(spacedRow(reasonRow))
        }
        content.addView(reasonGrid)

        options.forEach { option ->
            val item = card(if (option.id == preferredId) surface3 else surface2)
            item.orientation = LinearLayout.VERTICAL
            item.addView(label((if (option.id == preferredId) "[preferido] " else "") + option.name, white, 16f, true))
            item.addView(label(exerciseMeta(option), muted, 12f, false))
            item.addView(label("Nivel: " + option.level.ifBlank { "-" } + " | Grupo: " + option.muscle, muted, 12f, false))

            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            val use = actionButton("Usar", green, bg)
            use.setOnClickListener {
                applyRecommendedExerciseSwap(option)
                dialog.dismiss()
            }
            row.addView(use, LinearLayout.LayoutParams(0, dp(46), 1f))
            val prefer = actionButton("Preferir", surface2, if (option.id == preferredId) amber else white)
            prefer.setOnClickListener {
                setPreferredAlternative(current, option)
                dialog.dismiss()
            }
            row.addView(prefer, LinearLayout.LayoutParams(0, dp(46), 1f))
            item.addView(spacedRow(row))
            content.addView(item)
        }

        val cancel = actionButton("Cancelar", surface2, white)
        cancel.setOnClickListener { dialog.dismiss() }
        content.addView(buttonParams(cancel))

        val scroll = ScrollView(this)
        scroll.isFillViewport = true
        scroll.isVerticalScrollBarEnabled = true
        scroll.addView(content)

        dialog = AlertDialog.Builder(this)
            .setView(scroll)
            .create()
        dialog.setOnShowListener {
            content.startAnimation(smoothPopupAnimation())
            dialog.window?.setLayout(
                (resources.displayMetrics.widthPixels * 0.94f).roundToInt(),
                (resources.displayMetrics.heightPixels * 0.86f).roundToInt(),
            )
        }
        dialog.show()
    }

    private fun smoothPopupAnimation(): AnimationSet {
        val set = AnimationSet(true)
        if (prefs.getBoolean("accessibility_reduce_motion", false)) {
            set.addAnimation(AlphaAnimation(1f, 1f))
            set.duration = 0L
            return set
        }
        val fade = AlphaAnimation(0f, 1f)
        val slide = TranslateAnimation(0f, 0f, dp(18).toFloat(), 0f)
        set.addAnimation(fade)
        set.addAnimation(slide)
        set.duration = 220L
        return set
    }

    private fun applyRecommendedExerciseSwap(recommended: CatalogExercise) {
        val planIndex = selectedPlanIndex.coerceIn(plans.indices)
        val plan = plans[planIndex]
        val exercises = plan.exercises.toMutableList()
        val current = currentExercise()
        exercises[selectedExerciseIndex] = current.copy(
            name = recommended.name,
            notes = "Substituido por recomendado para " + recommended.muscle + ". " + current.notes,
        )
        replacePlanExercises(planIndex, exercises)
        prefs.edit().remove(plannedSetKey()).apply()
        Toast.makeText(this, "Exercicio trocado por " + recommended.name + ".", Toast.LENGTH_SHORT).show()
        render()
    }

    private fun showWorkoutSummaryPopup(completion: JSONObject) {
        val logs = todayLogs()
        val exerciseCounts = linkedMapOf<String, Int>()
        var totalVolume = 0.0
        var bestLoad = 0.0
        var bestExercise = "-"
        var rpeSum = 0.0
        var rpeCount = 0
        for (i in 0 until logs.length()) {
            val item = logs.getJSONObject(i)
            if (item.optString("plan") == currentPlan().title) {
                val name = item.optString("exercise")
                exerciseCounts[name] = (exerciseCounts[name] ?: 0) + 1
                val load = item.optDouble("load")
                totalVolume += load * item.optInt("reps")
                if (load > bestLoad) {
                    bestLoad = load
                    bestExercise = name
                }
                val rpe = item.optDouble("rpe", -1.0)
                if (rpe >= 0.0) {
                    rpeSum += rpe
                    rpeCount += 1
                }
            }
        }
        val runLines = todayRunSummaryLines()
        val partialExercises = jsonStringList(completion.optJSONArray("partial_exercises"))
        val skippedExercises = jsonStringList(completion.optJSONArray("skipped_exercises"))
        val avgRpe = if (rpeCount == 0) "-" else String.format(Locale("pt", "BR"), "%.1f", rpeSum / rpeCount)
        val exerciseLines = if (exerciseCounts.isEmpty()) {
            "Nenhuma serie registrada hoje."
        } else {
            exerciseCounts.entries.joinToString("\n") { entry -> "- " + entry.key + ": " + entry.value + " series" }
        }
        val message = listOf(
            "Treino: " + currentPlan().title,
            "Series: " + exerciseCounts.values.sum(),
            "Exercicios feitos: " + exerciseCounts.size,
            "Volume: " + totalVolume.roundToInt() + " kg",
            "RPE medio: " + avgRpe,
            "Melhor carga: " + if (bestLoad <= 0.0) "-" else bestExercise + " " + formatLoad(bestLoad),
            "",
            "Exercicios:",
            exerciseLines,
            "Parciais: " + if (partialExercises.isEmpty()) "Nenhum." else partialExercises.joinToString(", "),
            "Pulados: " + if (skippedExercises.isEmpty()) "Nenhum." else skippedExercises.joinToString(", "),
            "",
            "Corrida hoje:",
            if (runLines.isEmpty()) "Nenhuma corrida registrada hoje." else runLines.joinToString("\n"),
            "",
            "Proximo ajuste:",
            smartCoachLines().joinToString("\n"),
        ).joinToString("\n")

        val content = LinearLayout(this)
        content.orientation = LinearLayout.VERTICAL
        content.setPadding(dp(12), dp(14), dp(12), dp(8))
        content.addView(label("RESUMO POS-TREINO", green, 13f, true))
        content.addView(label(currentPlan().title + " concluido", white, 24f, true))
        content.addView(label(
            if (partialExercises.isEmpty() && skippedExercises.isEmpty()) {
                "Todos os exercicios planejados foram concluidos e salvos no historico."
            } else {
                "Treino encerrado. O app registrou o que foi feito e marcou o restante como parcial ou pulado."
            },
            muted,
            14f,
            false,
        ))
        content.addView(summaryMetricRow(
            Triple("Series", exerciseCounts.values.sum().toString(), exerciseCounts.size.toString() + " exercicios"),
            Triple("Volume", totalVolume.roundToInt().toString() + " kg", "movimentado"),
            green,
        ))
        content.addView(summaryMetricRow(
            Triple("RPE", avgRpe, "media da sessao"),
            Triple("Melhor carga", if (bestLoad <= 0.0) "-" else formatLoad(bestLoad), bestExercise),
            amber,
        ))

        val details = card(surface2)
        details.orientation = LinearLayout.VERTICAL
        details.addView(label("EXERCICIOS REALIZADOS", muted, 12f, true))
        details.addView(label(exerciseLines, white, 14f, false))
        if (partialExercises.isNotEmpty()) {
            details.addView(label("EXERCICIOS PARCIAIS", amber, 12f, true))
            details.addView(label(partialExercises.joinToString("\n") { exercise -> "- " + exercise }, white, 14f, false))
        }
        if (skippedExercises.isNotEmpty()) {
            details.addView(label("EXERCICIOS PULADOS", muted, 12f, true))
            details.addView(label(skippedExercises.joinToString("\n") { exercise -> "- " + exercise }, white, 14f, false))
        }
        if (runLines.isNotEmpty()) {
            details.addView(label("CORRIDA HOJE", muted, 12f, true))
            details.addView(label(runLines.joinToString("\n"), white, 14f, false))
        }
        content.addView(details)

        val scroll = ScrollView(this)
        scroll.addView(content)
        val dialog = AlertDialog.Builder(this)
            .setView(scroll)
            .setNeutralButton("Copiar") { _, _ -> copyTextToClipboard("Resumo Mo2 LOG", message, "Resumo copiado.") }
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .create()
        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawable(Mo2Drawables.rounded(this, surface, Mo2Radius.Modal, border))
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(green)
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(Mo2Colors.Running)
            content.startAnimation(smoothPopupAnimation())
        }
        dialog.show()
    }

    private fun todayRunSummaryLines(): List<String> {
        val runs = runLogs()
        val lines = mutableListOf<String>()
        for (i in 0 until runs.length()) {
            val item = runs.getJSONObject(i)
            if (item.optString("day") == dayKey()) {
                val title = item.optString("workout_title", "Corrida")
                lines.add("- " + title + ": " + formatKm(item.optDouble("distance")) + " em " + item.optString("duration"))
            }
        }
        return lines
    }

    private fun saveSet(exercise: String, repsRaw: String, loadRaw: String, rirRaw: String, rpeRaw: String, notes: String, autoAdvance: Boolean = false, renderAfter: Boolean = true): String {
        val reps = repsRaw.toIntOrNull() ?: 0
        val load = loadRaw.replace(',', '.').toDoubleOrNull() ?: 0.0
        val rir = rirRaw.toIntOrNull()
        val rpe = rpeRaw.replace(',', '.').toDoubleOrNull()
        val id = UUID.randomUUID().toString()
        val log = JSONObject()
            .put("id", id)
            .put("day", dayKey())
            .put("week", weekKey())
            .put("time", timeKey())
            .put("plan", currentPlan().title)
            .put("exercise", exercise)
            .put("reps", reps)
            .put("load", load)
            .put("rir", rir)
            .put("rpe", rpe)
            .put("notes", notes.trim())
        val logs = allLogs()
        logs.put(log)
        val editor = prefs.edit().putString("set_logs", logs.toString())
        val restSeconds = restSecondsFor(exercise)
        if (restSeconds > 0) {
            editor
                .putLong("rest_timer_end_at", System.currentTimeMillis() + restSeconds * 1000L)
                .putInt("rest_timer_duration_secs", restSeconds)
                .putString("rest_timer_exercise", exercise)
                .putBoolean("rest_timer_notified", false)
        }
        editor.apply()
        Toast.makeText(this, if (autoAdvance) "Serie salva. Descanso iniciado." else "Serie salva no celular.", Toast.LENGTH_SHORT).show()
        if (renderAfter) render()
        return id
    }

    private fun updateSet(id: String, repsRaw: String, loadRaw: String, rirRaw: String, rpeRaw: String, notesRaw: String) {
        val logs = allLogs()
        for (i in 0 until logs.length()) {
            val item = logs.getJSONObject(i)
            if (item.optString("id") == id) {
                val notes = notesRaw.ifBlank { item.optString("notes") }
                item
                    .put("reps", repsRaw.toIntOrNull() ?: item.optInt("reps"))
                    .put("load", loadRaw.replace(',', '.').toDoubleOrNull() ?: item.optDouble("load"))
                    .put("rir", rirRaw.toIntOrNull())
                    .put("rpe", rpeRaw.replace(',', '.').toDoubleOrNull())
                    .put("notes", notes.trim())
                    .put("edited_at", timestamp())
                logs.put(i, item)
                prefs.edit().putString("set_logs", logs.toString()).apply()
                Toast.makeText(this, "Ultima serie atualizada.", Toast.LENGTH_SHORT).show()
                render()
                return
            }
        }
        Toast.makeText(this, "Serie nao encontrada para edicao.", Toast.LENGTH_SHORT).show()
    }

    private fun undoLastSet() {
        val logs = allLogs()
        if (logs.length() == 0) {
            Toast.makeText(this, "Nenhuma serie para desfazer.", Toast.LENGTH_SHORT).show()
            return
        }
        val removed = logs.getJSONObject(logs.length() - 1)
        logs.remove(logs.length() - 1)
        val planIndex = plans.indexOfFirst { it.title == removed.optString("plan") }
        if (planIndex >= 0) {
            selectedPlanIndex = planIndex
            selectedExerciseIndex = plans[planIndex].exercises.indexOfFirst { it.name == removed.optString("exercise") }.coerceAtLeast(0)
        }
        prefs.edit()
            .putString("set_logs", logs.toString())
            .putInt("selected_plan", selectedPlanIndex)
            .putInt("selected_exercise", selectedExerciseIndex)
            .apply()
        Toast.makeText(this, "Ultima serie desfeita.", Toast.LENGTH_SHORT).show()
        render()
    }

    private fun startRunningWorkout(workout: RunningWorkout) {
        val firstStage = workout.stages.firstOrNull() ?: return
        val countdownEndAt = System.currentTimeMillis() + 5000L
        selectedRunId = workout.id
        prefs.edit()
            .putString("selected_run_id", workout.id)
            .putString("running_active_id", workout.id)
            .putInt("running_active_stage", 0)
            .putString("running_active_distance_done", "0.0")
            .putString("running_active_speed", firstStage.speedKmh.toString())
            .putString("running_active_stage_speeds", runStageSpeedArray(workout).toString())
            .putLong("running_active_last_tick_at", countdownEndAt)
            .putLong("running_countdown_end_at", countdownEndAt)
            .putLong("running_active_started_at_ms", countdownEndAt)
            .putLong("running_active_paused_total_ms", 0L)
            .putBoolean("running_active_paused", false)
            .putString("running_session_started_at", timestamp())
            .remove("running_active_pause_started_at")
            .remove("running_start_announced")
            .remove("running_30_announced_key")
            .remove("running_10_announced_key")
            .apply()
        Toast.makeText(this, "Timer de 5 segundos iniciado.", Toast.LENGTH_SHORT).show()
        render()
    }

    private fun updateActiveRunProgress(): Boolean {
        val workout = activeRunningWorkout() ?: return false
        if (workout.stages.isEmpty()) return false

        val now = System.currentTimeMillis()
        val countdownEndAt = prefs.getLong("running_countdown_end_at", 0L)
        if (countdownEndAt > now) return false
        if (countdownEndAt > 0L) {
            val alreadyAnnounced = prefs.getBoolean("running_start_announced", false)
            prefs.edit()
                .remove("running_countdown_end_at")
                .putBoolean("running_start_announced", true)
                .apply()
            if (!alreadyAnnounced) {
                speakRunCue("Comece. Primeira etapa: " + speakableStageCue(workout.stages.first()) + ".", flush = true)
            }
        }
        if (isActiveRunPaused()) return false

        var stageIndex = prefs.getInt("running_active_stage", 0).coerceIn(workout.stages.indices)
        var distanceDone = prefs.getString("running_active_distance_done", "0.0")?.toDoubleOrNull() ?: 0.0
        val speed = currentActiveRunSpeed(workout)
        val lastTick = prefs.getLong("running_active_last_tick_at", if (countdownEndAt > 0L) countdownEndAt else now)
        val elapsedMs = max(0L, now - lastTick)
        if (elapsedMs > 0L && speed > 0.0) {
            distanceDone += speed * (elapsedMs.toDouble() / 3600000.0)
        }

        while (stageIndex < workout.stages.size && distanceDone >= workout.stages[stageIndex].distanceKm) {
            distanceDone -= workout.stages[stageIndex].distanceKm
            stageIndex += 1
            prefs.edit()
                .remove("running_30_announced_key")
                .remove("running_10_announced_key")
                .apply()
            if (stageIndex < workout.stages.size) {
                prefs.edit().putString("running_active_speed", workout.stages[stageIndex].speedKmh.toString()).apply()
                speakRunCue("Etapa concluida. Agora: " + speakableStageCue(workout.stages[stageIndex]) + ".", flush = true)
            }
        }

        if (stageIndex >= workout.stages.size) {
            val elapsed = activeRunElapsedSeconds()
            val distance = totalRunDistance(workout)
            val log = saveRunCompletion(
                workout,
                manual = false,
                rerender = false,
                distanceOverride = distance,
                elapsedSecondsOverride = elapsed,
                completedStagesOverride = workout.stages.size,
            )
            if (log != null) announceRunCompletion(distance, elapsed, completed = true)
            clearActiveRun()
            if (log != null) pendingRunSummary = log
            return true
        }

        prefs.edit()
            .putInt("running_active_stage", stageIndex)
            .putString("running_active_distance_done", distanceDone.toString())
            .putLong("running_active_last_tick_at", now)
            .apply()
        return false
    }

    private fun adjustActiveRunSpeed(delta: Double) {
        val workout = activeRunningWorkout() ?: return
        if (updateActiveRunProgress()) {
            render()
            return
        }
        val speed = (currentActiveRunSpeed(workout) + delta).coerceIn(4.0, 18.0)
        val stageIndex = prefs.getInt("running_active_stage", 0).coerceIn(workout.stages.indices)
        val stageSpeeds = safeArray("running_active_stage_speeds")
        while (stageSpeeds.length() < workout.stages.size) {
            stageSpeeds.put(workout.stages[stageSpeeds.length()].speedKmh)
        }
        stageSpeeds.put(stageIndex, roundSpeed(speed))
        prefs.edit()
            .putString("running_active_speed", speed.toString())
            .putString("running_active_stage_speeds", stageSpeeds.toString())
            .apply()
        speakRunCue("Velocidade ajustada para " + speakableSpeed(speed) + ".", flush = true)
        refreshRunningSessionViews()
    }

    private fun runStageSpeedArray(workout: RunningWorkout): JSONArray {
        val speeds = JSONArray()
        workout.stages.forEach { stage -> speeds.put(roundSpeed(stage.speedKmh)) }
        return speeds
    }

    private fun runStageSnapshot(workout: RunningWorkout, includeActiveSpeeds: Boolean): JSONArray {
        val useActive = includeActiveSpeeds && prefs.getString("running_active_id", "") == workout.id
        val activeSpeeds = if (useActive) safeArray("running_active_stage_speeds") else JSONArray()
        val snapshot = JSONArray()
        workout.stages.forEachIndexed { index, stage ->
            val speed = activeSpeeds.optDouble(index, stage.speedKmh).takeIf { it > 0.0 } ?: stage.speedKmh
            snapshot.put(JSONObject()
                .put("index", index)
                .put("title", stage.title)
                .put("distance", roundKm(stage.distanceKm))
                .put("speed", roundSpeed(speed))
                .put("planned_speed", roundSpeed(stage.speedKmh))
                .put("note", stage.note))
        }
        return snapshot
    }

    private fun finishCurrentRunStage() {
        val workout = activeRunningWorkout() ?: return
        if (updateActiveRunProgress()) {
            render()
            return
        }
        val currentStage = prefs.getInt("running_active_stage", 0)
        if (currentStage >= workout.stages.lastIndex) {
            val elapsed = activeRunElapsedSeconds()
            val distance = totalRunDistance(workout)
            val log = saveRunCompletion(
                workout,
                manual = false,
                rerender = false,
                distanceOverride = distance,
                elapsedSecondsOverride = elapsed,
                completedStagesOverride = workout.stages.size,
            )
            if (log != null) announceRunCompletion(distance, elapsed, completed = true)
            clearActiveRun()
            if (log != null) pendingRunSummary = log
            render()
            return
        }
        val nextStage = currentStage + 1
        prefs.edit()
            .putInt("running_active_stage", nextStage)
            .putString("running_active_distance_done", "0.0")
            .putString("running_active_speed", workout.stages[nextStage].speedKmh.toString())
            .putLong("running_active_last_tick_at", System.currentTimeMillis())
            .remove("running_30_announced_key")
            .remove("running_10_announced_key")
            .apply()
        speakRunCue("Proxima etapa: " + speakableStageCue(workout.stages[nextStage]) + ".", flush = true)
        refreshRunningSessionViews()
    }

    private fun toggleActiveRunPause() {
        val workout = activeRunningWorkout() ?: return
        if (activeRunCountdownSeconds() > 0L) return
        val now = System.currentTimeMillis()
        if (isActiveRunPaused()) {
            val pausedAt = prefs.getLong("running_active_pause_started_at", now)
            val pausedTotal = prefs.getLong("running_active_paused_total_ms", 0L) + max(0L, now - pausedAt)
            prefs.edit()
                .putBoolean("running_active_paused", false)
                .putLong("running_active_paused_total_ms", pausedTotal)
                .putLong("running_active_last_tick_at", now)
                .remove("running_active_pause_started_at")
                .apply()
            speakRunCue("Treino retomado.", flush = true)
        } else {
            if (updateActiveRunProgress()) {
                render()
                return
            }
            prefs.edit()
                .putBoolean("running_active_paused", true)
                .putLong("running_active_pause_started_at", now)
                .putLong("running_active_last_tick_at", now)
                .apply()
            speakRunCue("Treino pausado.", flush = true)
        }
        refreshRunningSessionViews()
        runningSessionHandler.removeCallbacks(runningSessionRunnable)
        runningSessionHandler.post(runningSessionRunnable)
    }

    private fun finishActiveRunningWorkout(workout: RunningWorkout) {
        if (updateActiveRunProgress()) {
            render()
            return
        }
        val stageIndex = prefs.getInt("running_active_stage", 0).coerceIn(workout.stages.indices)
        val distance = activeRunCompletedDistance(workout)
        val elapsed = activeRunElapsedSeconds()
        val log = saveRunCompletion(
            workout,
            manual = false,
            rerender = false,
            distanceOverride = distance,
            elapsedSecondsOverride = elapsed,
            completedStagesOverride = stageIndex,
        )
        if (log != null) announceRunCompletion(distance, elapsed, completed = false)
        clearActiveRun()
        if (log != null) pendingRunSummary = log
        render()
    }

    private fun showCancelRunningWorkoutDialog() {
        val workout = activeRunningWorkout() ?: return
        AlertDialog.Builder(this)
            .setTitle("Cancelar corrida?")
            .setMessage(workout.title + " sera encerrado sem criar um registro no historico.")
            .setNegativeButton("Continuar treino", null)
            .setPositiveButton("Cancelar") { _, _ ->
                clearActiveRun()
                Toast.makeText(this, "Corrida cancelada.", Toast.LENGTH_SHORT).show()
                render()
            }
            .show()
    }

    private fun isActiveRunPaused(): Boolean = prefs.getBoolean("running_active_paused", false)

    private fun activeRunElapsedSeconds(): Long {
        val now = System.currentTimeMillis()
        val startedAt = prefs.getLong("running_active_started_at_ms", 0L)
        if (startedAt <= 0L) return 0L
        val effectiveNow = if (isActiveRunPaused()) {
            prefs.getLong("running_active_pause_started_at", now)
        } else {
            now
        }
        val pausedTotal = prefs.getLong("running_active_paused_total_ms", 0L)
        return max(0L, (effectiveNow - startedAt - pausedTotal) / 1000L)
    }

    private fun activeRunCompletedDistance(workout: RunningWorkout): Double {
        val stageIndex = prefs.getInt("running_active_stage", 0).coerceIn(workout.stages.indices)
        val completedDistance = workout.stages.take(stageIndex).sumOf { it.distanceKm }
        val currentDistance = prefs.getString("running_active_distance_done", "0.0")?.toDoubleOrNull() ?: 0.0
        return roundKm(completedDistance + currentDistance.coerceIn(0.0, workout.stages[stageIndex].distanceKm))
    }

    private fun saveRunCompletion(
        workout: RunningWorkout,
        manual: Boolean,
        rerender: Boolean = true,
        distanceOverride: Double? = null,
        elapsedSecondsOverride: Long? = null,
        completedStagesOverride: Int? = null,
    ): JSONObject? {
        if (isRunWorkoutCompleted(workout)) {
            Toast.makeText(this, "Este treino ja esta concluido nesta semana.", Toast.LENGTH_SHORT).show()
            return null
        }
        val distance = (distanceOverride ?: totalRunDistance(workout)).coerceAtLeast(0.0)
        val seconds = (elapsedSecondsOverride ?: estimatedWorkoutSeconds(workout)).coerceAtLeast(1L)
        val completedStages = (completedStagesOverride ?: workout.stages.size).coerceIn(0, workout.stages.size)
        val avgSpeed = if (seconds <= 0L) 0.0 else distance / (seconds.toDouble() / 3600.0)
        val paceSeconds = if (distance <= 0.0) 0L else (seconds.toDouble() / distance).roundToInt().toLong()
        val log = JSONObject()
            .put("id", UUID.randomUUID().toString())
            .put("day", dayKey())
            .put("week", weekKey())
            .put("time", timeKey())
            .put("run_workout_id", workout.id)
            .put("plan_week", workout.week)
            .put("workout_title", workout.dayName + " - " + workout.title)
            .put("distance", roundKm(distance))
            .put("speed", roundSpeed(avgSpeed))
            .put("duration", formatDuration(seconds))
            .put("duration_seconds", seconds)
            .put("pace_seconds_per_km", paceSeconds)
            .put("completed_stages", completedStages)
            .put("total_stages", workout.stages.size)
            .put("stages", runStageSnapshot(workout, !manual))
            .put("manual", manual)
            .put("notes", if (manual) "Marcado como concluido sem iniciar pelo app." else "Treino guiado pelo app.")
        val logs = runLogs()
        logs.put(log)
        prefs.edit().putString("run_logs", logs.toString()).apply()
        Toast.makeText(this, if (manual) "Treino marcado como concluido." else "Corrida salva localmente.", Toast.LENGTH_SHORT).show()
        if (rerender) render()
        return log
    }

    private fun showRunningWorkoutSummary(log: JSONObject) {
        val workout = runningWorkoutById(log.optString("run_workout_id"))
        val distance = log.optDouble("distance", 0.0)
        val seconds = log.optLong("duration_seconds", workout?.let { estimatedWorkoutSeconds(it) } ?: 0L)
        val paceSeconds = historyRunPaceSeconds(log)
        val completedStages = log.optInt("completed_stages", workout?.stages?.size ?: 0)
        val totalStages = log.optInt("total_stages", workout?.stages?.size ?: completedStages)

        val content = LinearLayout(this)
        content.orientation = LinearLayout.VERTICAL
        content.setPadding(dp(12), dp(14), dp(12), dp(8))
        content.addView(label("RESUMO POS-TREINO", green, 13f, true))
        content.addView(label(log.optString("workout_title", "Corrida concluida"), white, 24f, true))
        content.addView(label("Treino salvo localmente. Complete sua percepcao de esforco para melhorar os proximos ajustes.", muted, 14f, false))

        content.addView(summaryMetricRow(
            Triple("Duracao", formatDuration(seconds), "tempo ativo"),
            Triple("Distancia", formatKm(distance), "percorrida"),
            Mo2Colors.Running,
        ))
        content.addView(summaryMetricRow(
            Triple("Pace", if (paceSeconds > 0L) formatPaceSeconds(paceSeconds) else "-", "medio"),
            Triple("Etapas", completedStages.toString() + "/" + totalStages, "concluidas"),
            green,
        ))

        val effort = input("RPE de 1 a 10", log.optInt("rpe", -1).takeIf { it in 1..10 }?.toString().orEmpty())
        effort.inputType = InputType.TYPE_CLASS_NUMBER
        content.addView(label("PERCEPCAO DE ESFORCO", muted, 12f, true))
        content.addView(effort)
        val notes = textArea("Como foi a corrida?", log.optString("feedback_notes", ""))
        content.addView(label("NOTAS", muted, 12f, true))
        content.addView(notes)

        val scroll = ScrollView(this)
        scroll.addView(content)

        val dialog = AlertDialog.Builder(this)
            .setView(scroll)
            .setNegativeButton("Fechar", null)
            .setNeutralButton("Historico", null)
            .setPositiveButton("Salvar", null)
            .create()
        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawable(Mo2Drawables.rounded(this, surface, Mo2Radius.Modal, border))
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(green)
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(Mo2Colors.Running)
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(muted)
            content.startAnimation(smoothPopupAnimation())
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                if (updateRunFeedback(log.optString("id"), effort.textValue(), notes.textValue())) {
                    dialog.dismiss()
                    render()
                }
            }
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                if (updateRunFeedback(log.optString("id"), effort.textValue(), notes.textValue())) {
                    dialog.dismiss()
                    switchTab("history")
                }
            }
        }
        dialog.show()
    }

    private fun summaryMetricRow(
        first: Triple<String, String, String>,
        second: Triple<String, String, String>,
        accent: Int,
    ): View {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        val firstMetric = Mo2Components.metricCard(this, first.first, first.second, first.third, accent)
        val secondMetric = Mo2Components.metricCard(this, second.first, second.second, second.third, accent)
        row.addView(firstMetric, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        val secondParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        secondParams.setMargins(dp(8), 0, 0, 0)
        row.addView(secondMetric, secondParams)
        return row
    }

    private fun updateRunFeedback(logId: String, rpeRaw: String, notes: String): Boolean {
        val rpe = rpeRaw.trim().toIntOrNull()
        if (rpeRaw.isNotBlank() && (rpe == null || rpe !in 1..10)) {
            Toast.makeText(this, "Informe um RPE entre 1 e 10.", Toast.LENGTH_SHORT).show()
            return false
        }
        val logs = runLogs()
        for (index in 0 until logs.length()) {
            val item = logs.getJSONObject(index)
            if (item.optString("id") != logId) continue
            if (rpe != null) item.put("rpe", rpe) else item.remove("rpe")
            item.put("feedback_notes", notes.trim())
            logs.put(index, item)
            prefs.edit().putString("run_logs", logs.toString()).apply()
            Toast.makeText(this, "Resumo atualizado.", Toast.LENGTH_SHORT).show()
            return true
        }
        Toast.makeText(this, "Corrida nao encontrada no historico.", Toast.LENGTH_SHORT).show()
        return false
    }

    private fun clearActiveRun() {
        val editor = prefs.edit()
        clearActiveRunState(editor)
        editor.apply()
    }

    private fun clearActiveRunState(editor: SharedPreferences.Editor) {
        editor
            .remove("running_active_id")
            .remove("running_active_stage")
            .remove("running_active_distance_done")
            .remove("running_active_speed")
            .remove("running_active_stage_speeds")
            .remove("running_active_last_tick_at")
            .remove("running_countdown_end_at")
            .remove("running_active_started_at_ms")
            .remove("running_active_paused")
            .remove("running_active_pause_started_at")
            .remove("running_active_paused_total_ms")
            .remove("running_session_started_at")
            .remove("running_start_announced")
            .remove("running_30_announced_key")
            .remove("running_10_announced_key")
    }

    private fun activeRunningWorkout(): RunningWorkout? {
        val id = prefs.getString("running_active_id", "") ?: ""
        return if (id.isBlank()) null else runningWorkoutById(id)
    }

    private fun activeRunCountdownSeconds(): Long {
        val endAt = prefs.getLong("running_countdown_end_at", 0L)
        if (endAt <= 0L) return 0L
        return max(0L, ((endAt - System.currentTimeMillis()) + 999L) / 1000L)
    }

    private fun currentActiveRunSpeed(workout: RunningWorkout): Double {
        val stageIndex = prefs.getInt("running_active_stage", 0).coerceIn(workout.stages.indices)
        return prefs.getString("running_active_speed", workout.stages[stageIndex].speedKmh.toString())?.toDoubleOrNull()
            ?: workout.stages[stageIndex].speedKmh
    }

    private fun activeRunStageRemainingKm(workout: RunningWorkout): Double {
        val stageIndex = prefs.getInt("running_active_stage", 0).coerceIn(workout.stages.indices)
        val done = prefs.getString("running_active_distance_done", "0.0")?.toDoubleOrNull() ?: 0.0
        return max(0.0, workout.stages[stageIndex].distanceKm - done)
    }

    private fun announceRunTransitionIfNeeded(workout: RunningWorkout, stageIndex: Int, remainingSeconds: Long) {
        val key = workout.id + ":" + stageIndex
        if (remainingSeconds in 28L..30L && prefs.getString("running_30_announced_key", "") != key) {
            val message = if (stageIndex < workout.stages.lastIndex) {
                "Em 30 segundos, voce termina " + workout.stages[stageIndex].title + ". Depois: " +
                    speakableStageCue(workout.stages[stageIndex + 1]) + "."
            } else {
                "Em 30 segundos, voce termina a ultima etapa e conclui o treino."
            }
            prefs.edit().putString("running_30_announced_key", key).apply()
            speakRunCue(message, flush = true)
        }
        if (prefs.getBoolean("running_voice_10_seconds", true) &&
            remainingSeconds in 8L..10L && prefs.getString("running_10_announced_key", "") != key) {
            val message = if (stageIndex < workout.stages.lastIndex) {
                "Dez segundos. Prepare para " + workout.stages[stageIndex + 1].title + "."
            } else {
                "Dez segundos para concluir o treino."
            }
            prefs.edit().putString("running_10_announced_key", key).apply()
            speakRunCue(message, flush = true)
        }
    }

    private fun repeatCurrentRunCue(workout: RunningWorkout) {
        if (!isRunningVoiceEnabled()) {
            Toast.makeText(this, "Ative a voz para repetir a instrucao.", Toast.LENGTH_SHORT).show()
            return
        }
        if (!voiceCoachReady) {
            Toast.makeText(this, "A voz do Android ainda nao esta pronta.", Toast.LENGTH_SHORT).show()
            return
        }
        val stageIndex = prefs.getInt("running_active_stage", 0).coerceIn(workout.stages.indices)
        val stage = workout.stages[stageIndex]
        val speed = currentActiveRunSpeed(workout)
        val remaining = activeRunStageRemainingKm(workout)
        val next = if (stageIndex < workout.stages.lastIndex) {
            " Proxima etapa: " + speakableStageCue(workout.stages[stageIndex + 1]) + "."
        } else {
            " Esta e a ultima etapa."
        }
        speakRunCue(
            "Etapa atual: " + stage.title + ", restam " + speakableDistance(remaining) +
                ", a " + speakableSpeed(speed) + "." + next,
            flush = true,
        )
    }

    private fun announceRunCompletion(distance: Double, seconds: Long, completed: Boolean) {
        val status = if (completed) "Treino concluido." else "Treino encerrado."
        speakRunCue(
            status + " Distancia " + speakableDistance(distance) + ". Tempo " + speakableDuration(seconds) + ".",
            flush = true,
        )
    }

    private fun initVoiceCoach() {
        voiceCoach = TextToSpeech(this) { status ->
            voiceCoachReady = false
            if (status == TextToSpeech.SUCCESS) {
                val languageStatus = voiceCoach?.setLanguage(Locale("pt", "BR")) ?: TextToSpeech.LANG_NOT_SUPPORTED
                voiceCoachReady = languageStatus != TextToSpeech.LANG_MISSING_DATA && languageStatus != TextToSpeech.LANG_NOT_SUPPORTED
                if (voiceCoachReady) {
                    voiceCoach?.setSpeechRate(0.92f)
                    voiceCoach?.setPitch(1.0f)
                    voiceCoach?.setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build(),
                    )
                }
            }
        }
        voiceCoach?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit

            override fun onDone(utteranceId: String?) {
                if (utteranceId != null && utteranceId == activeRestUtteranceId) {
                    runOnUiThread {
                        activeRestUtteranceId = null
                        releaseRestAudioFocus()
                    }
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                if (utteranceId != null && utteranceId == activeRestUtteranceId) {
                    runOnUiThread {
                        activeRestUtteranceId = null
                        releaseRestAudioFocus()
                    }
                }
            }
        })
    }

    private fun isRunningVoiceEnabled(): Boolean = prefs.getBoolean("running_voice_enabled", true)

    private fun speakRunCue(text: String, flush: Boolean = false) {
        if (!isRunningVoiceEnabled() || !voiceCoachReady) return
        val queueMode = if (flush) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
        voiceCoach?.speak(text, queueMode, null, "mo2log_run_" + System.currentTimeMillis())
    }

    private fun saveRun(distance: Double, speed: Double, notes: String) {
        val durationMinutes = ((distance / speed) * 60.0).roundToInt()
        val stages = JSONArray().put(JSONObject()
            .put("index", 0)
            .put("title", "Corrida manual")
            .put("distance", roundKm(distance))
            .put("speed", roundSpeed(speed))
            .put("planned_speed", roundSpeed(speed))
            .put("note", notes.trim()))
        val log = JSONObject()
            .put("id", UUID.randomUUID().toString())
            .put("day", dayKey())
            .put("week", weekKey())
            .put("time", timeKey())
            .put("distance", distance)
            .put("speed", speed)
            .put("duration", durationMinutes.toString() + " min")
            .put("stages", stages)
            .put("notes", notes.trim())
        val logs = runLogs()
        logs.put(log)
        prefs.edit().putString("run_logs", logs.toString()).putString("run_speed", speed.toString()).putString("run_distance", distance.toString()).apply()
        Toast.makeText(this, "Corrida salva localmente.", Toast.LENGTH_SHORT).show()
        render()
    }

    private fun exportToClipboard() {
        val payload = backupPayload().toString(2)
        prefs.edit().putString("last_backup_day", dayKey()).apply()
        copyTextToClipboard("Mo2 LOG backup", payload, "Backup JSON copiado.")
        if (currentTab == "home") render()
    }

    private fun copyTextToClipboard(label: String, text: String, toast: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
        Toast.makeText(this, toast, Toast.LENGTH_SHORT).show()
    }

    private fun backupPayload(): JSONObject {
        val preferences = JSONObject()
        val preferenceTypes = JSONObject()
        prefs.all.forEach { entry ->
            if (entry.key == preImportBackupKey) return@forEach
            when (val value = entry.value) {
                is String -> {
                    preferences.put(entry.key, value)
                    preferenceTypes.put(entry.key, "string")
                }
                is Boolean -> {
                    preferences.put(entry.key, value)
                    preferenceTypes.put(entry.key, "boolean")
                }
                is Int -> {
                    preferences.put(entry.key, value)
                    preferenceTypes.put(entry.key, "int")
                }
                is Long -> {
                    preferences.put(entry.key, value)
                    preferenceTypes.put(entry.key, "long")
                }
                is Float -> {
                    preferences.put(entry.key, value.toDouble())
                    preferenceTypes.put(entry.key, "float")
                }
                is Set<*> -> {
                    val values = JSONArray()
                    value.filterIsInstance<String>().sorted().forEach { item -> values.put(item) }
                    preferences.put(entry.key, values)
                    preferenceTypes.put(entry.key, "string_set")
                }
                else -> {
                    preferences.put(entry.key, value?.toString() ?: "")
                    preferenceTypes.put(entry.key, "string")
                }
            }
        }
        return JSONObject()
            .put("source", backupSource)
            .put("schema", backupSchema)
            .put("version", versionName)
            .put("exported_at", timestamp())
            .put("preferences", preferences)
            .put("preference_types", preferenceTypes)
            .put("strength_logs", allLogs())
            .put("strength_sessions", strengthSessionLogs())
            .put("run_logs", runLogs())
            .put("summary", JSONObject()
                .put("strength_log_count", allLogs().length())
                .put("strength_session_count", strengthSessionLogs().length())
                .put("run_log_count", runLogs().length())
                .put("custom_plan", prefs.contains("custom_workout_plans")))
    }

    private fun importBackupJson(raw: String) {
        if (raw.isBlank()) {
            Toast.makeText(this, "Cole ou copie um JSON de backup primeiro.", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val payload = JSONObject(raw.trim())
            validateBackupPayload(payload)
            showBackupImportConfirmation(payload)
        } catch (error: Exception) {
            Toast.makeText(this, error.message ?: "JSON de backup invalido.", Toast.LENGTH_LONG).show()
        }
    }

    private fun validateBackupPayload(payload: JSONObject) {
        val source = payload.optString("source")
        val schema = payload.optString("schema")
        val hasLegacyContent = payload.optJSONArray("strength_logs") != null ||
            payload.optJSONArray("strength_sessions") != null ||
            payload.optJSONArray("run_logs") != null ||
            payload.optJSONObject("goals") != null
        if (source.isNotBlank() && source != backupSource) {
            throw IllegalArgumentException("Este backup nao pertence ao Mo2 LOG Android.")
        }
        if (schema.isNotBlank() && schema !in supportedBackupSchemas) {
            throw IllegalArgumentException("Versao de backup ainda nao suportada: " + schema + ".")
        }
        if (payload.optJSONObject("preferences") == null && !hasLegacyContent) {
            throw IllegalArgumentException("O JSON nao contem dados restauraveis do Mo2 LOG.")
        }
        listOf("strength_logs", "strength_sessions", "run_logs").forEach { key ->
            if (payload.has(key) && payload.optJSONArray(key) == null) {
                throw IllegalArgumentException("O campo " + key + " esta corrompido no backup.")
            }
        }
    }

    private fun showBackupImportConfirmation(payload: JSONObject) {
        val summary = backupSummary(payload)
        val backupVersion = payload.optString("version", "legado")
        val exportedAt = payload.optString("exported_at", "data nao informada")
        val message = "Backup v" + backupVersion + " | " + exportedAt + "\n\n" +
            summary.first + " series, " + summary.second + " treinos de musculacao e " +
            summary.third + " corridas.\n\n" +
            "Antes de importar, o app guardara automaticamente uma copia dos dados atuais."
        val dialog = AlertDialog.Builder(this)
            .setTitle("Importar este backup?")
            .setMessage(message)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Importar") { _, _ -> applyBackupPayload(payload, createSafetySnapshot = true) }
            .create()
        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawable(Mo2Drawables.rounded(this, surface, Mo2Radius.Modal, border))
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(green)
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(muted)
        }
        dialog.show()
    }

    private fun backupSummary(payload: JSONObject): Triple<Int, Int, Int> {
        val summary = payload.optJSONObject("summary")
        if (summary != null) {
            return Triple(
                summary.optInt("strength_log_count"),
                summary.optInt("strength_session_count"),
                summary.optInt("run_log_count"),
            )
        }
        val preferences = payload.optJSONObject("preferences")
        return Triple(
            backupArrayLength(payload, preferences, "strength_logs", "set_logs"),
            backupArrayLength(payload, preferences, "strength_sessions", "strength_session_logs"),
            backupArrayLength(payload, preferences, "run_logs", "run_logs"),
        )
    }

    private fun backupArrayLength(payload: JSONObject, preferences: JSONObject?, payloadKey: String, preferenceKey: String): Int {
        payload.optJSONArray(payloadKey)?.let { return it.length() }
        val raw = preferences?.optString(preferenceKey).orEmpty()
        return try {
            JSONArray(raw).length()
        } catch (_: Exception) {
            0
        }
    }

    private fun applyBackupPayload(payload: JSONObject, createSafetySnapshot: Boolean) {
        val safetySnapshot = if (createSafetySnapshot) backupPayload().toString() else null
        val editor = prefs.edit()
        val preferences = payload.optJSONObject("preferences")
        val preferenceTypes = payload.optJSONObject("preference_types")
        if (preferences != null) restorePreferences(editor, preferences, preferenceTypes)

        payload.optJSONArray("strength_logs")?.let { editor.putString("set_logs", it.toString()) }
        payload.optJSONArray("strength_sessions")?.let { editor.putString("strength_session_logs", it.toString()) }
        payload.optJSONArray("run_logs")?.let { editor.putString("run_logs", it.toString()) }
        payload.optJSONObject("goals")?.let { goals ->
            editor.putString("goal_week_sets", goals.optString("week_sets", "60"))
            editor.putString("goal_week_volume", goals.optString("week_volume", "12000"))
            editor.putString("body_weight", goals.optString("body_weight", ""))
        }
        clearActiveRunState(editor)
        editor.remove("rest_timer_end_at")
        if (safetySnapshot != null) editor.putString(preImportBackupKey, safetySnapshot) else editor.remove(preImportBackupKey)
        editor.putString("last_restore_at", timestamp())
        editor.putString("last_restore_version", payload.optString("version", "legado"))

        if (!editor.commit()) {
            Toast.makeText(this, "Nao foi possivel gravar o backup neste aparelho.", Toast.LENGTH_LONG).show()
            return
        }
        syncTrainingPlanVersion()
        restorePersistedState(currentTab)
        Toast.makeText(this, if (createSafetySnapshot) "Backup importado com copia de seguranca." else "Importacao desfeita.", Toast.LENGTH_LONG).show()
        render()
    }

    private fun restorePreferences(
        editor: SharedPreferences.Editor,
        preferences: JSONObject,
        preferenceTypes: JSONObject?,
    ) {
        val keys = preferences.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            if (key == preImportBackupKey) continue
            val value = preferences.get(key)
            val explicitType = preferenceTypes?.optString(key).orEmpty()
            val type = explicitType.ifBlank { legacyPreferenceType(key, value) }
            when (type) {
                "boolean" -> editor.putBoolean(key, value.toString().toBooleanStrictOrNull() ?: false)
                "int" -> editor.putInt(key, value.toString().toIntOrNull() ?: 0)
                "long" -> editor.putLong(key, value.toString().toLongOrNull() ?: 0L)
                "float" -> editor.putFloat(key, value.toString().toFloatOrNull() ?: 0f)
                "string_set" -> {
                    val array = value as? JSONArray ?: JSONArray()
                    val values = mutableSetOf<String>()
                    for (index in 0 until array.length()) array.optString(index).takeIf { it.isNotBlank() }?.let(values::add)
                    editor.putStringSet(key, values)
                }
                else -> editor.putString(key, value.toString())
            }
        }
    }

    private fun legacyPreferenceType(key: String, value: Any): String {
        return when {
            prefs.all[key] is Long || key in setOf(
                "running_goal_5k_seconds",
                "rest_timer_end_at",
                "running_active_last_tick_at",
                "running_countdown_end_at",
                "running_active_started_at_ms",
                "running_active_pause_started_at",
                "running_active_paused_total_ms",
            ) -> "long"
            prefs.all[key] is Int -> "int"
            prefs.all[key] is Boolean -> "boolean"
            prefs.all[key] is Float -> "float"
            value is Boolean -> "boolean"
            value is Int -> "int"
            value is Long -> "long"
            else -> "string"
        }
    }

    private fun showUndoBackupImportDialog() {
        AlertDialog.Builder(this)
            .setTitle("Desfazer ultima importacao?")
            .setMessage("Os dados que estavam no aparelho antes da ultima importacao serao restaurados.")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Desfazer") { _, _ ->
                val raw = prefs.getString(preImportBackupKey, "").orEmpty()
                try {
                    val payload = JSONObject(raw)
                    validateBackupPayload(payload)
                    applyBackupPayload(payload, createSafetySnapshot = false)
                } catch (_: Exception) {
                    Toast.makeText(this, "A copia de seguranca nao esta mais valida.", Toast.LENGTH_LONG).show()
                }
            }
            .show()
    }

    private fun readClipboardText(): String {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        return clipboard.primaryClip?.getItemAt(0)?.coerceToText(this)?.toString().orEmpty()
    }

    private fun allLogs(): JSONArray = safeArray("set_logs")
    private fun runLogs(): JSONArray = safeArray("run_logs")
    private fun strengthSessionLogs(): JSONArray = safeArray("strength_session_logs")

    private fun hasCompletedStrengthSession(day: String): Boolean {
        val sessions = strengthSessionLogs()
        for (index in 0 until sessions.length()) {
            val item = sessions.getJSONObject(index)
            if (item.optString("day") == day && item.optString("status", "completed") == "completed") return true
        }
        return false
    }

    private fun safeArray(key: String): JSONArray {
        return try {
            JSONArray(prefs.getString(key, "[]") ?: "[]")
        } catch (_: Exception) {
            JSONArray()
        }
    }

    private fun safeObject(key: String): JSONObject {
        return try {
            JSONObject(prefs.getString(key, "{}") ?: "{}")
        } catch (_: Exception) {
            JSONObject()
        }
    }

    private fun favoriteCatalogIds(): MutableSet<String> {
        val ids = mutableSetOf<String>()
        val array = safeArray("favorite_catalog_ids")
        for (i in 0 until array.length()) {
            val id = array.optString(i)
            if (id.isNotBlank()) ids.add(id)
        }
        return ids
    }

    private fun saveFavoriteCatalogIds(ids: Set<String>) {
        val array = JSONArray()
        ids.sorted().forEach { id -> array.put(id) }
        prefs.edit().putString("favorite_catalog_ids", array.toString()).apply()
    }

    private fun toggleFavorite(exercise: CatalogExercise) {
        val ids = favoriteCatalogIds()
        val added = if (ids.contains(exercise.id)) {
            ids.remove(exercise.id)
            false
        } else {
            ids.add(exercise.id)
            true
        }
        saveFavoriteCatalogIds(ids)
        Toast.makeText(this, if (added) "Exercicio favoritado." else "Favorito removido.", Toast.LENGTH_SHORT).show()
        render()
    }

    private fun favoriteCatalogExercises(): List<CatalogExercise> {
        val ids = favoriteCatalogIds()
        if (ids.isEmpty()) return emptyList()
        return catalog.filter { ids.contains(it.id) }
    }

    private fun hiddenCatalogIds(): MutableSet<String> {
        val ids = mutableSetOf<String>()
        val array = safeArray("hidden_catalog_ids")
        for (i in 0 until array.length()) {
            val id = array.optString(i)
            if (id.isNotBlank()) ids.add(id)
        }
        return ids
    }

    private fun saveHiddenCatalogIds(ids: Set<String>) {
        val array = JSONArray()
        ids.sorted().forEach { id -> array.put(id) }
        prefs.edit().putString("hidden_catalog_ids", array.toString()).apply()
    }

    private fun toggleHiddenCatalogExercise(exercise: CatalogExercise) {
        val hiddenIds = hiddenCatalogIds()
        val favoriteIds = favoriteCatalogIds()
        val hidden = if (hiddenIds.contains(exercise.id)) {
            hiddenIds.remove(exercise.id)
            false
        } else {
            hiddenIds.add(exercise.id)
            favoriteIds.remove(exercise.id)
            saveFavoriteCatalogIds(favoriteIds)
            true
        }
        saveHiddenCatalogIds(hiddenIds)
        Toast.makeText(this, if (hidden) "Exercicio oculto do catalogo." else "Exercicio restaurado.", Toast.LENGTH_SHORT).show()
        render()
    }

    private fun preferredAlternativeFor(exercise: CatalogExercise): CatalogExercise? {
        val preferredId = safeObject("preferred_catalog_alternatives").optString(exercise.id)
        if (preferredId.isBlank()) return null
        return catalog.firstOrNull { it.id == preferredId }
    }

    private fun setPreferredAlternative(exercise: CatalogExercise, alternative: CatalogExercise) {
        val map = safeObject("preferred_catalog_alternatives")
        map.put(exercise.id, alternative.id)
        prefs.edit().putString("preferred_catalog_alternatives", map.toString()).apply()
        Toast.makeText(this, "Substituto preferido salvo.", Toast.LENGTH_SHORT).show()
        render()
    }

    private fun todayLogs(): JSONArray {
        val result = JSONArray()
        val logs = allLogs()
        val today = dayKey()
        for (i in 0 until logs.length()) {
            val item = logs.getJSONObject(i)
            if (item.optString("day") == today) result.put(item)
        }
        return result
    }

    private fun computeStats(logs: JSONArray): JSONObject {
        var totalVolume = 0.0
        var todayVolume = 0.0
        var weekVolume = 0.0
        var weekSets = 0
        var rpeSum = 0.0
        var rpeCount = 0
        var bestLoad = 0.0
        var bestExercise = "-"
        val today = dayKey()
        val week = weekKey()
        for (i in 0 until logs.length()) {
            val item = logs.getJSONObject(i)
            val volume = item.optDouble("load") * item.optInt("reps")
            totalVolume += volume
            if (item.optString("day") == today) todayVolume += volume
            if (item.optString("week") == week || weekKeyForDay(item.optString("day")) == week) {
                weekVolume += volume
                weekSets += 1
            }
            val rpe = item.optDouble("rpe", -1.0)
            if (rpe >= 0) {
                rpeSum += rpe
                rpeCount += 1
            }
            val load = item.optDouble("load")
            if (load > bestLoad) {
                bestLoad = load
                bestExercise = item.optString("exercise")
            }
        }
        val avgRpe = if (rpeCount == 0) "-" else String.format(Locale("pt", "BR"), "%.1f", rpeSum / rpeCount)
        return JSONObject()
            .put("total_sets", logs.length())
            .put("total_volume", totalVolume.roundToInt())
            .put("today_volume", todayVolume.roundToInt())
            .put("week_volume", weekVolume.roundToInt())
            .put("week_sets", weekSets)
            .put("avg_rpe", avgRpe)
            .put("best_load", if (bestLoad <= 0.0) "-" else bestExercise + " " + bestLoad + " kg")
    }

    private fun bestLoadsByExercise(): List<String> {
        val best = linkedMapOf<String, Double>()
        val logs = allLogs()
        for (i in 0 until logs.length()) {
            val item = logs.getJSONObject(i)
            val exercise = item.optString("exercise")
            val load = item.optDouble("load")
            if (load > (best[exercise] ?: 0.0)) best[exercise] = load
        }
        if (best.isEmpty()) return listOf("Sem cargas registradas ainda.")
        return best.entries.sortedByDescending { it.value }.take(8).map { it.key + ": " + it.value + " kg" }
    }

    private fun localInsights(): List<String> {
        val stats = computeStats(allLogs())
        val insights = mutableListOf<String>()
        when (readinessStatus()) {
            "green" -> insights.add("Check-in verde: siga o plano e use progressao controlada.")
            "yellow" -> insights.add("Check-in amarelo: mantenha intensidade moderada e nao adicione volume extra.")
            "red" -> insights.add("Check-in vermelho: priorize recuperacao; se treinar, reduza carga e ritmo.")
            else -> insights.add("Faça o check-in rapido na Home antes de decidir a intensidade do dia.")
        }
        if (stats.optInt("week_sets") < 30) insights.add("Semana ainda com volume baixo. Priorize consistencia antes de aumentar carga.")
        else insights.add("Boa consistencia semanal. Mantenha progressao gradual.")
        if (stats.optString("avg_rpe") != "-") insights.add("Use RPE medio para decidir progressao: acima de 9 pede cautela; abaixo de 7 permite subir carga.")
        if (runLogs().length() == 0) insights.add("Nenhuma corrida salva. Use Corrida para registrar esteira e controlar 5 km.")
        if (stats.optInt("total_sets") == 0) insights.add("Primeiro passo: salve uma serie real no Treino.")
        return insights
    }

    private fun smartCoachLines(): List<String> {
        val strength = strengthAdjustmentFor(currentExercise())
        val run = smartRunningAdjustment()
        return listOf(
            "Musculacao: " + currentExercise().name + " -> " + formatLoad(strength.nextLoad) + " em " + strength.suggestedSetCount + " series.",
            "Carga: " + strength.loadReason,
            "Corrida: " + run.headline + " -> " + signedSpeedOffset(run.speedOffset) + " e distancia x" + String.format(Locale.US, "%.2f", run.distanceScale) + ".",
            "Ritmo: " + run.reason,
        )
    }

    private fun lastLoadFor(exercise: String): String {
        val logs = allLogs()
        for (i in logs.length() - 1 downTo 0) {
            val item = logs.getJSONObject(i)
            if (item.optString("exercise") == exercise) {
                val value = item.optDouble("load")
                return if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()
            }
        }
        return "0"
    }

    private fun lastSetFor(exercise: String): JSONObject? {
        val logs = allLogs()
        for (i in logs.length() - 1 downTo 0) {
            val item = logs.getJSONObject(i)
            if (item.optString("exercise") == exercise) return item
        }
        return null
    }

    private fun editorPlanIndex(): Int {
        return prefs.getInt("editor_plan", selectedPlanIndex).coerceIn(plans.indices)
    }

    private fun editorExerciseIndex(plan: WorkoutPlan): Int {
        return prefs.getInt("editor_exercise", 0).coerceIn(plan.exercises.indices)
    }

    private fun saveWorkoutPlanDetails(planIndex: Int, titleRaw: String, focusRaw: String) {
        val title = titleRaw.ifBlank { "Treino" }
        val focus = focusRaw.ifBlank { "Dia e foco" }
        val updated = plans.toMutableList()
        updated[planIndex] = updated[planIndex].copy(title = title, focus = focus)
        saveWorkoutPlans(updated)
        Toast.makeText(this, "Treino salvo.", Toast.LENGTH_SHORT).show()
        render()
    }

    private fun saveExerciseDetails(planIndex: Int, exerciseIndex: Int, nameRaw: String, targetRaw: String, restRaw: String, notesRaw: String) {
        val exercises = plans[planIndex].exercises.toMutableList()
        exercises[exerciseIndex] = ExercisePlan(
            name = nameRaw.ifBlank { "Exercicio" },
            target = targetRaw.ifBlank { "3 x 10" },
            rest = restRaw.ifBlank { "60s" },
            notes = notesRaw.ifBlank { "Sem notas." },
        )
        replacePlanExercises(planIndex, exercises)
        Toast.makeText(this, "Exercicio salvo.", Toast.LENGTH_SHORT).show()
        render()
    }

    private fun replacePlanExercises(planIndex: Int, exercises: List<ExercisePlan>) {
        val updated = plans.toMutableList()
        updated[planIndex] = updated[planIndex].copy(exercises = exercises)
        saveWorkoutPlans(updated)
    }

    private fun saveWorkoutPlans(updated: List<WorkoutPlan>) {
        prefs.edit()
            .putString("custom_workout_plans", workoutPlansToJson(updated).toString())
            .putInt("selected_plan", selectedPlanIndex.coerceIn(updated.indices))
            .putInt("selected_exercise", selectedExerciseIndex.coerceAtLeast(0))
            .apply()
    }

    private fun workoutPlansToJson(items: List<WorkoutPlan>): JSONArray {
        val array = JSONArray()
        items.forEach { plan ->
            val exercises = JSONArray()
            plan.exercises.forEach { exercise ->
                exercises.put(JSONObject()
                    .put("name", exercise.name)
                    .put("target", exercise.target)
                    .put("rest", exercise.rest)
                    .put("notes", exercise.notes))
            }
            array.put(JSONObject()
                .put("id", plan.id)
                .put("title", plan.title)
                .put("focus", plan.focus)
                .put("exercises", exercises))
        }
        return array
    }

    private fun customWorkoutPlans(): List<WorkoutPlan>? {
        val raw = prefs.getString("custom_workout_plans", null) ?: return null
        return try {
            val array = JSONArray(raw)
            val result = mutableListOf<WorkoutPlan>()
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                val exercisesArray = item.optJSONArray("exercises") ?: JSONArray()
                val exercises = mutableListOf<ExercisePlan>()
                for (j in 0 until exercisesArray.length()) {
                    val exercise = exercisesArray.getJSONObject(j)
                    exercises.add(ExercisePlan(
                        name = exercise.optString("name").ifBlank { "Exercicio" },
                        target = exercise.optString("target").ifBlank { "3 x 10" },
                        rest = exercise.optString("rest").ifBlank { "60s" },
                        notes = exercise.optString("notes").ifBlank { "Sem notas." },
                    ))
                }
                result.add(WorkoutPlan(
                    id = item.optString("id").ifBlank { "custom-" + i },
                    title = item.optString("title").ifBlank { "Treino" },
                    focus = item.optString("focus").ifBlank { "Dia e foco" },
                    exercises = exercises.ifEmpty { listOf(ExercisePlan("Exercicio", "3 x 10", "60s", "Sem notas.")) },
                ))
            }
            result.ifEmpty { null }
        } catch (_: Exception) {
            null
        }
    }

    private fun buildRunningPlan(): List<RunningWorkout> {
        val plan = mutableListOf<RunningWorkout>()
        for (week in 1..6) plan.addAll(buildRunningWeek(week))
        return plan
    }

    private fun buildRunningWeek(week: Int): List<RunningWorkout> {
        val longDistance = when (week) {
            1 -> 4.0
            2 -> 4.5
            3 -> 5.0
            4 -> 5.5
            5 -> 5.8
            else -> 4.2
        }
        val tempoDistance = when (week) {
            1, 2 -> 1.8
            3, 4 -> 2.2
            5 -> 2.6
            else -> 1.5
        }
        val raw = listOf(
            RunningWorkout(
                "w" + week + "-mon",
                week,
                "Segunda",
                1,
                if (week <= 2) "Tiros 400 m" else if (week <= 4) "Tiros 600 m" else "Tiros longos",
                "Velocidade e tolerancia a ritmo forte",
                "Sem musculacao neste dia. A corrida e o treino principal da noite.",
                intervalStages(week),
            ),
            RunningWorkout(
                "w" + week + "-tue",
                week,
                "Terca",
                2,
                "Leve pos Treino A",
                "Soltar depois de peito, ombro e triceps",
                "Faca depois da musculacao, mantendo conversa facil e sem buscar recorde.",
                listOf(
                    RunningStage("Leve continuo", if (week <= 2) 2.4 else if (week <= 4) 2.8 else 3.0, 7.2, "Respiracao controlada."),
                ),
            ),
            RunningWorkout(
                "w" + week + "-thu",
                week,
                "Quinta",
                4,
                "Regenerativo pos pernas",
                "Baixo impacto depois do Treino B",
                "Use como circulacao e tecnica. Se a perna pesar, reduza velocidade.",
                listOf(
                    RunningStage("Regenerativo", if (week <= 3) 1.8 else 2.2, 6.8, "Sem forcar apos pernas."),
                ),
            ),
            RunningWorkout(
                "w" + week + "-sat",
                week,
                "Sabado",
                6,
                "Ritmo controlado",
                "Aproximar do ritmo de prova",
                "Depois do Treino C, faca aquecimento curto e um bloco firme sem sprintar.",
                listOf(
                    RunningStage("Aquecimento", 0.8, 7.0, "Leve e progressivo."),
                    RunningStage("Ritmo", tempoDistance, if (week <= 2) 8.6 else if (week <= 4) 9.0 else 9.3, "RPE 7, firme e sustentavel."),
                    RunningStage("Soltar", 0.6, 6.8, "Baixe a frequencia cardiaca."),
                ),
            ),
            RunningWorkout(
                "w" + week + "-sun",
                week,
                "Domingo",
                7,
                "Longo leve",
                "Construir base aerobica",
                "Corrida facil. O objetivo e acumular tempo sem transformar em prova.",
                listOf(
                    RunningStage("Longo leve", longDistance, 7.0, "Confortavel do inicio ao fim."),
                ),
            ),
        )
        return raw.map { workout ->
            workout.copy(stages = workout.stages.map { stage -> personalizedRunningStage(stage) })
        }
    }

    private fun personalizedRunningStage(stage: RunningStage): RunningStage {
        val speedOffset = prefs.getString("running_speed_offset", "0.0")?.toDoubleOrNull() ?: 0.0
        val distanceScale = (prefs.getString("running_distance_scale", "1.0")?.toDoubleOrNull() ?: 1.0).coerceIn(0.60, 1.40)
        return stage.copy(
            distanceKm = roundKm(stage.distanceKm * distanceScale),
            speedKmh = roundSpeed((stage.speedKmh + speedOffset).coerceIn(4.0, 18.0)),
        )
    }

    private fun intervalStages(week: Int): List<RunningStage> {
        val stages = mutableListOf<RunningStage>()
        stages.add(RunningStage("Aquecimento", 1.0, 7.0, "Comece facil."))
        val repeats = when (week) {
            1 -> 6
            2 -> 7
            3, 4 -> 5
            5 -> 4
            else -> 3
        }
        val hardDistance = when (week) {
            1, 2 -> 0.4
            3, 4 -> 0.6
            5 -> 0.8
            else -> 1.0
        }
        val recoveryDistance = when (week) {
            1, 2 -> 0.2
            3, 4 -> 0.3
            else -> 0.35
        }
        val hardSpeed = when (week) {
            1 -> 9.6
            2 -> 9.8
            3 -> 10.0
            4 -> 10.2
            5 -> 10.3
            else -> 9.8
        }
        for (index in 1..repeats) {
            stages.add(RunningStage("Tiro " + index, hardDistance, hardSpeed, "Forte, sem perder tecnica."))
            if (index < repeats) stages.add(RunningStage("Recuperacao " + index, recoveryDistance, 6.6, "Caminhe ou trote leve."))
        }
        stages.add(RunningStage("Soltar", 0.8, 6.8, "Finalize leve."))
        return stages
    }

    private fun runningFiveKmForecast(): RunningForecast {
        val goal = prefs.getLong("running_goal_5k_seconds", 1800L).coerceIn(900L, 7200L)
        val samples = recentRunLogItems(20)
            .filterNot { it.optBoolean("manual", false) }
            .mapNotNull { item ->
                projectedFiveKmSeconds(item)?.let { seconds -> Pair(seconds, item.optDouble("distance")) }
            }
            .take(8)

        if (samples.isEmpty()) {
            val referenceSpeed = currentRunningWeekWorkouts()
                .flatMap { it.stages }
                .firstOrNull { it.title.equals("Ritmo", ignoreCase = true) }
                ?.speedKmh
                ?.takeIf { it > 0.0 }
                ?: 8.0
            val predicted = ((5.0 / referenceSpeed) * 3600.0).roundToInt().toLong().coerceIn(900L, 7200L)
            return RunningForecast(
                predictedSeconds = predicted,
                goalSeconds = goal,
                sampleCount = 0,
                confidence = "Inicial",
                source = "Referencia do bloco de ritmo da semana",
                trendSeconds = null,
                usesMeasuredRuns = false,
            )
        }

        val recent = samples.take(5)
        val predicted = medianSeconds(recent.map { it.first })
        val distance = recent.sumOf { it.second }
        val confidence = when {
            recent.size >= 5 && distance >= 20.0 -> "Alta"
            recent.size >= 2 && distance >= 6.0 -> "Media"
            else -> "Baixa"
        }
        val trend = if (samples.size >= 4) {
            medianSeconds(samples.take(2).map { it.first }) - medianSeconds(samples.drop(2).take(2).map { it.first })
        } else {
            null
        }
        return RunningForecast(
            predictedSeconds = predicted,
            goalSeconds = goal,
            sampleCount = recent.size,
            confidence = confidence,
            source = "Mediana de " + recent.size + " corrida" + (if (recent.size == 1) " valida" else "s validas"),
            trendSeconds = trend,
            usesMeasuredRuns = true,
        )
    }

    private fun projectedFiveKmSeconds(item: JSONObject): Long? {
        val distance = item.optDouble("distance")
        if (distance !in 1.0..20.0) return null
        val pace = historyRunPaceSeconds(item)
        if (pace <= 0L) return null
        val measuredSeconds = pace.toDouble() * distance
        val projected = (measuredSeconds * (5.0 / distance).pow(1.06)).roundToInt().toLong()
        return projected.takeIf { it in 900L..7200L }
    }

    private fun medianSeconds(values: List<Long>): Long {
        if (values.isEmpty()) return 0L
        val sorted = values.sorted()
        val middle = sorted.size / 2
        return if (sorted.size % 2 == 1) sorted[middle] else (sorted[middle - 1] + sorted[middle]) / 2L
    }

    private fun validRecentRunItems(limit: Int): List<JSONObject> {
        return recentRunLogItems(max(limit * 3, limit))
            .filterNot { it.optBoolean("manual", false) }
            .filter { projectedFiveKmSeconds(it) != null }
            .take(limit)
    }

    private fun smartRunningAdjustment(): RunningAdjustment {
        val currentOffset = prefs.getString("running_speed_offset", "0.0")?.toDoubleOrNull() ?: 0.0
        val currentScale = prefs.getString("running_distance_scale", "1.0")?.toDoubleOrNull() ?: 1.0
        val recent = validRecentRunItems(6)
        val weekWorkouts = currentRunningWeekWorkouts()
        val completedThisWeek = validCompletedRunCount(weekWorkouts)
        val plannedAvgSpeed = plannedAverageSpeed(weekWorkouts)
        val forecast = runningFiveKmForecast()
        val recentSpeeds = recent.mapNotNull { item ->
            historyRunPaceSeconds(item).takeIf { it > 0L }?.let { 3600.0 / it.toDouble() }
        }
        val avgRecentSpeed = recentSpeeds.takeIf { it.isNotEmpty() }?.average()
        val recentRpe = recent.map { it.optInt("rpe", 0) }.filter { it in 1..10 }
        val avgRpe = recentRpe.takeIf { it.isNotEmpty() }?.average()
        val readiness = readinessStatus()
        var suggestedOffset = currentOffset
        var suggestedScale = currentScale
        val headline: String
        val reason: String

        when {
            readiness == "red" || (avgRpe != null && avgRpe >= 9.0) -> {
                suggestedOffset = (currentOffset - 0.2).coerceIn(-2.0, 2.0)
                suggestedScale = (currentScale - 0.04).coerceIn(0.60, 1.40)
                headline = "Reduzir e recuperar"
                reason = "Prontidao ou RPE recente pede cautela. Reduza velocidade e distancia antes de voltar a progredir."
            }
            readiness == "yellow" || (avgRpe != null && avgRpe >= 8.0) || (forecast.trendSeconds ?: 0L) >= 45L -> {
                suggestedOffset = (currentOffset - 0.1).coerceIn(-2.0, 2.0)
                suggestedScale = (currentScale - 0.02).coerceIn(0.60, 1.40)
                headline = "Consolidar esta semana"
                reason = "O esforco ou a tendencia de 5 km subiu. Um ajuste pequeno ajuda a recuperar consistencia sem abandonar o ciclo."
            }
            completedThisWeek >= 4 && avgRecentSpeed != null && avgRecentSpeed >= plannedAvgSpeed - 0.2 &&
                (avgRpe == null || avgRpe <= 7.5) && (forecast.trendSeconds == null || forecast.trendSeconds <= 10L) -> {
                suggestedOffset = (currentOffset + 0.1).coerceIn(-2.0, 2.0)
                suggestedScale = (currentScale + 0.02).coerceIn(0.60, 1.40)
                headline = "Subir levemente"
                reason = "Consistencia, RPE e previsao de 5 km estao controlados. Proximo passo: +0,1 km/h e um pequeno aumento de distancia."
            }
            completedThisWeek >= 2 && avgRecentSpeed != null && avgRecentSpeed >= plannedAvgSpeed &&
                (avgRpe == null || avgRpe <= 7.5) -> {
                suggestedOffset = (currentOffset + 0.1).coerceIn(-2.0, 2.0)
                headline = "Acelerar um pouco"
                reason = "Ritmo valido esta igual ou acima do planejado com esforco controlado. Aumente somente 0,1 km/h."
            }
            recent.isEmpty() -> {
                headline = "Base atual mantida"
                reason = "Ainda nao ha corrida com ritmo plausivel para adaptar o plano. Registros incompletos ou de teste sao ignorados."
            }
            completedThisWeek <= 1 && currentRunningPlanWeek() > 1 -> {
                suggestedOffset = (currentOffset - 0.1).coerceIn(-2.0, 2.0)
                suggestedScale = (currentScale - 0.02).coerceIn(0.60, 1.40)
                headline = "Consolidar antes de subir"
                reason = "Poucos treinos concluidos nesta semana. Reduza levemente para voltar a consistencia."
            }
            else -> {
                headline = "Manter ritmo"
                reason = "Consistencia e previsao estao estaveis. Mantenha o ajuste atual e registre a proxima corrida com RPE."
            }
        }

        return RunningAdjustment(
            speedOffset = roundSpeed(suggestedOffset),
            distanceScale = roundScale(suggestedScale),
            headline = headline,
            reason = reason,
        )
    }

    private fun applySmartRunningAdjustment(adjustment: RunningAdjustment) {
        prefs.edit()
            .putString("running_speed_offset", adjustment.speedOffset.toString())
            .putString("running_distance_scale", adjustment.distanceScale.toString())
            .remove("selected_run_id")
            .apply()
        clearActiveRun()
        Toast.makeText(this, "Ajuste de corrida aplicado.", Toast.LENGTH_SHORT).show()
        render()
    }

    private fun recentRunLogItems(limit: Int): List<JSONObject> {
        val logs = runLogs()
        val result = mutableListOf<JSONObject>()
        for (i in logs.length() - 1 downTo 0) {
            result.add(logs.getJSONObject(i))
            if (result.size >= limit) break
        }
        return result
    }

    private fun plannedAverageSpeed(workouts: List<RunningWorkout>): Double {
        val distance = workouts.sumOf { totalRunDistance(it) }
        val seconds = workouts.sumOf { estimatedWorkoutSeconds(it) }
        return if (distance <= 0.0 || seconds <= 0L) 0.0 else distance / (seconds.toDouble() / 3600.0)
    }

    private fun validCompletedRunCount(workouts: List<RunningWorkout>): Int {
        val workoutIds = workouts.map { it.id }.toSet()
        val completedIds = mutableSetOf<String>()
        val logs = runLogs()
        for (index in 0 until logs.length()) {
            val item = logs.getJSONObject(index)
            val workoutId = item.optString("run_workout_id")
            if (workoutId in workoutIds && !item.optBoolean("manual", false) && projectedFiveKmSeconds(item) != null) {
                completedIds.add(workoutId)
            }
        }
        return completedIds.size
    }

    private fun completedRunningDistance(workouts: List<RunningWorkout>): Double {
        val workoutIds = workouts.map { it.id }.toSet()
        val logs = runLogs()
        var distance = 0.0
        for (index in 0 until logs.length()) {
            val item = logs.getJSONObject(index)
            if (item.optString("run_workout_id") in workoutIds) distance += item.optDouble("distance")
        }
        return roundKm(distance)
    }

    private fun signedSpeedOffset(value: Double): String {
        val rounded = roundSpeed(value)
        val sign = if (rounded > 0.0) "+" else ""
        return sign + String.format(Locale("pt", "BR"), "%.1f km/h", rounded)
    }

    private fun roundScale(value: Double): Double = (value * 100.0).roundToInt().toDouble() / 100.0

    private fun treadmillSpeedPlan(workout: RunningWorkout): String {
        val parts = workout.stages
            .distinctBy { stage -> stage.title + stage.speedKmh.toString() }
            .take(5)
            .map { stage -> stage.title + " " + formatSpeed(stage.speedKmh) }
        val suffix = if (workout.stages.size > 5) "..." else ""
        return (parts + suffix).filter { it.isNotBlank() }.joinToString(" | ")
    }

    private fun currentRunningWeekWorkouts(): List<RunningWorkout> {
        val week = currentRunningPlanWeek()
        return runningPlan.filter { it.week == week }.ifEmpty { runningPlan.take(5) }
    }

    private fun currentRunningPlanWeek(): Int {
        val startKey = prefs.getString("running_plan_start_day", dayKey()) ?: dayKey()
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val start = try { parser.parse(startKey)?.time ?: System.currentTimeMillis() } catch (_: Exception) { System.currentTimeMillis() }
        val today = try { parser.parse(dayKey())?.time ?: System.currentTimeMillis() } catch (_: Exception) { System.currentTimeMillis() }
        val days = max(0L, (today - start) / 86400000L)
        return ((days / 7L) + 1L).toInt().coerceIn(1, 6)
    }

    private fun todayRunningWorkout(): RunningWorkout? {
        runningPlan.firstOrNull { workout ->
            !isRunWorkoutCompleted(workout) && scheduledDayFor(workout) == dayKey()
        }?.let { return it }
        val workouts = currentRunningWeekWorkouts()
        return workouts
            .filterNot { isRunWorkoutCompleted(it) }
            .sortedBy { scheduledDayFor(it) }
            .firstOrNull { scheduledDayFor(it) >= dayKey() }
            ?: workouts.firstOrNull { !isRunWorkoutCompleted(it) }
            ?: workouts.firstOrNull()
    }

    private fun scheduledDayFor(workout: RunningWorkout): String {
        val override = safeObject("running_schedule_overrides").optString(workout.id)
        if (isValidDayKey(override)) return override
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        parser.isLenient = false
        val start = try {
            parser.parse(prefs.getString("running_plan_start_day", dayKey()).orEmpty()) ?: Date()
        } catch (_: Exception) {
            Date()
        }
        val calendar = Calendar.getInstance()
        calendar.time = start
        while (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }
        calendar.add(Calendar.WEEK_OF_YEAR, workout.week - 1)
        calendar.add(Calendar.DAY_OF_YEAR, workout.dayIndex - 1)
        return parser.format(calendar.time)
    }

    private fun isValidDayKey(value: String): Boolean {
        if (!Regex("\\d{4}-\\d{2}-\\d{2}").matches(value)) return false
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        parser.isLenient = false
        return try {
            parser.parse(value) != null
        } catch (_: Exception) {
            false
        }
    }

    private fun formatDayForDisplay(value: String): String {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        parser.isLenient = false
        return try {
            val parsed = parser.parse(value) ?: return value
            SimpleDateFormat("dd/MM", Locale("pt", "BR")).format(parsed)
        } catch (_: Exception) {
            value
        }
    }

    private fun runningWorkoutById(id: String): RunningWorkout? = runningPlan.firstOrNull { it.id == id }

    private fun isRunWorkoutCompleted(workout: RunningWorkout): Boolean {
        val logs = runLogs()
        for (i in 0 until logs.length()) {
            val item = logs.getJSONObject(i)
            if (item.optString("run_workout_id") == workout.id) return true
        }
        return false
    }

    private fun totalRunDistance(workout: RunningWorkout): Double = workout.stages.sumOf { it.distanceKm }

    private fun estimatedWorkoutSeconds(workout: RunningWorkout): Long {
        return workout.stages.sumOf { stage ->
            if (stage.speedKmh <= 0.0) 0L else ((stage.distanceKm / stage.speedKmh) * 3600.0).roundToInt().toLong()
        }
    }

    private fun stageCue(stage: RunningStage?): String {
        if (stage == null) return "-"
        return stage.title + " - " + formatKm(stage.distanceKm) + " a " + formatSpeed(stage.speedKmh)
    }

    private fun speakableStageCue(stage: RunningStage): String {
        return stage.title + ", " + speakableDistance(stage.distanceKm) + ", a " + speakableSpeed(stage.speedKmh)
    }

    private fun speakableDistance(distanceKm: Double): String {
        return if (distanceKm < 1.0) {
            (distanceKm * 1000.0).roundToInt().toString() + " metros"
        } else {
            String.format(Locale("pt", "BR"), "%.2f quilometros", distanceKm)
        }
    }

    private fun speakableSpeed(speedKmh: Double): String {
        return String.format(Locale("pt", "BR"), "%.1f quilometros por hora", speedKmh)
    }

    private fun speakableDuration(totalSeconds: Long): String {
        val safeSeconds = totalSeconds.coerceAtLeast(0L)
        val hours = safeSeconds / 3600L
        val minutes = (safeSeconds % 3600L) / 60L
        val seconds = safeSeconds % 60L
        val parts = mutableListOf<String>()
        if (hours > 0L) parts.add(hours.toString() + if (hours == 1L) " hora" else " horas")
        if (minutes > 0L) parts.add(minutes.toString() + if (minutes == 1L) " minuto" else " minutos")
        if (seconds > 0L || parts.isEmpty()) parts.add(seconds.toString() + if (seconds == 1L) " segundo" else " segundos")
        return parts.joinToString(" e ")
    }

    private fun formatKm(value: Double): String = String.format(Locale("pt", "BR"), "%.2f km", value)

    private fun formatSpeed(value: Double): String = String.format(Locale("pt", "BR"), "%.1f km/h", value)

    private fun formatPaceForSpeed(speed: Double): String {
        if (speed <= 0.0) return "- /km"
        return formatPaceSeconds((3600.0 / speed).roundToInt().toLong())
    }

    private fun formatPaceSeconds(secondsPerKm: Long): String {
        if (secondsPerKm <= 0L) return "- /km"
        val minutes = secondsPerKm / 60L
        val seconds = secondsPerKm % 60L
        return minutes.toString() + ":" + seconds.toString().padStart(2, '0') + " /km"
    }

    private fun roundKm(value: Double): Double = (value * 100.0).roundToInt().toDouble() / 100.0

    private fun roundSpeed(value: Double): Double = (value * 10.0).roundToInt().toDouble() / 10.0

    private fun currentPlan() = plans[selectedPlanIndex.coerceIn(plans.indices)]
    private fun currentExercise() = currentPlan().exercises[selectedExerciseIndex.coerceIn(currentPlan().exercises.indices)]

    private fun buildWorkoutPlans(): List<WorkoutPlan> = customWorkoutPlans() ?: defaultWorkoutPlans()

    private fun defaultWorkoutPlans(): List<WorkoutPlan> = listOf(
        WorkoutPlan("a", "Treino A", "Terca - Peito/Ombro/Triceps + corrida leve", listOf(
            ExercisePlan("Supino reto", "4 x 8-10", "90s", "Controle a descida e mantenha amplitude segura."),
            ExercisePlan("Maquina peitoral", "3 x 10-12", "75s", "Mantenha as escapulas apoiadas e controle a volta."),
            ExercisePlan("Supino inclinado com halteres", "3 x 10", "90s", "Suba carga quando fechar reps com tecnica limpa."),
            ExercisePlan("Desenvolvimento de ombros", "3 x 8-10", "90s", "Evite compensar com lombar."),
            ExercisePlan("Elevacao lateral", "3 x 12-15", "60s", "Cadencia limpa, sem embalo."),
            ExercisePlan("Triceps corda", "3 x 10-12", "60s", "Trave cotovelos perto do corpo."),
            ExercisePlan("Prancha", "3 x 45s", "45s", "Respiracao constante."),
        )),
        WorkoutPlan("b", "Treino B", "Quinta - Pernas/Core + corrida curta", listOf(
            ExercisePlan("Leg press", "4 x 10", "120s", "Amplitude segura e constante. Depois faca so 10-15 min leve."),
            ExercisePlan("Agachamento livre", "3 x 8", "120s", "Priorize tecnica e estabilidade antes de carga."),
            ExercisePlan("Agachamento guiado", "3 x 8-10", "90s", "Ajuste os pes e mantenha o tronco firme no trilho."),
            ExercisePlan("Cadeira extensora", "3 x 12", "75s", "Segure um segundo no topo."),
            ExercisePlan("Mesa flexora", "3 x 10-12", "75s", "Controle total na volta."),
            ExercisePlan("Stiff", "3 x 10", "90s", "Quadril para tras, coluna neutra."),
            ExercisePlan("Panturrilha", "4 x 12-15", "45s", "Pausa no alongamento."),
            ExercisePlan("Abdominal", "3 x 12-15", "45s", "Expire durante a contracao e evite puxar o pescoco."),
            ExercisePlan("Prancha", "3 x 45s", "45s", "Mantenha quadril, tronco e ombros alinhados."),
        )),
        WorkoutPlan("c", "Treino C", "Sabado - Costas/Biceps + corrida ritmo", listOf(
            ExercisePlan("Puxada frente", "4 x 8-10", "90s", "Puxe com cotovelos, nao com as maos."),
            ExercisePlan("Remada baixa", "4 x 10", "90s", "Pausa curta na contracao."),
            ExercisePlan("Remada unilateral", "3 x 10 cada", "75s", "Mantenha tronco firme."),
            ExercisePlan("Face pull", "3 x 12-15", "60s", "Foco em deltoide posterior."),
            ExercisePlan("Rosca direta", "3 x 8-10", "60s", "Controle na descida."),
            ExercisePlan("Rosca martelo", "3 x 10-12", "60s", "Punho neutro."),
        )),
    )

    private fun todayPlanIndex(): Int {
        val day = SimpleDateFormat("u", Locale.US).format(Date()).toIntOrNull() ?: 1
        return when (day) {
            2 -> 0
            4 -> 1
            6 -> 2
            1 -> 0
            3 -> 1
            5 -> 2
            else -> 0
        }
    }

    private fun estimatedRunTime(distanceRaw: String, speedRaw: String): String {
        val distance = distanceRaw.replace(',', '.').toDoubleOrNull() ?: return "-"
        val speed = speedRaw.replace(',', '.').toDoubleOrNull() ?: return "-"
        if (distance <= 0.0 || speed <= 0.0) return "-"
        return ((distance / speed) * 60.0).roundToInt().toString() + " min"
    }

    private fun switchTab(tab: String) {
        currentTab = tab
        prefs.edit().putString("current_tab", tab).apply()
        render()
    }

    private fun currentSectionTitle(): String {
        if (currentTab == "home") return "Essa Semana"
        return navItems.firstOrNull { it.id == currentTab }?.title ?: "Inicio"
    }

    private fun currentSectionSubtitle(): String {
        return when (currentTab) {
            "home" -> ""
            "workout" -> "Registro guiado com timer, midia de execucao, edicao e desfazer serie."
            "running" -> "Semana de corrida, planejamento 5 km e treino guiado por fases."
            "more" -> "Tudo que nao precisa ficar no menu principal, organizado por ferramenta."
            "plan_editor" -> "Edite seu plano pessoal de musculacao e corrida direto no celular."
            "exercises" -> "Catalogo completo com busca, midia por link, favoritos e alternativas."
            "history" -> "Calendario, recordes, graficos e edicao completa dos registros locais."
            "stats" -> "Indicadores de volume, frequencia e melhores cargas."
            "goals" -> "Metas semanais para manter treino e corrida em movimento."
            "coach" -> "Leitura simples do seu momento para ajustar a proxima sessao."
            "profile" -> "Configuracoes locais, exportacao e versao instalada."
            else -> "Resumo rapido para abrir o treino certo no menor numero de toques."
        }
    }

    private fun startRestTimerForCurrentExercise() {
        val exercise = currentExercise()
        startRestTimer(restSecondsFor(exercise.name), exercise.name)
    }

    private fun startRestTimer(seconds: Int, exercise: String) {
        if (seconds <= 0) return
        stopRestCompletionAlert()
        prefs.edit()
            .putLong("rest_timer_end_at", System.currentTimeMillis() + seconds * 1000L)
            .putInt("rest_timer_duration_secs", seconds)
            .putString("rest_timer_exercise", exercise)
            .putBoolean("rest_timer_notified", false)
            .apply()
        Toast.makeText(this, "Descanso iniciado: " + formatDuration(seconds.toLong()), Toast.LENGTH_SHORT).show()
        if (currentTab == "workout") renderWorkoutInPlace() else render()
    }

    private fun adjustRestTime(seconds: Int) {
        val now = System.currentTimeMillis()
        val currentEnd = prefs.getLong("rest_timer_end_at", 0L)
        if (currentEnd <= now && seconds < 0) {
            Toast.makeText(this, "O descanso ja esta pronto.", Toast.LENGTH_SHORT).show()
            return
        }
        val currentRemaining = if (currentEnd > now) restTimerRemainingSeconds() else 0L
        val adjustedRemaining = (currentRemaining + seconds).coerceAtLeast(0L)
        val currentDuration = prefs.getInt("rest_timer_duration_secs", currentRemaining.toInt())
        val adjustedDuration = if (currentEnd > now) {
            (currentDuration + seconds).coerceAtLeast(0)
        } else {
            adjustedRemaining.toInt()
        }
        prefs.edit()
            .putLong("rest_timer_end_at", now + adjustedRemaining * 1000L)
            .putInt("rest_timer_duration_secs", adjustedDuration)
            .putString("rest_timer_exercise", prefs.getString("rest_timer_exercise", currentExercise().name) ?: currentExercise().name)
            .putBoolean("rest_timer_notified", false)
            .apply()
        if (adjustedRemaining <= 0L) notifyRestTimerFinishedIfNeeded()
        Toast.makeText(this, if (seconds > 0) "+30s no descanso." else "-30s no descanso.", Toast.LENGTH_SHORT).show()
        if (currentTab == "workout") renderWorkoutInPlace() else render()
    }

    private fun clearRestTimer() {
        prefs.edit()
            .remove("rest_timer_end_at")
            .remove("rest_timer_duration_secs")
            .remove("rest_timer_exercise")
            .remove("rest_timer_notified")
            .apply()
        stopRestCompletionAlert()
        Toast.makeText(this, "Descanso parado.", Toast.LENGTH_SHORT).show()
        if (currentTab == "workout") renderWorkoutInPlace() else render()
    }

    private fun notifyRestTimerFinishedIfNeeded() {
        val endAt = prefs.getLong("rest_timer_end_at", 0L)
        if (endAt <= 0L || System.currentTimeMillis() < endAt) return
        if (prefs.getBoolean("rest_timer_notified", false)) return
        prefs.edit().putBoolean("rest_timer_notified", true).apply()
        playRestCompletionAlert()
    }

    private fun playRestCompletionAlert() {
        stopRestCompletionAlert()
        requestRestAudioFocus()
        if (voiceCoachReady) {
            val utteranceId = "mo2log_rest_" + System.currentTimeMillis()
            activeRestUtteranceId = utteranceId
            val result = voiceCoach?.speak(
                "Descanso finalizado. Inicie a proxima serie.",
                TextToSpeech.QUEUE_FLUSH,
                null,
                utteranceId,
            )
            if (result == TextToSpeech.SUCCESS) {
                restTimerHandler.postDelayed({
                    if (activeRestUtteranceId == utteranceId) {
                        activeRestUtteranceId = null
                        releaseRestAudioFocus()
                    }
                }, 7000L)
                return
            }
            activeRestUtteranceId = null
        }
        playRestFallbackTone()
    }

    private fun requestRestAudioFocus() {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(attributes)
            .setAcceptsDelayedFocusGain(false)
            .setWillPauseWhenDucked(false)
            .setOnAudioFocusChangeListener(restAudioFocusListener, restTimerHandler)
            .build()
        restAudioFocusRequest = request
        audioManager.requestAudioFocus(request)
    }

    private fun releaseRestAudioFocus() {
        restAudioFocusRequest?.let { request -> audioManager.abandonAudioFocusRequest(request) }
        restAudioFocusRequest = null
    }

    private fun stopRestCompletionAlert() {
        if (activeRestUtteranceId != null) voiceCoach?.stop()
        activeRestUtteranceId = null
        restToneGenerator?.stopTone()
        restToneGenerator?.release()
        restToneGenerator = null
        releaseRestAudioFocus()
    }

    private fun playRestFallbackTone() {
        try {
            restToneGenerator?.release()
            val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
            restToneGenerator = tone
            tone.startTone(ToneGenerator.TONE_PROP_BEEP2, 260)
            restTimerHandler.postDelayed({
                tone.startTone(ToneGenerator.TONE_PROP_ACK, 520)
                restTimerHandler.postDelayed({
                    if (restToneGenerator === tone) {
                        tone.release()
                        restToneGenerator = null
                        releaseRestAudioFocus()
                    }
                }, 560L)
            }, 300L)
        } catch (_: Exception) {
            releaseRestAudioFocus()
            Toast.makeText(this, "Descanso finalizado.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun restTimerRemainingSeconds(): Long {
        val endAt = prefs.getLong("rest_timer_end_at", 0L)
        if (endAt <= 0L) return 0L
        return max(0L, ((endAt - System.currentTimeMillis()) + 999L) / 1000L)
    }

    private fun restTimerDisplay(): String {
        val remaining = restTimerRemainingSeconds()
        return if (remaining <= 0L) "Pronto" else formatDuration(remaining)
    }

    private fun restTimerSubtitle(): String {
        val exercise = prefs.getString("rest_timer_exercise", currentExercise().name) ?: currentExercise().name
        val duration = prefs.getInt("rest_timer_duration_secs", restSecondsFor(currentExercise().name))
        return if (restTimerRemainingSeconds() > 0L) {
            "Descanso de " + formatDuration(duration.toLong()) + " para " + exercise + "."
        } else {
            "Ao salvar uma serie, o descanso do exercicio atual inicia automaticamente."
        }
    }

    private fun restSecondsFor(exerciseName: String): Int {
        val exercise = currentPlan().exercises.firstOrNull { it.name == exerciseName } ?: currentExercise()
        return restSecondsForPlanExercise(exercise)
    }

    private fun restSecondsForPlanExercise(exercise: ExercisePlan): Int {
        val rest = exercise.rest
        val value = Regex("(\\d+)").find(rest)?.value?.toIntOrNull() ?: return 90
        return if (rest.lowercase(Locale.US).contains("min")) value * 60 else value
    }

    private fun formatDuration(seconds: Long): String {
        val mins = seconds / 60L
        val secs = seconds % 60L
        return mins.toString().padStart(2, '0') + ":" + secs.toString().padStart(2, '0')
    }

    private fun mediaCacheDir(): File = File(cacheDir, "exercise_media")

    private fun mediaCacheFileCount(): Int {
        return mediaCacheDir().listFiles()?.count { it.isFile && it.length() > 0L } ?: 0
    }

    private fun mediaCacheSizeLabel(): String {
        val bytes = mediaCacheDir().listFiles()?.filter { it.isFile }?.sumOf { it.length() } ?: 0L
        return if (bytes < 1024L * 1024L) {
            (bytes / 1024L).coerceAtLeast(0L).toString() + " KB"
        } else {
            String.format(Locale("pt", "BR"), "%.1f MB", bytes.toDouble() / (1024.0 * 1024.0))
        }
    }

    private fun prefetchCurrentWorkoutMedia() {
        val exercises = currentPlan().exercises
        Toast.makeText(this, "Preparando midias do treino em segundo plano.", Toast.LENGTH_SHORT).show()
        Thread {
            var downloaded = 0
            var available = 0
            exercises.forEach { exercise ->
                val match = catalogMatchForWorkoutExercise(exercise.name) ?: return@forEach
                match.links.forEach { link ->
                    val file = File(mediaCacheDir(), Integer.toHexString(link.hashCode()) + ".img")
                    if (file.exists() && file.length() > 0L) {
                        available += 1
                    } else if (downloadMediaFrame(link, file)) {
                        downloaded += 1
                        available += 1
                    }
                }
            }
            runOnUiThread {
                Toast.makeText(
                    this,
                    available.toString() + " midias prontas" + if (downloaded > 0) " (" + downloaded + " novas)." else ".",
                    Toast.LENGTH_LONG,
                ).show()
                if (currentTab == "exercises") render()
            }
        }.start()
    }

    private fun downloadMediaFrame(link: String, file: File): Boolean {
        return try {
            mediaCacheDir().mkdirs()
            val connection = URL(link).openConnection()
            connection.connectTimeout = 7000
            connection.readTimeout = 9000
            val bytes = connection.getInputStream().use { input -> input.readBytes() }
            if (bytes.isEmpty()) return false
            file.writeBytes(bytes)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun clearMediaCache() {
        mediaCacheDir().listFiles()?.forEach { file ->
            if (file.isFile) file.delete()
        }
        Toast.makeText(this, "Cache de imagens limpo.", Toast.LENGTH_SHORT).show()
        render()
    }

    private fun statusChip(text: String): TextView {
        return Mo2Components.badge(this, text, true)
    }

    private fun compactMetric(title: String, value: String): View {
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.setPadding(dp(Mo2Spacing.Sm), dp(Mo2Spacing.Sm), dp(Mo2Spacing.Sm), dp(Mo2Spacing.Sm))
        box.background = rounded(surface, dp(Mo2Radius.Md), border)
        box.addView(label(title.uppercase(Locale("pt", "BR")), muted, 10f, true))
        box.addView(label(value, white, 14f, true))
        return box
    }

    private fun spacedRow(row: LinearLayout): View {
        val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        params.setMargins(0, dp(12), 0, 0)
        row.layoutParams = params
        for (index in 0 until row.childCount) {
            val child = row.getChildAt(index)
            val childParams = child.layoutParams as LinearLayout.LayoutParams
            childParams.setMargins(if (index == 0) 0 else dp(8), 0, 0, 0)
            child.layoutParams = childParams
        }
        return row
    }

    private fun metricGrid(metrics: List<Pair<String, String>>): View {
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        metrics.chunked(2).forEach { rowItems ->
            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            rowItems.forEach { item ->
                val card = card(surface2)
                card.orientation = LinearLayout.VERTICAL
                card.addView(label(item.first.uppercase(Locale("pt", "BR")), muted, 12f, true))
                card.addView(label(item.second, white, 20f, true))
                row.addView(card, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            }
            box.addView(row)
        }
        return box
    }

    private fun progressLine(title: String, value: Int, target: Int): TextView {
        val percent = if (target <= 0) 0 else ((value.toDouble() / target.toDouble()) * 100.0).roundToInt()
        return label(title + ": " + value + "/" + target + " (" + percent + "%)", white, 15f, true)
    }

    private fun heroCard(kicker: String, title: String, subtitle: String): View {
        val box = card(surface3)
        box.orientation = LinearLayout.VERTICAL
        box.addView(label(kicker.uppercase(Locale("pt", "BR")), green, 13f, true))
        box.addView(label(title, white, 30f, true))
        box.addView(label(subtitle, muted, 15f, false))
        return box
    }

    private fun input(hint: String, defaultValue: String): EditText {
        val input = EditText(this)
        input.hint = hint
        input.setText(defaultValue)
        input.setSingleLine(true)
        input.setTextColor(white)
        input.setHintTextColor(muted)
        input.textSize = accessibleTextSize(16f)
        input.setPadding(dp(14), 0, dp(14), 0)
        input.background = rounded(surface2, dp(8), border)
        val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(54))
        params.setMargins(0, dp(8), 0, 0)
        input.layoutParams = params
        return input
    }

    private fun textArea(hint: String, defaultValue: String): EditText {
        val input = EditText(this)
        input.hint = hint
        input.setText(defaultValue)
        input.setSingleLine(false)
        input.minLines = 4
        input.maxLines = 8
        input.gravity = Gravity.TOP
        input.setTextColor(white)
        input.setHintTextColor(muted)
        input.textSize = accessibleTextSize(14f)
        input.setPadding(dp(14), dp(12), dp(14), dp(12))
        input.background = rounded(surface2, dp(8), border)
        val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(150))
        params.setMargins(0, dp(8), 0, 0)
        input.layoutParams = params
        return input
    }

    private fun EditText.textValue() = text?.toString().orEmpty()

    private fun sectionTitle(text: String): TextView {
        return Mo2Components.sectionHeader(this, text)
    }

    private fun label(text: String, color: Int, size: Float, bold: Boolean): TextView {
        return Mo2Components.label(this, text, color, accessibleTextSize(size), bold)
    }

    private fun accessibleTextSize(size: Float): Float {
        if (!prefs.getBoolean("accessibility_large_text", false)) return size
        return (size * 1.12f).coerceAtMost(size + 4f)
    }

    private fun pill(text: String, active: Boolean, width: Int, height: Int): TextView {
        val view = label(text, if (active) bg else white, 14f, true)
        view.gravity = Gravity.CENTER
        view.setPadding(dp(12), 0, dp(12), 0)
        view.background = rounded(if (active) green else surface, dp(8), if (active) green else border)
        val params = LinearLayout.LayoutParams(dp(width), dp(height))
        params.setMargins(0, 0, dp(10), dp(10))
        view.layoutParams = params
        return view
    }

    private fun actionButton(text: String, color: Int, textColor: Int): Button {
        return Mo2Components.actionButton(this, text, color, textColor)
    }

    private fun buttonParams(button: Button): View {
        val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(54))
        params.setMargins(0, dp(10), 0, 0)
        button.layoutParams = params
        return button
    }

    private fun card(color: Int = surface2): LinearLayout {
        return Mo2Components.card(this, color)
    }

    private fun rounded(color: Int, radius: Int, stroke: Int? = null): GradientDrawable {
        return Mo2Drawables.roundedPx(this, color, radius, stroke)
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(window.decorView.windowToken, 0)
    }

    private fun dayKey() = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    private fun weekKey() = SimpleDateFormat("yyyy-ww", Locale.US).format(Date())
    private fun timeKey() = SimpleDateFormat("HH:mm", Locale("pt", "BR")).format(Date())
    private fun timestamp() = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(Date())
    private fun dp(value: Int) = (value * resources.displayMetrics.density).roundToInt()
}
