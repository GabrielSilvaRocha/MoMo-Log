package br.com.mo2log.mobile.bluetooth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class StandardGattBatteryDataSourceTest {
    @Test
    fun usesOnlyStandardBatteryUuids() {
        assertEquals(
            UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb"),
            StandardGattBatteryProtocol.BATTERY_SERVICE_UUID,
        )
        assertEquals(
            UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb"),
            StandardGattBatteryProtocol.BATTERY_LEVEL_UUID,
        )
    }

    @Test
    fun parsesZeroAndOneHundredPercent() {
        assertEquals(0, StandardGattBatteryProtocol.parseBatteryLevel(byteArrayOf(0)))
        assertEquals(100, StandardGattBatteryProtocol.parseBatteryLevel(byteArrayOf(100)))
    }

    @Test
    fun rejectsEmptyPayload() {
        assertNull(StandardGattBatteryProtocol.parseBatteryLevel(byteArrayOf()))
    }

    @Test
    fun rejectsPayloadWithMoreThanOneByte() {
        assertNull(StandardGattBatteryProtocol.parseBatteryLevel(byteArrayOf(80, 81)))
    }

    @Test
    fun rejectsPercentAboveOneHundred() {
        assertNull(StandardGattBatteryProtocol.parseBatteryLevel(byteArrayOf(101)))
    }

    @Test
    fun standardLevelIsStoredOnlyAsCombinedBattery() {
        val platform = FakeBluetoothPlatform().apply {
            gattResult = GattCharacteristicReadResult.Success(byteArrayOf(80))
        }
        val source = StandardGattBatteryDataSource(platform)
        var result: BluetoothBatteryReadResult? = null

        source.readBattery(
            BluetoothDeviceCandidate("soundcore Sport X20", "00:11:22:33:44:55"),
        ) { result = it }

        val success = result as BluetoothBatteryReadResult.Success
        assertEquals(80, success.values.combinedBatteryPercent)
        assertNull(success.values.leftBatteryPercent)
        assertNull(success.values.rightBatteryPercent)
        assertNull(success.values.caseBatteryPercent)
        assertEquals(
            BluetoothBatteryDataSourceType.STANDARD_GATT_BATTERY_SERVICE,
            success.dataSource,
        )
        assertEquals(StandardGattBatteryProtocol.BATTERY_SERVICE_UUID, platform.lastServiceUuid)
        assertEquals(StandardGattBatteryProtocol.BATTERY_LEVEL_UUID, platform.lastCharacteristicUuid)
    }

    @Test
    fun missingServiceIsUnavailableInsteadOfFatal() {
        val platform = FakeBluetoothPlatform().apply {
            gattResult = GattCharacteristicReadResult.ServiceUnavailable
        }
        val source = StandardGattBatteryDataSource(platform)
        var result: BluetoothBatteryReadResult? = null

        source.readBattery(
            BluetoothDeviceCandidate("Sport X20", "00:11:22:33:44:55"),
        ) { result = it }

        assertTrue(result is BluetoothBatteryReadResult.Unavailable)
    }

    @Test
    fun timeoutIsPreserved() {
        val platform = FakeBluetoothPlatform().apply {
            gattResult = GattCharacteristicReadResult.Timeout
        }
        val source = StandardGattBatteryDataSource(platform)
        var result: BluetoothBatteryReadResult? = null

        source.readBattery(
            BluetoothDeviceCandidate("Sport X20", "00:11:22:33:44:55"),
        ) { result = it }

        assertTrue(result is BluetoothBatteryReadResult.Timeout)
    }
}
