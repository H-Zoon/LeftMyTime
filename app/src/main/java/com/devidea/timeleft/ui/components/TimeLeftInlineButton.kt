package com.devidea.timeleft.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.devidea.timeleft.ui.theme.LayoutTokens
import com.devidea.timeleft.ui.theme.Spacing

/** Text actions on a page share the content's start edge, including short labels. */
@Composable
internal fun TimeLeftInlineButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.defaultMinSize(
            minWidth = LayoutTokens.MinTouchTarget,
            minHeight = LayoutTokens.MinTouchTarget,
        ),
        contentPadding = PaddingValues(vertical = Spacing.s),
    ) {
        // Keep a 48dp target without centering a short label inside that width.
        Row(
            modifier = Modifier.defaultMinSize(minWidth = LayoutTokens.MinTouchTarget),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}
