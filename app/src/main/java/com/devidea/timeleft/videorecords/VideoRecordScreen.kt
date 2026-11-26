package com.devidea.timeleft.videorecords

import android.graphics.Bitmap
import android.text.format.DateUtils
import android.util.LruCache
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.devidea.timeleft.R
import com.devidea.timeleft.ui.TimeDisplayItem
import com.devidea.timeleft.ui.components.TimeLeftInlineButton
import com.devidea.timeleft.ui.components.TimeLeftTopAppBar
import com.devidea.timeleft.ui.theme.LayoutTokens
import com.devidea.timeleft.ui.theme.Spacing
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit

@Composable
internal fun VideoRecordLink(item: TimeDisplayItem, enabled: Boolean = true, fullRow: Boolean = false) {
    if (!item.canRecordVideo) return
    val context = LocalContext.current
    val label = stringResource(R.string.video_record_title)
    val open = { VideoRecordActivity.open(context, item) }
    if (!fullRow) {
        TimeLeftInlineButton(onClick = open, enabled = enabled) { Text(label) }
        return
    }
    Row(
        Modifier.fillMaxWidth().heightIn(min = LayoutTokens.MinTouchTarget)
            .clickable(enabled = enabled, role = Role.Button, onClick = open)
            .padding(vertical = Spacing.m),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.m),
    ) {
        Icon(Icons.Outlined.VideoLibrary, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(
                stringResource(R.string.video_record_entry_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun VideoRecordScreen(
    title: String,
    target: LocalDate,
    clips: List<RecordedVideo>?,
    draft: RecordedVideo?,
    busy: Boolean,
    error: Int?,
    today: LocalDate = LocalDate.now(),
    onBack: () -> Unit = {},
    onRetry: () -> Unit = {},
    onClearError: () -> Unit = {},
    onRecord: () -> Unit = {},
    onPick: () -> Unit = {},
    onSave: () -> Unit = {},
    onDiscard: () -> Unit = {},
    onDelete: (RecordedVideo) -> Unit = {},
) {
    var chooseSource by rememberSaveable { mutableStateOf(false) }
    var playingFile by rememberSaveable { mutableStateOf<String?>(null) }
    var deletingFile by rememberSaveable { mutableStateOf<String?>(null) }
    val thumbnails = remember { LruCache<File, Bitmap>(24) }
    val zone = ZoneId.systemDefault()
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { TimeLeftTopAppBar(stringResource(R.string.video_record_title), onBack) },
    ) { insets ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(insets),
            contentPadding = PaddingValues(
                horizontal = LayoutTokens.ScreenHorizontal,
                vertical = LayoutTokens.ScreenVertical,
            ),
        ) {
            item(key = "schedule") {
                Column(Modifier.padding(bottom = LayoutTokens.SectionGap),
                    verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
                    Text(
                        stringResource(R.string.video_record_day_group, relativeDay(today, target), localizedDate(target)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (busy && draft == null) item(key = "loading") {
                LinearProgressIndicator(Modifier.fillMaxWidth().padding(bottom = Spacing.m))
            }
            if (error != null && draft == null) item(key = "error") {
                RecordError(error)
                if (clips == null) TimeLeftInlineButton(onClick = onRetry, enabled = !busy) {
                    Text(stringResource(R.string.widget_retry))
                }
            }
            if (clips != null) {
                if (clips.isEmpty()) {
                    item(key = "first-video") {
                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Text(
                                stringResource(R.string.video_record_empty),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.semantics { heading() },
                            )
                            Text(
                                stringResource(R.string.video_record_local),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            RecordSourceActions(
                                enabled = !busy,
                                onRecord = { onClearError(); onRecord() },
                                onPick = { onClearError(); onPick() },
                            )
                            Text(
                                stringResource(R.string.video_record_limits),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                } else {
                    item(key = "actions") {
                        RecordButton(R.string.video_record_add, Icons.Default.Add, enabled = !busy) {
                            onClearError()
                            chooseSource = true
                        }
                    }
                    if (clips.size > 1) item(key = "play-all") {
                        TimeLeftInlineButton(
                            onClick = { playingFile = clips.first().file.name },
                            modifier = Modifier.padding(top = Spacing.l),
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.padding(end = Spacing.xs))
                            Text(stringResource(R.string.video_record_play_all))
                        }
                    }
                    val byDay = clips.groupBy { it.recordedAt.atZone(zone).toLocalDate() }
                    byDay.forEach { (day, videos) ->
                        item(key = "day-$day") {
                            HorizontalDivider(
                                modifier = Modifier.padding(top = Spacing.m),
                                color = MaterialTheme.colorScheme.outlineVariant,
                            )
                            Text(
                                stringResource(R.string.video_record_day_group, relativeDay(day, target), localizedDate(day)),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = Spacing.m, bottom = Spacing.s).semantics { heading() },
                            )
                        }
                        items(videos, key = { it.file.name }) { clip ->
                            RecordVideoRow(
                                clip, thumbnails, !busy,
                                onPlay = { playingFile = clip.file.name },
                                onDelete = { deletingFile = clip.file.name },
                            )
                        }
                    }
                }
            }
        }
    }
    if (chooseSource) {
        RecordSheet(R.string.video_record_add, busy = false, onClose = { chooseSource = false }) {
            Text(stringResource(R.string.video_record_limits), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            RecordSourceActions(
                enabled = !busy,
                onRecord = { chooseSource = false; onRecord() },
                onPick = { chooseSource = false; onPick() },
            )
        }
    }
    if (draft != null) {
        RecordDraftSheet(draft, busy, error, onSave, onDiscard)
    }
    val pendingDelete = clips?.firstOrNull { it.file.name == deletingFile }
    if (pendingDelete != null) {
        AlertDialog(
            onDismissRequest = { deletingFile = null },
            title = { Text(stringResource(R.string.video_record_delete)) },
            text = { Text(stringResource(R.string.video_record_delete_message)) },
            confirmButton = {
                TextButton(onClick = { deletingFile = null; onDelete(pendingDelete) }, enabled = !busy) {
                    Text(stringResource(R.string.card_action_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingFile = null }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
    if (clips != null) {
        val clip = clips.firstOrNull { it.file.name == playingFile }
        if (clip != null) ClipPlayerDialog(clips, clip) { playingFile = null }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun RecordSourceActions(enabled: Boolean, onRecord: () -> Unit, onPick: () -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Spacing.l),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        RecordButton(R.string.video_record_capture, Icons.Outlined.Videocam, enabled, onRecord)
        TimeLeftInlineButton(onClick = onPick, enabled = enabled) {
            Text(stringResource(R.string.video_record_pick))
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun RecordDraftSheet(
    draft: RecordedVideo,
    busy: Boolean,
    error: Int?,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
) {
    RecordSheet(R.string.video_record_review, busy, onClose = { if (!busy) onDiscard() }) {
        ClipThumbnail(draft.file, Modifier.fillMaxWidth().aspectRatio(16f / 9f))
        Text(clipDuration(draft), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.l)) {
            RecordButton(R.string.action_save, enabled = !busy, onClick = onSave)
            TimeLeftInlineButton(onClick = onDiscard, enabled = !busy) { Text(stringResource(R.string.action_cancel)) }
        }
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (error != null) RecordError(error)
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun RecordSheet(
    title: Int,
    busy: Boolean,
    onClose: () -> Unit,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val maxHeight = LocalConfiguration.current.screenHeightDp.dp * LayoutTokens.DetailSheetMaxHeightFraction
    val applying by rememberUpdatedState(busy)
    val confirm = remember { { value: SheetValue -> value != SheetValue.Hidden || !applying } }
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true, confirmValueChange = confirm)
    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheet,
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier.heightIn(max = maxHeight),
    ) {
        CompositionLocalProvider(LocalDensity provides density) {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(horizontal = LayoutTokens.ScreenHorizontal).padding(bottom = Spacing.xxl),
                verticalArrangement = Arrangement.spacedBy(Spacing.m),
            ) {
                Text(stringResource(title), style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.semantics { heading() })
                content()
            }
        }
    }
}

@Composable
private fun RecordButton(label: Int, icon: ImageVector? = null, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick, enabled = enabled, shape = MaterialTheme.shapes.small,
        modifier = Modifier.heightIn(min = LayoutTokens.MinTouchTarget),
    ) {
        if (icon != null) Icon(icon, contentDescription = null, modifier = Modifier.padding(end = Spacing.xs))
        Text(stringResource(label))
    }
}

@Composable
private fun RecordError(message: Int) {
    Text(stringResource(message), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
}

@Composable
private fun RecordVideoRow(
    clip: RecordedVideo,
    thumbnails: LruCache<File, Bitmap>,
    enabled: Boolean,
    onPlay: () -> Unit,
    onDelete: () -> Unit,
) {
    var showOptions by remember { mutableStateOf(false) }
    val menuName = stringResource(R.string.video_record_options)
    val locale = LocalConfiguration.current.locales[0]
    val time = remember(clip.recordedAt, locale) {
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)
            .withZone(ZoneId.systemDefault()).format(clip.recordedAt)
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier.weight(1f).heightIn(min = LayoutTokens.MinTouchTarget)
                .clickable(enabled = enabled, role = Role.Button,
                    onClickLabel = stringResource(R.string.video_record_play), onClick = onPlay)
                .padding(vertical = Spacing.s),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            ClipThumbnail(clip.file, Modifier.width(88.dp).aspectRatio(16f / 10f),
                cache = thumbnails, showPlay = true)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(time, style = MaterialTheme.typography.bodyLarge)
                Text(clipDuration(clip), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Box {
            IconButton(onClick = { showOptions = true }, enabled = enabled) {
                Icon(Icons.Default.MoreVert, contentDescription = menuName, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            DropdownMenu(expanded = showOptions, onDismissRequest = { showOptions = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.video_record_delete), color = MaterialTheme.colorScheme.error) },
                    onClick = { showOptions = false; onDelete() },
                )
            }
        }
    }
}

@Composable
private fun localizedDate(date: LocalDate): String {
    val locale = LocalConfiguration.current.locales[0]
    return remember(date, locale) { DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale).format(date) }
}

@Composable
private fun relativeDay(date: LocalDate, target: LocalDate): String {
    val days = ChronoUnit.DAYS.between(date, target)
    return when {
        days > 0 -> stringResource(R.string.home_dday_before, days.toInt())
        days < 0 -> stringResource(R.string.home_dday_after, -days.toInt())
        else -> stringResource(R.string.home_dday_today)
    }
}

private fun clipDuration(clip: RecordedVideo): String =
    DateUtils.formatElapsedTime((clip.durationMillis + 999L) / 1_000L)
