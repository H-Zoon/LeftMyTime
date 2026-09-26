package com.devidea.timeleft.telemetry

import android.content.Context
import android.content.SharedPreferences
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
    private val prefs: SharedPreferences,
) {
    val usageEnabled: Boolean get() = prefs.getBoolean(KEY_USAGE, false)
    val diagnosticsEnabled: Boolean get() = prefs.getBoolean(KEY_DIAGNOSTICS, false)

    fun initialize() = safely {
        applyUsage(usageEnabled)
        if (!usageEnabled) FirebaseAnalytics.getInstance(context).resetAnalyticsData()
        // Automatic upload stays off, including before Application.onCreate. A saved
        // opt-in authorizes pending reports only when a new process starts. Opt-out
        // never relies on Crashlytics' next-launch automatic-collection override.
        FirebaseCrashlytics.getInstance().apply {
            setCrashlyticsCollectionEnabled(false)
            if (diagnosticsEnabled) sendUnsentReports() else deleteUnsentReports()
        }
    }

    fun setUsageEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_USAGE, enabled).apply()
        safely {
            applyUsage(enabled)
            if (!enabled) FirebaseAnalytics.getInstance(context).resetAnalyticsData()
        }
    }

    fun setDiagnosticsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DIAGNOSTICS, enabled).apply()
        if (!enabled) safely { FirebaseCrashlytics.getInstance().deleteUnsentReports() }
    }

    fun record(event: UsageEvent) {
        if (!usageEnabled) return
        safely { FirebaseAnalytics.getInstance(context).logEvent(event.eventName, null) }
    }

    private fun applyUsage(enabled: Boolean) {
        val analytics = FirebaseAnalytics.getInstance(context)
        analytics.setConsent(mapOf(
            FirebaseAnalytics.ConsentType.ANALYTICS_STORAGE to if (enabled)
                FirebaseAnalytics.ConsentStatus.GRANTED else FirebaseAnalytics.ConsentStatus.DENIED,
            FirebaseAnalytics.ConsentType.AD_STORAGE to FirebaseAnalytics.ConsentStatus.DENIED,
            FirebaseAnalytics.ConsentType.AD_USER_DATA to FirebaseAnalytics.ConsentStatus.DENIED,
            FirebaseAnalytics.ConsentType.AD_PERSONALIZATION to FirebaseAnalytics.ConsentStatus.DENIED,
        ))
        analytics.setAnalyticsCollectionEnabled(enabled)
    }

    private inline fun safely(action: () -> Unit) {
        try { action() }
        catch (_: Exception) { Log.w("AppTelemetry", "Telemetry unavailable") }
    }

    companion object {
        private const val KEY_USAGE = "privacy_usage_opt_in_v1"
        private const val KEY_DIAGNOSTICS = "privacy_diagnostics_opt_in_v1"
    }
}
