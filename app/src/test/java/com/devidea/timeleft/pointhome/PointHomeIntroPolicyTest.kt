package com.devidea.timeleft.pointhome

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PointHomeIntroPolicyTest {
    private fun inputs(
        buttonReady: Boolean = true,
        completedVersion: Int = 0,
        freshInstall: Boolean = false,
        homeLaunches: Int = 1,
        openedFromNotification: Boolean = false,
    ) = PointHomeIntroInputs(buttonReady, completedVersion, freshInstall, homeLaunches, openedFromNotification)

    @Test fun `an updated installation sees the intro on its first home start`() {
        assertTrue(shouldShowPointHomeIntro(inputs(freshInstall = false, homeLaunches = 1)))
    }

    @Test fun `a fresh installation waits for its second start`() {
        assertFalse(shouldShowPointHomeIntro(inputs(freshInstall = true, homeLaunches = 1)))
        assertTrue(shouldShowPointHomeIntro(inputs(freshInstall = true, homeLaunches = 2)))
    }

    @Test fun `the intro never describes a button the SDK did not create`() {
        assertFalse(shouldShowPointHomeIntro(inputs(buttonReady = false)))
    }

    @Test fun `a completed intro does not repeat but an older completion does not hide a newer version`() {
        assertFalse(shouldShowPointHomeIntro(inputs(completedVersion = POINTHOME_INTRO_VERSION)))
        assertFalse(shouldShowPointHomeIntro(inputs(completedVersion = POINTHOME_INTRO_VERSION + 1)))
        assertTrue(shouldShowPointHomeIntro(inputs(completedVersion = POINTHOME_INTRO_VERSION - 1)))
    }

    @Test fun `a reward notification session skips the intro`() {
        assertFalse(shouldShowPointHomeIntro(inputs(openedFromNotification = true)))
    }

    @Test fun `someone who already receives rewards only gets the introduction`() {
        assertEquals(PointHomeIntroMode.Choice, pointHomeIntroMode(rewardConsentGranted = false))
        assertEquals(PointHomeIntroMode.InformationOnly, pointHomeIntroMode(rewardConsentGranted = true))
    }
}
