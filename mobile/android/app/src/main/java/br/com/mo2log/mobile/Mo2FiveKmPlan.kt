package br.com.mo2log.mobile

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToLong

enum class Mo2FiveKmCyclePhase {
    BEFORE_START,
    WEEK_1,
    WEEK_2,
    WEEK_3,
    RACE_DAY,
    COMPLETED,
}

data class Mo2FiveKmCyclePosition(
    val phase: Mo2FiveKmCyclePhase,
    val week: Int?,
)

object Mo2FiveKmPlan {
    const val PresetId = "5k-2026-08-16-v1"
    const val CycleId = "5k-2026-08-16"
    const val CycleName = "5 km - Prova 16/08/2026"
    const val StartDay = "2026-07-27"
    const val RaceDay = "2026-08-16"
    const val TargetDistanceKm = 5.0
    const val TargetSeconds = 1470L

    fun strengthPlans(): List<WorkoutPlan> = listOf(
        WorkoutPlan(
            id = "$CycleId-strength-a",
            title = "Treino A",
            focus = "Peito, ombros, triceps e core; RPE 7-8; corrida leve depois",
            exercises = listOf(
                ExercisePlan("Supino reto", "4 x 8-10", "90s", "Tecnica limpa, amplitude segura e RPE 7-8. Use maquina peitoral como alternativa."),
                ExercisePlan("Supino inclinado com halteres", "3 x 10", "90s", "Controle a descida e encerre a serie com repeticoes de reserva."),
                ExercisePlan("Desenvolvimento de ombros", "3 x 8-10", "90s", "Mantenha o tronco firme e evite compensar com a lombar."),
                ExercisePlan("Elevacao lateral", "3 x 12-15", "60s", "Cadencia controlada, sem embalo."),
                ExercisePlan("Triceps corda", "3 x 10-12", "60s", "Mantenha os cotovelos proximos ao corpo."),
                ExercisePlan("Abdominal na polia", "3 x 12-15", "60s", "Expire durante a flexao e preserve o controle lombar."),
            ),
            dayIndex = 2,
            homeCardKey = Mo2WorkoutHomeCard.Push,
        ),
        WorkoutPlan(
            id = "$CycleId-strength-b",
            title = "Treino B",
            focus = "Pernas e core; RPE 7-8; sem corrida e sem falha",
            exercises = listOf(
                ExercisePlan("Agachamento no Smith", "3 x 6-8", "120s", "Execucao controlada, RPE 7-8 e sem falha. Agachamento livre fica disponivel como alternativa."),
                ExercisePlan("Leg press", "3 x 10", "120s", "Amplitude confortavel para o joelho e repeticoes de reserva."),
                ExercisePlan("Mesa flexora", "3 x 10-12", "75s", "Controle a fase de retorno."),
                ExercisePlan("Stiff", "3 x 8-10", "90s", "Quadril para tras, coluna neutra e sem buscar falha."),
                ExercisePlan("Panturrilha", "4 x 12-15", "45s", "Use amplitude completa e pausa curta."),
                ExercisePlan("Abdominal na polia", "3 x 12-15", "60s", "Mantenha o quadril estavel e expire na contracao."),
            ),
            dayIndex = 3,
            homeCardKey = Mo2WorkoutHomeCard.Legs,
        ),
        WorkoutPlan(
            id = "$CycleId-strength-c",
            title = "Treino C",
            focus = "Costas e biceps; RPE 7-8; corrida depois",
            exercises = listOf(
                ExercisePlan("Puxada frente", "4 x 8-10", "90s", "Conduza o movimento pelos cotovelos e mantenha o tronco estavel."),
                ExercisePlan("Remada baixa", "4 x 10", "90s", "Pausa curta na contracao e retorno controlado."),
                ExercisePlan("Remada unilateral", "3 x 10 cada lado", "75s", "Evite girar o tronco."),
                ExercisePlan("Face pull", "3 x 12-15", "60s", "Direcione a corda para a face com ombros baixos."),
                ExercisePlan("Rosca direta", "3 x 8-10", "60s", "Mantenha os cotovelos estaveis e controle a descida."),
                ExercisePlan("Rosca martelo", "3 x 10-12", "60s", "Punhos neutros e sem impulso."),
            ),
            dayIndex = 5,
            homeCardKey = Mo2WorkoutHomeCard.Pull,
        ),
    )

