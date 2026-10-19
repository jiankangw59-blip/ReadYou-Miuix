package me.ash.reader.ui.miuix

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.compose.elements.MarkdownCodeFence
import com.mikepenz.markdown.compose.elements.MarkdownHeader
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import com.mikepenz.markdown.model.markdownDimens
import com.mikepenz.markdown.model.markdownPadding
import me.ash.reader.ui.miuix.MiuixMarkdown

/**
 * ushio-md 瑙傛劅鐨?Markdown 闃呰鍣紙搴曞骇锛歮ikepenz/multiplatform-markdown-renderer-m3锛夈€? *
 * 鎺掔増鍙栬嚜 ushio-md 鐨?Milkdown 涓婚锛氭鏂?16sp / 琛岄珮 1.7脳銆佸鏉炬璺濄€佹爣棰樺眰绾ч€掑噺銆? * 琛屽唴浠ｇ爜 0.88em 绛夊锛涙墍鏈夐鑹查兘缁戝畾 MaterialTheme.colorScheme 璇箟鑹诧紝娣辫壊涓庡姩鎬佸彇鑹蹭笅鍚屾牱鎴愮珛銆? */
@Composable
fun MiuixMarkdownReader(
    content: String,
    modifier: Modifier = Modifier,
    onLinkClick: ((String) -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme

    val typography =
        markdownTypography(
            text =
                TextStyle(
                    fontSize = 16.sp,
                    lineHeight = 27.2.sp, // 1.7x
                    letterSpacing = 0.2.sp,
                    color = colors.onBackground,
                ),
            paragraph =
                TextStyle(
                    fontSize = 16.sp,
                    lineHeight = 27.2.sp,
                    letterSpacing = 0.2.sp,
                    color = colors.onBackground,
                ),
            h1 =
                TextStyle(
                    fontSize = 26.sp,
                    lineHeight = 34.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.onBackground,
                ),
            h2 =
                TextStyle(
                    fontSize = 22.sp,
                    lineHeight = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.onBackground,
                ),
            h3 =
                TextStyle(
                    fontSize = 19.sp,
                    lineHeight = 27.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onBackground,
                ),
            h4 =
                TextStyle(
                    fontSize = 17.sp,
                    lineHeight = 25.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onBackground,
                ),
            h5 =
                TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onBackground,
                ),
            h6 =
                TextStyle(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.onSurfaceVariant,
                ),
            quote =
                TextStyle(
                    fontSize = 15.sp,
                    lineHeight = 24.sp,
                    color = colors.onSurfaceVariant,
                ),
            code =
                TextStyle(
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    fontFamily = FontFamily.Monospace,
                    color = colors.onSurface,
                ),
            inlineCode =
                TextStyle(
                    fontSize = 0.88.em,
                    fontFamily = FontFamily.Monospace,
                    color = colors.onSurface,
                ),
        )

    val markdownColors =
        markdownColor(
            text = colors.onBackground,
            codeBackground = colors.surfaceContainerLow,
            inlineCodeBackground = colors.primary.copy(alpha = 0.10f),
            dividerColor = colors.outlineVariant.copy(alpha = 0.6f),
            tableBackground = colors.surfaceContainerLowest,
        )

    val dimens =
        markdownDimens(
            dividerThickness = 1.dp,
            codeBackgroundCornerSize = 12.dp,
            blockQuoteThickness = 0.dp,
            tableCornerSize = 12.dp,
            tableCellPadding = 12.dp,
        )

    val padding =
        markdownPadding(
            block = 14.dp,
            list = 6.dp,
            listItemTop = 3.dp,
            listItemBottom = 3.dp,
            listIndent = 20.dp,
        )

    val defaults = markdownComponents()
    val clipboard = LocalClipboardManager.current

    val components =
        markdownComponents(
            // ---- 寮曠敤鍧楋細鍦嗚娣″簳鍗＄墖锛屽乏渚?4dp 鍦嗗ご涓婚鑹茬珫鏉?----
            blockQuote = { model ->
                val barColor = colors.primary.copy(alpha = 0.75f)
                Row(
                    modifier =
                        Modifier.fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.primary.copy(alpha = 0.06f))
                            .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier =
                            Modifier.width(4.dp)
                                .height(28.dp)
                                .clip(CircleShape)
                                .background(barColor)
                    )
                    // Render the quote body inline (no default bar) so only our rounded 4dp bar shows.
                    Column(
                        modifier = Modifier.weight(1f).padding(end = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp),
                    ) {
                        val innerComponents =
                            com.mikepenz.markdown.compose.LocalMarkdownComponents.current
                        model.node.children.forEach { child ->
                            if (child.type == org.intellij.markdown.MarkdownTokenTypes.EOL) {
                                Spacer(modifier = Modifier.height(14.dp))
                            } else {
                                com.mikepenz.markdown.compose.MarkdownElement(
                                    node = child,
                                    components = innerComponents,
                                    content = model.content,
                                    includeSpacer = false,
                                )
                            }
                        }
                    }
                }
            },
            // ---- 浠ｇ爜鍧楋細鍗＄墖 + 璇█鏍?+ 澶嶅埗 ----
            codeFence = { model ->
                MarkdownCodeFence(
                    content = model.content,
                    node = model.node,
                    style = model.typography.code,
                ) { code, language, style ->
                    UshioCodeBlock(
                        code = code,
                        language = language,
                        style = style,
                        onCopy = {
                            clipboard.setText(AnnotatedString(code))
                        },
                    )
                }
            },
            // ---- H1 / H2锛氭煍鍜屽簳閮ㄧ粏鍒嗛殧绾?----
            heading1 = { model ->
                Column {
                    MarkdownHeader(model.content, model.node, model.typography.h1)
                    HorizontalDivider(
                        modifier = Modifier.padding(top = 8.dp),
                        thickness = 1.dp,
                        color = colors.outlineVariant.copy(alpha = 0.55f),
                    )
                }
            },
            heading2 = { model ->
                Column {
                    MarkdownHeader(model.content, model.node, model.typography.h2)
                    HorizontalDivider(
                        modifier = Modifier.padding(top = 6.dp),
                        thickness = 1.dp,
                        color = colors.outlineVariant.copy(alpha = 0.4f),
                    )
                }
            },
        )

    Markdown(
        content = content,
        modifier = modifier,
        colors = markdownColors,
        typography = typography,
        dimens = dimens,
        padding = padding,
        components = components,
    )
}

