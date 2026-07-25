package br.com.mo2log.mobile.bluetooth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SportX20MonitorTest {
    private val device = BluetoothDeviceCandidate("soundcore Sport X20", "00:11:22:33:44:55")

    @Test
    fun reportsUnavailableAdapter() {
        val platform = FakeBluetoothPlatform().apply { adapterAvailable = false }
        val monitor = monitor(platform = platform)

        monitor.start()

        assertEquals(BluetoothConnectionStatus.BLUETOOTH_UNAVAILABLE, monitor.currentState().connectionStatus)
    }

    @Test
    fun reportsDisabledBluetooth() {
        val platform = FakeBluetoothPlatform().apply { bluetoothEnabled = false }
        val monitor = monitor(platform = platform)

        monitor.start()

        assertEquals(BluetoothConnectionStatus.BLUETOOTH_DISABLED, monitor.currentState().connectionStatus)
    }

    @Test
    fun reportsRequiredPermission() {
        val platform = FakeBluetoothPlatform().apply { permissionGranted = false }
        val monitor = monitor(platform = platform)

        monitor.start()

        assertEquals(BluetoothConnectionStatus.PERMISSION_REQUIRED, monitor.currentState().connectionStatus)
    }

    @Test
    fun permissionResultReopensObservationAndRefreshesState() {
        val platform = connectedPlatform().apply { permissionGranted = false }
        val monitor = monitor(platform = platform)
        monitor.start()
        platform.permissionGranted = true

        monitor.onPermissionResult()

        assertEquals(2, platform.startCount)
        assertEquals(
            BluetoothConnectionStatus.CONNECTED_WITHOUT_BATTERY_DATA,
            monitor.currentState().connectionStatus,
        )
    }

    @Test
    fun reportsDeviceNotPaired() {
        val monitor = monitor(platform = FakeBluetoothPlatform())

        monitor.start()

        assertEquals(BluetoothConnectionStatus.DEVICE_NOT_PAIRED, monitor.currentState().connectionStatus)
    }

    @Test
    fun reportsMultipleCandidatesWithoutChoosingOne() {
        val platform = connectedPlatform().apply {
            devices = listOf(device, device.copy(address = "AA:BB:CC:DD:EE:FF"))
        }
        val store = FakeSportX20StateStore()
        val monitor = monitor(platform = platform, store = store)

        monitor.start()

        assertEquals(BluetoothConnectionStatus.MULTIPLE_CANDIDATES, monitor.currentState().connectionStatus)
        assertEquals(0, store.saveSelectedCount)
    }

    @Test
    fun reportsPairedDeviceAsDisconnected() {
        val platform = FakeBluetoothPlatform().apply {
            devices = listOf(device)
            deviceConnectionState = BluetoothPlatformConnectionState.DISCONNECTED
        }
        val monitor = monitor(platform = platform)

        monitor.start()

        assertEquals(BluetoothConnectionStatus.PAIRED_DISCONNECTED, monitor.currentState().connectionStatus)
    }

    @Test
    fun connectedDeviceWithoutPublicBatteryIsNotFatal() {
        val monitor = monitor(platform = connectedPlatform())

        monitor.start()

        assertEquals(
            BluetoothConnectionStatus.CONNECTED_WITHOUT_BATTERY_DATA,
            monitor.currentState().connectionStatus,
        )
        assertNull(monitor.currentState().combinedBatteryPercent)
        assertNull(monitor.currentState().errorMessage)
    }

    @Test
    fun validAggregateReadingIsPublishedAndPersisted() {
        val standard = FakeBatteryDataSource(
            BluetoothBatteryReadResult.Success(
                values = BluetoothBatteryValues(combinedBatteryPercent = 80),
                dataSource = BluetoothBatteryDataSourceType.STANDARD_GATT_BATTERY_SERVICE,
            ),
        )
        val store = FakeSportX20StateStore()
        val monitor = monitor(platform = connectedPlatform(), standard = standard, store = store)

        monitor.start()

        val state = monitor.currentState()
        assertEquals(BluetoothConnectionStatus.CONNECTED, state.connectionStatus)
        assertEquals(80, state.combinedBatteryPercent)
        assertNull(state.leftBatteryPercent)
        assertNull(state.rightBatteryPercent)
        assertNull(state.caseBatteryPercent)
        assertEquals(4_242L, state.lastUpdatedAt)
        assertEquals(BluetoothBatteryDataSourceType.STANDARD_GATT_BATTERY_SERVICE, state.dataSource)
        assertFalse(state.isStale)
        assertEquals(1, store.saveBatteryCount)
    }

    @Test
    fun invalidReadingEndsConnectedWithoutBattery() {
        val standard = FakeBatteryDataSource(BluetoothBatteryReadResult.InvalidData)
        val monitor = monitor(platform = connectedPlatform(), standard = standard)

        monitor.start()

        assertEquals(
            BluetoothConnectionStatus.CONNECTED_WITHOUT_BATTERY_DATA,
            monitor.currentState().connectionStatus,
        )
        assertTrue(monitor.currentState().errorMessage.orEmpty().contains("invalido"))
    }

    @Test
    fun timeoutEndsConnectedWithoutBattery() {
        val standard = FakeBatteryDataSource(BluetoothBatteryReadResult.Timeout)
        val monitor = monitor(platform = connectedPlatform(), standard = standard)

        monitor.start()

        assertEquals(
            BluetoothConnectionStatus.CONNECTED_WITHOUT_BATTERY_DATA,
            monitor.currentState().connectionStatus,
        )
        assertTrue(monitor.currentState().errorMessage.orEmpty().contains("limite"))
    }

    @Test
    fun persistedBatteryRemainsStaleWhileDisconnected() {
        val cached = cachedState(72)
        val store = FakeSportX20StateStore(
            lastState = cached,
            storedAddress = device.address,
            storedName = device.name,
        )
        val platform = FakeBluetoothPlatform().apply {
            devices = listOf(device)
            deviceConnectionState = BluetoothPlatformConnectionState.DISCONNECTED
        }
        val monitor = monitor(platform = platform, store = store)

        monitor.start()

        val state = monitor.currentState()
        assertEquals(BluetoothConnectionStatus.PAIRED_DISCONNECTED, state.connectionStatus)
        assertEquals(72, state.combinedBatteryPercent)
        assertTrue(state.isStale)
        assertEquals(BluetoothBatteryDataSourceType.PERSISTED_CACHE, state.dataSource)
        assertEquals(100L, state.lastUpdatedAt)
    }

    @Test
    fun successfulReadingBecomesStaleAfterDisconnect() {
        val platform = connectedPlatform()
        val standard = FakeBatteryDataSource(
            BluetoothBatteryReadResult.Success(
                values = BluetoothBatteryValues(combinedBatteryPercent = 55),
                dataSource = BluetoothBatteryDataSourceType.STANDARD_GATT_BATTERY_SERVICE,
            ),
        )
        val monitor = monitor(platform = platform, standard = standard)
        monitor.start()

        platform.deviceConnectionState = BluetoothPlatformConnectionState.DISCONNECTED
        platform.triggerChange()

        val state = monitor.currentState()
        assertEquals(BluetoothConnectionStatus.PAIRED_DISCONNECTED, state.connectionStatus)
        assertEquals(55, state.combinedBatteryPercent)
        assertEquals(4_242L, state.lastUpdatedAt)
        assertTrue(state.isStale)
    }

    @Test
    fun repeatedEnvironmentStateDoesNotEmitDuplicates() {
        val platform = connectedPlatform()
        val monitor = monitor(platform = platform)
        val states = mutableListOf<SportX20BatteryState>()
        monitor.addListener(SportX20StateListener { states += it })
        monitor.start()
        val countAfterStart = states.size

        platform.triggerChange()

        assertEquals(countAfterStart, states.size)
    }

    @Test
    fun concurrentGattReadsArePrevented() {
        val platform = connectedPlatform()
        val standard = FakeBatteryDataSource(result = null)
        val monitor = monitor(platform = platform, standard = standard)
        monitor.start()

        platform.triggerChange()
        platform.triggerChange()

        assertEquals(1, standard.readCount)
        standard.complete(BluetoothBatteryReadResult.Unavailable)
        assertEquals(
            BluetoothConnectionStatus.CONNECTED_WITHOUT_BATTERY_DATA,
            monitor.currentState().connectionStatus,
        )
    }

    @Test
    fun closeIsIdempotent() {
        val platform = connectedPlatform()
        val standard = FakeBatteryDataSource()
        val proprietary = FakeBatteryDataSource()
        val monitor = monitor(
            platform = platform,
            standard = standard,
            proprietary = proprietary,
        )
        monitor.start()

        monitor.close()
        monitor.close()

        assertEquals(1, platform.closeCount)
        assertEquals(1, standard.closeCount)
        assertEquals(1, proprietary.closeCount)
    }

    private fun connectedPlatform(): FakeBluetoothPlatform {
        return FakeBluetoothPlatform().apply {
            devices = listOf(device)
            deviceConnectionState = BluetoothPlatformConnectionState.CONNECTED
        }
    }

    private fun cachedState(percent: Int): SportX20BatteryState {
        return SportX20BatteryState.create(
            deviceName = device.name,
            deviceAddress = device.address,
            connectionStatus = BluetoothConnectionStatus.PAIRED_DISCONNECTED,
            combinedBatteryPercent = percent,
            lastUpdatedAt = 100L,
            dataSource = BluetoothBatteryDataSourceType.PERSISTED_CACHE,
            isStale = true,
        )
    }

    private fun monitor(
        platform: FakeBluetoothPlatform,
        standard: FakeBatteryDataSource = FakeBatteryDataSource(),
        proprietary: FakeBatteryDataSource = FakeBatteryDataSource(),
        store: FakeSportX20StateStore = FakeSportX20StateStore(),
    ): SportX20Monitor {
        return SportX20Monitor(
            platform = platform,
            standardBatteryDataSource = standard,
            sportX20BatteryDataSource = proprietary,
            stateStore = store,
            dispatcher = BluetoothCallbackDispatcher { it() },
            clock = { 4_242L },
        )
    }
}
