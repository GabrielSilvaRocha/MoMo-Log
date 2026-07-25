package br.com.mo2log.mobile.bluetooth

data class BluetoothBatteryValues(
    val leftBatteryPercent: Int? = null,
    val rightBatteryPercent: Int? = null,
    val combinedBatteryPercent: Int? = null,
    val caseBatteryPercent: Int? = null,
    val caseBatteryRange: String? = null,
) {
    init {
        require(leftBatteryPercent == safeBatteryPercent(leftBatteryPercent))
        require(rightBatteryPercent == safeBatteryPercent(rightBatteryPercent))
        require(combinedBatteryPercent == safeBatteryPercent(combinedBatteryPercent))
        require(caseBatteryPercent == safeBatteryPercent(caseBatteryPercent))
        require(caseBatteryPercent == null || caseBatteryRange == null)
    }

    fun hasData(): Boolean {
        return leftBatteryPercent != null ||
            rightBatteryPercent != null ||
            combinedBatteryPercent != null ||
            caseBatteryPercent != null ||
            caseBatteryRange != null
    }
}

sealed class BluetoothBatteryReadResult {
    data class Success(
        val values: BluetoothBatteryValues,
        val dataSource: BluetoothBatteryDataSourceType,
    ) : BluetoothBatteryReadResult()

    object Unavailable : BluetoothBatteryReadResult()
    object Timeout : BluetoothBatteryReadResult()
    object InvalidData : BluetoothBatteryReadResult()
    object Cancelled : BluetoothBatteryReadResult()
    data class Failure(val message: String) : BluetoothBatteryReadResult()
}

interface BluetoothBatteryDataSource {
    fun readBattery(
        device: BluetoothDeviceCandidate,
        callback: (BluetoothBatteryReadResult) -> Unit,
    ): BluetoothOperation

    fun close()
}
