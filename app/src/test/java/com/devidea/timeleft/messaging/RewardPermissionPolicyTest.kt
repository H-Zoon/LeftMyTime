package com.devidea.timeleft.messaging

import org.junit.Assert.assertEquals
import org.junit.Test

class RewardPermissionPolicyTest {
    private fun action(
        canPostRewards: Boolean = false,
        runtimePermissionDenied: Boolean = true,
        requestedBefore: Boolean = false,
        rationaleAvailable: Boolean = false,
    ) = rewardPermissionAction(canPostRewards, runtimePermissionDenied, requestedBefore, rationaleAvailable)

    @Test fun `nothing is asked when rewards can already be displayed`() {
        assertEquals(RewardPermissionAction.None, action(canPostRewards = true, runtimePermissionDenied = false))
    }

    @Test fun `a permission that was never requested is requested`() {
        assertEquals(RewardPermissionAction.RequestPermission, action())
    }

    @Test fun `a permission refused once can be requested again while the system allows it`() {
        assertEquals(
            RewardPermissionAction.RequestPermission,
            action(requestedBefore = true, rationaleAvailable = true),
        )
    }

    @Test fun `a permission refused for good only has system settings`() {
        assertEquals(RewardPermissionAction.OpenSettings, action(requestedBefore = true, rationaleAvailable = false))
    }

    @Test fun `a granted permission with blocked notifications or channel needs system settings`() {
        assertEquals(RewardPermissionAction.OpenSettings, action(runtimePermissionDenied = false))
    }
}
