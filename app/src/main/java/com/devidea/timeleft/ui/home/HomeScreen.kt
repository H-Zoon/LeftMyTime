package com.devidea.timeleft.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.devidea.timeleft.AdapterItem
import com.devidea.timeleft.R
import com.devidea.timeleft.database.itemdata.ItemType
import com.devidea.timeleft.preferences.UserPreferences
import com.devidea.timeleft.ui.components.remainingTimeLabel
import com.devidea.timeleft.ui.components.TimeLeftUnderlineTextField
import com.devidea.timeleft.ui.theme.LayoutTokens
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
    initialShowAll: Boolean = false,
) {
    var showAll by rememberSaveable { mutableStateOf(initialShowAll) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedSortValue by rememberSaveable(initialSortValue) { mutableStateOf(initialSortValue) }
    var selectedLayoutValue by rememberSaveable(initialLayoutValue) { mutableStateOf(initialLayoutValue) }
    var selectedActiveId by rememberSaveable { mutableStateOf<Int?>(null) }
    var showActiveMenu by remember { mutableStateOf(false) }
    val selectedSort = runCatching { HomeSortMode.valueOf(selectedSortValue) }.getOrDefault(HomeSortMode.Nearest)
    val isGrid = selectedLayoutValue == UserPreferences.HOME_LAYOUT_GRID
    // Keep the saved grid preference, but use one column when text cannot fit two.
    val compactToolbar = LocalConfiguration.current.screenWidthDp < 360 && LocalDensity.current.fontScale > 1.2f
    val columns = if (showAll && isGrid && LocalConfiguration.current.screenWidthDp >= 360 && LocalDensity.current.fontScale <= 1.15f) 2 else 1
    val displayedItems = customItems.filterNot { expiredItemsMode == UserPreferences.EXPIRED_ITEMS_HIDE && it.isExpired }
    val activeItems = activeTimeItems(displayedItems)
    val hero = selectHomeHero(displayedItems, selectedActiveId)
    val activeHeroId = hero?.takeIf { it.type == ItemType.Time }?.id
    LaunchedEffect(activeHeroId) { selectedActiveId = activeHeroId }
    val upcomingItems = upcomingTimeItems(displayedItems)
    val dateItems = homeDateItems(displayedItems).filterNot { it.id == hero?.id }
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
                    } else {
                        ScheduleAddButton(onAddTime, onAddDate, showText = false)
                    }
                    IconButton(onClick = onOpenSettings) { Icon(Icons.Default.Settings, stringResource(R.string.home_open_settings), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
            if (!showAll) {
                item(key = "period", span = { GridItemSpan(maxLineSpan) }) {
                    HeaderSection(topItems, progressDisplayMode)
                }
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
                if (displayedItems.isEmpty()) {
                    item(key = "empty", span = { GridItemSpan(maxLineSpan) }) { EmptyItemState(onAddTime, onAddDate) }
                } else {
                    item(key = "upcoming-heading", span = { GridItemSpan(maxLineSpan) }) {
                        Column {
                            if (hero != null) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            ScheduleListHeading(onAddTime, onAddDate)
                        }
                    }
                    if (upcomingItems.isEmpty() && dateItems.isEmpty()) item(key = "no-next", span = { GridItemSpan(maxLineSpan) }) {
                        Text(stringResource(R.string.home_no_next_schedule), style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = Spacing.l))
                    }
                    if (upcomingItems.isNotEmpty()) item(key = "time-heading", span = { GridItemSpan(maxLineSpan) }) {
                        ScheduleGroupHeading(stringResource(R.string.home_time_ranges))
                    }
                    items(upcomingItems.take(3), key = { "next-${it.id}" }, span = { GridItemSpan(maxLineSpan) }) { item ->
                        TimeLeftItemCard(item, onEditItem, onDeleteItem, upcoming = true, progressDisplayMode = progressDisplayMode)
                    }
                    if (dateItems.isNotEmpty()) item(key = "date-heading", span = { GridItemSpan(maxLineSpan) }) {
                        ScheduleGroupHeading(stringResource(R.string.home_date_schedules))
                    }
                    items(dateItems.take(3), key = { "date-${it.id}" }, span = { GridItemSpan(maxLineSpan) }) { item ->
                        TimeLeftItemCard(item, onEditItem, onDeleteItem, progressDisplayMode = progressDisplayMode)
                    }
                }
            } else {
                item(key = "controls", span = { GridItemSpan(maxLineSpan) }) {
                    Column(Modifier.padding(bottom = Spacing.page), verticalArrangement = Arrangement.spacedBy(Spacing.page)) {
                        SectionHeader(stringResource(R.string.home_my_items), visibleItems.size, isGrid, onGridChange = { grid ->
                            selectedLayoutValue = if (grid) UserPreferences.HOME_LAYOUT_GRID else UserPreferences.HOME_LAYOUT_LIST
                            onLayoutChange(selectedLayoutValue)
                        })
                        SearchAndSortSection(searchQuery, selectedSort, { searchQuery = it }, { selectedSortValue = it.name; onSortChange(it.name) })
                    }
                }
                if (displayedItems.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) { EmptyItemState(onAddTime, onAddDate) }
                else if (visibleItems.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) { Text(stringResource(R.string.home_empty_search)) }
                items(visibleItems, key = { it.id }) { item ->
                    TimeLeftItemCard(item, onEditItem, onDeleteItem, grid = columns == 2, progressDisplayMode = progressDisplayMode)
                }
            }
            item(key = "bottom-space", span = { GridItemSpan(maxLineSpan) }) { Spacer(Modifier.height(Spacing.xxl)) }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun ScheduleListHeading(onAddTime: () -> Unit, onAddDate: () -> Unit) {
    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(stringResource(R.string.home_next_schedules), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterVertically).padding(end = Spacing.m))
        ScheduleAddButton(onAddTime, onAddDate, showText = true)
    }
}

