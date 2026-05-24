package com.devidea.timeleft.notification

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

fun Context.isNotificationRuntimePermissionDenied(): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED

fun Context.canPostReminderNotifications(): Boolean {
    if (isNotificationRuntimePermissionDenied()) return false

    val manager = getSystemService(NotificationManager::class.java)
    if (!manager.areNotificationsEnabled()) return false

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
        manager.getNotificationChannel(ReminderScheduler.CHANNEL_ID)?.importance ==
        NotificationManager.IMPORTANCE_NONE
    ) {
        return false
    }

    return true
}
