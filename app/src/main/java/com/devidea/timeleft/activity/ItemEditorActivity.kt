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
import com.devidea.timeleft.calc.currentOccurrence
import com.devidea.timeleft.focus.isFocusSession
import com.devidea.timeleft.R
import com.devidea.timeleft.database.itemdata.ItemEntity
import com.devidea.timeleft.database.itemdata.ItemType
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
    @Inject lateinit var telemetry: com.devidea.timeleft.telemetry.AppTelemetry

    private var initialItem by mutableStateOf<ItemEntity?>(null)
    private var isLoading by mutableStateOf(false)
    private var isSaving by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val itemId = intent.getIntExtra(EXTRA_ITEM_ID, 0)
        val duplicateId = intent.getIntExtra(EXTRA_DUPLICATE_ID, 0)
        if (savedInstanceState == null && itemId == 0)
            telemetry.record(com.devidea.timeleft.telemetry.UsageEvent.ScheduleCreateOpened)
        val loadId = if (itemId != 0) itemId else duplicateId
        val initialType = intent.getStringExtra(EXTRA_ITEM_TYPE)
            ?.let { runCatching { ItemType.valueOf(it) }.getOrNull() }
            ?: ItemType.Time

        if (loadId == 0 && intent.hasExtra(EXTRA_DRAFT_PHRASE)) {
            initialItem = (com.devidea.timeleft.input.SchedulePhraseParser.parse(intent.getStringExtra(EXTRA_DRAFT_PHRASE).orEmpty())
                as? com.devidea.timeleft.input.SchedulePhrase.Draft)?.item
        }

        if (loadId != 0) {
            isLoading = true
            lifecycleScope.launch {
                runCatching {
                    withContext(Dispatchers.IO) { repository.getItem(loadId) }
                }.onSuccess {
                    if (it.isFocusSession) {
                        startActivity(com.devidea.timeleft.focus.FocusActivity.intent(this@ItemEditorActivity, it.id))
                        finish()
                        return@onSuccess
                    }
                    val current = it.currentOccurrence()
                    initialItem = if (duplicateId != 0) {
                        val draft = current.copy(id = 0, stableId = "", isTemplate = false, isPinned = false, pinnedUntilMillis = null, calendarSourceKey = "")
                        if (intent.getBooleanExtra(EXTRA_FROM_TEMPLATE, false) && draft.type == ItemType.Date) {
                            val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-M-d")
                            val duration = java.time.temporal.ChronoUnit.DAYS.between(java.time.LocalDate.parse(draft.startValue, formatter), java.time.LocalDate.parse(draft.endValue, formatter))
                            val today = java.time.LocalDate.now()
                            draft.copy(startValue = today.toString(), endValue = today.plusDays(duration).toString())
                        } else draft
                    } else current
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
                designKey = prefs.getString(UserPreferences.KEY_DESIGN, UserPreferences.DESIGN_TIME_FOCUS) ?: UserPreferences.DESIGN_TIME_FOCUS,
                themeMode = prefs.getString(UserPreferences.KEY_THEME, UserPreferences.THEME_AUTO)
                    ?: UserPreferences.THEME_AUTO,
                paletteKey = prefs.getString(
                    UserPreferences.KEY_COLOR_THEME,
                    UserPreferences.COLOR_THEME_CLAY
                ) ?: UserPreferences.COLOR_THEME_CLAY
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
        isSaving = true
        lifecycleScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    repository.saveOrUpdate(draft.toEntity(itemId).copy(
                        occurrenceStartMillis = initialItem?.occurrenceStartMillis,
                        occurrenceEndMillis = initialItem?.occurrenceEndMillis,
                        calendarSourceKey = initialItem?.calendarSourceKey.orEmpty()))
                }
            }.onSuccess { savedItem ->
                if (itemId == 0) telemetry.record(com.devidea.timeleft.telemetry.UsageEvent.ScheduleCreated)
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
            reminderOffsetDays = reminderOffsetDays,
            weekdays = weekdays,
            endNextDay = endNextDay
        )

    companion object {
        private const val EXTRA_DRAFT_PHRASE = "draft_phrase"
        private const val EXTRA_DUPLICATE_ID = "duplicate_item_id"
        private const val EXTRA_FROM_TEMPLATE = "from_template"
        private const val EXTRA_ITEM_ID = "com.devidea.timeleft.extra.ITEM_ID"
        private const val EXTRA_ITEM_TYPE = "com.devidea.timeleft.extra.ITEM_TYPE"

        fun createIntent(context: Context, type: ItemType): Intent =
            Intent(context, ItemEditorActivity::class.java)
                .putExtra(EXTRA_ITEM_TYPE, type.name)

        fun duplicateIntent(context: Context, itemId: Int, fromTemplate: Boolean = false): Intent =
            Intent(context, ItemEditorActivity::class.java).putExtra(EXTRA_DUPLICATE_ID, itemId).putExtra(EXTRA_FROM_TEMPLATE, fromTemplate)

        fun draftIntent(context: Context, phrase: String): Intent =
            Intent(context, ItemEditorActivity::class.java).putExtra(EXTRA_DRAFT_PHRASE, phrase)

        fun editIntent(context: Context, itemId: Int): Intent =
            Intent(context, ItemEditorActivity::class.java)
                .putExtra(EXTRA_ITEM_ID, itemId)
    }
}
