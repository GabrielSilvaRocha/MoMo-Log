package br.com.mo2log.mobile.bluetooth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class SportX20BatteryStateTest {
    @Test
    fun zeroPercentIsValid() {
        assertEquals(0, safeBatteryPercent(0))
    }

    @Test
    fun oneHundredPercentIsValid() {
        assertEquals(100, safeBatteryPercent(100))
    }

    @Test
    fun negativePercentIsRejected() {
        assertNull(safeBatteryPercent(-1))
    }

    @Test
    fun percentAboveOneHundredIsRejected() {
        assertNull(safeBatteryPercent(101))
    }

    @Test
    fun directInvalidStateIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            SportX20BatteryState(
                deviceName = null,
                deviceAddress = null,
                connectionStatus = BluetoothConnectionStatus.CONNECTED,
                leftBatteryPercent = -1,
                rightBatteryPercent = null,
                combinedBatteryPercent = null,
                caseBatteryPercent = null,
                caseBatteryRange = null,
                lastUpdatedAt = null,
                dataSource = BluetoothBatteryDataSourceType.NONE,
                isStale = false,
                errorMessage = null,
            )
        }
    }

    @Test
    fun missingValuesRemainNullAndCombinedIsNotCopied() {
        val state = SportX20BatteryState.create(
            connectionStatus = BluetoothConnectionStatus.CONNECTED,
            combinedBatteryPercent = 80,
        )

        assertEquals(80, state.combinedBatteryPercent)
        assertNull(state.leftBatteryPercent)
        assertNull(state.rightBatteryPercent)
        assertNull(state.caseBatteryPercent)
    }

    @Test
    fun caseRangeDoesNotBecomeAnExactPercent() {
        val state = SportX20BatteryState.create(
            connectionStatus = BluetoothConnectionStatus.CONNECTED,
            caseBatteryRange = "30-60%",
        )

        assertEquals("30-60%", state.caseBatteryRange)
        assertNull(state.caseBatteryPercent)
    }

    @Test
    fun exactCasePercentTakesPrecedenceOverRangeInSafeFactory() {
        val state = SportX20BatteryState.create(
            connectionStatus = BluetoothConnectionStatus.CONNECTED,
            caseBatteryPercent = 50,
            caseBatteryRange = "30-60%",
        )

        assertEquals(50, state.caseBatteryPercent)
        assertNull(state.caseBatteryRange)
    }

    @Test
    fun restoredStateIsStaleAndUsesCacheSource() {
        val state = restorePersistedSportX20State(
            PersistedSportX20BatterySnapshot(
                deviceName = "soundcore Sport X20",
                deviceAddress = "00:11:22:33:44:55",
                leftBatteryPercent = null,
                rightBatteryPercent = null,
                combinedBatteryPercent = 65,
                caseBatteryPercent = null,
                caseBatteryRange = null,
                lastUpdatedAt = 123L,
                originalDataSource = BluetoothBatteryDataSourceType.STANDARD_GATT_BATTERY_SERVICE,
            ),
        )

        assertTrue(state?.isStale == true)
        assertEquals(BluetoothBatteryDataSourceType.PERSISTED_CACHE, state?.dataSource)
        assertEquals(65, state?.combinedBatteryPercent)
    }

    @Test
    fun snapshotWithoutValidBatteryDataIsDiscarded() {
        val state = restorePersistedSportX20State(
            PersistedSportX20BatterySnapshot(
                deviceName = null,
                deviceAddress = null,
                leftBatteryPercent = -1,
                rightBatteryPercent = 101,
                combinedBatteryPercent = null,
                caseBatteryPercent = null,
                caseBatteryRange = null,
                lastUpdatedAt = 123L,
                originalDataSource = null,
            ),
        )

        assertNull(state)
    }

    @Test
    fun errorMessagesMaskMacAddresses() {
        val message = safeBluetoothErrorMessage("Falha em 00:11:22:33:44:55")

        assertFalse(message.orEmpty().contains("00:11:22:33:44:55"))
        assertTrue(message.orEmpty().contains("XX:XX:XX:XX:XX:XX"))
    }
}
