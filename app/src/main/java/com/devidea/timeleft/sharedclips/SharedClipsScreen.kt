package com.devidea.timeleft.sharedclips

import android.graphics.Bitmap
import android.net.Uri
import android.util.LruCache
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.devidea.timeleft.R
import com.devidea.timeleft.ui.components.TimeLeftInlineButton
import com.devidea.timeleft.ui.components.TimeLeftTopAppBar
import com.devidea.timeleft.ui.theme.LayoutTokens
import com.devidea.timeleft.ui.theme.Spacing
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit

private enum class AlbumSheet { Join, Invite }

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SharedClipsScreen(
    page: SharedClipsPage?,
    busy: Boolean,
    error: Int?,
    invitation: String? = null,
    selection: Uri? = null,
    today: LocalDate = LocalDate.now(),
    onBack: () -> Unit,
    onRefresh: () -> Unit = {},
    onOpen: (SharedAlbum) -> Unit = {},
    onCreate: (AlbumEvent) -> Unit = {},
    onJoin: (String) -> Unit = {},
    onIssueInvitation: () -> Unit = {},
    onShareInvitation: (String) -> Unit = {},
    onPick: () -> Unit = {},
    onUpload: () -> Unit = {},
    onCancelSelection: () -> Unit = {},
    onDelete: (SharedClip) -> Unit = {},
    onDismissError: () -> Unit = {},
) {
    val playback = (page as? SharedClipsPage.Album)?.playback
    var sheet by rememberSaveable { mutableStateOf<AlbumSheet?>(null) }
    var playingPath by rememberSaveable(playback?.album?.id) { mutableStateOf<String?>(null) }
    var deletingPath by rememberSaveable(playback?.album?.id) { mutableStateOf<String?>(null) }
    val clipToDelete = playback?.clips?.firstOrNull { it.path == deletingPath }
    val listState = rememberLazyListState()
    val thumbnails = remember(playback?.album?.id) { LruCache<Uri, Bitmap>(24) }
    val destination = playback?.album?.id ?: page?.javaClass?.simpleName
    LaunchedEffect(destination) {
        listState.scrollToItem(0)
        if (playback != null && sheet == AlbumSheet.Join) sheet = null
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { TimeLeftTopAppBar(stringResource(R.string.shared_clips_title), onBack) },
    ) { insets ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(insets),
            state = listState,
            contentPadding = PaddingValues(
                horizontal = LayoutTokens.ScreenHorizontal,
                vertical = LayoutTokens.ScreenVertical,
            ),
        ) {
            if (busy && sheet == null && selection == null) {
                item(key = "progress") {
                    LinearProgressIndicator(Modifier.fillMaxWidth().padding(bottom = Spacing.m))
                }
            }
            if (error != null && sheet == null && selection == null) {
                item(key = "error") {
                    Column(Modifier.padding(bottom = Spacing.l)) {
                        AlbumError(error)
                        if (page == null) TimeLeftInlineButton(onClick = onRefresh) {
                            Text(stringResource(R.string.widget_retry))
                        }
                    }
                }
            }
            when (page) {
                null -> Unit
                is SharedClipsPage.Directory -> {
                    if (page.albums.isEmpty() && error == null) {
                        item(key = "empty-directory") {
                            Column(verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
                                Text(stringResource(R.string.shared_clips_no_albums), style = MaterialTheme.typography.titleLarge)
                                Text(stringResource(R.string.shared_clips_directory_help), style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                AlbumPrimaryButton(R.string.shared_clips_join_action, enabled = !busy) {
                                    onDismissError()
                                    sheet = AlbumSheet.Join
                                }
                            }
                        }
                    } else {
                        item(key = "directory-actions") {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween) {
                                TimeLeftInlineButton(
                                    onClick = {
                                        onDismissError()
                                        sheet = AlbumSheet.Join
                                    },
                                    enabled = !busy,
                                ) { Text(stringResource(R.string.shared_clips_join_action)) }
                                AlbumRefreshButton(!busy, onRefresh)
                            }
                        }
                        items(page.albums, key = { it.id }) { album ->
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            SharedAlbumNavigationRow(
                                title = album.title,
                                summary = albumDate(album.day),
                                enabled = !busy,
                                onClick = { onOpen(album) },
                            )
                        }
                    }
                }
                is SharedClipsPage.Create -> item(key = "create") {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.l)) {
                        AlbumHeading(page.event.title, page.event.date, today)
                        Text(stringResource(R.string.shared_clips_create_description),
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        AlbumPrimaryButton(R.string.shared_clips_create, Icons.Default.Add, !busy) { onCreate(page.event) }
                    }
                }
                is SharedClipsPage.Album -> {
                    val album = page.playback.album
                    val clips = page.playback.clips
                    item(key = "heading") {
                        Column(Modifier.padding(bottom = LayoutTokens.SectionGap),
                            verticalArrangement = Arrangement.spacedBy(Spacing.l)) {
                            AlbumHeading(album.title, album.day, today)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.l),
                                verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                                AlbumPrimaryButton(R.string.shared_clips_pick, Icons.Default.Add, !busy, onPick)
                                if (album.isOwner) TimeLeftInlineButton(
                                    onClick = {
                                        onDismissError()
                                        sheet = AlbumSheet.Invite
                                    },
                                    enabled = !busy,
                                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                                ) { Text(stringResource(R.string.shared_clips_invite)) }
                            }
                        }
                    }
                    item(key = "timeline-heading") {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.shared_clips_timeline), style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f).semantics { heading() })
                            AlbumRefreshButton(!busy, onRefresh)
                        }
                    }
                    if (clips.isEmpty()) {
                        item(key = "empty-timeline") {
                            Column(
                                Modifier.fillMaxWidth().padding(vertical = Spacing.page),
                                verticalArrangement = Arrangement.spacedBy(Spacing.s),
                            ) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                Text(stringResource(R.string.shared_clips_empty), style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        item(key = "play-all") {
                            TimeLeftInlineButton(onClick = { playingPath = clips.first().path }) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.padding(end = Spacing.xs))
                                Text(stringResource(R.string.shared_clips_play_all))
                            }
                        }
                        val byDay = clips.groupBy { it.uploadedAt.atZone(ZoneId.systemDefault()).toLocalDate() }
                        for ((day, entries) in byDay) {
                            item(key = "day-$day") {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                Text(
                                    stringResource(R.string.shared_clips_day_group, ddayLabel(day, album.day), albumDate(day)),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = Spacing.m, bottom = Spacing.s).semantics { heading() },
                                )
                            }
                            items(entries, key = { it.path }) { clip ->
                                AlbumClipRow(
                                    playback = page.playback, clip = clip, thumbnails = thumbnails, enabled = !busy,
                                    onPlay = { playingPath = clip.path },
                                    onDelete = { deletingPath = clip.path },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    val activeSheet = sheet
    if (activeSheet == AlbumSheet.Join && playback == null) {
        JoinAlbumSheet(busy, error, onJoin) {
            if (!busy) {
                sheet = null
                onDismissError()
            }
        }
    }
    if (activeSheet == AlbumSheet.Invite && playback != null) {
        InviteAlbumSheet(invitation, busy, error, onIssueInvitation, onShareInvitation) {
            if (!busy) {
                sheet = null
                onDismissError()
            }
        }
    }
    if (selection != null && playback != null) {
        UploadClipSheet(selection, busy, error, onUpload) { if (!busy) onCancelSelection() }
    }
    if (clipToDelete != null) {
        AlertDialog(
            onDismissRequest = { deletingPath = null },
            title = { Text(stringResource(R.string.shared_clips_delete)) },
            text = { Text(stringResource(R.string.shared_clips_delete_description)) },
            confirmButton = {
                TextButton(onClick = { deletingPath = null; onDelete(clipToDelete) }, enabled = !busy) {
                    Text(stringResource(R.string.shared_clips_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingPath = null }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
    val activeClip = playback?.clips?.firstOrNull { it.path == playingPath }
    if (playback != null && activeClip != null) ClipPlayerDialog(playback, activeClip) { playingPath = null }
}

@Composable
internal fun SharedAlbumNavigationRow(
    title: String,
    summary: String? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = LayoutTokens.MinTouchTarget)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(vertical = Spacing.m),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.m),
    ) {
        Icon(Icons.Outlined.VideoLibrary, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (summary != null) {
                Text(summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AlbumHeading(title: String, date: LocalDate, today: LocalDate) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        Text(ddayLabel(today, date), style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.primary)
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
        Text(albumDate(date), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun AlbumPrimaryButton(
    label: Int,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick, enabled = enabled, shape = MaterialTheme.shapes.small,
        modifier = Modifier.heightIn(min = LayoutTokens.MinTouchTarget),
    ) {
        if (icon != null) Icon(icon, contentDescription = null, modifier = Modifier.padding(end = Spacing.xs))
        Text(stringResource(label))
    }
}

@Composable
internal fun AlbumError(message: Int) {
    Text(stringResource(message), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
}

@Composable
private fun AlbumRefreshButton(enabled: Boolean, onClick: () -> Unit) {
    IconButton(enabled = enabled, onClick = onClick) {
        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.shared_clips_refresh),
            tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AlbumClipRow(
    playback: AlbumPlayback,
    clip: SharedClip,
    thumbnails: LruCache<Uri, Bitmap>,
    enabled: Boolean,
    onPlay: () -> Unit,
    onDelete: () -> Unit,
) {
    var showOptions by remember { mutableStateOf(false) }
    val author = stringResource(if (clip.isMine) R.string.shared_clips_my_video else R.string.shared_clips_friend_video)
    val locale = LocalConfiguration.current.locales[0]
    val time = remember(clip.uploadedAt, locale) {
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)
            .withZone(ZoneId.systemDefault()).format(clip.uploadedAt)
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier.weight(1f).heightIn(min = LayoutTokens.MinTouchTarget)
                .clickable(enabled = enabled, role = Role.Button,
                    onClickLabel = stringResource(R.string.shared_clips_play_video), onClick = onPlay)
                .padding(vertical = Spacing.s),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            ClipThumbnail(
                uri = Uri.parse(playback.streamBase + clip.path), headers = playback.headers,
                modifier = Modifier.width(88.dp).aspectRatio(16f / 10f),
                cache = thumbnails, showPlay = true,
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(author, style = MaterialTheme.typography.bodyLarge)
                Text(time, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (clip.isMine) Box {
            IconButton(onClick = { showOptions = true }, enabled = enabled) {
                Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.shared_clips_clip_options),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            DropdownMenu(expanded = showOptions, onDismissRequest = { showOptions = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.shared_clips_delete), color = MaterialTheme.colorScheme.error) },
                    onClick = { showOptions = false; onDelete() },
                )
            }
        }
    }
}

@Composable
private fun albumDate(date: LocalDate): String {
    val locale = LocalConfiguration.current.locales[0]
    return remember(date, locale) { DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale).format(date) }
}

@Composable
private fun ddayLabel(date: LocalDate, target: LocalDate): String {
    val days = ChronoUnit.DAYS.between(date, target)
    return when {
        days > 0 -> stringResource(R.string.home_dday_before, days.toInt())
        days < 0 -> stringResource(R.string.home_dday_after, -days.toInt())
        else -> stringResource(R.string.home_dday_today)
    }
}
