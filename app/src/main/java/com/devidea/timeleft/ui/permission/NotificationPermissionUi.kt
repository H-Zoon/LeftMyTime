package com.devidea.timeleft.ui.permission

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.preference.PreferenceManager
import com.devidea.timeleft.R
import com.devidea.timeleft.notification.canPostReminderNotifications
import com.devidea.timeleft.notification.isNotificationRuntimePermissionDenied

private const val NOTIFICATION_PERMISSION_REQUESTED = "notification_permission_requested"

fun Context.markNotificationPermissionRequested() {
    PreferenceManager.getDefaultSharedPreferences(this)
        .edit()
        .putBoolean(NOTIFICATION_PERMISSION_REQUESTED, true)
        .apply()
}

fun Context.shouldOpenNotificationSettings(): Boolean =
    !canPostReminderNotifications() &&
        (!isNotificationRuntimePermissionDenied() ||
            PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean(NOTIFICATION_PERMISSION_REQUESTED, false) &&
            findActivity()?.shouldShowRequestPermissionRationale(
                Manifest.permission.POST_NOTIFICATIONS
            ) == false)

@Composable
fun NotificationPermissionExplanationDialog(
    onAllow: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.notification_permission_title)) },
        text = { Text(stringResource(R.string.notification_permission_message)) },
        confirmButton = {
            TextButton(onClick = onAllow) {
                Text(stringResource(R.string.notification_permission_allow))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.notification_permission_not_now))
            }
        }
    )
}

@Composable
fun NotificationPermissionSettingsDialog(
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.notification_settings_title)) },
        text = { Text(stringResource(R.string.notification_settings_message)) },
        confirmButton = {
            TextButton(onClick = onOpenSettings) {
                Text(stringResource(R.string.notification_settings_open))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
