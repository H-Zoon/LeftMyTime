package com.devidea.timeleft.widget

import com.devidea.timeleft.widget.WidgetSecondsDelivery.*
import org.junit.Assert.*
import org.junit.Test

class WidgetSecondsDeliveryTest {
    @Test fun `verified launcher and document profile are both required for live ruler`() {
        assertEquals(DrawInstructions, WidgetSecondsDelivery.resolve(true, drawSupported = true, launcherVerified = true))
        assertEquals(Chronometer, WidgetSecondsDelivery.resolve(true, drawSupported = true, launcherVerified = false))
    }

    @Test fun `a missing draw profile still provides host ticked seconds on every launcher`() {
        for (verified in listOf(true, false)) {
            assertEquals(Chronometer, WidgetSecondsDelivery.resolve(true, drawSupported = false, launcherVerified = verified))
        }
    }

    @Test fun `remote off disables both kinds of live seconds`() {
        for (drawSupported in listOf(true, false)) for (verified in listOf(true, false)) {
            assertEquals(Unavailable, WidgetSecondsDelivery.resolve(false, drawSupported, verified))
        }
    }
}
