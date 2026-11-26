package com.devidea.timeleft.messaging

import android.content.SharedPreferences

internal object RewardAdConsent {
    private const val SELECTION = "reward_notifications_consent"
    private const val CHANGED_AT = "reward_ads_consent_changed_at"
    private const val NOTICE_VERSION = "reward_ads_consent_notice_version"

    data class Saved(val selection: Boolean, val changedAt: Long)

    fun read(prefs: SharedPreferences): Saved {
        return Saved(
            selection = prefs.getBoolean(SELECTION, false),
            changedAt = prefs.getLong(CHANGED_AT, 0L),
        )
    }

    /** Called on the messaging executor after an explicit choice; no consent is inferred. */
    fun write(prefs: SharedPreferences, granted: Boolean, changedAt: Long): Boolean =
        prefs.edit().putBoolean(SELECTION, granted).putLong(CHANGED_AT, changedAt)
            .putInt(NOTICE_VERSION, 1).commit()
}
