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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.devidea.timeleft.AdapterItem
import com.devidea.timeleft.R
import com.devidea.timeleft.preferences.UserPreferences

@Composable
fun HomeScreen(
    themeMode: String,
    initialSortValue: String,
    initialTabValue: String,
    expiredItemsMode: String,
    progressDisplayMode: String,
    topItems: List<AdapterItem>,
    customItems: List<AdapterItem>,
    onToggleTheme: () -> Unit,
    onOpenSettings: () -> Unit,
    onSortChange: (String) -> Unit,
    onAddTime: () -> Unit,
    onAddDate: () -> Unit,
    onEditItem: (Int) -> Unit,
    onDeleteItem: (Int) -> Unit,
) {
    var fabExpanded by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedSortValue by rememberSaveable(initialSortValue) { mutableStateOf(initialSortValue) }
    var selectedTabValue by rememberSaveable { mutableStateOf(initialTabValue) }
    val selectedSort = remember(selectedSortValue) {
        runCatching { HomeSortMode.valueOf(selectedSortValue) }.getOrDefault(HomeSortMode.Nearest)
    }
    val selectedTab = remember(selectedTabValue) {
        runCatching { HomeMainTab.valueOf(selectedTabValue) }.getOrDefault(HomeMainTab.Overview)
    }
    val overviewListState = rememberLazyListState()
    val itemsListState = rememberLazyListState()
    val activeListState = if (selectedTab == HomeMainTab.Overview) overviewListState else itemsListState
    val targetCollapseFraction by remember(activeListState) {
        derivedStateOf {
            if (activeListState.firstVisibleItemIndex > 0 ||
                activeListState.firstVisibleItemScrollOffset >= HEADER_COLLAPSE_THRESHOLD_PX
            ) {
                1f
            } else {
                0f
            }
        }
    }
    val collapseFraction by animateFloatAsState(
        targetValue = targetCollapseFraction,
        animationSpec = tween(durationMillis = 180),
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
    val upcomingItems = remember(displayedItems, nextCountdown) {
        val nextId = nextCountdown?.id
        displayedItems
            .filterNot { it.isExpired }
            .filterNot { it.id == nextId }
            .sortedWith(compareBy<AdapterItem> { it.remainingSortKey }.thenBy { it.id })
            .take(3)
            .ifEmpty { displayedItems.filterNot { it.id == nextId }.take(3) }
    }

    Scaffold(
        bottomBar = {
            HomeBottomBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTabValue = it.name }
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AnimatedVisibility(
                    visible = fabExpanded,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
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
            if (selectedTab == HomeMainTab.Overview) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = dynamicDp(2.dp, 1.dp, collapseFraction)
                ) {
                    Column(
                        modifier = Modifier.padding(bottom = dynamicDp(12.dp, 6.dp, collapseFraction)),
                        verticalArrangement = Arrangement.spacedBy(dynamicDp(6.dp, 2.dp, collapseFraction))
                    ) {
                        HeaderSection(
                            themeMode = themeMode,
                            todayItem = topItems.firstOrNull(),
                            onToggleTheme = onToggleTheme,
                            onOpenSettings = onOpenSettings,
                            collapseFraction = collapseFraction
                        )
                        SummarySection(
                            topItems = topItems,
                            collapseFraction = collapseFraction
                        )
                    }
                }
            }

            if (selectedTab == HomeMainTab.Overview) {
                OverviewTabContent(
                    listState = overviewListState,
                    nextCountdown = nextCountdown,
                    upcomingItems = upcomingItems,
                    customItemsEmpty = displayedItems.isEmpty(),
                    progressDisplayMode = progressDisplayMode,
                    onAddTime = onAddTime,
                    onAddDate = onAddDate,
                    onEditItem = onEditItem,
                    onDeleteItem = onDeleteItem,
                    modifier = Modifier.weight(1f)
                )
            } else {
                ItemsTabContent(
                    listState = itemsListState,
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
                    onAddTime = onAddTime,
                    onAddDate = onAddDate,
                    onEditItem = onEditItem,
                    onDeleteItem = onDeleteItem,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun HomeBottomBar(
    selectedTab: HomeMainTab,
    onTabSelected: (HomeMainTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        HomeMainTab.values().forEach { tab ->
            NavigationBarItem(
                selected = selectedTab == tab,
                onClick = { onTabSelected(tab) },
                icon = {
                    Icon(
                        imageVector = when (tab) {
                            HomeMainTab.Overview -> Icons.Filled.Home
                            HomeMainTab.Items -> Icons.AutoMirrored.Filled.FormatListBulleted
                        },
                        contentDescription = stringResource(tab.labelRes)
                    )
                }
            )
        }
    }
}

@Composable
private fun OverviewTabContent(
    listState: androidx.compose.foundation.lazy.LazyListState,
    nextCountdown: AdapterItem?,
    upcomingItems: List<AdapterItem>,
    customItemsEmpty: Boolean,
    progressDisplayMode: String,
    onAddTime: () -> Unit,
    onAddDate: () -> Unit,
    onEditItem: (Int) -> Unit,
    onDeleteItem: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(top = 14.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (nextCountdown != null) {
            item {
                NextCountdownHero(
                    item = nextCountdown,
                    progressDisplayMode = progressDisplayMode,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        if (customItemsEmpty) {
            item {
                EmptyItemState(
                    onAddTime = onAddTime,
                    onAddDate = onAddDate,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        } else if (upcomingItems.isNotEmpty()) {
            item {
                SectionHeader(
                    title = stringResource(R.string.home_upcoming_items),
                    count = upcomingItems.size.takeIf { it > 0 },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            items(upcomingItems, key = { it.id }) { item ->
                TimeLeftItemCard(
                    item = item,
                    progressDisplayMode = progressDisplayMode,
                    onEditItem = onEditItem,
                    onDeleteItem = onDeleteItem,
                    compact = true,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun ItemsTabContent(
    listState: androidx.compose.foundation.lazy.LazyListState,
    customItems: List<AdapterItem>,
    visibleItems: List<AdapterItem>,
    progressDisplayMode: String,
    searchQuery: String,
    selectedSort: HomeSortMode,
    onQueryChange: (String) -> Unit,
    onSortChange: (HomeSortMode) -> Unit,
    onAddTime: () -> Unit,
    onAddDate: () -> Unit,
    onEditItem: (Int) -> Unit,
    onDeleteItem: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(top = 16.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionHeader(
                title = stringResource(R.string.home_my_items),
                count = if (customItems.isNotEmpty()) visibleItems.size else null,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        if (customItems.isEmpty()) {
            item {
                EmptyItemState(
                    onAddTime = onAddTime,
                    onAddDate = onAddDate,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        } else {
            item {
                SearchAndSortSection(
                    query = searchQuery,
                    selectedSort = selectedSort,
                    onQueryChange = onQueryChange,
                    onSortChange = onSortChange,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            if (visibleItems.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.home_empty_search),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            } else {
                items(visibleItems, key = { it.id }) { item ->
                    TimeLeftItemCard(
                        item = item,
                        progressDisplayMode = progressDisplayMode,
                        onEditItem = onEditItem,
                        onDeleteItem = onDeleteItem,
                        modifier = Modifier.padding(horizontal = 16.dp)
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
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )
        if (count != null) {
            Text(
                text = stringResource(R.string.home_item_count, count),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            HomeSortMode.values().forEach { mode ->
                FilterChip(
                    selected = selectedSort == mode,
                    onClick = { onSortChange(mode) },
                    label = { Text(stringResource(mode.labelRes)) },
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

private enum class HomeMainTab(val labelRes: Int) {
    Overview(R.string.home_tab_overview),
    Items(R.string.home_tab_items)
}

private const val HEADER_COLLAPSE_THRESHOLD_PX = 32
