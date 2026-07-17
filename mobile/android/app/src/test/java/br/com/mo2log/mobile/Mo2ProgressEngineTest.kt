package br.com.mo2log.mobile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Mo2ProgressEngineTest {
    @Test
    fun trendCalculatesIncrease() {
        val result = Mo2ProgressEngine.trend(current = 120.0, previous = 100.0)
        assertEquals(20, result.percent)
        assertEquals("subiu", result.direction)
    }

    @Test
    fun trendHandlesNewBaseline() {
        val result = Mo2ProgressEngine.trend(current = 20.0, previous = 0.0)
        assertNull(result.percent)
        assertEquals("novo", result.direction)
    }

    @Test
    fun consistencyScoreIsBounded() {
        assertEquals(100, Mo2ProgressEngine.consistencyScore(8, 5, 9, 5))
        assertEquals(0, Mo2ProgressEngine.consistencyScore(0, 5, 0, 5))
    }

    @Test
    fun dataHealthWarnsAboutOldBackup() {
        val result = Mo2ProgressEngine.dataHealth(invalidCollections = 0, backupAgeDays = 12, recordCount = 40)
        assertEquals("Atencao", result.status)
        assertTrue(result.issues.any { it.contains("7 dias") })
    }

    @Test
    fun dataHealthAcceptsCleanData() {
        val result = Mo2ProgressEngine.dataHealth(invalidCollections = 0, backupAgeDays = 1, recordCount = 40)
        assertEquals("Integro", result.status)
        assertTrue(result.issues.isEmpty())
    }

    @Test
    fun volumeBalanceFindsConcentration() {
        val message = Mo2ProgressEngine.volumeBalance(mapOf("Peito" to 6000.0, "Costas" to 1500.0))
        assertTrue(message.contains("Peito"))
        assertTrue(message.contains("Costas"))
    }
}
