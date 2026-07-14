package br.com.mo2log.mobile

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.text.InputType
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.view.animation.AnimationSet
import android.view.animation.AlphaAnimation
import android.view.animation.TranslateAnimation
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import br.com.mo2log.mobile.ui.Mo2Colors
import br.com.mo2log.mobile.ui.Mo2Components
import br.com.mo2log.mobile.ui.Mo2Drawables
import br.com.mo2log.mobile.ui.Mo2Radius
import br.com.mo2log.mobile.ui.Mo2Spacing
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.text.Normalizer
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.net.URL
import kotlin.math.max
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

data class HistoryActivity(
    val type: String,
    val item: JSONObject,
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
    private val versionName = "10.1.0"
    private val trainingPlanVersion = "10.1.0"
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
    private var runningNextText: TextView? = null
    private var runningTreadmillText: TextView? = null
    private var runningCountdownPanel: View? = null
    private var runningActiveContent: View? = null
    private var runningPauseButton: Button? = null
    private var pendingRunSummary: JSONObject? = null
    private val restTimerRunnable = object : Runnable {
        override fun run() {
            restTimerText?.let { view ->
                view.text = restTimerDisplay()
                val remaining = restTimerRemainingSeconds()
                if (remaining > 0L) {
                    restTimerHandler.postDelayed(this, 1000L)
                } else {
                    notifyRestTimerFinishedIfNeeded()
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
        currentTab = prefs.getString("current_tab", "home") ?: "home"
        syncTrainingPlanVersion()
        selectedPlanIndex = prefs.getInt("selected_plan", todayPlanIndex())
        selectedExerciseIndex = prefs.getInt("selected_exercise", 0)
        selectedRunId = prefs.getString("selected_run_id", todayRunningWorkout()?.id ?: currentRunningWeekWorkouts().first().id) ?: currentRunningWeekWorkouts().first().id
        initVoiceCoach()
        render()
    }

    override fun onDestroy() {
        restTimerHandler.removeCallbacks(restTimerRunnable)
        runningSessionHandler.removeCallbacks(runningSessionRunnable)
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        voiceCoach?.stop()
        voiceCoach?.shutdown()
        super.onDestroy()
    }

    private fun syncTrainingPlanVersion() {
        if (prefs.getString("training_plan_version", "") == trainingPlanVersion) return
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

    private fun render() {
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
        updateSessionWakeLock()

        val page = LinearLayout(this)
        page.orientation = LinearLayout.VERTICAL
        page.setBackgroundColor(bg)

        val scroll = ScrollView(this)
        scroll.setBackgroundColor(bg)

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(dp(18), dp(18), dp(18), dp(18))
        scroll.addView(root)

        val focusedRunningSession = currentTab == "running" && activeRunningWorkout() != null
        if (!focusedRunningSession) {
            root.addView(header())
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

        pendingRunSummary?.let { summary ->
            pendingRunSummary = null
            page.post { showRunningWorkoutSummary(summary) }
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
        titleBox.addView(label("Android offline | v" + versionName, muted, 14f, false))
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
        wrap.setPadding(0, dp(18), 0, dp(6))

        val title = label(currentSectionTitle(), white, 26f, true)
        wrap.addView(title)
        wrap.addView(label(currentSectionSubtitle(), muted, 14f, false))
        return wrap
    }

    private fun bottomNav(): View {
        val wrap = LinearLayout(this)
        wrap.orientation = LinearLayout.VERTICAL
        wrap.setPadding(dp(12), dp(10), dp(12), dp(12))
        wrap.setBackgroundColor(surface)

        val divider = View(this)
        divider.setBackgroundColor(border)
        val dividerParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1))
        dividerParams.setMargins(0, 0, 0, dp(8))
        wrap.addView(divider, dividerParams)

        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        primaryNavItems.forEach { item ->
            val active = isBottomNavActive(item.id)
            val button = LinearLayout(this)
            button.orientation = LinearLayout.VERTICAL
            button.gravity = Gravity.CENTER
            button.setPadding(dp(6), 0, dp(6), 0)
            button.setBackgroundColor(Color.TRANSPARENT)
            button.contentDescription = item.title
            button.isClickable = true
            button.isFocusable = true
            button.setOnClickListener { switchTab(item.id) }

            val marker = View(this)
            marker.background = rounded(
                if (active) green else surface,
                dp(Mo2Radius.Pill),
                if (active) green else muted,
            )
            val markerParams = LinearLayout.LayoutParams(dp(16), dp(16))
            markerParams.setMargins(0, 0, 0, dp(7))
            button.addView(marker, markerParams)

            val title = label(item.title, if (active) green else muted, 12f, active)
            title.gravity = Gravity.CENTER
            button.addView(title, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))

            val params = LinearLayout.LayoutParams(0, dp(62), 1f)
            params.setMargins(dp(2), 0, dp(2), 0)
            row.addView(button, params)
        }
        wrap.addView(row)
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
                dp(18),
                dp(18) + if (enforcedEdgeToEdge) 0 else topInset,
                dp(18),
                dp(18) + if (navigation == null) bottomInset else 0,
            )
            navigation?.setPadding(dp(12), dp(10), dp(12), dp(12) + bottomInset)
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
        root.addView(todayDashboardPanel())
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

    private fun weeklyDashboardPanel(): View {
        val stats = computeStats(allLogs())
        val weekTarget = prefs.getString("goal_week_sets", "60")?.toIntOrNull() ?: 60
        val weekSets = stats.optInt("week_sets")
        val strengthPercent = progressPercent(weekSets, weekTarget)
        val weekRuns = currentRunningWeekWorkouts()
        val runningDone = weekRuns.count { isRunWorkoutCompleted(it) }
        val runningPercent = progressPercent(runningDone, weekRuns.size)
        val distance = currentWeekRunDistance()

        val box = card(surface3)
        box.orientation = LinearLayout.VERTICAL
        box.addView(label("DASHBOARD SEMANAL", green, 13f, true))
        box.addView(label("Semana " + currentRunningPlanWeek() + " de 6", white, 30f, true))
        box.addView(label("Musculacao, corrida e prontidao em uma visao rapida para treinar hoje.", muted, 15f, false))

        val metrics = LinearLayout(this)
        metrics.orientation = LinearLayout.HORIZONTAL
        metrics.addView(compactMetric("Series", weekSets.toString() + "/" + weekTarget), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        metrics.addView(compactMetric("Corridas", runningDone.toString() + "/" + weekRuns.size), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        metrics.addView(compactMetric("Distancia", formatKm(distance)), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        box.addView(spacedRow(metrics))

        box.addView(dashboardProgressLine("Musculacao", weekSets.toString() + " series de " + weekTarget, strengthPercent, green))
        box.addView(dashboardProgressLine("Corrida 5 km", runningDone.toString() + " de " + weekRuns.size + " treinos concluidos", runningPercent, Mo2Colors.Running))
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

    private fun todayDashboardPanel(): View {
        val stats = computeStats(allLogs())
        val mission = todayMission()
        val plan = currentPlan()
        val run = todayRunningWorkout()
        val strengthText = if (isStrengthDayToday()) {
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

    private fun isStrengthDayToday(): Boolean {
        val day = SimpleDateFormat("u", Locale.US).format(Date()).toIntOrNull() ?: 1
        return day == 2 || day == 4 || day == 6
    }

    private fun isStrengthWeekDayCompleted(dayIndex: Int): Boolean {
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
        box.addView(label("COCKPIT V10", green, 13f, true))
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
        val strengthDone = todayLogs().length() > 0
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
        return when {
            status == "red" -> "Prontidao: vermelha. Reduza carga, corte extras e priorize recuperar."
            status == "yellow" -> "Prontidao: amarela. Mantenha o plano, mas sem buscar recorde hoje."
            status == "green" -> "Prontidao: verde. Pode seguir o plano com progressao controlada."
            restTimerRemainingSeconds() > 0L -> "Prontidao: em intervalo. Respire e volte quando o descanso terminar."
            avgRpe != null && avgRpe >= 9.0 -> "Prontidao: cautela. RPE recente alto, mantenha tecnica e evite extras."
            todayLogs().length() > 0 && todayRunCount() > 0 -> "Prontidao: dia completo registrado. Agora a prioridade e recuperar."
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
        val box = card()
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
        root.addView(heroCard("Treino de musculacao", plan.title + " - " + plan.focus, "Registre series, acompanhe descanso e troque exercicios quando a academia pedir."))
        root.addView(sectionTitle("Planos"))
        root.addView(planSelector())
        root.addView(workoutProgressPanel())
        root.addView(gymModePanel())
        root.addView(registerPanel())
        root.addView(selectedExerciseMediaPanel())
        root.addView(smartStrengthCoachPanel())
        root.addView(sectionTitle("Exercicios do treino"))
        root.addView(exerciseList())
    }

    private fun workoutProgressPanel(): View {
        val plan = currentPlan()
        val exercise = currentExercise()
        val logs = allLogs()
        var planSetsToday = 0
        var exerciseSetsToday = 0
        for (i in 0 until logs.length()) {
            val item = logs.getJSONObject(i)
            if (item.optString("day") == dayKey() && item.optString("plan") == plan.title) {
                planSetsToday += 1
                if (item.optString("exercise") == exercise.name) exerciseSetsToday += 1
            }
        }

        val sets = plannedSetsForCurrentExercise()
        val doneSets = countDonePlannedSets(sets)
        val exercisePercent = progressPercent(doneSets, sets.length())
        val planPercent = progressPercent(selectedExerciseIndex + 1, plan.exercises.size)

        val box = card(surface3)
        box.orientation = LinearLayout.VERTICAL
        box.addView(label("SESSAO DE HOJE", green, 13f, true))
        box.addView(label(exercise.name, white, 26f, true))
        box.addView(label(exercise.target + " | descanso " + exercise.rest + " | " + exercise.notes, muted, 14f, false))

        val metrics = LinearLayout(this)
        metrics.orientation = LinearLayout.HORIZONTAL
        metrics.addView(Mo2Components.metricCard(this, "Exercicio", (selectedExerciseIndex + 1).toString() + "/" + plan.exercises.size, plan.title, green), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        metrics.addView(Mo2Components.metricCard(this, "Series", doneSets.toString() + "/" + sets.length(), planSetsToday.toString() + " hoje", Mo2Colors.Running), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        metrics.addView(Mo2Components.metricCard(this, "Carga", lastLoadFor(exercise.name) + " kg", exerciseSetsToday.toString() + " neste exercicio", amber), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        box.addView(spacedRow(metrics))

        box.addView(dashboardProgressLine("Progresso do exercicio", doneSets.toString() + " de " + sets.length() + " series concluidas", exercisePercent, green))
        box.addView(dashboardProgressLine("Avanco do treino", (selectedExerciseIndex + 1).toString() + " de " + plan.exercises.size + " exercicios", planPercent, Mo2Colors.Running))

        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        val previous = actionButton("Anterior", surface2, white)
        previous.setOnClickListener {
            if (selectedExerciseIndex > 0) {
                selectedExerciseIndex -= 1
                prefs.edit().putInt("selected_exercise", selectedExerciseIndex).apply()
                render()
            }
        }
        row.addView(previous, LinearLayout.LayoutParams(0, dp(50), 1f))
        val next = actionButton("Proximo", green, bg)
        next.setOnClickListener {
            if (selectedExerciseIndex < plan.exercises.lastIndex) {
                selectedExerciseIndex += 1
                prefs.edit().putInt("selected_exercise", selectedExerciseIndex).apply()
                render()
            }
        }
        row.addView(next, LinearLayout.LayoutParams(0, dp(50), 1f))
        box.addView(spacedRow(row))
        return box
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

        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        val applyLoad = actionButton("Aplicar carga", green, bg)
        applyLoad.setOnClickListener { applySmartLoadToPendingSets() }
        row.addView(applyLoad, LinearLayout.LayoutParams(0, dp(50), 1f))

        val applyVolume = actionButton("Ajustar series", surface2, green)
        applyVolume.setOnClickListener { applySmartVolumeToCurrentExercise() }
        row.addView(applyVolume, LinearLayout.LayoutParams(0, dp(50), 1f))
        box.addView(spacedRow(row))
        return box
    }

    private fun restTimerPanel(): View {
        val box = card()
        box.orientation = LinearLayout.VERTICAL
        val active = restTimerRemainingSeconds() > 0L
        box.addView(label("DESCANSO", green, 13f, true))
        val timer = label(restTimerDisplay(), if (active) amber else white, 32f, true)
        restTimerText = timer
        box.addView(timer)
        box.addView(label(restTimerSubtitle(), muted, 14f, false))
        if (active) restTimerHandler.post(restTimerRunnable)
        else notifyRestTimerFinishedIfNeeded()

        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        val start = actionButton("Iniciar", green, bg)
        start.setOnClickListener { startRestTimerForCurrentExercise() }
        row.addView(start, LinearLayout.LayoutParams(0, dp(50), 1f))
        val add = actionButton("+30s", surface2, green)
        add.setOnClickListener { addRestTime(30) }
        row.addView(add, LinearLayout.LayoutParams(0, dp(50), 1f))
        val stop = actionButton("Parar", surface2, danger)
        stop.setOnClickListener { clearRestTimer() }
        row.addView(stop, LinearLayout.LayoutParams(0, dp(50), 1f))
        box.addView(spacedRow(row))
        return box
    }

    private fun selectedExerciseMediaPanel(): View {
        val planned = currentExercise()
        val matched = catalogMatchForWorkoutExercise(planned.name)
        val box = card()
        box.orientation = LinearLayout.VERTICAL
        box.addView(label("DETALHE DO EXERCICIO", green, 13f, true))
        box.addView(label(planned.name, white, 24f, true))
        box.addView(label(planned.target + " | descanso " + planned.rest, white, 15f, true))
        box.addView(label(planned.notes, muted, 14f, false))

        if (matched == null) {
            box.addView(label("Ainda nao encontrei uma midia confiavel no catalogo para este item do treino.", muted, 15f, false))
            box.addView(label("Use a aba Exercicios para buscar uma alternativa quando necessario.", muted, 14f, false))
            val swap = actionButton("Buscar troca recomendada", surface2, green)
            swap.setOnClickListener { swapCurrentExerciseForRecommended() }
            box.addView(buttonParams(swap))
            return box
        }

        val unavailable = isEquipmentUnavailable(matched.equipment)
        box.addView(label("Catalogo: " + matched.name, white, 17f, true))
        box.addView(label(exerciseMeta(matched), muted, 14f, false))
        box.addView(label(mediaHealthLabel(matched), muted, 13f, false))
        if (unavailable) {
            box.addView(label("Equipamento marcado como indisponivel: " + matched.equipment, danger, 14f, true))
        }

        if (matched.links.isNotEmpty()) {
            val media = RemoteExerciseMediaView(this, matched.links)
            val mediaParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(220))
            mediaParams.setMargins(0, dp(12), 0, dp(12))
            box.addView(media, mediaParams)
        } else {
            box.addView(label("Este exercicio existe no catalogo, mas ainda nao tem frames remotos.", amber, 14f, true))
        }

        box.addView(label("Descricao", green, 14f, true))
        box.addView(label(matched.description.ifBlank { "Exercicio do catalogo para " + matched.muscle + " com foco em " + matched.primary.ifBlank { matched.subgroup } + "." }, white, 15f, false))
        if (matched.technicalCare.isNotBlank()) {
            box.addView(label("Cuidados tecnicos", green, 14f, true))
            box.addView(label(matched.technicalCare, muted, 14f, false))
        }
        box.addView(label("Grupo e equipamento", green, 14f, true))
        box.addView(label(matched.muscle + " | " + matched.equipment.ifBlank { "equipamento variavel" } + " | nivel " + matched.level.ifBlank { "-" }, white, 14f, false))

        val alternatives = recommendedSwapOptions(matched, "same_muscle").take(4)
        box.addView(label("Alternativas do mesmo musculo", green, 14f, true))
        if (alternatives.isEmpty()) {
            box.addView(label("Nenhuma alternativa livre encontrada agora. Abra o catalogo para escolher manualmente.", muted, 14f, false))
        } else {
            alternatives.forEach { alternative ->
                val item = card(surface2)
                item.orientation = LinearLayout.VERTICAL
                item.addView(label(alternative.name, white, 15f, true))
                item.addView(label(exerciseMeta(alternative), muted, 12f, false))
                item.setOnClickListener { applyRecommendedExerciseSwap(alternative) }
                box.addView(item)
            }
        }

        val actions = LinearLayout(this)
        actions.orientation = LinearLayout.HORIZONTAL
        val swap = actionButton("Trocar", surface2, green)
        swap.setOnClickListener { swapCurrentExerciseForRecommended() }
        actions.addView(swap, LinearLayout.LayoutParams(0, dp(50), 1f))
        val unavailableButton = actionButton(if (unavailable) "Rever equip." else "Equip. indisponivel", surface2, if (unavailable) danger else amber)
        unavailableButton.setOnClickListener { showEquipmentUnavailableDialog(matched) }
        actions.addView(unavailableButton, LinearLayout.LayoutParams(0, dp(50), 1f))
        box.addView(spacedRow(actions))

        val open = actionButton("Abrir no catalogo", surface2, green)
        open.setOnClickListener {
            prefs.edit()
                .putString("catalog_muscle", matched.muscle)
                .putString("catalog_selected", matched.id)
                .apply()
            switchTab("exercises")
        }
        box.addView(buttonParams(open))
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
        root.addView(treadmillModePanel())
        root.addView(runningSmartCoachPanel())
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
        val distanceDone = workouts.filter { isRunWorkoutCompleted(it) }.sumOf { totalRunDistance(it) }

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
        val titleBox = LinearLayout(this)
        titleBox.orientation = LinearLayout.VERTICAL
        titleBox.addView(label(workout.dayName + " | " + workout.title, if (completed) green else white, 17f, true))
        titleBox.addView(label(formatKm(totalRunDistance(workout)) + " | " + formatDuration(estimatedWorkoutSeconds(workout)), muted, 13f, false))
        top.addView(titleBox, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        top.addView(Mo2Components.badge(this, if (completed) "Feito" else if (expanded) "Aberto" else "Pendente", completed))
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
                box.addView(label(marker + workout.dayName + ": " + workout.title + " | " + formatKm(totalRunDistance(workout)) + " | " + formatDuration(estimatedWorkoutSeconds(workout)), white, 13f, false))
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
        val speed = currentActiveRunSpeed(workout)
        val remainingSeconds = if (speed <= 0.0) 0L else ((remainingKm / speed) * 3600.0).roundToInt().toLong()
        val paused = isActiveRunPaused()

        runningCountdownPanel?.visibility = View.GONE
        runningActiveContent?.visibility = View.VISIBLE
        runningSessionTitleText?.text = workout.title
        runningSessionSubtitleText?.text = "Etapa " + (stageIndex + 1) + " de " + workout.stages.size + " | Esteira"
        runningStageText?.text = (if (paused) "PAUSADO | " else "ETAPA ATUAL | ") + stage.title
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
        box.addView(label("Use se quiser voltar ao plano A/B/C e corrida original da v10.1.0.", muted, 14f, false))
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
        val runs = filteredRunHistory()
        root.addView(historyTypeSelectorPanel())
        root.addView(historyAdvancedFiltersButton())
        if (prefs.getBoolean("history_filters_open", false)) root.addView(historyFilterPanel())
        root.addView(historyMetricsPanel(strengthLogs, runs))
        root.addView(historyActivityPanel(strengthLogs, runs))
        if (strengthLogs.isNotEmpty()) root.addView(exerciseEvolutionPanel(strengthLogs))

        val export = actionButton("Copiar exportacao completa", green, bg)
        export.setOnClickListener { exportToClipboard() }
        root.addView(buttonParams(export))
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
            prefs.edit()
                .putString("history_from", from.textValue().trim())
                .putString("history_to", to.textValue().trim())
                .putString("history_query", query.textValue().trim())
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
                .putString("history_type", "all")
                .apply()
            render()
        }
        row.addView(clear, LinearLayout.LayoutParams(0, dp(50), 1f))
        box.addView(spacedRow(row))
        return box
    }

    private fun historyMetricsPanel(strengthLogs: List<JSONObject>, runs: List<JSONObject>): View {
        val volume = strengthLogs.sumOf { it.optDouble("load") * it.optInt("reps") }.roundToInt()
        val runDistance = runs.sumOf { it.optDouble("distance") }
        val bestLoad = strengthLogs.maxOfOrNull { it.optDouble("load") } ?: 0.0
        val activities = strengthLogs.size + runs.size
        val box = card(surface3)
        box.orientation = LinearLayout.VERTICAL
        box.addView(label("RESUMO DO FILTRO", green, 13f, true))
        box.addView(label(activities.toString() + " atividades", white, 26f, true))
        box.addView(label(filteredHistoryDays(strengthLogs, runs).toString() + " dias com registro", muted, 14f, false))

        val metrics = LinearLayout(this)
        metrics.orientation = LinearLayout.HORIZONTAL
        metrics.addView(compactMetric("Series", strengthLogs.size.toString()), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        metrics.addView(compactMetric("Volume", volume.toString() + " kg"), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        metrics.addView(compactMetric("Corrida", formatKm(runDistance)), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        box.addView(spacedRow(metrics))
        if (bestLoad > 0.0) box.addView(label("Maior carga no filtro: " + formatLoad(bestLoad), muted, 13f, false))
        return box
    }

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

    private fun historyActivityPanel(strengthLogs: List<JSONObject>, runs: List<JSONObject>): View {
        val activities = (
            strengthLogs.map { HistoryActivity("strength", it) } +
                runs.map { HistoryActivity("run", it) }
            ).sortedByDescending { activity -> activity.item.optString("day") + activity.item.optString("time") }

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
        val item = activity.item
        val isRun = activity.type == "run"
        val box = card(surface2)
        box.orientation = LinearLayout.VERTICAL

        val top = LinearLayout(this)
        top.orientation = LinearLayout.HORIZONTAL
        top.gravity = Gravity.CENTER_VERTICAL
        val titleBox = LinearLayout(this)
        titleBox.orientation = LinearLayout.VERTICAL
        titleBox.addView(label(
            if (isRun) item.optString("workout_title", "Corrida") else item.optString("exercise", "Exercicio"),
            white,
            18f,
            true,
        ))
        titleBox.addView(label(item.optString("day") + " | " + item.optString("time"), muted, 12f, false))
        top.addView(titleBox, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        val type = label(if (isRun) "CORRIDA" else "MUSCULACAO", if (isRun) Mo2Colors.Running else green, 11f, true)
        type.gravity = Gravity.CENTER
        type.setPadding(dp(10), dp(5), dp(10), dp(5))
        type.background = rounded(surface, dp(Mo2Radius.Pill), border)
        top.addView(type)
        box.addView(top)

        if (isRun) {
            val paceSeconds = item.optLong("pace_seconds_per_km", 0L)
            val pace = if (paceSeconds > 0L) formatPaceSeconds(paceSeconds) else formatPaceForSpeed(item.optDouble("speed"))
            box.addView(label(
                formatKm(item.optDouble("distance")) + " | " + item.optString("duration", "-") + " | " + pace,
                Mo2Colors.Running,
                15f,
                true,
            ))
        } else {
            val rpe = item.optString("rpe").ifBlank { "-" }
            box.addView(label(
                item.optInt("reps").toString() + " reps | " + formatLoad(item.optDouble("load")) + " | RPE " + rpe,
                green,
                15f,
                true,
            ))
            box.addView(label(item.optString("plan"), muted, 13f, false))
        }

        val notes = if (isRun) item.optString("feedback_notes").ifBlank { item.optString("notes") } else item.optString("notes")
        if (notes.isNotBlank()) box.addView(label(notes, muted, 13f, false))

        val actions = LinearLayout(this)
        actions.orientation = LinearLayout.HORIZONTAL
        val edit = actionButton("Editar", surface, white)
        edit.setOnClickListener {
            if (isRun) showEditRunHistoryDialog(item) else showEditStrengthHistoryDialog(item)
        }
        actions.addView(edit, LinearLayout.LayoutParams(0, dp(46), 1f))
        val delete = actionButton("Excluir", surface, danger)
        delete.setOnClickListener { showDeleteHistoryActivityDialog(activity) }
        actions.addView(delete, LinearLayout.LayoutParams(0, dp(46), 1f))
        box.addView(spacedRow(actions))
        return box
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

    private fun historyEditorRow(first: EditText, second: EditText): View {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.addView(first, LinearLayout.LayoutParams(0, dp(54), 1f))
        row.addView(second, LinearLayout.LayoutParams(0, dp(54), 1f))
        return spacedRow(row)
    }

    private fun styleHistoryDialog(dialog: AlertDialog, content: View) {
        dialog.window?.setBackgroundDrawable(Mo2Drawables.rounded(this, surface, Mo2Radius.Lg, border))
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
        if (rpeRaw.isNotBlank() && (rpe == null || rpe !in 1.0..10.0)) {
            Toast.makeText(this, "RPE precisa estar entre 1 e 10.", Toast.LENGTH_SHORT).show()
            return false
        }

        val durationSeconds = if (durationInput > 0L) {
            durationInput
        } else {
            ((distance / speedInput!!) * 3600.0).roundToInt().toLong().coerceAtLeast(1L)
        }
        val speed = if (speedInput != null && speedInput > 0.0) speedInput else distance / (durationSeconds.toDouble() / 3600.0)
        val paceSeconds = (durationSeconds.toDouble() / distance).roundToInt().toLong()
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
            dialog.window?.setBackgroundDrawable(Mo2Drawables.rounded(this, surface, Mo2Radius.Lg, border))
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(danger)
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(muted)
        }
        dialog.show()
    }

    private fun deleteHistoryActivity(activity: HistoryActivity) {
        val isRun = activity.type == "run"
        val logs = if (isRun) runLogs() else allLogs()
        val index = findHistoryRecordIndex(logs, activity.item, activity.type)
        if (index < 0) {
            Toast.makeText(this, "Atividade nao encontrada.", Toast.LENGTH_SHORT).show()
            return
        }
        logs.remove(index)
        prefs.edit().putString(if (isRun) "run_logs" else "set_logs", logs.toString()).apply()
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
        root.addView(sectionTitle("Estatisticas"))
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
        summary.addView(label(mediaCacheFileCount().toString() + " frames salvos em cache local para abrir mais rapido.", muted, 14f, false))
        val clearCache = actionButton("Limpar cache de imagens", surface2, white)
        clearCache.setOnClickListener { clearMediaCache() }
        summary.addView(buttonParams(clearCache))
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
            prefs.edit().putString("catalog_query", search.textValue()).remove("catalog_selected").apply()
            hideKeyboard()
            render()
        }
        searchRow.addView(applySearch, LinearLayout.LayoutParams(0, dp(50), 1f))
        val clearSearch = actionButton("Limpar", surface2, white)
        clearSearch.setOnClickListener {
            prefs.edit().remove("catalog_query").remove("catalog_selected").apply()
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
                prefs.edit().putString("catalog_muscle", muscle).remove("catalog_selected").apply()
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
        filtered.take(80).forEach { exercise ->
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
        if (filtered.size > 80) {
            list.addView(label("Mostrando 80 de " + filtered.size + ". Use a busca para refinar.", amber, 14f, true))
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
        box.addView(label("COACH LOCAL", green, 13f, true))
        box.addView(label("Inteligencia, relatorios e adaptacao", white, 24f, true))
        localInsights().forEach { insight -> box.addView(label("- " + insight, white, 15f, false)) }
        root.addView(box)

        val report = card()
        report.orientation = LinearLayout.VERTICAL
        val stats = computeStats(allLogs())
        report.addView(label("RELATORIO RAPIDO", green, 13f, true))
        report.addView(label("Series: " + stats.optInt("total_sets") + " | Volume: " + stats.optInt("total_volume") + " kg", white, 16f, true))
        report.addView(label("Consistencia semanal: " + stats.optInt("week_sets") + " series.", muted, 15f, false))
        report.addView(label("Se a academia estiver cheia, troque por exercicio do mesmo padrao: empurrar, puxar, pernas ou core.", muted, 15f, false))
        root.addView(report)
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

        val planning = card(surface)
        planning.orientation = LinearLayout.VERTICAL
        planning.addView(label("PLANEJAMENTO", green, 13f, true))
        planning.addView(label("Preferencias de treino", white, 22f, true))
        planning.addView(label(
            "Plano " + trainingPlanVersion + " | corrida semana " + currentRunningPlanWeek() + " de 6",
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

    private fun showClearLocalDataDialog() {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Apagar historico local?")
            .setMessage("Todas as series e corridas salvas neste celular serao removidas. Plano, metas e favoritos serao mantidos.")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Apagar") { _, _ ->
                prefs.edit().remove("set_logs").remove("run_logs").apply()
                Toast.makeText(this, "Historico local apagado.", Toast.LENGTH_SHORT).show()
                render()
            }
            .create()
        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawable(Mo2Drawables.rounded(this, surface, Mo2Radius.Lg, border))
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
            val item = card(if (active) surface3 else surface)
            item.orientation = LinearLayout.VERTICAL
            item.setOnClickListener {
                selectedExerciseIndex = index
                prefs.edit().putInt("selected_exercise", index).apply()
                render()
            }
            item.addView(label((if (active) "Atual: " else "") + exercise.name, if (active) green else white, 18f, true))
            item.addView(label(exercise.target + " | descanso " + exercise.rest, if (active) white else muted, 14f, false))
            item.addView(label(exercise.notes, muted, 13f, false))
            box.addView(item)
        }
        return box
    }

    private fun registerPanel(): View {
        val exercise = currentExercise()
        val lastSet = lastSetFor(exercise.name)
        val sets = plannedSetsForCurrentExercise()
        val doneCount = countDonePlannedSets(sets)
        val box = card()
        box.orientation = LinearLayout.VERTICAL
        box.addView(label("SERIES DO EXERCICIO", green, 13f, true))
        box.addView(label(exercise.name, white, 24f, true))
        box.addView(label(doneCount.toString() + "/" + sets.length() + " series concluidas. Marque a checkbox ao terminar; deslize para a esquerda para apagar.", muted, 14f, false))
        if (lastSet != null) {
            box.addView(label("Ultima concluida: " + lastSet.optInt("reps") + " reps | " + lastSet.optDouble("load") + " kg", muted, 13f, false))
        }
        box.addView(dashboardProgressLine("Series planejadas", doneCount.toString() + " de " + sets.length() + " concluidas", progressPercent(doneCount, sets.length()), green))
        box.addView(restTimerPanel())

        for (index in 0 until sets.length()) {
            box.addView(plannedSetRow(index, sets.getJSONObject(index)))
        }

        val add = actionButton("+", surface2, green)
        add.textSize = 22f
        add.setOnClickListener { addPlannedSetForCurrentExercise() }
        box.addView(buttonParams(add))

        val swap = actionButton("Trocar por recomendado", surface2, green)
        swap.setOnClickListener { swapCurrentExerciseForRecommended() }
        box.addView(buttonParams(swap))

        val finish = actionButton("Concluir treino", green, bg)
        finish.setOnClickListener {
            prefs.edit().putString("last_finished_day", dayKey()).apply()
            showWorkoutSummaryPopup()
        }
        box.addView(buttonParams(finish))
        return box
    }

    private fun plannedSetRow(index: Int, item: JSONObject): View {
        val done = item.optBoolean("done", false)
        val row = card(if (done) surface3 else surface)
        row.orientation = LinearLayout.VERTICAL

        val content = LinearLayout(this)
        content.orientation = LinearLayout.HORIZONTAL
        content.gravity = Gravity.CENTER_VERTICAL

        val check = actionButton(if (done) "OK" else "Feita", if (done) green else surface2, if (done) bg else white)
        content.addView(check, LinearLayout.LayoutParams(dp(58), dp(54)))

        val load = input("kg", item.optString("load", lastLoadFor(currentExercise().name)))
        content.addView(load, LinearLayout.LayoutParams(0, dp(54), 1f))

        val reps = input("reps", item.optString("reps", defaultRepsFor(currentExercise().target)))
        content.addView(reps, LinearLayout.LayoutParams(0, dp(54), 1f))
        row.addView(content)

        row.addView(label("Serie " + (index + 1) + (if (done) " concluida" else " pendente") + " | carga e reps editaveis antes da conclusao", if (done) green else muted, 13f, false))
        check.setOnClickListener {
            if (done) {
                Toast.makeText(this, "Serie ja concluida.", Toast.LENGTH_SHORT).show()
            } else {
                completePlannedSet(index, load.textValue(), reps.textValue())
            }
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
                    if (delta < -dp(24)) view.background = rounded(danger, dp(Mo2Radius.Md), danger)
                    false
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    val delta = event.rawX - startX
                    if (delta < -dp(96)) {
                        deletePlannedSet(index)
                        true
                    } else {
                        view.background = rounded(if (done) surface3 else surface, dp(Mo2Radius.Lg), border)
                        false
                    }
                }
                else -> false
            }
        }
        return row
    }

    private fun plannedSetKey(): String {
        return "planned_sets_" + dayKey() + "_" + currentPlan().id + "_" + selectedExerciseIndex
    }

    private fun plannedSetsForCurrentExercise(): JSONArray {
        val key = plannedSetKey()
        val stored = safeArray(key)
        if (stored.length() > 0) return stored

        val defaults = JSONArray()
        val count = defaultSetCountFor(currentExercise().target)
        val reps = defaultRepsFor(currentExercise().target)
        val load = lastLoadFor(currentExercise().name)
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
            render()
        }
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
        render()
    }

    private fun deletePlannedSet(index: Int) {
        val sets = plannedSetsForCurrentExercise()
        if (sets.length() <= 1) {
            Toast.makeText(this, "Mantenha pelo menos uma serie.", Toast.LENGTH_SHORT).show()
            render()
            return
        }
        sets.remove(index)
        savePlannedSets(sets)
        Toast.makeText(this, "Serie removida.", Toast.LENGTH_SHORT).show()
        render()
    }

    private fun countDonePlannedSets(sets: JSONArray): Int {
        var count = 0
        for (i in 0 until sets.length()) {
            if (sets.getJSONObject(i).optBoolean("done", false)) count += 1
        }
        return count
    }

    private fun moveAfterExerciseCompleted() {
        if (selectedExerciseIndex < currentPlan().exercises.lastIndex) {
            selectedExerciseIndex += 1
            prefs.edit().putInt("selected_exercise", selectedExerciseIndex).apply()
            Toast.makeText(this, "Exercicio concluido. Proximo exercicio aberto.", Toast.LENGTH_SHORT).show()
            render()
        } else {
            prefs.edit().putString("last_finished_day", dayKey()).apply()
            showWorkoutSummaryPopup()
        }
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
        return if (selectedExerciseIndex < plan.exercises.lastIndex) {
            plan.exercises[selectedExerciseIndex + 1].name
        } else {
            "concluir treino"
        }
    }

    private fun strengthAdjustmentFor(exercise: ExercisePlan): StrengthAdjustment {
        val recent = recentSetsForExercise(exercise.name, 6)
        val targetSets = defaultSetCountFor(exercise.target)
        val plannedSets = plannedSetsForCurrentExercise()
        val completedSets = countDonePlannedSets(plannedSets)
        if (recent.isEmpty()) {
            val baseLoad = lastLoadFor(exercise.name).replace(',', '.').toDoubleOrNull() ?: 0.0
            return StrengthAdjustment(
                nextLoad = baseLoad,
                suggestedSetCount = targetSets.coerceAtLeast(completedSets),
                loadReason = if (baseLoad <= 0.0) "Sem historico para este exercicio. Use a primeira serie para registrar uma carga real." else "Use a ultima carga conhecida como ponto de partida.",
                volumeReason = "Comece pelo volume planejado e deixe o app ajustar depois dos registros.",
            )
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

        return StrengthAdjustment(
            nextLoad = nextLoad,
            suggestedSetCount = suggestedSets,
            loadReason = loadReason,
            volumeReason = volumeReason,
        )
    }

    private fun applySmartLoadToPendingSets() {
        val adjustment = strengthAdjustmentFor(currentExercise())
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
        content.background = rounded(surface, dp(Mo2Radius.Lg), border)
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
        content.background = rounded(surface, dp(Mo2Radius.Lg), border)
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
        val reasonRow = LinearLayout(this)
        reasonRow.orientation = LinearLayout.HORIZONTAL
        reasons.forEach { item ->
            val active = item.first == reason
            val button = actionButton(item.second, if (active) green else surface2, if (active) bg else white)
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
            reasonRow.addView(button, LinearLayout.LayoutParams(0, dp(46), 1f))
        }
        content.addView(spacedRow(reasonRow))

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

        dialog = AlertDialog.Builder(this)
            .setView(content)
            .create()
        dialog.setOnShowListener {
            content.startAnimation(smoothPopupAnimation())
        }
        dialog.show()
    }

    private fun smoothPopupAnimation(): AnimationSet {
        val set = AnimationSet(true)
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

    private fun showWorkoutSummaryPopup() {
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
        content.addView(label("Seu treino foi salvo localmente e ja esta disponivel no historico.", muted, 14f, false))
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
            dialog.window?.setBackgroundDrawable(Mo2Drawables.rounded(this, surface, Mo2Radius.Lg, border))
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
            .putLong("running_active_last_tick_at", countdownEndAt)
            .putLong("running_countdown_end_at", countdownEndAt)
            .putLong("running_active_started_at_ms", countdownEndAt)
            .putLong("running_active_paused_total_ms", 0L)
            .putBoolean("running_active_paused", false)
            .putString("running_session_started_at", timestamp())
            .remove("running_active_pause_started_at")
            .remove("running_30_announced_key")
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
        if (isActiveRunPaused()) return false

        var stageIndex = prefs.getInt("running_active_stage", 0).coerceIn(workout.stages.indices)
        var distanceDone = prefs.getString("running_active_distance_done", "0.0")?.toDoubleOrNull() ?: 0.0
        val speed = currentActiveRunSpeed(workout)
        val lastTick = prefs.getLong("running_active_last_tick_at", if (countdownEndAt > 0L) countdownEndAt else now)
        if (countdownEndAt > 0L && countdownEndAt <= now) prefs.edit().remove("running_countdown_end_at").apply()

        val elapsedMs = max(0L, now - lastTick)
        if (elapsedMs > 0L && speed > 0.0) {
            distanceDone += speed * (elapsedMs.toDouble() / 3600000.0)
        }

        while (stageIndex < workout.stages.size && distanceDone >= workout.stages[stageIndex].distanceKm) {
            distanceDone -= workout.stages[stageIndex].distanceKm
            stageIndex += 1
            prefs.edit().remove("running_30_announced_key").apply()
            if (stageIndex < workout.stages.size) {
                prefs.edit().putString("running_active_speed", workout.stages[stageIndex].speedKmh.toString()).apply()
                speakRunCue("Etapa concluida. A proxima etapa sera " + speakableStageCue(workout.stages[stageIndex]) + ".")
            }
        }

        if (stageIndex >= workout.stages.size) {
            val log = saveRunCompletion(
                workout,
                manual = false,
                rerender = false,
                distanceOverride = totalRunDistance(workout),
                elapsedSecondsOverride = activeRunElapsedSeconds(),
                completedStagesOverride = workout.stages.size,
            )
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
        prefs.edit().putString("running_active_speed", speed.toString()).apply()
        refreshRunningSessionViews()
    }

    private fun finishCurrentRunStage() {
        val workout = activeRunningWorkout() ?: return
        if (updateActiveRunProgress()) {
            render()
            return
        }
        val currentStage = prefs.getInt("running_active_stage", 0)
        if (currentStage >= workout.stages.lastIndex) {
            val log = saveRunCompletion(
                workout,
                manual = false,
                rerender = false,
                distanceOverride = totalRunDistance(workout),
                elapsedSecondsOverride = activeRunElapsedSeconds(),
                completedStagesOverride = workout.stages.size,
            )
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
            .apply()
        speakRunCue("Proxima etapa: " + speakableStageCue(workout.stages[nextStage]) + ".")
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
            speakRunCue("Treino retomado.")
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
            speakRunCue("Treino pausado.")
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
        val log = saveRunCompletion(
            workout,
            manual = false,
            rerender = false,
            distanceOverride = activeRunCompletedDistance(workout),
            elapsedSecondsOverride = activeRunElapsedSeconds(),
            completedStagesOverride = stageIndex,
        )
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
        val paceSeconds = log.optLong(
            "pace_seconds_per_km",
            if (distance <= 0.0) 0L else (seconds.toDouble() / distance).roundToInt().toLong(),
        )
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
            Triple("Pace", formatPaceSeconds(paceSeconds), "medio"),
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
            dialog.window?.setBackgroundDrawable(Mo2Drawables.rounded(this, surface, Mo2Radius.Lg, border))
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
        prefs.edit()
            .remove("running_active_id")
            .remove("running_active_stage")
            .remove("running_active_distance_done")
            .remove("running_active_speed")
            .remove("running_active_last_tick_at")
            .remove("running_countdown_end_at")
            .remove("running_active_started_at_ms")
            .remove("running_active_paused")
            .remove("running_active_pause_started_at")
            .remove("running_active_paused_total_ms")
            .remove("running_session_started_at")
            .remove("running_30_announced_key")
            .apply()
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
        if (remainingSeconds !in 1L..30L) return
        val key = workout.id + ":" + stageIndex
        if (prefs.getString("running_30_announced_key", "") == key) return
        val message = if (stageIndex < workout.stages.lastIndex) {
            "Em 30 segundos, voce termina a etapa atual. A proxima etapa sera " + speakableStageCue(workout.stages[stageIndex + 1]) + "."
        } else {
            "Em 30 segundos, voce termina a etapa atual e conclui o treino."
        }
        prefs.edit().putString("running_30_announced_key", key).apply()
        speakRunCue(message)
    }

    private fun initVoiceCoach() {
        voiceCoach = TextToSpeech(this) { status ->
            voiceCoachReady = status == TextToSpeech.SUCCESS
            if (voiceCoachReady) {
                voiceCoach?.language = Locale("pt", "BR")
                voiceCoach?.setSpeechRate(0.95f)
            }
        }
    }

    private fun speakRunCue(text: String) {
        if (!voiceCoachReady) return
        voiceCoach?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "mo2log_run_" + System.currentTimeMillis())
    }

    private fun saveRun(distance: Double, speed: Double, notes: String) {
        val durationMinutes = ((distance / speed) * 60.0).roundToInt()
        val log = JSONObject()
            .put("id", UUID.randomUUID().toString())
            .put("day", dayKey())
            .put("week", weekKey())
            .put("time", timeKey())
            .put("distance", distance)
            .put("speed", speed)
            .put("duration", durationMinutes.toString() + " min")
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
        prefs.all.forEach { entry ->
            when (val value = entry.value) {
                is String -> preferences.put(entry.key, value)
                is Boolean -> preferences.put(entry.key, value)
                is Int -> preferences.put(entry.key, value)
                is Long -> preferences.put(entry.key, value)
                is Float -> preferences.put(entry.key, value.toDouble())
                else -> preferences.put(entry.key, value?.toString() ?: "")
            }
        }
        return JSONObject()
            .put("source", "mo2log_native_android")
            .put("schema", "personal_backup_v1")
            .put("version", versionName)
            .put("exported_at", timestamp())
            .put("preferences", preferences)
            .put("strength_logs", allLogs())
            .put("run_logs", runLogs())
            .put("summary", JSONObject()
                .put("strength_log_count", allLogs().length())
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
            val editor = prefs.edit()
            val preferences = payload.optJSONObject("preferences")
            if (preferences != null) {
                val keys = preferences.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    when (val value = preferences.get(key)) {
                        is Boolean -> editor.putBoolean(key, value)
                        is Int -> editor.putInt(key, value)
                        is Long -> editor.putLong(key, value)
                        is Double -> editor.putString(key, value.toString())
                        else -> editor.putString(key, value.toString())
                    }
                }
            } else {
                if (payload.has("strength_logs")) editor.putString("set_logs", payload.getJSONArray("strength_logs").toString())
                if (payload.has("run_logs")) editor.putString("run_logs", payload.getJSONArray("run_logs").toString())
                payload.optJSONObject("goals")?.let { goals ->
                    editor.putString("goal_week_sets", goals.optString("week_sets", "60"))
                    editor.putString("goal_week_volume", goals.optString("week_volume", "12000"))
                    editor.putString("body_weight", goals.optString("body_weight", ""))
                }
            }
            editor
                .remove("rest_timer_end_at")
                .remove("running_active_id")
                .remove("running_countdown_end_at")
                .apply()
            Toast.makeText(this, "Backup importado.", Toast.LENGTH_SHORT).show()
            render()
        } catch (_: Exception) {
            Toast.makeText(this, "JSON de backup invalido.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun readClipboardText(): String {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        return clipboard.primaryClip?.getItemAt(0)?.coerceToText(this)?.toString().orEmpty()
    }

    private fun allLogs(): JSONArray = safeArray("set_logs")
    private fun runLogs(): JSONArray = safeArray("run_logs")

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
            if (item.optString("week") == week || item.optString("day").startsWith(week.take(4))) {
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

    private fun smartRunningAdjustment(): RunningAdjustment {
        val currentOffset = prefs.getString("running_speed_offset", "0.0")?.toDoubleOrNull() ?: 0.0
        val currentScale = prefs.getString("running_distance_scale", "1.0")?.toDoubleOrNull() ?: 1.0
        val recent = recentRunLogItems(4)
        val weekWorkouts = currentRunningWeekWorkouts()
        val completedThisWeek = weekWorkouts.count { isRunWorkoutCompleted(it) }
        val plannedAvgSpeed = plannedAverageSpeed(weekWorkouts)

        if (recent.isEmpty()) {
            return RunningAdjustment(
                speedOffset = roundSpeed(currentOffset),
                distanceScale = roundScale(currentScale),
                headline = "Base atual mantida",
                reason = "Sem corridas salvas ainda. Complete pelo menos um treino para calibrar ritmo e distancia.",
            )
        }

        val avgRecentSpeed = recent.map { it.optDouble("speed") }.filter { it > 0.0 }.average()
        var suggestedOffset = currentOffset
        var suggestedScale = currentScale
        val headline: String
        val reason: String

        when {
            completedThisWeek >= 4 && avgRecentSpeed >= plannedAvgSpeed - 0.2 -> {
                suggestedOffset = (currentOffset + 0.1).coerceIn(-2.0, 2.0)
                suggestedScale = (currentScale + 0.02).coerceIn(0.60, 1.40)
                headline = "Subir levemente"
                reason = "Voce esta concluindo bem a semana. Proximo passo seguro: +0,1 km/h e um pequeno aumento de distancia."
            }
            completedThisWeek >= 2 && avgRecentSpeed >= plannedAvgSpeed -> {
                suggestedOffset = (currentOffset + 0.1).coerceIn(-2.0, 2.0)
                headline = "Acelerar um pouco"
                reason = "Ritmo recente esta igual ou acima do planejado. Aumente so 0,1 km/h para manter controle."
            }
            completedThisWeek <= 1 && currentRunningPlanWeek() > 1 -> {
                suggestedOffset = (currentOffset - 0.1).coerceIn(-2.0, 2.0)
                suggestedScale = (currentScale - 0.02).coerceIn(0.60, 1.40)
                headline = "Consolidar antes de subir"
                reason = "Poucos treinos concluidos nesta semana. Reduza levemente para voltar a consistencia."
            }
            else -> {
                headline = "Manter ritmo"
                reason = "Semana em progresso normal. Mantenha o ajuste atual e registre a proxima corrida."
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
        val day = SimpleDateFormat("u", Locale.US).format(Date()).toIntOrNull() ?: 1
        val workouts = currentRunningWeekWorkouts()
        return workouts.firstOrNull { it.dayIndex == day }
            ?: workouts.firstOrNull { it.dayIndex > day }
            ?: workouts.firstOrNull()
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
        return String.format(Locale.US, "%.2f quilometros a %.1f quilometros por hora", stage.distanceKm, stage.speedKmh)
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
            ExercisePlan("Supino reto ou maquina peitoral", "4 x 8-10", "90s", "Controle e amplitude. Depois faca 15-25 min de corrida leve."),
            ExercisePlan("Supino inclinado com halteres", "3 x 10", "90s", "Suba carga quando fechar reps com tecnica limpa."),
            ExercisePlan("Desenvolvimento de ombros", "3 x 8-10", "90s", "Evite compensar com lombar."),
            ExercisePlan("Elevacao lateral", "3 x 12-15", "60s", "Cadencia limpa, sem embalo."),
            ExercisePlan("Triceps corda", "3 x 10-12", "60s", "Trave cotovelos perto do corpo."),
            ExercisePlan("Prancha", "3 x 45s", "45s", "Respiracao constante."),
        )),
        WorkoutPlan("b", "Treino B", "Quinta - Pernas/Core + corrida curta", listOf(
            ExercisePlan("Leg press", "4 x 10", "120s", "Amplitude segura e constante. Depois faca so 10-15 min leve."),
            ExercisePlan("Agachamento livre ou guiado", "3 x 8", "120s", "Priorize tecnica antes de carga."),
            ExercisePlan("Cadeira extensora", "3 x 12", "75s", "Segure um segundo no topo."),
            ExercisePlan("Mesa flexora", "3 x 10-12", "75s", "Controle total na volta."),
            ExercisePlan("Stiff", "3 x 10", "90s", "Quadril para tras, coluna neutra."),
            ExercisePlan("Panturrilha", "4 x 12-15", "45s", "Pausa no alongamento."),
            ExercisePlan("Abdominal ou prancha", "3 series", "45s", "Escolha a variacao do dia."),
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
        if (currentTab == "home") return "Dashboard Semanal"
        return navItems.firstOrNull { it.id == currentTab }?.title ?: "Inicio"
    }

    private fun currentSectionSubtitle(): String {
        return when (currentTab) {
            "home" -> "Visao da semana, missao de hoje e atalhos para registrar sem perder tempo."
            "workout" -> "Registro guiado com timer, midia de execucao, edicao e desfazer serie."
            "running" -> "Semana de corrida, planejamento 5 km e treino guiado por fases."
            "more" -> "Tudo que nao precisa ficar no menu principal, organizado por ferramenta."
            "plan_editor" -> "Edite seu plano pessoal de musculacao e corrida direto no celular."
            "exercises" -> "Catalogo completo com busca, midia por link, favoritos e alternativas."
            "history" -> "Filtros, metricas e evolucao por exercicio dos registros locais."
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
        prefs.edit()
            .putLong("rest_timer_end_at", System.currentTimeMillis() + seconds * 1000L)
            .putInt("rest_timer_duration_secs", seconds)
            .putString("rest_timer_exercise", exercise)
            .putBoolean("rest_timer_notified", false)
            .apply()
        Toast.makeText(this, "Descanso iniciado: " + formatDuration(seconds.toLong()), Toast.LENGTH_SHORT).show()
        render()
    }

    private fun addRestTime(seconds: Int) {
        val now = System.currentTimeMillis()
        val currentEnd = prefs.getLong("rest_timer_end_at", 0L)
        val base = max(now, currentEnd)
        prefs.edit()
            .putLong("rest_timer_end_at", base + seconds * 1000L)
            .putString("rest_timer_exercise", prefs.getString("rest_timer_exercise", currentExercise().name) ?: currentExercise().name)
            .apply()
        Toast.makeText(this, "+30s no descanso.", Toast.LENGTH_SHORT).show()
        render()
    }

    private fun clearRestTimer() {
        prefs.edit()
            .remove("rest_timer_end_at")
            .remove("rest_timer_duration_secs")
            .remove("rest_timer_exercise")
            .remove("rest_timer_notified")
            .apply()
        Toast.makeText(this, "Descanso parado.", Toast.LENGTH_SHORT).show()
        render()
    }

    private fun notifyRestTimerFinishedIfNeeded() {
        val endAt = prefs.getLong("rest_timer_end_at", 0L)
        if (endAt <= 0L || System.currentTimeMillis() < endAt) return
        if (prefs.getBoolean("rest_timer_notified", false)) return
        prefs.edit().putBoolean("rest_timer_notified", true).apply()
        try {
            ToneGenerator(AudioManager.STREAM_NOTIFICATION, 90).startTone(ToneGenerator.TONE_PROP_ACK, 450)
        } catch (_: Exception) {
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
        val rest = currentPlan().exercises.firstOrNull { it.name == exerciseName }?.rest ?: currentExercise().rest
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
        input.textSize = 16f
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
        input.textSize = 14f
        input.setPadding(dp(14), dp(12), dp(14), dp(12))
        input.background = rounded(surface2, dp(8), border)
        val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(150))
        params.setMargins(0, dp(8), 0, 0)
        input.layoutParams = params
        return input
    }

    private fun EditText.textValue() = text?.toString().orEmpty()

    private fun sectionTitle(text: String): TextView {
        val view = label(text.uppercase(Locale("pt", "BR")), green, 14f, true)
        view.setPadding(0, dp(20), 0, dp(8))
        return view
    }

    private fun label(text: String, color: Int, size: Float, bold: Boolean): TextView {
        return Mo2Components.label(this, text, color, size, bold)
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

    private fun card(color: Int = surface): LinearLayout {
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
