package br.com.mo2log.mobile.bluetooth

private val bluetoothMacAddressPattern = Regex("(?i)([0-9a-f]{2}:){5}[0-9a-f]{2}")

fun safeBatteryPercent(value: Int?): Int? = value?.takeIf { it in 0..100 }

fun safeBluetoothErrorMessage(message: String?): String? {
    return message
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?.replace(bluetoothMacAddressPattern, "XX:XX:XX:XX:XX:XX")
}

data class SportX20BatteryState(
    val deviceName: String?,
    val deviceAddress: String?,
    val connectionStatus: BluetoothConnectionStatus,
    val leftBatteryPercent: Int?,
    val rightBatteryPercent: Int?,
    val combinedBatteryPercent: Int?,
    val caseBatteryPercent: Int?,
    val caseBatteryRange: String?,
    val lastUpdatedAt: Long?,
    val dataSource: BluetoothBatteryDataSourceType,
    val isStale: Boolean,
    val errorMessage: String?,
) {
    init {
        require(leftBatteryPercent == safeBatteryPercent(leftBatteryPercent))
        require(rightBatteryPercent == safeBatteryPercent(rightBatteryPercent))
        require(combinedBatteryPercent == safeBatteryPercent(combinedBatteryPercent))
        require(caseBatteryPercent == safeBatteryPercent(caseBatteryPercent))
        require(caseBatteryPercent == null || caseBatteryRange == null)
        require(lastUpdatedAt == null || lastUpdatedAt >= 0L)
        require(errorMessage == safeBluetoothErrorMessage(errorMessage))
    }

    fun hasBatteryData(): Boolean {
        return leftBatteryPercent != null ||
            rightBatteryPercent != null ||
            combinedBatteryPercent != null ||
            caseBatteryPercent != null ||
            caseBatteryRange != null
    }

    companion object {
        fun create(
            deviceName: String? = null,
            deviceAddress: String? = null,
            connectionStatus: BluetoothConnectionStatus,
            leftBatteryPercent: Int? = null,
            rightBatteryPercent: Int? = null,
            combinedBatteryPercent: Int? = null,
            caseBatteryPercent: Int? = null,
            caseBatteryRange: String? = null,
            lastUpdatedAt: Long? = null,
            dataSource: BluetoothBatteryDataSourceType = BluetoothBatteryDataSourceType.NONE,
            isStale: Boolean = false,
            errorMessage: String? = null,
        ): SportX20BatteryState {
            val safeCasePercent = safeBatteryPercent(caseBatteryPercent)
            val safeCaseRange = caseBatteryRange
                ?.trim()
                ?.takeIf { it.isNotEmpty() && safeCasePercent == null }

            return SportX20BatteryState(
                deviceName = deviceName?.trim()?.takeIf { it.isNotEmpty() },
                deviceAddress = deviceAddress?.trim()?.takeIf { it.isNotEmpty() },
                connectionStatus = connectionStatus,
                leftBatteryPercent = safeBatteryPercent(leftBatteryPercent),
                rightBatteryPercent = safeBatteryPercent(rightBatteryPercent),
                combinedBatteryPercent = safeBatteryPercent(combinedBatteryPercent),
                caseBatteryPercent = safeCasePercent,
                caseBatteryRange = safeCaseRange,
                lastUpdatedAt = lastUpdatedAt?.takeIf { it >= 0L },
                dataSource = dataSource,
                isStale = isStale,
                errorMessage = safeBluetoothErrorMessage(errorMessage),
            )
        }

        fun empty(connectionStatus: BluetoothConnectionStatus): SportX20BatteryState {
            return create(connectionStatus = connectionStatus)
        }
    }
}
