package me.ash.reader.ui.miuix

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.AsyncImage
import me.ash.reader.R
import me.ash.reader.domain.model.article.ArticleFlowItem
import me.ash.reader.domain.model.article.ArticleWithFeed
import me.ash.reader.domain.model.general.Filter
import me.ash.reader.domain.model.general.MarkAsReadConditions
import me.ash.reader.ui.component.FeedIcon
import me.ash.reader.ui.ext.collectAsStateValue
import me.ash.reader.ui.page.adaptive.ArticleListReaderViewModel
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.TabRow
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * HyperOS / MIUI style article list, styled after Halcyon: floating translucent bar, oversized
 * heading, segmented tabs and squircle cards with a left-hand thumbnail.
 *
 * Reuses [ArticleListReaderViewModel], so paging, filters and read-state stay identical to the
 * Material You flow page.
 */
@Composable
fun MiuixFlowPage(
    viewModel: ArticleListReaderViewModel,
    onBack: () -> Unit = {},
    onArticleClick: (id: String, index: Int) -> Unit = { _, _ -> },
) {
    val flowUiState = viewModel.flowUiState.collectAsStateValue()
    if (flowUiState == null) return

    val pagerData = flowUiState.pagerData
    val filterState = pagerData.filterState
    val pagingItems = pagerData.pager.collectAsLazyPagingItems()

    val listState = rememberLazyListState()
    val progress = rememberTopBarProgress(listState)
    val diffMap = viewModel.diffMapHolder.diffMap
    val wallpaperSeed = rememberWallpaperSeed()
    val iconSeed by rememberImageSeed(filterState.feed?.icon)
    ProvideMiuixSeed(iconSeed ?: wallpaperSeed)
    val showMarkReadSheet = remember { mutableStateOf(false) }
    val barVisible = rememberAutoHideBarState()
    val barScrollConnection = remember(barVisible) { autoHideBarConnection(barVisible) }
    val colors = MiuixTheme.colorScheme

    val title =
        when {
            filterState.group != null -> filterState.group.name
            filterState.feed != null -> filterState.feed.name
            else -> filterState.filter.toName()
        }

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

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = miuixContentPadding(bottomExtra = 112),
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            if (pagingItems.itemCount == 0) {
                item {
                    Text(
                        text =
                            if (pagingItems.loadState.refresh is LoadState.Loading) {
                                stringResource(R.string.loading)
                            } else {
                                stringResource(R.string.empty)
                            },
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                        color = colors.onBackgroundVariant,
                        fontSize = 14.sp,
                    )
                }
            }

            items(
                count = pagingItems.itemCount,
                key = { index ->
                    when (val item = pagingItems.peek(index)) {
                        is ArticleFlowItem.Article -> item.articleWithFeed.article.id
                        is ArticleFlowItem.Date -> "date-${item.date}-$index"
                        null -> "placeholder-$index"
                    }
                },
            ) { index ->
                when (val item = pagingItems[index]) {
                    is ArticleFlowItem.Date ->
                        SmallTitle(
                            text = item.date,
                            insideMargin =
                                PaddingValues(
                                    start = 24.dp,
                                    end = 24.dp,
                                    top = 16.dp,
                                    bottom = 8.dp,
                                ),
                        )
                    is ArticleFlowItem.Article -> {
                        val article = item.articleWithFeed.article
                        MiuixArticleCard(
                            articleWithFeed = item.articleWithFeed,
                            isUnread = diffMap[article.id]?.isUnread ?: article.isUnread,
                            onClick = { onArticleClick(article.id, index) },
                        )
                    }
                    null -> Unit
                }
            }

            if (pagingItems.loadState.append is LoadState.Loading) {
                item {
                    Text(
                        text = stringResource(R.string.loading),
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                        color = colors.onBackgroundVariant,
                        fontSize = 14.sp,
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
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
            title = title,
            progress = { 0.35f + progress.value * 0.65f },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                    )
                }
            },
            actions = {
                IconButton(onClick = { showMarkReadSheet.value = true }) {
                    Icon(
                        imageVector = Icons.Rounded.DoneAll,
                        contentDescription = stringResource(R.string.mark_all_as_read),
                    )
                }
                IconButton(onClick = { viewModel.sync() }) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = stringResource(R.string.refresh),
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
                    viewModel.changeFilter(filterState.copy(filter = filter))
                },
            )
        }

        if (showMarkReadSheet.value) {
            MiuixMarkReadSheet(
                onDismiss = { showMarkReadSheet.value = false },
                onSelect = { condition ->
                    showMarkReadSheet.value = false
                    viewModel.updateReadStatus(
                        groupId = filterState.group?.id,
                        feedId = filterState.feed?.id,
                        articleId = null,
                        conditions = condition,
                        isUnread = false,
                    )
                },
            )
        }
    }
}