    fun runningWorkouts(): List<RunningWorkout> = listOf(
        runningWorkout(
            id = "$CycleId-w1-mon",
            week = 1,
            dayIndex = 1,
            date = "2026-07-27",
            title = "Intervalado 6 x 400 m",
            focus = "Velocidade controlada",
            description = "Sem musculacao. Corra forte com tecnica, sem transformar os tiros em sprint.",
            stages = intervalStages(
                warmDistance = 1.20,
                warmSpeed = 9.5,
                repeats = 6,
                hardDistance = 0.40,
                hardSpeed = 13.0,
                recoveryDistance = 0.20,
                coolDistance = 1.00,
            ),
        ),
        runningWorkout(
            id = "$CycleId-w1-tue",
            week = 1,
            dayIndex = 2,
            date = "2026-07-28",
            title = "Corrida leve apos Treino A",
            focus = "Recuperacao e conversa facil",
            description = "Faca a musculacao primeiro e mantenha a corrida confortavel, sem buscar recorde.",
            stages = listOf(stage("Leve continuo", 5.00, 10.0, "Respiracao controlada e conversa facil.")),
        ),
        runningWorkout(
            id = "$CycleId-w1-thu",
            week = 1,
            dayIndex = 4,
            date = "2026-07-30",
            title = "Ritmo controlado",
            focus = "Sustentar ritmo com controle",
            description = "Sessao especifica sem musculacao no mesmo dia.",
            stages = listOf(
                stage("Aquecimento", 1.00, 9.5, "Comece leve e progressivo."),
                stage("Ritmo", 3.00, 11.6, "Ritmo firme, postura estavel e respiracao controlada."),
                stage("Desaquecimento", 1.00, 9.0, "Reduza gradualmente."),
            ),
        ),
        runningWorkout(
            id = "$CycleId-w1-fri",
            week = 1,
            dayIndex = 5,
            date = "2026-07-31",
            title = "Longo leve apos Treino C",
            focus = "Base aerobica",
            description = "Faca a musculacao primeiro e preserve um ritmo facil do inicio ao fim.",
            stages = listOf(stage("Longo leve", 7.00, 9.8, "Confortavel, sem aceleracao final.")),
        ),
        runningWorkout(
            id = "$CycleId-w2-mon",
            week = 2,
            dayIndex = 1,
            date = "2026-08-03",
            title = "Intervalado 5 x 800 m",
            focus = "Resistencia em ritmo de 5 km",
            description = "Sem musculacao. Mantenha os tiros uniformes e sem sprint.",
            stages = intervalStages(
                warmDistance = 1.20,
                warmSpeed = 9.5,
                repeats = 5,
                hardDistance = 0.80,
                hardSpeed = 12.6,
                recoveryDistance = 0.40,
                coolDistance = 1.00,
            ),
        ),
        runningWorkout(
            id = "$CycleId-w2-tue",
            week = 2,
            dayIndex = 2,
            date = "2026-08-04",
            title = "Corrida leve apos Treino A",
            focus = "Volume leve",
            description = "Faca a musculacao primeiro e mantenha conversa facil.",
            stages = listOf(stage("Leve continuo", 5.50, 10.0, "Ritmo facil e respiracao controlada.")),
        ),
        runningWorkout(
            id = "$CycleId-w2-thu",
            week = 2,
            dayIndex = 4,
            date = "2026-08-06",
            title = "Ritmo especifico de 5 km",
            focus = "Reconhecer o ritmo-alvo",
            description = "Sessao especifica sem musculacao no mesmo dia.",
            stages = listOf(
                stage("Aquecimento", 1.00, 9.5, "Comece leve e progressivo."),
                stage("Ritmo de 5 km", 4.00, 12.0, "Estabilize tecnica e respiracao."),
                stage("Desaquecimento", 1.00, 9.0, "Reduza gradualmente."),
            ),
        ),
        runningWorkout(
            id = "$CycleId-w2-fri",
            week = 2,
            dayIndex = 5,
            date = "2026-08-07",
            title = "Longo progressivo apos Treino C",
            focus = "Resistencia com final controlado",
            description = "Faca a musculacao primeiro. A progressao deve continuar confortavel para o joelho.",
            stages = listOf(
                stage("Longo leve", 6.00, 9.8, "Acumule volume sem forcar."),
                stage("Progressivo", 2.00, 10.5, "Aumente de forma gradual, sem sprint."),
            ),
        ),
        runningWorkout(
            id = "$CycleId-w3-mon",
            week = 3,
            dayIndex = 1,
            date = "2026-08-10",
            title = "Afiamento 4 x 400 m",
            focus = "Velocidade com baixo volume",
            description = "Sem musculacao. Priorize leveza e tecnica.",
            stages = intervalStages(
                warmDistance = 1.00,
                warmSpeed = 9.5,
                repeats = 4,
                hardDistance = 0.40,
                hardSpeed = 13.4,
                recoveryDistance = 0.20,
                coolDistance = 1.00,
            ),
        ),
        runningWorkout(
            id = "$CycleId-w3-tue",
            week = 3,
            dayIndex = 2,
            date = "2026-08-11",
            title = "Corrida leve apos Treino A reduzido",
            focus = "Manter o gesto sem fadiga",
            description = "Faca as duas series de cada exercicio antes da corrida. RPE maximo 7.",
            stages = listOf(stage("Leve continuo", 4.50, 10.0, "Conversa facil e passada solta.")),
        ),
        runningWorkout(
            id = "$CycleId-w3-thu",
            week = 3,
            dayIndex = 4,
            date = "2026-08-13",
            title = "Ritmo de prova curto",
            focus = "Ativar o ritmo sem acumular fadiga",
            description = "Sessao curta e especifica, sem musculacao.",
            stages = listOf(
                stage("Aquecimento", 1.00, 9.5, "Comece leve e progressivo."),
                stage("Bloco 1", 1.00, 12.2, "Ritmo de prova controlado."),
                stage("Recuperacao", 0.40, 8.5, "Trote leve."),
                stage("Bloco 2", 1.00, 12.2, "Repita o ritmo com tecnica."),
                stage("Desaquecimento", 0.80, 9.0, "Finalize leve."),
            ),
        ),
        runningWorkout(
            id = "$CycleId-w3-fri",
            week = 3,
            dayIndex = 5,
            date = "2026-08-14",
            title = "Soltura apos Treino C reduzido",
            focus = "Ativacao curta antes da prova",
            description = "Faca duas series por exercicio, RPE 6-7, e depois corra sem acumular fadiga.",
            stages = raceWeekShakeoutStages(),
        ),
        runningWorkout(
            id = "$CycleId-w3-race",
            week = 3,
            dayIndex = 7,
            date = RaceDay,
            title = "PROVA 5 KM",
            focus = "Meta aproximada de 24:25-24:35",
            description = "Registre somente os 5,00 km da prova. Na rua, use o pace como referencia principal e ajuste por percepcao.",
            stages = listOf(
                stage("Km 1", 1.00, 12.0, "Largada controlada."),
                stage("Km 2 e 3", 2.00, 12.2, "Estabilize respiracao e tecnica."),
                stage("Km 4", 1.00, 12.4, "Aumente gradualmente."),
                stage("Km 5", 1.00, 12.6, "Use a velocidade como referencia e acelere por percepcao se houver reserva."),
            ),
            isRace = true,
        ),
    )

