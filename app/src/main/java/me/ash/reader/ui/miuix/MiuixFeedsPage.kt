package me.ash.reader.ui.miuix

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.work.WorkInfo
import me.ash.reader.R
import me.ash.reader.domain.model.feed.Feed
import me.ash.reader.domain.model.general.Filter
import me.ash.reader.domain.model.group.GroupWithFeed
import me.ash.reader.ui.component.FeedIcon
import me.ash.reader.ui.ext.collectAsStateValue
import me.ash.reader.ui.page.home.feeds.FeedsViewModel
import me.ash.reader.ui.page.home.feeds.subscribe.SubscribeDialog
import me.ash.reader.ui.page.home.feeds.subscribe.SubscribeViewModel
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.rememberPullToRefreshState
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * HyperOS / MIUI style feeds page, styled after Halcyon:
 * - compact title on the top-left with the subscribe / settings actions on the top-right,
 * - filters (All / Unread / Starred) in a floating bottom tab capsule,
 * - frosted 22dp cards floating over a soft colour backdrop.
 *
 * Shares [FeedsViewModel] (and therefore its database, sync and read-state) with the Material page.
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

    val owner = LocalLifecycleOwner.current
    var isSyncing by remember { mutableStateOf(false) }
    DisposableEffect(owner) {
        feedsViewModel.syncWorkLiveData.observe(owner) { workInfoList ->
            isSyncing = workInfoList.any { it.state == WorkInfo.State.RUNNING }
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

    val pullToRefreshState = rememberPullToRefreshState()
    val listState = rememberLazyListState()
    val barVisible = rememberAutoHideBarState()
    val barScrollConnection = remember(barVisible) { autoHideBarConnection(barVisible) }
    ProvideMiuixSeed(rememberWallpaperSeed())
    val progress = rememberTopBarProgress(listState)
    val colors = MiuixTheme.colorScheme

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
        MiuixGlassBackground()

        // Pull-to-refresh wrapper removed: after a filter switch it could stay in the "refreshing"
        // state and swallow every drag, which made the list feel unscrollable. Refresh remains
        // available from the top bar.
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding = miuixContentPadding(bottomExtra = 112),
            ) {
                item {
                    MiuixTile(
                        title = stringResource(R.string.all_articles),
                        subtitle = feedsUiState.importantSum,
                        modifier = Modifier.padding(horizontal = 12.dp).padding(top = 8.dp),
                        onClick = {
                            feedsViewModel.changeFilter(
                                filterState.copy(group = null, feed = null)
                            )
                            navigationToFlow()
                        },
                    )
                }

                item {
                    SmallTitle(
                        text = stringResource(R.string.feeds),
                        insideMargin =
                            PaddingValues(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 8.dp),
                    )
                }

                if (groupWithFeedList.isEmpty()) {
                    item {
                        Box(
                            modifier =
                                Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 32.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = stringResource(R.string.empty),
                                color = colors.onBackgroundVariant,
                                fontSize = 14.sp,
                            )
                        }
                    }
                }

                items(groupWithFeedList, key = { it.group?.id ?: -1 }) { groupWithFeed ->
                    MiuixGroupCard(
                        groupWithFeed = groupWithFeed,
                        onGroupClick = {
                            feedsViewModel.changeFilter(
                                filterState.copy(group = groupWithFeed.group, feed = null)
                            )
                            navigationToFlow()
                        },
                        onFeedClick = { feed ->
                            feedsViewModel.changeFilter(
                                filterState.copy(feed = feed, group = null)
                            )
                            navigationToFlow()
                        },
                    )
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }

        androidx.compose.animation.AnimatedVisibility(
            visible = barVisible.value,
            enter =
                androidx.compose.animation.slideInVertically { -it } +
                    androidx.compose.animation.fadeIn(),
            exit =
                androidx.compose.animation.slideOutVertically { -it } +
                    androidx.compose.animation.fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
        MiuixTopBarLeft(
            title = feedsUiState.account?.name ?: stringResource(R.string.feeds),
            subtitle = feedsUiState.importantSum,
            progress = { 0.35f + progress.value * 0.65f },
            modifier = Modifier.align(Alignment.TopCenter),
            actions = {
                if (subscribeViewModel.rssService.get().addSubscription) {
                    IconButton(onClick = { subscribeViewModel.showDrawer() }) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = stringResource(R.string.subscribe),
                        )
                    }
                }
                IconButton(onClick = navigateToSettings) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = stringResource(R.string.settings),
                    )
                }
            },
        )

        }
        Box(
            modifier =
                Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 8.dp)
        ) {
            MiuixBottomTabs(
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

        SubscribeDialog(subscribeViewModel = subscribeViewModel)
    }
}

@Composable
private fun MiuixGroupCard(
    groupWithFeed: GroupWithFeed,
    onGroupClick: () -> Unit,
    onFeedClick: (Feed) -> Unit,
) {
    val group = groupWithFeed.group
    val feeds = groupWithFeed.feeds
    val colors = MiuixTheme.colorScheme

    MiuixGlassCard(
        modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp),
    ) {
        BasicComponent(
            title = group.name,
            summary = stringResource(R.string.feeds_count, feeds.size),
            startAction = {
                Icon(
                    imageVector = Icons.Outlined.Folder,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 12.dp),
                    tint = colors.primary,
                )
            },
            insideMargin = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            onClick = onGroupClick,
        )
        feeds.forEach { feed ->
            MiuixRowDivider(modifier = Modifier.padding(start = 16.dp))
            BasicComponent(
                title = feed.name,
                startAction = {
                    FeedIcon(
                        modifier = Modifier.padding(end = 12.dp),
                        feedName = feed.name,
                        iconUrl = feed.icon,
                        size = 22.dp,
                    )
                },
                endActions = {
                    if (feed.important != 0) {
                        Text(
                            text = feed.important.toString(),
                            color = colors.primary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = null,
                        tint = colors.outline,
                        modifier = Modifier.size(18.dp),
                    )
                },
                insideMargin = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                onClick = { onFeedClick(feed) },
            )
        }
    }
}
