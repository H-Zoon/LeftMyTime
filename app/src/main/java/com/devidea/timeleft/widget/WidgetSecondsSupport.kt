package com.devidea.timeleft.widget

import android.os.Build
import android.widget.RemoteViews
import com.devidea.timeleft.config.AppRemoteConfig

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

    fun available(): Boolean = capability().productionProfile != null
}

/** Remote configuration can disable an existing production path, never enable an unsupported host. */
internal fun WidgetSecondsCapability.withRemoteSecondsEnabled(enabled: Boolean): WidgetSecondsCapability =
    if (enabled) this else copy(productionProfile = null)
