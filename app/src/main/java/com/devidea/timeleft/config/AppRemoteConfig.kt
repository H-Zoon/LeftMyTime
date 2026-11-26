package com.devidea.timeleft.config

import android.content.Context
import android.util.Log
import com.devidea.timeleft.R
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseApp
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import java.util.concurrent.Executor
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Fetch for the next process start; new network responses do not change running UI. */
object AppRemoteConfig {
    private const val WIDGET_SECONDS_ENABLED = "widget_seconds_enabled"
    private const val POINT_HOME_ENTRY_ENABLED = "pointhome_entry_enabled"
    private const val DEFAULT_POINT_HOME_ENTRY_ENABLED = true
    private const val FETCH_INTERVAL_SECONDS = 12 * 60 * 60L
    private val initializationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val completionExecutor = Executor { work -> initializationScope.launch { work.run() } }
    private val initializeLock = Any()
    private var initialized = false

    @Volatile private var processWidgetSecondsEnabled = true
    private val pointHomeEntrySnapshot = CompletableDeferred<Boolean>()

    /** Default immediately, then the startup cache/activation snapshot; consumers never wait on SDK I/O. */
    val widgetSecondsEnabled: Boolean get() = processWidgetSecondsEnabled

    /** Wait only for startup cache activation, never for this process's network fetch. */
    suspend fun isPointHomeEntryEnabled(): Boolean = pointHomeEntrySnapshot.await()

    fun initialize(context: Context) {
        synchronized(initializeLock) {
            if (initialized) return
            initialized = true
        }
        val applicationContext = context.applicationContext
        // Synchronous SDK getters can wait for disk caches. Keep the entire setup off the main thread.
        initializationScope.launch {
            safely {
                val app = FirebaseApp.initializeApp(applicationContext) ?: run {
                    pointHomeEntrySnapshot.complete(DEFAULT_POINT_HOME_ENTRY_ENABLED)
                    return@safely
                }
                val remoteConfig = FirebaseRemoteConfig.getInstance(app)
                processWidgetSecondsEnabled = readWidgetSecondsEnabled(remoteConfig)

                val defaults = remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)
                val settings = remoteConfig.setConfigSettingsAsync(
                    FirebaseRemoteConfigSettings.Builder()
                        .setMinimumFetchIntervalInSeconds(FETCH_INTERVAL_SECONDS)
                        .build(),
                )
                // Defaults and initialization are asynchronous. Finish all local setup before activating
                // the previous fetch, and never start this process's fetch until activation has finished.
                Tasks.whenAllComplete(defaults, settings, remoteConfig.ensureInitialized())
                    .addOnCompleteListener(completionExecutor) {
                        safely {
                            remoteConfig.activate().addOnCompleteListener(completionExecutor) {
                                processWidgetSecondsEnabled = readWidgetSecondsEnabled(remoteConfig)
                                pointHomeEntrySnapshot.complete(readPointHomeEntryEnabled(remoteConfig))
                                // If settings could not be saved, avoid fetching with a stale interval.
                                if (settings.isSuccessful) safely { remoteConfig.fetch() }
                            }
                        }
                    }
            }
        }
    }

    private fun readWidgetSecondsEnabled(remoteConfig: FirebaseRemoteConfig): Boolean = try {
        val value = remoteConfig.getValue(WIDGET_SECONDS_ENABLED)
        // A missing key has the SDK's static false default. It must preserve existing app behavior.
        widgetSecondsEnabledFromRemoteValue(
            if (value.source == FirebaseRemoteConfig.VALUE_SOURCE_REMOTE) value.asString() else null,
        )
    } catch (_: Exception) {
        true
    }

    private fun readPointHomeEntryEnabled(remoteConfig: FirebaseRemoteConfig): Boolean = try {
        val value = remoteConfig.getValue(POINT_HOME_ENTRY_ENABLED)
        pointHomeEntryEnabledFromRemoteValue(
            if (value.source == FirebaseRemoteConfig.VALUE_SOURCE_REMOTE) value.asString() else null,
        )
    } catch (_: Exception) {
        DEFAULT_POINT_HOME_ENTRY_ENABLED
    }

    private inline fun safely(action: () -> Unit) {
        try { action() }
        catch (_: Exception) {
            Log.w("AppRemoteConfig", "Remote Config unavailable")
            pointHomeEntrySnapshot.complete(DEFAULT_POINT_HOME_ENTRY_ENABLED)
        }
    }
}

/** Only the canonical Boolean false disables seconds; missing/malformed values keep the default. */
internal fun widgetSecondsEnabledFromRemoteValue(value: String?): Boolean =
    value?.toBooleanStrictOrNull() ?: true

/** Preserve the existing entry UI unless the remote value is the canonical Boolean false. */
internal fun pointHomeEntryEnabledFromRemoteValue(value: String?): Boolean =
    value?.toBooleanStrictOrNull() ?: true
