package com.devidea.timeleft.activity

import android.content.SharedPreferences
import android.content.Intent
import android.os.Bundle
import android.view.animation.PathInterpolator
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
import com.devidea.timeleft.notification.ReminderScheduler
import com.devidea.timeleft.preferences.UserPreferences
import com.devidea.timeleft.ui.home.HomeScreen
import com.devidea.timeleft.ui.theme.TimeLeftTheme
import com.devidea.timeleft.viewmodels.TimeLeftViewModel
import com.devidea.timeleft.widget.AppWidget
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject lateinit var prefs: SharedPreferences

    private val viewModel: TimeLeftViewModel by viewModels()
    private var themeMode by mutableStateOf(UserPreferences.THEME_AUTO)
    private var paletteKey by mutableStateOf(UserPreferences.COLOR_THEME_INDIGO)
    private var homeSort by mutableStateOf(UserPreferences.SORT_NEAREST)
    private var startScreen by mutableStateOf(UserPreferences.START_SCREEN_OVERVIEW)
    private var expiredItemsMode by mutableStateOf(UserPreferences.EXPIRED_ITEMS_SHOW)
    private var progressDisplayMode by mutableStateOf(UserPreferences.PROGRESS_DISPLAY_FULL)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        splashScreen.setOnExitAnimationListener { provider ->
            provider.view.animate()
                .alpha(0f)
                .scaleX(1.04f)
                .scaleY(1.04f)
                .translationY(-provider.view.height * 0.025f)
                .setDuration(230L)
                .setInterpolator(PathInterpolator(0.2f, 0f, 0f, 1f))
                .withEndAction(provider::remove)
                .start()
        }

        refreshPreferences()

        setContent {
            val topItems by viewModel.topItems.collectAsStateWithLifecycle()
            val customItems by viewModel.customItems.collectAsStateWithLifecycle()

            TimeLeftTheme(themeMode = themeMode, paletteKey = paletteKey) {
                HomeScreen(
                    themeMode = themeMode,
                    initialSortValue = homeSort,
                    initialTabValue = startScreen,
                    expiredItemsMode = expiredItemsMode,
                    progressDisplayMode = progressDisplayMode,
                    animateStartupEntry = savedInstanceState == null,
                    topItems = topItems,
                    customItems = customItems,
                    onToggleTheme = {
                        themeMode = nightModeChanger()
                    },
                    onOpenSettings = {
                        startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
                    },
                    onSortChange = { value ->
                        prefs.edit().putString(UserPreferences.KEY_HOME_SORT, value).apply()
                        homeSort = value
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
                        startActivity(ItemEditorActivity.editIntent(this@MainActivity, id))
                    },
                    onDeleteItem = { id ->
                        viewModel.deleteItem(id)
                        ReminderScheduler.cancel(this@MainActivity, id)
                        AppWidget().onDeleted(this@MainActivity, intArrayOf(id))
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::prefs.isInitialized) refreshPreferences()
    }

    private fun nightModeChanger(): String {
        val nextMode = when (currentThemeMode()) {
            UserPreferences.THEME_LIGHT -> UserPreferences.THEME_DARK
            UserPreferences.THEME_DARK -> UserPreferences.THEME_AUTO
            else -> UserPreferences.THEME_LIGHT
        }
        prefs.edit().putString(UserPreferences.KEY_THEME, nextMode).apply()
        applyNightMode(nextMode)
        return nextMode
    }

    private fun refreshPreferences() {
        themeMode = currentThemeMode()
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
