package com.devidea.timeleft.activity

import android.content.Intent
import android.content.SharedPreferences
import android.app.TimePickerDialog
import android.appwidget.AppWidgetManager
import android.os.Bundle
import android.provider.Settings
import android.webkit.WebView
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.devidea.timeleft.BuildConfig
import com.devidea.timeleft.ItemVisuals
import com.devidea.timeleft.notification.canPostReminderNotifications
import com.devidea.timeleft.notification.ReminderScheduler
import com.devidea.timeleft.preferences.UserPreferences
import com.devidea.timeleft.repository.TimeLeftRepository
import com.devidea.timeleft.ui.settings.PrivacyPolicyScreen
import com.devidea.timeleft.ui.settings.SettingsScreen
import com.devidea.timeleft.ui.theme.TimeLeftTheme
import com.devidea.timeleft.widget.AppWidget
import dagger.hilt.android.AndroidEntryPoint
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@AndroidEntryPoint
class SettingsActivity : AppCompatActivity() {

    @Inject lateinit var prefs: SharedPreferences
    @Inject lateinit var repository: TimeLeftRepository

    private var themeMode by mutableStateOf(UserPreferences.THEME_AUTO)
    private var paletteKey by mutableStateOf(UserPreferences.COLOR_THEME_INDIGO)
    private var homeSort by mutableStateOf(UserPreferences.SORT_NEAREST)
    private var startScreen by mutableStateOf(UserPreferences.START_SCREEN_OVERVIEW)
    private var expiredItemsMode by mutableStateOf(UserPreferences.EXPIRED_ITEMS_SHOW)
    private var progressDisplayMode by mutableStateOf(UserPreferences.PROGRESS_DISPLAY_FULL)
    private var defaultDateReminderOffset by mutableStateOf(ItemVisuals.REMINDER_DISABLED)
    private var defaultTimeReminderOffset by mutableStateOf(ItemVisuals.REMINDER_DISABLED)
    private var dateReminderTime by mutableStateOf(UserPreferences.DEFAULT_DATE_REMINDER_TIME)
    private var remindersEnabled by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        refreshSettingsState()

