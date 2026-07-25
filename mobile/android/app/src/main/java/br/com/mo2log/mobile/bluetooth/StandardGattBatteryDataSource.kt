package br.com.mo2log.mobile.bluetooth

import java.util.UUID

object StandardGattBatteryProtocol {
    val BATTERY_SERVICE_UUID: UUID = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb")
    val BATTERY_LEVEL_UUID: UUID = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")

    fun parseBatteryLevel(payload: ByteArray?): Int? {
        if (payload == null || payload.size != 1) return null
        return safeBatteryPercent(payload[0].toInt() and 0xff)
    }
}

class StandardGattBatteryDataSource(
    private val platform: BluetoothPlatform,
    private val timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS,
) : BluetoothBatteryDataSource {
    private val operations = mutableSetOf<DelegatingBluetoothOperation>()
    private var closed = false

    override fun readBattery(
        device: BluetoothDeviceCandidate,
        callback: (BluetoothBatteryReadResult) -> Unit,
    ): BluetoothOperation {
        val operation = DelegatingBluetoothOperation()
        synchronized(this) {
            if (closed) {
                callback(BluetoothBatteryReadResult.Cancelled)
                return CompletedBluetoothOperation
            }
            operations += operation
        }

        val delegate = platform.readGattCharacteristic(
            device = device,
            serviceUuid = StandardGattBatteryProtocol.BATTERY_SERVICE_UUID,
            characteristicUuid = StandardGattBatteryProtocol.BATTERY_LEVEL_UUID,
            timeoutMillis = timeoutMillis,
        ) { result ->
            synchronized(this) { operations -= operation }
            callback(mapResult(result))
        }
        operation.attach(delegate)
        return operation
    }

    override fun close() {
        val pending = synchronized(this) {
            if (closed) return
            closed = true
            operations.toList().also { operations.clear() }
        }
        pending.forEach { it.cancel() }
    }

    private fun mapResult(result: GattCharacteristicReadResult): BluetoothBatteryReadResult {
        return when (result) {
            is GattCharacteristicReadResult.Success -> {
                val percent = StandardGattBatteryProtocol.parseBatteryLevel(result.payload)
                if (percent == null) {
                    BluetoothBatteryReadResult.InvalidData
                } else {
                    BluetoothBatteryReadResult.Success(
                        values = BluetoothBatteryValues(combinedBatteryPercent = percent),
                        dataSource = BluetoothBatteryDataSourceType.STANDARD_GATT_BATTERY_SERVICE,
                    )
                }
            }
            GattCharacteristicReadResult.ServiceUnavailable,
            GattCharacteristicReadResult.CharacteristicUnavailable -> BluetoothBatteryReadResult.Unavailable
            GattCharacteristicReadResult.Timeout -> BluetoothBatteryReadResult.Timeout
            GattCharacteristicReadResult.Cancelled -> BluetoothBatteryReadResult.Cancelled
            is GattCharacteristicReadResult.Failure -> {
                BluetoothBatteryReadResult.Failure(
                    safeBluetoothErrorMessage(result.message) ?: "Falha na leitura GATT",
                )
            }
        }
    }

    private class DelegatingBluetoothOperation : BluetoothOperation {
        private var delegate: BluetoothOperation? = null
        private var cancelled = false

        @Synchronized
        fun attach(operation: BluetoothOperation) {
            if (cancelled) operation.cancel() else delegate = operation
        }

        @Synchronized
        override fun cancel() {
            if (cancelled) return
            cancelled = true
            delegate?.cancel()
            delegate = null
        }
    }

    companion object {
        const val DEFAULT_TIMEOUT_MILLIS = 8_000L
    }
}
