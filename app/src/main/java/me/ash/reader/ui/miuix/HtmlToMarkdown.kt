package me.ash.reader.ui.miuix

/**
 * 轻量 HTML → Markdown 转换，用来把 RSS 里常见的 HTML 正文喂给 Markdown 阅读器。
 *
 * 目标不是完美还原，而是保留结构（标题/段落/引用/列表/代码/链接/图片），让 ushio-md 风格的排版
 * 能作用在真实文章上；脚本、样式、iframe 等一律丢弃。
 */
internal object HtmlToMarkdown {

    private val blockTags =
        Regex("(?i)</?(div|section|article|header|footer|figure|figcaption|table|thead|tbody|tr|td|th)[^>]*>")

    fun looksLikeHtml(content: String): Boolean =
        Regex("<(p|div|br|img|a|ul|ol|li|h[1-6]|blockquote|pre|span)[ >/]", RegexOption.IGNORE_CASE)
            .containsMatchIn(content.take(4000))

    fun convert(html: String): String {
        var s = html

        // Drop non-content nodes entirely.
        s = s.replace(Regex("(?is)<(script|style|noscript|iframe|svg|head)[^>]*>.*?</\\1>"), "")
        s = s.replace(Regex("(?is)<!--.*?-->"), "")

        // Block level → markdown syntax (must run before the generic tag strip).
        s = s.replace(Regex("(?is)<h1[^>]*>(.*?)</h1>"), "\n\n# $1\n\n")
        s = s.replace(Regex("(?is)<h2[^>]*>(.*?)</h2>"), "\n\n## $1\n\n")
        s = s.replace(Regex("(?is)<h3[^>]*>(.*?)</h3>"), "\n\n### $1\n\n")
        s = s.replace(Regex("(?is)<h4[^>]*>(.*?)</h4>"), "\n\n#### $1\n\n")
        s = s.replace(Regex("(?is)<h5[^>]*>(.*?)</h5>"), "\n\n##### $1\n\n")
        s = s.replace(Regex("(?is)<h6[^>]*>(.*?)</h6>"), "\n\n###### $1\n\n")
        s = s.replace(Regex("(?is)<blockquote[^>]*>(.*?)</blockquote>"), "\n\n> $1\n\n")
        s = s.replace(Regex("(?is)<pre[^>]*>(.*?)</pre>"), "\n\n```\n$1\n```\n\n")
        s = s.replace(Regex("(?is)<code[^>]*>(.*?)</code>"), "`$1`")
        s = s.replace(Regex("(?is)<(strong|b)[^>]*>(.*?)</\\1>"), "**$2**")
        s = s.replace(Regex("(?is)<(em|i)[^>]*>(.*?)</\\1>"), "*$2*")
        s = s.replace(Regex("(?is)<a[^>]*href=[\"']([^\"']+)[\"'][^>]*>(.*?)</a>"), "[$2]($1)")
        s = s.replace(Regex("(?is)<img[^>]*src=[\"']([^\"']+)[\"'][^>]*>"), "\n\n![]($1)\n\n")
        s = s.replace(Regex("(?is)<hr[^>]*>"), "\n\n---\n\n")
        s = s.replace(Regex("(?is)<li[^>]*>(.*?)</li>"), "- $1\n")
        s = s.replace(Regex("(?is)</?(ul|ol)[^>]*>"), "\n")
        s = s.replace(Regex("(?is)<br\\s*/?>"), "\n")
        s = s.replace(Regex("(?is)</p>"), "\n\n")
        s = s.replace(Regex("(?is)<p[^>]*>"), "\n\n")

        // Remaining inline tags (span, font, figure…): unwrap.
        s = s.replace(blockTags, "\n")
        s = s.replace(Regex("(?is)<[^>]+>"), "")

        // Entities.
        s =
            s.replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&ldquo;", "“")
                .replace("&rdquo;", "”")

        // Tidy up whitespace so the parser sees clean blocks.
        s = s.replace(Regex("[\\t\\x0B\\f\\r]"), " ")
        s = s.replace(Regex(" {2,}"), " ")
        s = s.replace(Regex("(?m)^ +"), "")
        s = s.replace(Regex("\\n{3,}"), "\n\n")

        return s.trim()
    }
}
