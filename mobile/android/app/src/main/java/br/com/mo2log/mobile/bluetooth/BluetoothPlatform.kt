package br.com.mo2log.mobile.bluetooth

import java.util.UUID

enum class BluetoothPlatformConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    UNKNOWN,
}

fun interface BluetoothPlatformObserver {
    fun onBluetoothEnvironmentChanged()
}

sealed class GattCharacteristicReadResult {
    data class Success(val payload: ByteArray) : GattCharacteristicReadResult()
    object ServiceUnavailable : GattCharacteristicReadResult()
    object CharacteristicUnavailable : GattCharacteristicReadResult()
    object Timeout : GattCharacteristicReadResult()
    object Cancelled : GattCharacteristicReadResult()
    data class Failure(val message: String) : GattCharacteristicReadResult()
}

fun interface BluetoothOperation {
    fun cancel()
}

object CompletedBluetoothOperation : BluetoothOperation {
    override fun cancel() = Unit
}

interface BluetoothPlatform {
    fun hasBluetoothAdapter(): Boolean
    fun hasConnectPermission(): Boolean
    fun isBluetoothEnabled(): Boolean
    fun bondedDevices(): List<BluetoothDeviceCandidate>
    fun connectionState(device: BluetoothDeviceCandidate): BluetoothPlatformConnectionState
    fun startObserving(observer: BluetoothPlatformObserver)
    fun stopObserving()
    fun readGattCharacteristic(
        device: BluetoothDeviceCandidate,
        serviceUuid: UUID,
        characteristicUuid: UUID,
        timeoutMillis: Long,
        callback: (GattCharacteristicReadResult) -> Unit,
    ): BluetoothOperation
    fun close()
}
