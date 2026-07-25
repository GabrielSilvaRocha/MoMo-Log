package br.com.mo2log.mobile.bluetooth

import java.util.UUID

internal class FakeBluetoothPlatform : BluetoothPlatform {
    var adapterAvailable = true
    var permissionGranted = true
    var bluetoothEnabled = true
    var devices: List<BluetoothDeviceCandidate> = emptyList()
    var deviceConnectionState = BluetoothPlatformConnectionState.DISCONNECTED
    var gattResult: GattCharacteristicReadResult? = GattCharacteristicReadResult.ServiceUnavailable
    var gattReadCount = 0
    var startCount = 0
    var stopCount = 0
    var closeCount = 0
    var lastServiceUuid: UUID? = null
    var lastCharacteristicUuid: UUID? = null
    private var observer: BluetoothPlatformObserver? = null
    private var pendingGattCallback: ((GattCharacteristicReadResult) -> Unit)? = null

    override fun hasBluetoothAdapter(): Boolean = adapterAvailable
    override fun hasConnectPermission(): Boolean = permissionGranted
    override fun isBluetoothEnabled(): Boolean = bluetoothEnabled
    override fun bondedDevices(): List<BluetoothDeviceCandidate> = devices
    override fun connectionState(device: BluetoothDeviceCandidate): BluetoothPlatformConnectionState {
        return deviceConnectionState
    }

    override fun startObserving(observer: BluetoothPlatformObserver) {
        startCount += 1
        this.observer = observer
    }

    override fun stopObserving() {
        stopCount += 1
        observer = null
    }

    override fun readGattCharacteristic(
        device: BluetoothDeviceCandidate,
        serviceUuid: UUID,
        characteristicUuid: UUID,
        timeoutMillis: Long,
        callback: (GattCharacteristicReadResult) -> Unit,
    ): BluetoothOperation {
        gattReadCount += 1
        lastServiceUuid = serviceUuid
        lastCharacteristicUuid = characteristicUuid
        val result = gattResult
        if (result == null) pendingGattCallback = callback else callback(result)
        return BluetoothOperation { pendingGattCallback = null }
    }

    override fun close() {
        closeCount += 1
        observer = null
        pendingGattCallback = null
    }

    fun triggerChange() {
        observer?.onBluetoothEnvironmentChanged()
    }

    fun completeGatt(result: GattCharacteristicReadResult) {
        val callback = pendingGattCallback
        pendingGattCallback = null
        callback?.invoke(result)
    }
}

internal class FakeBatteryDataSource(
    var result: BluetoothBatteryReadResult? = BluetoothBatteryReadResult.Unavailable,
) : BluetoothBatteryDataSource {
    var readCount = 0
    var closeCount = 0
    var cancelCount = 0
    private var pendingCallback: ((BluetoothBatteryReadResult) -> Unit)? = null

    override fun readBattery(
        device: BluetoothDeviceCandidate,
        callback: (BluetoothBatteryReadResult) -> Unit,
    ): BluetoothOperation {
        readCount += 1
        val currentResult = result
        if (currentResult == null) pendingCallback = callback else callback(currentResult)
        return BluetoothOperation {
            cancelCount += 1
            pendingCallback = null
        }
    }

    override fun close() {
        closeCount += 1
        pendingCallback = null
    }

    fun complete(result: BluetoothBatteryReadResult) {
        val callback = pendingCallback
        pendingCallback = null
        callback?.invoke(result)
    }
}

internal class FakeSportX20StateStore(
    var lastState: SportX20BatteryState? = null,
    private var storedAddress: String? = null,
    private var storedName: String? = null,
) : SportX20StatePersistence {
    var saveSelectedCount = 0
    var saveBatteryCount = 0
    var clearCount = 0

    override fun selectedAddress(): String? = storedAddress
    override fun selectedName(): String? = storedName

    override fun saveSelectedDevice(device: BluetoothDeviceCandidate) {
        saveSelectedCount += 1
        storedAddress = device.address
        storedName = device.name
    }

    override fun loadLastBatteryState(): SportX20BatteryState? = lastState

    override fun saveBatteryState(state: SportX20BatteryState) {
        saveBatteryCount += 1
        lastState = state
    }

    override fun clear() {
        clearCount += 1
        lastState = null
        storedAddress = null
        storedName = null
    }
}
