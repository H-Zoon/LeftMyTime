package com.devidea.timeleft.messaging

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.devidea.timeleft.notification.isNotificationRuntimePermissionDenied

/** What to do after the user chose to receive rewards, given the device's current notification state. */
internal enum class RewardPermissionAction { None, RequestPermission, OpenSettings }

/**
 * [rationaleAvailable] is the system's "can ask again" signal. A permission that was requested before
 * and has no rationale left is permanently denied, so only system settings can allow it.
 */
internal fun rewardPermissionAction(
    canPostRewards: Boolean,
    runtimePermissionDenied: Boolean,
    requestedBefore: Boolean,
    rationaleAvailable: Boolean,
): RewardPermissionAction = when {
    canPostRewards -> RewardPermissionAction.None
    runtimePermissionDenied && !(requestedBefore && !rationaleAvailable) -> RewardPermissionAction.RequestPermission
    else -> RewardPermissionAction.OpenSettings
}

/** App notification settings when the app is blocked as a whole; otherwise the reward channel only. */
internal fun Context.rewardNotificationSettingsIntent(): Intent {
    val manager = getSystemService(NotificationManager::class.java)
    return if (isNotificationRuntimePermissionDenied() || !manager.areNotificationsEnabled()) {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
    } else {
        Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            .putExtra(Settings.EXTRA_CHANNEL_ID, PushNotifications.REWARD_CHANNEL_ID)
    }
}
