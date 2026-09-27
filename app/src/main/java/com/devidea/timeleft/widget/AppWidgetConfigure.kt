package com.devidea.timeleft.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.devidea.timeleft.AdapterItem
import com.devidea.timeleft.ItemGenerate
import com.devidea.timeleft.R
import com.devidea.timeleft.database.itemdata.ItemEntity
import com.devidea.timeleft.database.itemdata.ItemType
import com.devidea.timeleft.preferences.UserPreferences
import com.devidea.timeleft.repository.TimeLeftRepository
import com.devidea.timeleft.ui.components.TimeLeftTopAppBar
import com.devidea.timeleft.ui.theme.LayoutTokens
import com.devidea.timeleft.ui.theme.Spacing
import com.devidea.timeleft.ui.theme.TimeLeftTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class AppWidgetConfigure : AppCompatActivity() {

    @Inject lateinit var repository: TimeLeftRepository
    @Inject lateinit var prefs: SharedPreferences
    @Inject lateinit var telemetry: com.devidea.timeleft.telemetry.AppTelemetry

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
        setResult(RESULT_CANCELED, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))

        val manager = AppWidgetManager.getInstance(this)
        val isEditing = WidgetConfiguration.hasSavedSettings(prefs, widgetId)
        val initial = WidgetConfiguration.read(
            prefs, widgetId, forConfiguration = true,
            defaultSource = WidgetSource.defaultForProvider(manager.getAppWidgetInfo(widgetId)?.provider?.className),
        )
        setContent {
            TimeLeftTheme(themeMode = currentThemeMode(), paletteKey = currentPaletteKey(),
                designKey = prefs.getString(UserPreferences.KEY_DESIGN, UserPreferences.DESIGN_TIME_FOCUS) ?: UserPreferences.DESIGN_TIME_FOCUS) {
                WidgetConfigureRoute(
                    initial = initial,
                    isEditing = isEditing,
                    dimensions = WidgetDimensions.fromOptions(AppWidgetManager.getInstance(this).getAppWidgetOptions(widgetId), initial.source),
                    showProgress = prefs.getString(UserPreferences.KEY_PROGRESS_DISPLAY, UserPreferences.PROGRESS_DISPLAY_FULL) != UserPreferences.PROGRESS_DISPLAY_HIDDEN,
                    paletteKey = currentPaletteKey(),
                    themeMode = currentThemeMode(),
                    loadItems = { repository.allItems().map { it.forWidgetPreview() } },
                    onBack = ::finish,
                    onSave = ::saveWidgetConfiguration
                )
            }
        }
    }

    private suspend fun saveWidgetConfiguration(configuration: WidgetConfiguration): WidgetSaveResult {
        if (configuration.source == WidgetSource.Custom) {
            val itemId = configuration.itemId ?: return WidgetSaveResult.MissingItem
            try {
                val exists = withContext(Dispatchers.IO) { repository.allItems().any { it.id == itemId } }
                if (!exists) return WidgetSaveResult.MissingItem
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                return WidgetSaveResult.LoadFailed
            }
        }
        persistAndFinish(configuration, AppWidgetManager.getInstance(this))
        return WidgetSaveResult.Saved
    }

    private fun persistAndFinish(configuration: WidgetConfiguration, appWidgetManager: AppWidgetManager) {
        configuration.write(prefs, widgetId)
        telemetry.record(com.devidea.timeleft.telemetry.UsageEvent.WidgetConfigured)
        setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
        AppWidget().updateAppWidget(this, appWidgetManager, widgetId)
        finish()
    }

    private fun currentThemeMode(): String =
        prefs.getString(UserPreferences.KEY_THEME, UserPreferences.THEME_AUTO)
            ?: UserPreferences.THEME_AUTO

    private fun currentPaletteKey(): String =
        prefs.getString(UserPreferences.KEY_COLOR_THEME, UserPreferences.COLOR_THEME_CLAY)
            ?: UserPreferences.COLOR_THEME_CLAY
}

internal enum class WidgetSaveResult { Saved, MissingItem, LoadFailed }

