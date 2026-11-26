package com.devidea.timeleft.sharedclips

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.devidea.timeleft.R
import com.devidea.timeleft.ui.components.TimeLeftInlineButton
import com.devidea.timeleft.ui.components.TimeLeftUnderlineTextField
import com.devidea.timeleft.ui.theme.LayoutTokens
import com.devidea.timeleft.ui.theme.Spacing

private val invitationPattern = Regex("\\b[0-9a-fA-F]{8}-(?:[0-9a-fA-F]{4}-){3}[0-9a-fA-F]{12}\\b")

@Composable
internal fun JoinAlbumSheet(busy: Boolean, error: Int?, onJoin: (String) -> Unit, onClose: () -> Unit) {
    var input by rememberSaveable { mutableStateOf("") }
    val code = invitationPattern.find(input)?.value
    AlbumActionSheet(R.string.shared_clips_join_action, busy, error, onClose) {
        AlbumSheetDescription(R.string.shared_clips_join_help)
        TimeLeftUnderlineTextField(
            value = input, onValueChange = { input = it.takeLast(600) },
            label = { Text(stringResource(R.string.shared_clips_code)) },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, autoCorrect = false),
            modifier = Modifier.fillMaxWidth(),
        )
        AlbumPrimaryButton(R.string.shared_clips_join, enabled = !busy && code != null) {
            if (code != null) onJoin(code)
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun InviteAlbumSheet(
    invitation: String?,
    busy: Boolean,
    error: Int?,
    onIssue: () -> Unit,
    onShare: (String) -> Unit,
    onClose: () -> Unit,
) {
    AlbumActionSheet(R.string.shared_clips_invite, busy, error, onClose) {
        AlbumSheetDescription(R.string.shared_clips_invite_help)
        if (invitation == null) {
            AlbumPrimaryButton(R.string.shared_clips_issue_code, enabled = !busy, onClick = onIssue)
        } else {
            SelectionContainer { Text(invitation, style = MaterialTheme.typography.titleMedium) }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.l)) {
                AlbumPrimaryButton(R.string.shared_clips_share_code, enabled = !busy) { onShare(invitation) }
                TimeLeftInlineButton(onClick = onIssue, enabled = !busy) {
                    Text(stringResource(R.string.shared_clips_replace_code))
                }
            }
            AlbumSheetDescription(R.string.shared_clips_replace_help)
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun UploadClipSheet(uri: Uri, busy: Boolean, error: Int?, onUpload: () -> Unit, onClose: () -> Unit) {
    AlbumActionSheet(R.string.shared_clips_confirm_upload, busy, error, onClose) {
        ClipThumbnail(uri, modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f))
        AlbumSheetDescription(R.string.shared_clips_upload_description)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.l)) {
            AlbumPrimaryButton(R.string.shared_clips_upload, enabled = !busy, onClick = onUpload)
            TimeLeftInlineButton(onClick = onClose, enabled = !busy) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    }
}

@Composable
private fun AlbumSheetDescription(message: Int) {
    Text(stringResource(message), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun AlbumActionSheet(
    title: Int,
    busy: Boolean,
    error: Int?,
    onClose: () -> Unit,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val maxHeight = LocalConfiguration.current.screenHeightDp.dp * LayoutTokens.DetailSheetMaxHeightFraction
    val applying by rememberUpdatedState(busy)
    val confirmSheetChange = remember { { target: SheetValue -> target != SheetValue.Hidden || !applying } }
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true, confirmValueChange = confirmSheetChange)
    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = state,
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier.heightIn(max = maxHeight),
    ) {
        CompositionLocalProvider(LocalDensity provides density) {
            Column(
                modifier = Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState())
                    .padding(horizontal = LayoutTokens.ScreenHorizontal).padding(bottom = Spacing.xxl),
                verticalArrangement = Arrangement.spacedBy(Spacing.m),
            ) {
                Text(stringResource(title), style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.semantics { heading() })
                content()
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                if (error != null) AlbumError(error)
            }
        }
    }
}
