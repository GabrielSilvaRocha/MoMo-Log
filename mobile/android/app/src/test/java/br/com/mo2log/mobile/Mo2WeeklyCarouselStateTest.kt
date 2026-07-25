package br.com.mo2log.mobile

import br.com.mo2log.mobile.ui.Mo2WeeklyCarouselState
import org.junit.Assert.assertEquals
import org.junit.Test

class Mo2WeeklyCarouselStateTest {
    @Test
    fun wrapsAtBothEndsOfTheWeek() {
        assertEquals(0, Mo2WeeklyCarouselState.next(6, 7))
        assertEquals(6, Mo2WeeklyCarouselState.previous(0, 7))
    }

    @Test
    fun convertsIsoWeekDayToZeroBasedSlide() {
        assertEquals(0, Mo2WeeklyCarouselState.fromIsoDay(1))
        assertEquals(6, Mo2WeeklyCarouselState.fromIsoDay(7))
    }

    @Test
    fun handlesInvalidOrEmptyCollections() {
        assertEquals(0, Mo2WeeklyCarouselState.normalized(9, 0))
        assertEquals(1, Mo2WeeklyCarouselState.normalized(8, 7))
        assertEquals(6, Mo2WeeklyCarouselState.normalized(-1, 7))
    }

    @Test
    fun alternatesActivitiesInsideTheSameDay() {
        assertEquals(5000L, Mo2WeeklyCarouselState.ACTIVITY_SWITCH_MILLIS)
        assertEquals(1, Mo2WeeklyCarouselState.nextActivity(0, 2))
        assertEquals(0, Mo2WeeklyCarouselState.nextActivity(1, 2))
        assertEquals(0, Mo2WeeklyCarouselState.nextActivity(0, 1))
    }

    @Test
    fun opensTodayOrTheNextActuallyPlannedDay() {
        val scheduledDays = listOf(1, 2, 4, 6, 7)

        assertEquals(1, Mo2WeeklyCarouselState.initialIndexForDay(2, scheduledDays))
        assertEquals(2, Mo2WeeklyCarouselState.initialIndexForDay(3, scheduledDays))
        assertEquals(4, Mo2WeeklyCarouselState.initialIndexForDay(7, scheduledDays))
    }

    @Test
    fun staysOnLastWorkoutAfterTheWeeklyScheduleEnds() {
        assertEquals(2, Mo2WeeklyCarouselState.initialIndexForDay(7, listOf(2, 4, 6)))
        assertEquals(0, Mo2WeeklyCarouselState.initialIndexForDay(4, emptyList()))
    }
}
