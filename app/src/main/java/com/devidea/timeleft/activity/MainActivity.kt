package com.devidea.timeleft.activity

import com.devidea.timeleft.focus.FocusActivity
import com.devidea.timeleft.focus.FocusViewModel
import com.devidea.timeleft.R
import com.devidea.timeleft.TimeDetailRange
import com.devidea.timeleft.repository.TimeLeftRepository
import com.devidea.timeleft.templates.TemplatesActivity
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import android.content.SharedPreferences
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devidea.timeleft.database.itemdata.ItemType
import com.devidea.timeleft.preferences.UserPreferences
import com.devidea.timeleft.ui.home.HomeScreen
import com.devidea.timeleft.ui.launch.SplashExitTransition
import com.devidea.timeleft.ui.theme.TimeLeftTheme
import com.devidea.timeleft.viewmodels.TimeLeftViewModel
import com.devidea.timeleft.widget.AppWidget
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject lateinit var repository: TimeLeftRepository
    @Inject lateinit var prefs: SharedPreferences

    private val focusModel: FocusViewModel by viewModels()
    private val viewModel: TimeLeftViewModel by viewModels()
    private var themeMode by mutableStateOf(UserPreferences.THEME_AUTO)
    private var paletteKey by mutableStateOf(UserPreferences.COLOR_THEME_CLAY)
    private var designKey by mutableStateOf(UserPreferences.DESIGN_TIME_FOCUS)
    private var homeSort by mutableStateOf(UserPreferences.SORT_NEAREST)
    private var homeLayout by mutableStateOf(UserPreferences.HOME_LAYOUT_LIST)
    private var expiredItemsMode by mutableStateOf(UserPreferences.EXPIRED_ITEMS_SHOW)
    private var progressDisplayMode by mutableStateOf(UserPreferences.PROGRESS_DISPLAY_FULL)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        splashScreen.setOnExitAnimationListener { provider ->
            SplashExitTransition(this, provider).start()
        }

        refreshPreferences()

        setContent {
            val topItems by viewModel.topItems.collectAsStateWithLifecycle()
            val focusState by focusModel.state.collectAsStateWithLifecycle()
            val homeState by viewModel.homeState.collectAsStateWithLifecycle()

            TimeLeftTheme(themeMode = themeMode, paletteKey = paletteKey, designKey = designKey) {
                HomeScreen(
                    initialSortValue = homeSort,
                    initialLayoutValue = homeLayout,
                    expiredItemsMode = expiredItemsMode,
                    progressDisplayMode = progressDisplayMode,
                    topItems = topItems,
                    customItems = homeState.items,
                    loading = homeState.loading,
                    loadFailed = homeState.failed,
                    invalidCount = homeState.invalidCount,
                    actionFailed = homeState.actionFailed,
                    onRetry = viewModel::retryLoading,
                    onStartFocus = { minutes -> focusModel.start(minutes) { id -> startActivity(FocusActivity.intent(this@MainActivity, id)) } },
                    onOpenFocus = { id -> startActivity(FocusActivity.intent(this@MainActivity, id)) },
                    focusBusy = focusState.busy, focusError = focusState.error,
                    onDuplicate = { id -> startActivity(ItemEditorActivity.duplicateIntent(this@MainActivity, id)) },
                    onSaveTemplate = { id -> changeSchedule(success = R.string.template_saved) { repository.saveTemplate(id) } },
                    onOpenTemplates = { startActivity(Intent(this@MainActivity, TemplatesActivity::class.java)) },
                    onOpenPhrase = { startActivity(Intent(this@MainActivity, com.devidea.timeleft.input.PhraseInputActivity::class.java)) },
                    onOpenCalendar = { startActivity(Intent(this@MainActivity, com.devidea.timeleft.calendar.CalendarImportActivity::class.java)) },
                    onMorePeriods = { startActivity(Intent(this@MainActivity, com.devidea.timeleft.periods.PeriodsActivity::class.java)) },
                    onPin = { item ->
                        val until = item.endsAtMillis ?: (item.detailFacts?.range as? TimeDetailRange.Calendar)?.end
                            ?.plusDays(1)?.atStartOfDay(java.time.ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
                        changeSchedule { repository.pin(item.id, until) }
                    },
                    onMove = { id, delta ->
                        changeSchedule(after = {
                            homeSort = UserPreferences.SORT_MANUAL
                            prefs.edit().putString(UserPreferences.KEY_HOME_SORT, homeSort).apply()
                        }) { repository.move(id, delta) }
                    },
                    onOpenSettings = {
                        startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
                    },
                    onSortChange = { value ->
                        prefs.edit().putString(UserPreferences.KEY_HOME_SORT, value).apply()
                        homeSort = value
                    },
                    onLayoutChange = { value ->
                        prefs.edit().putString(UserPreferences.KEY_HOME_LAYOUT, value).apply()
                        homeLayout = value
                    },
                    onAddTime = {
                        startActivity(
                            ItemEditorActivity.createIntent(this@MainActivity, ItemType.Time)
                        )
                    },
                    onAddDate = {
                        startActivity(
                            ItemEditorActivity.createIntent(this@MainActivity, ItemType.Date)
                        )
                    },
                    onEditItem = { id ->
                        if (homeState.items.any { it.id == id && it.isFocusSession }) startActivity(FocusActivity.intent(this@MainActivity, id))
                        else startActivity(ItemEditorActivity.editIntent(this@MainActivity, id))
                    },
                    onDeleteItem = viewModel::deleteItem
                )
            }
        }
    }

    private fun changeSchedule(success: Int? = null, after: () -> Unit = {}, action: suspend () -> Unit) {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) { action() }
                after()
                success?.let { Toast.makeText(this@MainActivity, it, Toast.LENGTH_SHORT).show() }
                AppWidget.updateAllWidgets(this@MainActivity, android.appwidget.AppWidgetManager.getInstance(this@MainActivity))
            } catch (exception: CancellationException) { throw exception }
            catch (_: Exception) { Toast.makeText(this@MainActivity, R.string.home_action_failed, Toast.LENGTH_SHORT).show() }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::prefs.isInitialized) refreshPreferences()
        AppWidget.updateAllWidgets(this, android.appwidget.AppWidgetManager.getInstance(this))
    }

    private fun refreshPreferences() {
        themeMode = currentThemeMode()
        designKey = prefs.getString(UserPreferences.KEY_DESIGN, UserPreferences.DESIGN_TIME_FOCUS) ?: UserPreferences.DESIGN_TIME_FOCUS
        paletteKey = prefs.getString(
            UserPreferences.KEY_COLOR_THEME,
            UserPreferences.COLOR_THEME_CLAY
        ) ?: UserPreferences.COLOR_THEME_CLAY
        homeSort = prefs.getString(UserPreferences.KEY_HOME_SORT, UserPreferences.SORT_NEAREST)
            ?: UserPreferences.SORT_NEAREST
        homeLayout = prefs.getString(
            UserPreferences.KEY_HOME_LAYOUT,
            UserPreferences.HOME_LAYOUT_LIST
        ) ?: UserPreferences.HOME_LAYOUT_LIST
        expiredItemsMode = prefs.getString(
            UserPreferences.KEY_EXPIRED_ITEMS,
            UserPreferences.EXPIRED_ITEMS_SHOW
        ) ?: UserPreferences.EXPIRED_ITEMS_SHOW
        progressDisplayMode = prefs.getString(
            UserPreferences.KEY_PROGRESS_DISPLAY,
            UserPreferences.PROGRESS_DISPLAY_FULL
        ) ?: UserPreferences.PROGRESS_DISPLAY_FULL
        applyNightMode(themeMode)
    }

    private fun applyNightMode(themeMode: String) {
        when (themeMode) {
            UserPreferences.THEME_LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            UserPreferences.THEME_DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }

    private fun currentThemeMode(): String =
        prefs.getString(UserPreferences.KEY_THEME, UserPreferences.THEME_AUTO)
            ?: UserPreferences.THEME_AUTO
}
