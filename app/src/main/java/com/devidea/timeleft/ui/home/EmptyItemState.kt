package com.devidea.timeleft.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.devidea.timeleft.R
import com.devidea.timeleft.ui.theme.Spacing

@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun EmptyItemState(onAddTime: () -> Unit, onAddDate: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(vertical = Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
        Text(stringResource(R.string.home_empty_title), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.home_empty_body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            TextButton(onClick = onAddTime) { Text(stringResource(R.string.home_add_time_range)) }
            TextButton(onClick = onAddDate) { Text(stringResource(R.string.home_add_date)) }
        }
    }
}
