package com.devidea.timeleft.ui.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.devidea.timeleft.R
import com.devidea.timeleft.ItemVisuals
import com.devidea.timeleft.database.itemdata.ItemType
import com.devidea.timeleft.preferences.UserPreferences
import com.devidea.timeleft.ui.theme.Spacing
import com.devidea.timeleft.ui.theme.ThemePalette

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun SettingsScreen(
    themeMode: String,
    paletteKey: String,
    homeSort: String,
    startScreen: String,
    expiredItemsMode: String,
    progressDisplayMode: String,
    defaultDateReminderOffset: Int,
    defaultTimeReminderOffset: Int,
    dateReminderTime: String,
    remindersEnabled: Boolean,
    versionName: String,
    onBack: () -> Unit,
    onThemeSelected: (String) -> Unit,
    onPaletteSelected: (String) -> Unit,
    onSortSelected: (String) -> Unit,
    onStartScreenSelected: (String) -> Unit,
    onExpiredItemsModeSelected: (String) -> Unit,
    onProgressDisplayModeSelected: (String) -> Unit,
    onDefaultDateReminderSelected: (Int) -> Unit,
    onDefaultTimeReminderSelected: (Int) -> Unit,
    onSelectDateReminderTime: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
) {
    SettingsScaffold(
        title = stringResource(R.string.settings_title),
        onBack = onBack
    ) {
        SettingsSection(title = stringResource(R.string.settings_display)) {
            ChoiceRow(
                labelRes = R.string.settings_theme_system,
                selected = themeMode == UserPreferences.THEME_AUTO,
                onClick = { onThemeSelected(UserPreferences.THEME_AUTO) }
            )
            ChoiceRow(
                labelRes = R.string.settings_theme_light,
                selected = themeMode == UserPreferences.THEME_LIGHT,
                onClick = { onThemeSelected(UserPreferences.THEME_LIGHT) }
            )
            ChoiceRow(
                labelRes = R.string.settings_theme_dark,
                selected = themeMode == UserPreferences.THEME_DARK,
                onClick = { onThemeSelected(UserPreferences.THEME_DARK) }
            )
            Text(
                text = stringResource(R.string.settings_color_theme),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = Spacing.l, top = Spacing.m, bottom = 2.dp)
            )
            FlowRow(
                modifier = Modifier.padding(horizontal = Spacing.m, vertical = Spacing.s),
                horizontalArrangement = Arrangement.spacedBy(Spacing.s),
                verticalArrangement = Arrangement.spacedBy(Spacing.s)
            ) {
                ThemePalette.entries.forEach { palette ->
                    PaletteChip(
                        palette = palette,
                        selected = paletteKey == palette.key,
                        onClick = { onPaletteSelected(palette.key) }
                    )
                }
            }
        }

        SettingsSection(title = stringResource(R.string.settings_default_sort)) {
            ChoiceRow(
                labelRes = R.string.home_sort_nearest,
                selected = homeSort == UserPreferences.SORT_NEAREST,
                onClick = { onSortSelected(UserPreferences.SORT_NEAREST) }
            )
            ChoiceRow(
                labelRes = R.string.home_sort_created,
                selected = homeSort == UserPreferences.SORT_CREATED,
                onClick = { onSortSelected(UserPreferences.SORT_CREATED) }
            )
            ChoiceRow(
                labelRes = R.string.home_sort_title,
                selected = homeSort == UserPreferences.SORT_TITLE,
                onClick = { onSortSelected(UserPreferences.SORT_TITLE) }
            )
            ChoiceRow(
                labelRes = R.string.home_sort_progress,
                selected = homeSort == UserPreferences.SORT_PROGRESS,
                onClick = { onSortSelected(UserPreferences.SORT_PROGRESS) }
            )
        }

        SettingsSection(title = stringResource(R.string.settings_home_behavior)) {
            Text(
                text = stringResource(R.string.settings_start_screen),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = Spacing.l, top = Spacing.m, bottom = 2.dp)
            )
            ChoiceRow(
                labelRes = R.string.home_tab_overview,
                selected = startScreen == UserPreferences.START_SCREEN_OVERVIEW,
                onClick = { onStartScreenSelected(UserPreferences.START_SCREEN_OVERVIEW) }
            )
            ChoiceRow(
                labelRes = R.string.home_tab_items,
                selected = startScreen == UserPreferences.START_SCREEN_ITEMS,
                onClick = { onStartScreenSelected(UserPreferences.START_SCREEN_ITEMS) }
            )
            Text(
                text = stringResource(R.string.settings_expired_items),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = Spacing.l, top = Spacing.m, bottom = 2.dp)
            )
            ChoiceRow(
                labelRes = R.string.settings_expired_show,
                selected = expiredItemsMode == UserPreferences.EXPIRED_ITEMS_SHOW,
                onClick = { onExpiredItemsModeSelected(UserPreferences.EXPIRED_ITEMS_SHOW) }
            )
            ChoiceRow(
                labelRes = R.string.settings_expired_bottom,
                selected = expiredItemsMode == UserPreferences.EXPIRED_ITEMS_BOTTOM,
                onClick = { onExpiredItemsModeSelected(UserPreferences.EXPIRED_ITEMS_BOTTOM) }
            )
            ChoiceRow(
                labelRes = R.string.settings_expired_hide,
                selected = expiredItemsMode == UserPreferences.EXPIRED_ITEMS_HIDE,
                onClick = { onExpiredItemsModeSelected(UserPreferences.EXPIRED_ITEMS_HIDE) }
            )
            Text(
                text = stringResource(R.string.settings_progress_display),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = Spacing.l, top = Spacing.m, bottom = 2.dp)
            )
            ChoiceRow(
                labelRes = R.string.settings_progress_full,
                selected = progressDisplayMode == UserPreferences.PROGRESS_DISPLAY_FULL,
                onClick = { onProgressDisplayModeSelected(UserPreferences.PROGRESS_DISPLAY_FULL) }
            )
            ChoiceRow(
                labelRes = R.string.settings_progress_bar_only,
                selected = progressDisplayMode == UserPreferences.PROGRESS_DISPLAY_BAR_ONLY,
                onClick = { onProgressDisplayModeSelected(UserPreferences.PROGRESS_DISPLAY_BAR_ONLY) }
            )
            ChoiceRow(
                labelRes = R.string.settings_progress_hidden,
                selected = progressDisplayMode == UserPreferences.PROGRESS_DISPLAY_HIDDEN,
                onClick = { onProgressDisplayModeSelected(UserPreferences.PROGRESS_DISPLAY_HIDDEN) }
            )
        }

        SettingsSection(title = stringResource(R.string.settings_notifications)) {
            NavigationRow(
                title = stringResource(R.string.settings_notification_permission),
                summary = stringResource(
                    if (remindersEnabled) R.string.settings_notification_enabled
                    else R.string.settings_notification_disabled
                ),
                onClick = onOpenNotificationSettings
            )
            NavigationRow(
                title = stringResource(R.string.settings_date_reminder_time),
                summary = dateReminderTime,
                onClick = onSelectDateReminderTime
            )
            ReminderChoiceGroup(
                title = stringResource(R.string.settings_default_date_reminder),
                type = ItemType.Date,
                selectedOffset = defaultDateReminderOffset,
                onSelected = onDefaultDateReminderSelected
            )
            ReminderChoiceGroup(
                title = stringResource(R.string.settings_default_time_reminder),
                type = ItemType.Time,
                selectedOffset = defaultTimeReminderOffset,
                onSelected = onDefaultTimeReminderSelected
            )
        }

        SettingsSection(title = stringResource(R.string.settings_about)) {
            NavigationRow(
                title = stringResource(R.string.settings_privacy_policy),
                onClick = onOpenPrivacyPolicy
            )
            ValueRow(
                title = stringResource(R.string.settings_version),
                value = versionName
            )
        }
    }
}

