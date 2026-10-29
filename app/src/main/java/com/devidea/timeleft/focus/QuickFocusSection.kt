package com.devidea.timeleft.focus

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.devidea.timeleft.AdapterItem
import com.devidea.timeleft.R
import com.devidea.timeleft.ui.components.TimeLeftInlineButton
import com.devidea.timeleft.ui.theme.Spacing

@Composable
fun QuickFocusSection(current: AdapterItem?, busy: Boolean, error: Int?, onStart: () -> Unit, onOpen: (Int) -> Unit) {
    Column(Modifier.padding(vertical = Spacing.m), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        TimeLeftInlineButton(onClick = { if (current == null) onStart() else onOpen(current.id) }, enabled = !busy) {
            Text(if (current == null) stringResource(R.string.focus_quick_start)
                else stringResource(R.string.focus_open_named, current.title))
        }
        error?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
    }
}
