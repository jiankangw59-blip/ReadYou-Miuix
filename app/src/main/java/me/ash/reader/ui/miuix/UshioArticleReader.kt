package me.ash.reader.ui.miuix

import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.roundToInt
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.List
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode

/**
 * ushio-md style HTML article reader: Jsoup DOM -> typed blocks -> Compose cards.
 * Headings accent bar, quote card, code card (language + copy), rounded images with centered captions, table card, TOC jump.
 */
@Composable
fun UshioArticleReader(
    content: String,
    baseUrl: String?,
    listState: LazyListState,
    contentPadding: PaddingValues,
    title: String = "",
    feedName: String = "",
    author: String? = null,
    date: java.util.Date? = null,
    onLinkClick: (String) -> Unit = {},
    onImageClick: (String, String) -> Unit = { _, _ -> },
) {
    val blocks = remember(content, baseUrl) { UshioHtml.parse(content, baseUrl) }
    val headings = remember(blocks) { blocks.filterIsInstance<UshioBlock.Heading>() }
    val scope = rememberCoroutineScope()
    var showToc by remember { mutableStateOf(false) }
    val headerOffset = if (title.isNotBlank()) 1 else 0

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            contentPadding = contentPadding,
            modifier = Modifier.fillMaxSize(),
        ) {
            if (title.isNotBlank()) {
                item {
                    MiuixMarkdownArticleHeader(
                        title = title,
                        feedName = feedName,
                        author = author,
                        date = date ?: java.util.Date(0L),
                    )
                }
            }
            items(blocks.size) { index ->
                val block = blocks[index]
                val pad = Modifier.padding(horizontal = 20.dp)
                when (block) {
                    is UshioBlock.Heading ->
                        UshioHeadingBlock(
                            level = block.level,
                            text = block.text,
                            modifier = pad,
                        )
                    is UshioBlock.Paragraph -> 
                        UshioParagraph(block.html, pad, onLinkClick, ctaUrl = baseUrl)
                    is UshioBlock.Quote -> UshioQuoteBlock(block.html, pad, onLinkClick)
                    is UshioBlock.Code -> UshioCodeCard(block, pad)
                    is UshioBlock.Image -> UshioImageBlock(block, pad, onImageClick)
                    is UshioBlock.Table -> UshioTableBlock(block, pad, onLinkClick)
                    is UshioBlock.ListBlock -> UshioListBlock(block, pad, onLinkClick)
                    UshioBlock.Divider -> UshioDivider(pad)
                }
            }
        }

        val showBackToTop by remember {
            derivedStateOf {
                listState.firstVisibleItemIndex > 1 || listState.firstVisibleItemScrollOffset > 400
            }
        }

        // 只有滑块本身可拖动，从右边缘起手的普通滑动仍然滚动正文。
        UshioFastScrollBar(
            listState = listState,
            modifier = Modifier.align(Alignment.CenterEnd).padding(top = 120.dp, bottom = 140.dp).width(24.dp),
        )

        UshioBackToTop(
            visible = showBackToTop,
            onClick = { scope.launch { listState.animateScrollToItem(0) } },
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 84.dp),
        )

        if (headings.size >= 3) {
            Box(
                modifier =
                    Modifier.align(Alignment.BottomEnd)
                        .padding(end = 20.dp, bottom = 24.dp)
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f)),
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.foundation.layout.Box(
                    modifier =
                        Modifier.size(48.dp).clip(CircleShape).androidClickable { showToc = !showToc },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.List,
                        contentDescription = "TOC",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            if (showToc) {
                UshioTocSheet(
                    headings = headings,
                    onDismiss = { showToc = false },
                    onJump = { heading ->
                        showToc = false
                        scope.launch { listState.animateScrollToItem(heading.anchor + headerOffset) }
                    },
                )
            }
        }
    }
}

private fun Modifier.androidClickable(onClick: () -> Unit): Modifier =
    this.clickable(onClick = onClick)

@Composable
private fun UshioHeadingBlock(level: Int, text: String, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val size = when (level) {
        1 -> 24.sp
        2 -> 21.sp
        3 -> 18.sp
        else -> 16.sp
    }
    Column(modifier = modifier.padding(top = 24.dp, bottom = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier =
                    Modifier.width(3.dp)
                        .height((size.value * 0.9f).dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(colors.primary)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                fontSize = size,
                fontWeight = FontWeight.Bold,
                color = colors.onBackground,
                lineHeight = (size.value * 1.35f).sp,
            )
        }
        if (level <= 2) {
            HorizontalDivider(
                modifier = Modifier.padding(top = 8.dp),
                thickness = 1.dp,
                color = colors.outlineVariant.copy(alpha = 0.4f),
            )
        }
    }
}

