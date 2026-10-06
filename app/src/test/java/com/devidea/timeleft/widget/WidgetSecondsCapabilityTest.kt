package com.devidea.timeleft.widget

import org.junit.Assert.*
import org.junit.Test

class WidgetSecondsCapabilityTest {
    @Test fun `SDK alone never enables seconds and pre API 35 cannot use draw instructions`() {
        for (sdk in listOf(26, 34)) for (version in listOf(6L, 8L, 9L)) {
            val capability = WidgetSecondsCapability.forHost(sdk, version)
            assertNull(capability.validationProfile)
            assertNull(capability.productionProfile)
        }
        for (sdk in listOf(35, 36, 37)) for (version in listOf(null, -1L, 0L, 1L, 4L, 5L, 7L)) {
            val capability = WidgetSecondsCapability.forHost(sdk, version)
            assertNull(capability.validationProfile)
            assertNull(capability.productionProfile)
        }
    }

    @Test fun `validated lower hosts enable seconds with their compatible encoding`() {
        for (sdk in listOf(35, 36, 37)) {
            val v6 = WidgetSecondsCapability.forHost(sdk, 6)
            assertEquals(WidgetSecondsProfile.V6, v6.validationProfile)
            assertEquals(WidgetSecondsProfile.V6, v6.productionProfile)
            val v8 = WidgetSecondsCapability.forHost(sdk, 8)
            assertEquals(WidgetSecondsProfile.V7, v8.validationProfile)
            assertEquals(WidgetSecondsProfile.V7, v8.productionProfile)
        }
    }

    @Test fun `existing document 9 and newer support retains the V7 encoding`() {
        for (sdk in listOf(35, 36, 37)) for (version in listOf(9L, 10L, Long.MAX_VALUE)) {
            val capability = WidgetSecondsCapability.forHost(sdk, version)
            assertEquals(WidgetSecondsProfile.V7, capability.productionProfile)
            assertEquals(capability.productionProfile, capability.validationProfile)
        }
    }
}
