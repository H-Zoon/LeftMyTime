package com.devidea.timeleft.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.devidea.timeleft.AdapterItem
import com.devidea.timeleft.ItemVisuals
import com.devidea.timeleft.R
import com.devidea.timeleft.database.itemdata.ItemType
import com.devidea.timeleft.preferences.UserPreferences
import com.devidea.timeleft.ui.components.TimeLeftSection
import com.devidea.timeleft.ui.components.TimeLeftTopAppBar
import com.devidea.timeleft.ui.theme.LayoutTokens
import com.devidea.timeleft.ui.theme.Spacing

@Composable
fun SettingsScreen(
    themeMode: String,
    paletteKey: String,
    homeSort: String,
    expiredItemsMode: String,
    progressDisplayMode: String,
    defaultDateReminderOffset: Int,
    defaultTimeReminderOffset: Int,
    dateReminderTime: String,
    remindersEnabled: Boolean,
    versionName: String,
    onBack: () -> Unit,
    onOpenThemePreview: () -> Unit,
    onSortSelected: (String) -> Unit,
    onExpiredItemsModeSelected: (String) -> Unit,
    onProgressDisplayModeSelected: (String) -> Unit,
    onDefaultDateReminderSelected: (Int) -> Unit,
    onDefaultTimeReminderSelected: (Int) -> Unit,
    onSelectDateReminderTime: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    previewPeriods: List<AdapterItem> = emptyList(),
) {
    SettingsScaffold(
        title = stringResource(R.string.settings_title),
        onBack = onBack
    ) {
        SettingsSection(title = stringResource(R.string.settings_display)) {
            ThemePreviewEntry(themeMode, paletteKey, previewPeriods, progressDisplayMode, onOpenThemePreview)
        }

        SettingsSection(title = stringResource(R.string.settings_home_behavior)) {
            SettingsDropdownRow(
                title = stringResource(R.string.settings_default_sort),
                selectedValue = homeSort,
                options = listOf(
                    SettingsOption(UserPreferences.SORT_NEAREST, R.string.home_sort_nearest),
                    SettingsOption(UserPreferences.SORT_CREATED, R.string.home_sort_created),
                    SettingsOption(UserPreferences.SORT_TITLE, R.string.home_sort_title),
                    SettingsOption(UserPreferences.SORT_PROGRESS, R.string.home_sort_progress),
                ),
                onSelected = onSortSelected,
            )
            SettingsDropdownRow(
                title = stringResource(R.string.settings_expired_items),
                selectedValue = expiredItemsMode,
                options = listOf(
                    SettingsOption(UserPreferences.EXPIRED_ITEMS_SHOW, R.string.settings_expired_show),
                    SettingsOption(UserPreferences.EXPIRED_ITEMS_BOTTOM, R.string.settings_expired_bottom),
                    SettingsOption(UserPreferences.EXPIRED_ITEMS_HIDE, R.string.settings_expired_hide),
                ),
                onSelected = onExpiredItemsModeSelected,
            )
            SettingsSwitchRow(
                title = stringResource(R.string.settings_progress_display),
                checked = progressDisplayMode != UserPreferences.PROGRESS_DISPLAY_HIDDEN,
                onCheckedChange = { checked ->
                    onProgressDisplayModeSelected(if (checked) UserPreferences.PROGRESS_DISPLAY_FULL else UserPreferences.PROGRESS_DISPLAY_HIDDEN)
                },
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
            ReminderDropdownRow(
                title = stringResource(R.string.settings_default_date_reminder),
                type = ItemType.Date,
                selectedOffset = defaultDateReminderOffset,
                onSelected = onDefaultDateReminderSelected
            )
            ReminderDropdownRow(
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
private fun ReminderDropdownRow(
    title: String,
    type: ItemType,
    selectedOffset: Int,
    onSelected: (Int) -> Unit,
) {
    SettingsDropdownRow(
        title = title,
        summary = stringResource(R.string.settings_default_reminder_summary),
        selectedValue = selectedOffset,
        options = ItemVisuals.reminderOffsets(type).map { offset ->
            SettingsOption(offset, ItemVisuals.reminderNameRes(type, offset))
        },
        onSelected = onSelected,
    )
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
            TimeLeftTopAppBar(title = title, onBack = onBack)
        }
    ) { innerPadding ->
        val modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .let {
                if (scrollable) it.verticalScroll(rememberScrollState()) else it
            }
            .padding(horizontal = if (scrollable) LayoutTokens.ScreenHorizontal else 0.dp, vertical = if (scrollable) LayoutTokens.ScreenVertical else 0.dp)

        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(if (scrollable) LayoutTokens.SectionGap else 0.dp),
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
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = Spacing.xs)
        )
        TimeLeftSection(modifier = Modifier.fillMaxWidth()) {
            Column(content = content)
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = LayoutTokens.MinTouchTarget)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(vertical = Spacing.m),
        horizontalArrangement = Arrangement.spacedBy(Spacing.m),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Switch(checked = checked, onCheckedChange = null)
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
            .heightIn(min = LayoutTokens.MinTouchTarget)
            .clickable(onClick = onClick)
            .padding(vertical = Spacing.m),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs)
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
            .padding(vertical = Spacing.m),
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
