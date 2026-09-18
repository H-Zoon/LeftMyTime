package com.devidea.timeleft.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.devidea.timeleft.AdapterItem
import com.devidea.timeleft.R
import com.devidea.timeleft.preferences.UserPreferences
import com.devidea.timeleft.ui.components.RemainingTimeText
import com.devidea.timeleft.ui.components.TimeRuler
import com.devidea.timeleft.ui.theme.Spacing

@Composable
internal fun NextCountdownHero(
    item: AdapterItem,
    progressDisplayMode: String,
    onEditItem: (Int) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(top = Spacing.m, bottom = Spacing.xxl), verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
        Text(stringResource(R.string.home_active_until, item.endLabel), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            item.title, style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                .clickable(role = Role.Button, onClickLabel = stringResource(R.string.card_action_edit)) { onEditItem(item.id) }
        )
        RemainingTimeText(item, hero = true)
        if (progressDisplayMode != UserPreferences.PROGRESS_DISPLAY_HIDDEN) {
            TimeRuler(item.percent, item.startLabel, item.endLabel, currentLabel = item.currentLabel)
        }
    }
}
