package com.devidea.timeleft.ui.theme

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources

/** The device mode, independent of an AppCompat override applied to the current activity. */
internal fun systemUsesDarkTheme(context: Context): Boolean =
    when (Resources.getSystem().configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
        Configuration.UI_MODE_NIGHT_YES -> true
        Configuration.UI_MODE_NIGHT_NO -> false
        else -> context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    }
