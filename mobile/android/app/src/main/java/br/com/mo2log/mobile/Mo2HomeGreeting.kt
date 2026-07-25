package br.com.mo2log.mobile

data class Mo2HomeGreeting(
    val salutation: String,
    val emoji: String,
) {
    fun text(name: String): String {
        val safeName = name.trim().ifBlank { "Gabriel" }
        return "$salutation, $safeName $emoji"
    }

    companion object {
        fun forHour(hourOfDay: Int): Mo2HomeGreeting {
            return when (hourOfDay.coerceIn(0, 23)) {
                in 5..11 -> Mo2HomeGreeting("Bom dia", "\u2600\uFE0F")
                in 12..17 -> Mo2HomeGreeting("Boa tarde", "\uD83C\uDF24\uFE0F")
                else -> Mo2HomeGreeting("Boa noite", "\uD83C\uDF19")
            }
        }
    }
}
