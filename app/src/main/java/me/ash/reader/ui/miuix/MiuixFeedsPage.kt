package me.ash.reader.ui.miuix

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.RssFeed
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import me.ash.reader.R
import me.ash.reader.domain.model.feed.Feed
import me.ash.reader.domain.model.group.GroupWithFeed
import me.ash.reader.domain.model.general.Filter
import me.ash.reader.ui.component.FeedIcon
import me.ash.reader.ui.page.home.feeds.subscribe.SubscribeDialog
import me.ash.reader.ui.ext.collectAsStateValue
import me.ash.reader.ui.page.home.feeds.FeedsViewModel
import me.ash.reader.ui.page.home.feeds.subscribe.SubscribeViewModel


/**
 * Feeds home, rebuilt as a modern "mesh gradient + circular shortcuts + bento grid" layout.
 *
 * The Material implementation is untouched: this screen only reads the same [FeedsViewModel].
 */
@Composable
fun MiuixFeedsPage(
    feedsViewModel: FeedsViewModel = hiltViewModel(),
    subscribeViewModel: SubscribeViewModel = hiltViewModel(),
    navigateToSettings: () -> Unit = {},
    navigationToFlow: () -> Unit = {},
) {
    val feedsUiState = feedsViewModel.feedsUiState.collectAsStateValue()
    val filterState = feedsViewModel.filterStateFlow.collectAsStateValue()
    val groupWithFeedList: List<GroupWithFeed> =
        feedsViewModel.groupWithFeedsListFlow.collectAsStateValue()

    val owner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var isSyncing by remember { mutableStateOf(false) }
    DisposableEffect(owner) {
        feedsViewModel.syncWorkLiveData.observe(owner) { workInfoList ->
            isSyncing = workInfoList.any { it.state == androidx.work.WorkInfo.State.RUNNING }
        }
        val lifecycleObserver =
            androidx.lifecycle.LifecycleEventObserver { _, event ->
                if (
                    event == androidx.lifecycle.Lifecycle.Event.ON_PAUSE ||
                        event == androidx.lifecycle.Lifecycle.Event.ON_STOP
                ) {
                    feedsViewModel.commitDiffs()
                }
            }
        owner.lifecycle.addObserver(lifecycleObserver)
        onDispose {
            feedsViewModel.syncWorkLiveData.removeObservers(owner)
            owner.lifecycle.removeObserver(lifecycleObserver)
        }
    }

    ProvideMiuixSeed(rememberWallpaperSeed())

    val gridState = rememberLazyGridState()
    val feedCount = remember(groupWithFeedList) { groupWithFeedList.sumOf { it.feeds.size } }
    var expandedGroup by remember { mutableStateOf<GroupWithFeed?>(null) }

    val tabs =
        listOf(
            stringResource(R.string.all),
            stringResource(R.string.unread),
            stringResource(R.string.starred),
        )
    val selectedTab =
        when {
            filterState.filter.isUnread() -> 1
            filterState.filter.isStarred() -> 2
            else -> 0
        }

    Box(modifier = Modifier.fillMaxSize()) {
        MiuixSlateBackground()

        val pagerState = rememberPagerState(initialPage = selectedTab) { 3 }

        // Swiping the content follows the finger; the capsule mirrors the page.
        LaunchedEffect(pagerState.currentPage) {
            val page = pagerState.currentPage
            if (page != selectedTab) {
                feedsViewModel.changeFilter(
                    filterState.copy(
                        filter =
                            when (page) {
                                1 -> Filter.Unread
                                2 -> Filter.Starred
                                else -> Filter.All
                            }
                    )
                )
            }
        }
        LaunchedEffect(selectedTab) {
            if (pagerState.currentPage != selectedTab) pagerState.animateScrollToPage(selectedTab)
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // Header block is composed once — previously each pager page re-composed the title,
            // circular shortcuts and section label, tripling the work per frame.
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                MiuixFeedsHeader(
                    title = feedsUiState.account?.name ?: stringResource(R.string.feeds),
                    onSubscribe = {
                        if (subscribeViewModel.rssService.get().addSubscription) {
                            subscribeViewModel.showDrawer()
                        }
                    },
                    onSettings = navigateToSettings,
                )
            }
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                MiuixQuickEntries(
                    allCount = feedsUiState.importantSum,
                    unreadCount = feedsUiState.importantSum,
                    starredCount = "",
                    feedCount = feedCount,
                    onAll = {
                        feedsViewModel.changeFilter(filterState.copy(group = null, feed = null))
                        navigationToFlow()
                    },
                    onUnread = {
                        feedsViewModel.changeFilter(
                            filterState.copy(group = null, feed = null, filter = Filter.Unread)
                        )
                        navigationToFlow()
                    },
                    onStarred = {
                        feedsViewModel.changeFilter(
                            filterState.copy(group = null, feed = null, filter = Filter.Starred)
                        )
                        navigationToFlow()
                    },
                    onFeeds = {
                        feedsViewModel.changeFilter(filterState.copy(group = null, feed = null))
                    },
                )
            }
            Text(
                text = stringResource(R.string.feeds),
                color = slateText(),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 6.dp),
            )

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                beyondViewportPageCount = 1,
            ) { _ ->
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding =
                        PaddingValues(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 148.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (groupWithFeedList.isEmpty()) {
                        item(span = { GridItemSpan(2) }) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = stringResource(R.string.empty),
                                    color = Slate.muted,
                                    fontSize = 14.sp,
                                )
                            }
                        }
                    }

                    items(groupWithFeedList, key = { it.group?.id ?: -1 }) { groupWithFeed ->
                        MiuixBentoTile(
                            title = groupWithFeed.group?.name ?: stringResource(R.string.all_articles),
                            subtitle = "${groupWithFeed.feeds.size} 个订阅",
                            icon =
                                if (groupWithFeed.group == null) Icons.Rounded.Inbox
                                else Icons.Outlined.Folder,
                            badge =
                                groupWithFeed.feeds.sumOf { it.important }.takeIf { it > 0 }?.toString(),
                            onClick = {
                                if (groupWithFeed.group == null) {
                                    feedsViewModel.changeFilter(
                                        filterState.copy(group = null, feed = null)
                                    )
                                    navigationToFlow()
                                } else {
                                    expandedGroup = groupWithFeed
                                }
                            },
                        )
                    }
                }
            }
        }

        // Group expansion sheet
        androidx.activity.compose.BackHandler(enabled = expandedGroup != null) {
            expandedGroup = null
        }

        expandedGroup?.let { group ->
            MiuixGroupSheet(
                group = group,
                onDismiss = { expandedGroup = null },
                onGroupClick = {
                    expandedGroup = null
                    feedsViewModel.changeFilter(filterState.copy(group = group.group, feed = null))
                    navigationToFlow()
                },
                onFeedClick = { feed ->
                    expandedGroup = null
                    feedsViewModel.changeFilter(
                        filterState.copy(group = null, feed = feed, filter = Filter.All)
                    )
                    navigationToFlow()
                },
            )
        }

        if (expandedGroup == null) {
        Box(
            modifier =
                Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 44.dp)
        ) {
            MiuixFilterCapsule(
                tabs = tabs,
                selectedTabIndex = selectedTab,
                onTabSelected = { index ->
                    val filter =
                        when (index) {
                            1 -> Filter.Unread
                            2 -> Filter.Starred
                            else -> Filter.All
                        }
                    feedsViewModel.changeFilter(filterState.copy(filter = filter))
                },
            )
        }

        }

        SubscribeDialog(subscribeViewModel = subscribeViewModel)
    }
}

