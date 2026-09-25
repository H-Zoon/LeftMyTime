package com.devidea.timeleft.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.devidea.timeleft.AdapterItem
import com.devidea.timeleft.R
import com.devidea.timeleft.preferences.UserPreferences
import com.devidea.timeleft.ui.components.RemainingTimeText
import com.devidea.timeleft.ui.components.TimeRuler
import com.devidea.timeleft.ui.home.*
import com.devidea.timeleft.ui.theme.*

@Composable
internal fun ThemePreviewEntry(
    themeMode: String,
    paletteKey: String,
    periods: List<AdapterItem>,
    progressDisplayMode: String,
    onOpen: () -> Unit,
) {
    val modeRes = when (themeMode) {
        UserPreferences.THEME_LIGHT -> R.string.settings_theme_light
        UserPreferences.THEME_DARK -> R.string.settings_theme_dark
        else -> R.string.settings_theme_system
    }
    Column(
        Modifier.fillMaxWidth().heightIn(min = LayoutTokens.MinTouchTarget)
            .clickable(role = Role.Button, onClickLabel = stringResource(R.string.theme_preview_open), onClick = onOpen)
            .padding(vertical = Spacing.m),
        verticalArrangement = Arrangement.spacedBy(Spacing.s),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(stringResource(TimeLeftThemes.TimeFocus.nameRes), style = MaterialTheme.typography.bodyLarge)
                Text(stringResource(R.string.theme_selection_summary, stringResource(ThemePalette.fromKey(paletteKey).labelRes), stringResource(modeRes)),
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        periods.firstOrNull()?.let { today ->
            Text(stringResource(R.string.theme_preview_today), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            RemainingTimeText(today, hero = false, compact = true, showSeconds = true)
            if (progressDisplayMode != UserPreferences.PROGRESS_DISPLAY_HIDDEN) {
                TimeRuler(today.detailFacts?.percentElapsed ?: today.percent, "", "", glowEnabled = today.detailFacts?.glowActive == true)
            }
        }
        Text(stringResource(R.string.theme_preview_open), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    }
}

/** Reuses production renderers at the real available width, with action semantics removed. */
@Composable
internal fun ThemeHomePreview(snapshot: ThemePreviewSnapshot, expiredItemsMode: String, progressDisplayMode: String) {
    val items = snapshot.items.filterNot { expiredItemsMode == UserPreferences.EXPIRED_ITEMS_HIDE && it.isExpired }
    val hero = selectHomeHero(items, null)
    val times = upcomingTimeItems(items).take(3)
    val dates = homeDateItems(items).filterNot { it.id == hero?.id }.take(3)
    Column(Modifier.fillMaxWidth().padding(horizontal = LayoutTokens.ScreenHorizontal, vertical = Spacing.l)) {
        HeaderSection(snapshot.periods, progressDisplayMode, interactive = false)
        if (hero != null) NextCountdownHero(hero, progressDisplayMode, onEditItem = {}, interactive = false)
        if (items.isEmpty()) {
            EmptyItemState(onAddTime = {}, onAddDate = {}, interactive = false)
        } else {
            if (times.isNotEmpty()) PreviewGroupTitle(stringResource(R.string.home_time_ranges))
            times.forEach { TimeLeftItemCard(it, {}, {}, upcoming = true, progressDisplayMode = progressDisplayMode, interactive = false) }
            if (dates.isNotEmpty()) PreviewGroupTitle(stringResource(R.string.home_date_schedules))
            dates.forEach { TimeLeftItemCard(it, {}, {}, progressDisplayMode = progressDisplayMode, interactive = false) }
            if (times.isEmpty() && dates.isEmpty()) Text(stringResource(R.string.home_no_next_schedule),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PreviewGroupTitle(title: String) {
    Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = Spacing.l, bottom = Spacing.s))
}
