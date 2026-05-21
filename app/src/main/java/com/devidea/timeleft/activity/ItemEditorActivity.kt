package com.devidea.timeleft.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.devidea.timeleft.App
import com.devidea.timeleft.R
import com.devidea.timeleft.database.AppDatabase
import com.devidea.timeleft.database.itemdata.ItemEntity
import com.devidea.timeleft.database.itemdata.ItemType
import com.devidea.timeleft.repository.TimeLeftRepository
import com.devidea.timeleft.ui.editor.ItemEditorDraft
import com.devidea.timeleft.ui.editor.ItemEditorScreen
import com.devidea.timeleft.ui.theme.TimeLeftTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ItemEditorActivity : AppCompatActivity() {
    private val repository by lazy {
        TimeLeftRepository(AppDatabase.getDatabase(App.context()).itemDao())
    }

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
            TimeLeftTheme(themeMode = MainActivity.prefs.getString("theme", "auto") ?: "auto") {
                ItemEditorScreen(
                    initialType = initialType,
                    initialItem = initialItem,
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
                    repository.saveOrUpdate(draft.toEntity(itemId))
                }
            }.onSuccess {
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
            updateRate = updateRate
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
