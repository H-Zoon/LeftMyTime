package com.devidea.timeleft.messaging

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.preference.PreferenceManager
import com.devidea.timeleft.R
import com.devidea.timeleft.notification.isNotificationRuntimePermissionDenied
import com.devidea.timeleft.preferences.UserPreferences
import com.devidea.timeleft.ui.components.TimeLeftInlineButton
import com.devidea.timeleft.ui.components.TimeLeftTopAppBar
import com.devidea.timeleft.ui.theme.LayoutTokens
import com.devidea.timeleft.ui.theme.Spacing
import com.devidea.timeleft.ui.theme.TimeLeftTheme
import java.text.DateFormat
import java.util.Date

/** Available only in debug builds; open with the adb command in design/FCM_TESTING.md. */
class FcmDiagnosticsActivity : ComponentActivity() {
    private var notificationsEnabled by mutableStateOf(false)
    private var runtimePermissionDenied by mutableStateOf(false)
    private val permissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { refreshNotificationState() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        setContent {
            val state by PushMessaging.state.collectAsStateWithLifecycle()
            val registrationFailed = state.error?.startsWith("registration:") == true
            TimeLeftTheme(
                themeMode = prefs.getString(UserPreferences.KEY_THEME, UserPreferences.THEME_AUTO)
                    ?: UserPreferences.THEME_AUTO,
                paletteKey = prefs.getString(UserPreferences.KEY_COLOR_THEME, UserPreferences.COLOR_THEME_CLAY)
                    ?: UserPreferences.COLOR_THEME_CLAY,
                designKey = prefs.getString(UserPreferences.KEY_DESIGN, UserPreferences.DESIGN_TIME_FOCUS)
                    ?: UserPreferences.DESIGN_TIME_FOCUS,
            ) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    Column(Modifier.fillMaxSize()) {
                        TimeLeftTopAppBar(stringResource(R.string.fcm_debug_title), onBack = { finish() })
                        Column(
                            Modifier.fillMaxWidth().weight(1f)
                                .verticalScroll(rememberScrollState()).navigationBarsPadding()
                                .padding(horizontal = LayoutTokens.ScreenHorizontal, vertical = LayoutTokens.ScreenVertical),
                            verticalArrangement = Arrangement.spacedBy(Spacing.l),
                        ) {
                            Text(stringResource(R.string.fcm_debug_explanation), style = MaterialTheme.typography.bodyMedium)
                            DiagnosticField(
                                stringResource(R.string.fcm_debug_registration),
                                stringResource(when {
                                    state.registering -> R.string.fcm_debug_registering
                                    registrationFailed -> R.string.fcm_debug_failed
                                    state.installationId != null -> R.string.fcm_debug_registered
                                    else -> R.string.fcm_debug_waiting
                                }),
                            )
                            state.error?.let {
                                Text(stringResource(R.string.fcm_debug_error, it), color = MaterialTheme.colorScheme.error)
                            }
                            DiagnosticField(stringResource(R.string.fcm_debug_fid),
                                state.installationId ?: stringResource(R.string.fcm_debug_unavailable))
                            DiagnosticField(stringResource(R.string.fcm_debug_registered_at), timeText(state.registeredAtMillis))
                            TimeLeftInlineButton(
                                onClick = { state.installationId?.let(::copyFid) },
                                enabled = state.installationId != null && !state.registering && !registrationFailed,
                            ) { Text(stringResource(R.string.fcm_debug_copy_fid)) }
                            TimeLeftInlineButton(onClick = PushMessaging::register, enabled = !state.registering) {
                                Text(stringResource(R.string.fcm_debug_register_again))
                            }
                            DiagnosticField(stringResource(R.string.fcm_debug_notification_permission),
                                stringResource(if (notificationsEnabled) R.string.fcm_debug_notifications_enabled
                                else R.string.fcm_debug_notifications_disabled))
                            if (runtimePermissionDenied) {
                                TimeLeftInlineButton(onClick = ::requestNotificationPermission) {
                                    Text(stringResource(R.string.fcm_debug_request_permission))
                                }
                            }
                            TimeLeftInlineButton(onClick = ::openNotificationSettings) {
                                Text(stringResource(R.string.fcm_debug_open_settings))
                            }
                            DiagnosticField(stringResource(R.string.fcm_debug_received_at), timeText(state.lastReceivedAtMillis))
                            state.lastMessageId?.let { DiagnosticField(stringResource(R.string.fcm_debug_message_id), it) }
                            if (state.lastReceivedAtMillis != null) {
                                DiagnosticField(stringResource(R.string.fcm_debug_data_keys), state.lastDataKeyCount.toString())
                            }
                            if (state.notificationBlocked) {
                                Text(stringResource(R.string.fcm_debug_display_blocked), style = MaterialTheme.typography.bodyMedium)
                            }
                            Text(stringResource(R.string.fcm_debug_background_note),
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshNotificationState()
    }

    private fun refreshNotificationState() {
        runtimePermissionDenied = isNotificationRuntimePermissionDenied()
        notificationsEnabled = PushNotifications.canPostNotifications(this)
        PushMessaging.notificationResult(blocked = !notificationsEnabled)
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionRequest.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun openNotificationSettings() {
        startActivity(Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            .putExtra(Settings.EXTRA_CHANNEL_ID, PushNotifications.CHANNEL_ID))
    }

    private fun copyFid(fid: String) {
        getSystemService(ClipboardManager::class.java).setPrimaryClip(
            ClipData.newPlainText(getString(R.string.fcm_debug_fid), fid),
        )
        Toast.makeText(this, R.string.fcm_debug_copied, Toast.LENGTH_SHORT).show()
    }

    @Composable
    private fun timeText(millis: Long?): String = millis?.let {
        DateFormat.getDateTimeInstance().format(Date(it))
    } ?: stringResource(R.string.fcm_debug_unavailable)
}

@Composable
private fun DiagnosticField(label: String, value: String) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        SelectionContainer { Text(value, style = MaterialTheme.typography.bodyLarge) }
    }
}
