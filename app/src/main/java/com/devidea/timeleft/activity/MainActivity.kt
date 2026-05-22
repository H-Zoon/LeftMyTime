package com.devidea.timeleft.activity

import android.Manifest
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devidea.timeleft.database.itemdata.ItemType
import com.devidea.timeleft.notification.ReminderScheduler
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        applyNightMode(currentThemeMode())
        ReminderScheduler.createChannel(this)
        requestNotificationPermissionIfNeeded()

        setContent {
            val topItems by viewModel.topItems.collectAsStateWithLifecycle()
            val customItems by viewModel.customItems.collectAsStateWithLifecycle()
            var themeMode by remember { mutableStateOf(currentThemeMode()) }

            TimeLeftTheme(themeMode = themeMode) {
                HomeScreen(
                    themeMode = themeMode,
                    topItems = topItems,
                    customItems = customItems,
                    onToggleTheme = {
                        themeMode = nightModeChanger()
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

    private fun nightModeChanger(): String {
        val nextMode = when (currentThemeMode()) {
            "light" -> "dark"
            "dark" -> "auto"
            else -> "light"
        }
        prefs.edit().putString("theme", nextMode).apply()
        applyNightMode(nextMode)
        return nextMode
    }

    private fun applyNightMode(themeMode: String) {
        when (themeMode) {
            "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }

    private fun currentThemeMode(): String = prefs.getString("theme", "auto") ?: "auto"

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) return

        requestPermissions(
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            REQUEST_POST_NOTIFICATIONS
        )
    }

    companion object {
        private const val REQUEST_POST_NOTIFICATIONS = 3001
    }
}
