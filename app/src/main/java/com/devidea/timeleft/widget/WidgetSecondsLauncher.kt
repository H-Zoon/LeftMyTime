package com.devidea.timeleft.widget

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

/**
 * The document version comes from the framework in this process; the launcher still has to draw
 * the document. A launcher outside this list never receives draw instructions: an unknown or
 * future launcher gets the Chronometer countdown (see [WidgetSecondsDelivery]) or the static
 * widget instead of risking a crash in the home app.
 *
 * Live widget checks: Launcher3 (document 6) and Pixel Launcher (documents 8, 9) on 2026-10-06.
 * One UI Home 17.0.05.19 (One UI 8, Galaxy A33, document 6) was rejected on 2026-10-19: its
 * `HoneyAppWidgetHostView.updateAppWidgetInternal` threw `NullPointerException: getPackage(...) must
 * not be null` on every draw-instruction delivery and the launcher process died.
 */
internal object WidgetSecondsLauncher {
    private val verified = setOf(
        "com.android.launcher3",
        "com.google.android.apps.nexuslauncher",
    )

    fun isVerified(packageName: String?): Boolean = packageName != null && packageName in verified

    /** The default home app hosts launcher widgets. A chooser or an unreadable result is never verified. */
    fun homePackage(context: Context): String? = try {
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        context.packageManager.resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName
    } catch (_: RuntimeException) {
        null
    }
}
