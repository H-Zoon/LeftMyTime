package com.devidea.timeleft.widget

import org.junit.Assert.*
import org.junit.Test

class WidgetSecondsLauncherTest {
    @Test fun `launchers with a live widget check receive draw instructions`() {
        assertTrue(WidgetSecondsLauncher.isVerified("com.android.launcher3"))
        assertTrue(WidgetSecondsLauncher.isVerified("com.google.android.apps.nexuslauncher"))
    }

    @Test fun `One UI Home that died on draw instructions stays on the static widget`() {
        assertFalse(WidgetSecondsLauncher.isVerified("com.sec.android.app.launcher"))
    }

    @Test fun `unknown, chooser and unreadable launchers are never verified`() {
        for (packageName in listOf(null, "", "android", "com.android.launcher3.other", "COM.ANDROID.LAUNCHER3",
            "com.sec.android.app.easylauncher", "com.teslacoilsw.launcher", "org.lineageos.trebuchet")) {
            assertFalse(packageName.toString(), WidgetSecondsLauncher.isVerified(packageName))
        }
    }
}
