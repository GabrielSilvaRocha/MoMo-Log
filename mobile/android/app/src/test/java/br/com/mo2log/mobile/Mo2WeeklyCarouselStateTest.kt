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
}
