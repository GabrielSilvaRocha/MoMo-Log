package br.com.mo2log.mobile.bluetooth

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SoundcoreDiagnosticsAvailabilityTest {
    @Test
    fun diagnosticsAreVisibleInDebugConfiguration() {
        assertTrue(shouldShowSoundcoreDiagnostics(true))
    }

    @Test
    fun diagnosticsAreAbsentInReleaseConfiguration() {
        assertFalse(shouldShowSoundcoreDiagnostics(false))
    }
}
