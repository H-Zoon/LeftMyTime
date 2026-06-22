package com.devidea.timeleft.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.devidea.timeleft.AdapterItem
import com.devidea.timeleft.R
import com.devidea.timeleft.preferences.UserPreferences
import com.devidea.timeleft.ui.theme.Motion
import com.devidea.timeleft.ui.theme.Spacing

@Composable
fun HomeScreen(
    initialSortValue: String,
    initialLayoutValue: String,
    expiredItemsMode: String,
    progressDisplayMode: String,
    topItems: List<AdapterItem>,
    customItems: List<AdapterItem>,
    onOpenSettings: () -> Unit,
    onSortChange: (String) -> Unit,
    onLayoutChange: (String) -> Unit,
    onAddTime: () -> Unit,
    onAddDate: () -> Unit,
    onEditItem: (Int) -> Unit,
    onDeleteItem: (Int) -> Unit,
) {
    var fabExpanded by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedSortValue by rememberSaveable(initialSortValue) { mutableStateOf(initialSortValue) }
    var selectedLayoutValue by rememberSaveable(initialLayoutValue) {
        mutableStateOf(initialLayoutValue)
    }
    val selectedSort = remember(selectedSortValue) {
        runCatching { HomeSortMode.valueOf(selectedSortValue) }.getOrDefault(HomeSortMode.Nearest)
    }
    val isGrid = selectedLayoutValue == UserPreferences.HOME_LAYOUT_GRID
    val listState = rememberLazyGridState()
    val targetCollapseFraction by remember(listState) {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0 ||
                listState.firstVisibleItemScrollOffset >= HEADER_COLLAPSE_THRESHOLD_PX
            ) {
                1f
            } else {
                0f
            }
        }
    }
    val collapseFraction by animateFloatAsState(
        targetValue = targetCollapseFraction,
        animationSpec = tween(durationMillis = Motion.MediumMs),
        label = "timeFlowBandCollapse"
    )
    val displayedItems = remember(customItems, expiredItemsMode) {
        if (expiredItemsMode == UserPreferences.EXPIRED_ITEMS_HIDE) {
            customItems.filterNot { it.isExpired }
        } else {
            customItems
        }
    }
    val nextCountdown = displayedItems
        .filterNot { it.isExpired }
        .minByOrNull { it.remainingSortKey }
        ?: displayedItems.firstOrNull()
    val visibleItems = remember(displayedItems, searchQuery, selectedSort, expiredItemsMode) {
        val query = searchQuery.trim()
        displayedItems
            .filter { item ->
                query.isBlank() ||
                    item.title.contains(query, ignoreCase = true) ||
                    item.category.contains(query, ignoreCase = true)
            }
            .let { items ->
                val sorted = when (selectedSort) {
                    HomeSortMode.Nearest -> items.sortedWith(
                        compareBy<AdapterItem> { it.remainingSortKey }
                            .thenBy { it.id }
                    )
                    HomeSortMode.Created -> items.sortedByDescending { it.id }
                    HomeSortMode.Title -> items.sortedBy { it.title.lowercase() }
                    HomeSortMode.Progress -> items.sortedByDescending { it.percent }
                }
                if (expiredItemsMode == UserPreferences.EXPIRED_ITEMS_BOTTOM) {
                    sorted.sortedBy { it.isExpired }
                } else {
                    sorted
                }
            }
    }

    Scaffold(
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(Spacing.s)
            ) {
                AnimatedVisibility(
                    visible = fabExpanded,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(Spacing.s)
                    ) {
                        ExtendedFloatingActionButton(
                            onClick = {
                                fabExpanded = false
                                onAddTime()
                            },
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
                            onClick = {
                                fabExpanded = false
                                onAddDate()
                            },
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
                FloatingActionButton(
                    onClick = { fabExpanded = !fabExpanded },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(
                        imageVector = if (fabExpanded) Icons.Filled.Close else Icons.Filled.Add,
                        contentDescription = stringResource(R.string.home_add_item)
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = dynamicDp(2.dp, 1.dp, collapseFraction)
            ) {
                Column(
                    modifier = Modifier.padding(bottom = dynamicDp(Spacing.m, Spacing.s, collapseFraction)),
                    verticalArrangement = Arrangement.spacedBy(dynamicDp(Spacing.s, 2.dp, collapseFraction))
                ) {
                    HeaderSection(
                        topItems = topItems,
                        onOpenSettings = onOpenSettings,
                        collapseFraction = collapseFraction
                    )
                }
            }

            HomeContent(
                listState = listState,
                nextCountdown = nextCountdown,
                customItems = displayedItems,
                visibleItems = visibleItems,
                progressDisplayMode = progressDisplayMode,
                searchQuery = searchQuery,
                selectedSort = selectedSort,
                onQueryChange = { searchQuery = it },
                onSortChange = {
                    selectedSortValue = it.name
                    onSortChange(it.name)
                },
                isGrid = isGrid,
                onGridChange = { enabled ->
                    val value = if (enabled) {
                        UserPreferences.HOME_LAYOUT_GRID
                    } else {
                        UserPreferences.HOME_LAYOUT_LIST
                    }
                    selectedLayoutValue = value
                    onLayoutChange(value)
                },
                onAddTime = onAddTime,
                onAddDate = onAddDate,
                onEditItem = onEditItem,
                onDeleteItem = onDeleteItem,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun HomeContent(
    listState: androidx.compose.foundation.lazy.grid.LazyGridState,
    nextCountdown: AdapterItem?,
    customItems: List<AdapterItem>,
    visibleItems: List<AdapterItem>,
    progressDisplayMode: String,
    searchQuery: String,
    selectedSort: HomeSortMode,
    onQueryChange: (String) -> Unit,
    onSortChange: (HomeSortMode) -> Unit,
    isGrid: Boolean,
    onGridChange: (Boolean) -> Unit,
    onAddTime: () -> Unit,
    onAddDate: () -> Unit,
    onEditItem: (Int) -> Unit,
    onDeleteItem: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(if (isGrid) 2 else 1),
        state = listState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            start = Spacing.l,
            top = Spacing.m,
            end = Spacing.l,
            bottom = 88.dp
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.m),
        horizontalArrangement = Arrangement.spacedBy(Spacing.m)
    ) {
        if (nextCountdown != null) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                NextCountdownHero(
                    item = nextCountdown,
                    progressDisplayMode = progressDisplayMode
                )
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            SectionHeader(
                title = stringResource(R.string.home_my_items),
                count = if (customItems.isNotEmpty()) visibleItems.size else null,
                isGrid = isGrid,
                onGridChange = onGridChange
            )
        }

        if (customItems.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                EmptyItemState(
                    onAddTime = onAddTime,
                    onAddDate = onAddDate
                )
            }
        } else {
            item(span = { GridItemSpan(maxLineSpan) }) {
                SearchAndSortSection(
                    query = searchQuery,
                    selectedSort = selectedSort,
                    onQueryChange = onQueryChange,
                    onSortChange = onSortChange
                )
            }
            if (visibleItems.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = stringResource(R.string.home_empty_search),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(visibleItems, key = { it.id }) { item ->
                    TimeLeftItemCard(
                        item = item,
                        onEditItem = onEditItem,
                        onDeleteItem = onDeleteItem,
                        grid = isGrid
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    count: Int?,
    isGrid: Boolean,
    onGridChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
        if (count != null) {
            Text(
                text = stringResource(R.string.home_item_count, count),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(
            modifier = Modifier.padding(start = Spacing.s),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            LayoutModeButton(
                selected = !isGrid,
                onClick = { onGridChange(false) },
                icon = Icons.AutoMirrored.Filled.ViewList,
                contentDescription = stringResource(R.string.home_layout_list)
            )
            LayoutModeButton(
                selected = isGrid,
                onClick = { onGridChange(true) },
                icon = Icons.Filled.GridView,
                contentDescription = stringResource(R.string.home_layout_grid)
            )
        }
    }
}

@Composable
private fun LayoutModeButton(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
) {
    Surface(
        shape = MaterialTheme.shapes.extraSmall,
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        }
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@Composable
private fun SearchAndSortSection(
    query: String,
    selectedSort: HomeSortMode,
    onQueryChange: (String) -> Unit,
    onSortChange: (HomeSortMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null
                )
            },
            placeholder = { Text(stringResource(R.string.home_search_hint)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.s),
            horizontalArrangement = Arrangement.spacedBy(Spacing.s)
        ) {
            HomeSortMode.values().forEach { mode ->
                FilterChip(
                    selected = selectedSort == mode,
                    onClick = { onSortChange(mode) },
                    label = {
                        Text(
                            text = stringResource(mode.labelRes),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

private enum class HomeSortMode(val labelRes: Int) {
    Nearest(R.string.home_sort_nearest),
    Created(R.string.home_sort_created),
    Title(R.string.home_sort_title),
    Progress(R.string.home_sort_progress)
}

private const val HEADER_COLLAPSE_THRESHOLD_PX = 32
