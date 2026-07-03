package br.com.mo2log.mobile

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
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

class MainActivity : Activity() {
    private val bg = Color.rgb(5, 8, 7)
    private val surface = Color.rgb(13, 24, 20)
    private val surface2 = Color.rgb(19, 36, 30)
    private val border = Color.rgb(35, 58, 49)
    private val green = Color.rgb(105, 255, 95)
    private val muted = Color.rgb(151, 171, 164)
    private val white = Color.WHITE
    private val danger = Color.rgb(255, 116, 116)

    private val prefs by lazy { getSharedPreferences("mo2log_native", Context.MODE_PRIVATE) }
    private val plans by lazy { buildWorkoutPlans() }
    private var selectedPlanIndex = 0
    private var selectedExerciseIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        root.addView(todaySummary())
        root.addView(planSelector())
        root.addView(exerciseList())
        root.addView(registerPanel())
        root.addView(historyPanel())

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
        titleBox.addView(label("Treino pessoal Android", white, 26f, true))
        titleBox.addView(label("Sem servidor. Sem login. Dados salvos neste celular.", muted, 14f, false))
        top.addView(titleBox, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        box.addView(top)

        return box
    }

    private fun todaySummary(): View {
        val logs = todayLogs()
        val sets = logs.length()
        var volume = 0.0
        var rpeSum = 0.0
        var rpeCount = 0
        for (i in 0 until logs.length()) {
            val item = logs.getJSONObject(i)
            volume += item.optDouble("load") * item.optInt("reps")
            val rpe = item.optDouble("rpe", -1.0)
            if (rpe >= 0) {
                rpeSum += rpe
                rpeCount += 1
            }
        }

        val averageRpe = if (rpeCount == 0) "-" else String.format(Locale("pt", "BR"), "%.1f", rpeSum / rpeCount)
        val box = card()
        box.orientation = LinearLayout.VERTICAL
        box.addView(label("HOJE", green, 13f, true))
        box.addView(label("$sets series registradas", white, 28f, true))
        box.addView(label("Volume: ${volume.roundToInt()} kg | RPE medio: $averageRpe", muted, 15f, false))
        return box
    }

    private fun planSelector(): View {
        val wrapper = LinearLayout(this)
        wrapper.orientation = LinearLayout.VERTICAL
        wrapper.addView(sectionTitle("Programa de treinos"))

        val scroll = HorizontalScrollView(this)
        scroll.isHorizontalScrollBarEnabled = false
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL

        plans.forEachIndexed { index, plan ->
            val active = selectedPlanIndex == index
            val button = pill("${plan.title}\n${plan.focus}", active)
            button.setOnClickListener {
                selectedPlanIndex = index
                selectedExerciseIndex = 0
                prefs.edit().putInt("selected_plan", index).putInt("selected_exercise", 0).apply()
                render()
            }
            row.addView(button)
        }

        scroll.addView(row)
        wrapper.addView(scroll)
        return wrapper
    }

    private fun exerciseList(): View {
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.addView(sectionTitle("Exercicios do treino"))

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
            item.addView(label("${exercise.target} | descanso ${exercise.rest}", if (active) bg else muted, 14f, false))
            item.addView(label(exercise.notes, if (active) bg else muted, 13f, false))
            box.addView(item)
        }

