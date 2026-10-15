package com.devidea.timeleft.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WidgetSecondsRemoteConfigTest {
    @Test fun `remote false disables production without changing the validation capability`() {
        for (version in listOf(6L, 8L, 9L)) {
            val supported = WidgetSecondsCapability.forHost(35, version)
            val disabled = supported.withRemoteSecondsEnabled(false)
            assertNull(disabled.productionProfile)
            assertEquals(supported.validationProfile, disabled.validationProfile)
            assertEquals(supported.hostVersion, disabled.hostVersion)
            assertEquals(supported, supported.withRemoteSecondsEnabled(true))
        }
    }

    @Test fun `remote true cannot expand host support`() {
        for (sdk in listOf(26, 34, 35, 36)) for (version in listOf(null, 1L, 4L, 7L)) {
            val capability = WidgetSecondsCapability.forHost(sdk, version)
            assertNull(capability.withRemoteSecondsEnabled(true).productionProfile)
            assertNull(capability.withRemoteSecondsEnabled(false).productionProfile)
        }
        assertNull(WidgetSecondsCapability.forHost(34, 9).withRemoteSecondsEnabled(true).productionProfile)
    }
}