/** Code block card: 12dp radius, surfaceContainerLow, 1dp hairline border, language + copy header. */
@Composable
private fun UshioCodeBlock(
    code: String,
    language: String?,
    style: TextStyle,
    onCopy: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val highlighted = remember(code) { highlightCode(code, colors) }

    Column(
        modifier =
            Modifier.fillMaxWidth()
                .padding(vertical = 8.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.surfaceContainerLow)
                .border(
                    width = 1.dp,
                    color = colors.outlineVariant.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(12.dp),
                )
    ) {
        Row(
            modifier =
                Modifier.fillMaxWidth()
                    .padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = language?.ifBlank { "text" } ?: "text",
                style =
                    MaterialTheme.typography.labelMedium.copy(
                        color = colors.onSurfaceVariant,
                        fontSize = 12.sp,
                    ),
                modifier = Modifier.weight(1f),
            )
            MiuixTinyIconButton(onClick = onCopy) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Rounded.ContentCopy,
                    contentDescription = "澶嶅埗",
                    tint = colors.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        Box(
            modifier =
                Modifier.fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(start = 14.dp, end = 14.dp, top = 2.dp, bottom = 12.dp)
        ) {
            Text(text = highlighted, style = style)
        }
    }
}

/** Very light token highlighter: comments, strings, numbers and a few keywords. */
private fun highlightCode(code: String, colors: androidx.compose.material3.ColorScheme): AnnotatedString {
    val keyword = colors.tertiary
    val string = colors.primary
    val comment = colors.outline
    val number = colors.secondary

    return buildAnnotatedString {
        val keywords =
            setOf(
                "fun", "val", "var", "class", "object", "interface", "if", "else", "when",
                "for", "while", "return", "import", "package", "private", "public", "internal",
                "suspend", "const", "override", "data", "sealed", "this", "null", "true", "false",
            )
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

@Composable
private fun MiuixTinyIconButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier =
            Modifier.size(30.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/** True when the article body should be rendered through the markdown pipeline. */
internal fun looksLikeMarkdown(content: String): Boolean = MiuixMarkdown.isMarkdown(content)

private const val MARKDOWN_PREVIEW_SAMPLE = """
# 年号制度为什么消失了

**年号**是一种纪年方式，公元纪年也是一种纪年方式，因此从某种意义上来说，公元纪年可以看作是一种特殊的年号。

## 一、年号纪年的三个缺点

1. 一是时常更改，老皇上死了太子爷即位，就换了个新年号；
2. 二是容易重叠，同一个年号在不同政权反复出现；
3. 三是跨政权不可比较。

> 引用块示例：这条竖线应该是圆角的，背景是淡淡的主题色卡片。
> 第二行用来验证行高与换行间距。

### 代码块

```kotlin
fun main() {
    val greeting = "你好，ushio-md 风格"
    println(greeting) // 注释用弱化颜色
}
```

### 表格

| 项目 | 说明 |
| --- | --- |
| 行高 | 1.7 倍，接近 ushio-md |
| 行内代码 | `markdownTypography(code = ...)` |
| 圆角 | 12dp |

普通段落里可以出现 [链接](https://example.com)、`行内代码`、**加粗** 与 *斜体*。

---

段落间距用 `markdownPadding(block = 14.dp)` 控制，列表缩进 20dp。
"""

/**
 * Full-screen Markdown preview used from the Miuix settings page to eyeball the rendering rules
 * (headings, quote card, code card, table, inline code) without needing a markdown feed.
 */
@Composable
fun MiuixMarkdownPreview(onClose: () -> Unit) {
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val progress = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(1f) }
    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.ui.platform.LocalUriHandler provides
            object : androidx.compose.ui.platform.UriHandler {
                override fun openUri(uri: String) { /* no-op in preview */ }
            }
    ) {
        MiuixPageScaffold(
            topBarTitle = "Markdown 预览",
            progress = { 1f },
            navigationIcon = {
                top.yukonga.miuix.kmp.basic.IconButton(onClick = onClose) {
                    top.yukonga.miuix.kmp.basic.Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "关闭",
                    )
                }
            },
        ) {
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding =
                    androidx.compose.foundation.layout.PaddingValues(
                        top = 76.dp,
                        bottom = 72.dp,
                        start = 20.dp,
                        end = 20.dp,
                    ),
            ) {
                item { MiuixMarkdownReader(content = MARKDOWN_PREVIEW_SAMPLE) }
            }
        }
    }
}