    fun cyclePosition(dayKey: String): Mo2FiveKmCyclePosition {
        val day = parseDay(dayKey) ?: return Mo2FiveKmCyclePosition(Mo2FiveKmCyclePhase.BEFORE_START, 1)
        val start = LocalDate.parse(StartDay)
        val race = LocalDate.parse(RaceDay)
        if (day.isBefore(start)) return Mo2FiveKmCyclePosition(Mo2FiveKmCyclePhase.BEFORE_START, 1)
        if (day.isAfter(race)) return Mo2FiveKmCyclePosition(Mo2FiveKmCyclePhase.COMPLETED, null)
        if (day == race) return Mo2FiveKmCyclePosition(Mo2FiveKmCyclePhase.RACE_DAY, 3)
        val week = (ChronoUnit.DAYS.between(start, day) / 7L + 1L).toInt().coerceIn(1, 3)
        val phase = when (week) {
            1 -> Mo2FiveKmCyclePhase.WEEK_1
            2 -> Mo2FiveKmCyclePhase.WEEK_2
            else -> Mo2FiveKmCyclePhase.WEEK_3
        }
        return Mo2FiveKmCyclePosition(phase, week)
    }

    fun daysUntilRace(dayKey: String): Long? {
        val day = parseDay(dayKey) ?: return null
        return ChronoUnit.DAYS.between(day, LocalDate.parse(RaceDay))
    }

