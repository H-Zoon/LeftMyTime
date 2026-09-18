package com.devidea.timeleft.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.devidea.timeleft.AdapterItem
import com.devidea.timeleft.R
import com.devidea.timeleft.ui.components.RemainingTimeText
import com.devidea.timeleft.ui.theme.Spacing

@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun HeaderSection(
    topItems: List<AdapterItem>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    prominent: Boolean,
) {
    if (topItems.isEmpty()) return
    val index = selectedIndex.coerceIn(topItems.indices)
    // Preserve the existing persisted order: today, month, year.
    val labels = listOf(R.string.period_today, R.string.period_month, R.string.period_year)
    Column(Modifier.fillMaxWidth().padding(bottom = Spacing.xxl)) {
        if (!prominent) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        FlowRow(modifier = Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(Spacing.xl)) {
            topItems.forEachIndexed { position, item ->
                Column(Modifier.width(IntrinsicSize.Max).widthIn(min = 48.dp).selectable(selected = index == position, role = Role.Tab, onClick = { onSelectedIndexChange(position) })) {
                    Box(Modifier.heightIn(min = 48.dp).padding(vertical = Spacing.m)) {
                        Text(
                            if (position < labels.size) stringResource(labels[position]) else item.title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (index == position) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    HorizontalDivider(color = if (index == position) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.background)
                }
            }
        }
        RemainingTimeText(topItems[index], hero = prominent, modifier = Modifier.padding(top = Spacing.l))
    }
}
