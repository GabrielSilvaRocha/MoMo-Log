package br.com.mo2log.mobile

import java.text.Normalizer
import java.util.Locale
import kotlin.math.roundToInt

data class Mo2MediaLinksValidation(
    val links: List<String>,
    val invalidLinks: List<String>,
) {
    val isValid: Boolean
        get() = invalidLinks.isEmpty()
}

object Mo2PersonalPlanRules {
    private val bundledMediaLink = Regex(
        "^asset://exercise_media/[a-z0-9_-]+\\.(gif|jpe?g|png|webp)$",
        RegexOption.IGNORE_CASE,
    )
    private val legacyDays = listOf(2, 4, 6)
    private val namedDays = listOf(
        "segunda" to 1,
        "terca" to 2,
        "quarta" to 3,
        "quinta" to 4,
        "sexta" to 5,
        "sabado" to 6,
        "domingo" to 7,
    )

    fun resolveWorkoutDay(configuredDay: Int, legacyFocus: String, planIndex: Int): Int {
        if (configuredDay in 1..7) return configuredDay
        val normalizedFocus = normalize(legacyFocus)
        val named = namedDays.firstOrNull { (name, _) -> normalizedFocus.contains(name) }?.second
        return named ?: legacyDays.getOrElse(planIndex) { ((planIndex * 2 + 1) % 7) + 1 }
    }

    fun validateMediaLinks(raw: String): Mo2MediaLinksValidation {
        val links = raw.lines()
            .flatMap { line -> line.split(';') }
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinct()
        return Mo2MediaLinksValidation(
            links = links,
            invalidLinks = links.filterNot { link ->
                link.startsWith("https://", ignoreCase = true) || bundledMediaLink.matches(link)
            },
        )
    }

    fun canonicalWorkoutExerciseName(name: String): String {
        return when (normalize(name)) {
            "agachamento guiado" -> "Agachamento no Smith"
            else -> name
        }
    }

    fun baseRunningDistance(effectiveDistance: Double, distanceScale: Double): Double {
        val scale = distanceScale.coerceIn(0.60, 1.40)
        return roundHundredths(effectiveDistance / scale)
    }

    fun baseRunningSpeed(effectiveSpeed: Double, speedOffset: Double): Double {
        return roundTenths((effectiveSpeed - speedOffset).coerceIn(1.0, 30.0))
    }

    private fun normalize(text: String): String {
        return Normalizer.normalize(text.lowercase(Locale("pt", "BR")), Normalizer.Form.NFD)
            .replace("\\p{Mn}+".toRegex(), "")
            .replace("[^a-z0-9]+".toRegex(), " ")
            .trim()
    }

    private fun roundHundredths(value: Double): Double = (value * 100.0).roundToInt().toDouble() / 100.0

    private fun roundTenths(value: Double): Double = (value * 10.0).roundToInt().toDouble() / 10.0
}