@Composable
private fun PaletteChip(
    palette: ThemePalette,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(stringResource(palette.labelRes)) },
        leadingIcon = {
            Row {
                listOf(
                    palette.lightColors.primary,
                    palette.lightColors.secondary,
                    palette.lightColors.tertiary
                ).forEachIndexed { index, color ->
                    if (index > 0) Spacer(modifier = Modifier.width(2.dp))
                    Surface(
                        modifier = Modifier.size(9.dp),
                        shape = CircleShape,
                        color = color
                    ) {}
                }
            }
        }
    )
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun ReminderChoiceGroup(
    title: String,
    type: ItemType,
    selectedOffset: Int,
    onSelected: (Int) -> Unit,
) {
    Column(
        modifier = Modifier.padding(horizontal = Spacing.l, vertical = Spacing.m),
        verticalArrangement = Arrangement.spacedBy(Spacing.s)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = stringResource(R.string.settings_default_reminder_summary),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.s),
            verticalArrangement = Arrangement.spacedBy(Spacing.s)
        ) {
            ItemVisuals.reminderOffsets(type).forEach { offset ->
                FilterChip(
                    selected = selectedOffset == offset,
                    onClick = { onSelected(offset) },
                    label = { Text(stringResource(ItemVisuals.reminderNameRes(type, offset))) }
                )
            }
        }
    }
}

@Composable
fun PrivacyPolicyScreen(
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    SettingsScaffold(
        title = stringResource(R.string.settings_privacy_policy),
        onBack = onBack,
        scrollable = false
    ) {
        content()
    }
}

@Composable
private fun SettingsScaffold(
    title: String,
    onBack: () -> Unit,
    scrollable: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.s, vertical = Spacing.s),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    ) { innerPadding ->
        val modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .let {
                if (scrollable) it.verticalScroll(rememberScrollState()) else it
            }
            .padding(horizontal = if (scrollable) Spacing.xl else 0.dp, vertical = if (scrollable) Spacing.l else 0.dp)

        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(if (scrollable) Spacing.l else 0.dp),
            content = content
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = Spacing.xs)
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(content = content)
        }
    }
}

@Composable
private fun ChoiceRow(
    @StringRes labelRes: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.m, vertical = Spacing.s),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = Spacing.s)
        )
    }
}

@Composable
private fun NavigationRow(
    title: String,
    summary: String? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.l, vertical = Spacing.m),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (summary != null) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ValueRow(
    title: String,
    value: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.l, vertical = Spacing.m),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
