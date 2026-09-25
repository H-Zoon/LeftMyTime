package com.devidea.timeleft.ui.components

import android.animation.ValueAnimator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import com.devidea.timeleft.AdapterItem
import com.devidea.timeleft.R
import com.devidea.timeleft.calc.TimeRangePhase
import com.devidea.timeleft.ui.theme.LayoutTokens
import com.devidea.timeleft.ui.theme.Motion
import com.devidea.timeleft.ui.theme.Spacing

/** The caller keys this subtree by item + original range, not by the changing countdown. */
@Composable
internal fun InteractiveRemainingTime(item: AdapterItem, waiting: Boolean) {
    val facts = item.detailFacts
    val actual = if (waiting) facts?.secondsUntilStart ?: item.secondsUntilStart else
        item.remainingDays?.toLong() ?: facts?.secondsLeft ?: item.remainingSeconds
    val maximum = facts?.range?.maximum ?: facts?.total ?: 0L
    val eligible = !waiting && facts?.phase == TimeRangePhase.Active && actual != null && actual > 0 && maximum > actual
    var entered by rememberSaveable { mutableStateOf(false) }
    var totalSeconds by rememberSaveable { mutableStateOf(false) }
    val haptics = rememberInteractionHaptics()
    val entrance = remember { eligible && !entered && ValueAnimator.areAnimatorsEnabled() }
    val entrancePlan = remember { RemainingTimeEntrance(maximum, actual ?: 0L, inDays = item.remainingDays != null) }
    val unitReference = maxOf(maximum, actual ?: 0L)
    val progress = remember { Animatable(if (entrance) 0f else 1f) }
    LaunchedEffect(Unit) {
        // Mark before starting so rotation or restoration never replays the maximum.
        entered = true
        // The plan owns the easing per visible unit; this is only the shared 1200ms clock.
        if (entrance) progress.animateTo(1f, tween(Motion.DetailEntranceMs, easing = LinearEasing))
    }
    LaunchedEffect(eligible, totalSeconds) {
        if (!eligible || totalSeconds) progress.snapTo(1f)
    }
    val running = entrance && eligible && progress.value < 1f && ValueAnimator.areAnimatorsEnabled()
    val shown = if (running && actual != null) {
        // Keep the entry plan stable across ticks, but carry the live second forward.
        (entrancePlan.valueAt(progress.value) + actual - entrancePlan.target)
            .coerceIn(actual, maxOf(actual, entrancePlan.maximum))
    } else null
    val currentLabel = remainingTimeLabel(
        seconds = actual.takeIf { item.remainingDays == null },
        days = item.remainingDays, showSeconds = true,
    )
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        // Always retain the true value as the caption; the entrance never hides it.
        if (!waiting) Text(stringResource(R.string.detail_actual_remaining, currentLabel),
            modifier = Modifier.clearAndSetSemantics {}, // The large readout already exposes the true value.
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        RemainingTimeText(item, hero = true, showSeconds = true, untilStart = waiting, showRelation = !waiting,
            presentationValue = shown, reserveValue = maximum.takeIf { !waiting },
            unitReferenceSeconds = unitReference.takeIf { !waiting }, totalSeconds = totalSeconds)
        if (item.remainingDays == null && actual != null &&
            (actual >= 60 || (!waiting && maximum >= 60) || totalSeconds) && facts?.validRange != false) {
            TextButton(onClick = { totalSeconds = !totalSeconds; haptics.tick() },
                modifier = Modifier.heightIn(min = LayoutTokens.MinTouchTarget).semantics { selected = totalSeconds },
                contentPadding = PaddingValues(vertical = Spacing.s)) {
                Text(stringResource(if (totalSeconds) R.string.detail_show_time_units else R.string.detail_show_total_seconds))
            }
        }
    }
}
