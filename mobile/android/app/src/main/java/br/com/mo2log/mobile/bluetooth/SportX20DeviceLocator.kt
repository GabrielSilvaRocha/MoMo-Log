package br.com.mo2log.mobile.bluetooth

import java.text.Normalizer
import java.util.Locale

data class BluetoothDeviceCandidate(
    val name: String?,
    val address: String,
)

sealed class SportX20DeviceSelection {
    data class Found(val device: BluetoothDeviceCandidate) : SportX20DeviceSelection()
    data class MultipleCandidates(val devices: List<BluetoothDeviceCandidate>) : SportX20DeviceSelection()
    object NotFound : SportX20DeviceSelection()
}

class SportX20DeviceLocator {
    fun locate(
        bondedDevices: List<BluetoothDeviceCandidate>,
        selectedAddress: String?,
    ): SportX20DeviceSelection {
        val storedMatch = selectedAddress
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.let { address -> bondedDevices.firstOrNull { it.address.equals(address, ignoreCase = true) } }
        if (storedMatch != null) return SportX20DeviceSelection.Found(storedMatch)

        val namedMatches = bondedDevices.filter { isSportX20Name(it.name) }
        return when (namedMatches.size) {
            0 -> SportX20DeviceSelection.NotFound
            1 -> SportX20DeviceSelection.Found(namedMatches.single())
            else -> SportX20DeviceSelection.MultipleCandidates(namedMatches)
        }
    }

    fun isSportX20Name(name: String?): Boolean {
        val normalized = normalizeName(name)
        return sportX20NamePattern.containsMatchIn(normalized)
    }

    fun normalizeName(name: String?): String {
        if (name.isNullOrBlank()) return ""
        val withoutAccents = Normalizer.normalize(name, Normalizer.Form.NFD)
            .replace(combiningMarkPattern, "")
        return withoutAccents
            .lowercase(Locale.ROOT)
            .replace(separatorPattern, " ")
            .trim()
            .replace(repeatedWhitespacePattern, " ")
    }

    companion object {
        private val combiningMarkPattern = Regex("\\p{M}+")
        private val separatorPattern = Regex("[-_./:]+")
        private val repeatedWhitespacePattern = Regex("\\s+")
        private val sportX20NamePattern = Regex("(^| )sport x20($| )")
    }
}
