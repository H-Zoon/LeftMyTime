package com.devidea.timeleft.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.devidea.timeleft.R
import com.devidea.timeleft.ui.TimeDisplayItem
import com.devidea.timeleft.ui.components.RemainingTimeText
import com.devidea.timeleft.ui.components.TimeLeftInlineButton
import com.devidea.timeleft.ui.components.TimeRuler
import com.devidea.timeleft.ui.theme.LayoutTokens
import com.devidea.timeleft.ui.theme.Spacing

internal enum class AppIntroPage(val titleRes: Int, val descriptionRes: Int) {
    Time(R.string.app_intro_time_title, R.string.app_intro_time_description),
    Focus(R.string.app_intro_focus_title, R.string.app_intro_focus_description),
    Video(R.string.app_intro_video_title, R.string.app_intro_video_description),
}

@Composable
internal fun AppIntroScreen(
    today: TimeDisplayItem?,
    canOpenRecord: Boolean?,
    onComplete: () -> Unit,
    onVideoAction: () -> Unit,
    initialPage: AppIntroPage = AppIntroPage.Time,
) {
    val pages = AppIntroPage.entries
    var pageIndex by rememberSaveable(initialPage) { mutableIntStateOf(initialPage.ordinal) }
    val tripFrames = rememberIntroTripFrames()
    val currentIndex = pageIndex
    val page = pages[currentIndex]
    val lastPage = currentIndex == pages.lastIndex
    val stepDescription = stringResource(R.string.app_intro_progress, currentIndex + 1, pages.size)

    fun goBack() {
        if (pageIndex > 0) pageIndex-- else onComplete()
    }

    BackHandler { goBack() }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = LayoutTokens.ScreenHorizontal)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                TimeLeftInlineButton(onClick = onComplete) {
                    Text(stringResource(R.string.app_intro_skip))
                }
            }
            LinearProgressIndicator(
                progress = (currentIndex + 1f) / pages.size,
                modifier = Modifier.fillMaxWidth().semantics { stateDescription = stepDescription },
            )

            // Recompose the header, current page, and actions from the same step.
            key(page) {
                Column(
                    Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                        .padding(vertical = LayoutTokens.SectionGap),
                    verticalArrangement = Arrangement.spacedBy(Spacing.l),
                ) {
                    Text(
                        stringResource(page.titleRes),
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        stringResource(page.descriptionRes),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    when (page) {
                        AppIntroPage.Time -> {
                            if (today != null) {
                                Column(
                                    Modifier.fillMaxWidth().padding(top = Spacing.l),
                                    verticalArrangement = Arrangement.spacedBy(Spacing.m),
                                ) {
                                    Text(stringResource(R.string.period_today), style = MaterialTheme.typography.labelLarge)
                                    RemainingTimeText(today, hero = true, showSeconds = false)
                                    TimeRuler(today.detailFacts?.percentElapsed ?: today.percent, today.startLabel, today.endLabel)
                                }
                            }
                        }
                        AppIntroPage.Focus -> IntroFocusPreview(today)
                        AppIntroPage.Video -> IntroVideoPreview(tripFrames)
                    }
                }
            }

            Column(
                Modifier.fillMaxWidth().padding(vertical = Spacing.l),
                verticalArrangement = Arrangement.spacedBy(Spacing.s),
            ) {
                if (lastPage && canOpenRecord != null) {
                    TimeLeftInlineButton(onClick = onVideoAction) {
                        Text(stringResource(if (canOpenRecord) R.string.app_intro_open_video else R.string.app_intro_add_date))
                    }
                }
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.l),
                ) {
                    if (currentIndex > 0) {
                        TimeLeftInlineButton(onClick = ::goBack) {
                            Text(stringResource(R.string.app_intro_previous))
                        }
                    }
                    Button(
                        onClick = {
                            if (pageIndex == currentIndex) {
                                if (lastPage) onComplete() else pageIndex = currentIndex + 1
                            }
                        },
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.weight(1f).heightIn(min = LayoutTokens.MinTouchTarget),
                    ) {
                        Text(stringResource(if (lastPage) R.string.app_intro_start else R.string.app_intro_next))
                    }
                }
            }
        }
    }
}
