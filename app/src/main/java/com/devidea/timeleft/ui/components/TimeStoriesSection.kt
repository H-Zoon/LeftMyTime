package com.devidea.timeleft.ui.components

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import android.animation.ValueAnimator
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.unit.dp
import com.devidea.timeleft.R
import com.devidea.timeleft.ui.theme.LayoutTokens
import com.devidea.timeleft.ui.theme.Spacing
import com.devidea.timeleft.ui.theme.Motion
import kotlin.math.abs

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TimeStoriesSection(identity: String, remaining: Long, inDays: Boolean) {
    // Freeze the shortlist while reading, including rotation. A new item/phase opens a new set.
    // Historical stories do not become impossible when the countdown passes their duration.
    val anchor by rememberSaveable(identity, inDays) { mutableLongStateOf(remaining) }
    var index by rememberSaveable(identity, inDays) { mutableIntStateOf(0) }
    val candidates = remember(anchor, inDays) { timeStories(anchor, inDays) }
    if (remaining <= 0 || candidates.isEmpty()) return
    val selected = candidates[index.coerceIn(candidates.indices)]
    val context = LocalContext.current
    val haptics = rememberInteractionHaptics()
    var direction by remember(identity) { mutableIntStateOf(1) }
    val changeStory by rememberUpdatedState<(Int) -> Unit> { delta ->
        direction = delta
        index = (index + delta + candidates.size) % candidates.size
        haptics.tick()
    }
    val swipeDistance = with(LocalDensity.current) { LayoutTokens.MinTouchTarget.toPx() }
    val slideDistance = with(LocalDensity.current) { Spacing.s.roundToPx() }
    val anotherLabel = stringResource(R.string.time_stories_another)
    val previousLabel = stringResource(R.string.time_stories_previous)
    val durationLabel = stringResource(R.string.time_stories_duration, stringResource(selected.durationRes))
    val storyDescription = durationLabel + ". " + stringResource(selected.textRes)
    val swipe = if (candidates.size > 1) Modifier.pointerInput(identity, candidates) {
        var distance = 0f
        detectHorizontalDragGestures(
            onDragStart = { distance = 0f },
            onDragCancel = { distance = 0f },
            onDragEnd = {
                if (abs(distance) >= swipeDistance) changeStory(if (distance < 0) 1 else -1)
                distance = 0f
            },
        ) { change, amount -> distance += amount; change.consume() }
    } else Modifier
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Text(stringResource(R.string.time_stories_title), style = MaterialTheme.typography.titleMedium)
        AnimatedContent(targetState = selected, label = "Time story",
            modifier = Modifier.fillMaxWidth().then(swipe).clearAndSetSemantics {
                // Read only the selected story, not both outgoing and incoming text.
                contentDescription = storyDescription
                if (candidates.size > 1) customActions = listOf(
                    CustomAccessibilityAction(anotherLabel) { changeStory(1); true },
                    CustomAccessibilityAction(previousLabel) { changeStory(-1); true },
                )
            },
            transitionSpec = {
                if (!ValueAnimator.areAnimatorsEnabled()) EnterTransition.None togetherWith ExitTransition.None
                else ((fadeIn(tween(Motion.StoryChangeMs)) +
                    slideInHorizontally(tween(Motion.StoryChangeMs)) { direction * slideDistance })
                    togetherWith (fadeOut(tween(Motion.ShortMs)) +
                    slideOutHorizontally(tween(Motion.StoryChangeMs)) { -direction * slideDistance }))
                    .using(SizeTransform(clip = false) { _, _ -> tween(Motion.StoryChangeMs) })
            }) { story ->
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                Text(stringResource(R.string.time_stories_duration, stringResource(story.durationRes)),
                    style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(stringResource(story.textRes), style = MaterialTheme.typography.bodyLarge)
            }
        }
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.l)) {
            if (candidates.size > 1) {
                TextButton(onClick = { changeStory(1) },
                    modifier = Modifier.heightIn(min = LayoutTokens.MinTouchTarget).alignByBaseline(),
                    contentPadding = PaddingValues(horizontal = 0.dp, vertical = Spacing.s)) {
                    Text(stringResource(R.string.time_stories_another))
                }
            }
            TextButton(onClick = {
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(selected.sourceUrl)))
                } catch (_: ActivityNotFoundException) {
                    Toast.makeText(context, R.string.time_stories_source_unavailable, Toast.LENGTH_SHORT).show()
                }
            }, modifier = Modifier.heightIn(min = LayoutTokens.MinTouchTarget).alignByBaseline(),
                contentPadding = PaddingValues(horizontal = 0.dp, vertical = Spacing.s)) {
                Text(stringResource(R.string.time_stories_source, stringResource(selected.sourceNameRes)),
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
