package com.devidea.timeleft.messaging

import android.content.Intent

/** Only this fixed destination is supported; push data cannot name arbitrary screens or methods. */
object PointHomePushOpen {
    const val KIND_KEY = "notification_kind"
    const val TARGET_KEY = "open_target"
    const val REWARD_KIND = "reward"
    const val POINT_HOME_TARGET = "pointhome"

    fun isRequested(kind: String?, target: String?): Boolean =
        kind == REWARD_KIND && target == POINT_HOME_TARGET

    /** Both app-posted PendingIntents and FCM's background launcher extras use the same contract. */
    fun consume(intent: Intent?): Boolean {
        if (intent == null) return false
        val requested = isRequested(intent.getStringExtra(KIND_KEY), intent.getStringExtra(TARGET_KEY))
        intent.removeExtra(TARGET_KEY)
        return requested
    }
}
