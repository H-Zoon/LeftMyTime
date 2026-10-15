package com.devidea.timeleft.messaging

enum class RewardTopicSyncStatus { Syncing, Synced, Failed }

data class RewardMessagingState(
    val consentGranted: Boolean = false,
    val notificationsAllowed: Boolean = false,
    val topicSync: RewardTopicSyncStatus = RewardTopicSyncStatus.Syncing,
)

/** All access is serialized by RewardMessaging's lock; no Android or Firebase state is owned here. */
internal class RewardTopicSyncPolicy {
    data class Attempt(val sequence: Long, val generation: Long, val consentGranted: Boolean)

    var consentGranted = false
        private set
    var status = RewardTopicSyncStatus.Syncing
        private set
    private var installationId: String? = null
    private var generation = 0L
    private var sequence = 0L
    private var latestAttempt: Attempt? = null

    fun setConsent(granted: Boolean) {
        if (consentGranted == granted) return
        consentGranted = granted
        invalidate()
    }

    fun onRegistered(id: String) {
        if (installationId == id) return
        installationId = id
        invalidate()
    }

    private fun invalidate() {
        generation++
        status = RewardTopicSyncStatus.Syncing
    }

    fun begin(retryFailed: Boolean): Attempt? {
        if (status == RewardTopicSyncStatus.Synced) return null
        if (latestAttempt?.generation == generation && status == RewardTopicSyncStatus.Syncing) return null
        if (status == RewardTopicSyncStatus.Failed && !retryFailed) return null
        return Attempt(++sequence, generation, consentGranted).also {
            latestAttempt = it
            status = RewardTopicSyncStatus.Syncing
        }
    }

    /** A timeout is unconfirmed, so a later success can still settle this exact attempt. */
    fun complete(attempt: Attempt, success: Boolean) {
        if (attempt != latestAttempt || attempt.generation != generation) return
        status = if (success) RewardTopicSyncStatus.Synced else RewardTopicSyncStatus.Failed
    }

    fun localFailure() {
        generation++ // A late SDK result must not erase a failed local save.
        status = RewardTopicSyncStatus.Failed
    }
}

internal fun isRewardMessage(messageType: String?, from: String?, channelId: String?): Boolean =
    messageType == "reward" || from == "/topics/reward_notifications" || channelId == "reward_notifications"
