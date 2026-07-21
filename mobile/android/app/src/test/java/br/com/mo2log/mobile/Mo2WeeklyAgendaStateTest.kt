package br.com.mo2log.mobile

import br.com.mo2log.mobile.ui.Mo2WeeklyAgendaState
import br.com.mo2log.mobile.ui.Mo2WeeklyAgendaStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class Mo2WeeklyAgendaStateTest {
    @Test
    fun hybridDayTracksPartialAndCompletedStates() {
        assertEquals(
            Mo2WeeklyAgendaStatus.Partial,
            Mo2WeeklyAgendaState.status(
                hasStrength = true,
                strengthCompleted = true,
                hasRunning = true,
                runningCompleted = false,
            ),
        )
        assertEquals(
            Mo2WeeklyAgendaStatus.Completed,
            Mo2WeeklyAgendaState.status(
                hasStrength = true,
                strengthCompleted = true,
                hasRunning = true,
                runningCompleted = true,
            ),
        )
    }

    @Test
    fun dayWithoutTrainingIsRecovery() {
        assertEquals(
            Mo2WeeklyAgendaStatus.Recovery,
            Mo2WeeklyAgendaState.status(
                hasStrength = false,
                strengthCompleted = false,
                hasRunning = false,
                runningCompleted = false,
            ),
        )
    }

    @Test
    fun progressAndIndexStayWithinBounds() {
        assertEquals(62, Mo2WeeklyAgendaState.progressPercent(5, 8))
        assertEquals(100, Mo2WeeklyAgendaState.progressPercent(10, 8))
        assertEquals(6, Mo2WeeklyAgendaState.normalized(-1, 7))
    }
}
