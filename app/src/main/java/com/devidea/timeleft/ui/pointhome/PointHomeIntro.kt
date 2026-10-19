package com.devidea.timeleft.ui.pointhome

import android.Manifest
import android.annotation.SuppressLint
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.devidea.timeleft.R
import com.devidea.timeleft.messaging.PushNotifications
import com.devidea.timeleft.messaging.RewardMessaging
import com.devidea.timeleft.messaging.RewardPermissionAction
import com.devidea.timeleft.messaging.rewardNotificationSettingsIntent
import com.devidea.timeleft.messaging.rewardPermissionAction
import com.devidea.timeleft.notification.isNotificationRuntimePermissionDenied
import com.devidea.timeleft.pointhome.PointHomeIntroMode
import com.devidea.timeleft.telemetry.UsageEvent
import com.devidea.timeleft.ui.permission.findActivity
import com.devidea.timeleft.ui.permission.hasRequestedNotificationPermission
import com.devidea.timeleft.ui.permission.markNotificationPermissionRequested
import com.devidea.timeleft.ui.theme.LayoutTokens
import com.devidea.timeleft.ui.theme.Spacing

private const val STAGE_SHEET = 0
private const val STAGE_PERMISSION = 1
private const val STAGE_SETTINGS_GUIDE = 2
private const val STEP_INTRODUCTION = 1
private const val STEP_CHOICE = 2

/**
 * Introduces the PointHome floating button, then asks whether to receive reward notifications.
 * The sheet is the only place that explains the choice; the system permission follows immediately
 * and never changes the saved choice when it is refused.
 *
 * [onCompleted] runs once when the user ends the sheet, in any way; [onFinished] removes this flow
 * after any system permission or settings follow-up has ended.
 */
