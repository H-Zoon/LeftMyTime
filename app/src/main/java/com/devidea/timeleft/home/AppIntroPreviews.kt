package com.devidea.timeleft.home

import android.graphics.BitmapFactory
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.devidea.timeleft.R
import com.devidea.timeleft.focus.DEFAULT_FOCUS_MINUTES
import com.devidea.timeleft.ui.TimeDisplayItem
import com.devidea.timeleft.ui.components.RemainingTimeText
import com.devidea.timeleft.ui.components.TimeRuler
import com.devidea.timeleft.ui.theme.Motion
import com.devidea.timeleft.ui.theme.Spacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

@Composable
internal fun rememberIntroTripFrames(): ImageBitmap? {
    val resources = LocalContext.current.resources
    val frames by produceState<ImageBitmap?>(null, resources) {
        value = withContext(Dispatchers.IO) {
            BitmapFactory.decodeResource(resources, R.drawable.app_intro_trip_frames)?.asImageBitmap()
        }
    }
    return frames
}

@Composable
internal fun IntroFocusPreview(today: TimeDisplayItem?) {
    val animation = remember { Animatable(0f) }
    LaunchedEffect(animation) {
        animation.animateTo(1f, tween(Motion.IntroFocusPreviewMs, easing = LinearEasing))
    }
    val running by remember { derivedStateOf { animation.value > 0.12f } }
    val timer by remember {
        derivedStateOf {
            val seconds = DEFAULT_FOCUS_MINUTES * 60 - (animation.value * 4).toInt()
            String.format(Locale.ROOT, "%02d:%02d", seconds / 60, seconds % 60)
        }
    }

    Column(Modifier.fillMaxWidth().padding(top = Spacing.s), verticalArrangement = Arrangement.spacedBy(Spacing.page)) {
        Column(
            Modifier.fillMaxWidth().clearAndSetSemantics {},
            verticalArrangement = Arrangement.spacedBy(Spacing.s),
        ) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                Modifier.fillMaxWidth().padding(top = Spacing.s),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    stringResource(if (running) R.string.focus_running else R.string.focus_default_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Icon(
                    if (running) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Text(timer, style = MaterialTheme.typography.displaySmall.copy(fontFeatureSettings = "tnum"))
        }

        if (today != null) {
            val widgetPhase by remember { derivedStateOf { ((animation.value - .28f) / .72f).coerceIn(0f, 1f) } }
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                Text(
                    stringResource(R.string.theme_widget_preview_size),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Column(
                    Modifier.fillMaxWidth().graphicsLayer {
                        alpha = .58f + .42f * widgetPhase
                    }.border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.small)
                        .padding(Spacing.l),
                    verticalArrangement = Arrangement.spacedBy(Spacing.s),
                ) {
                    Text(stringResource(R.string.period_today), style = MaterialTheme.typography.titleMedium)
                    RemainingTimeText(today, hero = false, showSeconds = false)
                    TimeRuler(
                        (today.detailFacts?.percentElapsed ?: today.percent) * widgetPhase,
                        today.startLabel,
                        today.endLabel,
                    )
                }
            }
        }
    }
}

@Composable
internal fun IntroVideoPreview(frames: ImageBitmap?) {
    val animation = remember { Animatable(0f) }
    LaunchedEffect(frames) {
        if (frames != null) animation.animateTo(1f, tween(Motion.IntroVideoPreviewMs, easing = LinearEasing))
    }
    val scenes = listOf(R.string.app_intro_trip_prepare, R.string.app_intro_trip_depart, R.string.app_intro_trip_arrive)
    val mask = MaterialTheme.colorScheme.background
    val divider = MaterialTheme.colorScheme.outlineVariant
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column(
            Modifier.fillMaxWidth().padding(top = Spacing.s),
            verticalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            Box(
                Modifier.fillMaxWidth().aspectRatio(3f / 2f)
                    .clip(MaterialTheme.shapes.small).background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                if (frames != null) {
                    Image(frames, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    Row(Modifier.fillMaxSize()) {
                        scenes.forEachIndexed { index, _ ->
                            Box(
                                Modifier.weight(1f).fillMaxHeight()
                                    .graphicsLayer {
                                        val visible = (animation.value * 2f + 1f - index).coerceIn(0f, 1f)
                                        alpha = .88f * (1f - visible)
                                    }.background(mask),
                            )
                        }
                    }
                }
            }
            HorizontalDivider(color = divider)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                scenes.forEachIndexed { index, scene ->
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        Text(
                            if (index == scenes.lastIndex) stringResource(R.string.home_dday_today)
                            else stringResource(R.string.home_dday_before, scenes.lastIndex - index),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(stringResource(scene), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
