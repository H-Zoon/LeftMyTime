package com.devidea.timeleft.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.devidea.timeleft.AdapterItem
import com.devidea.timeleft.R
import com.devidea.timeleft.ui.itemAccentColor
import com.devidea.timeleft.ui.itemIconVector

@Composable
internal fun NextCountdownHero(
    item: AdapterItem,
    modifier: Modifier = Modifier,
) {
    val accent = itemAccentColor(item.colorKey, countdownAccent(item))
    val progress by animateFloatAsState(
        targetValue = (item.percent / 100f).coerceIn(0f, 1f),
        label = "heroProgress"
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = accent.copy(alpha = 0.12f),
                    contentColor = accent
                ) {
                    Icon(
                        imageVector = itemIconVector(item.iconKey),
                        contentDescription = null,
                        modifier = Modifier
                            .padding(7.dp)
                            .size(18.dp)
                    )
                }
                Text(
                    text = stringResource(R.string.home_next_countdown),
                    style = MaterialTheme.typography.labelLarge,
                    color = accent,
                    modifier = Modifier.weight(1f)
                )
            }
            Text(
                text = item.countdownText.ifBlank { formatPercent(item.percent) + "%" },
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = item.title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = item.dueText.ifBlank { item.leftString },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = accent,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Text(
                text = stringResource(R.string.card_progress_value, formatPercent(item.percent)),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
internal fun InfoChip(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.12f),
        contentColor = color
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
internal fun countdownAccent(item: AdapterItem): Color =
    when {
        item.isExpired -> MaterialTheme.colorScheme.onSurfaceVariant
        item.remainingSortKey <= SECONDS_PER_DAY -> MaterialTheme.colorScheme.error
        item.remainingSortKey <= SECONDS_PER_WEEK -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }

private const val SECONDS_PER_DAY = 86_400L
private const val SECONDS_PER_WEEK = SECONDS_PER_DAY * 7
