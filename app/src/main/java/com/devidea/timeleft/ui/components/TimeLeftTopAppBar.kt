package com.devidea.timeleft.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.devidea.timeleft.R
import com.devidea.timeleft.ui.theme.LayoutTokens
import com.devidea.timeleft.ui.theme.Spacing

@Composable
internal fun TimeLeftTopAppBar(title: String, onBack: () -> Unit) {
    // Material's fixed-height small bar ellipsizes long titles at large font scales.
    // Preserve the type scale and allow the common bar to grow with its actual text.
    Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)
        .windowInsetsPadding(WindowInsets.statusBars.union(WindowInsets.displayCutout).only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
        .heightIn(min = LayoutTokens.TopAppBarMinHeight).padding(start = Spacing.xs, end = Spacing.s, top = Spacing.s, bottom = Spacing.s),
        verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack, modifier = Modifier.size(LayoutTokens.MinTouchTarget)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back),
                tint = MaterialTheme.colorScheme.onBackground)
        }
        Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f).padding(start = Spacing.xs).semantics { heading() })
    }
}