@Composable
private fun UshioParagraph(
    element: Element,
    modifier: Modifier = Modifier,
    onLinkClick: (String) -> Unit,
    ctaUrl: String? = null,
) {
    val colors = MaterialTheme.colorScheme
    val text =
        remember(element.outerHtml(), ctaUrl) {
            element.toUshioText(colors.primary, onLinkClick, ctaUrl)
        }
    if (text.isBlank()) return
    SelectionContainer {
        Text(
            text = text,
            modifier = modifier.fillMaxWidth(),
            color = colors.onBackground,
            fontSize = 16.sp,
            lineHeight = 27.2.sp,
            style = MaterialTheme.typography.bodyLarge.copy(letterSpacing = 0.2.sp),
        )
    }
    Spacer(modifier = Modifier.height(14.dp))
}

@Composable
private fun UshioQuoteBlock(
    paragraphs: List<Element>,
    modifier: Modifier = Modifier,
    onLinkClick: (String) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(colors.surfaceContainerLow)
                .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier =
                Modifier.width(4.dp)
                    .height(28.dp)
                    .clip(CircleShape)
                    .background(colors.primary.copy(alpha = 0.8f))
        )
        Column(modifier = Modifier.weight(1f)) {
            paragraphs.forEach { paragraph ->
                val text =
                    remember(paragraph.outerHtml()) {
                        paragraph.toUshioText(colors.primary, onLinkClick)
                    }
                if (text.isNotBlank()) {
                    Text(
                        text = text,
                        color = colors.onSurfaceVariant,
                        fontSize = 15.sp,
                        lineHeight = 24.5.sp,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
private fun UshioCodeCard(block: UshioBlock.Code, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val clipboard = LocalClipboardManager.current
    val highlighted = remember(block.code) { highlightUshioCode(block.code, colors) }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.surfaceContainerLow)
                .border(1.dp, colors.outlineVariant.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 6.dp, top = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = block.language?.ifBlank { "text" } ?: "code",
                color = colors.onSurfaceVariant,
                fontSize = 12.sp,
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier =
                    Modifier.size(30.dp)
                        .clip(CircleShape)
                        .background(colors.onSurface.copy(alpha = 0.06f))
                        .androidClickable { clipboard.setText(AnnotatedString(block.code)) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.ContentCopy,
                    contentDescription = "copy",
                    tint = colors.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        Box(
            modifier =
                Modifier.fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 14.dp)
        ) {
            Text(
                text = highlighted,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                lineHeight = 20.sp,
                color = colors.onSurface,
            )
        }
    }
}

@Composable
private fun UshioImageBlock(
    block: UshioBlock.Image,
    modifier: Modifier = Modifier,
    onImageClick: (String, String) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Column(modifier = modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        // 以解码后的真实尺寸判定：宽 < 200px 的视为头像/图标，固定 72dp；其余满宽但限高。
        val painter =
            coil.compose.rememberAsyncImagePainter(model = block.url)
        val intrinsic = painter.intrinsicSize
        val widthOk = intrinsic.width.isFinite() && intrinsic.width > 0f
        val heightOk = intrinsic.height.isFinite() && intrinsic.height > 0f
        // 只有“宽高都小”才算头像/图标；横条图或竖窄图仍按正文图片满宽铺开。
        val measuredSmall =
            widthOk && heightOk && intrinsic.width < 200f && intrinsic.height < 200f
        val isSmall = block.small || measuredSmall
        // 普通图片按真实宽高比铺满宽度（比例过窄/过宽时收敛，避免超长截图或窄条），
        // 头像/图标固定 72dp。
        val ratio =
            if (widthOk && heightOk && intrinsic.height > 0f) {
                (intrinsic.width / intrinsic.height).coerceIn(0.55f, 2.4f)
            } else {
                16f / 9f
            }
        val imageModifier =
            if (isSmall) {
                Modifier.size(72.dp).clip(RoundedCornerShape(12.dp))
            } else {
                Modifier.fillMaxWidth()
                    .aspectRatio(ratio)
                    .clip(RoundedCornerShape(12.dp))
            }
        androidx.compose.foundation.Image(
            painter = painter,
            contentDescription = block.caption,
            contentScale = if (isSmall) ContentScale.Fit else ContentScale.Crop,
            modifier =
                imageModifier
                    .border(
                        1.dp,
                        colors.outlineVariant.copy(alpha = 0.35f),
                        RoundedCornerShape(12.dp),
                    )
                    .androidClickable { onImageClick(block.url, block.caption.orEmpty()) },
        )
        if (!block.caption.isNullOrBlank()) {
            Text(
                text = block.caption,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                color = colors.onSurfaceVariant.copy(alpha = 0.75f),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun UshioTableBlock(
    block: UshioBlock.Table,
    modifier: Modifier = Modifier,
    onLinkClick: (String) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.surfaceContainerLowest)
                .border(1.dp, colors.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.horizontalScroll(rememberScrollState())) {
            block.rows.forEachIndexed { rowIndex, row ->
                if (rowIndex > 0) {
                    HorizontalDivider(
                        thickness = 0.8.dp,
                        color = colors.outlineVariant.copy(alpha = 0.5f),
                    )
                }
                Row(
                    modifier =
                        Modifier.background(
                            if (rowIndex == 0 && block.hasHeader) {
                                colors.surfaceContainerHigh.copy(alpha = 0.7f)
                            } else {
                                Color.Transparent
                            }
                        )
                ) {
                    row.forEachIndexed { cellIndex, cell ->
                        if (cellIndex > 0) {
                            Box(
                                modifier =
                                    Modifier.width(0.8.dp)
                                        .height(44.dp)
                                        .background(colors.outlineVariant.copy(alpha = 0.5f))
                            )
                        }
                        val text =
                            remember(cell.outerHtml()) {
                                cell.toUshioText(colors.primary, onLinkClick)
                            }
                        Text(
                            text = text,
                            modifier = Modifier.widthIn(min = 96.dp).padding(12.dp),
                            color = colors.onBackground,
                            fontSize = 14.sp,
                            lineHeight = 21.sp,
                            fontWeight =
                                if (rowIndex == 0 && block.hasHeader) FontWeight.SemiBold
                                else FontWeight.Normal,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UshioListBlock(
    block: UshioBlock.ListBlock,
    modifier: Modifier = Modifier,
    onLinkClick: (String) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Column(modifier = modifier.fillMaxWidth().padding(bottom = 14.dp)) {
        block.items.forEachIndexed { index, item ->
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                Text(
                    text = if (block.ordered) "${index + 1}." else "\u2022",
                    color = colors.primary,
                    fontSize = 15.sp,
                    modifier = Modifier.width(22.dp),
                )
                val text = remember(item.outerHtml()) { item.toUshioText(colors.primary, onLinkClick) }
                Text(
                    text = text,
                    color = colors.onBackground,
                    fontSize = 16.sp,
                    lineHeight = 26.sp,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun UshioDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier.fillMaxWidth().padding(vertical = 12.dp),
        thickness = 0.8.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
    )
}

/** Bottom sheet listing the article outline (ushio-md style TOC). */
@Composable
private fun androidx.compose.foundation.layout.BoxScope.UshioTocSheet(
    headings: List<UshioBlock.Heading>,
    onDismiss: () -> Unit,
    onJump: (UshioBlock.Heading) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier =
            Modifier.fillMaxWidth()
                .androidClickable(onDismiss)
                .background(Color.Black.copy(alpha = 0.28f))
    )
    Column(
        modifier =
            Modifier.align(Alignment.BottomCenter)
                .padding(12.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(colors.surface)
                .padding(vertical = 10.dp)
    ) {
        Text(
            text = "TOC",
            color = colors.onSurfaceVariant,
            fontSize = 12.sp,
            modifier = Modifier.padding(start = 18.dp, top = 4.dp, bottom = 6.dp),
        )
        headings.forEach { heading ->
            Text(
                text = heading.text,
                color = colors.onSurface,
                fontSize = if (heading.level <= 2) 15.sp else 14.sp,
                fontWeight = if (heading.level == 1) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 2,
                modifier =
                    Modifier.fillMaxWidth()
                        .androidClickable { onJump(heading) }
                        .padding(
                            start = (18 + (heading.level - 1) * 12).dp,
                            end = 18.dp,
                            top = 8.dp,
                            bottom = 8.dp,
                        ),
            )
        }
    }
}

/** Builds the inline text (bold / italic / code / links / mark) of a DOM element. */
private fun Element.toUshioText(
    linkColor: Color,
    onLinkClick: (String) -> Unit,
    ctaUrl: String? = null,
): AnnotatedString {
    val built = buildAnnotatedString {
    fun appendNode(node: Node) {
        when (node) {
            is TextNode -> {
                val raw = node.text().replace('\u00a0', ' ')
                append(raw.replace(Regex("[\\t\\r\\n]+"), " ").replace(Regex(" {2,}"), " "))
            }
            is Element -> {
                when (node.tagName().lowercase()) {
                    "br" -> append("\n")
                    "strong", "b" -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        node.childNodes().forEach { appendNode(it) }
                    }
                    "em", "i" -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        node.childNodes().forEach { appendNode(it) }
                    }
                    "del", "s" -> withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                        node.childNodes().forEach { appendNode(it) }
                    }
                    "code" -> withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 0.88.em,
                            background = linkColor.copy(alpha = 0.10f),
                        )
                    ) {
                        node.childNodes().forEach { appendNode(it) }
                    }
                    "mark" -> withStyle(
                        SpanStyle(background = linkColor.copy(alpha = 0.18f))
                    ) {
                        node.childNodes().forEach { appendNode(it) }
                    }
                    "sup" -> withStyle(SpanStyle(baselineShift = BaselineShift.Superscript)) {
                        node.childNodes().forEach { appendNode(it) }
                    }
                    "sub" -> withStyle(SpanStyle(baselineShift = BaselineShift.Subscript)) {
                        node.childNodes().forEach { appendNode(it) }
                    }
                    // 锚点里只有图片时（常见于知乎/微博的图文链接），用 alt 文本兜底，
                    // 否则整个链接会变成空白。
                    "img" -> {
                        val alt = node.attr("alt").ifBlank { "图片" }
                        append(alt)
                    }
                    "a" -> {
                        val href = node.absUrl("href").ifBlank { node.attr("href") }
                        if (href.isNotBlank()) {
                            // Style the span explicitly *and* attach the click annotation: some
                            // Material3 versions ignore the annotation styles alone.
                            withStyle(
                                SpanStyle(
                                    color = linkColor,
                                    textDecoration = TextDecoration.Underline,
                                )
                            ) {
                                withLink(
                                    LinkAnnotation.Clickable(
                                        tag = href,
                                        styles = TextLinkStyles(style = SpanStyle()),
                                        linkInteractionListener = { onLinkClick(href) },
                                    )
                                ) {
                                    node.childNodes().forEach { appendNode(it) }
                                }
                            }
                        } else {
                            node.childNodes().forEach { appendNode(it) }
                        }
                    }
                    else -> node.childNodes().forEach { appendNode(it) }
                }
            }
        }
    }
    childNodes().forEach { appendNode(it) }
    }

    // 「查看知乎原文 / 阅读原文 / 查看讨论」这类 CTA 在部分源的 HTML 里没有 <a>，
    // 用文章链接兜底成可点击链接。
    val plain = built.text.trim()
    val isCta =
        ctaUrl != null &&
            select("a").isEmpty() &&
            Regex("^(查看|阅读|点击|前往).{0,8}(原文|讨论|全文|出处|来源).{0,6}$").matches(plain)
    if (!isCta) return built
    return buildAnnotatedString {
        append(built)
        addLink(
            LinkAnnotation.Clickable(
                tag = ctaUrl!!,
                styles = TextLinkStyles(style = SpanStyle()),
                linkInteractionListener = { onLinkClick(ctaUrl) },
            ),
            0,
            length,
        )
        addStyle(
            SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline),
            0,
            length,
        )
    }
}

/** Lightweight syntax highlighter shared by the HTML and markdown code cards. */
internal fun highlightUshioCode(
    code: String,
    colors: androidx.compose.material3.ColorScheme,
): AnnotatedString {
    val keyword = colors.tertiary
    val string = colors.primary
    val comment = colors.outline
    val number = colors.secondary
    val keywords =
        setOf(
            "fun", "val", "var", "class", "object", "interface", "if", "else", "when", "for",
            "while", "return", "import", "package", "private", "public", "internal", "suspend",
            "const", "override", "data", "sealed", "this", "null", "true", "false", "def", "fn",
            "let", "const", "function", "new", "await", "async", "try", "catch", "throw",
        )
    return buildAnnotatedString {
        var i = 0
        while (i < code.length) {
            val c = code[i]
            when {
                c == '/' && i + 1 < code.length && code[i + 1] == '/' -> {
                    val end = code.indexOf('\n', i).let { if (it == -1) code.length else it }
                    withStyle(SpanStyle(color = comment, fontStyle = FontStyle.Italic)) {
                        append(code.substring(i, end))
                    }
                    i = end
                }
                c == '#' -> {
                    val end = code.indexOf('\n', i).let { if (it == -1) code.length else it }
                    withStyle(SpanStyle(color = comment, fontStyle = FontStyle.Italic)) {
                        append(code.substring(i, end))
                    }
                    i = end
                }
                c == '"' || c == '\'' -> {
                    val end = code.indexOf(c, i + 1).let { if (it == -1) code.length - 1 else it }
                    withStyle(SpanStyle(color = string)) { append(code.substring(i, end + 1)) }
                    i = end + 1
                }
                c.isDigit() -> {
                    var j = i
                    while (j < code.length && (code[j].isDigit() || code[j] == '.')) j++
                    withStyle(SpanStyle(color = number)) { append(code.substring(i, j)) }
                    i = j
                }
                c.isLetter() -> {
                    var j = i
                    while (j < code.length && (code[j].isLetterOrDigit() || code[j] == '_')) j++
                    val word = code.substring(i, j)
                    if (word in keywords) withStyle(SpanStyle(color = keyword)) { append(word) }
                    else append(word)
                    i = j
                }
                else -> {
                    append(c)
                    i++
                }
            }
        }
    }
}

/** 右下角「回到顶部」按钮：正文下滑后出现。 */
@Composable
private fun UshioBackToTop(
    visible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    androidx.compose.animation.AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.scaleIn(),
        exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.scaleOut(),
    ) {
        Box(
            modifier =
                Modifier.size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f))
                    .androidClickable(onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowUp,
                contentDescription = "回到顶部",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

/** Snapshot of the list position: recomposes only when an item index changes, not every frame. */
/** Snapshot of the list position: recomposes only when an item index changes, not every frame. */
/** Snapshot of the list position: recomposes only when an item index changes, not every frame. */
private data class UshioScrollInfo(val total: Int, val first: Int, val visible: Int)

/**
 * Minimal fast-scroll bar. Only the thumb is draggable (the rest of the strip ignores touches), so
 * swipes that start near the right edge keep scrolling the article normally.
 */
@Composable
private fun UshioFastScrollBar(
    listState: LazyListState,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val info by remember {
        derivedStateOf {
            val layout = listState.layoutInfo
            UshioScrollInfo(
                total = layout.totalItemsCount,
                first = layout.visibleItemsInfo.firstOrNull()?.index ?: 0,
                visible = layout.visibleItemsInfo.size.coerceAtLeast(1),
            )
        }
    }
    if (info.total <= 4) return

    val colors = MaterialTheme.colorScheme
    val scrollable = (info.total - info.visible).coerceAtLeast(1)
    val listProgress = (info.first.toFloat() / scrollable).coerceIn(0f, 1f)
    val thumbFraction = (info.visible.toFloat() / info.total).coerceIn(0.06f, 1f)

    // While dragging we follow the finger instead of the list, to avoid fighting with fling.
    var dragProgress by remember { mutableStateOf<Float?>(null) }
    val progress = dragProgress ?: listProgress

    BoxWithConstraints(modifier = modifier) {
        val density = androidx.compose.ui.platform.LocalDensity.current
        val thumbHeight = (maxHeight * thumbFraction).coerceAtLeast(36.dp)
        val travel = (maxHeight - thumbHeight).coerceAtLeast(0.dp)
        val travelPx = with(density) { travel.toPx() }.coerceAtLeast(1f)

        Canvas(modifier = Modifier.fillMaxSize()) {
            val barWidth = 3.dp.toPx()
            val centerX = size.width / 2f
            drawRoundRect(
                color = colors.onSurface.copy(alpha = 0.10f),
                topLeft = Offset(centerX - barWidth / 2f, 0f),
                size = androidx.compose.ui.geometry.Size(barWidth, size.height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f),
            )
            drawRoundRect(
                color = colors.primary.copy(alpha = 0.55f),
                topLeft = Offset(centerX - barWidth / 2f, (travel * progress).toPx()),
                size = androidx.compose.ui.geometry.Size(barWidth, thumbHeight.toPx()),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f),
            )
        }

        // Invisible, generous hit area that sits exactly on top of the thumb.
        Box(
            modifier =
                Modifier.fillMaxWidth()
                    .height(thumbHeight)
                    .offset(y = travel * progress)
                    .pointerInput(info.total, travelPx) {
                        detectVerticalDragGestures(
                            onDragStart = { dragProgress = progress },
                            onDragEnd = { dragProgress = null },
                            onDragCancel = { dragProgress = null },
                        ) { change, dragAmount ->
                            change.consume()
                            val base = dragProgress ?: progress
                            val next = (base + dragAmount / travelPx).coerceIn(0f, 1f)
                            dragProgress = next
                            val target =
                                (next * (info.total - 1)).roundToInt().coerceIn(0, info.total - 1)
                            scope.launch { listState.scrollToItem(target) }
                        }
                    }
        )
    }
}
