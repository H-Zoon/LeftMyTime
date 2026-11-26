package com.devidea.timeleft.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.devidea.timeleft.R
import com.devidea.timeleft.ui.components.TimeLeftInlineButton
import com.devidea.timeleft.ui.theme.LayoutTokens
import com.devidea.timeleft.ui.theme.Spacing

/** One entry point keeps every schedule source available without repeating actions on the page. */
@Composable
internal fun ScheduleAddButton(
    onAddTime: () -> Unit,
    onAddDate: () -> Unit,
    showText: Boolean,
    onOpenTemplates: (() -> Unit)? = null,
    onOpenCalendar: (() -> Unit)? = null,
    emphasized: Boolean = false,
) {
    var expanded by remember { mutableStateOf(false) }
    val label = stringResource(R.string.home_add_schedule)
    val stateLabel = stringResource(if (expanded) R.string.settings_options_expanded else R.string.settings_options_collapsed)
    val actionModifier = Modifier.heightIn(min = LayoutTokens.MinTouchTarget)
        .semantics { stateDescription = stateLabel }
    Box {
        when {
            showText && emphasized -> Button(onClick = { expanded = true }, modifier = actionModifier,
                shape = MaterialTheme.shapes.small) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(Spacing.xs))
                Text(label)
            }
            showText -> TimeLeftInlineButton(onClick = { expanded = true }, modifier = actionModifier) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(Spacing.xs))
                Text(label)
            }
            else -> IconButton(onClick = { expanded = true }, modifier = actionModifier) {
                Icon(Icons.Default.Add, contentDescription = label)
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text(stringResource(R.string.home_add_time_range)) },
                modifier = Modifier.heightIn(min = LayoutTokens.MinTouchTarget),
                onClick = { expanded = false; onAddTime() })
            DropdownMenuItem(text = { Text(stringResource(R.string.home_add_date)) },
                modifier = Modifier.heightIn(min = LayoutTokens.MinTouchTarget),
                onClick = { expanded = false; onAddDate() })
            if (onOpenTemplates != null) DropdownMenuItem(text = { Text(stringResource(R.string.templates_title)) },
                modifier = Modifier.heightIn(min = LayoutTokens.MinTouchTarget),
                onClick = { expanded = false; onOpenTemplates() })
            if (onOpenCalendar != null) DropdownMenuItem(text = { Text(stringResource(R.string.calendar_import_title)) },
                modifier = Modifier.heightIn(min = LayoutTokens.MinTouchTarget),
                onClick = { expanded = false; onOpenCalendar() })
        }
    }
}
