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
import com.devidea.timeleft.datadase.AppDatabase
import com.devidea.timeleft.datadase.itemdata.ItemEntity
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
            ?: TimeLeftRepository.TYPE_TIME

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
                    Toast.makeText(this@ItemEditorActivity, "항목을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(this@ItemEditorActivity, "저장하지 못했습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun ItemEditorDraft.toEntity(itemId: Int): ItemEntity =
        ItemEntity(
            type = type,
            title = title,
            startValue = startValue,
            endValue = endValue,
            updateFlag = updateFlag,
            updateRate = updateRate
        ).also {
            it.id = itemId
        }

    companion object {
        private const val EXTRA_ITEM_ID = "com.devidea.timeleft.extra.ITEM_ID"
        private const val EXTRA_ITEM_TYPE = "com.devidea.timeleft.extra.ITEM_TYPE"

        fun createIntent(context: Context, type: String): Intent =
            Intent(context, ItemEditorActivity::class.java)
                .putExtra(EXTRA_ITEM_TYPE, type)

        fun editIntent(context: Context, itemId: Int): Intent =
            Intent(context, ItemEditorActivity::class.java)
                .putExtra(EXTRA_ITEM_ID, itemId)
    }
}
