package me.ash.reader.ui.miuix

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.ash.reader.R
import me.ash.reader.ui.component.reader.LocalTextContentWidth
import me.ash.reader.ui.component.reader.MediumContentWidth
import me.ash.reader.ui.ext.collectAsStateValue
import me.ash.reader.ui.page.adaptive.ArticleListReaderViewModel
import me.ash.reader.ui.page.adaptive.ReaderState
import me.ash.reader.ui.page.home.reading.Content
import me.ash.reader.ui.page.home.reading.ImageData
import me.ash.reader.ui.page.home.reading.ReaderImageViewer
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton

/**
 * HyperOS / MIUI style reading page.
 *
 * - the body is still rendered by Read You's reader (HTML renderer / WebView + all reading
 *   preferences); only the chrome is Miuix,
 * - the top bar slides away when scrolling down and comes back when scrolling up,
 * - tapping an inline image opens the full-screen zoomable viewer.
 */
@Composable
fun MiuixReadingPage(
    viewModel: ArticleListReaderViewModel,
    onBack: () -> Unit,
) {
    val readerState = viewModel.readerStateStateFlow.collectAsStateValue()
    val readingUiState = viewModel.readingUiState.collectAsStateValue()
    val uriHandler = LocalUriHandler.current

    val listState: LazyListState = rememberLazyListState()
    val wallpaperSeed = rememberWallpaperSeed()
    val coverSeed by rememberImageSeed(readingUiState.articleWithFeed?.article?.img)
    ProvideMiuixSeed(coverSeed ?: wallpaperSeed)
    val barVisible = remember { mutableStateOf(true) }
    val imageData = remember { mutableStateOf(ImageData()) }
    val showImageViewer = remember { mutableStateOf(false) }
    val browserUrl = remember { mutableStateOf<String?>(null) }

    val nestedScrollConnection =
        remember {
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    val dy = available.y
                    if (dy < -8f && barVisible.value) {
                        barVisible.value = false
                    } else if (dy > 8f && !barVisible.value) {
                        barVisible.value = true
                    }
                    return Offset.Zero
                }
            }
        }

    BackHandler { onBack() }

    Box(modifier = Modifier.fillMaxSize()) {
        MiuixSlateBackground()

        Box(modifier = Modifier.fillMaxSize().nestedScroll(nestedScrollConnection)) {
            // Every http(s) link inside the article is hijacked into the in-app browser, so nothing
            // ever falls through to the system "choose an app" dialog.
            val systemUriHandler = LocalUriHandler.current
            val inAppUriHandler =
                remember(systemUriHandler) {
                    object : UriHandler {
                        override fun openUri(uri: String) {
                            val normalized = normalizeHttpUrl(uri, readerState.link)
                            if (normalized != null) browserUrl.value = normalized
                            else runCatching { systemUriHandler.openUri(uri) }
                        }
                    }
                }
            CompositionLocalProvider(
                LocalTextContentWidth provides MediumContentWidth,
                LocalUriHandler provides inAppUriHandler,
            ) {
                val scrollState = remember(readerState.articleId) { ScrollState(0) }
                val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                val navBottom =
                    WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                val rawContent = readerState.content.text ?: ""
                // Only *real* markdown goes through the markdown pipeline. HTML bodies are rendered
                // by Read You's own reader, which also renders the title / date / author header.
                val isMarkdown = remember(rawContent) { MiuixMarkdown.isMarkdown(rawContent) }
                if (isMarkdown) {
                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = listState,
                        contentPadding =
                            PaddingValues(top = statusTop + 52.dp, bottom = navBottom + 72.dp),
                    ) {
                        item {
                            MiuixMarkdownArticleHeader(
                                title = readerState.title.orEmpty(),
                                feedName = readerState.feedName,
                                author = readerState.author,
                                date = readerState.publishedDate,
                            )
                            MiuixMarkdownReader(
                                content = resolveRelativeUrls(rawContent, readerState.link),
                                modifier = Modifier.padding(horizontal = 20.dp),
                                onLinkClick = { url -> browserUrl.value = url },
                            )
                        }
                    }
                } else if (UshioHtml.looksUsable(rawContent)) {
                    // HTML 正文：走 ushio 渲染层（大标题/引用/代码/图表卡片 + TOC）
                    UshioArticleReader(
                        content = rawContent,
                        baseUrl = readerState.link,
                        listState = listState,
                        title = readerState.title.orEmpty(),
                        feedName = readerState.feedName,
                        author = readerState.author,
                        date = readerState.publishedDate,
                        contentPadding =
                            PaddingValues(top = statusTop + 52.dp, bottom = navBottom + 96.dp),
                        onLinkClick = { url -> browserUrl.value = url },
                        onImageClick = { url, alt ->
                            imageData.value = ImageData(url, alt)
                            showImageViewer.value = true
                        },
                    )
                } else {
                    Content(
                        modifier = Modifier.fillMaxSize(),
                        content = rawContent,
                        feedName = readerState.feedName,
                        title = readerState.title ?: "",
                        author = readerState.author,
                        link = readerState.link,
                        publishedDate = readerState.publishedDate,
                        scrollState = scrollState,
                        listState = listState,
                        isLoading = readerState.content is ReaderState.Loading,
                        contentPadding = PaddingValues(top = statusTop, bottom = navBottom),
                        onImageClick = { url, alt ->
                            imageData.value = ImageData(url, alt)
                            showImageViewer.value = true
                        },
                        onLinkClick = { url -> normalizeHttpUrl(url, readerState.link)?.let { browserUrl.value = it } },
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = barVisible.value,
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            SlateReaderTopBar(
                source = readerState.feedName,
                meta = readerState.publishedDate.let { d ->
                    java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(d)
                } + " · " + ((readerState.content.text?.length ?: 0)) + " 文字",
                starred = readingUiState.isStarred,
                onBack = onBack,
                onToggleStar = { viewModel.updateStarredStatus(!readingUiState.isStarred) },
                onOpenBrowser = {
                    readerState.link?.let { link ->
                        normalizeHttpUrl(link, link)?.let { browserUrl.value = it }
                    }
                },
            )
        }

        browserUrl.value?.let { url ->
            MiuixInlineBrowser(url = url, onClose = { browserUrl.value = null })
        }

        if (showImageViewer.value) {
            ReaderImageViewer(
                imageData = imageData.value,
                onDownloadImage = { url -> viewModel.downloadImage(url) },
                onDismissRequest = { showImageViewer.value = false },
            )
        }
    }
}
