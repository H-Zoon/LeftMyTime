package com.devidea.timeleft.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.Modifier
import com.devidea.timeleft.R
import com.devidea.timeleft.messaging.RewardMessagingState
import com.devidea.timeleft.ui.components.TimeLeftInlineButton
import com.devidea.timeleft.ui.theme.LayoutTokens

/** Notification taps land directly on the same consent control used by app settings. */
@Composable
fun RewardNotificationSettingsScreen(
    state: RewardMessagingState,
    onBack: () -> Unit,
    onConsentChanged: (Boolean) -> Unit,
    onOpenSystemSettings: () -> Unit,
    onRetry: () -> Unit,
    onOpenReward: () -> Unit,
) {
    SettingsScaffold(title = stringResource(R.string.reward_notification_settings_title), onBack = onBack) {
        Text(stringResource(R.string.reward_notification_settings_body),
            style = MaterialTheme.typography.bodyLarge)
        Button(
            onClick = { onConsentChanged(false) },
            enabled = !state.consentReady || state.consentGranted,
            modifier = Modifier.fillMaxWidth().heightIn(min = LayoutTokens.MinTouchTarget),
        ) { Text(stringResource(R.string.reward_notification_unsubscribe)) }
        RewardNotificationPreferences(
            consentGranted = state.consentGranted,
            notificationsAllowed = state.notificationsAllowed,
            topicSync = state.topicSync,
            consentReady = state.consentReady,
            onConsentChanged = onConsentChanged,
            onOpenSystemSettings = onOpenSystemSettings,
            onRetry = onRetry,
        )
        TimeLeftInlineButton(onClick = onOpenReward) {
            Text(stringResource(R.string.reward_notification_open_reward))
        }
    }
}
