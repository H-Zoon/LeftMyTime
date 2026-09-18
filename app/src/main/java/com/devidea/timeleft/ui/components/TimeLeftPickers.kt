package com.devidea.timeleft.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.devidea.timeleft.R
import com.devidea.timeleft.ui.theme.Spacing
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun TimeLeftDatePicker(value: LocalDate, onDismiss: () -> Unit, onSelect: (LocalDate) -> Unit) {
    if (LocalConfiguration.current.screenWidthDp < 360 || LocalDensity.current.fontScale > 1.3f) {
        DateInputDialog(value, onDismiss, onSelect)
        return
    }
    // Material's date picker exchanges UTC-midnight dates, not local instants.
    val state = rememberDatePickerState(initialSelectedDateMillis = value.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = {
            state.selectedDateMillis?.let { onSelect(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
            onDismiss()
        }, enabled = state.selectedDateMillis != null) { Text(stringResource(R.string.action_confirm)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    ) { DatePicker(state = state) }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun TimeLeftTimePicker(value: LocalTime, label: String, onDismiss: () -> Unit, onSelect: (LocalTime) -> Unit) {
    val context = LocalContext.current
    val use24Hour = android.text.format.DateFormat.is24HourFormat(context)
    var hours by rememberSaveable(value) { mutableStateOf((if (use24Hour) value.hour else (value.hour % 12).let { if (it == 0) 12 else it }).toString()) }
    var minutes by rememberSaveable(value) { mutableStateOf(value.minute.toString().padStart(2, '0')) }
    var afternoon by rememberSaveable(value) { mutableStateOf(value.hour >= 12) }
    val selectedTime = parseTimeInput(hours, minutes, use24Hour, afternoon)
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.widthIn(max = 480.dp).fillMaxWidth().padding(Spacing.l), shape = MaterialTheme.shapes.large) {
            Column(Modifier.imePadding().verticalScroll(rememberScrollState()).padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.l)) {
                Text(label, style = MaterialTheme.typography.titleLarge)
                val fieldWidth = 104.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
                    OutlinedTextField(hours, onValueChange = { hours = it.filter(Char::isDigit).take(2) },
                        label = { Text(stringResource(R.string.time_hour_field)) }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = MaterialTheme.typography.displaySmall,
                        modifier = Modifier.weight(1f).widthIn(min = fieldWidth))
                    OutlinedTextField(minutes, onValueChange = { minutes = it.filter(Char::isDigit).take(2) },
                        label = { Text(stringResource(R.string.time_minute_field)) }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = MaterialTheme.typography.displaySmall,
                        modifier = Modifier.weight(1f).widthIn(min = fieldWidth))
                }
                if (!use24Hour) {
                    val names = java.text.DateFormatSymbols(context.resources.configuration.locales[0]).amPmStrings
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                        FilterChip(!afternoon, onClick = { afternoon = false }, label = { Text(names[0]) })
                        FilterChip(afternoon, onClick = { afternoon = true }, label = { Text(names[1]) })
                    }
                }
                if (selectedTime == null) Text(stringResource(R.string.time_input_invalid), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
                    TextButton(onClick = { selectedTime?.let(onSelect); onDismiss() }, enabled = selectedTime != null) { Text(stringResource(R.string.action_confirm)) }
                }
            }
        }
    }
}

internal fun parseTimeInput(hours: String, minutes: String, use24Hour: Boolean, afternoon: Boolean): LocalTime? {
    val h = hours.toIntOrNull() ?: return null
    val m = minutes.toIntOrNull() ?: return null
    if (m !in 0..59 || h !in (if (use24Hour) 0..23 else 1..12)) return null
    return LocalTime.of(if (use24Hour) h else h % 12 + if (afternoon) 12 else 0, m)
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun DateInputDialog(value: LocalDate, onDismiss: () -> Unit, onSelect: (LocalDate) -> Unit) {
    var year by rememberSaveable(value) { mutableStateOf(value.year.toString()) }
    var month by rememberSaveable(value) { mutableStateOf(value.monthValue.toString()) }
    var day by rememberSaveable(value) { mutableStateOf(value.dayOfMonth.toString()) }
    val selected = parseDateInput(year, month, day)
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.widthIn(max = 480.dp).fillMaxWidth().padding(Spacing.l), shape = MaterialTheme.shapes.large) {
            Column(Modifier.imePadding().verticalScroll(rememberScrollState()).padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.l)) {
                Text(stringResource(R.string.date_input_title), style = MaterialTheme.typography.titleLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.m), verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
                    listOf(Triple(R.string.date_year_field, year, 4), Triple(R.string.date_month_field, month, 2), Triple(R.string.date_day_field, day, 2)).forEachIndexed { index, (label, text, length) ->
                        OutlinedTextField(text, onValueChange = {
                            val number = it.filter(Char::isDigit).take(length)
                            when (index) { 0 -> year = number; 1 -> month = number; else -> day = number }
                        }, label = { Text(stringResource(label)) }, singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            textStyle = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.weight(1f).widthIn(min = 110.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)))
                    }
                }
                if (selected == null) Text(stringResource(R.string.date_input_invalid), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
                    TextButton(onClick = { selected?.let(onSelect); onDismiss() }, enabled = selected != null) { Text(stringResource(R.string.action_confirm)) }
                }
            }
        }
    }
}

internal fun parseDateInput(year: String, month: String, day: String): LocalDate? {
    val y = year.toIntOrNull()?.takeIf { it in 1..9999 } ?: return null
    val m = month.toIntOrNull() ?: return null
    val d = day.toIntOrNull() ?: return null
    return runCatching { LocalDate.of(y, m, d) }.getOrNull()
}
