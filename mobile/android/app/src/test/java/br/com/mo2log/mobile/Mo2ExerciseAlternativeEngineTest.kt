package br.com.mo2log.mobile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Mo2ExerciseAlternativeEngineTest {
    @Test
    fun dumbbellBenchIsPrioritizedForBarbellBench() {
        val ranked = Mo2ExerciseAlternativeEngine.rank(
            current = profile(
                id = "barbell-flat",
                name = "Supino reto com barra",
                subgroup = "Peitoral geral",
                movement = "Empurrar horizontal",
                type = "composto",
                equipment = "Barra",
                alternatives = "supino com halteres; chest press; crossover; peck deck",
            ),
            candidates = listOf(
                profile(
                    id = "fly",
                    name = "Crossover horizontal",
                    subgroup = "Isolamento/aducao horizontal",
                    movement = "Aducao horizontal",
                    type = "isolador",
                    equipment = "Cabos",
                ),
                profile(
                    id = "dumbbell-flat",
                    name = "Supino reto com halteres",
                    subgroup = "Peitoral geral",
                    movement = "Empurrar horizontal",
                    type = "composto",
                    equipment = "Halteres",
                ),
                profile(
                    id = "machine-flat",
                    name = "Chest press horizontal",
                    subgroup = "Peitoral geral",
                    movement = "Empurrar horizontal",
                    type = "composto",
                    equipment = "Maquina",
                ),
            ),
        )

        assertEquals("dumbbell-flat", ranked.first().id)
        assertTrue(ranked.first().score > ranked.last().score)
    }

    @Test
    fun wordsInsertedInExerciseNameStillMatchExplicitAlternative() {
        val current = profile(
            id = "barbell-flat",
            name = "Supino reto com barra",
            alternatives = "supino com halteres",
        )
        val dumbbell = profile(id = "dumbbell-flat", name = "Supino reto com halteres")

        val ranked = Mo2ExerciseAlternativeEngine.rank(current, listOf(dumbbell))

        assertEquals(listOf("dumbbell-flat"), ranked.map { it.id })
        assertTrue(ranked.single().score >= 100)
    }

    @Test
    fun aTextualMatchFromAnotherMuscleIsNeverRecommendedAutomatically() {
        val chest = profile(
            id = "bench",
            name = "Supino reto com barra",
            muscle = "Peito",
            alternatives = "peck deck",
        )
        val reverseFly = profile(
            id = "reverse-fly",
            name = "Crucifixo inverso no peck deck",
            muscle = "Ombros",
            primary = "deltoide posterior",
        )

        assertTrue(Mo2ExerciseAlternativeEngine.rank(chest, listOf(reverseFly)).isEmpty())
    }

    @Test
    fun sameMuscleWithoutEquivalentPatternOrRegionIsNotRecommendedAutomatically() {
        val wristExtension = profile(
            id = "wrist-extension",
            name = "Extensao de punho com barra",
            muscle = "Antebraco/Pegada",
            subgroup = "Extensores do punho",
            movement = "Extensao de punho",
            primary = "antebracos",
        )
        val platePinch = profile(
            id = "plate-pinch",
            name = "Pinca com anilhas",
            muscle = "Antebraco/Pegada",
            subgroup = "Pegada isometrica",
            movement = "Preensao isometrica",
            primary = "antebracos",
        )

        assertTrue(Mo2ExerciseAlternativeEngine.rank(wristExtension, listOf(platePinch)).isEmpty())
    }

    @Test
    fun exercisesAlreadyPlannedAreExcludedBeforeRanking() {
        val current = profile(id = "bench", name = "Supino reto com barra")
        val dumbbell = profile(id = "dumbbell", name = "Supino reto com halteres")
        val machine = profile(id = "machine", name = "Supino reto na maquina")

        val ranked = Mo2ExerciseAlternativeEngine.rank(
            current,
            listOf(dumbbell, machine),
            excludedIds = setOf("dumbbell"),
        )

        assertFalse(ranked.any { it.id == "dumbbell" })
        assertEquals(listOf("machine"), ranked.map { it.id })
    }

    @Test
    fun manualAlternativesAreValidatedDeduplicatedAndLimited() {
        val ids = (1..20).map { "exercise-$it" }
        val normalized = Mo2ExerciseAlternativeEngine.normalizeManualIds(
            currentId = "exercise-1",
            ids = listOf("exercise-2", "exercise-2", "invalid") + ids,
            validIds = ids.toSet(),
        )

        assertFalse(normalized.contains("exercise-1"))
        assertFalse(normalized.contains("invalid"))
        assertEquals(normalized.distinct(), normalized)
        assertEquals(Mo2ExerciseAlternativeEngine.maxManualAlternatives(), normalized.size)
    }

    private fun profile(
        id: String,
        name: String,
        muscle: String = "Peito",
        subgroup: String = "Peitoral geral",
        movement: String = "Empurrar horizontal",
        type: String = "composto",
        primary: String = "peitoral maior",
        equipment: String = "",
        alternatives: String = "",
    ): Mo2ExerciseProfile {
        return Mo2ExerciseProfile(
            id = id,
            name = name,
            slug = name,
            muscle = muscle,
            subgroup = subgroup,
            movement = movement,
            type = type,
            level = "iniciante a intermediario",
            primary = primary,
            equipment = equipment,
            alternatives = alternatives,
        )
    }
}
