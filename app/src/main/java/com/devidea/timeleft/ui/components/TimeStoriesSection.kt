package com.devidea.timeleft.ui.components

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.devidea.timeleft.R
import com.devidea.timeleft.ui.theme.LayoutTokens
import com.devidea.timeleft.ui.theme.Spacing

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TimeStoriesSection(identity: String, remaining: Long, inDays: Boolean) {
    // Freeze the shortlist while reading, including rotation. A new item/phase opens a new set.
    // Historical stories do not become impossible when the countdown passes their duration.
    val anchor by rememberSaveable(identity, inDays) { mutableLongStateOf(remaining) }
    var index by rememberSaveable(identity, inDays) { mutableIntStateOf(0) }
    val candidates = remember(anchor, inDays) { timeStories(anchor, inDays) }
    if (remaining <= 0 || candidates.isEmpty()) return
    val selected = candidates[index.coerceIn(candidates.indices)]
    val context = LocalContext.current
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Text(stringResource(R.string.time_stories_title), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.time_stories_duration, stringResource(selected.durationRes)),
            style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(stringResource(selected.textRes), style = MaterialTheme.typography.bodyLarge)
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.l)) {
            if (candidates.size > 1) {
                TextButton(onClick = { index = (index + 1) % candidates.size },
                    modifier = Modifier.heightIn(min = LayoutTokens.MinTouchTarget).alignByBaseline(),
                    contentPadding = PaddingValues(horizontal = 0.dp, vertical = Spacing.s)) {
                    Text(stringResource(R.string.time_stories_another))
                }
            }
            TextButton(onClick = {
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(selected.sourceUrl)))
                } catch (_: ActivityNotFoundException) {
                    Toast.makeText(context, R.string.time_stories_source_unavailable, Toast.LENGTH_SHORT).show()
                }
            }, modifier = Modifier.heightIn(min = LayoutTokens.MinTouchTarget).alignByBaseline(),
                contentPadding = PaddingValues(horizontal = 0.dp, vertical = Spacing.s)) {
                Text(stringResource(R.string.time_stories_source, stringResource(selected.sourceNameRes)),
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
