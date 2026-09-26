package com.devidea.timeleft.periods

import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.devidea.timeleft.R
import com.devidea.timeleft.activity.ItemEditorActivity
import com.devidea.timeleft.database.itemdata.ItemType
import com.devidea.timeleft.preferences.UserPreferences
import com.devidea.timeleft.ui.components.TimeDetailContent
import com.devidea.timeleft.ui.components.TimeLeftTopAppBar
import com.devidea.timeleft.ui.settings.SettingsDropdownRow
import com.devidea.timeleft.ui.settings.SettingsOption
import com.devidea.timeleft.ui.theme.*
import com.devidea.timeleft.widget.PinWidgetButton
import com.devidea.timeleft.widget.WidgetSource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.time.LocalDate
import javax.inject.Inject

@AndroidEntryPoint
class PeriodsActivity : AppCompatActivity() {
    @Inject lateinit var prefs: SharedPreferences
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var choice by rememberSaveable { mutableStateOf(CalendarPeriod.Week.name) }
            val period = CalendarPeriod.valueOf(choice)
            var today by remember { mutableStateOf(LocalDate.now()) }
            LaunchedEffect(Unit) {
                lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    while (isActive) { today = LocalDate.now(); delay(1000) }
                }
            }
            val item = remember(period, today) { calendarPeriodItem(this@PeriodsActivity, period, today) }
            TimeLeftTheme(designKey = prefs.getString(UserPreferences.KEY_DESIGN, UserPreferences.DESIGN_TIME_FOCUS) ?: UserPreferences.DESIGN_TIME_FOCUS,
                themeMode = prefs.getString(UserPreferences.KEY_THEME, UserPreferences.THEME_AUTO) ?: UserPreferences.THEME_AUTO,
                paletteKey = prefs.getString(UserPreferences.KEY_COLOR_THEME, UserPreferences.COLOR_THEME_CLAY) ?: UserPreferences.COLOR_THEME_CLAY) {
                Scaffold(containerColor = MaterialTheme.colorScheme.background,
                    topBar = { TimeLeftTopAppBar(stringResource(R.string.period_more), ::finish) }) { padding ->
                    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(horizontal = LayoutTokens.ScreenHorizontal, vertical = Spacing.l),
                        verticalArrangement = Arrangement.spacedBy(Spacing.l)) {
                        item {
                            SettingsDropdownRow(title = stringResource(R.string.period_choose), selectedValue = choice,
                                options = CalendarPeriod.entries.map { SettingsOption(it.name, it.title) }, onSelected = { choice = it })
                            Text(stringResource(R.string.period_calendar_policy), style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        item { TimeDetailContent(item, prefs.getString(UserPreferences.KEY_PROGRESS_DISPLAY, UserPreferences.PROGRESS_DISPLAY_FULL)
                            ?: UserPreferences.PROGRESS_DISPLAY_FULL) }
                        item { PinWidgetButton(item, if (period == CalendarPeriod.Week) WidgetSource.Week else WidgetSource.Quarter,
                            contentPadding = PaddingValues(vertical = Spacing.s)) }
                        item {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Text(stringResource(R.string.period_custom_hint), Modifier.padding(top = Spacing.l), style = MaterialTheme.typography.bodyMedium)
                            TextButton(onClick = { startActivity(ItemEditorActivity.createIntent(this@PeriodsActivity, ItemType.Date)) },
                                modifier = Modifier.heightIn(min = LayoutTokens.MinTouchTarget), contentPadding = PaddingValues(vertical = Spacing.s)) {
                                Text(stringResource(R.string.period_custom_add))
                            }
                        }
                    }
                }
            }
        }
    }
}
