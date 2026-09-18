package com.devidea.timeleft.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.unit.dp
import com.devidea.timeleft.AdapterItem
import com.devidea.timeleft.R
import com.devidea.timeleft.preferences.UserPreferences
import com.devidea.timeleft.ui.components.remainingTimeLabel
import com.devidea.timeleft.ui.theme.LayoutTokens
import com.devidea.timeleft.ui.theme.Spacing

@Composable
fun HomeScreen(
    initialSortValue: String,
    initialLayoutValue: String,
    headerItemIndex: Int,
    expiredItemsMode: String,
    progressDisplayMode: String,
    topItems: List<AdapterItem>,
    customItems: List<AdapterItem>,
    onOpenSettings: () -> Unit,
    onSortChange: (String) -> Unit,
    onLayoutChange: (String) -> Unit,
    onHeaderItemChange: (Int) -> Unit,
    onAddTime: () -> Unit,
    onAddDate: () -> Unit,
    onEditItem: (Int) -> Unit,
    onDeleteItem: (Int) -> Unit,
    initialShowAll: Boolean = false,
) {
    var showAll by rememberSaveable { mutableStateOf(initialShowAll) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedSortValue by rememberSaveable(initialSortValue) { mutableStateOf(initialSortValue) }
    var selectedLayoutValue by rememberSaveable(initialLayoutValue) { mutableStateOf(initialLayoutValue) }
    var selectedActiveId by rememberSaveable { mutableStateOf<Int?>(null) }
    var showActiveMenu by remember { mutableStateOf(false) }
    var showAddMenu by remember { mutableStateOf(false) }
    val selectedSort = runCatching { HomeSortMode.valueOf(selectedSortValue) }.getOrDefault(HomeSortMode.Nearest)
    val isGrid = selectedLayoutValue == UserPreferences.HOME_LAYOUT_GRID
    // Keep the saved grid preference, but use one column when text cannot fit two.
    val compactToolbar = LocalConfiguration.current.screenWidthDp < 360 && LocalDensity.current.fontScale > 1.2f
    val columns = if (showAll && isGrid && LocalConfiguration.current.screenWidthDp >= 360 && LocalDensity.current.fontScale <= 1.15f) 2 else 1
    val displayedItems = customItems.filterNot { expiredItemsMode == UserPreferences.EXPIRED_ITEMS_HIDE && it.isExpired }
    val activeItems = activeTimeItems(displayedItems)
    val hero = selectActiveTimeItem(displayedItems, selectedActiveId)
    LaunchedEffect(hero?.id) { selectedActiveId = hero?.id }
    val upcomingItems = upcomingTimeItems(displayedItems)
    val visibleItems = displayedItems.filter {
        searchQuery.isBlank() || it.title.contains(searchQuery.trim(), true) || it.category.contains(searchQuery.trim(), true)
    }.let { items ->
        val sorted = when (selectedSort) {
            HomeSortMode.Nearest -> items.sortedWith(compareBy<AdapterItem> { it.remainingSortKey }.thenBy { it.id })
            HomeSortMode.Created -> items.sortedByDescending { it.id }
            HomeSortMode.Title -> items.sortedBy { it.title.lowercase() }
            HomeSortMode.Progress -> items.sortedByDescending { it.percent }
        }
        if (expiredItemsMode == UserPreferences.EXPIRED_ITEMS_BOTTOM) sorted.sortedBy { it.isExpired } else sorted
    }
    val scrollState = rememberLazyGridState()
    LaunchedEffect(showAll) { scrollState.scrollToItem(0) }
    BackHandler(showAll) { showAll = false }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            state = scrollState,
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(horizontal = LayoutTokens.ScreenHorizontal, vertical = Spacing.s),
            horizontalArrangement = Arrangement.spacedBy(Spacing.l)
        ) {
            item(key = "toolbar", span = { GridItemSpan(maxLineSpan) }) {
                Row(Modifier.fillMaxWidth().padding(bottom = Spacing.l), verticalAlignment = Alignment.CenterVertically) {
                    if (showAll) {
                        IconButton(onClick = { showAll = false }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back))
                        }
                    }
                    Text(
                        stringResource(if (showAll) R.string.home_all_items else R.string.app_name),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    if (!showAll) {
                        if (compactToolbar) IconButton(onClick = { showAll = true }) {
                            Icon(Icons.AutoMirrored.Filled.ViewList, stringResource(R.string.home_all_items))
                        } else TextButton(onClick = { showAll = true }) { Text(stringResource(R.string.home_all_items)) }
                    } else Box {
                        IconButton(onClick = { showAddMenu = true }) { Icon(Icons.Default.Add, stringResource(R.string.home_add_item)) }
                        DropdownMenu(showAddMenu, { showAddMenu = false }) {
                            DropdownMenuItem(text = { Text(stringResource(R.string.home_add_time_range)) }, onClick = { showAddMenu = false; onAddTime() })
                            DropdownMenuItem(text = { Text(stringResource(R.string.home_add_date)) }, onClick = { showAddMenu = false; onAddDate() })
                        }
                    }
                    IconButton(onClick = onOpenSettings) { Icon(Icons.Default.Settings, stringResource(R.string.home_open_settings), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
            if (!showAll) {
                if (hero != null) {
                    item(key = "hero", span = { GridItemSpan(maxLineSpan) }) {
                        Column {
                            if (activeItems.size > 1) Box {
                                TextButton(onClick = { showActiveMenu = true }) {
                                    Text(pluralStringResource(R.plurals.home_active_count, activeItems.size, activeItems.size))
                                    Icon(Icons.Default.KeyboardArrowDown, null)
                                }
                                DropdownMenu(showActiveMenu, { showActiveMenu = false }) {
                                    activeItems.forEach { item ->
                                        DropdownMenuItem(text = { Text(item.title + " · " + remainingTimeLabel(item.remainingSeconds, null)) }, onClick = { selectedActiveId = item.id; showActiveMenu = false })
                                    }
                                }
                            }
                            NextCountdownHero(hero, progressDisplayMode, onEditItem = onEditItem)
                        }
                    }
                }
                item(key = "period", span = { GridItemSpan(maxLineSpan) }) {
                    HeaderSection(topItems, headerItemIndex, onHeaderItemChange, prominent = hero == null)
                }
                item(key = "upcoming-heading", span = { GridItemSpan(maxLineSpan) }) {
                    Column {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.home_next_ranges), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                            TextButton(onClick = onAddTime) { Icon(Icons.Default.Add, null, Modifier.size(18.dp)); Text(stringResource(R.string.home_add_range)) }
                        }
                    }
                }
                if (displayedItems.isEmpty()) {
                    item(key = "empty", span = { GridItemSpan(maxLineSpan) }) { EmptyItemState(onAddTime, onAddDate) }
                } else {
                    if (upcomingItems.isEmpty()) item(key = "no-next", span = { GridItemSpan(maxLineSpan) }) {
                        Text(stringResource(R.string.home_no_next_range), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = Spacing.l))
                    }
                    items(upcomingItems.take(3), key = { "next-${it.id}" }, span = { GridItemSpan(maxLineSpan) }) { item ->
                        TimeLeftItemCard(item, onEditItem, onDeleteItem, upcoming = true)
                    }
                    item(key = "manage", span = { GridItemSpan(maxLineSpan) }) {
                        TextButton(onClick = { showAll = true }, modifier = Modifier.fillMaxWidth().padding(top = Spacing.m)) {
                            Text(pluralStringResource(R.plurals.home_view_all_count, displayedItems.size, displayedItems.size))
                        }
                    }
                }
            } else {
                item(key = "controls", span = { GridItemSpan(maxLineSpan) }) {
                    Column {
                        SectionHeader(stringResource(R.string.home_my_items), visibleItems.size, isGrid, onGridChange = { grid ->
                            selectedLayoutValue = if (grid) UserPreferences.HOME_LAYOUT_GRID else UserPreferences.HOME_LAYOUT_LIST
                            onLayoutChange(selectedLayoutValue)
                        })
                        SearchAndSortSection(searchQuery, selectedSort, { searchQuery = it }, { selectedSortValue = it.name; onSortChange(it.name) })
                        Spacer(Modifier.height(Spacing.l))
                    }
                }
                if (displayedItems.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) { EmptyItemState(onAddTime, onAddDate) }
                else if (visibleItems.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) { Text(stringResource(R.string.home_empty_search)) }
                items(visibleItems, key = { it.id }) { item -> TimeLeftItemCard(item, onEditItem, onDeleteItem, grid = columns == 2) }
            }
            item(key = "bottom-space", span = { GridItemSpan(maxLineSpan) }) { Spacer(Modifier.height(Spacing.xxl)) }
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
                text = pluralStringResource(R.plurals.home_item_count, count, count),
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
            modifier = Modifier.size(LayoutTokens.MinTouchTarget)
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
@OptIn(ExperimentalLayoutApi::class)
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
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.s),
            horizontalArrangement = Arrangement.spacedBy(Spacing.s),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            HomeSortMode.values().forEach { mode ->
                FilterChip(
                    selected = selectedSort == mode,
                    onClick = { onSortChange(mode) },
                    label = {
                        Text(
                            text = stringResource(mode.labelRes)
                        )
                    }
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
