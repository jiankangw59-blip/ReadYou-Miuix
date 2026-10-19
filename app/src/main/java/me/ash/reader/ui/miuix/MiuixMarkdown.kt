package me.ash.reader.ui.miuix

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * A dependency-free, *static* Markdown renderer for the Miuix reading page.
 *
 * The referenced project (ushio-md) is a Flutter/Dart app built on `flutter_markdown_plus`, so its
 * core cannot be linked into a Kotlin/Compose app. This is the Compose equivalent: the same block
 * model (headings, paragraphs, quotes, lists, code, images, rules) parsed once per article and
 * rendered without any live editing / re-layout, which keeps scrolling cheap.
 */
internal sealed interface MdBlock {
    data class Heading(val level: Int, val text: String) : MdBlock

    data class Paragraph(val text: String) : MdBlock

    data class Quote(val text: String) : MdBlock

    data class Code(val language: String, val code: String) : MdBlock

    data class Bullet(val items: List<String>) : MdBlock

    data class Ordered(val items: List<String>) : MdBlock

    data class Image(val url: String, val alt: String) : MdBlock

    data object Rule : MdBlock
}

internal object MiuixMarkdown {

    private val cache =
        object : LinkedHashMap<Int, List<MdBlock>>(16, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, List<MdBlock>>?) =
                size > 40
        }

    /** Heuristic: looks like Markdown and is not HTML. */
    fun isMarkdown(content: String): Boolean {
        if (content.isBlank()) return false
        val sample = content.take(4000)
        if (Regex("<(p|div|br|img|a|ul|li|h[1-6])[ >]", RegexOption.IGNORE_CASE).containsMatchIn(sample)) {
            return false
        }
        val signals =
            listOf(
                Regex("(?m)^#{1,6}\\s+"),
                Regex("(?m)^\\s*[-*+]\\s+"),
                Regex("(?m)^\\s*\\d+\\.\\s+"),
                Regex("\\*\\*[^*]+\\*\\*"),
                Regex("(?m)^```"),
                Regex("(?m)^>\\s+"),
            )
        return signals.count { it.containsMatchIn(sample) } >= 2
    }

    /** Parsed blocks for [content]; results are memoised by content hash. */
    fun blocks(content: String): List<MdBlock> {
        val key = content.hashCode() * 31 + content.length
        synchronized(cache) { cache[key] }?.let {
            return it
        }
        val parsed = parse(content)
        synchronized(cache) { cache[key] = parsed }
        return parsed
    }

    private fun parse(content: String): List<MdBlock> {
        val blocks = mutableListOf<MdBlock>()
        val lines = content.replace("\r\n", "\n").split('\n')
        var index = 0
        val paragraph = StringBuilder()

        fun flushParagraph() {
            if (paragraph.isNotBlank()) {
                blocks += MdBlock.Paragraph(paragraph.toString().trim())
                paragraph.clear()
            }
        }

        while (index < lines.size) {
            val raw = lines[index]
            val line = raw.trimEnd()

            when {
                line.isBlank() -> {
                    flushParagraph()
                    index++
                }
                line.startsWith("```") -> {
                    flushParagraph()
                    val language = line.removePrefix("```").trim()
                    val code = StringBuilder()
                    index++
                    while (index < lines.size && !lines[index].trimEnd().startsWith("```")) {
                        code.appendLine(lines[index])
                        index++
                    }
                    index++ // closing fence
                    blocks += MdBlock.Code(language, code.toString().trimEnd())
                }
                Regex("^#{1,6}\\s+").containsMatchIn(line) -> {
                    flushParagraph()
                    val level = line.takeWhile { it == '#' }.length.coerceIn(1, 6)
                    blocks += MdBlock.Heading(level, line.dropWhile { it == '#' }.trim())
                    index++
                }
                line.startsWith(">") -> {
                    flushParagraph()
                    val quoted = StringBuilder()
                    while (index < lines.size && lines[index].trimStart().startsWith(">")) {
                        quoted.appendLine(lines[index].trimStart().removePrefix(">").trim())
                        index++
                    }
                    blocks += MdBlock.Quote(quoted.toString().trim())
                }
                Regex("^\\s*[-*+]\\s+").containsMatchIn(line) -> {
                    flushParagraph()
                    val items = mutableListOf<String>()
                    while (index < lines.size &&
                        Regex("^\\s*[-*+]\\s+").containsMatchIn(lines[index])) {
                        items += lines[index].replaceFirst(Regex("^\\s*[-*+]\\s+"), "")
                        index++
                    }
                    blocks += MdBlock.Bullet(items)
                }
                Regex("^\\s*\\d+\\.\\s+").containsMatchIn(line) -> {
                    flushParagraph()
                    val items = mutableListOf<String>()
                    while (index < lines.size &&
                        Regex("^\\s*\\d+\\.\\s+").containsMatchIn(lines[index])) {
                        items += lines[index].replaceFirst(Regex("^\\s*\\d+\\.\\s+"), "")
                        index++
                    }
                    blocks += MdBlock.Ordered(items)
                }
                line.trim() == "---" || line.trim() == "***" -> {
                    flushParagraph()
                    blocks += MdBlock.Rule
                    index++
                }
                Regex("^!\\[([^]]*)]\\(([^)]+)\\)\\s*$").containsMatchIn(line.trim()) -> {
                    flushParagraph()
                    val match = Regex("^!\\[([^]]*)]\\(([^)]+)\\)\\s*$").find(line.trim())!!
                    blocks += MdBlock.Image(match.groupValues[2], match.groupValues[1])
                    index++
                }
                else -> {
                    paragraph.appendLine(line)
                    index++
                }
            }
        }
        flushParagraph()
        return blocks
    }

    /** Minimal inline formatting: **bold**, *italic*, `code`, [text](url). */
    fun inline(text: String, linkColor: Color): AnnotatedString = buildAnnotatedString {
        var i = 0
        while (i < text.length) {
            when {
                text.startsWith("**", i) -> {
                    val end = text.indexOf("**", i + 2)
                    if (end > 0) {
                        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                            append(text.substring(i + 2, end))
                        }
                        i = end + 2
                    } else {
                        append(text[i]); i++
                    }
                }
                text[i] == '`' -> {
                    val end = text.indexOf('`', i + 1)
                    if (end > 0) {
                        withStyle(
                            SpanStyle(
                                fontFamily = FontFamily.Monospace,
                                background = linkColor.copy(alpha = 0.10f),
                            )
                        ) {
                            append(text.substring(i + 1, end))
                        }
                        i = end + 1
                    } else {
                        append(text[i]); i++
                    }
                }
                text.startsWith("[", i) -> {
                    val closeBracket = text.indexOf(']', i)
                    val openParen = if (closeBracket > 0) text.indexOf('(', closeBracket) else -1
                    val closeParen = if (openParen > 0) text.indexOf(')', openParen) else -1
                    if (closeBracket > 0 && openParen == closeBracket + 1 && closeParen > 0) {
                        withStyle(
                            SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)
                        ) {
                            append(text.substring(i + 1, closeBracket))
                        }
                        i = closeParen + 1
                    } else {
                        append(text[i]); i++
                    }
                }
                text[i] == '*' || text[i] == '_' -> {
                    val marker = text[i]
                    val end = text.indexOf(marker, i + 1)
                    if (end > i + 1) {
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                            append(text.substring(i + 1, end))
                        }
                        i = end + 1
                    } else {
                        append(text[i]); i++
                    }
                }
                else -> {
                    append(text[i]); i++
                }
            }
        }
    }
}

