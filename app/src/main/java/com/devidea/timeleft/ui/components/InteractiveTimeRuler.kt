package com.devidea.timeleft.ui.components

import android.animation.ValueAnimator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.devidea.timeleft.AdapterItem
import com.devidea.timeleft.R
import com.devidea.timeleft.TimeDetailRange
import com.devidea.timeleft.calc.TimeRangePhase
import com.devidea.timeleft.formatRemainingTime
import com.devidea.timeleft.ui.theme.LayoutTokens
import com.devidea.timeleft.ui.theme.Motion
import com.devidea.timeleft.ui.theme.Spacing
import com.devidea.timeleft.ui.theme.TimeRulerTokens
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.abs

/** Only app details use this controller. Static home/list/widget rulers share the drawing. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun InteractiveTimeRuler(item: AdapterItem, range: TimeDetailRange) {
    val facts = item.detailFacts ?: return
    val context = LocalContext.current
    val locale = context.resources.configuration.locales[0]
    val haptics = rememberInteractionHaptics()
    var probe by rememberSaveable { mutableStateOf<Long?>(null) }
    var width by remember { mutableFloatStateOf(1f) }
    var dragging by remember { mutableStateOf(false) }
    var touchFraction by remember { mutableStateOf<Float?>(null) }
    var pulse by remember { mutableIntStateOf(0) }
    val touchStrength = remember { Animatable(0f) }
    val snapRadius = with(LocalDensity.current) { TimeRulerTokens.CurrentSnapRadius.dp.toPx() }
    val current = range.clamp(facts.elapsed)
    val selected = range.clamp(probe ?: current)

    fun selectOffset(offset: Long) {
        val target = range.clamp(offset)
        val previous = probe ?: current
        if ((target != previous && (target / range.hapticStep != previous / range.hapticStep || target == current)) ||
            (probe == null && target == current)) haptics.tick()
        probe = target
        touchFraction = range.fraction(target)
        if (!dragging) pulse++
    }
    fun reset() { probe = null; touchFraction = null; dragging = false }
    val selectAt by rememberUpdatedState<(Float) -> Unit> { fraction ->
        val snapFraction = minOf(snapRadius / width, TimeRulerTokens.CurrentSnapMaxFraction)
        val nearCurrent = facts.phase == TimeRangePhase.Active &&
            abs(fraction - range.fraction(current)) <= snapFraction
        selectOffset(if (nearCurrent) current else range.offsetAt(fraction))
    }
    val moveBy by rememberUpdatedState<(Long) -> Unit> { delta -> selectOffset((probe ?: current) + delta) }
    val dragState by rememberUpdatedState<(Boolean) -> Unit> { dragging = it }
    LaunchedEffect(pulse, dragging, touchFraction == null) {
        if (touchFraction == null || !ValueAnimator.areAnimatorsEnabled()) {
            touchStrength.snapTo(0f)
        } else {
            touchStrength.animateTo(1f, tween(Motion.ShortMs))
            if (!dragging) touchStrength.animateTo(0f, tween(Motion.MediumMs))
        }
    }
    val timeLabel = when (range) {
        is TimeDetailRange.Calendar -> range.start.plusDays(selected - range.minimum)
            .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))
        is TimeDetailRange.Clock -> {
            val time = range.start.plusSeconds(selected)
            val seconds = range.maximum < 60 || time.second != 0
            val pattern = if (android.text.format.DateFormat.is24HourFormat(context)) {
                if (seconds) "H:mm:ss" else "H:mm"
            } else if (seconds) "h:mm:ss a" else "h:mm a"
            time.format(DateTimeFormatter.ofPattern(pattern, locale))
        }
    }
    val remaining = range.remaining(selected)
    val remainingLabel = formatRemainingTime(context,
        seconds = remaining.takeIf { range is TimeDetailRange.Clock },
        days = remaining.toInt().takeIf { range is TimeDetailRange.Calendar }, showSeconds = true)
    val previewLabel = stringResource(R.string.detail_probe_value, timeLabel, remainingLabel)
    val rulerLabel = stringResource(R.string.detail_probe_description, item.startLabel, item.endLabel)
    val previousLabel = stringResource(R.string.detail_probe_previous)
    val nextLabel = stringResource(R.string.detail_probe_next)
    val resetLabel = stringResource(R.string.detail_probe_now)
    val currentLabel = stringResource(R.string.detail_probe_current, timeLabel)

    val input = Modifier.onSizeChanged { width = it.width.toFloat().coerceAtLeast(1f) }
        .semantics {
            contentDescription = rulerLabel
            stateDescription = if (probe == null) currentLabel else previewLabel
            progressBarRangeInfo = ProgressBarRangeInfo(selected.toFloat(), range.minimum.toFloat()..range.maximum.toFloat())
            setProgress { value ->
                if (value.isFinite()) { selectOffset(range.offsetAt(value / range.maximum)); true } else false
            }
            customActions = listOf(
                CustomAccessibilityAction(previousLabel) { selectOffset(selected - range.step); true },
                CustomAccessibilityAction(nextLabel) { selectOffset(selected + range.step); true },
                CustomAccessibilityAction(resetLabel) { reset(); true },
            )
        }
        .onKeyEvent { event ->
            if (event.type != KeyEventType.KeyDown) false else when (event.key) {
                Key.DirectionLeft -> { selectOffset(selected - range.step); true }
                Key.DirectionRight -> { selectOffset(selected + range.step); true }
                Key.MoveHome -> { selectOffset(range.minimum); true }
                Key.MoveEnd -> { selectOffset(range.maximum); true }
                Key.Escape -> { reset(); true }
                else -> false
            }
        }.focusable()
        .pointerInput(range) {
            detectTapGestures(onTap = { selectAt(it.x / size.width.coerceAtLeast(1)) })
        }
        .pointerInput(range) {
            try {
                detectHorizontalDragGestures(
                    onDragStart = { dragState(true); selectAt(it.x / size.width.coerceAtLeast(1)) },
                    onDragEnd = { dragState(false) },
                    onDragCancel = { dragState(false) },
                ) { change, _ ->
                    selectAt(change.position.x / size.width.coerceAtLeast(1))
                    change.consume()
                }
            } finally { dragState(false) }
        }
        .pointerInput(range) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    if (event.type == PointerEventType.Scroll) {
                        val x = event.changes.sumOf { it.scrollDelta.x.toDouble() }.toFloat()
                        val y = event.changes.sumOf { it.scrollDelta.y.toDouble() }.toFloat()
                        // Preserve vertical scrolling for the sheet and page.
                        if (abs(x) > abs(y) && x != 0f) {
                            moveBy(if (x > 0) range.step else -range.step)
                            event.changes.forEach { it.consume() }
                        }
                    }
                }
            }
        }
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        TimeRuler(facts.percentElapsed, item.startLabel, item.endLabel, glowEnabled = facts.glowActive,
            interaction = RulerInteraction(probe?.let(range::fraction), touchFraction, touchStrength.value),
            trackModifier = input)
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.l),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(if (probe == null) currentLabel else previewLabel, modifier = Modifier.alignByBaseline(),
                style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (probe != null) TextButton(onClick = { reset(); haptics.tick() },
                modifier = Modifier.heightIn(min = LayoutTokens.MinTouchTarget).alignByBaseline(),
                contentPadding = PaddingValues(vertical = Spacing.s)) { Text(resetLabel) }
        }
        Text(stringResource(R.string.detail_probe_hint), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
