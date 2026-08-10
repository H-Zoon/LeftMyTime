package com.devidea.timeleft.activity

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.devidea.timeleft.ItemVisuals
import com.devidea.timeleft.R
import com.devidea.timeleft.database.itemdata.ItemEntity
import com.devidea.timeleft.database.itemdata.ItemType
import com.devidea.timeleft.notification.canPostReminderNotifications
import com.devidea.timeleft.notification.ReminderScheduler
import com.devidea.timeleft.preferences.UserPreferences
import com.devidea.timeleft.repository.TimeLeftRepository
import com.devidea.timeleft.ui.editor.ItemEditorDraft
import com.devidea.timeleft.ui.editor.ItemEditorScreen
import com.devidea.timeleft.ui.theme.TimeLeftTheme
import com.devidea.timeleft.widget.AppWidget
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class ItemEditorActivity : AppCompatActivity() {

    @Inject lateinit var repository: TimeLeftRepository
    @Inject lateinit var prefs: SharedPreferences

    private var initialItem by mutableStateOf<ItemEntity?>(null)
    private var isLoading by mutableStateOf(false)
    private var isSaving by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val itemId = intent.getIntExtra(EXTRA_ITEM_ID, 0)
        val initialType = intent.getStringExtra(EXTRA_ITEM_TYPE)
            ?.let { runCatching { ItemType.valueOf(it) }.getOrNull() }
            ?: ItemType.Time

        if (itemId != 0) {
            isLoading = true
            lifecycleScope.launch {
                runCatching {
                    withContext(Dispatchers.IO) { repository.getItem(itemId) }
                }.onSuccess {
                    initialItem = it
                    isLoading = false
                }.onFailure {
                    isLoading = false
                    Toast.makeText(
                        this@ItemEditorActivity,
                        getString(R.string.editor_error_load_failed),
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            }
        }

        setContent {
            TimeLeftTheme(
                themeMode = prefs.getString(UserPreferences.KEY_THEME, UserPreferences.THEME_AUTO)
                    ?: UserPreferences.THEME_AUTO,
                paletteKey = prefs.getString(
                    UserPreferences.KEY_COLOR_THEME,
                    UserPreferences.COLOR_THEME_INDIGO
                ) ?: UserPreferences.COLOR_THEME_INDIGO
            ) {
                ItemEditorScreen(
                    initialType = initialType,
                    initialItem = initialItem,
                    defaultDateReminderOffset = prefs.getInt(
                        UserPreferences.KEY_DEFAULT_DATE_REMINDER,
                        UserPreferences.DEFAULT_REMINDER_OFFSET
                    ),
                    defaultTimeReminderOffset = prefs.getInt(
                        UserPreferences.KEY_DEFAULT_TIME_REMINDER,
                        UserPreferences.DEFAULT_REMINDER_OFFSET
                    ),
                    isLoading = isLoading,
                    isSaving = isSaving,
                    onBack = { finish() },
                    onSave = { draft -> saveItem(itemId, draft) }
                )
            }
        }
    }

    private fun saveItem(itemId: Int, draft: ItemEditorDraft) {
        if (isSaving) return
        val safeDraft = if (draft.reminderOffsetDays != ItemVisuals.REMINDER_DISABLED &&
            !canPostReminderNotifications()
        ) {
            draft.copy(reminderOffsetDays = ItemVisuals.REMINDER_DISABLED)
        } else {
            draft
        }

        isSaving = true
        lifecycleScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    repository.saveOrUpdate(safeDraft.toEntity(itemId))
                }
            }.onSuccess { savedItem ->
                ReminderScheduler.schedule(this@ItemEditorActivity, savedItem)
                AppWidget.updateAllWidgets(
                    context = this@ItemEditorActivity,
                    appWidgetManager = AppWidgetManager.getInstance(this@ItemEditorActivity)
                )
                finish()
            }.onFailure {
                isSaving = false
                Toast.makeText(
                    this@ItemEditorActivity,
                    getString(R.string.editor_error_save_failed),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun ItemEditorDraft.toEntity(itemId: Int): ItemEntity =
        ItemEntity(
            id = itemId,
            type = type,
            title = title,
            startValue = startValue,
            endValue = endValue,
            updateFlag = updateFlag,
            updateRate = updateRate,
            category = category,
            colorKey = colorKey,
            iconKey = iconKey,
            reminderOffsetDays = reminderOffsetDays
        )

    companion object {
        private const val EXTRA_ITEM_ID = "com.devidea.timeleft.extra.ITEM_ID"
        private const val EXTRA_ITEM_TYPE = "com.devidea.timeleft.extra.ITEM_TYPE"

        fun createIntent(context: Context, type: ItemType): Intent =
            Intent(context, ItemEditorActivity::class.java)
                .putExtra(EXTRA_ITEM_TYPE, type.name)

        fun editIntent(context: Context, itemId: Int): Intent =
            Intent(context, ItemEditorActivity::class.java)
                .putExtra(EXTRA_ITEM_ID, itemId)
    }
}
