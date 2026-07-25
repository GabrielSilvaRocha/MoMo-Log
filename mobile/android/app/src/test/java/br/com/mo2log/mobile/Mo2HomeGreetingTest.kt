package br.com.mo2log.mobile

import org.junit.Assert.assertEquals
import org.junit.Test

class Mo2HomeGreetingTest {
    @Test
    fun morningStartsAtFiveAndEndsBeforeNoon() {
        assertEquals("Bom dia", Mo2HomeGreeting.forHour(5).salutation)
        assertEquals("Bom dia", Mo2HomeGreeting.forHour(11).salutation)
    }

    @Test
    fun afternoonRunsFromNoonUntilSix() {
        assertEquals("Boa tarde", Mo2HomeGreeting.forHour(12).salutation)
        assertEquals("Boa tarde", Mo2HomeGreeting.forHour(17).salutation)
    }

    @Test
    fun nightCoversEveningAndEarlyMorning() {
        assertEquals("Boa noite", Mo2HomeGreeting.forHour(18).salutation)
        assertEquals("Boa noite", Mo2HomeGreeting.forHour(4).salutation)
    }

    @Test
    fun textUsesPersonalNameAndMatchingEmoji() {
        assertEquals(
            "Bom dia, Gabriel \u2600\uFE0F",
            Mo2HomeGreeting.forHour(8).text("Gabriel"),
        )
        assertEquals(
            "Boa noite, Gabriel \uD83C\uDF19",
            Mo2HomeGreeting.forHour(22).text(""),
        )
    }
}
