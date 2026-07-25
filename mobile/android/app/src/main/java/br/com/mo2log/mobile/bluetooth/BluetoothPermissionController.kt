package br.com.mo2log.mobile.bluetooth

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

class BluetoothPermissionController(
    private val sdkInt: Int,
    private val permissionGranted: (String) -> Boolean,
) {
    fun requiredPermissions(): List<String> = requiredPermissionsForSdk(sdkInt)

    fun missingPermissions(): List<String> = requiredPermissions().filterNot(permissionGranted)

    fun hasRequiredPermissions(): Boolean = missingPermissions().isEmpty()

    fun canQueryBondedDevices(): Boolean = hasRequiredPermissions()

    companion object {
        const val BLUETOOTH_CONNECT_PERMISSION = "android.permission.BLUETOOTH_CONNECT"
        const val REQUEST_CODE = 2020

        fun requiredPermissionsForSdk(sdkInt: Int): List<String> {
            return if (sdkInt >= 31) listOf(BLUETOOTH_CONNECT_PERMISSION) else emptyList()
        }

        fun from(context: Context): BluetoothPermissionController {
            val applicationContext = context.applicationContext
            return BluetoothPermissionController(Build.VERSION.SDK_INT) { permission ->
                if (permission == Manifest.permission.BLUETOOTH_CONNECT && Build.VERSION.SDK_INT < 31) {
                    true
                } else {
                    applicationContext.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
                }
            }
        }
    }
}
