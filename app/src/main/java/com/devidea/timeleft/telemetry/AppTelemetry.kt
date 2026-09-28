package com.devidea.timeleft.telemetry

import android.content.Context
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
