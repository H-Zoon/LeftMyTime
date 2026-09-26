package com.devidea.timeleft.calendar

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.preference.PreferenceManager
import com.devidea.timeleft.R
import com.devidea.timeleft.preferences.UserPreferences
import com.devidea.timeleft.ui.components.TimeLeftTopAppBar
import com.devidea.timeleft.ui.theme.*
import com.devidea.timeleft.ui.settings.SettingsDropdownRow
import com.devidea.timeleft.ui.settings.SettingsOption
import com.devidea.timeleft.widget.widgetScheduleDescription
import java.time.ZoneId

@dagger.hilt.android.AndroidEntryPoint
class CalendarImportActivity : AppCompatActivity() {
    private val model: CalendarImportViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        setContent {
            val state by model.state.collectAsStateWithLifecycle()
            var permissionAsked by rememberSaveable { mutableStateOf(false) }
            val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { model.checkPermission() }
            val owner = LocalLifecycleOwner.current
            DisposableEffect(owner) {
                val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) model.checkPermission() }
                owner.lifecycle.addObserver(observer)
                model.checkPermission()
                onDispose { owner.lifecycle.removeObserver(observer) }
            }
            BackHandler(state.saving) {}
            TimeLeftTheme(designKey = prefs.getString(UserPreferences.KEY_DESIGN, UserPreferences.DESIGN_TIME_FOCUS) ?: UserPreferences.DESIGN_TIME_FOCUS,
                themeMode = prefs.getString(UserPreferences.KEY_THEME, UserPreferences.THEME_AUTO) ?: UserPreferences.THEME_AUTO,
                paletteKey = prefs.getString(UserPreferences.KEY_COLOR_THEME, UserPreferences.COLOR_THEME_CLAY) ?: UserPreferences.COLOR_THEME_CLAY) {
                CalendarImportScreen(state, onBack = { if (!state.saving) finish() },
                    onPermission = { permissionAsked = true; permission.launch(Manifest.permission.READ_CALENDAR) },
                    onSettings = if (permissionAsked) ({ startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))) }) else null,
                    onCalendar = { model.refresh(calendarId = it) }, onDays = { model.refresh(days = it) },
                    onRefresh = { model.refresh() }, onToggle = model::toggle, onImport = model::save,
                    describe = { widgetScheduleDescription(this, it.toItem(ZoneId.systemDefault())) })
            }
        }
    }
}

