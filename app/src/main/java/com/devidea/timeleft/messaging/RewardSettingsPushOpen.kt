package com.devidea.timeleft.messaging

import android.content.Intent

/** Fixed app-owned destination for Console notifications; no arbitrary Activity or URL is accepted. */
internal object RewardSettingsPushOpen {
    const val TARGET = "reward_settings"

    fun isRequested(kind: String?, target: String?): Boolean =
        kind == PointHomePushOpen.REWARD_KIND && target == TARGET

    fun consume(intent: Intent?): Boolean {
        if (intent == null || !isRequested(
                intent.getStringExtra(PointHomePushOpen.KIND_KEY),
                intent.getStringExtra(PointHomePushOpen.TARGET_KEY),
            )) return false
        intent.removeExtra(PointHomePushOpen.TARGET_KEY)
        return true
    }
}