/** Also used by the debug gallery, so selection and preview never become separate mock screens. */
@Composable
internal fun WidgetConfigureRoute(
    initial: WidgetConfiguration,
    dimensions: WidgetDimensions,
    paletteKey: String,
    themeMode: String,
    loadItems: suspend () -> List<ItemEntity>,
    onBack: () -> Unit,
    onSave: suspend (WidgetConfiguration) -> WidgetSaveResult,
    isEditing: Boolean = false,
    showProgress: Boolean = true,
) {
    var items by remember { mutableStateOf<List<ItemEntity>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var loadFailed by remember { mutableStateOf(false) }
    var reload by remember { mutableStateOf(0) }
    var saving by remember { mutableStateOf(false) }
    var sourceValue by rememberSaveable { mutableStateOf(initial.source.prefValue) }
    var selectedItemId by rememberSaveable { mutableStateOf(initial.itemId) }
    var showRemaining by rememberSaveable { mutableStateOf(initial.showRemaining) }
    var legacySummary by rememberSaveable { mutableStateOf(initial.legacySummary) }
    var showSeconds by rememberSaveable { mutableStateOf(initial.showSeconds) }
    var selectionPage by rememberSaveable { mutableStateOf<String?>(null) }
    var returnToSources by rememberSaveable { mutableStateOf(false) }
    var returnFocusTo by rememberSaveable { mutableStateOf<String?>(null) }
    val sourceFocus = remember { FocusRequester() }
    val itemFocus = remember { FocusRequester() }
    val scope = rememberCoroutineScope()
    val source = WidgetSource.fromPrefValue(sourceValue)
    LaunchedEffect(selectionPage) {
        if (selectionPage == null && returnFocusTo != null) {
            withFrameNanos { }
            if (returnFocusTo == "item") itemFocus.requestFocus() else sourceFocus.requestFocus()
            returnFocusTo = null
        }
    }
    LaunchedEffect(reload) {
        loading = true
        loadFailed = false
        try {
            items = withContext(Dispatchers.IO) { loadItems() }
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            loadFailed = true
        } finally {
            loading = false
        }
    }
    fun selectSource(next: WidgetSource) {
        if (next != source) legacySummary = false
        sourceValue = next.prefValue
    }
    fun closeSelection() {
        selectionPage = if (selectionPage == "items" && returnToSources) "sources" else null
    }
    BackHandler(enabled = selectionPage != null || saving) {
        if (!saving) closeSelection()
    }
    if (selectionPage != null) {
        WidgetSelectionScreen(
            pickingItems = selectionPage == "items", source = source, selectedItemId = selectedItemId,
            items = items, loading = loading, loadFailed = loadFailed,
            onBack = ::closeSelection, onRetry = { reload++ },
            onSourceSelected = { next ->
                if (next == WidgetSource.Custom) {
                    returnToSources = true
                    selectionPage = "items"
                } else {
                    selectSource(next)
                    selectionPage = null
                }
            },
            onItemSelected = { id ->
                selectSource(WidgetSource.Custom)
                selectedItemId = id
                selectionPage = null
            },
        )
        return
    }
    val configuration = WidgetConfiguration(source, selectedItemId, showRemaining,
        legacySummary && source != WidgetSource.Overview, showSeconds)
    WidgetConfigureScreen(
        configuration = configuration, dimensions = dimensions, paletteKey = paletteKey, showProgress = showProgress,
        dark = when (themeMode) {
            UserPreferences.THEME_DARK -> true
            UserPreferences.THEME_LIGHT -> false
            else -> isSystemInDarkTheme()
        },
        items = items, loading = loading, loadFailed = loadFailed, saving = saving, isEditing = isEditing,
        canKeepLegacySummary = initial.legacySummary,
        onBack = { if (!saving) onBack() },
        sourceFocus = sourceFocus, itemFocus = itemFocus,
        onChangeSource = { returnFocusTo = "source"; selectionPage = "sources" },
        onChangeItem = { returnFocusTo = "item"; returnToSources = false; selectionPage = "items" },
        onRetry = { reload++ },
        onShowRemainingChanged = { showRemaining = it },
        onLegacySummaryChanged = { legacySummary = it },
        onShowSecondsChanged = { showSeconds = it },
        onSave = {
            if (!saving) {
                saving = true
                scope.launch {
                    var saved = false
                    try {
                        when (onSave(configuration)) {
                            WidgetSaveResult.Saved -> saved = true
                            WidgetSaveResult.MissingItem -> items = items.filterNot { it.id == selectedItemId }
                            WidgetSaveResult.LoadFailed -> loadFailed = true
                        }
                    } finally {
                        // Keep the completed action disabled until the activity leaves the screen.
                        if (!saved) saving = false
                    }
                }
            }
        },
    )
}

@Composable
private fun WidgetConfigureScreen(
    configuration: WidgetConfiguration,
    dimensions: WidgetDimensions,
    paletteKey: String,
    dark: Boolean,
    showProgress: Boolean,
    items: List<ItemEntity>,
    loading: Boolean,
    loadFailed: Boolean,
    saving: Boolean,
    isEditing: Boolean,
    canKeepLegacySummary: Boolean,
    sourceFocus: FocusRequester,
    itemFocus: FocusRequester,
    onBack: () -> Unit,
    onChangeSource: () -> Unit,
    onChangeItem: () -> Unit,
    onRetry: () -> Unit,
    onShowRemainingChanged: (Boolean) -> Unit,
    onLegacySummaryChanged: (Boolean) -> Unit,
    onShowSecondsChanged: (Boolean) -> Unit,
    onSave: () -> Unit,
) {
    val context = LocalContext.current
    val generator = remember(context) { ItemGenerate(context) }
    val periods = listOf(generator.timeItem(), generator.monthItem(), generator.yearItem())
    fun customItem(entity: ItemEntity?): AdapterItem? = entity?.let {
        if (it.type == ItemType.Time) generator.customTimeItem(it) else generator.customMonthItem(it)
    }
    val source = configuration.source
    val personal = source == WidgetSource.Custom || source == WidgetSource.Next
    val selectedItem = items.firstOrNull { it.id == configuration.itemId }
    val customMissing = source == WidgetSource.Custom && !loading && !loadFailed && selectedItem == null
    val unavailable = personal && (loading || loadFailed)
    val previewItem = if (unavailable) null else when (source) {
        WidgetSource.Today -> periods[0]
        WidgetSource.Month -> periods[1]
        WidgetSource.Year -> periods[2]
        WidgetSource.Week -> com.devidea.timeleft.periods.calendarPeriodItem(context, com.devidea.timeleft.periods.CalendarPeriod.Week)
        WidgetSource.Quarter -> com.devidea.timeleft.periods.calendarPeriodItem(context, com.devidea.timeleft.periods.CalendarPeriod.Quarter)
        WidgetSource.Overview -> null
        WidgetSource.Next -> customItem(NextCountdownSelector.select(items, clock = com.devidea.timeleft.focus.readFocusClock(context)))
            ?: periods[1].takeIf { configuration.legacySummary }
        WidgetSource.Custom -> customItem(selectedItem)
            ?: periods[1].takeIf { configuration.legacySummary }
    }
    val emptyMessage = when {
        loading -> R.string.widget_loading
        loadFailed -> R.string.widget_load_failed
        source == WidgetSource.Custom && configuration.itemId != null -> R.string.widget_selected_deleted
        source == WidgetSource.Custom -> R.string.widget_choose_schedule
        else -> R.string.widget_no_upcoming
    }
    // Compare actual formatted results; built-in periods and waiting time ranges need no switch.
    val remainingValue = previewItem?.let { it.toWidgetData(context, true, it.type == ItemType.Time).value }
    val countdownValue = previewItem?.let { it.toWidgetData(context, false, it.type == ItemType.Time).value }
    val showFormat = personal && !unavailable && !customMissing && remainingValue != countdownValue
    val canShowSeconds = WidgetSecondsSupport.available() && !unavailable &&
        WidgetSecondsPlan.create(configuration, previewItem, System.currentTimeMillis()) != null
    val secondsEnabled = canShowSeconds && configuration.showSeconds
    Scaffold(
        topBar = { TimeLeftTopAppBar(stringResource(R.string.widget_configure_title), onBack) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding)) {
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState())
                    .padding(horizontal = LayoutTokens.ScreenHorizontal, vertical = Spacing.l),
                verticalArrangement = Arrangement.spacedBy(LayoutTokens.SectionGap),
            ) {
                WidgetSelectionSummary(
                    label = stringResource(R.string.widget_configure_source_title),
                    value = stringResource(source.labelRes), enabled = !saving, onChange = onChangeSource,
                    focusRequester = sourceFocus,
                )
                if (source == WidgetSource.Custom) {
                    WidgetSelectionSummary(
                        label = stringResource(R.string.widget_configure_custom_item_title),
                        value = when {
                            loading -> stringResource(R.string.widget_loading)
                            loadFailed -> stringResource(R.string.widget_load_failed)
                            selectedItem != null -> selectedItem.title
                            configuration.itemId != null -> stringResource(R.string.widget_selected_deleted)
                            else -> stringResource(R.string.widget_choose_schedule)
                        },
                        detail = selectedItem?.takeUnless { unavailable }?.let { widgetScheduleDescription(context, it) },
                        enabled = !saving, onChange = onChangeItem,
                        focusRequester = itemFocus,
                    )
                } else if (source == WidgetSource.Next) {
                    Text(stringResource(R.string.widget_auto_hint), style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else if (source == WidgetSource.Overview) {
                    Text(stringResource(R.string.widget_overview_description), style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (personal && loadFailed) {
                    TextButton(onClick = onRetry, enabled = !saving) { Text(stringResource(R.string.widget_retry)) }
                } else if (source == WidgetSource.Next && !loading && items.isEmpty()) {
                    Text(stringResource(R.string.widget_auto_empty_hint), style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                WidgetPreviewBand(dimensions, configuration, previewItem, periods, paletteKey, dark, emptyMessage, showProgress)
                if (canShowSeconds) {
                    com.devidea.timeleft.ui.components.TimeLeftSwitchRow(
                        title = stringResource(R.string.widget_seconds_option), checked = configuration.showSeconds,
                        onCheckedChange = onShowSecondsChanged, enabled = !saving,
                        summary = stringResource(R.string.widget_seconds_hint))
                }
                if (source == WidgetSource.Overview || source == WidgetSource.Today || previewItem?.type == ItemType.Time) {
                    if (!secondsEnabled) Text(stringResource(if (configuration.showSeconds) R.string.widget_seconds_unavailable else R.string.widget_snapshot_hint),
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (showFormat && !secondsEnabled) {
                    Column(Modifier.selectableGroup()) {
                        WidgetSectionLabel(stringResource(R.string.widget_display_format))
                        WidgetChoiceRow(stringResource(R.string.widget_format_units), configuration.showRemaining,
                            detail = remainingValue, enabled = !saving) { onShowRemainingChanged(true) }
                        WidgetChoiceRow(stringResource(R.string.widget_format_countdown), !configuration.showRemaining,
                            detail = countdownValue, enabled = !saving) { onShowRemainingChanged(false) }
                    }
                }
                if (source != WidgetSource.Overview && canKeepLegacySummary) {
                    WidgetOptionRow(stringResource(R.string.widget_legacy_summary_option),
                        configuration.legacySummary, !saving, onLegacySummaryChanged)
                }
            }
            Button(
                onClick = onSave,
                enabled = !saving && (!personal || (!loading && !loadFailed && !customMissing)),
                shape = MaterialTheme.shapes.small,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onBackground,
                    contentColor = MaterialTheme.colorScheme.background),
                modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = LayoutTokens.ScreenHorizontal, vertical = Spacing.l)
                    .heightIn(min = 52.dp),
            ) {
                Text(stringResource(when {
                    saving -> R.string.widget_saving
                    isEditing -> R.string.widget_apply_changes
                    else -> R.string.widget_add
                }))
            }
        }
    }
}

@Composable
private fun WidgetSelectionSummary(
    label: String, value: String, detail: String? = null, enabled: Boolean,
    focusRequester: FocusRequester, onChange: () -> Unit,
) {
    val changeDescription = stringResource(R.string.widget_change_description, label)
    Column {
        WidgetSectionLabel(label)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(value, style = MaterialTheme.typography.titleLarge)
                detail?.let { Text(it, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            TextButton(onClick = onChange, enabled = enabled,
                modifier = Modifier.heightIn(min = LayoutTokens.MinTouchTarget).focusRequester(focusRequester)
                    .semantics { contentDescription = changeDescription }) {
                Text(stringResource(R.string.widget_change))
            }
        }
        HorizontalDivider(Modifier.padding(top = Spacing.s), color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
internal fun WidgetSectionLabel(label: String) {
    Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = Spacing.s))
}

@Composable
internal fun WidgetChoiceRow(label: String, selected: Boolean, detail: String? = null, enabled: Boolean = true, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = LayoutTokens.MinTouchTarget)
            .selectable(selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = Spacing.m),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.m),
    ) {
        RadioButton(selected = selected, onClick = null, enabled = enabled)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            detail?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable
private fun WidgetOptionRow(label: String, checked: Boolean, enabled: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = LayoutTokens.MinTouchTarget)
            .toggleable(checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(vertical = Spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

@Composable
internal fun WidgetPreviewBand(
    dimensions: WidgetDimensions,
    configuration: WidgetConfiguration,
    item: AdapterItem?,
    periods: List<AdapterItem>,
    paletteKey: String,
    dark: Boolean,
    emptyMessage: Int,
    showProgress: Boolean,
    previewDescription: String? = null,
    snapshotTimeMillis: Long? = null,
) {
    val base = LocalContext.current
    val designKey = com.devidea.timeleft.ui.theme.LocalTimeLayout.current.key
    val previewFontScale = LocalDensity.current.fontScale
    val context = remember(base, previewFontScale) {
        base.createConfigurationContext(android.content.res.Configuration(base.resources.configuration).apply {
            fontScale = previewFontScale
        })
    }
    val meetsMinimum = dimensions.meetsMinimumFor(configuration.source)
    val previewDimensions = if (meetsMinimum) dimensions else WidgetDimensions.previewFor(configuration.source)
    val views = remember(context, previewDimensions, configuration, item, periods, paletteKey, dark, designKey, emptyMessage, showProgress, snapshotTimeMillis) {
        AppWidget().previewViews(context, previewDimensions, configuration, item, periods, paletteKey, dark, emptyMessage, showProgress,
            snapshotTimeMillis = snapshotTimeMillis, designKey = designKey)
    }
    val spokenPreview = if (previewDescription != null && views.layoutId == R.layout.app_widget_resize)
        stringResource(R.string.widget_size_hint) else previewDescription
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        WidgetSectionLabel(stringResource(when {
            previewDescription != null -> R.string.theme_widget_preview_size
            meetsMinimum -> R.string.widget_preview_current_size
            else -> R.string.widget_preview_recommended_size
        }))
        Text(stringResource(if (configuration.source == WidgetSource.Overview) R.string.widget_overview_size_hint else R.string.widget_minimum_size_hint), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (!meetsMinimum || views.layoutId == R.layout.app_widget_resize) {
            Text(stringResource(R.string.widget_size_hint), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val availableWidth = maxWidth
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                if (previewDimensions.width.dp > availableWidth) {
                    Text(stringResource(R.string.widget_preview_scroll_hint), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                    AndroidView(
                        factory = { FrameLayout(context).apply {
                            if (previewDescription != null) {
                                importantForAccessibility = android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
                                descendantFocusability = android.view.ViewGroup.FOCUS_BLOCK_DESCENDANTS
                            }
                        } },
                        update = { parent ->
                            parent.removeAllViews()
                            val content = views.apply(context, parent)
                            if (previewDescription != null) content.removePreviewActions()
                            parent.addView(content)
                        },
                        modifier = Modifier.width(previewDimensions.width.dp).height(previewDimensions.height.dp)
                            .then(if (spokenPreview != null) Modifier.clearAndSetSemantics { contentDescription = spokenPreview } else Modifier),
                    )
                }
            }
        }
    }
}

/** Preserve rendered appearance without advertising refresh/open actions inside a theme preview. */
private fun android.view.View.removePreviewActions() {
    setOnClickListener(null)
    isClickable = false
    isLongClickable = false
    isFocusable = false
    if (this is android.view.ViewGroup) (0 until childCount).forEach { getChildAt(it).removePreviewActions() }
}
