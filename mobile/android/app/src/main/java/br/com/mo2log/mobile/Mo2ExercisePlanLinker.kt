package br.com.mo2log.mobile

import java.text.Normalizer
import java.util.Locale

data class Mo2CatalogExerciseIdentity(
    val id: String,
    val name: String,
)

object Mo2ExercisePlanLinker {
    fun resolve(
        catalogExerciseId: String?,
        legacyName: String,
        catalog: List<Mo2CatalogExerciseIdentity>,
    ): Mo2CatalogExerciseIdentity? {
        catalogExerciseId?.let { id ->
            catalog.firstOrNull { it.id == id }?.let { return it }
        }
        val normalizedLegacyName = normalize(legacyName)
        return catalog.firstOrNull { normalize(it.name) == normalizedLegacyName }
    }

    private fun normalize(value: String): String {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace("\\p{Mn}+".toRegex(), "")
            .lowercase(Locale.ROOT)
            .replace("[^a-z0-9]+".toRegex(), " ")
            .trim()
    }
}
