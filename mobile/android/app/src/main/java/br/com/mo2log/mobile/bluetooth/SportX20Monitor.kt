package br.com.mo2log.mobile.bluetooth

import android.content.Context
import android.content.SharedPreferences

fun interface SportX20StateListener {
    fun onStateChanged(state: SportX20BatteryState)
}

fun interface BluetoothCallbackDispatcher {
    fun dispatch(action: () -> Unit)
}

class SportX20Monitor(
    private val platform: BluetoothPlatform,
    private val standardBatteryDataSource: BluetoothBatteryDataSource,
    private val sportX20BatteryDataSource: BluetoothBatteryDataSource,
    private val stateStore: SportX20StatePersistence,
    private val deviceLocator: SportX20DeviceLocator = SportX20DeviceLocator(),
    private val dispatcher: BluetoothCallbackDispatcher = BluetoothCallbackDispatcher { it() },
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val listeners = linkedSetOf<SportX20StateListener>()
    private var cachedBatteryState = stateStore.loadLastBatteryState()
    private var started = false
    private var closed = false
    private var readInProgress = false
    private var readGeneration = 0L
    private var activeRead: BluetoothOperation? = null
    private var selectedDevice: BluetoothDeviceCandidate? = null

    @Volatile
    private var state = cachedBatteryState
        ?: SportX20BatteryState.empty(BluetoothConnectionStatus.DEVICE_NOT_PAIRED)

    private val platformObserver = BluetoothPlatformObserver {
        dispatcher.dispatch {
            if (started && !closed) refreshInternal(forceBatteryRead = false)
        }
    }

    fun currentState(): SportX20BatteryState = state

    fun addListener(listener: SportX20StateListener) {
        dispatcher.dispatch {
            if (closed) return@dispatch
            val added = listeners.add(listener)
            if (added) listener.onStateChanged(state)
        }
    }

    fun removeListener(listener: SportX20StateListener) {
        dispatcher.dispatch { listeners.remove(listener) }
    }

    fun start() {
        dispatcher.dispatch {
            if (closed || started) return@dispatch
            started = true
            platform.startObserving(platformObserver)
            refreshInternal(forceBatteryRead = true)
        }
    }

    fun stop() {
        dispatcher.dispatch {
            if (!started) return@dispatch
            started = false
            cancelActiveRead()
            platform.stopObserving()
        }
    }

    fun refresh() {
        dispatcher.dispatch {
            if (started && !closed) refreshInternal(forceBatteryRead = true)
        }
    }

    fun onPermissionResult() {
        dispatcher.dispatch {
            if (started && !closed) {
                platform.startObserving(platformObserver)
                refreshInternal(forceBatteryRead = true)
            }
        }
    }

    fun close() {
        dispatcher.dispatch {
            if (closed) return@dispatch
            if (started) {
                started = false
                cancelActiveRead()
                platform.stopObserving()
            } else {
                cancelActiveRead()
            }
            closed = true
            listeners.clear()
            standardBatteryDataSource.close()
            sportX20BatteryDataSource.close()
            platform.close()
        }
    }

    private fun refreshInternal(forceBatteryRead: Boolean) {
        if (!platform.hasBluetoothAdapter()) {
            cancelActiveRead()
            selectedDevice = null
            emit(cachedStateWith(BluetoothConnectionStatus.BLUETOOTH_UNAVAILABLE))
            return
        }
        if (!platform.hasConnectPermission()) {
            cancelActiveRead()
            selectedDevice = null
            emit(cachedStateWith(BluetoothConnectionStatus.PERMISSION_REQUIRED))
            return
        }
        if (!platform.isBluetoothEnabled()) {
            cancelActiveRead()
            selectedDevice = null
            emit(cachedStateWith(BluetoothConnectionStatus.BLUETOOTH_DISABLED))
            return
        }

        when (val selection = deviceLocator.locate(platform.bondedDevices(), stateStore.selectedAddress())) {
            SportX20DeviceSelection.NotFound -> {
                cancelActiveRead()
                selectedDevice = null
                emit(cachedStateWith(BluetoothConnectionStatus.DEVICE_NOT_PAIRED))
            }
            is SportX20DeviceSelection.MultipleCandidates -> {
                cancelActiveRead()
                selectedDevice = null
                emit(
                    cachedStateWith(
                        status = BluetoothConnectionStatus.MULTIPLE_CANDIDATES,
                        errorMessage = "Mais de um Sport X20 pareado",
                    ),
                )
            }
            is SportX20DeviceSelection.Found -> {
                val device = selection.device
                selectedDevice = device
                if (!stateStore.selectedAddress().equals(device.address, ignoreCase = true) ||
                    stateStore.selectedName() != device.name
                ) {
                    stateStore.saveSelectedDevice(device)
                }
                when (platform.connectionState(device)) {
                    BluetoothPlatformConnectionState.CONNECTED -> {
                        handleConnectedDevice(device, forceBatteryRead)
                    }
                    BluetoothPlatformConnectionState.CONNECTING -> {
                        cancelActiveRead()
                        emit(cachedStateWith(BluetoothConnectionStatus.CONNECTING, device))
                    }
                    BluetoothPlatformConnectionState.DISCONNECTED,
                    BluetoothPlatformConnectionState.UNKNOWN -> {
                        cancelActiveRead()
                        emit(cachedStateWith(BluetoothConnectionStatus.PAIRED_DISCONNECTED, device))
                    }
                }
            }
        }
    }

    private fun handleConnectedDevice(device: BluetoothDeviceCandidate, forceBatteryRead: Boolean) {
        if (readInProgress) return
        val currentMatchesDevice = state.deviceAddress.equals(device.address, ignoreCase = true)
        val hasFreshBattery = currentMatchesDevice && state.hasBatteryData() && !state.isStale
        val hasCompletedAttempt = currentMatchesDevice &&
            state.connectionStatus == BluetoothConnectionStatus.CONNECTED_WITHOUT_BATTERY_DATA

        if (!forceBatteryRead && (hasFreshBattery || hasCompletedAttempt)) {
            val status = if (hasFreshBattery) {
                BluetoothConnectionStatus.CONNECTED
            } else {
                BluetoothConnectionStatus.CONNECTED_WITHOUT_BATTERY_DATA
            }
            emit(state.copy(connectionStatus = status, errorMessage = safeBluetoothErrorMessage(state.errorMessage)))
            return
        }

        val waitingState = if (hasFreshBattery) {
            state.copy(connectionStatus = BluetoothConnectionStatus.CONNECTED, errorMessage = null)
        } else {
            cachedStateWith(BluetoothConnectionStatus.CONNECTED_WITHOUT_BATTERY_DATA, device)
        }
        emit(waitingState)
        beginStandardBatteryRead(device)
    }

    private fun beginStandardBatteryRead(device: BluetoothDeviceCandidate) {
        if (readInProgress) return
        readInProgress = true
        val generation = ++readGeneration
        val operation = standardBatteryDataSource.readBattery(device) { result ->
            dispatcher.dispatch { handleStandardBatteryResult(generation, device, result) }
        }
        if (readInProgress && generation == readGeneration) {
            activeRead = operation
        } else {
            operation.cancel()
        }
    }

    private fun handleStandardBatteryResult(
        generation: Long,
        device: BluetoothDeviceCandidate,
        result: BluetoothBatteryReadResult,
    ) {
        if (!isCurrentRead(generation, device)) return
        activeRead = null
        when (result) {
            is BluetoothBatteryReadResult.Success -> {
                if (result.values.hasData() &&
                    result.dataSource != BluetoothBatteryDataSourceType.SPORT_X20_PROPRIETARY_PROTOCOL
                ) {
                    readInProgress = false
                    publishSuccessfulRead(device, result)
                } else {
                    beginProprietaryFallback(generation, device, "Dados de bateria invalidos")
                }
            }
            BluetoothBatteryReadResult.Unavailable -> {
                beginProprietaryFallback(generation, device, null)
            }
            BluetoothBatteryReadResult.Timeout -> {
                beginProprietaryFallback(generation, device, "Tempo limite na leitura de bateria")
            }
            BluetoothBatteryReadResult.InvalidData -> {
                beginProprietaryFallback(generation, device, "Nivel de bateria GATT invalido")
            }
            is BluetoothBatteryReadResult.Failure -> {
                beginProprietaryFallback(generation, device, result.message)
            }
            BluetoothBatteryReadResult.Cancelled -> {
                readInProgress = false
            }
        }
    }

    private fun beginProprietaryFallback(
        generation: Long,
        device: BluetoothDeviceCandidate,
        publicReadMessage: String?,
    ) {
        if (!isCurrentRead(generation, device)) return
        val operation = sportX20BatteryDataSource.readBattery(device) { result ->
            dispatcher.dispatch {
                handleProprietaryFallbackResult(generation, device, publicReadMessage, result)
            }
        }
        if (readInProgress && generation == readGeneration) {
            activeRead = operation
        } else {
            operation.cancel()
        }
    }

    private fun handleProprietaryFallbackResult(
        generation: Long,
        device: BluetoothDeviceCandidate,
        publicReadMessage: String?,
        result: BluetoothBatteryReadResult,
    ) {
        if (!isCurrentRead(generation, device)) return
        readInProgress = false
        activeRead = null

        // Proprietary data is deliberately rejected in this phase, even if supplied by a test double.
        val fallbackMessage = when (result) {
            is BluetoothBatteryReadResult.Failure -> result.message
            BluetoothBatteryReadResult.Timeout -> "Tempo limite no provider do Sport X20"
            BluetoothBatteryReadResult.InvalidData -> "Dados proprietarios invalidos"
            else -> publicReadMessage
        }
        emit(
            cachedStateWith(
                status = BluetoothConnectionStatus.CONNECTED_WITHOUT_BATTERY_DATA,
                device = device,
                errorMessage = fallbackMessage,
            ),
        )
    }

    private fun publishSuccessfulRead(
        device: BluetoothDeviceCandidate,
        result: BluetoothBatteryReadResult.Success,
    ) {
        val values = result.values
        val freshState = SportX20BatteryState.create(
            deviceName = device.name,
            deviceAddress = device.address,
            connectionStatus = BluetoothConnectionStatus.CONNECTED,
            leftBatteryPercent = values.leftBatteryPercent,
            rightBatteryPercent = values.rightBatteryPercent,
            combinedBatteryPercent = values.combinedBatteryPercent,
            caseBatteryPercent = values.caseBatteryPercent,
            caseBatteryRange = values.caseBatteryRange,
            lastUpdatedAt = clock(),
            dataSource = result.dataSource,
            isStale = false,
        )
        stateStore.saveBatteryState(freshState)
        cachedBatteryState = freshState.copy(
            connectionStatus = BluetoothConnectionStatus.PAIRED_DISCONNECTED,
            dataSource = BluetoothBatteryDataSourceType.PERSISTED_CACHE,
            isStale = true,
        )
        emit(freshState)
    }

    private fun cachedStateWith(
        status: BluetoothConnectionStatus,
        device: BluetoothDeviceCandidate? = null,
        errorMessage: String? = null,
    ): SportX20BatteryState {
        val cached = cachedBatteryState
        return SportX20BatteryState.create(
            deviceName = device?.name ?: cached?.deviceName,
            deviceAddress = device?.address ?: cached?.deviceAddress,
            connectionStatus = status,
            leftBatteryPercent = cached?.leftBatteryPercent,
            rightBatteryPercent = cached?.rightBatteryPercent,
            combinedBatteryPercent = cached?.combinedBatteryPercent,
            caseBatteryPercent = cached?.caseBatteryPercent,
            caseBatteryRange = cached?.caseBatteryRange,
            lastUpdatedAt = cached?.lastUpdatedAt,
            dataSource = if (cached?.hasBatteryData() == true) {
                BluetoothBatteryDataSourceType.PERSISTED_CACHE
            } else {
                BluetoothBatteryDataSourceType.NONE
            },
            isStale = cached?.hasBatteryData() == true,
            errorMessage = errorMessage,
        )
    }

    private fun isCurrentRead(generation: Long, device: BluetoothDeviceCandidate): Boolean {
        return started &&
            !closed &&
            readInProgress &&
            generation == readGeneration &&
            selectedDevice?.address.equals(device.address, ignoreCase = true)
    }

    private fun cancelActiveRead() {
        readGeneration += 1L
        readInProgress = false
        activeRead?.cancel()
        activeRead = null
    }

    private fun emit(newState: SportX20BatteryState) {
        if (state == newState) return
        state = newState
        listeners.toList().forEach { it.onStateChanged(newState) }
    }

    companion object {
        fun create(
            applicationContext: Context,
            preferences: SharedPreferences,
            dispatcher: BluetoothCallbackDispatcher,
        ): SportX20Monitor {
            val appContext = applicationContext.applicationContext
            val platform = AndroidBluetoothPlatform(appContext)
            return SportX20Monitor(
                platform = platform,
                standardBatteryDataSource = StandardGattBatteryDataSource(platform),
                sportX20BatteryDataSource = SportX20BatteryDataSource(),
                stateStore = SportX20StateStore(preferences),
                dispatcher = dispatcher,
            )
        }
    }
}
