package com.devidea.timeleft.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.devidea.timeleft.R
import com.devidea.timeleft.ui.theme.Spacing

@Composable
internal fun EmptyItemState(
    onAddTime: () -> Unit,
    onAddDate: () -> Unit,
    modifier: Modifier = Modifier,
    interactive: Boolean = true,
    onOpenTemplates: (() -> Unit)? = null,
    onOpenCalendar: (() -> Unit)? = null,
) {
    Column(modifier.fillMaxWidth().padding(vertical = Spacing.page), verticalArrangement = Arrangement.spacedBy(Spacing.l)) {
        Text(stringResource(R.string.home_empty_title), style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(R.string.home_empty_body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (interactive) ScheduleAddButton(onAddTime, onAddDate, showText = true,
            onOpenTemplates = onOpenTemplates, onOpenCalendar = onOpenCalendar, emphasized = true)
    }
}
