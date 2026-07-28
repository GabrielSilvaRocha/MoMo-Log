package br.com.mo2log.mobile

import java.text.Normalizer
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

data class Mo2WorkoutSwapRequest(
    val currentAliases: List<String>,
    val plannedAliasGroups: List<List<String>>,
    val reason: String,
    val currentExerciseId: String? = null,
    val plannedExerciseIds: Set<String> = emptySet(),
    val hiddenIds: Set<String> = emptySet(),
    val unavailableEquipmentKeys: Set<String> = emptySet(),
    val manualAlternativesByExerciseId: Map<String, List<String>> = emptyMap(),
    val preferredAlternativeByExerciseId: Map<String, String> = emptyMap(),
    val limit: Int = 8,
)

data class Mo2WorkoutSwapResult(
    val current: CatalogExercise,
    val baseOptions: List<CatalogExercise>,
    val preferredId: String?,
)

object Mo2WorkoutSwapEngine {
    private val combiningMarksRegex = "\\p{Mn}+".toRegex()
    private val nonAlphaNumericRegex = "[^a-z0-9]+".toRegex()
    private val normalizedTextCache = ConcurrentHashMap<String, String>()
    private val ignoredMatchTokens = setOf("com", "para", "sem")

    private data class IndexedExercise(
        val exercise: CatalogExercise,
        val normalizedName: String,
        val normalizedHaystack: String,
    )

    fun resolve(
        catalog: List<CatalogExercise>,
        request: Mo2WorkoutSwapRequest,
    ): Mo2WorkoutSwapResult? {
        val visible = catalog
            .asSequence()
            .filterNot { exercise -> exercise.id in request.hiddenIds }
            .map(::index)
            .toList()
        if (visible.isEmpty()) return null

        val visibleById = visible.associateBy { indexed -> indexed.exercise.id }
        val current = request.currentExerciseId
            ?.let(visibleById::get)
            ?.exercise
            ?: bestMatch(request.currentAliases, visible)
            ?: return null
        val stablePlannedIds = request.plannedExerciseIds.filterTo(mutableSetOf()) { it in visibleById }
        val plannedIds = stablePlannedIds.ifEmpty {
            request.plannedAliasGroups
                .mapNotNull { aliases -> bestMatch(aliases, visible)?.id }
                .toSet()
        }
        val plannedNames = request.plannedAliasGroups
            .asSequence()
            .flatten()
            .map(::normalize)
            .filter(String::isNotBlank)
            .toSet()
        val byId = visibleById.mapValues { (_, indexed) -> indexed.exercise }
        val manualIds = request.manualAlternativesByExerciseId[current.id]
        val ranked = if (manualIds != null) {
            manualIds.mapNotNull(byId::get)
        } else {
            val profiles = visible.map { indexed -> indexed.exercise.toAlternativeProfile() }
            Mo2ExerciseAlternativeEngine.rank(
                current = current.toAlternativeProfile(),
                candidates = profiles,
                excludedIds = plannedIds,
            ).mapNotNull { match -> byId[match.id] }
        }
        val unavailableKeys = request.unavailableEquipmentKeys.map(::normalize).toSet()
        val limit = request.limit.coerceAtLeast(1)
        val baseOptions = ranked.asSequence()
            .filter { candidate -> candidate.id != current.id }
            .filterNot { candidate -> candidate.id in plannedIds }
            .filterNot { candidate -> normalize(candidate.name) in plannedNames }
            .filterNot { candidate ->
                val equipmentKey = normalize(candidate.equipment)
                equipmentKey.isNotBlank() && equipmentKey in unavailableKeys
            }
            .distinctBy(CatalogExercise::id)
            .take(limit)
            .toList()
        val preferredId = request.preferredAlternativeByExerciseId[current.id]
            ?.takeIf { id -> baseOptions.any { option -> option.id == id } }

        return Mo2WorkoutSwapResult(
            current = current,
            baseOptions = baseOptions,
            preferredId = preferredId,
        )
    }

    fun optionsForReason(
        result: Mo2WorkoutSwapResult,
        reason: String,
        limit: Int = 8,
    ): List<CatalogExercise> {
        val current = result.current
        val filtered = when (reason) {
            "same_level" -> result.baseOptions.filter { candidate ->
                candidate.level.isBlank() ||
                    current.level.isBlank() ||
                    normalize(candidate.level) == normalize(current.level)
            }
            "same_muscle" -> result.baseOptions.filter { candidate ->
                normalize(candidate.muscle) == normalize(current.muscle)
            }
            else -> result.baseOptions
        }.ifEmpty { result.baseOptions }
        val preferred = result.preferredId
            ?.let { id -> result.baseOptions.firstOrNull { option -> option.id == id } }

        return (listOfNotNull(preferred) + filtered)
            .distinctBy(CatalogExercise::id)
            .take(limit.coerceAtLeast(1))
    }

    private fun bestMatch(
        aliases: List<String>,
        candidates: List<IndexedExercise>,
    ): CatalogExercise? {
        val mediaCandidates = candidates.filter { indexed -> indexed.exercise.links.isNotEmpty() }
        return scoreBestMatch(aliases, mediaCandidates)
            ?: scoreBestMatch(aliases, candidates)
    }

    private fun scoreBestMatch(
        aliases: List<String>,
        candidates: List<IndexedExercise>,
    ): CatalogExercise? {
        var best: CatalogExercise? = null
        var bestScore = 0
        aliases.forEach { alias ->
            val normalizedAlias = normalize(alias)
            val tokens = normalizedAlias
                .split(' ')
                .filter { token -> token.length > 2 && token !in ignoredMatchTokens }
            candidates.forEach { indexed ->
                var score = 0
                if (indexed.normalizedName == normalizedAlias) score += 120
                if (
                    indexed.normalizedName.contains(normalizedAlias) ||
                    normalizedAlias.contains(indexed.normalizedName)
                ) {
                    score += 70
                }
                score += tokens.count(indexed.normalizedHaystack::contains) * 14
                if (indexed.exercise.links.isNotEmpty()) score += 4
                if (score > bestScore) {
                    bestScore = score
                    best = indexed.exercise
                }
            }
        }
        return best.takeIf { bestScore >= 28 }
    }

    private fun index(exercise: CatalogExercise): IndexedExercise {
        return IndexedExercise(
            exercise = exercise,
            normalizedName = normalize(exercise.name),
            normalizedHaystack = normalize(
                listOf(
                    exercise.name,
                    exercise.slug,
                    exercise.muscle,
                    exercise.subgroup,
                    exercise.primary,
                    exercise.secondary,
                    exercise.equipment,
                    exercise.movement,
                ).joinToString(" "),
            ),
        )
    }

    private fun CatalogExercise.toAlternativeProfile(): Mo2ExerciseProfile {
        return Mo2ExerciseProfile(
            id = id,
            name = name,
            slug = slug,
            muscle = muscle,
            subgroup = subgroup,
            movement = movement,
            type = type,
            level = level,
            primary = primary,
            equipment = equipment,
            alternatives = alternatives,
        )
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
