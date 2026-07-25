package br.com.mo2log.mobile.bluetooth.diagnostics

import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.Build
import android.os.Handler
import android.os.Looper
import br.com.mo2log.mobile.bluetooth.BluetoothBatteryDataSourceType
import br.com.mo2log.mobile.bluetooth.BluetoothConnectionStatus
import br.com.mo2log.mobile.bluetooth.BluetoothDeviceCandidate
import br.com.mo2log.mobile.bluetooth.BluetoothPermissionController
import br.com.mo2log.mobile.bluetooth.SportX20BatteryState
import br.com.mo2log.mobile.bluetooth.SportX20DeviceLocator
import br.com.mo2log.mobile.bluetooth.SportX20DeviceSelection
import br.com.mo2log.mobile.bluetooth.SportX20StateStore
import br.com.mo2log.mobile.bluetooth.safeBluetoothErrorMessage
import java.util.Locale

enum class DiagnosticRefreshRequest {
    STARTED,
    PERMISSION_REQUIRED,
    BLUETOOTH_UNAVAILABLE,
    BLUETOOTH_DISABLED,
    DEVICE_NOT_FOUND,
    MULTIPLE_CANDIDATES,
}

class SportX20DiagnosticsController(
    context: Context,
    preferences: SharedPreferences,
    private val onStateChanged: (SportX20DiagnosticScreenState) -> Unit,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val applicationContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val permissionController = BluetoothPermissionController.from(applicationContext)
    private val bluetoothManager = applicationContext.getSystemService(BluetoothManager::class.java)
    private val locator = SportX20DeviceLocator()
    private val stateStore = SportX20StateStore(preferences)
    private val eventBuffer = DiagnosticEventBuffer(clock)
    private val readGuard = DiagnosticReadGuard()
    private val gattInspector = DebugGattInspector(applicationContext, clock)
    private val startedAt = clock()
    private val requestedProfiles = mutableSetOf<Int>()

    private var started = false
    private var closed = false
    private var receiverRegistered = false
    private var a2dpProxy: BluetoothProfile? = null
    private var headsetProxy: BluetoothProfile? = null
    private var selectedDevice: BluetoothDevice? = null
    private var selectedCandidate: BluetoothDeviceCandidate? = null
    private var selectedOrigin = DiagnosticSelectionOrigin.NONE
    private var relevantCandidates = emptyList<BluetoothDeviceCandidate>()
    private var aclState: String? = null
    private var latestRepositoryState = stateStore.loadLastBatteryState()

    private var state = SportX20DiagnosticScreenState(
        environment = buildEnvironment(),
        device = emptyDevice(),
        repositoryState = latestRepositoryState
            ?: SportX20BatteryState.empty(BluetoothConnectionStatus.DEVICE_NOT_PAIRED),
    )

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val currentIntent = intent ?: return
            when (currentIntent.action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val adapterState = currentIntent.getIntExtra(
                        BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR,
                    )
                    addEvent("BLUETOOTH", "Adapter state=${adapterStateName(adapterState)}")
                    if (adapterState == BluetoothAdapter.STATE_ON) {
                        requestProfileProxies()
                    } else {
                        closeProfileProxies()
                        requestedProfiles.clear()
                        aclState = null
                    }
                    refreshSnapshot()
                }
                BluetoothDevice.ACTION_ACL_CONNECTED,
                BluetoothDevice.ACTION_ACL_DISCONNECTED,
                BluetoothDevice.ACTION_BOND_STATE_CHANGED,
                BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED,
                BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED -> {
                    val device = currentIntent.bluetoothDevice() ?: return
                    if (!isSelectedDevice(device)) return
                    when (currentIntent.action) {
                        BluetoothDevice.ACTION_ACL_CONNECTED -> {
                            aclState = "CONNECTED"
                            addEvent("ACL", "Sport X20 conectado")
                        }
                        BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                            aclState = "DISCONNECTED"
                            addEvent("ACL", "Sport X20 desconectado")
                        }
                        BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                            addEvent(
                                "BOND",
                                "Estado=${bondStateName(currentIntent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1))}",
                            )
                        }
                        BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED -> {
                            addEvent(
                                "A2DP",
                                "Estado=${profileStateName(currentIntent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1))}",
                            )
                        }
                        BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED -> {
                            addEvent(
                                "HEADSET",
                                "Estado=${profileStateName(currentIntent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1))}",
                            )
                        }
                    }
                    refreshSnapshot()
                }
            }
        }
    }

    private val profileListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
            if (proxy == null) return
            if (!started || closed) {
                closeProfileProxy(profile, proxy)
                return
            }
            when (profile) {
                BluetoothProfile.A2DP -> a2dpProxy = proxy
                BluetoothProfile.HEADSET -> headsetProxy = proxy
                else -> closeProfileProxy(profile, proxy)
            }
            addEvent("PERFIL", "Proxy ${profileName(profile)} disponivel")
            refreshSnapshot()
        }

        override fun onServiceDisconnected(profile: Int) {
            when (profile) {
                BluetoothProfile.A2DP -> a2dpProxy = null
                BluetoothProfile.HEADSET -> headsetProxy = null
            }
            requestedProfiles -= profile
            addEvent("PERFIL", "Proxy ${profileName(profile)} desconectado")
            refreshSnapshot()
        }
    }

    fun currentState(): SportX20DiagnosticScreenState = state

    fun requiredPermissions(): Array<String> = permissionController.requiredPermissions().toTypedArray()

    fun hasConnectPermission(): Boolean = permissionController.hasRequiredPermissions()

    fun start() {
        if (closed || started) return
        started = true
        addEvent("SESSAO", "Diagnostico iniciado; nenhuma leitura automatica executada")
        refreshSnapshot()
        registerReceiver()
        requestProfileProxies()
    }

    fun stop() {
        if (!started) return
        started = false
        cancelInspection()
        unregisterReceiver()
        closeProfileProxies()
        requestedProfiles.clear()
        state = state.copy(isLoading = false, events = eventBuffer.snapshot())
        publish()
    }

    fun close() {
        if (closed) return
        stop()
        closed = true
        gattInspector.close()
    }

    fun refreshAndInspect(): DiagnosticRefreshRequest {
        if (closed) return DiagnosticRefreshRequest.BLUETOOTH_UNAVAILABLE
        if (!started) start()
        refreshSnapshot()

        val environment = state.environment
        if (!environment.bluetoothAvailable) {
            addEvent("VALIDACAO", "Bluetooth indisponivel neste aparelho")
            return DiagnosticRefreshRequest.BLUETOOTH_UNAVAILABLE
        }
        if (!environment.connectPermissionGranted) {
            addEvent("VALIDACAO", "Permissao BLUETOOTH_CONNECT necessaria")
            return DiagnosticRefreshRequest.PERMISSION_REQUIRED
        }
        if (environment.bluetoothEnabled != true) {
            addEvent("VALIDACAO", "Bluetooth desligado")
            return DiagnosticRefreshRequest.BLUETOOTH_DISABLED
        }
        if (selectedOrigin == DiagnosticSelectionOrigin.MULTIPLE_CANDIDATES) {
            addEvent("VALIDACAO", "Multiplos candidatos; leitura nao iniciada")
            return DiagnosticRefreshRequest.MULTIPLE_CANDIDATES
        }
        val device = selectedDevice
        if (device == null) {
            addEvent("VALIDACAO", "Sport X20 pareado nao encontrado")
            return DiagnosticRefreshRequest.DEVICE_NOT_FOUND
        }

        if (gattInspector.isRunning()) {
            addEvent("GATT", "Tentativa anterior sera cancelada antes da nova leitura")
            gattInspector.cancel()
            readGuard.cancel()
        }
        val token = readGuard.tryStart()
        if (token == null) {
            addEvent("GATT", "Nova leitura bloqueada para evitar duas conexoes simultaneas")
            return DiagnosticRefreshRequest.STARTED
        }

        addEvent("SESSAO", "Atualizacao manual solicitada")
        state = state.copy(isLoading = true, lastError = null, events = eventBuffer.snapshot())
        publish()
        gattInspector.inspect(
            device = device,
            eventSink = { category, message -> addEvent(category, message) },
        ) { result ->
            if (!readGuard.finish(token)) return@inspect
            handleInspectionResult(result)
        }
        return DiagnosticRefreshRequest.STARTED
    }

    fun clearTemporaryEvents() {
        eventBuffer.clear()
        state = state.clearTemporaryEvents()
        publish()
    }

    fun copyableReport(): String = formatSportX20DiagnosticReport(
        state.copy(events = eventBuffer.snapshot()),
    )

    private fun handleInspectionResult(result: GattInspectionResult) {
        val candidate = selectedCandidate
        val percent = result.officialBatteryPercent
        if (candidate != null && percent != null) {
            val freshState = SportX20BatteryState.create(
                deviceName = candidate.name,
                deviceAddress = candidate.address,
                connectionStatus = BluetoothConnectionStatus.CONNECTED,
                combinedBatteryPercent = percent,
                lastUpdatedAt = clock(),
                dataSource = BluetoothBatteryDataSourceType.STANDARD_GATT_BATTERY_SERVICE,
                isStale = false,
            )
            stateStore.saveBatteryState(freshState)
            latestRepositoryState = freshState
            addEvent("BATTERY", "Battery Level padrao confirmado: $percent%")
        } else {
            latestRepositoryState = buildRepositoryState(
                forceConnectedWithoutBattery = result.outcome == GattInspectionOutcome.COMPLETED,
                errorMessage = result.errorMessage,
            )
        }
        state = state.copy(
            repositoryState = latestRepositoryState ?: state.repositoryState,
            services = result.services,
            readings = result.readings,
            events = eventBuffer.snapshot(),
            isLoading = false,
            lastError = result.errorMessage,
        )
        publish()
    }

    private fun refreshSnapshot() {
        resolveSelectedDevice()
        val environment = buildEnvironment()
        val device = buildDeviceSnapshot()
        val repository = buildRepositoryState()
        latestRepositoryState = repository
        state = state.copy(
            environment = environment,
            device = device,
            repositoryState = repository,
            events = eventBuffer.snapshot(),
        )
        publish()
    }

    private fun resolveSelectedDevice() {
        selectedDevice = null
        selectedCandidate = null
        selectedOrigin = DiagnosticSelectionOrigin.NONE
        relevantCandidates = emptyList()
        if (!hasConnectPermission()) return
        val adapter = bluetoothManager?.adapter ?: return
        val bondedDevices = try {
            adapter.bondedDevices.toList()
        } catch (_: SecurityException) {
            return
        }
        val candidates = bondedDevices.mapNotNull { it.toCandidateOrNull() }
        val savedAddress = stateStore.selectedAddress()
        val savedMatch = savedAddress?.let { address ->
            candidates.firstOrNull { it.address.equals(address, ignoreCase = true) }
        }
        val namedMatches = candidates.filter { locator.isSportX20Name(it.name) }
        relevantCandidates = (namedMatches + listOfNotNull(savedMatch)).distinctBy { it.address.uppercase(Locale.ROOT) }

        when (val selection = locator.locate(candidates, savedAddress)) {
            SportX20DeviceSelection.NotFound -> Unit
            is SportX20DeviceSelection.MultipleCandidates -> {
                selectedOrigin = DiagnosticSelectionOrigin.MULTIPLE_CANDIDATES
                relevantCandidates = selection.devices
            }
            is SportX20DeviceSelection.Found -> {
                selectedCandidate = selection.device
                selectedOrigin = if (savedMatch != null &&
                    savedMatch.address.equals(selection.device.address, ignoreCase = true)
                ) {
                    DiagnosticSelectionOrigin.SAVED_ADDRESS
                } else {
                    DiagnosticSelectionOrigin.NORMALIZED_NAME
                }
                selectedDevice = bondedDevices.firstOrNull { bluetoothDevice ->
                    bluetoothDevice.safeAddress()?.equals(selection.device.address, ignoreCase = true) == true
                }
                if (!savedAddress.equals(selection.device.address, ignoreCase = true) ||
                    stateStore.selectedName() != selection.device.name
                ) {
                    stateStore.saveSelectedDevice(selection.device)
                }
            }
        }
    }

    private fun buildEnvironment(): DiagnosticEnvironment {
        val adapter = bluetoothManager?.adapter
        val permissionGranted = hasConnectPermission()
        val bluetoothEnabled = if (adapter == null || !permissionGranted) {
            null
        } else {
            try {
                adapter.isEnabled
            } catch (_: SecurityException) {
                null
            }
        }
        return DiagnosticEnvironment(
            appVersion = appVersion(),
            androidVersion = Build.VERSION.RELEASE ?: DIAGNOSTIC_UNAVAILABLE,
            androidApi = Build.VERSION.SDK_INT,
            manufacturer = Build.MANUFACTURER,
            phoneModel = Build.MODEL,
            bluetoothAvailable = adapter != null,
            bluetoothEnabled = bluetoothEnabled,
            connectPermissionGranted = permissionGranted,
            startedAt = startedAt,
        )
    }

    private fun buildDeviceSnapshot(): DiagnosticDevice {
        val device = selectedDevice
        val a2dp = profileState(a2dpProxy, device)
        val headset = profileState(headsetProxy, device)
        val inferredAcl = when {
            aclState != null -> aclState
            a2dp == BluetoothProfile.STATE_CONNECTED || headset == BluetoothProfile.STATE_CONNECTED -> {
                "CONNECTED (inferido por perfil publico)"
            }
            else -> null
        }
        return DiagnosticDevice(
            name = device?.safeName() ?: selectedCandidate?.name,
            alias = device?.safeAlias(),
            maskedAddress = selectedCandidate?.address?.let(::maskBluetoothAddress),
            deviceType = device?.safeType()?.let(::deviceTypeName),
            bluetoothClass = device?.safeBluetoothClass(),
            bondState = device?.safeBondState()?.let(::bondStateName),
            transport = device?.safeType()?.let(::transportName),
            a2dpState = a2dp?.let(::profileStateName),
            headsetState = headset?.let(::profileStateName),
            aclState = inferredAcl,
            candidateCount = relevantCandidates.size,
            selectionOrigin = selectedOrigin,
            candidates = relevantCandidates.map { candidate ->
                DiagnosticCandidate(candidate.name, maskBluetoothAddress(candidate.address))
            },
        )
    }

    private fun buildRepositoryState(
        forceConnectedWithoutBattery: Boolean = false,
        errorMessage: String? = null,
    ): SportX20BatteryState {
        val environment = buildEnvironment()
        val candidate = selectedCandidate
        val previous = latestRepositoryState
        val a2dp = profileState(a2dpProxy, selectedDevice)
        val headset = profileState(headsetProxy, selectedDevice)
        val connected = forceConnectedWithoutBattery ||
            aclState == "CONNECTED" ||
            a2dp == BluetoothProfile.STATE_CONNECTED ||
            headset == BluetoothProfile.STATE_CONNECTED
        val connecting = a2dp == BluetoothProfile.STATE_CONNECTING ||
            headset == BluetoothProfile.STATE_CONNECTING
        val status = when {
            !environment.bluetoothAvailable -> BluetoothConnectionStatus.BLUETOOTH_UNAVAILABLE
            !environment.connectPermissionGranted -> BluetoothConnectionStatus.PERMISSION_REQUIRED
            environment.bluetoothEnabled != true -> BluetoothConnectionStatus.BLUETOOTH_DISABLED
            selectedOrigin == DiagnosticSelectionOrigin.MULTIPLE_CANDIDATES -> {
                BluetoothConnectionStatus.MULTIPLE_CANDIDATES
            }
            candidate == null -> BluetoothConnectionStatus.DEVICE_NOT_PAIRED
            connected && previous?.hasBatteryData() == true && !previous.isStale -> {
                BluetoothConnectionStatus.CONNECTED
            }
            connected -> BluetoothConnectionStatus.CONNECTED_WITHOUT_BATTERY_DATA
            connecting -> BluetoothConnectionStatus.CONNECTING
            else -> BluetoothConnectionStatus.PAIRED_DISCONNECTED
        }
        val keepFresh = connected && previous?.hasBatteryData() == true && !previous.isStale
        val hasCachedData = previous?.hasBatteryData() == true
        return SportX20BatteryState.create(
            deviceName = candidate?.name ?: previous?.deviceName,
            deviceAddress = candidate?.address ?: previous?.deviceAddress,
            connectionStatus = status,
            leftBatteryPercent = previous?.leftBatteryPercent,
            rightBatteryPercent = previous?.rightBatteryPercent,
            combinedBatteryPercent = previous?.combinedBatteryPercent,
            caseBatteryPercent = previous?.caseBatteryPercent,
            caseBatteryRange = previous?.caseBatteryRange,
            lastUpdatedAt = previous?.lastUpdatedAt,
            dataSource = when {
                keepFresh -> previous?.dataSource ?: BluetoothBatteryDataSourceType.NONE
                hasCachedData -> BluetoothBatteryDataSourceType.PERSISTED_CACHE
                else -> BluetoothBatteryDataSourceType.NONE
            },
            isStale = hasCachedData && !keepFresh,
            errorMessage = safeBluetoothErrorMessage(
                errorMessage ?: if (status == BluetoothConnectionStatus.MULTIPLE_CANDIDATES) {
                    "Mais de um Sport X20 pareado"
                } else {
                    null
                },
            ),
        )
    }

    private fun registerReceiver() {
        if (receiverRegistered) return
        val filter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
            addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
        }
        try {
            if (Build.VERSION.SDK_INT >= 33) {
                applicationContext.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("DEPRECATION")
                applicationContext.registerReceiver(receiver, filter)
            }
            receiverRegistered = true
        } catch (_: SecurityException) {
            addEvent("RECEIVER", "Registro indisponivel sem permissao")
        }
    }

    private fun unregisterReceiver() {
        if (!receiverRegistered) return
        try {
            applicationContext.unregisterReceiver(receiver)
        } catch (_: IllegalArgumentException) {
            // Receiver already removed by the platform.
        }
        receiverRegistered = false
    }

    private fun requestProfileProxies() {
        val adapter = bluetoothManager?.adapter ?: return
        if (!hasConnectPermission() || buildEnvironment().bluetoothEnabled != true) return
        listOf(BluetoothProfile.A2DP, BluetoothProfile.HEADSET).forEach { profile ->
            if (!requestedProfiles.add(profile)) return@forEach
            try {
                if (!adapter.getProfileProxy(applicationContext, profileListener, profile)) {
                    requestedProfiles -= profile
                }
            } catch (_: SecurityException) {
                requestedProfiles -= profile
            }
        }
    }

    private fun closeProfileProxies() {
        a2dpProxy?.let { closeProfileProxy(BluetoothProfile.A2DP, it) }
        headsetProxy?.let { closeProfileProxy(BluetoothProfile.HEADSET, it) }
        a2dpProxy = null
        headsetProxy = null
    }

    private fun closeProfileProxy(profile: Int, proxy: BluetoothProfile) {
        try {
            bluetoothManager?.adapter?.closeProfileProxy(profile, proxy)
        } catch (_: SecurityException) {
            // Nothing else can be done after permission revocation.
        }
    }

    private fun cancelInspection() {
        readGuard.cancel()
        gattInspector.cancel()
    }

    private fun addEvent(category: String, message: String) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            eventBuffer.add(category, message)
            state = state.copy(events = eventBuffer.snapshot())
            publish()
        } else {
            mainHandler.post { addEvent(category, message) }
        }
    }

    private fun publish() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            onStateChanged(state)
        } else {
            mainHandler.post { onStateChanged(state) }
        }
    }

    @Suppress("DEPRECATION")
    private fun appVersion(): String {
        return try {
            applicationContext.packageManager
                .getPackageInfo(applicationContext.packageName, 0)
                .versionName
                ?: DIAGNOSTIC_UNAVAILABLE
        } catch (_: Exception) {
            DIAGNOSTIC_UNAVAILABLE
        }
    }

    private fun profileState(profile: BluetoothProfile?, device: BluetoothDevice?): Int? {
        if (profile == null || device == null || !hasConnectPermission()) return null
        return try {
            profile.getConnectionState(device)
        } catch (_: SecurityException) {
            null
        }
    }

    private fun isSelectedDevice(device: BluetoothDevice): Boolean {
        val expectedAddress = selectedCandidate?.address ?: return false
        return device.safeAddress()?.equals(expectedAddress, ignoreCase = true) == true
    }

    private fun BluetoothDevice.toCandidateOrNull(): BluetoothDeviceCandidate? {
        return try {
            BluetoothDeviceCandidate(name, address)
        } catch (_: SecurityException) {
            null
        }
    }

    private fun BluetoothDevice.safeAddress(): String? = try {
        address
    } catch (_: SecurityException) {
        null
    }

    private fun BluetoothDevice.safeName(): String? = try {
        name
    } catch (_: SecurityException) {
        null
    }

    private fun BluetoothDevice.safeAlias(): String? {
        if (Build.VERSION.SDK_INT < 30 || !hasConnectPermission()) return null
        return try {
            alias
        } catch (_: SecurityException) {
            null
        }
    }

    private fun BluetoothDevice.safeType(): Int? = try {
        type
    } catch (_: SecurityException) {
        null
    }

    private fun BluetoothDevice.safeBondState(): Int? = try {
        bondState
    } catch (_: SecurityException) {
        null
    }

    private fun BluetoothDevice.safeBluetoothClass(): String? {
        return try {
            bluetoothClass?.let { value ->
                "device=0x${value.deviceClass.toString(16).uppercase(Locale.ROOT)} " +
                    "major=0x${value.majorDeviceClass.toString(16).uppercase(Locale.ROOT)}"
            }
        } catch (_: SecurityException) {
            null
        }
    }

    @Suppress("DEPRECATION")
    private fun Intent.bluetoothDevice(): BluetoothDevice? {
        return if (Build.VERSION.SDK_INT >= 33) {
            getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        } else {
            getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        }
    }

    private fun emptyDevice(): DiagnosticDevice {
        return DiagnosticDevice(
            name = null,
            alias = null,
            maskedAddress = null,
            deviceType = null,
            bluetoothClass = null,
            bondState = null,
            transport = null,
            a2dpState = null,
            headsetState = null,
            aclState = null,
            candidateCount = 0,
            selectionOrigin = DiagnosticSelectionOrigin.NONE,
            candidates = emptyList(),
        )
    }

    private fun deviceTypeName(type: Int): String = when (type) {
        BluetoothDevice.DEVICE_TYPE_CLASSIC -> "CLASSIC"
        BluetoothDevice.DEVICE_TYPE_LE -> "LE"
        BluetoothDevice.DEVICE_TYPE_DUAL -> "DUAL"
        BluetoothDevice.DEVICE_TYPE_UNKNOWN -> "UNKNOWN"
        else -> "UNKNOWN($type)"
    }

    private fun transportName(type: Int): String = when (type) {
        BluetoothDevice.DEVICE_TYPE_CLASSIC -> "BR/EDR; inspecao GATT tenta LE"
        BluetoothDevice.DEVICE_TYPE_LE -> "LE"
        BluetoothDevice.DEVICE_TYPE_DUAL -> "BR/EDR + LE"
        else -> "Desconhecido; inspecao GATT tenta LE"
    }

    private fun profileStateName(state: Int): String = when (state) {
        BluetoothProfile.STATE_CONNECTED -> "CONNECTED"
        BluetoothProfile.STATE_CONNECTING -> "CONNECTING"
        BluetoothProfile.STATE_DISCONNECTED -> "DISCONNECTED"
        BluetoothProfile.STATE_DISCONNECTING -> "DISCONNECTING"
        else -> "UNKNOWN($state)"
    }

    private fun bondStateName(state: Int): String = when (state) {
        BluetoothDevice.BOND_BONDED -> "BONDED"
        BluetoothDevice.BOND_BONDING -> "BONDING"
        BluetoothDevice.BOND_NONE -> "NONE"
        else -> "UNKNOWN($state)"
    }

    private fun adapterStateName(state: Int): String = when (state) {
        BluetoothAdapter.STATE_ON -> "ON"
        BluetoothAdapter.STATE_TURNING_ON -> "TURNING_ON"
        BluetoothAdapter.STATE_OFF -> "OFF"
        BluetoothAdapter.STATE_TURNING_OFF -> "TURNING_OFF"
        else -> "UNKNOWN($state)"
    }

    private fun profileName(profile: Int): String = when (profile) {
        BluetoothProfile.A2DP -> "A2DP"
        BluetoothProfile.HEADSET -> "HEADSET"
        else -> profile.toString()
    }
}
