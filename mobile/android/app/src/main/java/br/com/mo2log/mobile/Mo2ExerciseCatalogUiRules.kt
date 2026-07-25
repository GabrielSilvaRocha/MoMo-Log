package br.com.mo2log.mobile

import java.text.Normalizer
import java.util.Locale

object Mo2ExerciseCatalogUiRules {
    val quickFilters = listOf("Todos", "Favoritos", "Peito", "Costas", "Pernas")

    private val lowerBodyTerms = setOf(
        "adutores",
        "gluteos",
        "isquiotibiais",
        "panturrilhas",
        "pernas",
        "posterior",
        "posteriores",
        "quadriceps",
        "tibial",
    )

    fun matchesFilter(
        filter: String,
        muscle: String,
        isFavorite: Boolean,
        isHidden: Boolean,
    ): Boolean {
        return when (normalize(filter)) {
            "todos" -> !isHidden
            "favoritos" -> isFavorite && !isHidden
            "ocultos" -> isHidden
            "pernas" -> !isHidden && lowerBodyTerms.any { normalize(muscle).contains(it) }
            else -> !isHidden && normalize(muscle) == normalize(filter)
        }
    }

    fun executionSteps(
        name: String,
        equipment: String,
        movement: String,
        description: String,
    ): List<String> {
        val normalizedName = normalize(name)
        val normalizedEquipment = normalize(equipment)
        val normalizedMovement = normalize(movement)
        val setup = when {
            normalizedName.contains("agachamento") && normalizedEquipment.contains("smith") ->
                "Ajuste a barra na altura dos ombros, posicione os pes firmes e destrave o equipamento."
            normalizedName.contains("supino") ->
                "Ajuste o banco e os apoios para iniciar com escapulas firmes e pes apoiados."
            normalizedName.contains("remada") || normalizedName.contains("puxada") ->
                "Ajuste o apoio e a pegada antes de iniciar, mantendo o tronco estavel."
            normalizedName.contains("rosca") || normalizedName.contains("triceps") ->
                "Escolha uma carga controlavel e estabilize ombros, tronco e cotovelos."
            else ->
                "Ajuste o equipamento e a postura antes de iniciar a primeira repeticao."
        }
        val descriptionIsMetadata = normalize(description).startsWith("foco principal") &&
            normalize(description).contains("padrao de movimento")
        val execution = if (description.isNotBlank() && !descriptionIsMetadata) {
            description.trim().trimEnd('.') + "."
        } else {
            when {
                normalizedName.contains("agachamento") || normalizedMovement.contains("agachar") ->
                    "Desca com controle, flexionando quadris e joelhos sem perder o alinhamento dos pes."
                normalizedName.contains("supino") || normalizedMovement.contains("empurrar") ->
                    "Conduza a carga pela amplitude confortavel, mantendo o tronco firme e o movimento simetrico."
                normalizedName.contains("remada") || normalizedName.contains("puxada") ||
                    normalizedMovement.contains("puxar") ->
                    "Puxe a carga com os cotovelos, sem compensar o movimento com o tronco."
                normalizedName.contains("stiff") || normalizedName.contains("terra") ||
                    normalizedMovement.contains("quadril") ->
                    "Leve o quadril para tras com a coluna neutra e mantenha a carga proxima ao corpo."
                normalizedName.contains("prancha") || normalizedName.contains("abdominal") ->
                    "Mantenha o tronco organizado e execute a fase ativa sem compensar com pescoco ou lombar."
                else ->
                    "Execute o movimento na amplitude confortavel, mantendo a carga sob controle."
            }
        }
        val finish = when {
            normalizedName.contains("agachamento") ->
                "Suba empurrando o apoio com os pes e finalize sem travar os joelhos."
            normalizedName.contains("supino") ->
                "Retorne a carga com controle e mantenha a posicao das escapulas."
            else ->
                "Retorne a carga com controle e reorganize a postura antes da proxima repeticao."
        }
        return listOf(setup, execution, finish)
    }

    fun equipmentLabel(name: String, equipment: String): String {
        val normalizedName = normalize(name)
        return when {
            normalizedName.contains("smith") -> "Smith"
            normalizedName.contains("landmine") -> "Landmine"
            normalizedName.contains("chest press") || normalizedName.contains("leg press") ||
                normalizedName.contains("hack") -> "Maquina"
            normalizedName.contains("halter") || normalizedName.contains("goblet") -> "Halter"
            normalizedName.contains("barra") -> "Barra"
            normalizedName.contains("maquina") || normalizedName.contains("cadeira") ||
                normalizedName.contains("mesa flexora") -> "Maquina"
            normalizedName.contains("polia") || normalizedName.contains("cabo") -> "Polia"
            normalizedName.contains("elastico") -> "Elastico"
            normalizedName.contains("prancha") || normalizedName.contains("solo") -> "Peso corporal"
            else -> equipment
                .split("/", ";", ",")
                .map(String::trim)
                .firstOrNull(String::isNotBlank)
                ?: "Equipamento variavel"
        }
    }

    fun displayTags(
        muscle: String,
        equipment: String,
        type: String,
        level: String,
    ): List<String> {
        return listOf(muscle, equipment, type, level)
            .map(String::trim)
            .filter { it.isNotBlank() && normalize(it) !in setOf("nao informado", "variavel") }
            .distinctBy(::normalize)
            .take(4)
    }

    private fun normalize(value: String): String {
        return Normalizer.normalize(value.lowercase(Locale("pt", "BR")), Normalizer.Form.NFD)
            .replace("\\p{Mn}+".toRegex(), "")
            .replace("[^a-z0-9]+".toRegex(), " ")
            .trim()
    }
}
