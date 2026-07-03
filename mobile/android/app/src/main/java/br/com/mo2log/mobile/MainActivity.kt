package br.com.mo2log.mobile

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
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

data class NavItem(
    val id: String,
    val title: String,
)

data class CatalogExercise(
    val name: String,
    val muscle: String,
    val equipment: String,
    val movement: String,
    val description: String,
)

class ExerciseAnimationView(context: Context, private val movement: String) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val phase = ((System.currentTimeMillis() % 1200L).toFloat() / 1200f)
        val swing = kotlin.math.sin((phase * Math.PI * 2).toFloat())

        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(8, 18, 15)
        canvas.drawRoundRect(RectF(0f, 0f, w, h), 28f, 28f, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 7f
        paint.strokeCap = Paint.Cap.ROUND
        paint.color = Color.rgb(105, 255, 95)

        val cx = w * 0.48f
        val headY = h * 0.25f
        val hipY = h * 0.58f
        val shoulderY = h * 0.38f
        val shift = swing * w * 0.08f

        paint.style = Paint.Style.FILL
        canvas.drawCircle(cx, headY, 18f, paint)
        paint.style = Paint.Style.STROKE

        when (movement) {
            "push" -> {
                canvas.drawLine(cx - 18f, shoulderY, cx + 22f, hipY, paint)
                canvas.drawLine(cx - 18f, shoulderY, cx - 95f - shift, shoulderY + 10f, paint)
                canvas.drawLine(cx + 10f, shoulderY + 8f, cx + 95f + shift, shoulderY + 8f, paint)
                canvas.drawLine(cx + 20f, hipY, cx - 35f, h * 0.82f, paint)
                canvas.drawLine(cx + 20f, hipY, cx + 55f, h * 0.82f, paint)
                canvas.drawLine(cx - 120f - shift, shoulderY + 10f, cx + 120f + shift, shoulderY + 8f, paint)
            }
            "pull" -> {
                canvas.drawLine(cx, shoulderY, cx, hipY, paint)
                canvas.drawLine(cx - 85f + shift, shoulderY - 10f, cx - 18f, shoulderY + 30f, paint)
                canvas.drawLine(cx + 85f - shift, shoulderY - 10f, cx + 18f, shoulderY + 30f, paint)
                canvas.drawLine(cx - 120f, shoulderY - 18f, cx + 120f, shoulderY - 18f, paint)
                canvas.drawLine(cx, hipY, cx - 50f, h * 0.84f, paint)
                canvas.drawLine(cx, hipY, cx + 50f, h * 0.84f, paint)
            }
            "squat" -> {
                val kneeY = h * (0.72f + swing * 0.05f)
                canvas.drawLine(cx, shoulderY, cx + shift, hipY, paint)
                canvas.drawLine(cx + shift, hipY, cx - 65f, kneeY, paint)
                canvas.drawLine(cx + shift, hipY, cx + 65f, kneeY, paint)
                canvas.drawLine(cx - 65f, kneeY, cx - 80f, h * 0.9f, paint)
                canvas.drawLine(cx + 65f, kneeY, cx + 80f, h * 0.9f, paint)
                canvas.drawLine(cx - 95f, shoulderY - 8f, cx + 95f, shoulderY - 8f, paint)
            }
            "hinge" -> {
                canvas.drawLine(cx - 10f, shoulderY, cx + 50f + shift, hipY, paint)
                canvas.drawLine(cx + 50f + shift, hipY, cx - 30f, h * 0.86f, paint)
                canvas.drawLine(cx + 50f + shift, hipY, cx + 90f, h * 0.86f, paint)
                canvas.drawLine(cx + 20f + shift, shoulderY + 20f, cx - 95f, h * 0.66f, paint)
                canvas.drawLine(cx + 20f + shift, shoulderY + 20f, cx + 120f, h * 0.66f, paint)
            }
            "core" -> {
                canvas.drawLine(cx - 120f, hipY, cx + 120f, hipY + shift * 0.2f, paint)
                canvas.drawLine(cx - 30f, hipY, cx - 70f, h * 0.8f, paint)
                canvas.drawLine(cx + 50f, hipY, cx + 92f, h * 0.8f, paint)
                canvas.drawLine(cx - 150f, h * 0.86f, cx + 150f, h * 0.86f, paint)
            }
            "cardio" -> {
                canvas.drawLine(cx, shoulderY, cx + shift, hipY, paint)
                canvas.drawLine(cx + shift, hipY, cx - 60f - shift, h * 0.86f, paint)
                canvas.drawLine(cx + shift, hipY, cx + 75f + shift, h * 0.8f, paint)
                canvas.drawLine(cx, shoulderY + 10f, cx - 72f - shift, h * 0.52f, paint)
                canvas.drawLine(cx, shoulderY + 10f, cx + 70f + shift, h * 0.48f, paint)
            }
            else -> {
                canvas.drawLine(cx, shoulderY, cx, hipY, paint)
                canvas.drawLine(cx, shoulderY + 8f, cx - 80f - shift, h * 0.5f, paint)
                canvas.drawLine(cx, shoulderY + 8f, cx + 80f + shift, h * 0.5f, paint)
                canvas.drawLine(cx, hipY, cx - 50f, h * 0.84f, paint)
                canvas.drawLine(cx, hipY, cx + 50f, h * 0.84f, paint)
            }
        }

        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(151, 171, 164)
        paint.textSize = 26f
        paint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText("execucao animada", 26f, h - 26f, paint)
        postInvalidateDelayed(80L)
    }
}

