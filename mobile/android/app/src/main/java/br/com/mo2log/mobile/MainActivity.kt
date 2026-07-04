package br.com.mo2log.mobile

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
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
        setBackgroundColor(Color.rgb(8, 18, 15))

        image.scaleType = ImageView.ScaleType.FIT_CENTER
        image.adjustViewBounds = false
        addView(image, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))

        status.text = "Carregando frames de execucao..."
        status.setTextColor(Color.rgb(151, 171, 164))
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
            val loaded = links.mapNotNull { link ->
                try {
                    val connection = URL(link).openConnection()
                    connection.connectTimeout = 7000
                    connection.readTimeout = 9000
                    connection.getInputStream().use { input -> BitmapFactory.decodeStream(input) }
                } catch (_: Exception) {
                    null
                }
            }

            post {
                frames.clear()
                frames.addAll(loaded)
                if (frames.isNotEmpty()) {
                    image.setImageBitmap(frames.first())
                    status.text = "Fonte: Free Exercise DB"
                } else {
                    status.text = "Nao foi possivel carregar a midia agora"
                }
            }
        }.start()
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
    private val versionName = "8.6.0"
    private val bg = Color.rgb(5, 8, 7)
    private val surface = Color.rgb(13, 24, 20)
    private val surface2 = Color.rgb(19, 36, 30)
    private val surface3 = Color.rgb(8, 31, 24)
    private val border = Color.rgb(35, 58, 49)
    private val green = Color.rgb(105, 255, 95)
    private val muted = Color.rgb(151, 171, 164)
    private val white = Color.WHITE
    private val danger = Color.rgb(255, 116, 116)
    private val amber = Color.rgb(255, 198, 88)

    private val prefs by lazy { getSharedPreferences("mo2log_native", Context.MODE_PRIVATE) }
    private val plans by lazy { buildWorkoutPlans() }
    private val catalog by lazy { loadExerciseCatalog() }
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

    private fun navBar(): View {
        val wrap = LinearLayout(this)
        wrap.orientation = LinearLayout.VERTICAL
        val title = label(currentSectionTitle(), white, 19f, true)
        title.setPadding(0, dp(8), 0, dp(6))
        wrap.addView(title)

        val scroll = HorizontalScrollView(this)
        scroll.isHorizontalScrollBarEnabled = false
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        navItems.forEach { item ->
            val active = currentTab == item.id
            val button = pill(item.title, active, 124, 46)
            button.setOnClickListener {
                currentTab = item.id
                prefs.edit().putString("current_tab", currentTab).apply()
                render()
            }
            row.addView(button)
        }
        scroll.addView(row)
        wrap.addView(scroll)
        return wrap
    }

    private fun renderHome(root: LinearLayout) {
        val logs = todayLogs()
        val stats = computeStats(allLogs())
        val next = currentPlan()

        val hero = card(surface3)
        hero.orientation = LinearLayout.VERTICAL
        hero.addView(label("RESUMO DE HOJE", green, 13f, true))
        hero.addView(label(logs.length().toString() + " series registradas", white, 28f, true))
        hero.addView(label("Volume: " + stats.optInt("today_volume") + " kg | RPE medio: " + stats.optString("avg_rpe"), muted, 15f, false))
        val heroActions = LinearLayout(this)
        heroActions.orientation = LinearLayout.HORIZONTAL
        val start = actionButton("Abrir treino", green, bg)
        start.setOnClickListener { switchTab("workout") }
        heroActions.addView(start, LinearLayout.LayoutParams(0, dp(52), 1f))
        val catalogButton = actionButton("Catalogo", surface2, white)
        catalogButton.setOnClickListener { switchTab("exercises") }
        heroActions.addView(catalogButton, LinearLayout.LayoutParams(0, dp(52), 1f))
        hero.addView(spacedRow(heroActions))
        root.addView(hero)

        val nextBox = card()
        nextBox.orientation = LinearLayout.VERTICAL
        nextBox.addView(label("PROXIMO TREINO", green, 13f, true))
        nextBox.addView(label(next.title + " - " + next.focus, white, 23f, true))
        nextBox.addView(label(next.exercises.size.toString() + " exercicios prontos. O primeiro toque ja leva ao registro.", muted, 15f, false))
        val nextActions = LinearLayout(this)
        nextActions.orientation = LinearLayout.HORIZONTAL
        val workout = actionButton("Registrar", green, bg)
        workout.setOnClickListener { switchTab("workout") }
        nextActions.addView(workout, LinearLayout.LayoutParams(0, dp(50), 1f))
        val run = actionButton("Corrida", surface2, white)
        run.setOnClickListener { switchTab("running") }
        nextActions.addView(run, LinearLayout.LayoutParams(0, dp(50), 1f))
        nextBox.addView(spacedRow(nextActions))
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
        localInsights().forEach { insight -> insightBox.addView(label("- " + insight, white, 15f, false)) }
        root.addView(insightBox)
    }

    private fun renderWorkout(root: LinearLayout) {
        val plan = currentPlan()
        root.addView(heroCard("Treino selecionado", plan.title + " - " + plan.focus, "Toque no exercicio, confira a meta e registre a serie no painel abaixo."))
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
        summary.addView(label(catalogItems.size.toString() + " exercicios", white, 25f, true))
        summary.addView(label("Busque por nome, musculo ou equipamento. Abra um exercicio para ver execucao, cuidados e alternativas.", muted, 15f, false))
        root.addView(summary)

        val muscleOptions = listOf("Todos") + catalogItems.map { it.muscle }.distinct().sorted()
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
            .filter { selectedMuscle == "Todos" || it.muscle == selectedMuscle }
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

        detail.addView(label("Alternativos para o mesmo musculo", green, 14f, true))
        alternativesFor(selected).forEach { alternative ->
            val alt = pill(alternative.name, false, 290, 48)
            alt.setOnClickListener {
                prefs.edit().putString("catalog_selected", alternative.id).apply()
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
        filtered.take(80).forEach { exercise ->
            val item = card(if (exercise.id == selected.id) surface2 else surface)
            item.orientation = LinearLayout.VERTICAL
            item.setOnClickListener {
                prefs.edit().putString("catalog_selected", exercise.id).apply()
                render()
            }
            item.addView(label(exercise.name, white, 17f, true))
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

    private fun exerciseMeta(exercise: CatalogExercise): String {
        val equipment = exercise.equipment.ifBlank { "equipamento variavel" }
        val primary = exercise.primary.ifBlank { exercise.subgroup.ifBlank { exercise.muscle } }
        return exercise.muscle + " | " + equipment + " | " + primary
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

    private fun currentSectionTitle(): String {
        return navItems.firstOrNull { it.id == currentTab }?.title ?: "Inicio"
    }

    private fun statusChip(text: String): TextView {
        val view = label(text.uppercase(Locale("pt", "BR")), green, 12f, true)
        view.gravity = Gravity.CENTER
        view.setPadding(dp(10), 0, dp(10), 0)
        view.background = rounded(surface, dp(8), border)
        return view
    }

    private fun compactMetric(title: String, value: String): View {
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.setPadding(dp(8), dp(8), dp(8), dp(8))
        box.background = rounded(surface, dp(8), border)
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
                val card = card()
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

    private fun EditText.textValue() = text?.toString().orEmpty()

    private fun sectionTitle(text: String): TextView {
        val view = label(text.uppercase(Locale("pt", "BR")), green, 14f, true)
        view.setPadding(0, dp(20), 0, dp(8))
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
        view.background = rounded(if (active) green else surface, dp(8), if (active) green else border)
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
        button.background = rounded(color, dp(8), if (color == green) green else border)
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
        box.background = rounded(color, dp(8), border)
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
