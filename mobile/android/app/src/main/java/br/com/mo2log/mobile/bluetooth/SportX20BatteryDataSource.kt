package br.com.mo2log.mobile.bluetooth

class SportX20BatteryDataSource : BluetoothBatteryDataSource {
    override fun readBattery(
        device: BluetoothDeviceCandidate,
        callback: (BluetoothBatteryReadResult) -> Unit,
    ): BluetoothOperation {
        // The proprietary Sport X20 protocol is intentionally not implemented until verified.
        callback(BluetoothBatteryReadResult.Unavailable)
        return CompletedBluetoothOperation
    }

    override fun close() = Unit
}
