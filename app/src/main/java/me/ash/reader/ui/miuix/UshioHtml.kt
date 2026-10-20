package me.ash.reader.ui.miuix

import android.text.Spanned
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode

/**
 * ushio-md 椋庢牸鐨?HTML 闃呰灞傦細Jsoup DOM 鈫?寮虹被鍨?block 鈫?Compose 鍗＄墖銆? *
 * 涓嶅啀鎶?HTML 杞垚 Markdown锛岃€屾槸鍦?DOM 鏄犲皠灞傜洿鎺ヤ骇鍑?ushio 鐨勬帓鐗堜笌缁勪欢锛? * 鏍囬宸︿晶寮鸿皟鏉°€佸紩鐢ㄥ潡鍗＄墖銆佷唬鐮佸潡鍗＄墖銆佸浘琛ㄥ崱鐗囥€佸浘鐗囧渾瑙?+ 灞呬腑鍥炬敞銆佽〃鏍煎崱鐗囥€? * 鍚屾椂鏀堕泦 h1~h4 鐢熸垚鏂囧唴鐩綍锛圱OC锛夈€? */
internal sealed interface UshioBlock {
    data class Heading(val level: Int, val text: String, val anchor: Int) : UshioBlock

    data class Paragraph(val html: Element) : UshioBlock

    data class Quote(val html: List<Element>) : UshioBlock

    data class Code(val language: String?, val code: String) : UshioBlock

    data class Image(val url: String, val caption: String?, val small: Boolean = false) : UshioBlock

    data class Table(val rows: List<List<Element>>, val hasHeader: Boolean) : UshioBlock

    data class ListBlock(val items: List<Element>, val ordered: Boolean) : UshioBlock

    data object Divider : UshioBlock
}

internal data class UshioHeading(val level: Int, val text: String)

internal object UshioHtml {

