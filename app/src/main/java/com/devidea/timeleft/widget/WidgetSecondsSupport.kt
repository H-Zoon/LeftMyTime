package com.devidea.timeleft.widget

import android.os.Build
import android.widget.RemoteViews

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
    }

    fun available(): Boolean = capability().productionProfile != null
}