/**
 * HyperOS-style bottom sheet with the same mark-as-read options as the Material flow page:
 * 7 / 3 / 1 days and "all".
 */
@Composable
private fun BoxScope.MiuixMarkReadSheet(
    onDismiss: () -> Unit,
    onSelect: (MarkAsReadConditions) -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    Box(
        modifier =
            Modifier.fillMaxSize()
                .background(Color.Black.copy(alpha = 0.32f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                )
    )
    Column(
        modifier =
            Modifier.align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 12.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(colors.surface)
    ) {
        SmallTitle(
            text = stringResource(R.string.mark_all_as_read),
            insideMargin = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 4.dp),
        )
        BasicComponent(
            title = stringResource(R.string.seven_days),
            summary = stringResource(R.string.mark_as_read_seven_days),
            insideMargin = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            onClick = { onSelect(MarkAsReadConditions.SevenDays) },
        )
        MiuixRowDivider(modifier = Modifier.padding(start = 20.dp))
        BasicComponent(
            title = stringResource(R.string.three_days),
            summary = stringResource(R.string.mark_as_read_three_days),
            insideMargin = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            onClick = { onSelect(MarkAsReadConditions.ThreeDays) },
        )
        MiuixRowDivider(modifier = Modifier.padding(start = 20.dp))
        BasicComponent(
            title = stringResource(R.string.one_day),
            summary = stringResource(R.string.mark_as_read_one_day),
            insideMargin = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            onClick = { onSelect(MarkAsReadConditions.OneDay) },
        )
        MiuixRowDivider(modifier = Modifier.padding(start = 20.dp))
        BasicComponent(
            title = stringResource(R.string.mark_all_as_read),
            insideMargin = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            onClick = { onSelect(MarkAsReadConditions.All) },
        )
        MiuixRowDivider(modifier = Modifier.padding(start = 20.dp))
        BasicComponent(
            title = stringResource(R.string.cancel),
            insideMargin = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            onClick = onDismiss,
        )
    }
}

@Composable
private fun MiuixArticleCard(
    articleWithFeed: ArticleWithFeed,
    isUnread: Boolean,
    onClick: () -> Unit,
) {
    val article = articleWithFeed.article
    val feed = articleWithFeed.feed
    val colors = MiuixTheme.colorScheme

    MiuixGlassCard(
        modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!article.img.isNullOrBlank()) {
                AsyncImage(
                    model =
                        coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                            .data(article.img)
                            .size(200)
                            .crossfade(false)
                            .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier =
                        Modifier.size(64.dp)
                            .clip(RoundedCornerShape(12.dp)),
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FeedIcon(
                        feedName = feed.name,
                        iconUrl = feed.icon,
                        size = 13.dp,
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = feed.name,
                        modifier = Modifier.weight(1f),
                        color = colors.onSurfaceVariantSummary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (!article.dateString.isNullOrEmpty()) {
                        Text(
                            text = article.dateString!!,
                            color = colors.onSurfaceVariantSummary,
                            fontSize = 12.sp,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = article.title,
                    color = colors.onSurface,
                    fontSize = 16.sp,
                    fontWeight = if (isUnread) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 21.sp,
                )

                if (article.shortDescription.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = article.shortDescription,
                        color = colors.onSurfaceVariantSummary,
                        fontSize = 13.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 18.sp,
                    )
                }

                if (isUnread) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Spacer(
                            modifier =
                                Modifier.size(7.dp)
                                    .clip(CircleShape)
                                    .then(Modifier.background(colors.primary))
                        )
                        Text(
                            text = stringResource(R.string.unread),
                            color = colors.primary,
                            fontSize = 11.sp,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Host that swaps between [MiuixFlowPage] (article list) and the Miuix reading page, keeping the
 * shared [ArticleListReaderViewModel] alive so paging position and read-state stay consistent.
 */
@Composable
fun MiuixReadingHost(
    viewModel: ArticleListReaderViewModel,
    onBack: () -> Unit = {},
) {
    var selectedArticleId by rememberSaveable { mutableStateOf<String?>(null) }

    if (selectedArticleId == null) {
        MiuixFlowPage(
            viewModel = viewModel,
            onBack = onBack,
            onArticleClick = { id, index ->
                selectedArticleId = id
                viewModel.initData(articleId = id, listIndex = index)
            },
        )
    } else {
        MiuixReadingPage(
            viewModel = viewModel,
            onBack = {
                viewModel.clearReadingData()
                selectedArticleId = null
            },
        )
    }
}
