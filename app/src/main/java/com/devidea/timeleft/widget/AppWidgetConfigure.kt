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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import com.devidea.timeleft.ui.components.TimeRuler
import com.devidea.timeleft.ui.components.TimeLeftSection
import com.devidea.timeleft.ui.theme.LayoutTokens
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.compose.ui.platform.LocalContext
import com.devidea.timeleft.ItemGenerate
import com.devidea.timeleft.AdapterItem
import com.devidea.timeleft.database.itemdata.ItemType
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
        prefs.getString(UserPreferences.KEY_COLOR_THEME, UserPreferences.COLOR_THEME_CLAY)
            ?: UserPreferences.COLOR_THEME_CLAY
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
    var showRemaining by rememberSaveable { mutableStateOf(true) }
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
    val context = LocalContext.current
    val generator = remember(context) { ItemGenerate(context) }
    fun previewCustom(entity: ItemEntity?): AdapterItem? = entity?.let {
        if (it.type == ItemType.Time) generator.customTimeItem(it) else generator.customMonthItem(it)
    }
    val previewItem = when (selectedSource) {
        WidgetSource.Today -> generator.timeItem()
        WidgetSource.Month -> generator.monthItem()
        WidgetSource.Year -> generator.yearItem()
        WidgetSource.Next -> previewCustom(NextCountdownSelector.select(items)) ?: generator.monthItem()
        WidgetSource.Custom -> previewCustom(items.firstOrNull { it.id == selectedItemId })
    }
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
                color = MaterialTheme.colorScheme.background
            ) {
                Column(
                    modifier = Modifier.padding(
                        start = LayoutTokens.ScreenHorizontal,
                        top = Spacing.l,
                        end = LayoutTokens.ScreenHorizontal,
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
                        item = previewItem,
                        showRemaining = showRemaining
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = LayoutTokens.ScreenHorizontal, vertical = Spacing.l),
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
                        modifier = Modifier.fillMaxWidth().toggleable(value = showRemaining, role = Role.Switch, onValueChange = onShowRemainingChanged).padding(horizontal = Spacing.m, vertical = Spacing.m),
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
                            onCheckedChange = null
                        )
                    }
                }
            }

            Button(
                onClick = onSave,
                enabled = saveEnabled,
                shape = MaterialTheme.shapes.small,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onBackground, contentColor = MaterialTheme.colorScheme.background),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = LayoutTokens.ScreenHorizontal, end = LayoutTokens.ScreenHorizontal, bottom = Spacing.l)
                    .heightIn(min = 52.dp)
            ) {
                Text(stringResource(R.string.action_save))
            }
        }
    }
}

@Composable
private fun WidgetPreviewBand(item: AdapterItem?, showRemaining: Boolean) {
    val context = LocalContext.current
    val data = item?.toWidgetData(context, showRemaining, item.type == ItemType.Time)
    TimeLeftSection(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = Spacing.m), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
            Text(stringResource(R.string.widget_configure_preview), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (data == null) Text(stringResource(R.string.widget_configure_no_items), style = MaterialTheme.typography.bodyMedium)
            else {
                Text(data.title, style = MaterialTheme.typography.titleMedium)
                Text(data.value, style = MaterialTheme.typography.displaySmall)
                Text(data.meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TimeRuler(item.percent, item.startLabel, item.endLabel)
            }
        }
    }
}

@Composable
private fun SelectableSurfaceRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    TimeLeftSection(
        modifier = Modifier.fillMaxWidth().heightIn(min = LayoutTokens.MinTouchTarget)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.m, vertical = Spacing.s),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selected,
                onClick = null
            )
            Spacer(modifier = Modifier.width(Spacing.xs))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
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
