package com.devidea.timeleft.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Groups related fields on the page without introducing another card surface. */
@Composable
internal fun TimeLeftSection(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(modifier) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        content()
    }
}
