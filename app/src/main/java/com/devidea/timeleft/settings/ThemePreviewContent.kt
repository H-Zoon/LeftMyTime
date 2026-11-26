package com.devidea.timeleft.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.devidea.timeleft.R
import com.devidea.timeleft.config.UserPreferences
import com.devidea.timeleft.home.*
import com.devidea.timeleft.ui.theme.*

@Composable
internal fun ThemePreviewEntry(
    themeMode: String,
    paletteKey: String,
    onOpen: () -> Unit,
) {
    val modeRes = when (themeMode) {
        UserPreferences.THEME_LIGHT -> R.string.settings_theme_light
        UserPreferences.THEME_DARK -> R.string.settings_theme_dark
        else -> R.string.settings_theme_system
    }
    SettingsNavigationRow(
        title = stringResource(R.string.theme_preview_title),
        summary = stringResource(R.string.settings_theme_summary,
            stringResource(LocalTimeLayout.current.labelRes),
            stringResource(ThemePalette.fromKey(paletteKey).labelRes), stringResource(modeRes)),
        onClick = onOpen,
    )
}

/** Reuses production renderers at the real available width, with action semantics removed. */
@Composable
internal fun ThemeHomePreview(snapshot: ThemePreviewSnapshot, expiredItemsMode: String, progressDisplayMode: String) {
    val items = snapshot.items.filterNot { expiredItemsMode == UserPreferences.EXPIRED_ITEMS_HIDE && it.isExpired }
    val candidates = HomeHeroCandidates(items)
    val hero = candidates.hero(null)
    val times = upcomingTimeItems(items).take(3)
    val dates = candidates.dateItems.filterNot { it.id == hero?.id }.take(3)
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
