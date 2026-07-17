package br.com.mo2log.mobile

import kotlin.math.roundToInt

data class Mo2Trend(
    val percent: Int?,
    val direction: String,
)

data class Mo2DataHealth(
    val status: String,
    val issues: List<String>,
)

object Mo2ProgressEngine {
    fun trend(current: Double, previous: Double): Mo2Trend {
        if (previous <= 0.0) {
            return Mo2Trend(
                percent = null,
                direction = if (current > 0.0) "novo" else "estavel",
            )
        }
        val percent = (((current - previous) / previous) * 100.0).roundToInt()
        return Mo2Trend(
            percent = percent,
            direction = when {
                percent >= 3 -> "subiu"
                percent <= -3 -> "caiu"
                else -> "estavel"
            },
        )
    }

    fun consistencyScore(completed: Int, planned: Int, activeDays: Int, targetDays: Int): Int {
        val completion = if (planned <= 0) 0.0 else completed.coerceAtLeast(0).toDouble() / planned.toDouble()
        val frequency = if (targetDays <= 0) 0.0 else activeDays.coerceAtLeast(0).toDouble() / targetDays.toDouble()
        return ((completion.coerceIn(0.0, 1.0) * 70.0) + (frequency.coerceIn(0.0, 1.0) * 30.0)).roundToInt()
    }

    fun dataHealth(invalidCollections: Int, backupAgeDays: Int?, recordCount: Int): Mo2DataHealth {
        val issues = mutableListOf<String>()
        if (invalidCollections > 0) issues.add(invalidCollections.toString() + " colecao(oes) local(is) precisam de revisao")
        if (recordCount > 0 && backupAgeDays == null) issues.add("backup ainda nao criado")
        if (backupAgeDays != null && backupAgeDays > 7) issues.add("backup tem mais de 7 dias")
        return Mo2DataHealth(
            status = when {
                invalidCollections > 0 -> "Revisar"
                issues.isNotEmpty() -> "Atencao"
                else -> "Integro"
            },
            issues = issues,
        )
    }

    fun volumeBalance(volumes: Map<String, Double>): String {
        val positive = volumes.filterValues { it > 0.0 }
        if (positive.size < 2) return "Registre mais grupos musculares para comparar o equilibrio."
        val strongest = positive.maxBy { it.value }
        val weakest = positive.minBy { it.value }
        if (weakest.value <= 0.0 || strongest.value / weakest.value >= 3.0) {
            return "Volume concentrado em " + strongest.key + ". Revise " + weakest.key + " na proxima semana."
        }
        return "Distribuicao equilibrada entre os grupos registrados."
    }
}
