package com.devidea.timeleft.focus

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.devidea.timeleft.ui.TimeDisplayItem
import com.devidea.timeleft.R
import com.devidea.timeleft.ui.components.TimeLeftInlineButton
import com.devidea.timeleft.ui.theme.Spacing

@Composable
fun QuickFocusSection(
    current: TimeDisplayItem?, busy: Boolean, error: Int?, onStart: () -> Unit, onOpen: (Int) -> Unit,
    inActionRow: Boolean = false,
) {
    Column(if (inActionRow) Modifier else Modifier.fillMaxWidth().padding(vertical = Spacing.s),
        verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        TimeLeftInlineButton(onClick = { if (current == null) onStart() else onOpen(current.id) }, enabled = !busy,
            colors = ButtonDefaults.textButtonColors(contentColor = if (current == null)
                MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary)) {
            Text(if (current == null) stringResource(R.string.focus_quick_start)
                else stringResource(R.string.focus_open_named, current.title))
        }
        error?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
    }
}
