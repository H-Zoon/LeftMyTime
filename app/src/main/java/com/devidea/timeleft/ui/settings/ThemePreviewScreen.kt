package com.devidea.timeleft.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.ExperimentalFoundationApi
import com.devidea.timeleft.ui.components.RemainingTimeText
import com.devidea.timeleft.ui.components.TimeRuler
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.devidea.timeleft.R
import com.devidea.timeleft.preferences.UserPreferences
import com.devidea.timeleft.ui.components.TimeLeftTopAppBar
import com.devidea.timeleft.ui.components.remainingTimeLabel
import com.devidea.timeleft.ui.home.selectHomeHero
import com.devidea.timeleft.ui.theme.*
import com.devidea.timeleft.widget.*

@Composable
@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
internal fun ThemePreviewScreen(
    initialThemeMode: String,
    initialPaletteKey: String,
    systemDark: Boolean,
    state: ThemePreviewState,
    expiredItemsMode: String,
    progressDisplayMode: String,
    onBack: () -> Unit,
    onApply: (ThemeSelection) -> Unit,
    onRetry: () -> Unit,
    initialDesignKey: String = UserPreferences.DESIGN_TIME_FOCUS,
) {
    val initial = ThemeSelection(initialThemeMode, initialPaletteKey, initialDesignKey).normalized()
    var designKey by rememberSaveable { mutableStateOf(initial.designKey) }
    var mode by rememberSaveable { mutableStateOf(initial.mode) }
    var paletteKey by rememberSaveable { mutableStateOf(initial.paletteKey) }
    var showFullPreview by rememberSaveable { mutableStateOf(false) }
    var showWidgets by rememberSaveable { mutableStateOf(false) }
    var applying by remember { mutableStateOf(false) }
    val selection = ThemeSelection(mode, paletteKey, designKey)
    val theme = resolveTheme(selection, systemDark)
    BackHandler(onBack = onBack)
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { TimeLeftTopAppBar(stringResource(R.string.theme_preview_title), onBack) },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.background) {
                Row(Modifier.fillMaxWidth().navigationBarsPadding()
                    .padding(horizontal = LayoutTokens.ScreenHorizontal, vertical = Spacing.m),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
                    TextButton(onClick = onBack, modifier = Modifier.weight(1f).heightIn(min = LayoutTokens.MinTouchTarget)) {
                        Text(stringResource(R.string.action_cancel))
                    }
                    Button(onClick = { if (!applying && selection != initial) { applying = true; onApply(selection) } }, enabled = selection != initial && !applying,
                        modifier = Modifier.weight(1f).heightIn(min = LayoutTokens.MinTouchTarget)) {
                        Text(stringResource(R.string.theme_apply))
                    }
                }
            }
        },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(vertical = Spacing.l)) {
            stickyHeader(key = "live-sample") {
                TimeLeftTheme(themeMode = if (theme.dark) UserPreferences.THEME_DARK else UserPreferences.THEME_LIGHT, paletteKey = paletteKey, designKey = designKey) {
                    Surface(color = MaterialTheme.colorScheme.background) {
                        Column(Modifier.fillMaxWidth().padding(horizontal = LayoutTokens.ScreenHorizontal, vertical = Spacing.m),
                            verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                            val item = state.snapshot?.periods?.firstOrNull()
                            if (item != null) {
                                com.devidea.timeleft.ui.components.TimeHeadline(spacing = Spacing.s,
                                    label = { Text(item.title, style = MaterialTheme.typography.titleMedium) },
                                    value = { RemainingTimeText(item, hero = false, compact = true, showSeconds = true,
                                        centered = theme.definition.layout == TimeLayout.TimeBoard) })
                                if (progressDisplayMode != UserPreferences.PROGRESS_DISPLAY_HIDDEN)
                                    TimeRuler(item.detailFacts?.percentElapsed ?: item.percent, "", "", glowEnabled = item.detailFacts?.glowActive == true)
                            } else if (state.loading) Text(stringResource(R.string.theme_preview_loading))
                            else TextButton(onClick = onRetry) { Text(stringResource(R.string.theme_preview_retry)) }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }
            item(key = "choices") {
                Column(Modifier.padding(horizontal = LayoutTokens.ScreenHorizontal), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    SettingsDropdownRow(title = stringResource(R.string.settings_layout_theme), selectedValue = designKey,
                        options = TimeLayout.entries.map { SettingsOption(it.key, it.labelRes) }, onSelected = { designKey = it })
                    Text(stringResource(R.string.theme_preview_explanation), style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    SettingsDropdownRow(
                        title = stringResource(R.string.settings_theme_mode), selectedValue = mode,
                        options = listOf(
                            SettingsOption(UserPreferences.THEME_AUTO, R.string.settings_theme_system),
                            SettingsOption(UserPreferences.THEME_LIGHT, R.string.settings_theme_light),
                            SettingsOption(UserPreferences.THEME_DARK, R.string.settings_theme_dark),
                        ), onSelected = { mode = it },
                        summary = if (mode == UserPreferences.THEME_AUTO)
                            stringResource(if (systemDark) R.string.theme_system_dark else R.string.theme_system_light) else null,
                    )
                    SettingsDropdownRow(
                        title = stringResource(R.string.settings_color_theme), selectedValue = paletteKey,
                        options = theme.definition.palettes.map { SettingsOption(it.key, it.labelRes) },
                        onSelected = { paletteKey = it },
                    )
                    TextButton(onClick = { showFullPreview = !showFullPreview },
                        modifier = Modifier.heightIn(min = LayoutTokens.MinTouchTarget)) {
                        Text(stringResource(if (showFullPreview) R.string.theme_hide_full_preview else R.string.theme_show_full_preview))
                    }
                    if (showFullPreview) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                        FilterChip(selected = !showWidgets, onClick = { showWidgets = false },
                            modifier = Modifier.heightIn(min = LayoutTokens.MinTouchTarget),
                            label = { Text(stringResource(R.string.theme_preview_app)) })
                        FilterChip(selected = showWidgets, onClick = { showWidgets = true },
                            modifier = Modifier.heightIn(min = LayoutTokens.MinTouchTarget),
                            label = { Text(stringResource(R.string.theme_preview_widgets)) })
                    }
                    }
                    Text(stringResource(R.string.theme_preview_snapshot), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = Spacing.m))
                }
            }
            if (showFullPreview) item(key = "preview") {
                // Resolve system mode from the device, even when the parent app is forced light/dark.
                TimeLeftTheme(themeMode = if (theme.dark) UserPreferences.THEME_DARK else UserPreferences.THEME_LIGHT, paletteKey = paletteKey, designKey = designKey) {
                    Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.background) {
                        Column {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            when {
                                state.loading -> Text(stringResource(R.string.theme_preview_loading),
                                    modifier = Modifier.padding(LayoutTokens.ScreenHorizontal), style = MaterialTheme.typography.bodyMedium)
                                state.failed || state.snapshot == null -> Column(Modifier.padding(LayoutTokens.ScreenHorizontal)) {
                                    Text(stringResource(R.string.theme_preview_error), style = MaterialTheme.typography.bodyMedium)
                                    TextButton(onClick = onRetry, modifier = Modifier.heightIn(min = LayoutTokens.MinTouchTarget)) {
                                        Text(stringResource(R.string.theme_preview_retry))
                                    }
                                }
                                showWidgets -> ThemeWidgetPreviews(state.snapshot, paletteKey, theme.dark, expiredItemsMode, progressDisplayMode)
                                else -> ThemeHomePreview(state.snapshot, expiredItemsMode, progressDisplayMode)
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeWidgetPreviews(snapshot: ThemePreviewSnapshot, paletteKey: String, dark: Boolean,
    expiredItemsMode: String, progressDisplayMode: String) {
    val items = snapshot.items.filterNot { expiredItemsMode == UserPreferences.EXPIRED_ITEMS_HIDE && it.isExpired }
    val hero = selectHomeHero(items, null)
    val singleItem = hero ?: snapshot.periods.firstOrNull()
    val source = if (hero == null) WidgetSource.Today else WidgetSource.Custom
    Column(Modifier.padding(horizontal = LayoutTokens.ScreenHorizontal, vertical = Spacing.l),
        verticalArrangement = Arrangement.spacedBy(Spacing.l)) {
        Text(stringResource(R.string.theme_widget_single), style = MaterialTheme.typography.titleMedium)
        WidgetPreviewBand(
            dimensions = WidgetDimensions.previewFor(source), configuration = WidgetConfiguration(source = source, itemId = hero?.id),
            item = singleItem, periods = snapshot.periods, paletteKey = paletteKey, dark = dark,
            emptyMessage = R.string.widget_no_upcoming, showProgress = progressDisplayMode != UserPreferences.PROGRESS_DISPLAY_HIDDEN,
            snapshotTimeMillis = snapshot.capturedAtMillis,
            previewDescription = stringResource(R.string.theme_widget_description, singleItem?.title.orEmpty(),
                remainingTimeLabel(singleItem?.remainingSeconds, singleItem?.remainingDays, singleItem?.leftString.orEmpty())),
        )
        Text(stringResource(R.string.theme_widget_overview), style = MaterialTheme.typography.titleMedium)
        WidgetPreviewBand(
            dimensions = WidgetDimensions.previewFor(WidgetSource.Overview), configuration = WidgetConfiguration(source = WidgetSource.Overview),
            item = null, periods = snapshot.periods, paletteKey = paletteKey, dark = dark,
            emptyMessage = R.string.widget_no_upcoming, showProgress = progressDisplayMode != UserPreferences.PROGRESS_DISPLAY_HIDDEN,
            snapshotTimeMillis = snapshot.capturedAtMillis,
            previewDescription = snapshot.periods.joinToString { it.title + " · " + it.leftString },
        )
    }
}
