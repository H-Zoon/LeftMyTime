package com.devidea.timeleft.messaging

import android.content.Context
import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.devidea.timeleft.settings.SettingsActivity
import java.util.UUID
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RewardAdConsentTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun previousRewardSelectionsArePreservedWithoutInventingANewConsentRecord() {
        val name = "reward_ads_test_${UUID.randomUUID()}"
        val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)
        try {
            assertFalse(RewardAdConsent.read(prefs).selection)
            for (selected in listOf(true, false)) {
                assertTrue(prefs.edit().putBoolean("reward_notifications_consent", selected).commit())
                val saved = RewardAdConsent.read(prefs)
                assertEquals(selected, saved.selection)
                assertEquals(0L, saved.changedAt)
                assertFalse(prefs.contains("reward_ads_consent_notice_version"))
            }
        } finally { context.deleteSharedPreferences(name) }
    }

    @Test fun explicitSelectionsRecordTheirNoticeVersionAndDateWithoutChangingOtherSettings() {
        val name = "reward_ads_test_${UUID.randomUUID()}"
        val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)
        try {
            assertTrue(prefs.edit().putString("unrelated", "preserved").commit())
            assertTrue(RewardAdConsent.write(prefs, true, 100L))
            assertEquals(RewardAdConsent.Saved(true, 100L), RewardAdConsent.read(prefs))
            assertTrue(RewardAdConsent.write(prefs, false, 200L))
            val reopened = context.createPackageContext(context.packageName, 0)
                .getSharedPreferences(name, Context.MODE_PRIVATE)
            assertEquals(RewardAdConsent.Saved(false, 200L), RewardAdConsent.read(reopened))
            assertEquals(1, reopened.getInt("reward_ads_consent_notice_version", 0))
            assertEquals("preserved", reopened.getString("unrelated", null))
        } finally { context.deleteSharedPreferences(name) }
    }

    @Test fun aConsoleNotificationRouteIsConsumedOnceAndDoesNotChangeConsent() {
        val intent = Intent().putExtra(PointHomePushOpen.KIND_KEY, "reward")
            .putExtra(PointHomePushOpen.TARGET_KEY, "pointhome")
            .putExtra("unrelated", "keep")
        assertTrue(PointHomePushOpen.consume(intent))
        assertFalse(PointHomePushOpen.consume(intent))
        assertEquals("keep", intent.getStringExtra("unrelated"))

        val removedTarget = Intent().putExtra(PointHomePushOpen.KIND_KEY, "reward")
            .putExtra(PointHomePushOpen.TARGET_KEY, "reward_settings")
        assertFalse(PointHomePushOpen.consume(removedTarget))

        val open = SettingsActivity.rewardNotificationIntent(context)
        assertEquals(SettingsActivity::class.java.name, open.component?.className)
        assertTrue(open.getBooleanExtra("reward_notifications_only", false))
        assertFalse(open.getBooleanExtra("unsubscribe_reward_ads", true))
        val unsubscribe = SettingsActivity.rewardNotificationIntent(context, unsubscribe = true)
        assertTrue(unsubscribe.getBooleanExtra("unsubscribe_reward_ads", false))
    }
}
