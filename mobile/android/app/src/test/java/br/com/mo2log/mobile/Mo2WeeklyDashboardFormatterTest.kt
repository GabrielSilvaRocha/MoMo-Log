package br.com.mo2log.mobile

import br.com.mo2log.mobile.ui.Mo2WeeklyDashboardFormatter
import org.junit.Assert.assertEquals
import org.junit.Test

class Mo2WeeklyDashboardFormatterTest {
    @Test
    fun percentClampsAndHandlesMissingTarget() {
        assertEquals(0, Mo2WeeklyDashboardFormatter.percent(1, 0))
        assertEquals(33, Mo2WeeklyDashboardFormatter.percent(1, 3))
        assertEquals(100, Mo2WeeklyDashboardFormatter.percent(4, 3))
    }

    @Test
    fun formatsDistanceForPortugueseLocale() {
        assertEquals("5,25 km", Mo2WeeklyDashboardFormatter.distance(5.25))
    }

    @Test
    fun combinesHoursAndMinutesWithoutExposingSeconds() {
        assertEquals("0h 00min", Mo2WeeklyDashboardFormatter.activityTime(59L))
        assertEquals("1h 31min", Mo2WeeklyDashboardFormatter.activityTime(5_499L))
    }
}
