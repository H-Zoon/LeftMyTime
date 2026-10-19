package com.devidea.timeleft.widget

import com.devidea.timeleft.widget.WidgetSecondsDelivery.*
import org.junit.Assert.*
import org.junit.Test

class WidgetSecondsDeliveryTest {
    @Test fun `a verified launcher keeps the live draw-instruction widget with or without exact alarms`() {
        assertEquals(DrawInstructions, WidgetSecondsDelivery.resolve(true, launcherVerified = true, exactAlarms = true))
        assertEquals(DrawInstructions, WidgetSecondsDelivery.resolve(true, launcherVerified = true, exactAlarms = false))
    }

    @Test fun `an unverified launcher gets the Chronometer only when an exact alarm can end it`() {
        assertEquals(Chronometer, WidgetSecondsDelivery.resolve(true, launcherVerified = false, exactAlarms = true))
        assertEquals(NeedsExactAlarm, WidgetSecondsDelivery.resolve(true, launcherVerified = false, exactAlarms = false))
    }

    @Test fun `an unsupported platform or the remote switch keeps every launcher static`() {
        for (verified in listOf(true, false)) for (exact in listOf(true, false))
            assertEquals(Unavailable, WidgetSecondsDelivery.resolve(false, verified, exact))
    }

    @Test fun `only live deliveries show seconds`() {
        assertEquals(setOf(DrawInstructions, Chronometer), values().filter { it.showsSeconds }.toSet())
    }
}
