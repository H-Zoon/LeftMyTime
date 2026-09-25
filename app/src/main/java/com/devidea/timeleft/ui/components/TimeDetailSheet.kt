package com.devidea.timeleft.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.devidea.timeleft.AdapterItem
import com.devidea.timeleft.R
import com.devidea.timeleft.ui.theme.LayoutTokens
import com.devidea.timeleft.ui.theme.Spacing
import com.devidea.timeleft.widget.PinWidgetButton
import com.devidea.timeleft.widget.WidgetSource
import kotlinx.coroutines.launch

/** Shared by calendar periods and personal schedules; one explicit opening owns motion state. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TimeDetailSheet(
    item: AdapterItem,
    title: String,
    progressDisplayMode: String,
    source: WidgetSource?,
    onDismiss: () -> Unit,
) {
    val contentDensity = LocalDensity.current
    val maxSheetHeight = LocalConfiguration.current.screenHeightDp.dp * LayoutTokens.DetailSheetMaxHeightFraction
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState,
        // Cap the whole surface, including its drag handle; the body scrolls within it.
        modifier = Modifier.heightIn(max = maxSheetHeight),
        containerColor = MaterialTheme.colorScheme.background) {
        // Dialog windows may replace LocalDensity; preserve the caller's text scale.
        CompositionLocalProvider(LocalDensity provides contentDensity) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = LayoutTokens.ScreenHorizontal).padding(bottom = Spacing.xxl),
                verticalArrangement = Arrangement.spacedBy(Spacing.l)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                    IconButton(onClick = { scope.launch { sheetState.hide(); onDismiss() } }) {
                        Icon(Icons.Default.Close, stringResource(R.string.period_close_details))
                    }
                }
                TimeDetailContent(item, progressDisplayMode)
                source?.let { PinWidgetButton(item, it, contentPadding = PaddingValues(vertical = Spacing.s)) }
            }
        }
    }
}
