package br.com.mo2log.mobile.bluetooth

import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.Looper
import java.util.Collections
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

class AndroidBluetoothPlatform(
    context: Context,
    private val permissionController: BluetoothPermissionController =
        BluetoothPermissionController.from(context),
) : BluetoothPlatform {
    private val applicationContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val bluetoothManager = applicationContext.getSystemService(BluetoothManager::class.java)
    private val knownConnectionStates = ConcurrentHashMap<String, BluetoothPlatformConnectionState>()
    private val activeGattOperations = Collections.synchronizedSet(mutableSetOf<AndroidGattReadOperation>())

    @Volatile
    private var observer: BluetoothPlatformObserver? = null
    private var receiverRegistered = false
    private var closed = false
    private var a2dpProxy: BluetoothProfile? = null
    private var headsetProxy: BluetoothProfile? = null
    private val requestedProfiles = mutableSetOf<Int>()

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val currentIntent = intent ?: return
            when (currentIntent.action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val adapterState = currentIntent.getIntExtra(
                        BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR,
                    )
                    if (adapterState == BluetoothAdapter.STATE_ON) {
                        requestProfileProxiesIfPermitted()
                    } else {
                        knownConnectionStates.clear()
                        closeAudioProfileProxies()
                        requestedProfiles.clear()
                    }
                }
                BluetoothDevice.ACTION_ACL_CONNECTED -> {
                    currentIntent.bluetoothDevice()?.safeAddress()?.let { address ->
                        knownConnectionStates[addressKey(address)] = BluetoothPlatformConnectionState.CONNECTED
                    }
                }
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    currentIntent.bluetoothDevice()?.safeAddress()?.let { address ->
                        knownConnectionStates[addressKey(address)] = BluetoothPlatformConnectionState.DISCONNECTED
                    }
                }
                BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED,
                BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED -> {
                    val state = currentIntent.getIntExtra(
                        BluetoothProfile.EXTRA_STATE,
                        BluetoothProfile.STATE_DISCONNECTED,
                    )
                    currentIntent.bluetoothDevice()?.safeAddress()?.let { address ->
                        knownConnectionStates[addressKey(address)] = state.toPlatformConnectionState()
                    }
                }
            }
            notifyObserver()
        }
    }

    private val profileListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
            if (proxy == null) return
            if (closed || observer == null) {
                closeProfileProxy(profile, proxy)
                return
            }
            when (profile) {
                BluetoothProfile.A2DP -> a2dpProxy = proxy
                BluetoothProfile.HEADSET -> headsetProxy = proxy
                else -> closeProfileProxy(profile, proxy)
            }
            notifyObserver()
        }

        override fun onServiceDisconnected(profile: Int) {
            when (profile) {
                BluetoothProfile.A2DP -> a2dpProxy = null
                BluetoothProfile.HEADSET -> headsetProxy = null
            }
            requestedProfiles -= profile
            if (!closed && observer != null) requestProfileProxiesIfPermitted()
            notifyObserver()
        }
    }

    override fun hasBluetoothAdapter(): Boolean = bluetoothManager?.adapter != null

    override fun hasConnectPermission(): Boolean = permissionController.canQueryBondedDevices()

    override fun isBluetoothEnabled(): Boolean {
        if (!hasConnectPermission()) return false
        return try {
            bluetoothManager?.adapter?.isEnabled == true
        } catch (_: SecurityException) {
            false
        }
    }

    override fun bondedDevices(): List<BluetoothDeviceCandidate> {
        if (!hasConnectPermission()) return emptyList()
        return try {
            bluetoothManager?.adapter?.bondedDevices
                .orEmpty()
                .mapNotNull { it.toCandidateOrNull() }
                .sortedBy { it.name.orEmpty() }
        } catch (_: SecurityException) {
            emptyList()
        }
    }

    override fun connectionState(device: BluetoothDeviceCandidate): BluetoothPlatformConnectionState {
        if (!hasConnectPermission()) return BluetoothPlatformConnectionState.UNKNOWN
        val bluetoothDevice = bluetoothDeviceFor(device) ?: return BluetoothPlatformConnectionState.UNKNOWN
        val profileStates = listOfNotNull(a2dpProxy, headsetProxy).mapNotNull { proxy ->
            try {
                proxy.getConnectionState(bluetoothDevice).toPlatformConnectionState()
            } catch (_: SecurityException) {
                null
            }
        }
        if (profileStates.any { it == BluetoothPlatformConnectionState.CONNECTED }) {
            return BluetoothPlatformConnectionState.CONNECTED
        }
        if (profileStates.any { it == BluetoothPlatformConnectionState.CONNECTING }) {
            return BluetoothPlatformConnectionState.CONNECTING
        }
        return knownConnectionStates[addressKey(device.address)]
            ?: if (profileStates.isNotEmpty()) {
                BluetoothPlatformConnectionState.DISCONNECTED
            } else {
                BluetoothPlatformConnectionState.UNKNOWN
            }
    }

    @Synchronized
    override fun startObserving(observer: BluetoothPlatformObserver) {
        if (closed) return
        this.observer = observer
        registerReceiverIfNeeded()
        requestProfileProxiesIfPermitted()
    }

    @Synchronized
    override fun stopObserving() {
        observer = null
        if (receiverRegistered) {
            try {
                applicationContext.unregisterReceiver(receiver)
            } catch (_: IllegalArgumentException) {
                // Already unregistered by the platform.
            }
            receiverRegistered = false
        }
        closeAudioProfileProxies()
        requestedProfiles.clear()
        knownConnectionStates.clear()
    }

    override fun readGattCharacteristic(
        device: BluetoothDeviceCandidate,
        serviceUuid: UUID,
        characteristicUuid: UUID,
        timeoutMillis: Long,
        callback: (GattCharacteristicReadResult) -> Unit,
    ): BluetoothOperation {
        if (closed || !hasConnectPermission()) {
            callback(GattCharacteristicReadResult.Failure("Permissao Bluetooth indisponivel"))
            return CompletedBluetoothOperation
        }
        if (!isBluetoothEnabled()) {
            callback(GattCharacteristicReadResult.Failure("Bluetooth desligado"))
            return CompletedBluetoothOperation
        }
        val bluetoothDevice = bluetoothDeviceFor(device)
        if (bluetoothDevice == null) {
            callback(GattCharacteristicReadResult.Failure("Dispositivo pareado indisponivel"))
            return CompletedBluetoothOperation
        }

        lateinit var operation: AndroidGattReadOperation
        operation = AndroidGattReadOperation(
            device = bluetoothDevice,
            serviceUuid = serviceUuid,
            characteristicUuid = characteristicUuid,
            timeoutMillis = timeoutMillis.coerceIn(1_000L, 15_000L),
            callback = callback,
            onFinished = { activeGattOperations -= operation },
        )
        activeGattOperations += operation
        operation.start()
        return operation
    }

    @Synchronized
    override fun close() {
        if (closed) return
        stopObserving()
        closed = true
        val operations = synchronized(activeGattOperations) {
            activeGattOperations.toList().also { activeGattOperations.clear() }
        }
        operations.forEach { it.cancel() }
    }

    private fun registerReceiverIfNeeded() {
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
            receiverRegistered = false
        }
    }

    private fun requestProfileProxiesIfPermitted() {
        if (!hasConnectPermission() || !isBluetoothEnabled()) return
        val adapter = bluetoothManager?.adapter ?: return
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

    private fun closeAudioProfileProxies() {
        a2dpProxy?.let { closeProfileProxy(BluetoothProfile.A2DP, it) }
        headsetProxy?.let { closeProfileProxy(BluetoothProfile.HEADSET, it) }
        a2dpProxy = null
        headsetProxy = null
    }

    private fun closeProfileProxy(profile: Int, proxy: BluetoothProfile) {
        try {
            bluetoothManager?.adapter?.closeProfileProxy(profile, proxy)
        } catch (_: SecurityException) {
            // There is no further operation to perform without permission.
        }
    }

    private fun bluetoothDeviceFor(candidate: BluetoothDeviceCandidate): BluetoothDevice? {
        return try {
            bluetoothManager?.adapter?.bondedDevices
                ?.firstOrNull { it.safeAddress()?.equals(candidate.address, ignoreCase = true) == true }
        } catch (_: SecurityException) {
            null
        }
    }

    private fun BluetoothDevice.toCandidateOrNull(): BluetoothDeviceCandidate? {
        return try {
            BluetoothDeviceCandidate(name = name, address = address)
        } catch (_: SecurityException) {
            null
        }
    }

    private fun BluetoothDevice.safeAddress(): String? {
        return try {
            address
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

    private fun notifyObserver() {
        val target = observer ?: return
        if (Looper.myLooper() == Looper.getMainLooper()) {
            if (observer === target) target.onBluetoothEnvironmentChanged()
        } else {
            mainHandler.post {
                if (observer === target) target.onBluetoothEnvironmentChanged()
            }
        }
    }

    private fun addressKey(address: String): String = address.uppercase(Locale.ROOT)

    private fun Int.toPlatformConnectionState(): BluetoothPlatformConnectionState {
        return when (this) {
            BluetoothProfile.STATE_CONNECTED -> BluetoothPlatformConnectionState.CONNECTED
            BluetoothProfile.STATE_CONNECTING -> BluetoothPlatformConnectionState.CONNECTING
            BluetoothProfile.STATE_DISCONNECTED,
            BluetoothProfile.STATE_DISCONNECTING -> BluetoothPlatformConnectionState.DISCONNECTED
            else -> BluetoothPlatformConnectionState.UNKNOWN
        }
    }

    private inner class AndroidGattReadOperation(
        private val device: BluetoothDevice,
        private val serviceUuid: UUID,
        private val characteristicUuid: UUID,
        private val timeoutMillis: Long,
        private val callback: (GattCharacteristicReadResult) -> Unit,
        private val onFinished: () -> Unit,
    ) : BluetoothOperation {
        private val completed = AtomicBoolean(false)
        private val lock = Any()
        private var bluetoothGatt: BluetoothGatt? = null
        private val timeout = Runnable { finish(GattCharacteristicReadResult.Timeout) }

        private val gattCallback = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    finish(GattCharacteristicReadResult.Failure("Falha de conexao GATT: $status"))
                    return
                }
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        val discoveryStarted = try {
                            gatt.discoverServices()
                        } catch (_: SecurityException) {
                            false
                        }
                        if (!discoveryStarted) {
                            finish(GattCharacteristicReadResult.Failure("Descoberta GATT indisponivel"))
                        }
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        finish(GattCharacteristicReadResult.Failure("Conexao GATT encerrada"))
                    }
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    finish(GattCharacteristicReadResult.Failure("Falha ao descobrir servicos GATT: $status"))
                    return
                }
                val service = gatt.getService(serviceUuid)
                if (service == null) {
                    finish(GattCharacteristicReadResult.ServiceUnavailable)
                    return
                }
                val characteristic = service.getCharacteristic(characteristicUuid)
                if (characteristic == null) {
                    finish(GattCharacteristicReadResult.CharacteristicUnavailable)
                    return
                }
                @Suppress("DEPRECATION")
                val readStarted = try {
                    gatt.readCharacteristic(characteristic)
                } catch (_: SecurityException) {
                    false
                }
                if (!readStarted) {
                    finish(GattCharacteristicReadResult.Failure("Leitura GATT nao iniciada"))
                }
            }

            @Deprecated("Deprecated by Android 13")
            override fun onCharacteristicRead(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                status: Int,
            ) {
                @Suppress("DEPRECATION")
                finishCharacteristicRead(characteristic.uuid, characteristic.value?.clone(), status)
            }

            override fun onCharacteristicRead(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                value: ByteArray,
                status: Int,
            ) {
                finishCharacteristicRead(characteristic.uuid, value.clone(), status)
            }
        }

        fun start() {
            if (completed.get()) return
            mainHandler.postDelayed(timeout, timeoutMillis)
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
                finish(GattCharacteristicReadResult.Failure("Conexao GATT nao iniciada"))
                return
            }
            synchronized(lock) {
                if (completed.get()) safeCloseGatt(openedGatt) else bluetoothGatt = openedGatt
            }
        }

        override fun cancel() {
            finish(GattCharacteristicReadResult.Cancelled)
        }

        private fun finishCharacteristicRead(uuid: UUID, payload: ByteArray?, status: Int) {
            if (uuid != characteristicUuid) return
            if (status != BluetoothGatt.GATT_SUCCESS) {
                finish(GattCharacteristicReadResult.Failure("Falha ao ler bateria GATT: $status"))
                return
            }
            finish(GattCharacteristicReadResult.Success(payload ?: byteArrayOf()))
        }

        private fun finish(result: GattCharacteristicReadResult) {
            if (!completed.compareAndSet(false, true)) return
            mainHandler.removeCallbacks(timeout)
            val gatt = synchronized(lock) {
                bluetoothGatt.also { bluetoothGatt = null }
            }
            gatt?.let { safeCloseGatt(it) }
            onFinished()
            callback(result)
        }

        private fun safeCloseGatt(gatt: BluetoothGatt) {
            try {
                gatt.disconnect()
            } catch (_: SecurityException) {
                // close() is still required even if disconnect is denied.
            } finally {
                try {
                    gatt.close()
                } catch (_: SecurityException) {
                    // The platform owns the final cleanup when permission is revoked mid-read.
                }
            }
        }
    }
}
