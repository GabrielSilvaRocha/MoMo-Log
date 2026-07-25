package br.com.mo2log.mobile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Mo2ExerciseCatalogUiRulesTest {
    @Test
    fun pernasIncludesLowerBodyGroupsAndExcludesUpperBody() {
        assertTrue(Mo2ExerciseCatalogUiRules.matchesFilter("Pernas", "Quadriceps", false, false))
        assertTrue(Mo2ExerciseCatalogUiRules.matchesFilter("Pernas", "Gluteos", false, false))
        assertTrue(Mo2ExerciseCatalogUiRules.matchesFilter("Pernas", "Panturrilhas", false, false))
        assertFalse(Mo2ExerciseCatalogUiRules.matchesFilter("Pernas", "Peito", false, false))
    }

    @Test
    fun favoritesNeverShowsHiddenExercises() {
        assertTrue(Mo2ExerciseCatalogUiRules.matchesFilter("Favoritos", "Peito", true, false))
        assertFalse(Mo2ExerciseCatalogUiRules.matchesFilter("Favoritos", "Peito", true, true))
        assertFalse(Mo2ExerciseCatalogUiRules.matchesFilter("Favoritos", "Peito", false, false))
    }

    @Test
    fun hiddenFilterOnlyShowsHiddenExercises() {
        assertTrue(Mo2ExerciseCatalogUiRules.matchesFilter("Ocultos", "Costas", false, true))
        assertFalse(Mo2ExerciseCatalogUiRules.matchesFilter("Ocultos", "Costas", false, false))
    }

    @Test
    fun executionPresentationAlwaysHasThreeSteps() {
        val steps = Mo2ExerciseCatalogUiRules.executionSteps(
            name = "Agachamento no Smith",
            equipment = "Smith",
            movement = "Agachar",
            description = "Foco principal: quadriceps. Padrao de movimento: Agachar.",
        )

        assertEquals(3, steps.size)
        assertTrue(steps.all(String::isNotBlank))
        assertTrue(steps.first().contains("barra", ignoreCase = true))
        assertTrue(steps[1].contains("Desca", ignoreCase = true))
    }

    @Test
    fun tagsAreCompactDistinctAndIgnoreEmptyValues() {
        val tags = Mo2ExerciseCatalogUiRules.displayTags(
            muscle = "Quadriceps",
            equipment = "Smith",
            type = "Composto",
            level = "",
        )

        assertEquals(listOf("Quadriceps", "Smith", "Composto"), tags)
    }

    @Test
    fun equipmentLabelUsesExerciseNameBeforeGenericFamily() {
        assertEquals(
            "Smith",
            Mo2ExerciseCatalogUiRules.equipmentLabel(
                "Agachamento no Smith",
                "Barra/Halter/Smith/Landmine",
            ),
        )
        assertEquals(
            "Halter",
            Mo2ExerciseCatalogUiRules.equipmentLabel(
                "Agachamento goblet",
                "Barra/Halter/Smith/Landmine",
            ),
        )
        assertEquals(
            "Maquina",
            Mo2ExerciseCatalogUiRules.equipmentLabel(
                "Chest press horizontal",
                "Barra/Halteres/Maquina/Smith",
            ),
        )
    }
}