class MainActivity : Activity() {
    private val versionName = "8.4.0"
    private val bg = Color.rgb(5, 8, 7)
    private val surface = Color.rgb(13, 24, 20)
    private val surface2 = Color.rgb(19, 36, 30)
    private val border = Color.rgb(35, 58, 49)
    private val green = Color.rgb(105, 255, 95)
    private val muted = Color.rgb(151, 171, 164)
    private val white = Color.WHITE
    private val danger = Color.rgb(255, 116, 116)
    private val amber = Color.rgb(255, 198, 88)

    private val prefs by lazy { getSharedPreferences("mo2log_native", Context.MODE_PRIVATE) }
    private val plans by lazy { buildWorkoutPlans() }
    private val navItems = listOf(
        NavItem("home", "Inicio"),
        NavItem("workout", "Treino"),
        NavItem("running", "Corrida"),
        NavItem("history", "Historico"),
        NavItem("stats", "Stats"),
        NavItem("exercises", "Exercicios"),
        NavItem("goals", "Metas"),
        NavItem("coach", "Coach"),
        NavItem("profile", "Perfil"),
    )

    private var currentTab = "home"
    private var selectedPlanIndex = 0
    private var selectedExerciseIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentTab = prefs.getString("current_tab", "home") ?: "home"
        selectedPlanIndex = prefs.getInt("selected_plan", todayPlanIndex())
        selectedExerciseIndex = prefs.getInt("selected_exercise", 0)
        render()
    }

    private fun render() {
        val scroll = ScrollView(this)
        scroll.setBackgroundColor(bg)

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(dp(18), dp(18), dp(18), dp(28))
        scroll.addView(root)

        root.addView(header())
        root.addView(navBar())

        when (currentTab) {
            "home" -> renderHome(root)
            "workout" -> renderWorkout(root)
            "running" -> renderRunning(root)
            "history" -> renderHistory(root)
            "stats" -> renderStats(root)
            "exercises" -> renderExercises(root)
            "goals" -> renderGoals(root)
            "coach" -> renderCoach(root)
            "profile" -> renderProfile(root)
            else -> renderHome(root)
        }

        setContentView(scroll)
    }

    private fun header(): View {
        val box = card()
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
        logo.background = rounded(green, dp(16))
        top.addView(logo, LinearLayout.LayoutParams(dp(52), dp(52)))

        val titleBox = LinearLayout(this)
        titleBox.orientation = LinearLayout.VERTICAL
        titleBox.setPadding(dp(14), 0, 0, 0)
        titleBox.addView(label("MO2 LOG", green, 14f, true))
        titleBox.addView(label("Treino pessoal Android", white, 25f, true))
        titleBox.addView(label("App local nativo | v" + versionName, muted, 14f, false))
        top.addView(titleBox, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        box.addView(top)

        return box
    }

    private fun navBar(): View {
        val scroll = HorizontalScrollView(this)
        scroll.isHorizontalScrollBarEnabled = false
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        navItems.forEach { item ->
            val active = currentTab == item.id
            val button = pill(item.title, active, 124, 50)
            button.setOnClickListener {
                currentTab = item.id
                prefs.edit().putString("current_tab", currentTab).apply()
                render()
            }
            row.addView(button)
        }
        scroll.addView(row)
        return scroll
    }

    private fun renderHome(root: LinearLayout) {
        val logs = todayLogs()
        val stats = computeStats(allLogs())
        root.addView(heroCard("Resumo de hoje", logs.length().toString() + " series", "Volume hoje: " + stats.optInt("today_volume") + " kg | RPE medio: " + stats.optString("avg_rpe")))

        val next = currentPlan()
        val nextBox = card()
        nextBox.orientation = LinearLayout.VERTICAL
        nextBox.addView(label("PROXIMO TREINO", green, 13f, true))
        nextBox.addView(label(next.title + " - " + next.focus, white, 24f, true))
        nextBox.addView(label(next.exercises.size.toString() + " exercicios carregados. Toque em Treino para registrar.", muted, 15f, false))
        val start = actionButton("Abrir treino", green, bg)
        start.setOnClickListener { switchTab("workout") }
        nextBox.addView(buttonParams(start))
        root.addView(nextBox)

        root.addView(metricGrid(listOf(
            Pair("Semana", stats.optInt("week_sets").toString() + " series"),
            Pair("Volume total", stats.optInt("total_volume").toString() + " kg"),
            Pair("Melhor carga", stats.optString("best_load")),
            Pair("Corridas", runLogs().length().toString()),
        )))

        val insightBox = card()
        insightBox.orientation = LinearLayout.VERTICAL
        insightBox.addView(label("INSIGHTS", green, 13f, true))
        localInsights().forEach { insight -> insightBox.addView(label("• " + insight, white, 15f, false)) }
        root.addView(insightBox)
    }

    private fun renderWorkout(root: LinearLayout) {
        root.addView(sectionTitle("Programa de treinos"))
        root.addView(planSelector())
        root.addView(sectionTitle("Exercicios do treino"))
        root.addView(exerciseList())
        root.addView(registerPanel())
    }

    private fun renderRunning(root: LinearLayout) {
        val box = card()
        box.orientation = LinearLayout.VERTICAL
        box.addView(label("RUNNING COACH", green, 13f, true))
        box.addView(label("Plano de esteira 5 km", white, 26f, true))
        box.addView(label("Calcula tempo por velocidade e salva sessoes de corrida localmente.", muted, 15f, false))

        val speed = input("Velocidade km/h", prefs.getString("run_speed", "8.0") ?: "8.0")
        val distance = input("Distancia km", prefs.getString("run_distance", "5.0") ?: "5.0")
        val notes = input("Observacao", "")
        box.addView(speed)
        box.addView(distance)
        box.addView(notes)

        val calc = actionButton("Calcular e salvar corrida", green, bg)
        calc.setOnClickListener {
            val speedValue = speed.textValue().replace(',', '.').toDoubleOrNull() ?: 0.0
            val distanceValue = distance.textValue().replace(',', '.').toDoubleOrNull() ?: 0.0
            if (speedValue <= 0.0 || distanceValue <= 0.0) {
                Toast.makeText(this, "Informe velocidade e distancia.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            saveRun(distanceValue, speedValue, notes.textValue())
        }
        box.addView(buttonParams(calc))
        root.addView(box)

        val plan = card()
        plan.orientation = LinearLayout.VERTICAL
        plan.addView(label("ESTRUTURA SUGERIDA", green, 13f, true))
        plan.addView(label("1. Aquecimento: 5 min leve", white, 15f, false))
        plan.addView(label("2. Bloco principal: mantenha ritmo constante", white, 15f, false))
        plan.addView(label("3. Final: 3 min desacelerando", white, 15f, false))
        plan.addView(label("Tempo estimado: " + estimatedRunTime(distance.textValue(), speed.textValue()), amber, 16f, true))
        root.addView(plan)

        val history = card()
        history.orientation = LinearLayout.VERTICAL
        history.addView(label("CORRIDAS RECENTES", green, 13f, true))
        val runs = runLogs()
        if (runs.length() == 0) history.addView(label("Nenhuma corrida salva ainda.", muted, 15f, false))
        for (i in runs.length() - 1 downTo max(0, runs.length() - 5)) {
            val item = runs.getJSONObject(i)
            history.addView(label(item.optString("day") + " | " + item.optDouble("distance") + " km | " + item.optDouble("speed") + " km/h | " + item.optString("duration"), white, 14f, false))
        }
        root.addView(history)
    }

    private fun renderHistory(root: LinearLayout) {
        val box = card()
        box.orientation = LinearLayout.VERTICAL
        box.addView(label("HISTORICO DE TREINOS", green, 13f, true))
        val logs = allLogs()
        if (logs.length() == 0) {
            box.addView(label("Nenhuma serie salva ainda.", muted, 15f, false))
        } else {
            for (i in logs.length() - 1 downTo max(0, logs.length() - 30)) {
                val item = logs.getJSONObject(i)
                box.addView(label(item.optString("day") + " " + item.optString("time") + " | " + item.optString("plan"), muted, 12f, false))
                box.addView(label(item.optString("exercise") + " | " + item.optInt("reps") + " reps | " + item.optDouble("load") + " kg | RPE " + item.optString("rpe"), white, 15f, false))
            }
        }
        root.addView(box)

        val export = actionButton("Copiar exportacao completa", green, bg)
        export.setOnClickListener { exportToClipboard() }
        root.addView(buttonParams(export))
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

        val prs = card()
        prs.orientation = LinearLayout.VERTICAL
        prs.addView(label("TOP EXERCICIOS POR CARGA", green, 13f, true))
        bestLoadsByExercise().forEach { line -> prs.addView(label(line, white, 15f, false)) }
        root.addView(prs)
    }

    private fun renderExercises(root: LinearLayout) {
        root.addView(sectionTitle("Catalogo de exercicios"))

        val selectedMuscle = prefs.getString("catalog_muscle", "Todos") ?: "Todos"
        val muscles = listOf("Todos") + exerciseCatalog().map { it.muscle }.distinct().sorted()
        val muscleScroll = HorizontalScrollView(this)
        muscleScroll.isHorizontalScrollBarEnabled = false
        val muscleRow = LinearLayout(this)
        muscleRow.orientation = LinearLayout.HORIZONTAL
        muscles.forEach { muscle ->
            val active = selectedMuscle == muscle
            val button = pill(muscle, active, 150, 50)
            button.setOnClickListener {
                prefs.edit().putString("catalog_muscle", muscle).remove("catalog_selected").apply()
                render()
            }
            muscleRow.addView(button)
        }
        muscleScroll.addView(muscleRow)
        root.addView(muscleScroll)

        val filtered = exerciseCatalog().filter { selectedMuscle == "Todos" || it.muscle == selectedMuscle }
        val selectedName = prefs.getString("catalog_selected", null)
        val selected = filtered.firstOrNull { it.name == selectedName } ?: filtered.first()

        val detail = card()
        detail.orientation = LinearLayout.VERTICAL
        detail.addView(label("GIF DE EXECUCAO", green, 13f, true))
        detail.addView(label(selected.name, white, 25f, true))
        detail.addView(label(selected.muscle + " | " + selected.equipment, muted, 14f, false))
        val animation = ExerciseAnimationView(this, selected.movement)
        val animationParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(190))
        animationParams.setMargins(0, dp(12), 0, dp(12))
        detail.addView(animation, animationParams)
        detail.addView(label("Execucao", green, 14f, true))
        detail.addView(label(selected.description, white, 15f, false))

        detail.addView(label("Alternativos para o mesmo musculo", green, 14f, true))
        alternativesFor(selected).forEach { alternative ->
            val alt = pill(alternative.name, false, 260, 48)
            alt.setOnClickListener {
                prefs.edit().putString("catalog_selected", alternative.name).apply()
                render()
            }
            detail.addView(alt)
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
        filtered.forEach { exercise ->
            val item = card(if (exercise.name == selected.name) surface2 else surface)
            item.orientation = LinearLayout.VERTICAL
            item.setOnClickListener {
                prefs.edit().putString("catalog_selected", exercise.name).apply()
                render()
            }
            item.addView(label(exercise.name, white, 17f, true))
            item.addView(label(exercise.muscle + " | " + exercise.equipment, muted, 13f, false))
            list.addView(item)
        }
        root.addView(list)
    }

    private fun alternativesFor(exercise: CatalogExercise): List<CatalogExercise> {
        return exerciseCatalog()
            .filter { it.muscle == exercise.muscle && it.name != exercise.name }
            .take(8)
    }

    private fun exerciseCatalog(): List<CatalogExercise> = listOf(
        CatalogExercise("Supino reto com barra", "Peitoral", "Barra e banco", "push", "Deite no banco, retraia escapulas, desca a barra ao peito com controle e empurre sem perder a base dos pes."),
        CatalogExercise("Supino reto com halteres", "Peitoral", "Halteres", "push", "Desca os halteres ate linha do peito, mantenha punhos firmes e suba juntando levemente sem bater os pesos."),
        CatalogExercise("Supino inclinado com barra", "Peitoral", "Barra e banco inclinado", "push", "Use banco inclinado, desca a barra na parte alta do peito e suba mantendo cotovelos abaixo da linha dos ombros."),
        CatalogExercise("Supino inclinado com halteres", "Peitoral", "Halteres", "push", "Controle a descida, alongue o peitoral superior e suba com trajetoria levemente diagonal."),
        CatalogExercise("Crucifixo com halteres", "Peitoral", "Halteres", "push", "Abra os bracos com cotovelos semi-flexionados, sinta alongar o peitoral e feche sem transformar em supino."),
        CatalogExercise("Crucifixo no cabo", "Peitoral", "Crossover", "push", "Ajuste as polias, incline levemente o tronco e una as maos mantendo tensao constante no peitoral."),
        CatalogExercise("Peck deck", "Peitoral", "Maquina", "push", "Apoie as costas, una os bracos a frente e retorne devagar sem perder contato com o encosto."),
        CatalogExercise("Flexao de bracos", "Peitoral", "Peso corporal", "push", "Mantenha corpo alinhado, desca o peito em direcao ao solo e empurre mantendo abdomen firme."),

        CatalogExercise("Puxada frente", "Costas", "Pulley", "pull", "Segure a barra, puxe os cotovelos para baixo ate a parte alta do peito e retorne alongando as dorsais."),
        CatalogExercise("Puxada neutra", "Costas", "Pulley", "pull", "Use pegada neutra, mantenha peito aberto e puxe sem balancar o tronco."),
        CatalogExercise("Barra fixa", "Costas", "Barra", "pull", "Comece pendurado, puxe o corpo ate o queixo passar da barra e desca com controle."),
        CatalogExercise("Remada curvada", "Costas", "Barra", "pull", "Incline o tronco com coluna neutra, puxe a barra para o abdomen e controle a descida."),
        CatalogExercise("Remada baixa", "Costas", "Cabo", "pull", "Sente firme, puxe a pegada ao abdomen, una escapulas e volte sem arredondar lombar."),
        CatalogExercise("Remada unilateral", "Costas", "Halter", "pull", "Apoie uma mao no banco, puxe o halter com o cotovelo junto ao corpo e desca alongando."),
        CatalogExercise("Remada cavalinho", "Costas", "Maquina ou barra T", "pull", "Mantenha tronco estavel, puxe a carga ao peito baixo e evite roubar com quadril."),
        CatalogExercise("Pullover no cabo", "Costas", "Cabo", "pull", "Com bracos quase estendidos, leve a barra da altura do rosto ate a coxa sentindo as dorsais."),

        CatalogExercise("Desenvolvimento militar", "Ombros", "Barra", "push", "Empurre a barra acima da cabeca, contraia abdomen e evite arquear a lombar."),
        CatalogExercise("Desenvolvimento com halteres", "Ombros", "Halteres", "push", "Comece com halteres na altura dos ombros e suba ate quase estender os cotovelos."),
        CatalogExercise("Elevacao lateral", "Ombros", "Halteres", "other", "Eleve os bracos ate a linha dos ombros com cotovelos levemente flexionados e sem embalo."),
        CatalogExercise("Elevacao frontal", "Ombros", "Halteres ou anilha", "other", "Suba a carga a frente ate a altura dos ombros e controle a descida."),
        CatalogExercise("Crucifixo inverso", "Ombros", "Halteres ou maquina", "pull", "Incline o tronco ou use maquina e abra os bracos focando deltoide posterior."),
        CatalogExercise("Face pull", "Ombros", "Cabo", "pull", "Puxe a corda em direcao ao rosto, abrindo cotovelos e girando externamente os ombros."),
        CatalogExercise("Encolhimento", "Ombros", "Barra ou halteres", "other", "Eleve os ombros para cima sem girar, pause no topo e desca com controle."),

        CatalogExercise("Agachamento livre", "Quadriceps", "Barra", "squat", "Apoie a barra, desca mantendo joelhos alinhados aos pes e suba empurrando o chao."),
        CatalogExercise("Agachamento guiado", "Quadriceps", "Smith", "squat", "Posicione os pes, desca com tronco firme e suba sem travar agressivamente os joelhos."),
        CatalogExercise("Leg press 45", "Quadriceps", "Maquina", "squat", "Desca a plataforma com controle, mantenha lombar apoiada e empurre sem tirar o quadril do banco."),
        CatalogExercise("Cadeira extensora", "Quadriceps", "Maquina", "squat", "Estenda os joelhos, segure um segundo no topo e retorne devagar."),
        CatalogExercise("Passada", "Quadriceps", "Halteres", "squat", "Dê um passo a frente, desca ate formar angulos seguros e suba empurrando a perna da frente."),
        CatalogExercise("Afundo bulgaro", "Quadriceps", "Banco e halteres", "squat", "Apoie o pe de tras no banco, desca verticalmente e suba pela perna da frente."),
        CatalogExercise("Hack machine", "Quadriceps", "Maquina", "squat", "Apoie costas e ombros, desca mantendo controle e suba empurrando pela sola dos pes."),
        CatalogExercise("Sissy squat", "Quadriceps", "Peso corporal ou maquina", "squat", "Incline o corpo mantendo quadril estendido e controle forte no quadriceps."),

        CatalogExercise("Stiff", "Posterior", "Barra ou halteres", "hinge", "Leve o quadril para tras, mantenha coluna neutra e sinta alongar posteriores antes de subir."),
        CatalogExercise("Levantamento terra romeno", "Posterior", "Barra", "hinge", "Desca a barra rente ao corpo, preserve leve flexao nos joelhos e suba contraindo gluteos."),
        CatalogExercise("Mesa flexora", "Posterior", "Maquina", "hinge", "Flexione os joelhos levando os calcanhares ao gluteo e retorne controlando."),
        CatalogExercise("Cadeira flexora", "Posterior", "Maquina", "hinge", "Ajuste o apoio, flexione os joelhos e evite tirar o quadril do banco."),
        CatalogExercise("Good morning", "Posterior", "Barra", "hinge", "Com barra nas costas, dobre o quadril para tras e retorne mantendo coluna neutra."),
        CatalogExercise("Hip thrust", "Gluteos", "Banco e barra", "hinge", "Apoie as costas no banco, suba o quadril contraindo gluteos e pause no topo."),
        CatalogExercise("Gluteo no cabo", "Gluteos", "Cabo", "hinge", "Prenda a tornozeleira, estenda o quadril para tras sem girar o tronco."),
        CatalogExercise("Abdutora", "Gluteos", "Maquina", "squat", "Abra as pernas contra a resistencia, pause e retorne sem bater as placas."),

        CatalogExercise("Rosca direta", "Biceps", "Barra", "pull", "Mantenha cotovelos proximos ao corpo, suba a barra sem balancar e desca controlando."),
        CatalogExercise("Rosca alternada", "Biceps", "Halteres", "pull", "Flexione um braco por vez, supinando o punho e mantendo tronco parado."),
        CatalogExercise("Rosca martelo", "Biceps", "Halteres", "pull", "Use pegada neutra, suba sem abrir cotovelos e controle a descida."),
        CatalogExercise("Rosca scott", "Biceps", "Banco scott", "pull", "Apoie os bracos no banco, flexione ate contrair e retorne sem esticar demais."),
        CatalogExercise("Rosca no cabo", "Biceps", "Cabo", "pull", "Use a polia baixa para manter tensao constante durante toda a repeticao."),
        CatalogExercise("Rosca concentrada", "Biceps", "Halter", "pull", "Apoie o cotovelo na coxa, flexione com controle e evite giro do tronco."),

        CatalogExercise("Triceps corda", "Triceps", "Cabo", "push", "Cotovelos fixos ao lado do corpo, estenda e abra a corda no final do movimento."),
        CatalogExercise("Triceps barra", "Triceps", "Cabo", "push", "Empurre a barra para baixo sem mover os ombros e retorne ate alongar o triceps."),
        CatalogExercise("Triceps frances", "Triceps", "Halter ou barra", "push", "Leve a carga atras da cabeca, mantenha cotovelos apontados para frente e estenda."),
        CatalogExercise("Triceps testa", "Triceps", "Barra", "push", "Deitado, desca a barra em direcao a testa e estenda mantendo cotovelos estaveis."),
        CatalogExercise("Mergulho no banco", "Triceps", "Banco", "push", "Desca o corpo proximo ao banco e suba estendendo cotovelos sem elevar ombros."),
        CatalogExercise("Paralelas", "Triceps", "Peso corporal", "push", "Desca entre as barras, mantenha controle e suba sem perder estabilidade escapular."),

        CatalogExercise("Panturrilha em pe", "Panturrilhas", "Maquina", "squat", "Suba na ponta dos pes, pause no topo e desca ate alongar panturrilhas."),
        CatalogExercise("Panturrilha sentado", "Panturrilhas", "Maquina", "squat", "Com joelhos flexionados, eleve calcanhares e controle a fase de descida."),
        CatalogExercise("Panturrilha no leg press", "Panturrilhas", "Leg press", "squat", "Use a ponta dos pes na plataforma e mova apenas tornozelos com amplitude segura."),

        CatalogExercise("Prancha", "Core", "Peso corporal", "core", "Apoie antebracos, mantenha corpo alinhado e contraia abdomen sem prender respiracao."),
        CatalogExercise("Abdominal crunch", "Core", "Peso corporal", "core", "Flexione o tronco tirando as escapulas do solo e retorne sem relaxar totalmente."),
        CatalogExercise("Elevacao de pernas", "Core", "Banco ou barra", "core", "Eleve as pernas com controle, evite embalo e mantenha lombar protegida."),
        CatalogExercise("Abdominal na polia", "Core", "Cabo", "core", "Ajoelhado, flexione a coluna levando cotovelos ao chao contraindo abdomen."),
        CatalogExercise("Russian twist", "Core", "Anilha ou bola", "core", "Gire o tronco de um lado ao outro mantendo abdomen firme."),
        CatalogExercise("Prancha lateral", "Core", "Peso corporal", "core", "Apoie antebraco lateral, mantenha quadril elevado e corpo alinhado."),

        CatalogExercise("Esteira caminhada", "Cardio", "Esteira", "cardio", "Caminhe com postura alta, passada natural e controle respiratorio."),
        CatalogExercise("Esteira corrida", "Cardio", "Esteira", "cardio", "Corra mantendo cadencia estavel e aumente velocidade gradualmente."),
        CatalogExercise("Bicicleta ergometrica", "Cardio", "Bicicleta", "cardio", "Ajuste o banco, pedale com joelhos alinhados e resistencia progressiva."),
        CatalogExercise("Eliptico", "Cardio", "Eliptico", "cardio", "Mantenha tronco alto, empurre e puxe as alcas em ritmo constante."),
        CatalogExercise("Remo indoor", "Cardio", "Remo", "cardio", "Empurre com pernas, finalize com costas e bracos, retorne em ordem inversa.")
    )


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
        localInsights().forEach { insight -> box.addView(label("• " + insight, white, 15f, false)) }
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
        val box = card()
        box.orientation = LinearLayout.VERTICAL
        box.addView(label("PERFIL LOCAL", green, 13f, true))
        box.addView(label("Mo2 LOG pessoal", white, 24f, true))
        box.addView(label("Versao nativa " + versionName + ". Seus dados ficam somente neste celular.", muted, 15f, false))
        val export = actionButton("Exportar todos os dados", green, bg)
        export.setOnClickListener { exportToClipboard() }
        box.addView(buttonParams(export))
        val clear = actionButton("Apagar dados locais", surface2, danger)
        clear.setOnClickListener {
            prefs.edit().remove("set_logs").remove("run_logs").apply()
            Toast.makeText(this, "Dados locais apagados.", Toast.LENGTH_SHORT).show()
            render()
        }
        box.addView(buttonParams(clear))
        root.addView(box)
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
            val item = card(if (active) green else surface)
            item.orientation = LinearLayout.VERTICAL
            item.setOnClickListener {
                selectedExerciseIndex = index
                prefs.edit().putInt("selected_exercise", index).apply()
                render()
            }
            item.addView(label(exercise.name, if (active) bg else white, 18f, true))
            item.addView(label(exercise.target + " | descanso " + exercise.rest, if (active) bg else muted, 14f, false))
            item.addView(label(exercise.notes, if (active) bg else muted, 13f, false))
            box.addView(item)
        }
        return box
    }

    private fun registerPanel(): View {
        val exercise = currentExercise()
        val box = card()
        box.orientation = LinearLayout.VERTICAL
        box.addView(label("REGISTRAR SERIE", green, 13f, true))
        box.addView(label(exercise.name, white, 24f, true))
        val reps = input("Reps", "10")
        val load = input("Carga kg", lastLoadFor(exercise.name))
        val rir = input("RIR", "2")
        val rpe = input("RPE", "8")
        val notes = input("Observacao", "")
        box.addView(reps)
        box.addView(load)
        box.addView(rir)
        box.addView(rpe)
        box.addView(notes)
        val save = actionButton("Salvar serie", green, bg)
        save.setOnClickListener {
            hideKeyboard()
            saveSet(exercise.name, reps.textValue(), load.textValue(), rir.textValue(), rpe.textValue(), notes.textValue())
        }
        box.addView(buttonParams(save))
        val finish = actionButton("Finalizar treino de hoje", surface2, green)
        finish.setOnClickListener {
            prefs.edit().putString("last_finished_day", dayKey()).apply()
            Toast.makeText(this, "Treino finalizado localmente.", Toast.LENGTH_SHORT).show()
        }
        box.addView(buttonParams(finish))
        return box
    }

    private fun saveSet(exercise: String, repsRaw: String, loadRaw: String, rirRaw: String, rpeRaw: String, notes: String) {
        val reps = repsRaw.toIntOrNull() ?: 0
        val load = loadRaw.replace(',', '.').toDoubleOrNull() ?: 0.0
        val rir = rirRaw.toIntOrNull()
        val rpe = rpeRaw.replace(',', '.').toDoubleOrNull()
        val log = JSONObject()
            .put("id", UUID.randomUUID().toString())
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
        prefs.edit().putString("set_logs", logs.toString()).apply()
        Toast.makeText(this, "Serie salva no celular.", Toast.LENGTH_SHORT).show()
        render()
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
        val payload = JSONObject()
            .put("source", "mo2log_native_android")
            .put("version", versionName)
            .put("exported_at", timestamp())
            .put("strength_logs", allLogs())
            .put("run_logs", runLogs())
            .put("goals", JSONObject()
                .put("week_sets", prefs.getString("goal_week_sets", "60"))
                .put("week_volume", prefs.getString("goal_week_volume", "12000"))
                .put("body_weight", prefs.getString("body_weight", "")))
            .toString(2)
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Mo2 LOG export", payload))
        Toast.makeText(this, "Exportacao copiada.", Toast.LENGTH_SHORT).show()
    }

    private fun allLogs(): JSONArray = safeArray("set_logs")
    private fun runLogs(): JSONArray = safeArray("run_logs")

    private fun safeArray(key: String): JSONArray {
        return try {
            JSONArray(prefs.getString(key, "[]"))
        } catch (_: Exception) {
            JSONArray()
        }
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
        if (stats.optInt("week_sets") < 30) insights.add("Semana ainda com volume baixo. Priorize consistencia antes de aumentar carga.")
        else insights.add("Boa consistencia semanal. Mantenha progressao gradual.")
        if (stats.optString("avg_rpe") != "-") insights.add("Use RPE medio para decidir progressao: acima de 9 pede cautela; abaixo de 7 permite subir carga.")
        if (runLogs().length() == 0) insights.add("Nenhuma corrida salva. Use Corrida para registrar esteira e controlar 5 km.")
        if (stats.optInt("total_sets") == 0) insights.add("Primeiro passo: salve uma serie real no Treino.")
        return insights
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

    private fun currentPlan() = plans[selectedPlanIndex.coerceIn(plans.indices)]
    private fun currentExercise() = currentPlan().exercises[selectedExerciseIndex.coerceIn(currentPlan().exercises.indices)]

    private fun buildWorkoutPlans(): List<WorkoutPlan> = listOf(
        WorkoutPlan("a", "Treino A", "Peito/Ombro/Triceps", listOf(
            ExercisePlan("Supino reto ou maquina peitoral", "4 x 8-10", "90s", "Priorize controle e amplitude."),
            ExercisePlan("Supino inclinado com halteres", "3 x 10", "90s", "Suba carga quando fechar reps."),
            ExercisePlan("Desenvolvimento de ombros", "3 x 8-10", "90s", "Evite compensar com lombar."),
            ExercisePlan("Elevacao lateral", "3 x 12-15", "60s", "Cadencia limpa, sem embalo."),
            ExercisePlan("Triceps corda", "3 x 10-12", "60s", "Trave cotovelos perto do corpo."),
            ExercisePlan("Prancha", "3 x 45s", "45s", "Respiracao constante."),
        )),
        WorkoutPlan("b", "Treino B", "Costas/Biceps", listOf(
            ExercisePlan("Puxada frente", "4 x 8-10", "90s", "Puxe com cotovelos, nao com as maos."),
            ExercisePlan("Remada baixa", "4 x 10", "90s", "Pausa curta na contracao."),
            ExercisePlan("Remada unilateral", "3 x 10 cada", "75s", "Mantenha tronco firme."),
            ExercisePlan("Face pull", "3 x 12-15", "60s", "Foco em deltoide posterior."),
            ExercisePlan("Rosca direta", "3 x 8-10", "60s", "Controle na descida."),
            ExercisePlan("Rosca martelo", "3 x 10-12", "60s", "Punho neutro."),
        )),
        WorkoutPlan("c", "Treino C", "Pernas/Core", listOf(
            ExercisePlan("Leg press", "4 x 10", "120s", "Amplitude segura e constante."),
            ExercisePlan("Agachamento livre ou guiado", "3 x 8", "120s", "Priorize tecnica antes de carga."),
            ExercisePlan("Cadeira extensora", "3 x 12", "75s", "Segure um segundo no topo."),
            ExercisePlan("Mesa flexora", "3 x 10-12", "75s", "Controle total na volta."),
            ExercisePlan("Stiff", "3 x 10", "90s", "Quadril para tras, coluna neutra."),
            ExercisePlan("Panturrilha", "4 x 12-15", "45s", "Pausa no alongamento."),
            ExercisePlan("Abdominal ou prancha", "3 series", "45s", "Escolha a variacao do dia."),
        )),
        WorkoutPlan("d", "Treino D", "Full body leve", listOf(
            ExercisePlan("Esteira inclinada", "10-15 min", "livre", "Aquecimento progressivo."),
            ExercisePlan("Supino maquina", "3 x 12", "75s", "Carga moderada."),
            ExercisePlan("Puxada ou remada", "3 x 12", "75s", "Movimento limpo."),
            ExercisePlan("Leg press leve", "3 x 12", "90s", "Sem buscar falha."),
            ExercisePlan("Elevacao lateral", "2 x 15", "45s", "Bombeamento."),
            ExercisePlan("Mobilidade final", "5 min", "livre", "Quadril, ombro e respiracao."),
        )),
    )

    private fun todayPlanIndex(): Int {
        val day = SimpleDateFormat("u", Locale.US).format(Date()).toIntOrNull() ?: 1
        return when (day) {
            1 -> 0
            2 -> 1
            3 -> 2
            4 -> 0
            5 -> 1
            6 -> 3
            else -> 2
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

    private fun metricGrid(metrics: List<Pair<String, String>>): View {
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        metrics.chunked(2).forEach { rowItems ->
            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            rowItems.forEach { item ->
                val card = card()
                card.orientation = LinearLayout.VERTICAL
                card.addView(label(item.first.uppercase(Locale("pt", "BR")), muted, 12f, true))
                card.addView(label(item.second, white, 22f, true))
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
        val box = card()
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
        input.background = rounded(surface2, dp(14), border)
        val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(54))
        params.setMargins(0, dp(8), 0, 0)
        input.layoutParams = params
        return input
    }

    private fun EditText.textValue() = text?.toString().orEmpty()

    private fun sectionTitle(text: String): TextView {
        val view = label(text.uppercase(Locale("pt", "BR")), green, 14f, true)
        view.setPadding(0, dp(18), 0, dp(8))
        return view
    }

    private fun label(text: String, color: Int, size: Float, bold: Boolean): TextView {
        val view = TextView(this)
        view.text = text
        view.setTextColor(color)
        view.textSize = size
        view.setLineSpacing(0f, 1.08f)
        if (bold) view.typeface = Typeface.DEFAULT_BOLD
        return view
    }

    private fun pill(text: String, active: Boolean, width: Int, height: Int): TextView {
        val view = label(text, if (active) bg else white, 14f, true)
        view.gravity = Gravity.CENTER
        view.setPadding(dp(12), 0, dp(12), 0)
        view.background = rounded(if (active) green else surface, dp(18), border)
        val params = LinearLayout.LayoutParams(dp(width), dp(height))
        params.setMargins(0, 0, dp(10), dp(10))
        view.layoutParams = params
        return view
    }

    private fun actionButton(text: String, color: Int, textColor: Int): Button {
        val button = Button(this)
        button.text = text
        button.textSize = 14f
        button.typeface = Typeface.DEFAULT_BOLD
        button.setTextColor(textColor)
        button.background = rounded(color, dp(16), if (color == green) green else border)
        button.isAllCaps = false
        return button
    }

    private fun buttonParams(button: Button): View {
        val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(54))
        params.setMargins(0, dp(10), 0, 0)
        button.layoutParams = params
        return button
    }

    private fun card(color: Int = surface): LinearLayout {
        val box = LinearLayout(this)
        box.setPadding(dp(16), dp(16), dp(16), dp(16))
        box.background = rounded(color, dp(22), border)
        val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        params.setMargins(0, dp(8), 0, dp(8))
        box.layoutParams = params
        return box
    }

    private fun rounded(color: Int, radius: Int, stroke: Int? = null): GradientDrawable {
        val drawable = GradientDrawable()
        drawable.setColor(color)
        drawable.cornerRadius = radius.toFloat()
        if (stroke != null) drawable.setStroke(dp(1), stroke)
        return drawable
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