/**
 * Header block for markdown articles: title, feed name, author and publication date — the same
 * information Read You's own reader renders above an HTML body.
 */
@Composable
internal fun MiuixMarkdownArticleHeader(
    title: String,
    feedName: String,
    author: String?,
    date: java.util.Date,
) {
    val colors = MaterialTheme.colorScheme
    androidx.compose.foundation.layout.Column(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        androidx.compose.material3.Text(
            text = title,
            style =
                MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 24.sp,
                    lineHeight = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.onBackground,
                ),
        )
        Spacer(modifier = Modifier.height(8.dp))
        val meta =
            buildList {
                    add(feedName)
                    if (!author.isNullOrBlank()) add(author)
                }
                .joinToString(" · ")
        androidx.compose.material3.Text(
            text = meta,
            style =
                MaterialTheme.typography.labelMedium.copy(
                    fontSize = 12.sp,
                    color = colors.onSurfaceVariant,
                ),
        )
        Spacer(modifier = Modifier.height(2.dp))
        androidx.compose.material3.Text(
            text =
                remember(date) {
                    java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                        .format(date)
                },
            style =
                MaterialTheme.typography.labelMedium.copy(
                    fontSize = 12.sp,
                    color = colors.onSurfaceVariant,
                ),
        )
    }
}

/**
 * Resolves relative image / link targets in a markdown document against the article URL, so
 * markdown articles that use site-relative paths still load their images.
 */
internal fun resolveRelativeUrls(markdown: String, base: String?): String {
    if (base.isNullOrBlank()) return markdown
    val baseUri =
        runCatching { java.net.URI(base) }.getOrNull() ?: return markdown
    fun resolve(url: String): String {
        if (url.startsWith("http://") || url.startsWith("https://") || url.startsWith("data:")) {
            return url
        }
        return runCatching { baseUri.resolve(url).toString() }.getOrNull() ?: url
    }
    // ![alt](url)
    var result =
        Regex("!\\[([^\\]]*)]\\(([^)\\s]+)\\)").replace(markdown) { match ->
            "![${match.groupValues[1]}](${resolve(match.groupValues[2])})"
        }
    // [text](url)
    result =
        Regex("(?<!!)\\[([^\\]]*)]\\(([^)\\s]+)\\)").replace(result) { match ->
            "[${match.groupValues[1]}](${resolve(match.groupValues[2])})"
        }
    return result
}
