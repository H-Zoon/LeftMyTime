package com.devidea.timeleft.focus

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import com.devidea.timeleft.R
import com.devidea.timeleft.formatRemainingTime
import com.devidea.timeleft.ui.components.TimeLeftInlineButton
import com.devidea.timeleft.ui.components.TimeLeftTopAppBar
import com.devidea.timeleft.ui.theme.LayoutTokens
import com.devidea.timeleft.ui.theme.Spacing
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/** Shared by the real preparation route and the debug gallery. No session is created here. */
@Composable
fun FocusSetupScreen(
    minutes: Int,
    onMinutesChange: (Int) -> Unit,
    busy: Boolean,
    loading: Boolean,
    failed: Boolean,
    error: Int?,
    onRetry: () -> Unit,
    onStart: () -> Unit,
    onBack: () -> Unit,
    onHistory: () -> Unit,
) {
    var hoursScrolling by remember { mutableStateOf(false) }
    var minutesScrolling by remember { mutableStateOf(false) }
    val enabled = !busy && !loading && !failed
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { TimeLeftTopAppBar(stringResource(R.string.focus_default_title), onBack) },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                .padding(horizontal = LayoutTokens.ScreenHorizontal, vertical = LayoutTokens.ScreenVertical),
            verticalArrangement = Arrangement.spacedBy(Spacing.xxl),
        ) {
            Text(stringResource(R.string.focus_setup_title), style = MaterialTheme.typography.titleLarge)
            Text(
                formatRemainingTime(LocalContext.current, minutes * 60L, null, showSeconds = false),
                style = MaterialTheme.typography.displaySmall,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.xl)) {
                FocusDurationWheel(
                    value = minutes / 60,
                    range = 0..24,
                    label = stringResource(R.string.focus_duration_hours_label),
                    enabled = enabled,
                    onValueChange = { hours ->
                        onMinutesChange(if (hours == 24) 1440 else (hours * 60 + minutes % 60).coerceAtLeast(1))
                    },
                    onScrollingChange = { hoursScrolling = it },
                    modifier = Modifier.weight(1f),
                )
                val minuteRange = when {
                    minutes == 1440 -> 0..0
                    minutes < 60 -> 1..59
                    else -> 0..59
                }
                key(minuteRange) {
                    FocusDurationWheel(
                        value = minutes % 60,
                        range = minuteRange,
                        label = stringResource(R.string.focus_duration_minutes_label),
                        enabled = enabled && minutes != 1440,
                        onValueChange = { value -> onMinutesChange((minutes / 60 * 60 + value).coerceIn(1, 1440)) },
                        onScrollingChange = { minutesScrolling = it },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Text(stringResource(R.string.focus_setup_hint), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (loading || failed) {
                Text(stringResource(if (failed) R.string.home_load_failed else R.string.home_loading))
                if (failed) TimeLeftInlineButton(onClick = onRetry) { Text(stringResource(R.string.widget_retry)) }
            }
            error?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
            Button(
                onClick = { if (enabled && !hoursScrolling && !minutesScrolling) onStart() },
                enabled = enabled && !hoursScrolling && !minutesScrolling,
                modifier = Modifier.fillMaxWidth().heightIn(min = LayoutTokens.MinTouchTarget),
            ) { Text(stringResource(R.string.focus_start)) }
            TimeLeftInlineButton(onClick = onHistory, enabled = !busy) {
                Text(stringResource(R.string.focus_history))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FocusDurationWheel(
    value: Int,
    range: IntRange,
    label: String,
    enabled: Boolean,
    onValueChange: (Int) -> Unit,
    onScrollingChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = rememberLazyListState(initialFirstVisibleItemIndex = value - range.first)
    val scope = rememberCoroutineScope()
    val currentOnChange by rememberUpdatedState(onValueChange)
    val currentOnScrollingChange by rememberUpdatedState(onScrollingChange)
    // Three rows with a full 48dp target; grow the row rather than clipping larger system text.
    val numberStyle = MaterialTheme.typography.headlineMedium
    val rowHeight = maxOf(LayoutTokens.MinTouchTarget, with(LocalDensity.current) { numberStyle.fontSize.toDp() } * 1.5f)
    LaunchedEffect(value, range) {
        if (!state.isScrollInProgress) state.scrollToItem(value - range.first)
    }
    LaunchedEffect(state, range, enabled) {
        snapshotFlow {
            if (!enabled || state.isScrollInProgress) null
            else {
                val layout = state.layoutInfo
                val center = (layout.viewportStartOffset + layout.viewportEndOffset) / 2
                layout.visibleItemsInfo.minByOrNull { kotlin.math.abs(it.offset + it.size / 2 - center) }
                    ?.index?.plus(range.first)
            }
        }.filterNotNull().distinctUntilChanged().collect { currentOnChange(it) }
    }
    LaunchedEffect(state) {
        snapshotFlow { state.isScrollInProgress }.distinctUntilChanged().collect { currentOnScrollingChange(it) }
    }
    DisposableEffect(Unit) { onDispose { currentOnScrollingChange(false) } }
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box(Modifier.fillMaxWidth().height(rowHeight * 3).clearAndSetSemantics {
            contentDescription = label
            stateDescription = value.toString()
            if (!enabled) disabled()
            progressBarRangeInfo = ProgressBarRangeInfo(value.toFloat(), range.first.toFloat()..range.last.toFloat(),
                (range.last - range.first - 1).coerceAtLeast(0))
            if (enabled) setProgress { requested ->
                val selected = kotlin.math.round(requested).toInt().coerceIn(range)
                currentOnChange(selected)
                scope.launch { state.animateScrollToItem(selected - range.first) }
                true
            }
        }) {
            LazyColumn(
                state = state,
                userScrollEnabled = enabled,
                flingBehavior = rememberSnapFlingBehavior(state),
                contentPadding = PaddingValues(vertical = rowHeight),
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                items(range.toList(), key = { it }) { number ->
                    Box(
                        Modifier.fillMaxWidth().height(rowHeight).clickable(enabled = enabled) {
                            scope.launch { state.animateScrollToItem(number - range.first) }
                        }, contentAlignment = Alignment.Center,
                    ) {
                        Text(number.toString(), style = numberStyle, textAlign = TextAlign.Center,
                            color = if (number == value) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Column(Modifier.align(Alignment.Center).fillMaxWidth().height(rowHeight), verticalArrangement = Arrangement.SpaceBetween) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}
