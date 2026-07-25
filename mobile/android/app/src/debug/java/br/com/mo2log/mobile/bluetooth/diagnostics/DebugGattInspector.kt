package br.com.mo2log.mobile.bluetooth.diagnostics

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Handler
import android.os.Looper
import java.util.ArrayDeque
import java.util.concurrent.atomic.AtomicBoolean

enum class GattInspectionOutcome {
    COMPLETED,
    TIMEOUT,
    CANCELLED,
    FAILED,
}

data class GattInspectionResult(
    val outcome: GattInspectionOutcome,
    val services: List<DiagnosticGattService>,
    val readings: List<DiagnosticGattRead>,
    val officialBatteryPercent: Int?,
    val errorMessage: String?,
)

class DebugGattInspector(
    context: Context,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val applicationContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private var activeOperation: InspectionOperation? = null

    @Synchronized
    fun inspect(
        device: BluetoothDevice,
        eventSink: (category: String, message: String) -> Unit,
        callback: (GattInspectionResult) -> Unit,
    ) {
        activeOperation?.cancel()
        lateinit var operation: InspectionOperation
        operation = InspectionOperation(
            device = device,
            eventSink = eventSink,
            callback = callback,
            onFinished = {
                synchronized(this) {
                    if (activeOperation === operation) activeOperation = null
                }
            },
        )
        activeOperation = operation
        operation.start()
    }

    @Synchronized
    fun cancel() {
        activeOperation?.cancel()
        activeOperation = null
    }

    @Synchronized
    fun isRunning(): Boolean = activeOperation != null

    fun close() = cancel()

    private inner class InspectionOperation(
        private val device: BluetoothDevice,
        private val eventSink: (category: String, message: String) -> Unit,
        private val callback: (GattInspectionResult) -> Unit,
        private val onFinished: () -> Unit,
    ) {
        private val completed = AtomicBoolean(false)
        private val lock = Any()
        private val readableQueue = ArrayDeque<ReadableCharacteristic>()
        private val readings = mutableListOf<DiagnosticGattRead>()
        private val officialBatteryValues = mutableListOf<Int>()
        private var services = emptyList<DiagnosticGattService>()
        private var currentRead: ReadableCharacteristic? = null
        private var bluetoothGatt: BluetoothGatt? = null
        private val timeout = Runnable {
            emit("TIMEOUT", "Tempo limite global de ${DIAGNOSTIC_GATT_TIMEOUT_MILLIS} ms atingido")
            finish(GattInspectionOutcome.TIMEOUT, "Tempo limite na inspecao GATT")
        }

        private val gattCallback = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                mainHandler.post {
                    runCallbackSafely("onConnectionStateChange") {
                        emit(
                            "CALLBACK",
                            "onConnectionStateChange status=$status newState=${connectionStateName(newState)}",
                        )
                        if (status != BluetoothGatt.GATT_SUCCESS) {
                            finish(GattInspectionOutcome.FAILED, "Falha de conexao GATT: $status")
                            return@runCallbackSafely
                        }
                        when (newState) {
                            BluetoothProfile.STATE_CONNECTED -> {
                                emit("GATT", "Conectado; iniciando descoberta de servicos")
                                val started = try {
                                    gatt.discoverServices()
                                } catch (_: RuntimeException) {
                                    false
                                }
                                if (!started) {
                                    finish(
                                        GattInspectionOutcome.FAILED,
                                        "Descoberta de servicos nao iniciada",
                                    )
                                }
                            }
                            BluetoothProfile.STATE_DISCONNECTED -> {
                                finish(
                                    GattInspectionOutcome.FAILED,
                                    "GATT desconectado antes da conclusao",
                                )
                            }
                        }
                    }
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                mainHandler.post {
                    runCallbackSafely("onServicesDiscovered") {
                        emit(
                            "CALLBACK",
                            "onServicesDiscovered status=$status services=${gatt.services.size}",
                        )
                        if (status != BluetoothGatt.GATT_SUCCESS) {
                            finish(GattInspectionOutcome.FAILED, "Falha ao descobrir servicos: $status")
                            return@runCallbackSafely
                        }
                        services = snapshotServices(gatt)
                        enqueueReadableCharacteristics(gatt)
                        if (readableQueue.isEmpty()) {
                            emit("GATT", "Nenhuma caracteristica publica com propriedade READ")
                            finish(GattInspectionOutcome.COMPLETED, null)
                        } else {
                            emit("GATT", "${readableQueue.size} caracteristicas READ na fila")
                            readNext(gatt)
                        }
                    }
                }
            }

            @Deprecated("Deprecated by Android 13")
            override fun onCharacteristicRead(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                status: Int,
            ) {
                @Suppress("DEPRECATION")
                val value = characteristic.value?.clone()
                mainHandler.post {
                    runCallbackSafely("onCharacteristicRead") {
                        handleCharacteristicRead(gatt, characteristic, value, status)
                    }
                }
            }

            override fun onCharacteristicRead(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                value: ByteArray,
                status: Int,
            ) {
                val safeValue = value.clone()
                mainHandler.post {
                    runCallbackSafely("onCharacteristicRead") {
                        handleCharacteristicRead(gatt, characteristic, safeValue, status)
                    }
                }
            }
        }

        fun start() {
            emit("GATT", "Inicio da conexao LE, autoConnect=false")
            mainHandler.postDelayed(timeout, DIAGNOSTIC_GATT_TIMEOUT_MILLIS)
            val openedGatt = try {
                device.connectGatt(
                    applicationContext,
                    false,
                    gattCallback,
                    BluetoothDevice.TRANSPORT_LE,
                )
            } catch (_: SecurityException) {
                null
            } catch (_: IllegalArgumentException) {
                null
            }
            if (openedGatt == null) {
                finish(GattInspectionOutcome.FAILED, "Conexao GATT nao iniciada")
                return
            }
            synchronized(lock) {
                if (completed.get()) safeCloseGatt(openedGatt) else bluetoothGatt = openedGatt
            }
        }

        fun cancel() {
            emit("GATT", "Inspecao cancelada")
            finish(GattInspectionOutcome.CANCELLED, null)
        }

        private fun snapshotServices(gatt: BluetoothGatt): List<DiagnosticGattService> {
            return gatt.services
                .sortedBy { it.uuid.toString() }
                .map { service ->
                    emit("SERVICO", service.uuid.toString())
                    val characteristics = service.characteristics
                        .sortedBy { it.uuid.toString() }
                        .map { characteristic ->
                            val propertyNames = diagnosticGattPropertyNames(characteristic.properties)
                            emit(
                                "CARACTERISTICA",
                                "${characteristic.uuid} properties=${propertyNames.joinToString(",")}",
                            )
                            val descriptors = characteristic.descriptors
                                .sortedBy { it.uuid.toString() }
                                .map { descriptor ->
                                    emit("DESCRITOR", "${characteristic.uuid} -> ${descriptor.uuid}")
                                    DiagnosticGattDescriptor(descriptor.uuid.toString())
                                }
                            DiagnosticGattCharacteristic(
                                uuid = characteristic.uuid.toString(),
                                properties = characteristic.properties,
                                propertyNames = propertyNames,
                                descriptors = descriptors,
                            )
                        }
                    DiagnosticGattService(service.uuid.toString(), characteristics)
                }
        }

        private fun enqueueReadableCharacteristics(gatt: BluetoothGatt) {
            gatt.services
                .sortedBy { it.uuid.toString() }
                .forEach { service ->
                    service.characteristics
                        .filter { characteristic ->
                            characteristic.properties and BluetoothGattCharacteristic.PROPERTY_READ != 0
                        }
                        .sortedBy { it.uuid.toString() }
                        .forEach { characteristic ->
                            readableQueue += ReadableCharacteristic(
                                serviceUuid = service.uuid.toString(),
                                characteristic = characteristic,
                            )
                        }
                }
        }

        private fun readNext(gatt: BluetoothGatt) {
            while (!completed.get() && readableQueue.isNotEmpty()) {
                val next = readableQueue.removeFirst()
                currentRead = next
                emit("READ", "Iniciando leitura ${next.characteristic.uuid}")
                @Suppress("DEPRECATION")
                val started = try {
                    gatt.readCharacteristic(next.characteristic)
                } catch (_: RuntimeException) {
                    false
                }
                if (started) return

                emit("READ", "Leitura nao iniciada ${next.characteristic.uuid}")
                readings += DiagnosticGattRead(
                    serviceUuid = next.serviceUuid,
                    characteristicUuid = next.characteristic.uuid.toString(),
                    status = "READ_NOT_STARTED",
                    rawHex = null,
                    decimalValues = null,
                    interpretation = "DESCONHECIDA",
                    timestamp = clock(),
                )
                currentRead = null
            }
            if (!completed.get()) finish(GattInspectionOutcome.COMPLETED, null)
        }

        private fun handleCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            payload: ByteArray?,
            status: Int,
        ) {
            if (completed.get()) return
            val current = currentRead
            val serviceUuid = current?.serviceUuid
                ?: characteristic.service?.uuid?.toString()
                ?: DIAGNOSTIC_UNAVAILABLE
            val safePayload = payload?.clone()
            val interpretation = if (status == BluetoothGatt.GATT_SUCCESS) {
                interpretGattValue(characteristic.uuid.toString(), safePayload)
            } else {
                DiagnosticInterpretation("DESCONHECIDA", null)
            }
            val read = DiagnosticGattRead(
                serviceUuid = serviceUuid,
                characteristicUuid = characteristic.uuid.toString(),
                status = if (status == BluetoothGatt.GATT_SUCCESS) {
                    "GATT_SUCCESS (0)"
                } else {
                    "GATT_STATUS_$status"
                },
                rawHex = diagnosticHex(safePayload),
                decimalValues = diagnosticDecimal(safePayload),
                interpretation = interpretation.label,
                timestamp = clock(),
            )
            readings += read
            interpretation.officialBatteryPercent?.let { officialBatteryValues += it }
            emit(
                "CALLBACK",
                "onCharacteristicRead uuid=${characteristic.uuid} status=$status " +
                    "hex=${diagnosticValue(read.rawHex)} decimal=${diagnosticValue(read.decimalValues)} " +
                    "interpretacao=${read.interpretation}",
            )
            currentRead = null
            readNext(gatt)
        }

        private inline fun runCallbackSafely(callbackName: String, action: () -> Unit) {
            if (completed.get()) return
            try {
                action()
            } catch (error: RuntimeException) {
                emit("CALLBACK", "$callbackName falhou: ${error.javaClass.simpleName}")
                finish(GattInspectionOutcome.FAILED, "Falha recuperavel em $callbackName")
            }
        }

        private fun finish(outcome: GattInspectionOutcome, errorMessage: String?) {
            if (!completed.compareAndSet(false, true)) return
            mainHandler.removeCallbacks(timeout)
            val gatt = synchronized(lock) {
                bluetoothGatt.also { bluetoothGatt = null }
            }
            gatt?.let { safeCloseGatt(it) }
            val distinctBatteryValues = officialBatteryValues.distinct()
            val officialBattery = distinctBatteryValues.singleOrNull()
            val conflictMessage = if (distinctBatteryValues.size > 1) {
                "Battery Level padrao retornou valores conflitantes na mesma sessao"
            } else {
                null
            }
            val finalError = errorMessage ?: conflictMessage
            emit("GATT", "Encerramento outcome=$outcome error=${diagnosticValue(finalError)}")
            val result = GattInspectionResult(
                outcome = outcome,
                services = services,
                readings = readings.toList(),
                officialBatteryPercent = officialBattery,
                errorMessage = finalError,
            )
            onFinished()
            mainHandler.post { callback(result) }
        }

        private fun safeCloseGatt(gatt: BluetoothGatt) {
            emit("GATT", "Solicitando disconnect")
            try {
                gatt.disconnect()
            } catch (_: RuntimeException) {
                emit("GATT", "disconnect indisponivel; close sera executado")
            } finally {
                try {
                    gatt.close()
                    emit("GATT", "BluetoothGatt fechado")
                } catch (_: RuntimeException) {
                    emit("GATT", "Falha recuperavel ao fechar BluetoothGatt")
                }
            }
        }

        private fun emit(category: String, message: String) {
            eventSink(category, message)
        }

        private fun connectionStateName(state: Int): String {
            return when (state) {
                BluetoothProfile.STATE_CONNECTED -> "CONNECTED"
                BluetoothProfile.STATE_CONNECTING -> "CONNECTING"
                BluetoothProfile.STATE_DISCONNECTED -> "DISCONNECTED"
                BluetoothProfile.STATE_DISCONNECTING -> "DISCONNECTING"
                else -> "UNKNOWN($state)"
            }
        }
    }

    private data class ReadableCharacteristic(
        val serviceUuid: String,
        val characteristic: BluetoothGattCharacteristic,
    )
}
