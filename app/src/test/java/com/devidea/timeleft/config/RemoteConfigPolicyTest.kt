package com.devidea.timeleft.config

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteConfigPolicyTest {
    @Test fun `only a canonical remote false disables seconds`() {
        assertFalse(widgetSecondsEnabledFromRemoteValue("false"))
        for (value in listOf(null, "true", "", "0", "off", "no", "FALSE", " false ", "invalid")) {
            assertTrue(widgetSecondsEnabledFromRemoteValue(value))
        }
    }
}
