package com.devidea.timeleft.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.devidea.timeleft.R
import com.devidea.timeleft.database.itemdata.ItemEntity
import com.devidea.timeleft.repository.TimeLeftRepository
import com.devidea.timeleft.ui.theme.Spacing
import com.devidea.timeleft.ui.theme.TimeLeftTheme
import com.devidea.timeleft.preferences.UserPreferences
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class AppWidgetConfigure : AppCompatActivity() {

    @Inject lateinit var repository: TimeLeftRepository
    @Inject lateinit var prefs: SharedPreferences

    private var widgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setResult(RESULT_CANCELED)

        widgetId = intent.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContent {
            TimeLeftTheme(themeMode = currentThemeMode(), paletteKey = currentPaletteKey()) {
                WidgetConfigureRoute(
                    loadItems = { repository.allItems() },
                    onSave = ::saveWidgetConfiguration
                )
            }
        }
    }

    private fun saveWidgetConfiguration(
        source: WidgetSource,
        selectedItemId: Int?,
        showRemaining: Boolean,
    ) {
        val appWidgetManager = AppWidgetManager.getInstance(this)

        if (source == WidgetSource.Custom) {
            val itemId = selectedItemId
            if (itemId == null) {
                showSelectionToast()
                return
            }

            lifecycleScope.launch {
                val exists = withContext(Dispatchers.IO) {
                    runCatching { repository.getItem(itemId) }.isSuccess
                }
                if (!exists) {
                    showSelectionToast()
                    return@launch
                }
                persistAndFinish(itemId.toString(), showRemaining, appWidgetManager)
            }
        } else {
            persistAndFinish(source.prefValue, showRemaining, appWidgetManager)
        }
    }

    private fun persistAndFinish(
        value: String,
        showRemaining: Boolean,
        appWidgetManager: AppWidgetManager,
    ) {
        prefs.edit()
            .putString(widgetId.toString(), value)
            .putBoolean("${widgetId}option", showRemaining)
            .apply()

        val resultValue = Intent()
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        setResult(RESULT_OK, resultValue)
        AppWidget().updateAppWidget(this, appWidgetManager, widgetId)
        finish()
    }

    private fun showSelectionToast() {
        Toast.makeText(
            this,
            getString(R.string.widget_configure_select_required),
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun currentThemeMode(): String =
        prefs.getString(UserPreferences.KEY_THEME, UserPreferences.THEME_AUTO)
            ?: UserPreferences.THEME_AUTO

    private fun currentPaletteKey(): String =
        prefs.getString(UserPreferences.KEY_COLOR_THEME, UserPreferences.COLOR_THEME_INDIGO)
            ?: UserPreferences.COLOR_THEME_INDIGO
}

@Composable
private fun WidgetConfigureRoute(
    loadItems: suspend () -> List<ItemEntity>,
    onSave: (WidgetSource, Int?, Boolean) -> Unit,
) {
    var items by remember { mutableStateOf<List<ItemEntity>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var selectedSourceValue by rememberSaveable { mutableStateOf(WidgetSource.Today.prefValue) }
    var selectedItemId by rememberSaveable { mutableStateOf<Int?>(null) }
    var showRemaining by rememberSaveable { mutableStateOf(false) }
    val selectedSource = WidgetSource.fromPrefValue(selectedSourceValue)

    LaunchedEffect(Unit) {
        items = withContext(Dispatchers.IO) { loadItems() }
        loading = false
    }

    LaunchedEffect(selectedSource, items) {
        if (selectedSource == WidgetSource.Custom && items.none { it.id == selectedItemId }) {
            selectedItemId = items.firstOrNull()?.id
        }
    }

    WidgetConfigureScreen(
        loading = loading,
        items = items,
        selectedSource = selectedSource,
        selectedItemId = selectedItemId,
        showRemaining = showRemaining,
        onSourceSelected = { selectedSourceValue = it.prefValue },
        onItemSelected = { selectedItemId = it },
        onShowRemainingChanged = { showRemaining = it },
        onSave = { onSave(selectedSource, selectedItemId, showRemaining) }
    )
}

@Composable
private fun WidgetConfigureScreen(
    loading: Boolean,
    items: List<ItemEntity>,
    selectedSource: WidgetSource,
    selectedItemId: Int?,
    showRemaining: Boolean,
    onSourceSelected: (WidgetSource) -> Unit,
    onItemSelected: (Int) -> Unit,
    onShowRemainingChanged: (Boolean) -> Unit,
    onSave: () -> Unit,
) {
    val scrollState = rememberScrollState()
    val selectedItemTitle = items.firstOrNull { it.id == selectedItemId }?.title
    val saveEnabled = selectedSource != WidgetSource.Custom || selectedItemTitle != null

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(
                        start = 20.dp,
                        top = 20.dp,
                        end = 20.dp,
                        bottom = 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(Spacing.m)
                ) {
                    Text(
                        text = stringResource(R.string.widget_configure_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    WidgetPreviewBand(
                        source = selectedSource,
                        selectedItemTitle = selectedItemTitle,
                        showRemaining = showRemaining
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = Spacing.xl, vertical = Spacing.l),
                verticalArrangement = Arrangement.spacedBy(Spacing.m)
            ) {
                Text(
                    text = stringResource(R.string.widget_configure_source_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                WidgetSource.values().forEach { source ->
                    SelectableSurfaceRow(
                        label = stringResource(source.labelRes),
                        selected = source == selectedSource,
                        onClick = { onSourceSelected(source) }
                    )
                }

                AnimatedVisibility(visible = selectedSource == WidgetSource.Custom) {
                    CustomItemSection(
                        loading = loading,
                        items = items,
                        selectedItemId = selectedItemId,
                        onItemSelected = onItemSelected
                    )
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = Spacing.m, vertical = Spacing.m),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.widget_configure_remaining_option),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = showRemaining,
                            onCheckedChange = onShowRemainingChanged
                        )
                    }
                }
            }

            Button(
                onClick = onSave,
                enabled = saveEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = Spacing.xl, end = Spacing.xl, bottom = Spacing.l)
            ) {
                Text(stringResource(R.string.action_save))
            }
        }
    }
}