    fun nextWorkout(
        workouts: List<RunningWorkout>,
        dayKey: String,
        completedIds: Set<String>,
    ): RunningWorkout? {
        if (cyclePosition(dayKey).phase == Mo2FiveKmCyclePhase.COMPLETED) return null
        val day = parseDay(dayKey) ?: return null
        return workouts
            .asSequence()
            .filterNot { it.id in completedIds }
            .mapNotNull { workout -> parseDay(workout.scheduledDate)?.let { Pair(it, workout) } }
            .filter { (scheduled, _) -> !scheduled.isBefore(day) }
            .sortedBy { it.first }
            .map { it.second }
            .firstOrNull()
    }

    fun paceSecondsPerKm(speedKmh: Double): Long? {
        if (speedKmh <= 0.0) return null
        return (3600.0 / speedKmh).roundToLong()
    }

    fun totalDistance(workout: RunningWorkout): Double = workout.stages.sumOf(RunningStage::distanceKm)

    fun effectiveStrengthPlan(
        plan: WorkoutPlan,
        dayKey: String,
        readiness: String,
    ): WorkoutPlan {
        if (!plan.id.startsWith("$CycleId-strength-")) return plan
        val isRaceWeek = cyclePosition(dayKey).week == 3
        if (plan.id.endsWith("strength-b") && readiness == "red") {
            return plan.copy(
                title = "Treino B - Recuperacao",
                focus = "Pernas e core leves; sem corrida",
                exercises = plan.exercises
                    .filterNot { it.name == "Stiff" }
                    .map { exercise ->
                        exercise.copy(
                            target = recoveryTarget(exercise.name),
                            notes = exercise.notes + " Check-in vermelho: carga leve, RPE 5-6 e sem falha.",
                        )
                    },
            )
        }
        if (!isRaceWeek && readiness != "yellow") return plan
        if (plan.id.endsWith("strength-b") && isRaceWeek) {
            return plan.copy(
                focus = "Ativacao de pernas e core; sem corrida e sem falha",
                exercises = plan.exercises
                    .filterNot { it.name == "Stiff" }
                    .map { exercise ->
                        exercise.copy(
                            target = raceWeekLegTarget(exercise.name),
                            notes = exercise.notes + " Semana da prova: carga leve e RPE maximo 7.",
                        )
                    },
            )
        }
        return plan.copy(
            focus = if (isRaceWeek) plan.focus.replace("RPE 7-8", "RPE 6-7") else plan.focus,
            exercises = plan.exercises.map { exercise ->
                exercise.copy(
                    target = if (isRaceWeek) twoSetTarget(exercise.target) else exercise.target,
                    notes = exercise.notes + when {
                        isRaceWeek -> " Semana da prova: RPE 6-7, duas series e sem falha."
                        readiness == "yellow" -> " Check-in amarelo: reduza a carga e nao busque falha."
                        else -> ""
                    },
                )
            },
        )
    }

