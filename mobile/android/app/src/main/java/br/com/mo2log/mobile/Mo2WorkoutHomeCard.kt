package br.com.mo2log.mobile

object Mo2WorkoutHomeCard {
    const val Push = "push"
    const val Legs = "legs"
    const val Pull = "pull"

    val keys: List<String> = listOf(Push, Legs, Pull)

    fun defaultForPlanIndex(planIndex: Int): String {
        return keys[Math.floorMod(planIndex, keys.size)]
    }

    fun normalize(key: String?, planIndex: Int): String {
        return key?.takeIf(keys::contains) ?: defaultForPlanIndex(planIndex)
    }
}
