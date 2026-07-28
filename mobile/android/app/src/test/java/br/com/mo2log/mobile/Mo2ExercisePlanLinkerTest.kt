package br.com.mo2log.mobile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class Mo2ExercisePlanLinkerTest {
    private val catalog = listOf(
        Mo2CatalogExerciseIdentity("bench-press", "Supino reto com barra"),
        Mo2CatalogExerciseIdentity("smith-squat", "Agachamento no Smith"),
    )

    @Test
    fun stableIdKeepsPlanConnectedAfterCatalogRename() {
        val resolved = Mo2ExercisePlanLinker.resolve(
            catalogExerciseId = "bench-press",
            legacyName = "Nome antigo",
            catalog = catalog,
        )

        assertEquals("Supino reto com barra", resolved?.name)
    }

    @Test
    fun legacyPlanIsMigratedByNormalizedName() {
        val resolved = Mo2ExercisePlanLinker.resolve(
            catalogExerciseId = null,
            legacyName = "Agachamento no smith",
            catalog = catalog,
        )

        assertEquals("smith-squat", resolved?.id)
    }

    @Test
    fun unknownLegacyExerciseIsPreservedAsUnlinked() {
        assertNull(Mo2ExercisePlanLinker.resolve(null, "Exercicio pessoal", catalog))
    }
}