    // Small LRU so re-opening the same article is instant instead of re-parsing the DOM.
    private val parseCache =
        object : LinkedHashMap<Int, List<UshioBlock>>(8, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, List<UshioBlock>>?) =
                size > 4
        }

    /** Parses + cleans the HTML body and flattens it into renderable blocks. */
    fun parse(html: String, baseUrl: String?): List<UshioBlock> {
        if (html.isBlank()) return emptyList()
        val cacheKey = (html.hashCode() * 31) + (baseUrl?.hashCode() ?: 0)
        synchronized(parseCache) { parseCache[cacheKey] }?.let { return it }
        val doc =
            runCatching {
                    org.jsoup.Jsoup.parse(html, baseUrl ?: "")
                }
                .getOrNull() ?: return emptyList()
        clean(doc, baseUrl)
        val blocks = mutableListOf<UshioBlock>()
        doc.body().children().forEach { element -> collect(element, blocks) }
        runCatching {
            doc.body().select("p,div,span").take(400).filter { it.text().contains("知乎原文") || it.text().contains("知乎讨论") }.take(3).forEach { el ->
                android.util.Log.i("UshioHtml", "RAW tag=${el.tagName()} html=" + el.outerHtml().take(400))
            }
            doc.body().select("a").take(6).forEach { a ->
                android.util.Log.i(
                    "UshioHtml",
                    "ANCHOR href='" + a.attr("href") + "' abs='" + a.absUrl("href") + "' text='" + a.text().take(40) + "' outer=" + a.outerHtml().take(160),
                )
            }
        }
        runCatching {
            blocks.forEachIndexed { index, block ->
                val preview =
                    when (block) {
                        is UshioBlock.Paragraph -> block.html.text().take(60)
                        is UshioBlock.Heading -> block.text.take(60)
                        is UshioBlock.Quote -> block.html.firstOrNull()?.text()?.take(60) ?: ""
                        is UshioBlock.Code -> block.code.take(40)
                        is UshioBlock.Image -> block.url.take(60)
                        is UshioBlock.Table -> "table rows=" + block.rows.size
                        is UshioBlock.ListBlock -> "list " + block.items.size
                        UshioBlock.Divider -> "---"
                    }
                android.util.Log.i("UshioHtml", "[$index] ${block::class.simpleName}: $preview")
            }
        }
        synchronized(parseCache) { parseCache[cacheKey] = blocks }
        return blocks
    }

    /** Cheap guard: only take over when the HTML actually has readable structure. */
    fun looksUsable(html: String): Boolean {
        if (html.isBlank()) return false
        val textish = html.replace(Regex("(?is)<(script|style)[^>]*>.*?</\\1>"), "")
        val tags = Regex("(?i)<(p|div|h[1-6]|blockquote|pre|ul|ol|table|figure|img|br)[ >/]").findAll(textish).count()
        return tags >= 2
    }

    /** Repairs lazy images, absolutises URLs, drops inline styles and empty placeholders. */
    private fun clean(doc: Document, baseUrl: String?) {
        doc.select("script,style,noscript,iframe,svg,head,form,button").remove()

        doc.select("img").forEach { img ->
            // wechat2rss proxies WeChat images through /img-proxy/?k=<signed>&u=<encoded> and that
            // proxy answers 403 (the signature is short-lived), which is why images in company
            // WeChat feeds never loaded. Unwrap it back to the original mmbiz.qpic.cn URL.
            // Feed HTML is double-escaped, so `&` arrives as `&#38;` / `&amp;`. Normalise before
            // looking at the query string, otherwise the `u=` parameter is never found.
            val normalized = normalizeFeedImageUrl(img.attr("src"))
            if (!normalized.isNullOrBlank()) img.attr("src", normalized)

            val lazy =
                listOf("data-src", "data-original", "data-lazy-src", "data-actualsrc", "data-url")
                    .firstNotNullOfOrNull { key -> img.attr(key).takeIf { it.isNotBlank() } }
            if (lazy != null) img.attr("src", lazy)
            img.removeAttr("style")
            img.removeAttr("class")
            img.removeAttr("srcset")
            img.removeAttr("sizes")
            val src = img.attr("src")
            if (baseUrl != null && src.isNotBlank() && !src.startsWith("http")) {
                img.attr("src", absolutize(baseUrl, src))
            }
        }

        // Some sites (Zhihu, Weibo, …) put the target in data-href / data-url instead of <a href>.
        // Promote those to real anchors so the reader can style and open them.
        doc.select("[data-href],[data-url],[data-link],[data-target-url]").forEach { el ->
            val url =
                el.attr("data-href")
                    .ifBlank { el.attr("data-url") }
                    .ifBlank { el.attr("data-link") }
                    .ifBlank { el.attr("data-target-url") }
            if (url.isNotBlank() && el.tagName().lowercase() != "a") {
                runCatching {
                    el.tagName("a")
                    el.attr("href", url)
                    el.removeAttr("data-href")
                    el.removeAttr("data-url")
                }
            }
        }

        doc.select("[style]").forEach { it.removeAttr("style") }
        doc.select("div,span,p").forEach { element ->
            if (element.text().isBlank() && element.select("img,pre,table,figure").isEmpty()) {
                element.remove()
            }
        }
        doc.select("br").forEach { br ->
            val prev = br.previousSibling()
            val next = br.nextSibling()
            if ((prev == null || (prev is TextNode && prev.text().isBlank())) &&
                (next == null || (next is TextNode && next.text().isBlank()))) {
                br.remove()
            }
        }
    }

    private fun absolutize(base: String, url: String): String =
        runCatching { java.net.URI(base).resolve(url).toString() }.getOrNull() ?: url

    private fun collect(element: Element, out: MutableList<UshioBlock>) {
        val tag = element.tagName().lowercase()
        when (tag) {
            "h1", "h2", "h3", "h4", "h5", "h6" -> {
                val text = element.text().trim()
                if (text.isNotEmpty()) {
                    out += UshioBlock.Heading(level = tag.substring(1).toIntOrNull() ?: 2, text = text, anchor = out.size)
                }
            }
            "p" -> {
                if (element.select("img,pre,table,figure").isNotEmpty()) {
                    flattenMixed(element, out)
                } else if (element.text().isNotBlank()) {
                    out += UshioBlock.Paragraph(element)
                }
            }
            "blockquote", "aside" -> {
                val paragraphs = element.children().filter { it.text().isNotBlank() }
                if (paragraphs.isEmpty()) {
                    if (element.text().isNotBlank()) out += UshioBlock.Paragraph(element)
                } else {
                    out += UshioBlock.Quote(paragraphs)
                }
            }
            "pre" -> {
                val codeEl = element.selectFirst("code")
                val code = (codeEl ?: element).wholeText().trimEnd()
                if (code.isNotBlank()) {
                    out += UshioBlock.Code(language = languageOf(element, codeEl), code = code)
                }
            }
            "figure" -> {
                val img = element.selectFirst("img")
                val caption = element.selectFirst("figcaption")?.text()?.trim()
                if (img != null) {
                    out += UshioBlock.Image(
                        url = img.attr("src"),
                        caption = caption?.takeIf { it.isNotBlank() },
                        small = isSmallImage(img),
                    )
                } else {
                    flattenMixed(element, out)
                }
            }
            "img" -> {
                val src = element.attr("src")
                if (src.isNotBlank()) {
                    out += UshioBlock.Image(
                        url = src,
                        caption = element.attr("alt").takeIf { it.isNotBlank() },
                        small = isSmallImage(element),
                    )
                }
            }
            "table" -> {
                val rows = element.select("tr").map { it.children().filter { c -> c.tagName() != "" } }
                if (rows.isNotEmpty()) {
                    val hasHeader = element.selectFirst("thead") != null ||
                        element.select("tr").firstOrNull()?.children()?.any { it.tagName() == "th" } == true
                    out += UshioBlock.Table(rows = rows, hasHeader = hasHeader)
                }
            }
            "ul", "ol" -> {
                val items = element.children().filter { it.tagName() == "li" }
                if (items.isNotEmpty()) out += UshioBlock.ListBlock(items, ordered = tag == "ol")
            }
            "hr" -> out += UshioBlock.Divider
            else -> {
                if (element.children().isEmpty()) {
                    if (element.text().isNotBlank()) out += UshioBlock.Paragraph(element)
                } else {
                    element.children().forEach { collect(it, out) }
                }
            }
        }
    }

    /** Splits a container that mixes text and block children (images / code / tables). */
    private fun flattenMixed(element: Element, out: MutableList<UshioBlock>) {
        val buffer = mutableListOf<Node>()
        fun flush() {
            if (buffer.isEmpty()) return
            val wrapper = Element("p")
            buffer.toList().forEach { buffer.remove(it); wrapper.appendChild(it) }
            if (wrapper.text().isNotBlank()) out += UshioBlock.Paragraph(wrapper)
        }
        element.childNodes().forEach { node ->
            if (node is Element && node.tagName().lowercase() in setOf("img", "pre", "table", "figure", "blockquote")) {
                flush()
                collect(node, out)
            } else {
                buffer += node
            }
        }
        flush()
    }

    /**
     * Heuristic for avatar / emoji / logo sized images: declared pixel size, or a URL / class hint.
     * These must not be blown up to full width by the reader.
     */
    private fun isSmallImage(img: Element): Boolean {
        val declared = img.attr("width").toIntOrNull() ?: img.attr("height").toIntOrNull()
        if (declared != null) return declared in 1..200
        val hints = (img.attr("src") + " " + img.attr("class") + " " + img.attr("alt")).lowercase()
        return listOf("avatar", "icon", "emoji", "logo", "profile", "emoji", "badge", "head")
            .any { hints.contains(it) }
    }

    private fun languageOf(pre: Element, code: Element?): String? {
        val classes = (code?.attr("class").orEmpty() + " " + pre.attr("class")).lowercase()
        val match = Regex("(?:language|lang)-([a-z0-9+#]+)").find(classes) ?: return null
        return match.groupValues[1]
    }
}

/** Extracts the original image URL from a wechat2rss `img-proxy` link. */
private fun unwrapImageProxy(src: String): String? {
    val cleaned = normalizeUrlEntities(src)
    val uri = runCatching { android.net.Uri.parse(cleaned) }.getOrNull()
    val fromUri = uri?.getQueryParameter("u")
    if (!fromUri.isNullOrBlank()) return fromUri
    val raw = Regex("[?&]u=([^&]+)").find(cleaned)?.groupValues?.getOrNull(1) ?: return null
    return runCatching { java.net.URLDecoder.decode(raw, "UTF-8") }.getOrNull()
}

/** Decodes the HTML entities that survive one Jsoup pass (notably `&#38;` inside query strings). */
private fun normalizeUrlEntities(url: String): String =
    url.replace("&#38;", "&")
        .replace("&#x26;", "&", ignoreCase = true)
        .replace("&amp;", "&")
        .replace("\u200b", "")
        .trim()