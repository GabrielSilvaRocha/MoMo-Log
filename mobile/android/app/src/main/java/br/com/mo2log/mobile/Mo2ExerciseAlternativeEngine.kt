package br.com.mo2log.mobile

import java.text.Normalizer
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

data class Mo2ExerciseProfile(
    val id: String,
    val name: String,
    val slug: String,
    val muscle: String,
    val subgroup: String,
    val movement: String,
    val type: String,
    val level: String,
    val primary: String,
    val equipment: String,
    val alternatives: String,
)

data class Mo2RankedExerciseAlternative(
    val id: String,
    val score: Int,
    val rationale: String,
)

object Mo2ExerciseAlternativeEngine {
    private const val MaxManualAlternatives = 12
    private val combiningMarksRegex = "\\p{Mn}+".toRegex()
    private val nonAlphaNumericRegex = "[^a-z0-9]+".toRegex()
    private val normalizedTextCache = ConcurrentHashMap<String, String>()
    private val normalizedProfileCache = ConcurrentHashMap<Mo2ExerciseProfile, NormalizedProfile>()

    private val connectorTokens = setOf(
        "a", "as", "com", "da", "das", "de", "do", "dos", "e", "em",
        "na", "nas", "no", "nos", "ou", "para", "por",
    )
    private val equipmentTokens = setOf(
        "barra", "barras", "banco", "cabo", "cabos", "corporal", "corda",
        "halter", "halteres", "livre", "maquina", "polia", "smith", "trx",
    )

    private data class NormalizedProfile(
        val name: String,
        val muscle: String,
        val subgroup: String,
        val movement: String,
        val type: String,
        val level: String,
        val primaryMuscles: Set<String>,
        val familyTokens: Set<String>,
        val searchableTokens: Set<String>,
        val alternativeTokenSets: List<Set<String>>,
    )

    fun rank(
        current: Mo2ExerciseProfile,
        candidates: List<Mo2ExerciseProfile>,
        excludedIds: Set<String> = emptySet(),
    ): List<Mo2RankedExerciseAlternative> {
        val normalizedCurrent = normalizedProfile(current)
        val ranked = ArrayList<Pair<Mo2RankedExerciseAlternative, String>>(candidates.size)
        candidates.forEach { candidate ->
            if (candidate.id == current.id || candidate.id in excludedIds) return@forEach
            val normalizedCandidate = normalizedProfile(candidate)
            if (normalizedCandidate.muscle != normalizedCurrent.muscle) return@forEach
            val explicitMatch = explicitAlternativeMatch(normalizedCurrent, normalizedCandidate)
            if (!isAutomaticAlternative(normalizedCurrent, normalizedCandidate, explicitMatch)) return@forEach
            ranked += Pair(
                Mo2RankedExerciseAlternative(
                    id = candidate.id,
                    score = score(normalizedCurrent, normalizedCandidate, explicitMatch),
                    rationale = compatibilityLabel(normalizedCurrent, normalizedCandidate),
                ),
                normalizedCandidate.name,
            )
        }
        ranked.sortWith(
            compareByDescending<Pair<Mo2RankedExerciseAlternative, String>> { it.first.score }
                .thenBy { it.second },
        )
        return ranked.map(Pair<Mo2RankedExerciseAlternative, String>::first)
    }

    fun compatibilityLabel(current: Mo2ExerciseProfile, candidate: Mo2ExerciseProfile): String {
        return compatibilityLabel(normalizedProfile(current), normalizedProfile(candidate))
    }

    private fun compatibilityLabel(
        current: NormalizedProfile,
        candidate: NormalizedProfile,
    ): String {
        if (current.muscle != candidate.muscle) {
            return "Outro grupo muscular (selecao manual)"
        }
        val sameMovement = sameValue(current.movement, candidate.movement)
        val sameSubgroup = sameValue(current.subgroup, candidate.subgroup)
        return when {
            sameMovement && sameSubgroup -> "Mesmo padrao e regiao"
            sameMovement -> "Mesmo padrao de movimento"
            sameSubgroup -> "Mesma regiao muscular"
            else -> "Mesmo grupo muscular"
        }
    }

    fun normalizeManualIds(
        currentId: String,
        ids: Iterable<String>,
        validIds: Set<String>,
    ): List<String> {
        return ids.asSequence()
            .map(String::trim)
            .filter(String::isNotBlank)
            .filter { id -> id != currentId && id in validIds }
            .distinct()
            .take(MaxManualAlternatives)
            .toList()
    }

    fun maxManualAlternatives(): Int = MaxManualAlternatives

    private fun score(
        current: NormalizedProfile,
        candidate: NormalizedProfile,
        explicitMatch: Boolean,
    ): Int {
        var score = 10
        if (explicitMatch) score += 110
        if (sameValue(current.movement, candidate.movement)) score += 95
        if (sameValue(current.subgroup, candidate.subgroup)) score += 80
        if (samePrimaryMuscle(current.primaryMuscles, candidate.primaryMuscles)) score += 50
        if (sameValue(current.type, candidate.type)) score += 25
        if (sameValue(current.level, candidate.level)) score += 8

        val sharedFamily = current.familyTokens.intersect(candidate.familyTokens)
        score += sharedFamily.size * 24
        if (current.familyTokens.isNotEmpty() && sharedFamily.containsAll(current.familyTokens)) score += 30
        return score
    }

    private fun isAutomaticAlternative(
        current: NormalizedProfile,
        candidate: NormalizedProfile,
        explicitMatch: Boolean,
    ): Boolean {
        return explicitMatch ||
            sameValue(current.movement, candidate.movement) ||
            sameValue(current.subgroup, candidate.subgroup)
    }

    private fun explicitAlternativeMatch(
        current: NormalizedProfile,
        candidate: NormalizedProfile,
    ): Boolean {
        return current.alternativeTokenSets.any { wanted ->
            candidate.searchableTokens.containsAll(wanted)
        }
    }

    private fun samePrimaryMuscle(first: Set<String>, second: Set<String>): Boolean {
        return first.isNotEmpty() && first.intersect(second).isNotEmpty()
    }

    private fun sameValue(first: String, second: String): Boolean {
        return first.isNotBlank() && first == second
    }

    private fun normalizedProfile(profile: Mo2ExerciseProfile): NormalizedProfile {
        return normalizedProfileCache.getOrPut(profile) {
            NormalizedProfile(
                name = normalize(profile.name),
                muscle = normalize(profile.muscle),
                subgroup = normalize(profile.subgroup),
                movement = normalize(profile.movement),
                type = normalize(profile.type),
                level = normalize(profile.level),
                primaryMuscles = profile.primary.split(';', '/', ',')
                    .map(::normalize)
                    .filter(String::isNotBlank)
                    .toSet(),
                familyTokens = tokenSet(profile.name).filterNot(equipmentTokens::contains).toSet(),
                searchableTokens = tokenSet(profile.name + " " + profile.slug),
                alternativeTokenSets = profile.alternatives.split(';')
                    .map(::tokenSet)
                    .filter(Set<String>::isNotEmpty),
            )
        }
    }

    private fun tokenSet(value: String): Set<String> {
        return normalize(value)
            .split(' ')
            .filter { token -> token.length > 1 && token !in connectorTokens }
            .toSet()
    }

    private fun normalize(value: String): String {
        return normalizedTextCache.getOrPut(value) {
            Normalizer.normalize(value.lowercase(Locale("pt", "BR")), Normalizer.Form.NFD)
                .replace(combiningMarksRegex, "")
                .replace(nonAlphaNumericRegex, " ")
                .trim()
        }
    }
}
