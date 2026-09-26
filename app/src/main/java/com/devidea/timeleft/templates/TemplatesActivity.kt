package com.devidea.timeleft.templates

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.devidea.timeleft.R
import com.devidea.timeleft.activity.ItemEditorActivity
import com.devidea.timeleft.preferences.UserPreferences
import com.devidea.timeleft.repository.TimeLeftRepository
import com.devidea.timeleft.ui.components.TimeLeftTopAppBar
import com.devidea.timeleft.ui.theme.LayoutTokens
import com.devidea.timeleft.ui.theme.Spacing
import com.devidea.timeleft.ui.theme.TimeLeftTheme
import com.devidea.timeleft.widget.widgetScheduleDescription
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

@AndroidEntryPoint
class TemplatesActivity : AppCompatActivity() {
    @Inject lateinit var repository: TimeLeftRepository
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        setContent {
            var retry by remember { mutableIntStateOf(0) }
            val load by produceState(TemplateLoad(), retry) {
                value = TemplateLoad()
                try { repository.templates.collect { value = TemplateLoad(it, loading = false) } }
                catch (error: CancellationException) { throw error }
                catch (_: Exception) { value = TemplateLoad(loading = false, failed = true) }
            }
            val templates = load.items
            var deleting by remember { mutableStateOf<Int?>(null) }
            TimeLeftTheme(designKey = prefs.getString(UserPreferences.KEY_DESIGN, UserPreferences.DESIGN_TIME_FOCUS) ?: UserPreferences.DESIGN_TIME_FOCUS,
                themeMode = prefs.getString(UserPreferences.KEY_THEME, UserPreferences.THEME_AUTO) ?: UserPreferences.THEME_AUTO,
                paletteKey = prefs.getString(UserPreferences.KEY_COLOR_THEME, UserPreferences.COLOR_THEME_CLAY) ?: UserPreferences.COLOR_THEME_CLAY) {
                Scaffold(containerColor = MaterialTheme.colorScheme.background,
                    topBar = { TimeLeftTopAppBar(stringResource(R.string.templates_title), ::finish) }) { padding ->
                    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(horizontal = LayoutTokens.ScreenHorizontal, vertical = Spacing.l),
                        verticalArrangement = Arrangement.spacedBy(Spacing.l)) {
                        item { Text(stringResource(R.string.templates_explanation), style = MaterialTheme.typography.bodyMedium) }
                        when {
                            load.loading -> item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
                            load.failed -> item {
                                Text(stringResource(R.string.home_load_failed))
                                TextButton(onClick = { retry++ }) { Text(stringResource(R.string.theme_preview_retry)) }
                            }
                            templates.isEmpty() -> item { Text(stringResource(R.string.templates_empty)) }
                        }
                        items(templates, key = { it.id }) { template ->
                            Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                Text(template.title, style = MaterialTheme.typography.titleMedium)
                                Text(widgetScheduleDescription(this@TemplatesActivity, template), style = MaterialTheme.typography.bodyMedium)
                                TextButton(onClick = { startActivity(ItemEditorActivity.duplicateIntent(this@TemplatesActivity, template.id, fromTemplate = true)) },
                                    modifier = Modifier.heightIn(min = LayoutTokens.MinTouchTarget)) { Text(stringResource(R.string.templates_use)) }
                                TextButton(onClick = { deleting = template.id }, modifier = Modifier.heightIn(min = LayoutTokens.MinTouchTarget)) {
                                    Text(stringResource(R.string.card_action_delete), color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
                deleting?.let { id -> AlertDialog(onDismissRequest = { deleting = null },
                    title = { Text(stringResource(R.string.template_delete_confirm)) },
                    text = { Text(stringResource(R.string.template_delete_message)) },
                    confirmButton = { TextButton(onClick = {
                        deleting = null
                        lifecycleScope.launch {
                            runCatching { repository.delete(id) }.onFailure { Toast.makeText(this@TemplatesActivity, R.string.home_action_failed, Toast.LENGTH_SHORT).show() }
                        }
                    }) { Text(stringResource(R.string.card_action_delete)) } },
                    dismissButton = { TextButton(onClick = { deleting = null }) { Text(stringResource(R.string.action_cancel)) } }) }
            }
        }
    }
}

private data class TemplateLoad(val items: List<com.devidea.timeleft.database.itemdata.ItemEntity> = emptyList(),
    val loading: Boolean = true, val failed: Boolean = false)