@Composable
private fun WidgetPreviewBand(
    source: WidgetSource,
    selectedItemTitle: String?,
    showRemaining: Boolean,
) {
    val title = selectedItemTitle ?: stringResource(source.labelRes)
    val value = if (showRemaining) {
        if (source == WidgetSource.Today) {
            stringResource(R.string.home_time_left, "12:34")
        } else {
            stringResource(R.string.home_days_left, 12)
        }
    } else {
        "68%"
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
    ) {
        Column(
            modifier = Modifier.padding(Spacing.m),
            verticalArrangement = Arrangement.spacedBy(Spacing.s)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.widget_configure_preview),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            LinearProgressIndicator(
                progress = { 0.68f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)
            )
        }
    }
}

@Composable
private fun SelectableSurfaceRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.small,
        color = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        tonalElevation = if (selected) 1.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.m, vertical = Spacing.s),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selected,
                onClick = onClick
            )
            Spacer(modifier = Modifier.width(Spacing.xs))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun CustomItemSection(
    loading: Boolean,
    items: List<ItemEntity>,
    selectedItemId: Int?,
    onItemSelected: (Int) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.s)
    ) {
        Text(
            text = stringResource(R.string.widget_configure_custom_item_title),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        when {
            loading -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Spacing.m),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            }
            items.isEmpty() -> {
                Text(
                    text = stringResource(R.string.widget_configure_no_items),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = Spacing.s)
                )
            }
            else -> {
                items.forEach { item ->
                    SelectableSurfaceRow(
                        label = item.title,
                        selected = item.id == selectedItemId,
                        onClick = { onItemSelected(item.id) }
                    )
                }
            }
        }
    }
}


private enum class WidgetSource(
    val prefValue: String,
    val labelRes: Int,
) {
    Today("embedTime", R.string.widget_configure_today),
    Month("embedMonth", R.string.widget_configure_month),
    Year("embedYear", R.string.widget_configure_year),
    Next("nextCustom", R.string.widget_configure_next),
    Custom("custom", R.string.widget_configure_custom);

    companion object {
        fun fromPrefValue(value: String): WidgetSource =
            values().firstOrNull { it.prefValue == value } ?: Today
    }
}