@Composable
private fun ScheduleGroupHeading(title: String) {
    Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = Spacing.l, bottom = Spacing.s))
}

@Composable
private fun ScheduleAddButton(onAddTime: () -> Unit, onAddDate: () -> Unit, showText: Boolean) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        if (showText) {
            TextButton(onClick = { expanded = true }, modifier = Modifier.heightIn(min = LayoutTokens.MinTouchTarget)) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(Spacing.xs))
                Text(stringResource(R.string.home_add_schedule))
            }
        } else {
            IconButton(onClick = { expanded = true }) { Icon(Icons.Default.Add, stringResource(R.string.home_add_item)) }
        }
        DropdownMenu(expanded, { expanded = false }) {
            DropdownMenuItem(text = { Text(stringResource(R.string.home_add_time_range)) }, onClick = { expanded = false; onAddTime() })
            DropdownMenuItem(text = { Text(stringResource(R.string.home_add_date)) }, onClick = { expanded = false; onAddDate() })
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun SectionHeader(
    title: String,
    count: Int?,
    isGrid: Boolean,
    onGridChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(Spacing.m)
    ) {
        Row(
            modifier = Modifier.align(Alignment.CenterVertically).padding(end = Spacing.l),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.s)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            if (count != null) {
                Text(
                    text = pluralStringResource(R.plurals.home_item_count, count, count),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Row(
            modifier = Modifier.selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.s)
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
            modifier = Modifier.size(LayoutTokens.MinTouchTarget).semantics { this.selected = selected }
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
    val focusManager = LocalFocusManager.current
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.l)) {
        TimeLeftUnderlineTextField(
            value = query,
            onValueChange = onQueryChange,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null
                )
            },
            label = { Text(stringResource(R.string.home_search_hint)) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
            modifier = Modifier.fillMaxWidth()
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.s),
            verticalArrangement = Arrangement.spacedBy(Spacing.s)
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
