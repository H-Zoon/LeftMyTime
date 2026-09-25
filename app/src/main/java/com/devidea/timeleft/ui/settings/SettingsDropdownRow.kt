package com.devidea.timeleft.ui.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.devidea.timeleft.R
import com.devidea.timeleft.ui.theme.LayoutTokens
import com.devidea.timeleft.ui.theme.Spacing

internal data class SettingsOption<T>(val value: T, @StringRes val labelRes: Int)

/** Controlled selection: opening/dismissing a menu never writes a preference. */
@Composable
internal fun <T> SettingsDropdownRow(
    title: String,
    selectedValue: T,
    options: List<SettingsOption<T>>,
    onSelected: (T) -> Unit,
    summary: String? = null,
) {
    var expanded by remember(title) { mutableStateOf(false) }
    val currentLabel = stringResource(options.firstOrNull { it.value == selectedValue }?.labelRes
        ?: R.string.settings_choose_value)
    val expansionLabel = stringResource(if (expanded) R.string.settings_options_expanded else R.string.settings_options_collapsed)
    val chooseLabel = stringResource(R.string.settings_choose_option)
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        // Keep the anchor width explicit when entering the menu's separate receiver scope.
        val menuWidth = maxWidth
        val stacked = maxWidth < 300.dp || LocalDensity.current.fontScale > 1.2f
        Row(
            modifier = Modifier.fillMaxWidth()
                .heightIn(min = LayoutTokens.MinTouchTarget)
                .clickable(enabled = options.isNotEmpty(), role = Role.Button, onClickLabel = chooseLabel) { expanded = true }
                .semantics(mergeDescendants = true) { stateDescription = expansionLabel }
                .padding(vertical = Spacing.m),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                if (stacked) {
                    Text(title, style = MaterialTheme.typography.bodyLarge)
                    Text(currentLabel, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
                        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f).alignByBaseline())
                        Text(currentLabel, style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.End,
                            modifier = Modifier.weight(1f).alignByBaseline())
                    }
                }
                if (summary != null) Text(summary, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.width(menuWidth)) {
            options.forEach { option ->
                val chosen = option.value == selectedValue
                DropdownMenuItem(
                    text = { Text(stringResource(option.labelRes), style = MaterialTheme.typography.bodyLarge) },
                    trailingIcon = {
                        if (chosen) Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = LayoutTokens.MinTouchTarget)
                        .semantics { selected = chosen },
                    onClick = {
                        expanded = false
                        if (!chosen) onSelected(option.value)
                    },
                )
            }
        }
    }
}
