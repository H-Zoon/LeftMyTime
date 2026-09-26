package com.devidea.timeleft.backup

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.devidea.timeleft.R
import com.devidea.timeleft.ui.components.TimeLeftTopAppBar
import com.devidea.timeleft.ui.settings.SettingsDropdownRow
import com.devidea.timeleft.ui.settings.SettingsOption
import com.devidea.timeleft.ui.theme.LayoutTokens
import com.devidea.timeleft.ui.theme.Spacing
import java.time.LocalDate

@Composable
fun BackupScreen(state: BackupState, onBack: () -> Unit, onExport: (android.net.Uri) -> Unit,
    onInspect: (android.net.Uri) -> Unit, onRestore: (RestoreMode, String?) -> Unit, onCancelPreview: () -> Unit) {
    val context = LocalContext.current
    val create = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { it?.let(onExport) }
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let(onInspect) }
    var useBackup by rememberSaveable(state.preview) { mutableStateOf(false) }
    val mode = if (useBackup) RestoreMode.UseBackup else RestoreMode.KeepExisting
    var selectedPin by rememberSaveable(state.preview, mode) { mutableStateOf(state.preview?.preferredPin(mode)) }
    fun back() { if (!state.busy) { if (state.preview != null) onCancelPreview() else onBack() } }
    BackHandler { back() }
    Scaffold(containerColor = MaterialTheme.colorScheme.background,
        topBar = { TimeLeftTopAppBar(stringResource(R.string.backup_title), ::back) },
        bottomBar = {
            if (state.preview != null) Surface(color = MaterialTheme.colorScheme.background) {
                Button(onClick = { onRestore(mode, selectedPin) },
                    enabled = !state.busy,
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(Spacing.l).heightIn(min = LayoutTokens.MinTouchTarget)) {
                    Text(stringResource(if (state.busy) R.string.action_saving else R.string.backup_restore_confirm))
                }
            }
        }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = LayoutTokens.ScreenHorizontal, vertical = Spacing.l),
            verticalArrangement = Arrangement.spacedBy(Spacing.l)) {
            item {
                Text(stringResource(R.string.backup_scope), style = MaterialTheme.typography.bodyMedium)
            }
            state.message?.let { message -> item {
                Text(stringResource(message), style = MaterialTheme.typography.bodyLarge)
                state.restored?.let { Text(stringResource(R.string.backup_restored_count, it), style = MaterialTheme.typography.bodyMedium) }
            } }
            if (state.busy) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            val preview = state.preview
            if (preview == null) {
                item { Button(onClick = { create.launch("LeftMyTime-${LocalDate.now()}.json") }, enabled = !state.busy,
                    modifier = Modifier.fillMaxWidth().heightIn(min = LayoutTokens.MinTouchTarget)) { Text(stringResource(R.string.backup_export)) } }
                item { OutlinedButton(onClick = { open.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }, enabled = !state.busy,
                    modifier = Modifier.fillMaxWidth().heightIn(min = LayoutTokens.MinTouchTarget)) { Text(stringResource(R.string.backup_import)) } }
            } else {
                item {
                    Text(stringResource(R.string.backup_review), style = MaterialTheme.typography.titleLarge)
                    Text(stringResource(R.string.backup_counts, preview.additions, preview.conflicts, preview.unchanged),
                        style = MaterialTheme.typography.bodyMedium)
                }
                if (preview.conflicts > 0) item {
                    SettingsDropdownRow(title = stringResource(R.string.backup_conflict_title), selectedValue = useBackup,
                        options = listOf(SettingsOption(false, R.string.backup_keep_existing), SettingsOption(true, R.string.backup_use_file)),
                        onSelected = { if (!state.busy) useBackup = it })
                    Text(stringResource(R.string.backup_conflict_explanation), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                val pins = preview.pinCandidates(mode)
                if (pins.size > 1) item {
                    SettingsDropdownRow(
                        title = stringResource(R.string.backup_pin_title), selectedValue = selectedPin,
                        options = listOf(SettingsOption<String?>(null, R.string.backup_pin_automatic)) + pins.map { candidate ->
                            val current = preview.existing.any { it.stableId == candidate.stableId && it.isPinned }
                            SettingsOption<String?>(candidate.stableId, label = stringResource(
                                if (current) R.string.backup_pin_current else R.string.backup_pin_file, candidate.title))
                        },
                        onSelected = { selectedPin = it }, enabled = !state.busy,
                        summary = stringResource(R.string.backup_pin_explanation),
                    )
                }
                items(preview.imported, key = { it.stableId }) { candidate ->
                    val existing = preview.existing.firstOrNull { it.stableId == candidate.stableId }
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Text(candidate.title, style = MaterialTheme.typography.titleMedium)
                        if (candidate.isTemplate || candidate.focusDurationMillis != null)
                            Text(stringResource(if (candidate.isTemplate) R.string.backup_kind_template else R.string.backup_kind_focus),
                                style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(com.devidea.timeleft.widget.widgetScheduleDescription(context, candidate), style = MaterialTheme.typography.bodyMedium)
                        Text(stringResource(when {
                            existing == null -> R.string.backup_addition
                            sameSchedule(existing, candidate) -> R.string.backup_unchanged
                            else -> R.string.backup_conflict
                        }), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        if (existing != null && !sameSchedule(existing, candidate)) {
                            Text(if (existing.deletedAt != null) stringResource(R.string.backup_existing_deleted)
                                else stringResource(R.string.backup_existing_value, existing.title, existing.startValue, existing.endValue),
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