/** Static Markdown reader body: parsed once per article, rendered as a plain lazy list. */
@Composable
internal fun MiuixMarkdownBody(
    content: String,
    listState: LazyListState,
    modifier: Modifier = Modifier,
    onLinkClick: (String) -> Unit,
    onImageClick: (String, String) -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    val blocks = remember(content) { MiuixMarkdown.blocks(content) }

    SelectionContainer {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            state = listState,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 72.dp,
                bottom = 96.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(blocks.size) { index ->
                when (val block = blocks[index]) {
                    is MdBlock.Heading -> {
                        val size = when (block.level) {
                            1 -> 26.sp
                            2 -> 22.sp
                            3 -> 19.sp
                            else -> 17.sp
                        }
                        Text(
                            text = MiuixMarkdown.inline(block.text, colors.primary),
                            color = colors.onBackground,
                            fontSize = size,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = if (block.level <= 2) 12.dp else 6.dp),
                        )
                    }
                    is MdBlock.Paragraph -> Text(
                        text = MiuixMarkdown.inline(block.text, colors.primary),
                        color = colors.onBackground,
                        fontSize = 16.sp,
                        lineHeight = 26.sp,
                    )
                    is MdBlock.Quote -> Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            modifier =
                                Modifier.size(width = 3.dp, height = 40.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(colors.primary.copy(alpha = 0.55f))
                        )
                        Text(
                            text = MiuixMarkdown.inline(block.text, colors.primary),
                            color = colors.onBackgroundVariant,
                            fontSize = 15.sp,
                            lineHeight = 24.sp,
                        )
                    }
                    is MdBlock.Code -> Box(
                        modifier =
                            Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.surfaceContainerHigh.copy(alpha = 0.7f))
                                .padding(12.dp)
                    ) {
                        Text(
                            text = block.code,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                            color = colors.onBackground,
                        )
                    }
                    is MdBlock.Bullet -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        block.items.forEach { item ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("•", color = colors.primary, fontSize = 16.sp)
                                Text(
                                    text = MiuixMarkdown.inline(item, colors.primary),
                                    color = colors.onBackground,
                                    fontSize = 16.sp,
                                    lineHeight = 25.sp,
                                )
                            }
                        }
                    }
                    is MdBlock.Ordered -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        block.items.forEachIndexed { itemIndex, item ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "${itemIndex + 1}.",
                                    color = colors.primary,
                                    fontSize = 15.sp,
                                )
                                Text(
                                    text = MiuixMarkdown.inline(item, colors.primary),
                                    color = colors.onBackground,
                                    fontSize = 16.sp,
                                    lineHeight = 25.sp,
                                )
                            }
                        }
                    }
                    is MdBlock.Image -> AsyncImage(
                        model = block.url,
                        contentDescription = block.alt,
                        contentScale = ContentScale.FillWidth,
                        modifier =
                            Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onImageClick(block.url, block.alt) },
                    )
                    MdBlock.Rule -> Box(
                        modifier =
                            Modifier.fillMaxWidth()
                                .height(1.dp)
                                .padding(vertical = 8.dp)
                                .background(colors.dividerLine)
                    )
                }
            }
        }
    }
}
