package br.com.mo2log.mobile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Mo2FiveKmPlanTest {
    private val runs = Mo2FiveKmPlan.runningWorkouts()

    @Test
    fun cycleUsesTheRequiredRealDatesAndStopsAfterTheRace() {
        assertEquals(
            Mo2FiveKmCyclePosition(Mo2FiveKmCyclePhase.BEFORE_START, 1),
            Mo2FiveKmPlan.cyclePosition("2026-07-26"),
        )
        assertEquals(
            Mo2FiveKmCyclePosition(Mo2FiveKmCyclePhase.WEEK_1, 1),
            Mo2FiveKmPlan.cyclePosition("2026-07-27"),
        )
        assertEquals(
            Mo2FiveKmCyclePosition(Mo2FiveKmCyclePhase.WEEK_2, 2),
            Mo2FiveKmPlan.cyclePosition("2026-08-03"),
        )
        assertEquals(
            Mo2FiveKmCyclePosition(Mo2FiveKmCyclePhase.WEEK_3, 3),
            Mo2FiveKmPlan.cyclePosition("2026-08-10"),
        )
        assertEquals(
            Mo2FiveKmCyclePosition(Mo2FiveKmCyclePhase.RACE_DAY, 3),
            Mo2FiveKmPlan.cyclePosition("2026-08-16"),
        )
        assertEquals(
            Mo2FiveKmCyclePosition(Mo2FiveKmCyclePhase.COMPLETED, null),
            Mo2FiveKmPlan.cyclePosition("2026-08-17"),
        )
    }

    @Test
    fun paceIsAlwaysDerivedFromSpeed() {
        assertEquals(300L, Mo2FiveKmPlan.paceSecondsPerKm(12.0))
        assertEquals(295L, Mo2FiveKmPlan.paceSecondsPerKm(12.2))
        assertNull(Mo2FiveKmPlan.paceSecondsPerKm(0.0))
    }

    @Test
    fun strengthScheduleIsTuesdayWednesdayAndFriday() {
        val strength = Mo2FiveKmPlan.strengthPlans()

        assertEquals(listOf(2, 3, 5), strength.map(WorkoutPlan::dayIndex))
        assertTrue(strength[1].focus.contains("sem corrida"))
        assertTrue(strength[1].exercises.any { it.name == "Stiff" })
        assertTrue(strength[0].exercises.any { it.name == "Abdominal na polia" })
        assertTrue(strength[1].exercises.any { it.name == "Abdominal na polia" })
    }

    @Test
    fun runningScheduleHasFourWeekdaySessionsAndOnlyTheRaceOnSunday() {
        assertEquals(13, runs.size)
        assertEquals(mapOf(1 to 4, 2 to 4, 3 to 5), runs.groupingBy(RunningWorkout::week).eachCount())
        assertFalse(runs.any { it.dayIndex == 3 })
        assertFalse(runs.any { it.dayIndex == 6 })

        val weekend = runs.filter { it.dayIndex >= 6 }
        assertEquals(1, weekend.size)
        assertTrue(weekend.single().isRace)
        assertEquals("2026-08-16", weekend.single().scheduledDate)
    }

    @Test
    fun hybridDaysKeepStrengthBeforeRunning() {
        val strengthDays = Mo2FiveKmPlan.strengthPlans().map(WorkoutPlan::dayIndex).toSet()
        for (week in 1..3) {
            val runDays = runs.filter { it.week == week }.map(RunningWorkout::dayIndex).toSet()
            assertTrue(2 in strengthDays && 2 in runDays)
            assertTrue(5 in strengthDays && 5 in runDays)
            assertTrue(3 in strengthDays && 3 !in runDays)
        }
    }

    @Test
    fun workoutIdsAreUniqueAndCannotCollideWithTheLegacyCycle() {
        val ids = runs.map(RunningWorkout::id)
        val legacyIds = setOf("w1-mon", "w1-tue", "w1-thu", "w1-sat", "w1-sun")

        assertEquals(ids.size, ids.distinct().size)
        assertTrue(ids.all { it.startsWith("5k-2026-08-16-") })
        assertTrue(ids.none { it in legacyIds })
    }

    @Test
    fun everyWorkoutDistanceMatchesThePrescription() {
        val expected = mapOf(
            "5k-2026-08-16-w1-mon" to 5.60,
            "5k-2026-08-16-w1-tue" to 5.00,
            "5k-2026-08-16-w1-thu" to 5.00,
            "5k-2026-08-16-w1-fri" to 7.00,
            "5k-2026-08-16-w2-mon" to 7.80,
            "5k-2026-08-16-w2-tue" to 5.50,
            "5k-2026-08-16-w2-thu" to 6.00,
            "5k-2026-08-16-w2-fri" to 8.00,
            "5k-2026-08-16-w3-mon" to 4.20,
            "5k-2026-08-16-w3-tue" to 4.50,
            "5k-2026-08-16-w3-thu" to 4.20,
            "5k-2026-08-16-w3-fri" to 3.70,
            "5k-2026-08-16-w3-race" to 5.00,
        )

        assertEquals(expected.keys, runs.map(RunningWorkout::id).toSet())
        expected.forEach { (id, distance) ->
            assertEquals(distance, Mo2FiveKmPlan.totalDistance(runs.first { it.id == id }), 0.001)
        }
    }

    @Test
    fun intervalRecoveriesExistOnlyBetweenRepetitions() {
        val weekOne = runs.first { it.id == "5k-2026-08-16-w1-mon" }
        val weekTwo = runs.first { it.id == "5k-2026-08-16-w2-mon" }
        val taper = runs.first { it.id == "5k-2026-08-16-w3-mon" }

        assertEquals(6, weekOne.stages.count { it.title.startsWith("Tiro") })
        assertEquals(5, weekOne.stages.count { it.title.startsWith("Recuperacao") })
        assertEquals(5, weekTwo.stages.count { it.title.startsWith("Tiro") })
        assertEquals(4, weekTwo.stages.count { it.title.startsWith("Recuperacao") })
        assertEquals(4, taper.stages.count { it.title.startsWith("Tiro") })
        assertEquals(3, taper.stages.count { it.title.startsWith("Recuperacao") })
    }

    @Test
    fun raceContainsOnlyTheOfficialFiveKilometers() {
        val race = runs.single { it.isRace }

        assertEquals(4, race.stages.size)
        assertEquals(5.0, Mo2FiveKmPlan.totalDistance(race), 0.001)
        assertFalse(race.stages.any { it.title.contains("Aquecimento", ignoreCase = true) })
    }

    @Test
    fun nextWorkoutUsesTheRealDateAndNeverRestartsAfterTheRace() {
        assertEquals(
            "5k-2026-08-16-w1-mon",
            Mo2FiveKmPlan.nextWorkout(runs, "2026-07-24", emptySet())?.id,
        )
        assertEquals(
            "5k-2026-08-16-w1-tue",
            Mo2FiveKmPlan.nextWorkout(
                runs,
                "2026-07-28",
                setOf("5k-2026-08-16-w1-mon"),
            )?.id,
        )
        assertNull(Mo2FiveKmPlan.nextWorkout(runs, "2026-08-17", emptySet()))
    }

    @Test
    fun raceWeekDeloadDoesNotMutateTheBaseStrengthPlan() {
        val base = Mo2FiveKmPlan.strengthPlans()
        val reducedA = Mo2FiveKmPlan.effectiveStrengthPlan(base[0], "2026-08-11", "green")
        val reducedB = Mo2FiveKmPlan.effectiveStrengthPlan(base[1], "2026-08-12", "green")
        val reducedC = Mo2FiveKmPlan.effectiveStrengthPlan(base[2], "2026-08-14", "green")

        assertTrue(reducedA.exercises.all { it.target.startsWith("2 x") })
        assertTrue(reducedC.exercises.all { it.target.startsWith("2 x") })
        assertFalse(reducedB.exercises.any { it.name == "Stiff" })
        assertEquals("2 x 8", reducedB.exercises.first { it.name == "Agachamento no Smith" }.target)
        assertEquals("2 x 10", reducedB.exercises.first { it.name == "Leg press" }.target)
        assertEquals("2 x 12", reducedB.exercises.first { it.name == "Mesa flexora" }.target)
        assertEquals("2 x 15", reducedB.exercises.first { it.name == "Panturrilha" }.target)
        assertEquals("2 x 12", reducedB.exercises.first { it.name == "Abdominal na polia" }.target)

        assertTrue(base[1].exercises.any { it.name == "Stiff" })
        assertEquals("3 x 6-8", base[1].exercises.first { it.name == "Agachamento no Smith" }.target)
    }

    @Test
    fun redReadinessCreatesARecoveryLegSessionWithoutRunning() {
        val baseLegs = Mo2FiveKmPlan.strengthPlans()[1]
        val recovery = Mo2FiveKmPlan.effectiveStrengthPlan(baseLegs, "2026-08-05", "red")

        assertTrue(recovery.title.contains("Recuperacao"))
        assertTrue(recovery.focus.contains("sem corrida"))
        assertFalse(recovery.exercises.any { it.name == "Stiff" })
        assertTrue(recovery.exercises.all { it.notes.contains("RPE 5-6") })
    }
}
