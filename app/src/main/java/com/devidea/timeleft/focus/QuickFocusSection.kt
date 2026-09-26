package com.devidea.timeleft.focus

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.devidea.timeleft.AdapterItem
import com.devidea.timeleft.R
import com.devidea.timeleft.ui.components.TimeLeftUnderlineTextField
import com.devidea.timeleft.ui.theme.LayoutTokens
import com.devidea.timeleft.ui.theme.Spacing

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun QuickFocusSection(current: AdapterItem?, busy: Boolean, error: Int?, onStart: (Int) -> Unit, onOpen: (Int) -> Unit) {
    var custom by rememberSaveable { mutableStateOf(false) }
    var minutes by rememberSaveable { mutableStateOf("25") }
    Column(Modifier.padding(vertical = Spacing.m), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        if (current == null) {
            Text(stringResource(R.string.focus_quick_start), style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.s),
                verticalArrangement = Arrangement.spacedBy(Spacing.s),
            ) {
                listOf(15, 25, 50).forEach { value ->
                    OutlinedButton(onClick = { onStart(value) }, enabled = !busy, modifier = Modifier.heightIn(min = LayoutTokens.MinTouchTarget)) {
                        Text(stringResource(R.string.focus_minutes, value))
                    }
                }
                TextButton(onClick = { custom = true }, enabled = !busy, modifier = Modifier.heightIn(min = LayoutTokens.MinTouchTarget)) {
                    Text(stringResource(R.string.focus_custom))
                }
            }
        } else TextButton(onClick = { onOpen(current.id) }, modifier = Modifier.heightIn(min = LayoutTokens.MinTouchTarget)) {
            Text(stringResource(R.string.focus_open_named, current.title))
        }
        TextButton(onClick = { onOpen(-1) }, modifier = Modifier.heightIn(min = LayoutTokens.MinTouchTarget)) {
            Text(stringResource(R.string.focus_history))
        }
        error?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
    }
    if (custom) AlertDialog(onDismissRequest = { custom = false },
        title = { Text(stringResource(R.string.focus_quick_start)) },
        text = { Column {
            TimeLeftUnderlineTextField(value = minutes, onValueChange = { minutes = it.filter(Char::isDigit).take(4) },
                label = { Text(stringResource(R.string.focus_duration_minutes)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            Text(stringResource(R.string.focus_duration_limit), style = MaterialTheme.typography.bodySmall)
        } },
        confirmButton = { TextButton(onClick = { minutes.toIntOrNull()?.takeIf { it in 1..1440 }?.let { custom = false; onStart(it) } },
            enabled = !busy && (minutes.toIntOrNull() ?: 0) in 1..1440) { Text(stringResource(R.string.focus_start)) } },
        dismissButton = { TextButton(onClick = { custom = false }) { Text(stringResource(R.string.action_cancel)) } })
}
