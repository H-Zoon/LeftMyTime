package com.devidea.timeleft.activity

import android.content.*
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.preference.PreferenceManager
import com.devidea.timeleft.*
import com.devidea.timeleft.database.AppDatabase
import com.devidea.timeleft.database.itemdata.ItemType
import com.devidea.timeleft.ui.home.HomeScreen
import com.devidea.timeleft.ui.theme.TimeLeftTheme
import com.devidea.timeleft.viewmodels.TimeLeftViewModel
import com.devidea.timeleft.viewmodels.TimeLeftViewModelFactory
import com.devidea.timeleft.widget.AppWidget
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


class MainActivity : AppCompatActivity() {
    private lateinit var viewModel: TimeLeftViewModel

    companion object {
        val ITEM_GENERATE: InterfaceItem = ItemGenerate()
        val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(App.context())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(
            this,
            TimeLeftViewModelFactory(AppDatabase.getDatabase(App.context()).itemDao())
        )[TimeLeftViewModel::class.java]

        applyNightMode(currentThemeMode())

        setContent {
            val timeValue by viewModel.timeValue.collectAsStateWithLifecycle()
            val topItems by viewModel.topItems.collectAsStateWithLifecycle()
            val customItems by viewModel.customItems.collectAsStateWithLifecycle()
            var themeMode by remember { mutableStateOf(currentThemeMode()) }
            val dateText = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern(getString(R.string.pattern_header_date)))

            TimeLeftTheme(themeMode = themeMode) {
                HomeScreen(
                    dateText = dateText,
                    timeText = timeValue,
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
                        AppWidget().onDeleted(App.context(), intArrayOf(id))
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
        with(prefs.edit()) {
            putString("theme", nextMode)
        }.apply()
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
}
