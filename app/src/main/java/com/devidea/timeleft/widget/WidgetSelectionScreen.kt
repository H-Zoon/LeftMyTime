package com.devidea.timeleft.widget

import android.content.Context
import com.devidea.timeleft.formatClockTime
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import com.devidea.timeleft.R
import com.devidea.timeleft.database.itemdata.ItemEntity
import com.devidea.timeleft.database.itemdata.ItemType
import com.devidea.timeleft.ui.components.TimeLeftTopAppBar
import com.devidea.timeleft.ui.components.TimeLeftUnderlineTextField
import com.devidea.timeleft.ui.theme.LayoutTokens
import com.devidea.timeleft.ui.theme.Spacing
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** One selection surface: custom schedules replace the source list instead of stacking dialogs. */
@Composable
internal fun WidgetSelectionScreen(
    pickingItems: Boolean,
    source: WidgetSource,
    selectedItemId: Int?,
    items: List<ItemEntity>,
    loading: Boolean,
    loadFailed: Boolean,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onSourceSelected: (WidgetSource) -> Unit,
    onItemSelected: (Int) -> Unit,
) {
    var query by rememberSaveable(pickingItems) { mutableStateOf("") }
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    val matchingItems = remember(items, query) {
        val term = query.trim()
        items.filter { term.isEmpty() || it.title.contains(term, ignoreCase = true) || it.category.contains(term, ignoreCase = true) }
    }
    Scaffold(
        topBar = {
            TimeLeftTopAppBar(stringResource(if (pickingItems) R.string.widget_choose_schedule else R.string.widget_choose_content)) {
                keyboard?.hide()
                onBack()
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).imePadding().padding(horizontal = LayoutTokens.ScreenHorizontal)) {
            if (pickingItems) {
                TimeLeftUnderlineTextField(
                    value = query, onValueChange = { query = it },
                    label = { Text(stringResource(R.string.home_search_hint)) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                    modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.l),
                )
            }
            LazyColumn(
                Modifier.weight(1f).fillMaxWidth().selectableGroup(),
                contentPadding = PaddingValues(vertical = Spacing.l),
            ) {
                if (!pickingItems) {
                    item { WidgetSectionLabel(stringResource(R.string.widget_group_periods)) }
                    items(listOf(WidgetSource.Today, WidgetSource.Month, WidgetSource.Year, WidgetSource.Week, WidgetSource.Quarter, WidgetSource.Overview)) { option ->
                        WidgetChoiceRow(stringResource(option.labelRes), option == source,
                            detail = if (option == WidgetSource.Overview) stringResource(R.string.widget_overview_description) else null,
                        ) { onSourceSelected(option) }
                    }
                    item {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(Modifier.height(LayoutTokens.SectionGap))
                        WidgetSectionLabel(stringResource(R.string.widget_group_schedules))
                    }
                    items(listOf(WidgetSource.Next, WidgetSource.Custom)) { option ->
                        WidgetChoiceRow(stringResource(option.labelRes), option == source,
                            detail = stringResource(if (option == WidgetSource.Next) R.string.widget_auto_hint else R.string.widget_manual_hint),
                        ) { onSourceSelected(option) }
                    }
                } else when {
                    loading -> item { Text(stringResource(R.string.widget_loading), style = MaterialTheme.typography.bodyLarge) }
                    loadFailed -> item {
                        Text(stringResource(R.string.widget_load_failed), style = MaterialTheme.typography.bodyLarge)
                        TextButton(onClick = onRetry) { Text(stringResource(R.string.widget_retry)) }
                    }
                    items.isEmpty() -> item {
                        Text(stringResource(R.string.widget_configure_no_items), style = MaterialTheme.typography.bodyLarge)
                        Text(stringResource(R.string.widget_empty_selection_hint), style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = Spacing.s))
                    }
                    matchingItems.isEmpty() -> item {
                        Text(stringResource(R.string.home_empty_search), style = MaterialTheme.typography.bodyLarge)
                        TextButton(onClick = { query = "" }) { Text(stringResource(R.string.widget_clear_search)) }
                    }
                    else -> items(matchingItems, key = { it.id }) { item ->
                        WidgetChoiceRow(item.title, source == WidgetSource.Custom && item.id == selectedItemId,
                            detail = widgetScheduleDescription(context, item)) {
                            keyboard?.hide()
                            onItemSelected(item.id)
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }
}

internal fun widgetScheduleDescription(context: Context, item: ItemEntity): String {
    if (item.occurrenceStartMillis != null && item.occurrenceEndMillis != null) return com.devidea.timeleft.calendar.occurrenceLabel(context, item.occurrenceStartMillis) + " – " +
            com.devidea.timeleft.calendar.occurrenceLabel(context, item.occurrenceEndMillis)
    val locale = context.resources.configuration.locales[0]
    val range = runCatching {
        if (item.type == ItemType.Time) {
            val storage = DateTimeFormatter.ofPattern("H:m")
            "${formatClockTime(context, LocalTime.parse(item.startValue, storage))} – ${formatClockTime(context, LocalTime.parse(item.endValue, storage))}" +
                if (item.endNextDay) " · " + context.getString(R.string.editor_next_day) else ""
        } else {
            val storage = DateTimeFormatter.ofPattern("yyyy-M-d")
            val display = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
            "${LocalDate.parse(item.startValue, storage).format(display)} – ${LocalDate.parse(item.endValue, storage).format(display)}"
        }
    }.getOrElse { "${item.startValue} – ${item.endValue}" }
    val type = context.getString(if (item.type == ItemType.Time) R.string.widget_schedule_time else R.string.widget_schedule_date)
    return "$type · $range"
}