    private fun runningWorkout(
        id: String,
        week: Int,
        dayIndex: Int,
        date: String,
        title: String,
        focus: String,
        description: String,
        stages: List<RunningStage>,
        isRace: Boolean = false,
    ): RunningWorkout = RunningWorkout(
        id = id,
        week = week,
        dayName = weekdayName(dayIndex),
        dayIndex = dayIndex,
        title = title,
        focus = focus,
        description = description,
        stages = stages,
        scheduledDate = date,
        cycleId = CycleId,
        isRace = isRace,
    )

    private fun intervalStages(
        warmDistance: Double,
        warmSpeed: Double,
        repeats: Int,
        hardDistance: Double,
        hardSpeed: Double,
        recoveryDistance: Double,
        coolDistance: Double,
    ): List<RunningStage> {
        val stages = mutableListOf(stage("Aquecimento", warmDistance, warmSpeed, "Comece leve e progressivo."))
        for (index in 1..repeats) {
            stages.add(stage("Tiro $index", hardDistance, hardSpeed, "Forte e controlado, sem sprint."))
            if (index < repeats) {
                stages.add(stage("Recuperacao $index", recoveryDistance, 8.5, "Trote leve entre os tiros."))
            }
        }
        stages.add(stage("Desaquecimento", coolDistance, 9.0, "Finalize leve."))
        return stages
    }

    private fun raceWeekShakeoutStages(): List<RunningStage> {
        val stages = mutableListOf(stage("Corrida leve", 3.00, 9.5, "Passada solta e respiracao facil."))
        for (index in 1..4) {
            stages.add(stage("Aceleracao $index", 0.10, 13.0, "Rapida e controlada, sem sprint maximo."))
            if (index < 4) stages.add(stage("Recuperacao $index", 0.10, 8.5, "Trote leve entre as aceleracoes."))
        }
        return stages
    }

    private fun stage(title: String, distance: Double, speed: Double, note: String): RunningStage {
        return RunningStage(title, distance, speed, note)
    }

    private fun weekdayName(dayIndex: Int): String {
        return listOf("Segunda", "Terca", "Quarta", "Quinta", "Sexta", "Sabado", "Domingo")
            .getOrElse(dayIndex - 1) { "Dia" }
    }

    private fun raceWeekLegTarget(name: String): String = when (name) {
        "Agachamento no Smith" -> "2 x 8"
        "Leg press" -> "2 x 10"
        "Mesa flexora" -> "2 x 12"
        "Panturrilha" -> "2 x 15"
        "Abdominal na polia" -> "2 x 12"
        else -> "2 x 10"
    }

    private fun recoveryTarget(name: String): String = when (name) {
        "Agachamento no Smith" -> "2 x 10 leve"
        "Leg press" -> "2 x 12 leve"
        "Mesa flexora" -> "2 x 12 leve"
        "Panturrilha" -> "2 x 15 leve"
        "Abdominal na polia" -> "2 x 12"
        else -> "2 x 10 leve"
    }

    private fun twoSetTarget(target: String): String {
        return target.replace(Regex("^\\s*\\d+\\s*x\\s*"), "2 x ")
    }

    private fun parseDay(value: String): LocalDate? = runCatching { LocalDate.parse(value) }.getOrNull()
}
