package com.devidea.timeleft.telemetry

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** A closed, parameter-free vocabulary: never accept titles, dates, IDs or arbitrary strings. */
enum class UsageEvent(val eventName: String) {
    ScheduleCreateOpened("schedule_create_opened"),
    ScheduleCreated("schedule_created"),
    WidgetConfigured("widget_configured"),
    ThemeApplied("theme_applied"),
    ThemeCancelled("theme_cancelled"),
    FocusStarted("focus_started"),
    // PointHome intro funnel: only whether it was shown and how it ended, never consent state or IDs.
    PointHomeIntroShown("pointhome_intro_shown"),
    PointHomeIntroAccepted("pointhome_intro_accepted"),
    PointHomeIntroDeclined("pointhome_intro_declined"),
    PointHomeIntroClosed("pointhome_intro_closed"),
}

/** Fixed app entry paths only; no message content, campaign strings or user identifiers. */
enum class PointHomeEntrySource(val analyticsValue: String) {
    FloatingButton("floating_button"),
    RewardPush("reward_push"),
}

@Singleton
class AppTelemetry @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    fun initialize() {
        // Explicitly replace SDK overrides persisted by the former opt-in version.
        // Manifest defaults alone do not override those saved collection settings.
        safely {
            enableUsageCollection()
        }
        safely {
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
        }
    }

    fun record(event: UsageEvent) {
        safely { FirebaseAnalytics.getInstance(context).logEvent(event.eventName, null) }
    }

    /** Count confirmed SDK opens; Analytics supplies event time and distinct-user aggregation. */
    fun recordPointHomeOpened(source: PointHomeEntrySource) {
        safely {
            FirebaseAnalytics.getInstance(context).logEvent(
                "pointhome_open",
                Bundle().apply { putString("entry_source", source.analyticsValue) },
            )
        }
    }

    private fun enableUsageCollection() {
        val analytics = FirebaseAnalytics.getInstance(context)
        analytics.setConsent(mapOf(
            FirebaseAnalytics.ConsentType.ANALYTICS_STORAGE to FirebaseAnalytics.ConsentStatus.GRANTED,
            FirebaseAnalytics.ConsentType.AD_STORAGE to FirebaseAnalytics.ConsentStatus.DENIED,
            FirebaseAnalytics.ConsentType.AD_USER_DATA to FirebaseAnalytics.ConsentStatus.DENIED,
            FirebaseAnalytics.ConsentType.AD_PERSONALIZATION to FirebaseAnalytics.ConsentStatus.DENIED,
        ))
        analytics.setAnalyticsCollectionEnabled(true)
    }

    private inline fun safely(action: () -> Unit) {
        try { action() }
        catch (_: Exception) { Log.w("AppTelemetry", "Telemetry unavailable") }
    }
}
