package br.com.mo2log.mobile.bluetooth

import android.content.SharedPreferences

interface SportX20StatePersistence {
    fun selectedAddress(): String?
    fun selectedName(): String?
    fun saveSelectedDevice(device: BluetoothDeviceCandidate)
    fun loadLastBatteryState(): SportX20BatteryState?
    fun saveBatteryState(state: SportX20BatteryState)
    fun clear()
}

data class PersistedSportX20BatterySnapshot(
    val deviceName: String?,
    val deviceAddress: String?,
    val leftBatteryPercent: Int?,
    val rightBatteryPercent: Int?,
    val combinedBatteryPercent: Int?,
    val caseBatteryPercent: Int?,
    val caseBatteryRange: String?,
    val lastUpdatedAt: Long?,
    val originalDataSource: BluetoothBatteryDataSourceType?,
)

fun restorePersistedSportX20State(
    snapshot: PersistedSportX20BatterySnapshot,
): SportX20BatteryState? {
    val timestamp = snapshot.lastUpdatedAt?.takeIf { it >= 0L } ?: return null
    val restored = SportX20BatteryState.create(
        deviceName = snapshot.deviceName,
        deviceAddress = snapshot.deviceAddress,
        connectionStatus = BluetoothConnectionStatus.PAIRED_DISCONNECTED,
        leftBatteryPercent = snapshot.leftBatteryPercent,
        rightBatteryPercent = snapshot.rightBatteryPercent,
        combinedBatteryPercent = snapshot.combinedBatteryPercent,
        caseBatteryPercent = snapshot.caseBatteryPercent,
        caseBatteryRange = snapshot.caseBatteryRange,
        lastUpdatedAt = timestamp,
        dataSource = BluetoothBatteryDataSourceType.PERSISTED_CACHE,
        isStale = true,
    )
    return restored.takeIf { it.hasBatteryData() }
}

class SportX20StateStore(
    private val preferences: SharedPreferences,
) : SportX20StatePersistence {
    override fun selectedAddress(): String? = safeString(KEY_SELECTED_ADDRESS)

    override fun selectedName(): String? = safeString(KEY_SELECTED_NAME)

    override fun saveSelectedDevice(device: BluetoothDeviceCandidate) {
        preferences.edit()
            .putString(KEY_SELECTED_ADDRESS, device.address)
            .putString(KEY_SELECTED_NAME, device.name)
            .apply()
    }

    override fun loadLastBatteryState(): SportX20BatteryState? {
        val source = safeString(KEY_LAST_DATA_SOURCE)?.let { stored ->
            BluetoothBatteryDataSourceType.entries.firstOrNull { it.name == stored }
        }
        return restorePersistedSportX20State(
            PersistedSportX20BatterySnapshot(
                deviceName = selectedName(),
                deviceAddress = selectedAddress(),
                leftBatteryPercent = safeOptionalInt(KEY_LAST_LEFT_BATTERY),
                rightBatteryPercent = safeOptionalInt(KEY_LAST_RIGHT_BATTERY),
                combinedBatteryPercent = safeOptionalInt(KEY_LAST_COMBINED_BATTERY),
                caseBatteryPercent = safeOptionalInt(KEY_LAST_CASE_BATTERY),
                caseBatteryRange = safeString(KEY_LAST_CASE_RANGE),
                lastUpdatedAt = safeOptionalLong(KEY_LAST_BATTERY_TIMESTAMP),
                originalDataSource = source,
            ),
        )
    }

    override fun saveBatteryState(state: SportX20BatteryState) {
        if (!state.hasBatteryData() || state.lastUpdatedAt == null) return
        if (state.dataSource == BluetoothBatteryDataSourceType.NONE ||
            state.dataSource == BluetoothBatteryDataSourceType.PERSISTED_CACHE ||
            state.dataSource == BluetoothBatteryDataSourceType.SPORT_X20_PROPRIETARY_PROTOCOL
        ) {
            return
        }

        val editor = preferences.edit()
            .putLong(KEY_LAST_BATTERY_TIMESTAMP, state.lastUpdatedAt)
            .putString(KEY_LAST_DATA_SOURCE, state.dataSource.name)
        state.deviceAddress?.let { editor.putString(KEY_SELECTED_ADDRESS, it) }
        state.deviceName?.let { editor.putString(KEY_SELECTED_NAME, it) }
        editor.putOptionalInt(KEY_LAST_LEFT_BATTERY, state.leftBatteryPercent)
        editor.putOptionalInt(KEY_LAST_RIGHT_BATTERY, state.rightBatteryPercent)
        editor.putOptionalInt(KEY_LAST_COMBINED_BATTERY, state.combinedBatteryPercent)
        editor.putOptionalInt(KEY_LAST_CASE_BATTERY, state.caseBatteryPercent)
        if (state.caseBatteryRange == null) {
            editor.remove(KEY_LAST_CASE_RANGE)
        } else {
            editor.putString(KEY_LAST_CASE_RANGE, state.caseBatteryRange)
        }
        editor.apply()
    }

    override fun clear() {
        preferences.edit()
            .remove(KEY_SELECTED_ADDRESS)
            .remove(KEY_SELECTED_NAME)
            .remove(KEY_LAST_LEFT_BATTERY)
            .remove(KEY_LAST_RIGHT_BATTERY)
            .remove(KEY_LAST_COMBINED_BATTERY)
            .remove(KEY_LAST_CASE_BATTERY)
            .remove(KEY_LAST_CASE_RANGE)
            .remove(KEY_LAST_BATTERY_TIMESTAMP)
            .remove(KEY_LAST_DATA_SOURCE)
            .apply()
    }

    private fun safeString(key: String): String? {
        return try {
            preferences.getString(key, null)?.trim()?.takeIf { it.isNotEmpty() }
        } catch (_: ClassCastException) {
            null
        }
    }

    private fun safeOptionalInt(key: String): Int? {
        if (!preferences.contains(key)) return null
        return try {
            safeBatteryPercent(preferences.getInt(key, -1))
        } catch (_: ClassCastException) {
            null
        }
    }

    private fun safeOptionalLong(key: String): Long? {
        if (!preferences.contains(key)) return null
        return try {
            preferences.getLong(key, -1L).takeIf { it >= 0L }
        } catch (_: ClassCastException) {
            null
        }
    }

    private fun SharedPreferences.Editor.putOptionalInt(key: String, value: Int?) {
        if (value == null) remove(key) else putInt(key, value)
    }

    companion object {
        const val KEY_SELECTED_ADDRESS = "sport_x20_selected_address"
        const val KEY_SELECTED_NAME = "sport_x20_selected_name"
        const val KEY_LAST_LEFT_BATTERY = "sport_x20_last_left_battery"
        const val KEY_LAST_RIGHT_BATTERY = "sport_x20_last_right_battery"
        const val KEY_LAST_COMBINED_BATTERY = "sport_x20_last_combined_battery"
        const val KEY_LAST_CASE_BATTERY = "sport_x20_last_case_battery"
        const val KEY_LAST_CASE_RANGE = "sport_x20_last_case_range"
        const val KEY_LAST_BATTERY_TIMESTAMP = "sport_x20_last_battery_timestamp"
        const val KEY_LAST_DATA_SOURCE = "sport_x20_last_data_source"
    }
}
