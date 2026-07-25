package br.com.mo2log.mobile.bluetooth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SportX20DeviceLocatorTest {
    private val locator = SportX20DeviceLocator()
    private val sport = BluetoothDeviceCandidate("soundcore Sport X20", "00:11:22:33:44:55")

    @Test
    fun findsPreviouslySelectedAddressFirst() {
        val renamed = sport.copy(name = "Meu fone")
        val result = locator.locate(listOf(renamed), "00:11:22:33:44:55")
        assertEquals(renamed, (result as SportX20DeviceSelection.Found).device)
    }

    @Test
    fun findsDeviceByRecognizedName() {
        val result = locator.locate(listOf(sport), null)
        assertEquals(sport, (result as SportX20DeviceSelection.Found).device)
    }

    @Test
    fun normalizesCaseWhitespaceAndSeparators() {
        assertTrue(locator.isSportX20Name("  SOUNDCORE---Sport   X20  "))
        assertEquals("soundcore sport x20", locator.normalizeName(" SOUNDCORE-Sport  X20 "))
    }

    @Test
    fun genericSoundcoreNameIsNotAccepted() {
        val result = locator.locate(
            listOf(BluetoothDeviceCandidate("soundcore Liberty 4", "AA:11:22:33:44:55")),
            null,
        )
        assertTrue(result is SportX20DeviceSelection.NotFound)
    }

    @Test
    fun similarModelNameIsNotAccepted() {
        assertTrue(!locator.isSportX20Name("soundcore Sport X200"))
    }

    @Test
    fun returnsNotFoundWithoutCandidates() {
        assertTrue(locator.locate(emptyList(), null) is SportX20DeviceSelection.NotFound)
    }

    @Test
    fun returnsAllCandidatesWhenNameIsAmbiguous() {
        val second = sport.copy(address = "AA:BB:CC:DD:EE:FF")
        val result = locator.locate(listOf(sport, second), null)
        assertEquals(2, (result as SportX20DeviceSelection.MultipleCandidates).devices.size)
    }

    @Test
    fun missingStoredAddressFallsBackToName() {
        val result = locator.locate(listOf(sport), "FF:EE:DD:CC:BB:AA")
        assertEquals(sport, (result as SportX20DeviceSelection.Found).device)
    }
}
