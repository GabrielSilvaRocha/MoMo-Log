package br.com.mo2log.mobile

import org.junit.Assert.assertEquals
import org.junit.Test

class Mo2WorkoutHomeCardTest {
    @Test
    fun existingPlansReceiveStableDefaultCards() {
        assertEquals(Mo2WorkoutHomeCard.Push, Mo2WorkoutHomeCard.defaultForPlanIndex(0))
        assertEquals(Mo2WorkoutHomeCard.Legs, Mo2WorkoutHomeCard.defaultForPlanIndex(1))
        assertEquals(Mo2WorkoutHomeCard.Pull, Mo2WorkoutHomeCard.defaultForPlanIndex(2))
        assertEquals(Mo2WorkoutHomeCard.Push, Mo2WorkoutHomeCard.defaultForPlanIndex(3))
    }

    @Test
    fun invalidOrMissingCardFallsBackWithoutLosingValidChoice() {
        assertEquals(Mo2WorkoutHomeCard.Pull, Mo2WorkoutHomeCard.normalize(null, 2))
        assertEquals(Mo2WorkoutHomeCard.Legs, Mo2WorkoutHomeCard.normalize("unknown", 1))
        assertEquals(Mo2WorkoutHomeCard.Push, Mo2WorkoutHomeCard.normalize(Mo2WorkoutHomeCard.Push, 2))
    }
}
