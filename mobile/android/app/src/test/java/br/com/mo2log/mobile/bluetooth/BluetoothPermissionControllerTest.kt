package br.com.mo2log.mobile.bluetooth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BluetoothPermissionControllerTest {
    @Test
    fun api26DoesNotRequireRuntimePermission() {
        val controller = BluetoothPermissionController(26) { false }
        assertTrue(controller.requiredPermissions().isEmpty())
        assertTrue(controller.canQueryBondedDevices())
    }

    @Test
    fun api30DoesNotRequireRuntimePermission() {
        val controller = BluetoothPermissionController(30) { false }
        assertTrue(controller.requiredPermissions().isEmpty())
        assertTrue(controller.canQueryBondedDevices())
    }

    @Test
    fun api31RequiresBluetoothConnect() {
        val controller = BluetoothPermissionController(31) { true }
        assertEquals(
            listOf(BluetoothPermissionController.BLUETOOTH_CONNECT_PERMISSION),
            controller.requiredPermissions(),
        )
    }

    @Test
    fun api35RequiresBluetoothConnect() {
        val controller = BluetoothPermissionController(35) { true }
        assertEquals(1, controller.requiredPermissions().size)
    }

    @Test
    fun deniedBluetoothConnectBlocksBondedDevices() {
        val controller = BluetoothPermissionController(35) { false }
        assertFalse(controller.hasRequiredPermissions())
        assertFalse(controller.canQueryBondedDevices())
    }

    @Test
    fun grantedBluetoothConnectAllowsBondedDevices() {
        val controller = BluetoothPermissionController(35) { true }
        assertTrue(controller.hasRequiredPermissions())
        assertTrue(controller.canQueryBondedDevices())
    }
}
