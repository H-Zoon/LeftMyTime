package com.devidea.timeleft.widget

import android.os.Build
import android.widget.RemoteViews

internal object WidgetSecondsSupport {
    /** API level alone does not describe the host's document operations and clock variables. */
    fun available(): Boolean = if (Build.VERSION.SDK_INT < 35) false else try {
        RemoteViews.DrawInstructions.getSupportedVersion() >= 9
    } catch (_: LinkageError) {
        false
    } catch (_: RuntimeException) {
        false
    }
}
