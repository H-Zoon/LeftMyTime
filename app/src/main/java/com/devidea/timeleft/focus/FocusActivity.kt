package com.devidea.timeleft.focus

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devidea.timeleft.ItemGenerate
import com.devidea.timeleft.R
import com.devidea.timeleft.formatRemainingTime
import com.devidea.timeleft.notification.canPostReminderNotifications
import com.devidea.timeleft.preferences.UserPreferences
import com.devidea.timeleft.ui.components.RemainingTimeText
import com.devidea.timeleft.ui.components.TimeLeftTopAppBar
import com.devidea.timeleft.ui.components.TimeLeftSwitchRow
import com.devidea.timeleft.ui.components.TimeRuler
import com.devidea.timeleft.ui.theme.LayoutTokens
import com.devidea.timeleft.ui.theme.Spacing
import com.devidea.timeleft.ui.theme.TimeLeftTheme
import dagger.hilt.android.AndroidEntryPoint
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

@AndroidEntryPoint
class FocusActivity : AppCompatActivity() {
    @Inject lateinit var prefs: SharedPreferences
    private val model: FocusViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = intent.getIntExtra(ITEM_ID, -1)
        setContent {
            val state by model.state.collectAsStateWithLifecycle()
            val generator = remember { ItemGenerate(this) }
            var showHistory by rememberSaveable { mutableStateOf(id < 0) }
            var keepAwake by rememberSaveable { mutableStateOf(false) }
            var confirmStop by rememberSaveable { mutableStateOf(false) }
            val item = state.items.firstOrNull { it.id == id }
            val display = remember(item, state.now) { item?.let(generator::customTimeItem) }
            val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
            fun back() { if (showHistory && id > 0) showHistory = false else finish() }
            BackHandler { back() }
            DisposableEffect(keepAwake, showHistory, display?.focusState) {
                fun apply() {
                    val enabled = keepAwake && !showHistory && display?.focusState == FocusSession.RUNNING && lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
                    if (enabled) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
                val observer = LifecycleEventObserver { _, _ -> apply() }
                lifecycle.addObserver(observer); apply()
                onDispose { lifecycle.removeObserver(observer); window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
            }
            TimeLeftTheme(designKey = prefs.getString(UserPreferences.KEY_DESIGN, UserPreferences.DESIGN_TIME_FOCUS) ?: UserPreferences.DESIGN_TIME_FOCUS,
                themeMode = prefs.getString(UserPreferences.KEY_THEME, UserPreferences.THEME_AUTO) ?: UserPreferences.THEME_AUTO,
                paletteKey = prefs.getString(UserPreferences.KEY_COLOR_THEME, UserPreferences.COLOR_THEME_CLAY) ?: UserPreferences.COLOR_THEME_CLAY) {
                Scaffold(containerColor = MaterialTheme.colorScheme.background,
                    topBar = { TimeLeftTopAppBar(if (showHistory) stringResource(R.string.focus_history) else display?.title ?: stringResource(R.string.focus_default_title), ::back) }) { padding ->
                    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(horizontal = LayoutTokens.ScreenHorizontal, vertical = Spacing.l),
                        verticalArrangement = Arrangement.spacedBy(Spacing.l)) {
                        if (state.loading || state.failed || (!showHistory && display == null)) item {
                            Text(stringResource(if (state.failed) R.string.home_load_failed else if (state.loading) R.string.home_loading else R.string.widget_selected_deleted))
                            if (state.failed) TextButton(onClick = model::retry,
                                modifier = Modifier.heightIn(min = LayoutTokens.MinTouchTarget)) { Text(stringResource(R.string.widget_retry)) }
                        }
                        else if (showHistory) {
                            val weekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                            val history = state.items.map { FocusSession.settle(it, state.now) }.filter { it.focusStoppedAt != null }.sortedByDescending { it.focusStoppedAt }
                            val week = history.filter { (it.focusStoppedAt ?: 0) >= weekStart && (it.focusStoppedAt ?: 0) <= state.now }
                            item {
                                Text(stringResource(R.string.focus_weekly), style = MaterialTheme.typography.titleLarge)
                                Text(stringResource(R.string.focus_weekly_summary, week.count { it.focusState == FocusSession.COMPLETED }, week.count { it.focusState == FocusSession.ABORTED }), style = MaterialTheme.typography.bodyLarge)
                                Text(formatRemainingTime(this@FocusActivity, week.sumOf { it.focusElapsedMillis } / 1000, null, showSeconds = true), style = MaterialTheme.typography.headlineMedium)
                                Text(stringResource(R.string.focus_history_basis), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (history.isEmpty()) item { Text(stringResource(R.string.focus_history_empty)) }
                            items(history, key = { it.stableId }) { entry ->
                                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                    Text(entry.title, style = MaterialTheme.typography.titleMedium)
                                    Text(Instant.ofEpochMilli(entry.focusStoppedAt!!).atZone(ZoneId.systemDefault()).toLocalDate().toString())
                                    Text(stringResource(if (entry.focusState == FocusSession.COMPLETED) R.string.focus_completed else R.string.focus_stopped))
                                    Text(formatRemainingTime(this@FocusActivity, entry.focusElapsedMillis / 1000, null, showSeconds = true))
                                }
                            }
                        } else if (display != null) {
                            item {
                                Text(display.dueText, style = MaterialTheme.typography.titleMedium)
                                if (display.isExpired) {
                                    Text(stringResource(R.string.focus_recorded_time), style = MaterialTheme.typography.bodyMedium)
                                    val elapsed = display.detailFacts?.elapsed ?: 0L
                                    RemainingTimeText(display.copy(remainingSeconds = elapsed,
                                        detailFacts = display.detailFacts?.copy(secondsLeft = elapsed)),
                                        hero = true, showSeconds = true, showRelation = false)
                                } else RemainingTimeText(display, hero = true, showSeconds = true)
                            }
                            item {
                                if (display.isExpired) Text(stringResource(R.string.focus_planned_duration,
                                    formatRemainingTime(this@FocusActivity, display.detailFacts?.total, null, showSeconds = true)))
                                else TimeRuler(display.percent, display.startLabel, display.endLabel, glowEnabled = display.detailFacts?.glowActive == true)
                            }
                            item { Text(stringResource(R.string.focus_elapsed_basis), style = MaterialTheme.typography.bodySmall) }
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                                    when (display.focusState) {
                                        FocusSession.RUNNING -> Button(onClick = { model.change(id, FocusSession.PAUSED) }, enabled = !state.busy, modifier = Modifier.fillMaxWidth().heightIn(min = LayoutTokens.MinTouchTarget)) { Text(stringResource(R.string.focus_pause)) }
                                        FocusSession.PAUSED -> Button(onClick = { model.change(id, FocusSession.RUNNING) }, enabled = !state.busy, modifier = Modifier.fillMaxWidth().heightIn(min = LayoutTokens.MinTouchTarget)) { Text(stringResource(R.string.focus_resume)) }
                                    }
                                    if (display.focusState in setOf(FocusSession.RUNNING, FocusSession.PAUSED)) {
                                        TextButton(onClick = { confirmStop = true }, enabled = !state.busy, modifier = Modifier.heightIn(min = LayoutTokens.MinTouchTarget)) { Text(stringResource(R.string.focus_stop)) }
                                        TimeLeftSwitchRow(
                                            title = stringResource(R.string.focus_keep_awake),
                                            checked = keepAwake,
                                            onCheckedChange = { keepAwake = it },
                                        )
                                    }
                                    TextButton(onClick = { showHistory = true }) { Text(stringResource(R.string.focus_history)) }
                                    state.error?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
                                }
                            }
                            if (display.focusState == FocusSession.RUNNING && !canPostReminderNotifications()) item {
                                Text(stringResource(R.string.focus_notifications_off), style = MaterialTheme.typography.bodyMedium)
                                TextButton(onClick = {
                                    if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED)
                                        permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    else startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName))
                                }) { Text(stringResource(R.string.notification_permission_allow)) }
                            }
                            if (display.focusState == FocusSession.RUNNING && !canUseExactAlarms(this@FocusActivity) && Build.VERSION.SDK_INT >= 31) item {
                                Text(stringResource(R.string.focus_exact_explanation), style = MaterialTheme.typography.bodyMedium)
                                TextButton(onClick = { startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:$packageName"))) }) {
                                    Text(stringResource(R.string.focus_exact_settings))
                                }
                            }
                        }
                    }
                }
                if (confirmStop) AlertDialog(onDismissRequest = { confirmStop = false },
                    title = { Text(stringResource(R.string.focus_stop_confirm)) },
                    text = { Text(stringResource(R.string.focus_stop_message)) },
                    confirmButton = { TextButton(onClick = { confirmStop = false; model.change(id, FocusSession.ABORTED) }) { Text(stringResource(R.string.focus_stop)) } },
                    dismissButton = { TextButton(onClick = { confirmStop = false }) { Text(stringResource(R.string.action_cancel)) } })
            }
        }
    }

    companion object {
        private const val ITEM_ID = "focus_item_id"
        fun intent(context: Context, id: Int): Intent = Intent(context, FocusActivity::class.java).putExtra(ITEM_ID, id)
            .setData(Uri.parse("timeleft://focus/$id"))
    }
}