        setContent {
            TimeLeftTheme(themeMode = themeMode, paletteKey = paletteKey) {
                SettingsScreen(
                    themeMode = themeMode,
                    paletteKey = paletteKey,
                    homeSort = homeSort,
                    startScreen = startScreen,
                    expiredItemsMode = expiredItemsMode,
                    progressDisplayMode = progressDisplayMode,
                    defaultDateReminderOffset = defaultDateReminderOffset,
                    defaultTimeReminderOffset = defaultTimeReminderOffset,
                    dateReminderTime = dateReminderTime,
                    remindersEnabled = remindersEnabled,
                    versionName = BuildConfig.VERSION_NAME,
                    onBack = { finish() },
                    onThemeSelected = ::selectTheme,
                    onPaletteSelected = ::selectPalette,
                    onSortSelected = ::selectSort,
                    onStartScreenSelected = ::selectStartScreen,
                    onExpiredItemsModeSelected = ::selectExpiredItemsMode,
                    onProgressDisplayModeSelected = ::selectProgressDisplayMode,
                    onDefaultDateReminderSelected = ::selectDefaultDateReminder,
                    onDefaultTimeReminderSelected = ::selectDefaultTimeReminder,
                    onSelectDateReminderTime = ::selectDateReminderTime,
                    onOpenNotificationSettings = ::openNotificationSettings,
                    onOpenPrivacyPolicy = {
                        startActivity(Intent(this, PrivacyPolicyActivity::class.java))
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::prefs.isInitialized) refreshSettingsState()
    }

    private fun refreshSettingsState() {
        themeMode = prefs.getString(UserPreferences.KEY_THEME, UserPreferences.THEME_AUTO)
            ?: UserPreferences.THEME_AUTO
        paletteKey = prefs.getString(
            UserPreferences.KEY_COLOR_THEME,
            UserPreferences.COLOR_THEME_INDIGO
        ) ?: UserPreferences.COLOR_THEME_INDIGO
        homeSort = prefs.getString(UserPreferences.KEY_HOME_SORT, UserPreferences.SORT_NEAREST)
            ?: UserPreferences.SORT_NEAREST
        startScreen = prefs.getString(
            UserPreferences.KEY_START_SCREEN,
            UserPreferences.START_SCREEN_OVERVIEW
        ) ?: UserPreferences.START_SCREEN_OVERVIEW
        expiredItemsMode = prefs.getString(
            UserPreferences.KEY_EXPIRED_ITEMS,
            UserPreferences.EXPIRED_ITEMS_SHOW
        ) ?: UserPreferences.EXPIRED_ITEMS_SHOW
        progressDisplayMode = prefs.getString(
            UserPreferences.KEY_PROGRESS_DISPLAY,
            UserPreferences.PROGRESS_DISPLAY_FULL
        ) ?: UserPreferences.PROGRESS_DISPLAY_FULL
        defaultDateReminderOffset = prefs.getInt(
            UserPreferences.KEY_DEFAULT_DATE_REMINDER,
            ItemVisuals.REMINDER_DISABLED
        )
        defaultTimeReminderOffset = prefs.getInt(
            UserPreferences.KEY_DEFAULT_TIME_REMINDER,
            ItemVisuals.REMINDER_DISABLED
        )
        dateReminderTime = prefs.getString(
            UserPreferences.KEY_DATE_REMINDER_TIME,
            UserPreferences.DEFAULT_DATE_REMINDER_TIME
        ) ?: UserPreferences.DEFAULT_DATE_REMINDER_TIME
        remindersEnabled = canPostReminderNotifications()
        applyNightMode(themeMode)
    }

    private fun selectTheme(value: String) {
        prefs.edit().putString(UserPreferences.KEY_THEME, value).apply()
        themeMode = value
        applyNightMode(value)
    }

    private fun selectPalette(value: String) {
        prefs.edit().putString(UserPreferences.KEY_COLOR_THEME, value).apply()
        paletteKey = value
        updateWidgets()
    }

    private fun updateWidgets() {
        val appWidgetManager = AppWidgetManager.getInstance(this)
        AppWidget.updateAllWidgets(this, appWidgetManager)
    }

    private fun selectSort(value: String) {
        prefs.edit().putString(UserPreferences.KEY_HOME_SORT, value).apply()
        homeSort = value
    }

    private fun selectStartScreen(value: String) {
        prefs.edit().putString(UserPreferences.KEY_START_SCREEN, value).apply()
        startScreen = value
    }

    private fun selectExpiredItemsMode(value: String) {
        prefs.edit().putString(UserPreferences.KEY_EXPIRED_ITEMS, value).apply()
        expiredItemsMode = value
    }

    private fun selectProgressDisplayMode(value: String) {
        prefs.edit().putString(UserPreferences.KEY_PROGRESS_DISPLAY, value).apply()
        progressDisplayMode = value
    }

    private fun selectDefaultDateReminder(value: Int) {
        prefs.edit().putInt(UserPreferences.KEY_DEFAULT_DATE_REMINDER, value).apply()
        defaultDateReminderOffset = value
    }

    private fun selectDefaultTimeReminder(value: Int) {
        prefs.edit().putInt(UserPreferences.KEY_DEFAULT_TIME_REMINDER, value).apply()
        defaultTimeReminderOffset = value
    }

    private fun selectDateReminderTime() {
        val initialTime = runCatching {
            LocalTime.parse(dateReminderTime, SETTINGS_TIME_FORMATTER)
        }.getOrDefault(LocalTime.of(9, 0))
        TimePickerDialog(
            this,
            { _, hour, minute ->
                dateReminderTime = LocalTime.of(hour, minute).format(SETTINGS_TIME_FORMATTER)
                prefs.edit()
                    .putString(UserPreferences.KEY_DATE_REMINDER_TIME, dateReminderTime)
                    .apply()
                rescheduleReminders()
            },
            initialTime.hour,
            initialTime.minute,
            true
        ).show()
    }

    private fun rescheduleReminders() {
        lifecycleScope.launch {
            val items = withContext(Dispatchers.IO) { repository.allItems() }
            ReminderScheduler.rescheduleAll(this@SettingsActivity, items)
        }
    }

    private fun openNotificationSettings() {
        startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        )
    }

    private fun applyNightMode(value: String) {
        AppCompatDelegate.setDefaultNightMode(
            when (value) {
                UserPreferences.THEME_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                UserPreferences.THEME_DARK -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
        )
    }

    companion object {
        private val SETTINGS_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm")
    }
}

@AndroidEntryPoint
class PrivacyPolicyActivity : AppCompatActivity() {

    @Inject lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TimeLeftTheme(
                themeMode = prefs.getString(UserPreferences.KEY_THEME, UserPreferences.THEME_AUTO)
                    ?: UserPreferences.THEME_AUTO,
                paletteKey = prefs.getString(
                    UserPreferences.KEY_COLOR_THEME,
                    UserPreferences.COLOR_THEME_INDIGO
                ) ?: UserPreferences.COLOR_THEME_INDIGO
            ) {
                val context = LocalContext.current
                val colors = MaterialTheme.colorScheme
                val html = remember(colors.background, colors.onBackground, colors.primary) {
                    context.assets.open("privacy.html").bufferedReader().use { reader ->
                        styledPrivacyHtml(
                            body = reader.readText(),
                            background = colors.background,
                            foreground = colors.onBackground,
                            accent = colors.primary
                        )
                    }
                }
                PrivacyPolicyScreen(
                    onBack = { finish() }
                ) {
                    AndroidView(
                        factory = { context ->
                            WebView(context).apply {
                                settings.javaScriptEnabled = false
                                setBackgroundColor(colors.background.toArgb())
                            }
                        },
                        update = { webView ->
                            webView.setBackgroundColor(colors.background.toArgb())
                            webView.loadDataWithBaseURL(
                                "file:///android_asset/",
                                html,
                                "text/html",
                                "UTF-8",
                                null
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }
            }
        }
    }
}

private fun styledPrivacyHtml(
    body: String,
    background: Color,
    foreground: Color,
    accent: Color,
): String = """
    <!doctype html>
    <html lang="ko">
    <head>
      <meta charset="utf-8">
      <meta name="viewport" content="width=device-width, initial-scale=1">
      <style>
        html, body {
          background: ${background.toCssColor()};
          color: ${foreground.toCssColor()};
          font-family: sans-serif;
          font-size: 15px;
          line-height: 1.65;
          margin: 0;
          padding: 12px 16px 24px;
        }
        a { color: ${accent.toCssColor()}; }
      </style>
    </head>
    <body>$body</body>
    </html>
""".trimIndent()

private fun Color.toCssColor(): String =
    String.format("#%06X", toArgb() and 0xFFFFFF)
