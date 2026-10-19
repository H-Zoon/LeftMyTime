package com.devidea.timeleft.pointhome

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.edit
import com.devidea.timeleft.preferences.UserPreferences

/** Raise this to show the intro again for a new campaign; older completions stay valid for the old one. */
internal const val POINTHOME_INTRO_VERSION = 1

/** A fresh installation waits until its second start so the first run stays about remaining time. */
internal const val POINTHOME_INTRO_FRESH_INSTALL_MIN_LAUNCHES = 2

private const val FRESH_INSTALL_WINDOW_MILLIS = 60_000L

internal data class PointHomeIntroInputs(
    /** The SDK reported that its floating button was created, so the intro describes something visible. */
    val buttonReady: Boolean,
    val completedVersion: Int,
    val freshInstall: Boolean,
    val homeLaunches: Int,
    /** A reward notification opened this session; that user has already found the service. */
    val openedFromNotification: Boolean,
)

internal fun shouldShowPointHomeIntro(inputs: PointHomeIntroInputs): Boolean =
    inputs.buttonReady &&
        !inputs.openedFromNotification &&
        inputs.completedVersion < POINTHOME_INTRO_VERSION &&
        (!inputs.freshInstall || inputs.homeLaunches >= POINTHOME_INTRO_FRESH_INSTALL_MIN_LAUNCHES)

/** Someone who already chose reward notifications only needs the introduction, not the choice. */
internal enum class PointHomeIntroMode { Hidden, Choice, InformationOnly }

internal fun pointHomeIntroMode(rewardConsentGranted: Boolean): PointHomeIntroMode =
    if (rewardConsentGranted) PointHomeIntroMode.InformationOnly else PointHomeIntroMode.Choice

/** Device-local intro state. It is not part of schedule backups and never changes the reward consent. */
internal object PointHomeIntroStore {
    fun completedVersion(prefs: SharedPreferences): Int =
        prefs.getInt(UserPreferences.KEY_POINTHOME_INTRO_VERSION, 0)

    fun markCompleted(prefs: SharedPreferences) {
        prefs.edit { putInt(UserPreferences.KEY_POINTHOME_INTRO_VERSION, POINTHOME_INTRO_VERSION) }
    }

    /** Counts cold starts only while the intro is pending, and stops writing once the threshold is met. */
    fun recordHomeLaunch(prefs: SharedPreferences, countThisLaunch: Boolean): Int {
        val count = prefs.getInt(UserPreferences.KEY_POINTHOME_INTRO_HOME_LAUNCHES, 0)
        if (!countThisLaunch || completedVersion(prefs) >= POINTHOME_INTRO_VERSION ||
            count >= POINTHOME_INTRO_FRESH_INSTALL_MIN_LAUNCHES
        ) return count
        val next = count + 1
        prefs.edit { putInt(UserPreferences.KEY_POINTHOME_INTRO_HOME_LAUNCHES, next) }
        return next
    }
}

/**
 * True when the package has never been updated. If the system cannot say, stay conservative and
 * treat it as fresh, which only delays the intro to the next start.
 */
internal fun isFreshInstall(context: Context): Boolean = try {
    val packageManager = context.packageManager
    val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION")
        packageManager.getPackageInfo(context.packageName, 0)
    }
    info.lastUpdateTime - info.firstInstallTime < FRESH_INSTALL_WINDOW_MILLIS
} catch (_: Exception) {
    true
}