/** Transparent top bar: no background plate, so the mesh gradient runs edge to edge. */
@Composable
private fun MiuixFeedsHeader(
    title: String,
    onSubscribe: () -> Unit,
    onSettings: () -> Unit,
) {
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 10.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            color = slateText(),
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onSubscribe) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = stringResource(R.string.subscribe),
                tint = slateText(),
                modifier = Modifier.size(22.dp),
            )
        }
        IconButton(onClick = onSettings) {
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = stringResource(R.string.settings),
                tint = slateText(),
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

/** Four circular shortcuts replacing the old full-width "all articles" card. */
@Composable
private fun MiuixQuickEntries(
    allCount: String,
    unreadCount: String,
    starredCount: String,
    feedCount: Int,
    onAll: () -> Unit,
    onUnread: () -> Unit,
    onStarred: () -> Unit,
    onFeeds: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        MiuixQuickEntry(
            label = stringResource(R.string.all),
            subtitle = allCount,
            icon = Icons.Rounded.Inbox,
            accent = Slate.accent,
            onClick = onAll,
            modifier = Modifier.weight(1f),
        )
        MiuixQuickEntry(
            label = stringResource(R.string.unread),
            subtitle = unreadCount,
            icon = Icons.Rounded.RssFeed,
            accent = MiuixAccentSky,
            onClick = onUnread,
            modifier = Modifier.weight(1f),
        )
        MiuixQuickEntry(
            label = stringResource(R.string.starred),
            subtitle = starredCount,
            icon = Icons.Rounded.Bookmark,
            accent = MiuixAccentCoral,
            onClick = onStarred,
            modifier = Modifier.weight(1f),
        )
        MiuixQuickEntry(
            label = "订阅源",
            subtitle = "$feedCount 个",
            icon = Icons.Outlined.Folder,
            accent = Slate.muted,
            onClick = onFeeds,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun MiuixQuickEntry(
    label: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier =
                Modifier.size(64.dp)
                    .clip(CircleShape)
                    .background(slateSunken())
                    .border(1.dp, Color.White.copy(alpha = 0.95f), CircleShape)
                    .border(1.2.dp, accent.copy(alpha = 0.75f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = accent,
                modifier = Modifier.size(26.dp),
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            color = slateText(),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = subtitle,
            color = Slate.muted,
            fontSize = 12.sp,
            maxLines = 1,
        )
    }
}

/** One bento tile: title + icon top-left, count bottom-left, breathing room in between. */
@Composable
private fun MiuixBentoTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    badge: String?,
    onClick: () -> Unit,
) {
    Column(
        modifier =
            Modifier.fillMaxWidth()
                .height(108.dp)
                .shadow(2.dp, RoundedCornerShape(22.dp), clip = false)
                .clip(RoundedCornerShape(22.dp))
                .background(slateRaised())
                .border(1.dp, Color.White.copy(alpha = 0.95f), RoundedCornerShape(22.dp))
                .clickable(onClick = onClick)
                .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier =
                    Modifier.size(30.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MiuixAccentSky.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MiuixAccentSky,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                color = slateText(),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = subtitle,
                color = Slate.muted,
                fontSize = 13.sp,
                modifier = Modifier.weight(1f),
            )
            if (badge != null) {
                Text(
                    text = badge,
                    color = MiuixAccentSky,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

/** Bottom sheet listing the feeds of a group. */
@Composable
private fun androidx.compose.foundation.layout.BoxScope.MiuixGroupSheet(
    group: GroupWithFeed,
    onDismiss: () -> Unit,
    onGroupClick: () -> Unit,
    onFeedClick: (Feed) -> Unit,
) {
    Box(
        modifier =
            Modifier.fillMaxSize()
                .background(Color.Black.copy(alpha = 0.28f))
                .clickable(onClick = onDismiss)
    )
    Column(
        modifier =
            Modifier.align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(12.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(
                    slateRaised()
                )
                .padding(vertical = 12.dp)
    ) {
        Row(
            modifier =
                Modifier.fillMaxWidth()
                    .clickable(onClick = onGroupClick)
                    .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = group.group?.name ?: "",
                color = slateText(),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = Slate.muted,
                modifier = Modifier.size(18.dp),
            )
        }
        group.feeds.forEach { feed ->
            Row(
                modifier =
                    Modifier.fillMaxWidth()
                        .clickable { onFeedClick(feed) }
                        .padding(horizontal = 20.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FeedIcon(feedName = feed.name, iconUrl = feed.icon, size = 24.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = feed.name,
                    color = slateText(),
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (feed.important != 0) {
                    Text(
                        text = feed.important.toString(),
                        color = MiuixAccentSky,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}