        return box
    }

    private fun registerPanel(): View {
        val exercise = currentExercise()
        val box = card()
        box.orientation = LinearLayout.VERTICAL
        box.addView(label("Registrar serie", green, 13f, true))
        box.addView(label(exercise.name, white, 24f, true))

        val reps = input("Reps", defaultValue = "10")
        val load = input("Carga kg", defaultValue = lastLoadFor(exercise.name))
        val rir = input("RIR", defaultValue = "2")
        val rpe = input("RPE", defaultValue = "8")
        val notes = input("Observacao", defaultValue = "")

        val grid = LinearLayout(this)
        grid.orientation = LinearLayout.VERTICAL
        grid.addView(reps)
        grid.addView(load)
        grid.addView(rir)
        grid.addView(rpe)
        grid.addView(notes)
        box.addView(grid)

        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL

        val save = actionButton("Salvar serie", green, bg)
        save.setOnClickListener {
            hideKeyboard()
            saveSet(exercise.name, reps.textValue(), load.textValue(), rir.textValue(), rpe.textValue(), notes.textValue())
        }
        row.addView(save, LinearLayout.LayoutParams(0, dp(54), 1f))

        val export = actionButton("Exportar", surface2, white)
        export.setOnClickListener { exportToClipboard() }
        val exportParams = LinearLayout.LayoutParams(0, dp(54), 1f)
        exportParams.setMargins(dp(10), 0, 0, 0)
        row.addView(export, exportParams)
        box.addView(row)

        val finish = actionButton("Finalizar treino de hoje", surface2, green)
        finish.setOnClickListener {
            prefs.edit().putString("last_finished_day", dayKey()).apply()
            Toast.makeText(this, "Treino finalizado localmente.", Toast.LENGTH_SHORT).show()
            render()
        }
        val finishParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(54))
        finishParams.setMargins(0, dp(10), 0, 0)
        box.addView(finish, finishParams)

        return box
    }

    private fun historyPanel(): View {
        val box = card()
        box.orientation = LinearLayout.VERTICAL
        box.addView(label("Historico local", green, 13f, true))

        val logs = allLogs()
        if (logs.length() == 0) {
            box.addView(label("Nenhuma serie salva ainda.", muted, 15f, false))
            return box
        }

        val start = maxOf(0, logs.length() - 8)
        for (i in logs.length() - 1 downTo start) {
            val item = logs.getJSONObject(i)
            val time = item.optString("time")
            val exercise = item.optString("exercise")
            val reps = item.optInt("reps")
            val load = item.optDouble("load")
            val text = "$time | $exercise | $reps reps | $load kg"
            box.addView(label(text, white, 14f, false))
        }

        val clear = actionButton("Limpar historico local", surface2, danger)
        clear.setOnClickListener {
            prefs.edit().remove("set_logs").apply()
            Toast.makeText(this, "Historico local apagado.", Toast.LENGTH_SHORT).show()
            render()
        }
        val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(52))
        params.setMargins(0, dp(12), 0, 0)
        box.addView(clear, params)
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

    private fun exportToClipboard() {
        val payload = JSONObject()
            .put("source", "mo2log_native_android")
            .put("exported_at", timestamp())
            .put("logs", allLogs())
            .toString(2)
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Mo2 LOG export", payload))
        Toast.makeText(this, "Exportacao copiada.", Toast.LENGTH_SHORT).show()
    }

    private fun allLogs(): JSONArray {
        return try {
            JSONArray(prefs.getString("set_logs", "[]"))
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
        WorkoutPlan(
            "a",
            "Treino A",
            "Peito/Ombro/Triceps",
            listOf(
                ExercisePlan("Supino reto ou maquina peitoral", "4 x 8-10", "90s", "Priorize controle e amplitude."),
                ExercisePlan("Supino inclinado com halteres", "3 x 10", "90s", "Suba carga quando fechar reps."),
                ExercisePlan("Desenvolvimento de ombros", "3 x 8-10", "90s", "Evite compensar com lombar."),
                ExercisePlan("Elevacao lateral", "3 x 12-15", "60s", "Cadencia limpa, sem embalo."),
                ExercisePlan("Triceps corda", "3 x 10-12", "60s", "Trave cotovelos perto do corpo."),
                ExercisePlan("Prancha", "3 x 45s", "45s", "Respiracao constante."),
            ),
        ),
        WorkoutPlan(
            "b",
            "Treino B",
            "Costas/Biceps",
            listOf(
                ExercisePlan("Puxada frente", "4 x 8-10", "90s", "Puxe com cotovelos, nao com as maos."),
                ExercisePlan("Remada baixa", "4 x 10", "90s", "Pausa curta na contracao."),
                ExercisePlan("Remada unilateral", "3 x 10 cada", "75s", "Mantenha tronco firme."),
                ExercisePlan("Face pull", "3 x 12-15", "60s", "Foco em deltoide posterior."),
                ExercisePlan("Rosca direta", "3 x 8-10", "60s", "Controle na descida."),
                ExercisePlan("Rosca martelo", "3 x 10-12", "60s", "Punho neutro."),
            ),
        ),
        WorkoutPlan(
            "c",
            "Treino C",
            "Pernas/Core",
            listOf(
                ExercisePlan("Leg press", "4 x 10", "120s", "Amplitude segura e constante."),
                ExercisePlan("Agachamento livre ou guiado", "3 x 8", "120s", "Priorize tecnica antes de carga."),
                ExercisePlan("Cadeira extensora", "3 x 12", "75s", "Segure um segundo no topo."),
                ExercisePlan("Mesa flexora", "3 x 10-12", "75s", "Controle total na volta."),
                ExercisePlan("Stiff", "3 x 10", "90s", "Quadril para tras, coluna neutra."),
                ExercisePlan("Panturrilha", "4 x 12-15", "45s", "Pausa no alongamento."),
                ExercisePlan("Abdominal ou prancha", "3 series", "45s", "Escolha a variacao do dia."),
            ),
        ),
        WorkoutPlan(
            "d",
            "Treino D",
            "Full body leve",
            listOf(
                ExercisePlan("Esteira inclinada", "10-15 min", "livre", "Aquecimento progressivo."),
                ExercisePlan("Supino maquina", "3 x 12", "75s", "Carga moderada."),
                ExercisePlan("Puxada ou remada", "3 x 12", "75s", "Movimento limpo."),
                ExercisePlan("Leg press leve", "3 x 12", "90s", "Sem buscar falha."),
                ExercisePlan("Elevacao lateral", "2 x 15", "45s", "Bombeamento."),
                ExercisePlan("Mobilidade final", "5 min", "livre", "Quadril, ombro e respiracao."),
            ),
        ),
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

    private fun pill(text: String, active: Boolean): TextView {
        val view = label(text, if (active) bg else white, 15f, true)
        view.gravity = Gravity.CENTER
        view.setPadding(dp(16), 0, dp(16), 0)
        view.background = rounded(if (active) green else surface, dp(18), border)
        val params = LinearLayout.LayoutParams(dp(168), dp(72))
        params.setMargins(0, 0, dp(10), 0)
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

    private fun card(color: Int = surface): LinearLayout {
        val box = LinearLayout(this)
        box.setPadding(dp(16), dp(16), dp(16), dp(16))
        box.background = rounded(color, dp(22), border)
        val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        params.setMargins(0, dp(10), 0, dp(10))
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
    private fun timeKey() = SimpleDateFormat("HH:mm", Locale("pt", "BR")).format(Date())
    private fun timestamp() = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(Date())
    private fun dp(value: Int) = (value * resources.displayMetrics.density).roundToInt()
}
