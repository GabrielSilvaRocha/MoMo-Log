package br.com.mo2log.mobile.bluetooth.diagnostics

import br.com.mo2log.mobile.bluetooth.BluetoothBatteryDataSourceType
import br.com.mo2log.mobile.bluetooth.BluetoothConnectionStatus
import br.com.mo2log.mobile.bluetooth.SportX20BatteryState
import br.com.mo2log.mobile.bluetooth.StandardGattBatteryProtocol
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SportX20DiagnosticsFormatterTest {
    @Test
    fun masksBluetoothAddressWithStableFormat() {
        assertEquals("AA:BB:**:**:EE:FF", maskBluetoothAddress("aa:bb:cc:dd:ee:ff"))
    }

    @Test
    fun masksEveryBluetoothAddressInsideText() {
        val masked = maskBluetoothAddressesInText(
            "selecionado AA:BB:CC:DD:EE:FF e ignorado 10:20:30:40:50:60",
        )

        assertEquals(
            "selecionado AA:BB:**:**:EE:FF e ignorado 10:20:**:**:50:60",
            masked,
        )
    }

    @Test
    fun invalidOrMissingAddressIsUnavailable() {
        assertEquals(DIAGNOSTIC_UNAVAILABLE, maskBluetoothAddress(null))
        assertEquals(DIAGNOSTIC_UNAVAILABLE, maskBluetoothAddress("nao-e-mac"))
    }

    @Test
    fun nullValueIsUnavailableInsteadOfZero() {
        assertEquals(DIAGNOSTIC_UNAVAILABLE, diagnosticValue(null))
    }

    @Test
    fun translatesGattPropertiesInStableDocumentedOrder() {
        assertEquals(
            listOf(
                "READ",
                "WRITE",
                "WRITE_NO_RESPONSE",
                "NOTIFY",
                "INDICATE",
                "BROADCAST",
                "SIGNED_WRITE",
                "EXTENDED_PROPS",
            ),
            diagnosticGattPropertyNames(0xFF),
        )
    }

    @Test
    fun unknownUuidIsNeverInterpretedAsBattery() {
        val result = interpretGattValue(
            "12345678-1234-5678-1234-56789abcdef0",
            byteArrayOf(85.toByte()),
        )

        assertEquals("DESCONHECIDA", result.label)
        assertNull(result.officialBatteryPercent)
    }

    @Test
    fun officialBatteryLevelUuidIsInterpreted() {
        val result = interpretGattValue(
            StandardGattBatteryProtocol.BATTERY_LEVEL_UUID.toString(),
            byteArrayOf(85.toByte()),
        )

        assertEquals("BATTERY_LEVEL_OFICIAL (85%)", result.label)
        assertEquals(85, result.officialBatteryPercent)
    }

    @Test
    fun invalidOfficialBatteryLevelIsRejected() {
        val result = interpretGattValue(
            StandardGattBatteryProtocol.BATTERY_LEVEL_UUID.toString(),
            byteArrayOf(101.toByte()),
        )

        assertEquals("BATTERY_LEVEL_OFICIAL_INVALIDO", result.label)
        assertNull(result.officialBatteryPercent)
    }

    @Test
    fun readGuardPreventsConcurrentGattReads() {
        val guard = DiagnosticReadGuard()
        val firstToken = guard.tryStart()

        assertTrue(firstToken != null)
        assertNull(guard.tryStart())
        assertTrue(guard.finish(firstToken!!))
        assertTrue(guard.tryStart() != null)
    }

    @Test
    fun cancelledReadCannotCompleteWithOldToken() {
        val guard = DiagnosticReadGuard()
        val oldToken = guard.tryStart()!!

        guard.cancel()

        assertFalse(guard.finish(oldToken))
        assertFalse(guard.isRunning)
    }

    @Test
    fun diagnosticGattTimeoutRemainsEightSeconds() {
        assertEquals(8_000L, DIAGNOSTIC_GATT_TIMEOUT_MILLIS)
    }

    @Test
    fun clearingEventsPreservesRepositoryCacheAndGattEvidence() {
        val repository = SportX20BatteryState.create(
            deviceName = "soundcore Sport X20",
            deviceAddress = "AA:BB:CC:DD:EE:FF",
            connectionStatus = BluetoothConnectionStatus.CONNECTED,
            combinedBatteryPercent = 72,
            lastUpdatedAt = 123L,
            dataSource = BluetoothBatteryDataSourceType.PERSISTED_CACHE,
            isStale = true,
        )
        val state = sampleState(repository).copy(
            services = listOf(DiagnosticGattService("service", emptyList())),
            events = listOf(DiagnosticEvent(1L, 0L, "GATT", "evento")),
        )

        val cleared = state.clearTemporaryEvents()

        assertTrue(cleared.events.isEmpty())
        assertEquals(repository, cleared.repositoryState)
        assertEquals(state.services, cleared.services)
    }

    @Test
    fun eventOrderingIsStableByTimestampThenInsertion() {
        val timestamps = listOf(20L, 10L, 20L).iterator()
        val buffer = DiagnosticEventBuffer(clock = { timestamps.next() })
        buffer.add("GATT", "primeiro-em-20")
        buffer.add("GATT", "primeiro-em-10")
        buffer.add("GATT", "segundo-em-20")

        assertEquals(
            listOf("primeiro-em-10", "primeiro-em-20", "segundo-em-20"),
            buffer.snapshot().map { it.message },
        )
    }

    @Test
    fun copiedReportMasksAddressesAndShowsUnavailableFields() {
        val repository = SportX20BatteryState.create(
            deviceName = "soundcore Sport X20",
            deviceAddress = "AA:BB:CC:DD:EE:FF",
            connectionStatus = BluetoothConnectionStatus.CONNECTED_WITHOUT_BATTERY_DATA,
        )
        val state = sampleState(repository).copy(
            lastError = "falha em AA:BB:CC:DD:EE:FF",
            events = listOf(
                DiagnosticEvent(1L, 0L, "GATT", "callback AA:BB:CC:DD:EE:FF"),
            ),
        )

        val report = formatSportX20DiagnosticReport(state)

        assertFalse(report.contains("AA:BB:CC:DD:EE:FF"))
        assertTrue(report.contains("AA:BB:**:**:EE:FF"))
        assertTrue(report.contains("leftBatteryPercent: $DIAGNOSTIC_UNAVAILABLE"))
    }

    private fun sampleState(repository: SportX20BatteryState): SportX20DiagnosticScreenState {
        return SportX20DiagnosticScreenState(
            environment = DiagnosticEnvironment(
                appVersion = "debug",
                androidVersion = "14",
                androidApi = 34,
                manufacturer = "Fabricante",
                phoneModel = "Modelo",
                bluetoothAvailable = true,
                bluetoothEnabled = true,
                connectPermissionGranted = true,
                startedAt = 1L,
            ),
            device = DiagnosticDevice(
                name = "soundcore Sport X20",
                alias = null,
                maskedAddress = "AA:BB:**:**:EE:FF",
                deviceType = "DUAL",
                bluetoothClass = "AUDIO_VIDEO",
                bondState = "BONDED",
                transport = "AUTO",
                a2dpState = "CONNECTED",
                headsetState = "CONNECTED",
                aclState = "CONNECTED",
                candidateCount = 1,
                selectionOrigin = DiagnosticSelectionOrigin.SAVED_ADDRESS,
                candidates = listOf(
                    DiagnosticCandidate("soundcore Sport X20", "AA:BB:**:**:EE:FF"),
                ),
            ),
            repositoryState = repository,
        )
    }
}
