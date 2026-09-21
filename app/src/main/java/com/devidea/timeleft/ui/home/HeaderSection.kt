package com.devidea.timeleft.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.devidea.timeleft.AdapterItem
import com.devidea.timeleft.R
import com.devidea.timeleft.ui.components.RemainingTimeText
import com.devidea.timeleft.ui.components.TimeDetailContent
import com.devidea.timeleft.ui.components.remainingTimeLabel
import com.devidea.timeleft.ui.theme.LayoutTokens
import com.devidea.timeleft.ui.theme.Spacing
import kotlinx.coroutines.launch
import com.devidea.timeleft.widget.PinWidgetButton
import com.devidea.timeleft.widget.WidgetSource

/** All calendar periods remain visible; only an explicit action opens a larger view. */
@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
internal fun HeaderSection(
    topItems: List<AdapterItem>,
    progressDisplayMode: String,
) {
    if (topItems.isEmpty()) return
    val labels = listOf(R.string.period_today, R.string.period_month, R.string.period_year)
    var detailIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    val detailAction = stringResource(R.string.period_show_details)
    val periodFocus = remember(topItems.size) { List(topItems.size) { FocusRequester() } }
    var returnFocus by remember { mutableStateOf<FocusRequester?>(null) }
    LaunchedEffect(detailIndex) {
        if (detailIndex == null) {
            // Restore keyboard focus after the modal has left the composition.
            withFrameNanos { }
            returnFocus?.requestFocus()
        }
    }
    fun openDetail(position: Int, focus: FocusRequester) {
        returnFocus = focus
        detailIndex = position
    }

    Column(Modifier.fillMaxWidth()) {
        FlowRow(Modifier.fillMaxWidth().padding(vertical = Spacing.m),
            horizontalArrangement = Arrangement.SpaceBetween, verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(stringResource(R.string.period_summary_title), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(stringResource(R.string.period_tap_hint), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        BoxWithConstraints(Modifier.fillMaxWidth().padding(bottom = Spacing.l)) {
            val stacked = maxWidth < 300.dp || LocalDensity.current.fontScale > 1.2f
            @Composable fun Period(position: Int, modifier: Modifier) {
                val item = topItems[position]
                val label = if (position < labels.size) stringResource(labels[position]) else item.title
                val remaining = remainingTimeLabel(item.detailFacts?.secondsLeft ?: item.remainingSeconds,
                    item.remainingDays, item.leftString, showSeconds = position == 0)
                val description = stringResource(R.string.period_summary_description, label, remaining)
                val cellModifier = modifier.heightIn(min = LayoutTokens.MinTouchTarget)
                    .focusRequester(periodFocus[position])
                    .clickable(role = Role.Button, onClickLabel = detailAction) { openDetail(position, periodFocus[position]) }
                    .clearAndSetSemantics { contentDescription = description }
                    .padding(vertical = Spacing.xs)
                if (stacked) {
                    Row(cellModifier, horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
                        Text(label, style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f).alignByBaseline())
                        RemainingTimeText(item, hero = false, compact = true, showSeconds = position == 0,
                            modifier = Modifier.weight(2f).alignByBaseline())
                    }
                } else {
                    Column(cellModifier, verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        RemainingTimeText(item, hero = false, compact = true, showSeconds = position == 0)
                    }
                }
            }
            if (stacked) {
                Column { topItems.indices.forEach { Period(it, Modifier.fillMaxWidth()) } }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
                    topItems.indices.forEach { Period(it, Modifier.weight(if (it == 0) 1.5f else 1f)) }
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(Spacing.m))
    }

    detailIndex?.let { position ->
        val contentDensity = LocalDensity.current
        val item = topItems[position.coerceIn(topItems.indices)]
        val title = if (position in labels.indices) stringResource(labels[position]) else item.title
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val scope = rememberCoroutineScope()
        ModalBottomSheet(
            onDismissRequest = { detailIndex = null },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.background,
        ) {
            // Dialog windows may replace LocalDensity; preserve the caller's text scale.
            CompositionLocalProvider(LocalDensity provides contentDensity) {
                Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(horizontal = LayoutTokens.ScreenHorizontal).padding(bottom = Spacing.xxl),
                    verticalArrangement = Arrangement.spacedBy(Spacing.l)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                        IconButton(onClick = { scope.launch { sheetState.hide(); detailIndex = null } }) {
                            Icon(Icons.Default.Close, stringResource(R.string.period_close_details))
                        }
                    }
                    TimeDetailContent(item, progressDisplayMode)
                    listOf(WidgetSource.Today, WidgetSource.Month, WidgetSource.Year).getOrNull(position)?.let { source ->
                        PinWidgetButton(item, source, contentPadding = PaddingValues(vertical = Spacing.s))
                    }
                }
            }
        }
    }
}
