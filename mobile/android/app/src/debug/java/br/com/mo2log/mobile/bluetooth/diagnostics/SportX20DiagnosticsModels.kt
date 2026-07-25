package br.com.mo2log.mobile.bluetooth.diagnostics

import br.com.mo2log.mobile.bluetooth.SportX20BatteryState

const val DIAGNOSTIC_GATT_TIMEOUT_MILLIS = 8_000L
const val DIAGNOSTIC_UNAVAILABLE = "Indispon\u00edvel"

enum class DiagnosticSelectionOrigin {
    SAVED_ADDRESS,
    NORMALIZED_NAME,
    MULTIPLE_CANDIDATES,
    NONE,
}

data class DiagnosticEnvironment(
    val appVersion: String,
    val androidVersion: String,
    val androidApi: Int,
    val manufacturer: String,
    val phoneModel: String,
    val bluetoothAvailable: Boolean,
    val bluetoothEnabled: Boolean?,
    val connectPermissionGranted: Boolean,
    val startedAt: Long,
)

data class DiagnosticCandidate(
    val name: String?,
    val maskedAddress: String,
)

data class DiagnosticDevice(
    val name: String?,
    val alias: String?,
    val maskedAddress: String?,
    val deviceType: String?,
    val bluetoothClass: String?,
    val bondState: String?,
    val transport: String?,
    val a2dpState: String?,
    val headsetState: String?,
    val aclState: String?,
    val candidateCount: Int,
    val selectionOrigin: DiagnosticSelectionOrigin,
    val candidates: List<DiagnosticCandidate>,
)

data class DiagnosticGattDescriptor(
    val uuid: String,
)

data class DiagnosticGattCharacteristic(
    val uuid: String,
    val properties: Int,
    val propertyNames: List<String>,
    val descriptors: List<DiagnosticGattDescriptor>,
)

data class DiagnosticGattService(
    val uuid: String,
    val characteristics: List<DiagnosticGattCharacteristic>,
)

data class DiagnosticGattRead(
    val serviceUuid: String,
    val characteristicUuid: String,
    val status: String,
    val rawHex: String?,
    val decimalValues: String?,
    val interpretation: String,
    val timestamp: Long,
)

data class DiagnosticEvent(
    val timestamp: Long,
    val sequence: Long,
    val category: String,
    val message: String,
)

data class DiagnosticInterpretation(
    val label: String,
    val officialBatteryPercent: Int?,
)

data class SportX20DiagnosticScreenState(
    val environment: DiagnosticEnvironment,
    val device: DiagnosticDevice,
    val repositoryState: SportX20BatteryState,
    val services: List<DiagnosticGattService> = emptyList(),
    val readings: List<DiagnosticGattRead> = emptyList(),
    val events: List<DiagnosticEvent> = emptyList(),
    val isLoading: Boolean = false,
    val lastError: String? = null,
) {
    fun clearTemporaryEvents(): SportX20DiagnosticScreenState = copy(events = emptyList())
}

class DiagnosticEventBuffer(
    private val clock: () -> Long = System::currentTimeMillis,
    private val maxEvents: Int = 600,
) {
    private val events = mutableListOf<DiagnosticEvent>()
    private var sequence = 0L

    @Synchronized
    fun add(category: String, message: String): DiagnosticEvent {
        val event = DiagnosticEvent(
            timestamp = clock(),
            sequence = sequence++,
            category = category.trim().ifEmpty { "GERAL" },
            message = maskBluetoothAddressesInText(message),
        )
        events += event
        if (events.size > maxEvents) events.removeAt(0)
        return event
    }

    @Synchronized
    fun snapshot(): List<DiagnosticEvent> {
        return events.sortedWith(compareBy(DiagnosticEvent::timestamp, DiagnosticEvent::sequence))
    }

    @Synchronized
    fun clear() {
        events.clear()
    }
}

class DiagnosticReadGuard {
    private var generation = 0L
    var isRunning: Boolean = false
        private set

    @Synchronized
    fun tryStart(): Long? {
        if (isRunning) return null
        isRunning = true
        generation += 1L
        return generation
    }

    @Synchronized
    fun finish(token: Long): Boolean {
        if (!isRunning || token != generation) return false
        isRunning = false
        return true
    }

    @Synchronized
    fun cancel() {
        generation += 1L
        isRunning = false
    }
}
