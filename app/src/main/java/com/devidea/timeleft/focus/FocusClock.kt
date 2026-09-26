package com.devidea.timeleft.focus

import android.content.Context
import android.os.SystemClock
import android.provider.Settings

internal fun readFocusClock(context: Context) = FocusClockReading(
    elapsedRealtime = SystemClock.elapsedRealtime(),
    bootCount = Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, 0),
)
