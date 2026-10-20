package me.ash.reader.ui.miuix

import java.net.URLDecoder

/**
 * Normalises image URLs that arrive from RSS feeds.
 *
 * Two problems show up with WeChat-based feeds:
 *  1. HTML is double escaped, so `&` comes through as `&#38;` / `&amp;`.
 *  2. wechat2rss rewrites every image to `<host>/img-proxy/?k=<signed>&u=<encoded original>`, and
 *     that proxy answers 403 once the signature expires — which made covers and inline images blank.
 *
 * Unwrapping back to the original mmbiz.qpic.cn URL fixes both the list covers and the reader body.
 */
internal fun normalizeFeedImageUrl(url: String?): String? {
    if (url.isNullOrBlank()) return null
    val cleaned =
        url.replace("&#38;", "&")
            .replace("&#x26;", "&", ignoreCase = true)
            .replace("&amp;", "&")
            .replace("\u200b", "")
            .trim()

    if (!cleaned.contains("img-proxy")) return cleaned

    // ?k=...&u=https%3A%2F%2F...
    val fromUri =
        runCatching { android.net.Uri.parse(cleaned).getQueryParameter("u") }.getOrNull()
    if (!fromUri.isNullOrBlank()) return fromUri

    val raw = Regex("[?&]u=([^&]+)").find(cleaned)?.groupValues?.getOrNull(1) ?: return cleaned
    return runCatching { URLDecoder.decode(raw, "UTF-8") }.getOrDefault(cleaned)
}