// POST_NOTIFICATIONS is only requested when rewardPermissionAction reports a denied runtime permission,
// which exists only on API 33+; below that the constant is never used.
@SuppressLint("InlinedApi")
@Composable
internal fun PointHomeIntroFlow(
    mode: PointHomeIntroMode,
    onCompleted: () -> Unit,
    onUsage: (UsageEvent) -> Unit,
    onFinished: () -> Unit,
) {
    val context = LocalContext.current
    var stage by rememberSaveable { mutableIntStateOf(STAGE_SHEET) }
    var step by rememberSaveable { mutableIntStateOf(STEP_INTRODUCTION) }
    var shownRecorded by rememberSaveable { mutableStateOf(false) }
    val choiceOffered = mode == PointHomeIntroMode.Choice

    fun finish(@StringRes message: Int?) {
        message?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
        onFinished()
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        RewardMessaging.refresh()
        finish(
            if (PushNotifications.canPostRewardNotifications(context)) R.string.pointhome_intro_accepted
            else R.string.settings_reward_blocked,
        )
    }

    LaunchedEffect(Unit) {
        if (!shownRecorded) {
            shownRecorded = true
            onUsage(UsageEvent.PointHomeIntroShown)
        }
    }

    fun accept() {
        if (stage != STAGE_SHEET) return
        // The choice is saved first and kept even when the system permission is refused.
        RewardMessaging.setConsent(true)
        onUsage(UsageEvent.PointHomeIntroAccepted)
        onCompleted()
        val action = rewardPermissionAction(
            canPostRewards = PushNotifications.canPostRewardNotifications(context),
            runtimePermissionDenied = context.isNotificationRuntimePermissionDenied(),
            requestedBefore = context.hasRequestedNotificationPermission(),
            rationaleAvailable = context.findActivity()
                ?.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) == true,
        )
        when (action) {
            RewardPermissionAction.None -> finish(R.string.pointhome_intro_accepted)
            RewardPermissionAction.RequestPermission -> {
                context.markNotificationPermissionRequested()
                stage = STAGE_PERMISSION
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            RewardPermissionAction.OpenSettings -> stage = STAGE_SETTINGS_GUIDE
        }
    }

    fun decline() {
        if (stage != STAGE_SHEET) return
        onUsage(UsageEvent.PointHomeIntroDeclined)
        onCompleted()
        finish(R.string.pointhome_intro_declined)
    }

    fun close() {
        if (stage != STAGE_SHEET) return
        // Closing never asks again; the settings path is the only way back, so say where it is.
        if (choiceOffered) onUsage(UsageEvent.PointHomeIntroClosed)
        onCompleted()
        finish(if (choiceOffered) R.string.pointhome_intro_closed else null)
    }

    when (stage) {
        STAGE_SHEET -> PointHomeIntroSheet(
            step = step,
            totalSteps = if (choiceOffered) 2 else 1,
            consentNotice = stringResource(R.string.pointhome_intro_consent_notice),
            onNext = { step = STEP_CHOICE },
            onAccept = ::accept,
            onDecline = ::decline,
            onClose = ::close,
        )
        STAGE_SETTINGS_GUIDE -> AlertDialog(
            onDismissRequest = { finish(R.string.settings_reward_blocked) },
            title = { Text(stringResource(R.string.pointhome_intro_settings_title)) },
            text = { Text(stringResource(R.string.pointhome_intro_settings_message)) },
            confirmButton = {
                TextButton(onClick = {
                    context.startActivity(context.rewardNotificationSettingsIntent())
                    onFinished()
                }) { Text(stringResource(R.string.notification_settings_open)) }
            },
            dismissButton = {
                TextButton(onClick = { finish(R.string.settings_reward_blocked) }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
        // STAGE_PERMISSION: the system dialog owns the screen until the launcher returns.
    }
}

/**
 * Stateless so the debug gallery can render the real sheet. Dismissing it by any gesture is [onClose].
 * [consentNotice] is a reserved slot for the advertising-information consent wording; it is hidden while blank.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PointHomeIntroSheet(
    step: Int,
    totalSteps: Int,
    consentNotice: String,
    onNext: () -> Unit,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onClose: () -> Unit,
) {
    val contentDensity = LocalDensity.current
    val maxSheetHeight = LocalConfiguration.current.screenHeightDp.dp * LayoutTokens.DetailSheetMaxHeightFraction
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
        modifier = Modifier.heightIn(max = maxSheetHeight),
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        // Dialog windows may replace LocalDensity; preserve the caller's text scale.
        CompositionLocalProvider(LocalDensity provides contentDensity) {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(horizontal = LayoutTokens.ScreenHorizontal).padding(bottom = Spacing.xxl),
                verticalArrangement = Arrangement.spacedBy(Spacing.l),
            ) {
                if (totalSteps > 1) {
                    val stepDescription = stringResource(R.string.pointhome_intro_step_description, step, totalSteps)
                    Text(
                        stringResource(R.string.pointhome_intro_step, step, totalSteps),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.semantics { contentDescription = stepDescription },
                    )
                }
                if (step == STEP_INTRODUCTION) {
                    IntroductionContent(totalSteps = totalSteps, onNext = onNext, onClose = onClose)
                } else {
                    ChoiceContent(consentNotice = consentNotice, onAccept = onAccept, onDecline = onDecline)
                }
            }
        }
    }
}

@Composable
private fun IntroductionContent(totalSteps: Int, onNext: () -> Unit, onClose: () -> Unit) {
    SheetText(
        title = stringResource(R.string.pointhome_intro_title),
        body = stringResource(R.string.pointhome_intro_body, stringResource(R.string.pointhome_floating_message)),
    )
    if (totalSteps > 1) {
        SheetButtons(
            primary = stringResource(R.string.pointhome_intro_next), onPrimary = onNext,
            secondary = stringResource(R.string.pointhome_intro_later), onSecondary = onClose,
        )
    } else {
        // Someone who already receives rewards only needs the introduction.
        SheetButtons(primary = stringResource(R.string.pointhome_intro_confirm), onPrimary = onClose)
    }
}

@Composable
private fun ChoiceContent(consentNotice: String, onAccept: () -> Unit, onDecline: () -> Unit) {
    SheetText(
        title = stringResource(R.string.pointhome_intro_choice_title),
        body = stringResource(R.string.pointhome_intro_choice_body, stringResource(R.string.settings_reward_notifications)),
    )
    if (consentNotice.isNotBlank()) {
        Text(
            consentNotice,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    SheetButtons(
        primary = stringResource(R.string.pointhome_intro_accept), onPrimary = onAccept,
        secondary = stringResource(R.string.pointhome_intro_decline), onSecondary = onDecline,
    )
}

@Composable
private fun SheetText(title: String, body: String) {
    Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
    Text(body, style = MaterialTheme.typography.bodyLarge)
}

/** Both choices share size and touch target; declining is never hidden or reduced. */
@Composable
private fun SheetButtons(
    primary: String,
    onPrimary: () -> Unit,
    secondary: String? = null,
    onSecondary: () -> Unit = {},
) {
    val colors = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
        Button(
            onClick = onPrimary,
            shape = MaterialTheme.shapes.small,
            colors = ButtonDefaults.buttonColors(containerColor = colors.onBackground, contentColor = colors.background),
            modifier = Modifier.fillMaxWidth().heightIn(min = PRIMARY_ACTION_MIN_HEIGHT),
        ) { Text(primary) }
        if (secondary != null) {
            OutlinedButton(
                onClick = onSecondary,
                shape = MaterialTheme.shapes.small,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.onBackground),
                modifier = Modifier.fillMaxWidth().heightIn(min = PRIMARY_ACTION_MIN_HEIGHT),
            ) { Text(secondary) }
        }
    }
}

// DESIGN_SYSTEM: main input actions are at least 52dp high, the same as the editor's save action.
private val PRIMARY_ACTION_MIN_HEIGHT = 52.dp
