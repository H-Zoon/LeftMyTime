package com.devidea.timeleft.messaging

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RewardMessagingPolicyTest {
    @Test fun `a fresh installation starts opted out and reconciles unsubscribe`() {
        val policy = RewardTopicSyncPolicy()
        assertFalse(policy.consentGranted)
        assertFalse(requireNotNull(policy.begin(retryFailed = false)).consentGranted)
    }

    @Test fun `revocation queues without waiting for an offline subscribe and ignores its completion`() {
        val policy = RewardTopicSyncPolicy()
        policy.setConsent(true)
        val subscribe = requireNotNull(policy.begin(retryFailed = false))
        policy.setConsent(false)
        val unsubscribe = requireNotNull(policy.begin(retryFailed = false))
        assertFalse(unsubscribe.consentGranted)
        policy.complete(subscribe, success = true)
        assertEquals(RewardTopicSyncStatus.Syncing, policy.status)
        policy.complete(unsubscribe, success = true)
        assertEquals(RewardTopicSyncStatus.Synced, policy.status)
    }

    @Test fun `rapid off on changes require the final subscribe even if the older subscribe completes`() {
        val policy = RewardTopicSyncPolicy()
        policy.setConsent(true)
        val firstSubscribe = requireNotNull(policy.begin(retryFailed = false))
        policy.setConsent(false)
        val unsubscribe = requireNotNull(policy.begin(retryFailed = false))
        policy.setConsent(true)
        val finalSubscribe = requireNotNull(policy.begin(retryFailed = false))
        assertTrue(finalSubscribe.consentGranted)
        policy.complete(firstSubscribe, success = true)
        policy.complete(unsubscribe, success = true)
        assertEquals(RewardTopicSyncStatus.Syncing, policy.status)
        policy.complete(finalSubscribe, success = true)
        assertEquals(RewardTopicSyncStatus.Synced, policy.status)
    }

    @Test fun `same FID does not duplicate a pending or settled operation but rotation resubscribes`() {
        val policy = RewardTopicSyncPolicy()
        policy.setConsent(true)
        policy.onRegistered("first-installation")
        val first = requireNotNull(policy.begin(retryFailed = false))
        policy.onRegistered("first-installation")
        assertNull(policy.begin(retryFailed = false))
        policy.complete(first, success = true)
        policy.onRegistered("first-installation")
        assertNull(policy.begin(retryFailed = false))
        policy.onRegistered("replacement-installation")
        assertTrue(requireNotNull(policy.begin(retryFailed = false)).consentGranted)
    }

    @Test fun `timeout can settle late but an explicit retry rejects the old result`() {
        val policy = RewardTopicSyncPolicy()
        val timedOut = requireNotNull(policy.begin(retryFailed = false))
        policy.complete(timedOut, success = false)
        assertEquals(RewardTopicSyncStatus.Failed, policy.status)
        assertNull(policy.begin(retryFailed = false))
        val retried = requireNotNull(policy.begin(retryFailed = true))
        policy.complete(timedOut, success = true)
        assertEquals(RewardTopicSyncStatus.Syncing, policy.status)
        policy.complete(retried, success = false)
        policy.complete(retried, success = true)
        assertEquals(RewardTopicSyncStatus.Synced, policy.status)
    }

    @Test fun `a failed preference save cannot be erased by an older network success`() {
        val policy = RewardTopicSyncPolicy()
        val old = requireNotNull(policy.begin(retryFailed = false))
        policy.localFailure()
        policy.complete(old, success = true)
        assertEquals(RewardTopicSyncStatus.Failed, policy.status)
        assertFalse(requireNotNull(policy.begin(retryFailed = true)).consentGranted)
    }

    @Test fun `each supported reward marker is recognized without classifying a general message`() {
        assertTrue(isRewardMessage("reward", null, null))
        assertTrue(isRewardMessage(null, "/topics/reward_notifications", null))
        assertTrue(isRewardMessage(null, null, "reward_notifications"))
        assertFalse(isRewardMessage(null, null, null))
        assertFalse(isRewardMessage("reminder", "/topics/other", "remote_messages"))
        assertFalse(isRewardMessage("rewards", "/topics/reward_notifications_extra", null))
    }
}
