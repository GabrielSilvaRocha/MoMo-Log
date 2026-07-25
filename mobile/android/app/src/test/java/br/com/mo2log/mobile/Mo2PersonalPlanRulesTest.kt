package br.com.mo2log.mobile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Mo2PersonalPlanRulesTest {
    @Test
    fun configuredWorkoutDayWinsOverLegacyText() {
        assertEquals(5, Mo2PersonalPlanRules.resolveWorkoutDay(5, "Terca - Peito", 0))
    }

    @Test
    fun legacyWorkoutDayIsRecoveredFromPortugueseFocus() {
        assertEquals(2, Mo2PersonalPlanRules.resolveWorkoutDay(0, "Terca - Peito", 0))
        assertEquals(6, Mo2PersonalPlanRules.resolveWorkoutDay(0, "Sabado - Costas", 0))
    }

    @Test
    fun legacyWorkoutDayFallsBackToOriginalSchedule() {
        assertEquals(2, Mo2PersonalPlanRules.resolveWorkoutDay(0, "Sem dia", 0))
        assertEquals(4, Mo2PersonalPlanRules.resolveWorkoutDay(0, "Sem dia", 1))
        assertEquals(6, Mo2PersonalPlanRules.resolveWorkoutDay(0, "Sem dia", 2))
    }

    @Test
    fun mediaLinksAcceptHttpsDeduplicateAndPreserveOrder() {
        val result = Mo2PersonalPlanRules.validateMediaLinks(
            "https://cdn.test/a.gif\nhttps://cdn.test/b.jpg; https://cdn.test/a.gif",
        )

        assertTrue(result.isValid)
        assertEquals(listOf("https://cdn.test/a.gif", "https://cdn.test/b.jpg"), result.links)
    }

    @Test
    fun mediaLinksRejectClearTextUrls() {
        val result = Mo2PersonalPlanRules.validateMediaLinks("http://cdn.test/a.gif")

        assertFalse(result.isValid)
        assertEquals(listOf("http://cdn.test/a.gif"), result.invalidLinks)
    }

    @Test
    fun mediaLinksAcceptBundledExerciseMedia() {
        val result = Mo2PersonalPlanRules.validateMediaLinks(
            "asset://exercise_media/agachamento_no_smith.gif",
        )

        assertTrue(result.isValid)
        assertEquals(
            listOf("asset://exercise_media/agachamento_no_smith.gif"),
            result.links,
        )
    }

    @Test
    fun guidedSquatIsMigratedToTheUnambiguousSmithName() {
        assertEquals(
            "Agachamento no Smith",
            Mo2PersonalPlanRules.canonicalWorkoutExerciseName("Agachamento guiado"),
        )
        assertEquals(
            "Agachamento livre",
            Mo2PersonalPlanRules.canonicalWorkoutExerciseName("Agachamento livre"),
        )
    }

    @Test
    fun effectiveRunningValuesAreConvertedBackToBasePlan() {
        assertEquals(2.0, Mo2PersonalPlanRules.baseRunningDistance(1.8, 0.9), 0.001)
        assertEquals(8.0, Mo2PersonalPlanRules.baseRunningSpeed(8.3, 0.3), 0.001)
    }
}
