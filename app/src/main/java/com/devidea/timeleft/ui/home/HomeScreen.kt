package com.devidea.timeleft.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.devidea.timeleft.AdapterItem
import com.devidea.timeleft.R

@Composable
fun HomeScreen(
    dateText: String,
    timeText: String,
    themeMode: String,
    topItems: List<AdapterItem>,
    customItems: List<AdapterItem>,
    onToggleTheme: () -> Unit,
    onAddTime: () -> Unit,
    onAddDate: () -> Unit,
    onEditItem: (Int) -> Unit,
    onDeleteItem: (Int) -> Unit,
) {
    val nextCountdown = customItems
        .filterNot { it.isExpired }
        .minByOrNull { it.remainingSortKey }
        ?: customItems.firstOrNull()

    Scaffold(
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ExtendedFloatingActionButton(
                    onClick = onAddTime,
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                    icon = {
                        Icon(
                            Icons.Filled.AccessTime,
                            contentDescription = stringResource(R.string.home_add_time_range)
                        )
                    },
                    text = { Text(stringResource(R.string.home_add_time_range)) }
                )
                ExtendedFloatingActionButton(
                    onClick = onAddDate,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    icon = {
                        Icon(
                            Icons.Filled.CalendarMonth,
                            contentDescription = stringResource(R.string.home_add_date)
                        )
                    },
                    text = { Text(stringResource(R.string.home_add_date)) }
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 148.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                HeaderSection(
                    dateText = dateText,
                    timeText = timeText,
                    themeMode = themeMode,
                    onToggleTheme = onToggleTheme
                )
            }

            if (nextCountdown != null) {
                item {
                    NextCountdownHero(
                        item = nextCountdown,
                        onEditItem = onEditItem,
                        onDeleteItem = onDeleteItem,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
            }

            item {
                SummarySection(topItems = topItems)
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.home_my_items),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f)
                    )
                    if (customItems.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.home_item_count, customItems.size),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (customItems.isEmpty()) {
                item {
                    EmptyItemState(
                        onAddTime = onAddTime,
                        onAddDate = onAddDate,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
            } else {
                items(customItems, key = { it.id }) { item ->
                    TimeLeftItemCard(
                        item = item,
                        onEditItem = onEditItem,
                        onDeleteItem = onDeleteItem,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
            }
        }
    }
}
