package com.devidea.timeleft.widget

import android.content.Context
import android.os.Build
import android.widget.RemoteViews
import com.devidea.timeleft.config.AppRemoteConfig
import com.devidea.timeleft.focus.canUseExactAlarms

internal object WidgetSecondsSupport {
    /** API level alone does not describe the host's document operations and clock variables. */
    fun capability(): WidgetSecondsCapability {
        val version = if (Build.VERSION.SDK_INT < 35) null else try {
            RemoteViews.DrawInstructions.getSupportedVersion()
        } catch (_: LinkageError) {
            null
        } catch (_: RuntimeException) {
            null
        }
        return WidgetSecondsCapability.forHost(Build.VERSION.SDK_INT, version)
            .withRemoteSecondsEnabled(AppRemoteConfig.widgetSecondsEnabled)
    }

    /** Framework and remote switch only. Use [delivery] before sending a widget to a launcher. */
    fun available(): Boolean = capability().productionProfile != null

    fun delivery(context: Context): WidgetSecondsDelivery = WidgetSecondsDelivery.resolve(
        platformAvailable = available(),
        launcherVerified = WidgetSecondsLauncher.isVerified(WidgetSecondsLauncher.homePackage(context)),
        exactAlarms = canUseExactAlarms(context),
    )
}

/** How a seconds widget reaches the launcher that hosts it. */
internal enum class WidgetSecondsDelivery {
    /** Seconds are off: unsupported OS, or the remote switch is off. Static widget. */
    Unavailable,
    /** Launcher verified for draw instructions: live number and ruler, no app process needed. */
    DrawInstructions,
    /** Unverified launcher: host-ticked number, snapshot ruler, exact alarm at the end. */
    Chronometer,
    /** Unverified launcher without exact-alarm access: the end could not be replaced in time. Static widget. */
    NeedsExactAlarm;

    val showsSeconds: Boolean get() = this == DrawInstructions || this == Chronometer

    companion object {
        /**
         * The launcher decides how seconds can be delivered, not the framework version. Chronometer
         * counts below zero, so it is used only when an exact alarm can swap the widget at the end.
         */
        fun resolve(platformAvailable: Boolean, launcherVerified: Boolean, exactAlarms: Boolean): WidgetSecondsDelivery = when {
            !platformAvailable -> Unavailable
            launcherVerified -> DrawInstructions
            exactAlarms -> Chronometer
            else -> NeedsExactAlarm
        }
    }
}

/** Remote configuration can disable an existing production path, never enable an unsupported host. */
internal fun WidgetSecondsCapability.withRemoteSecondsEnabled(enabled: Boolean): WidgetSecondsCapability =
    if (enabled) this else copy(productionProfile = null)