@Composable
internal fun CalendarImportScreen(state: CalendarImportState, onBack: () -> Unit, onPermission: () -> Unit,
    onSettings: (() -> Unit)?, onCalendar: (Long) -> Unit, onDays: (Int) -> Unit, onRefresh: () -> Unit,
    onToggle: (String) -> Unit, onImport: () -> Unit, describe: (CalendarOccurrence) -> String) {
    var showPolicy by rememberSaveable { mutableStateOf(false) }
    Scaffold(containerColor = MaterialTheme.colorScheme.background,
        topBar = { TimeLeftTopAppBar(stringResource(R.string.calendar_import_title), onBack) },
        bottomBar = {
            if (state.permission && state.preview != null) Column(Modifier.fillMaxWidth().navigationBarsPadding()
                .padding(horizontal = LayoutTokens.ScreenHorizontal, vertical = Spacing.s)) {
                if (state.imported != null) {
                    Text(stringResource(R.string.calendar_import_done, state.imported), style = MaterialTheme.typography.bodyMedium)
                    Button(onClick = onBack, modifier = Modifier.fillMaxWidth().heightIn(min = LayoutTokens.MinTouchTarget)) { Text(stringResource(R.string.calendar_done)) }
                } else {
                    Text(stringResource(R.string.calendar_selection_limit, state.selected.size, CalendarImportRepository.MAX_SELECTION), style = MaterialTheme.typography.bodySmall)
                    Button(onClick = onImport, enabled = !state.loading && !state.saving && state.selected.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth().heightIn(min = LayoutTokens.MinTouchTarget)) {
                        Text(stringResource(if (state.saving) R.string.calendar_import_saving else R.string.calendar_import_selected, state.selected.size))
                    }
                }
            }
        }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = LayoutTokens.ScreenHorizontal, vertical = Spacing.l),
            verticalArrangement = Arrangement.spacedBy(Spacing.l)) {
            item {
                Text(stringResource(R.string.calendar_import_explanation), style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = { showPolicy = !showPolicy }, modifier = Modifier.heightIn(min = LayoutTokens.MinTouchTarget)) {
                    Text(stringResource(if (showPolicy) R.string.calendar_policy_hide else R.string.calendar_policy_show))
                }
                if (showPolicy) Text(stringResource(R.string.calendar_import_policy), style = MaterialTheme.typography.bodyMedium)
            }
            if (!state.permission) {
                item {
                    Text(stringResource(R.string.calendar_permission_explanation))
                    Button(onClick = onPermission, modifier = Modifier.heightIn(min = LayoutTokens.MinTouchTarget)) { Text(stringResource(R.string.calendar_allow_read)) }
                    if (onSettings != null) TextButton(onClick = onSettings) { Text(stringResource(R.string.calendar_open_settings)) }
                }
            } else {
                if (state.calendars.isNotEmpty()) item {
                    CalendarChoice(stringResource(R.string.calendar_choose), state.calendarId ?: state.calendars.first().id,
                        state.calendars.map { it.id to it.name }, !state.saving && !state.loading && state.imported == null, onCalendar)
                    CalendarChoice(stringResource(R.string.calendar_range), state.days,
                        listOf(7, 30, 90).map { it to stringResource(R.string.calendar_next_days, it) },
                        !state.saving && !state.loading && state.imported == null, onDays)
                }
                if (state.loading || state.saving) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
                if (state.error != null) item {
                    Text(stringResource(state.error), color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = onRefresh, enabled = !state.saving) { Text(stringResource(R.string.theme_preview_retry)) }
                }
                if (!state.loading && state.error == null && state.calendars.isEmpty()) item {
                    Text(stringResource(R.string.calendar_no_calendars))
                    TextButton(onClick = onRefresh) { Text(stringResource(R.string.theme_preview_retry)) }
                }
                if (!state.loading && state.preview?.entries?.isEmpty() == true) item { Text(stringResource(R.string.calendar_no_events)) }
                if (state.preview?.truncated == true) item { Text(stringResource(R.string.calendar_truncated, CalendarImportRepository.MAX_VISIBLE)) }
                items(state.preview?.entries.orEmpty(), key = { it.sourceKey }) { occurrence ->
                    val existing = occurrence.sourceKey in state.preview!!.alreadyImported
                    val checked = occurrence.sourceKey in state.selected
                    val enabled = !existing && !state.saving && !state.loading && state.imported == null &&
                        (checked || state.selected.size < CalendarImportRepository.MAX_SELECTION)
                    Column {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Row(Modifier.fillMaxWidth().heightIn(min = LayoutTokens.MinTouchTarget)
                            .toggleable(checked || existing, enabled = enabled, role = Role.Checkbox, onValueChange = { onToggle(occurrence.sourceKey) })
                            .padding(vertical = Spacing.m), verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                            Checkbox(checked = checked || existing, onCheckedChange = null, enabled = enabled)
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                                Text(occurrence.title, style = MaterialTheme.typography.titleMedium)
                                Text(describe(occurrence), style = MaterialTheme.typography.bodyMedium)
                                if (existing) Text(stringResource(R.string.calendar_already_imported), style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun <T> CalendarChoice(label: String, selected: T, options: List<Pair<T, String>>, enabled: Boolean, onSelect: (T) -> Unit) {
    SettingsDropdownRow(title = label, selectedValue = selected,
        options = options.map { SettingsOption(it.first, label = it.second) }, onSelected = onSelect, enabled = enabled)
}
