package br.com.mo2log.mobile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Mo2WorkoutSwapEngineTest {
    @Test
    fun resolvesOnceAndExcludesPlannedAndUnavailableOptions() {
        val current = exercise(
            id = "bench-bar",
            name = "Supino reto com barra",
            equipment = "Barra",
        )
        val dumbbell = exercise(
            id = "bench-dumbbell",
            name = "Supino reto com halteres",
            equipment = "Halteres",
        )
        val machine = exercise(
            id = "bench-machine",
            name = "Supino reto na maquina",
            equipment = "Maquina",
        )
        val cable = exercise(
            id = "bench-cable",
            name = "Supino reto na polia",
            equipment = "Cabos",
        )

        val result = Mo2WorkoutSwapEngine.resolve(
            catalog = listOf(current, dumbbell, machine, cable),
            request = request(
                currentAliases = listOf("Supino reto com barra"),
                plannedAliasGroups = listOf(listOf("Supino reto na maquina")),
                unavailableEquipmentKeys = setOf("cabos"),
                manualAlternativesByExerciseId = mapOf(
                    current.id to listOf(dumbbell.id, machine.id, cable.id),
                ),
                preferredAlternativeByExerciseId = mapOf(current.id to dumbbell.id),
            ),
        )

        assertEquals(current.id, result?.current?.id)
        assertEquals(listOf(dumbbell.id), result?.baseOptions?.map(CatalogExercise::id))
        assertEquals(dumbbell.id, result?.preferredId)
    }

    @Test
    fun automaticRankingDoesNotSuggestAnExerciseAlreadyPlannedByAlias() {
        val current = exercise(
            id = "bench-bar",
            name = "Supino reto com barra",
            alternatives = "supino reto com halteres; supino reto na maquina",
        )
        val dumbbell = exercise(
            id = "bench-dumbbell",
            name = "Supino reto com halteres",
        )
        val machine = exercise(
            id = "bench-machine",
            name = "Supino reto na maquina",
        )

        val result = Mo2WorkoutSwapEngine.resolve(
            catalog = listOf(current, dumbbell, machine),
            request = request(
                currentAliases = listOf("Supino reto com barra"),
                plannedAliasGroups = listOf(
                    listOf("Supino maquina", "Supino reto na maquina"),
                ),
            ),
        )

        assertEquals(listOf(dumbbell.id), result?.baseOptions?.map(CatalogExercise::id))
    }

    @Test
    fun stablePlanIdsDoNotConfuseInclinedAndFlatDumbbellBench() {
        val barbellFlat = exercise(
            id = "EX0222",
            name = "Supino reto com barra",
            alternatives = "supino com halteres; chest press",
        )
        val dumbbellFlat = exercise(id = "EX0223", name = "Supino reto com halteres")
        val dumbbellIncline = exercise(id = "EX0233", name = "Supino inclinado com halteres")

        val result = Mo2WorkoutSwapEngine.resolve(
            catalog = listOf(barbellFlat, dumbbellFlat, dumbbellIncline),
            request = request(
                currentAliases = listOf(barbellFlat.name),
                plannedAliasGroups = listOf(listOf("Supino com halteres")),
                currentExerciseId = barbellFlat.id,
                plannedExerciseIds = setOf(barbellFlat.id, dumbbellIncline.id),
            ),
        )

        assertEquals(listOf(dumbbellFlat.id), result?.baseOptions?.map(CatalogExercise::id))
    }

    @Test
    fun changingReasonReusesBaseOptionsAndKeepsPreferredFirst() {
        val current = exercise(
            id = "bench-bar",
            name = "Supino reto com barra",
            level = "intermediario",
        )
        val advanced = exercise(
            id = "bench-advanced",
            name = "Supino reto avancado",
            level = "avancado",
        )
        val sameLevel = exercise(
            id = "bench-same-level",
            name = "Supino reto com halteres",
            level = "intermediario",
        )
        val result = Mo2WorkoutSwapEngine.resolve(
            catalog = listOf(current, advanced, sameLevel),
            request = request(
                currentAliases = listOf(current.name),
                manualAlternativesByExerciseId = mapOf(
                    current.id to listOf(advanced.id, sameLevel.id),
                ),
                preferredAlternativeByExerciseId = mapOf(current.id to advanced.id),
            ),
        )!!

        val options = Mo2WorkoutSwapEngine.optionsForReason(result, "same_level")

        assertEquals(listOf(advanced.id, sameLevel.id), options.map(CatalogExercise::id))
        assertEquals(2, result.baseOptions.size)
    }

    @Test
    fun hiddenCurrentExerciseReturnsNoResult() {
        val current = exercise(id = "bench-bar", name = "Supino reto com barra")

        val result = Mo2WorkoutSwapEngine.resolve(
            catalog = listOf(current),
            request = request(
                currentAliases = listOf(current.name),
                hiddenIds = setOf(current.id),
            ),
        )

        assertNull(result)
    }

    @Test
    fun resultLimitIsAppliedAfterAllExclusions() {
        val current = exercise(id = "bench-bar", name = "Supino reto com barra")
        val alternatives = (1..12).map { index ->
            exercise(
                id = "bench-$index",
                name = "Supino reto variante $index",
            )
        }
        val result = Mo2WorkoutSwapEngine.resolve(
            catalog = listOf(current) + alternatives,
            request = request(
                currentAliases = listOf(current.name),
                manualAlternativesByExerciseId = mapOf(
                    current.id to alternatives.map(CatalogExercise::id),
                ),
                limit = 8,
            ),
        )!!

        assertEquals(8, result.baseOptions.size)
        assertTrue(result.baseOptions.none { it.id == current.id })
    }

    private fun request(
        currentAliases: List<String>,
        plannedAliasGroups: List<List<String>> = emptyList(),
        hiddenIds: Set<String> = emptySet(),
        unavailableEquipmentKeys: Set<String> = emptySet(),
        manualAlternativesByExerciseId: Map<String, List<String>> = emptyMap(),
        preferredAlternativeByExerciseId: Map<String, String> = emptyMap(),
        currentExerciseId: String? = null,
        plannedExerciseIds: Set<String> = emptySet(),
        limit: Int = 8,
    ): Mo2WorkoutSwapRequest {
        return Mo2WorkoutSwapRequest(
            currentAliases = currentAliases,
            plannedAliasGroups = plannedAliasGroups,
            reason = "occupied",
            currentExerciseId = currentExerciseId,
            plannedExerciseIds = plannedExerciseIds,
            hiddenIds = hiddenIds,
            unavailableEquipmentKeys = unavailableEquipmentKeys,
            manualAlternativesByExerciseId = manualAlternativesByExerciseId,
            preferredAlternativeByExerciseId = preferredAlternativeByExerciseId,
            limit = limit,
        )
    }

    private fun exercise(
        id: String,
        name: String,
        equipment: String = "Barra",
        level: String = "intermediario",
        alternatives: String = "",
    ): CatalogExercise {
        return CatalogExercise(
            id = id,
            name = name,
            slug = name,
            muscle = "Peito",
            subgroup = "Peitoral geral",
            movement = "Empurrar horizontal",
            type = "composto",
            level = level,
            primary = "peitoral maior",
            secondary = "triceps",
            equipment = equipment,
            alternatives = alternatives,
            description = "",
            technicalCare = "",
            source = "test",
            sourceId = id,
            status = "active",
            review = "",
            links = listOf("https://example.com/$id.gif"),
        )
    }
}
