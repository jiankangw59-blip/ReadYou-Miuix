package me.ash.reader.ui.miuix

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import me.ash.reader.R
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Normalises whatever the reader hands us into an absolute http(s) URL, or null when the scheme
 * cannot be rendered inside a WebView (mailto:, tel:, intent:, custom app schemes...).
 */
internal fun normalizeHttpUrl(raw: String?, base: String?): String? {
    if (raw.isNullOrBlank()) return null
    val trimmed = raw.trim()
    val lower = trimmed.lowercase()
    return when {
        lower.startsWith("http://") || lower.startsWith("https://") -> trimmed
        trimmed.startsWith("//") -> "https:$trimmed"
        lower.contains("://") || lower.startsWith("mailto:") || lower.startsWith("tel:") -> null
        else ->
            runCatching { java.net.URI(base ?: "").resolve(trimmed).toString() }
                .getOrNull()
                ?.takeIf { it.startsWith("http") }
                ?: runCatching { java.net.URI("https://$trimmed").toString() }.getOrNull()
    }
}

/**
 * Minimal in-app browser ("Via"-like): links open inside the app instead of jumping to an external
 * browser, with the same Miuix chrome as the rest of the skin.
 *
 * The WebView is created lazily through [AndroidView] and released together with the composition.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MiuixInlineBrowser(
    url: String,
    onClose: () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    val context = LocalContext.current
    var progress by remember { mutableIntStateOf(0) }
    var title by remember { mutableStateOf("") }
    var webView by remember { mutableStateOf<WebView?>(null) }
    val barVisible = rememberAutoHideBarState()

    BackHandler {
        val view = webView
        if (view != null && view.canGoBack()) view.goBack() else onClose()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MiuixGlassBackground()

        AndroidView(
            modifier = Modifier.fillMaxSize().navigationBarsPadding(),
            factory = { context ->
                WebView(context)
                    .apply {
                        layoutParams =
                            ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT,
                            )
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.loadsImagesAutomatically = true
                        settings.useWideViewPort = true
                        settings.loadWithOverviewMode = true
                        webViewClient =
                            object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                ): Boolean {
                                    val scheme = request?.url?.scheme?.lowercase()
                                    if (scheme != null && scheme != "http" && scheme != "https") {
                                        runCatching {
                                            view?.context?.startActivity(
                                                android.content.Intent(
                                                    android.content.Intent.ACTION_VIEW,
                                                    request.url,
                                                )
                                            )
                                        }
                                        return true
                                    }
                                    return false
                                }

                                override fun onPageStarted(
                                    view: WebView?,
                                    url: String?,
                                    favicon: Bitmap?,
                                ) {
                                    progress = 5
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    progress = 100
                                    title = view?.title.orEmpty()
                                }
                            }
                        webChromeClient =
                            object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    progress = newProgress
                                }

                                override fun onReceivedTitle(view: WebView?, pageTitle: String?) {
                                    title = pageTitle.orEmpty()
                                }
                            }
                        setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
                            val dy = scrollY - oldScrollY
                            if (dy > 8 && barVisible.value) {
                                barVisible.value = false
                            } else if (dy < -8 && !barVisible.value) {
                                barVisible.value = true
                            }
                        }
                        loadUrl(url)
                        webView = this
                    }
            },
            update = { view ->
                if (view.url != url) view.loadUrl(url)
            },
        )

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
            title = title.ifBlank { url },
            progress = { 1f },
            modifier = Modifier.align(Alignment.TopCenter),
            actions = {
                IconButton(
                    onClick = {
                        val current = webView?.url ?: url
                        runCatching {
                            context.startActivity(
                                android.content.Intent(
                                    android.content.Intent.ACTION_VIEW,
                                    android.net.Uri.parse(current),
                                )
                            )
                        }
                        onClose()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.OpenInNew,
                        contentDescription = stringResource(R.string.open_in_browser),
                    )
                }
                IconButton(onClick = { webView?.reload() }) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = stringResource(R.string.refresh),
                    )
                }
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.close),
                    )
                }
            },
        )

        }

        if (progress in 1..99) {
            // Thin HyperOS-style loading line under the title bar.
            Box(
                modifier =
                    Modifier.align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(top = MiuixTopBarHeight)
                        .height(2.dp)
            ) {
                Box(
                    modifier =
                        Modifier.fillMaxWidth(progress / 100f)
                            .height(2.dp)
                            .background(colors.primary)
                )
            }
        }
    }
}
