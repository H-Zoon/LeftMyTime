package com.devidea.timeleft.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.devidea.timeleft.ui.theme.LocalTimeLayout
import com.devidea.timeleft.ui.theme.Spacing
import com.devidea.timeleft.ui.theme.TimeLayout

/** Layout alone changes; callers retain the same numbers, semantics and actions. */
@Composable
internal fun TimeHeadline(
    spacing: Dp = Spacing.m,
    label: @Composable () -> Unit,
    value: @Composable () -> Unit,
) {
    val board = LocalTimeLayout.current == TimeLayout.TimeBoard
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing),
        horizontalAlignment = if (board) Alignment.CenterHorizontally else Alignment.Start) {
        if (board) { value(); label() } else { label(); value() }
    }
}
