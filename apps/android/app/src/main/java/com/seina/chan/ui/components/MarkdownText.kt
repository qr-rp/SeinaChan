package com.seina.chan.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.seina.chan.ui.theme.AppShapes
import com.seina.chan.ui.theme.CodeBg
import com.seina.chan.ui.theme.CodeText
import com.seina.chan.ui.theme.Primary
import com.seina.chan.ui.theme.TextStyles

// === Markdown 解析数据模型 ===

private sealed class MarkdownSegment {
    data class CodeBlock(val language: String, val code: String) : MarkdownSegment()
    data class Paragraph(val spans: List<InlineSpan>) : MarkdownSegment()
    data class Table(val headers: List<String>, val rows: List<List<String>>) : MarkdownSegment()
}

private sealed class InlineSpan {
    data class Plain(val text: String) : InlineSpan()
    data class Bold(val text: String) : InlineSpan()
    data class Italic(val text: String) : InlineSpan()
    data class Code(val text: String) : InlineSpan()
    data class Link(val text: String, val url: String) : InlineSpan()
}

// === 正则 ===

private val codeBlockPattern = Regex("""```([a-zA-Z0-9+-_]*)[ \t]*\n([\s\S]*?)```""")
private val linkPattern = Regex("""\[([^\]]+)\]\(([^)]+)\)""")

// === 解析器 ===

private fun parseMarkdown(content: String): List<MarkdownSegment> {
    val segments = mutableListOf<MarkdownSegment>()
    var lastIndex = 0

    codeBlockPattern.findAll(content).forEach { match ->
        // 代码块之前的文本解析为段落（同时提取表格）
        if (match.range.first > lastIndex) {
            val textBefore = content.substring(lastIndex, match.range.first)
            if (textBefore.isNotBlank()) {
                segments.addAll(parseTextWithTables(textBefore))
            }
        }
        val language = match.groupValues[1]
        val code = match.groupValues[2].trimEnd()
        segments.add(MarkdownSegment.CodeBlock(language, code))
        lastIndex = match.range.last + 1
    }

    // 剩余文本
    if (lastIndex < content.length) {
        val remaining = content.substring(lastIndex)
        if (remaining.isNotBlank()) {
            segments.addAll(parseTextWithTables(remaining))
        }
    }

    return segments
}

private fun parseParagraphs(text: String): List<MarkdownSegment.Paragraph> {
    return text.split(Regex("\n{2,}"))
        .filter { it.isNotBlank() }
        .map { paragraph ->
            MarkdownSegment.Paragraph(parseInlineSpans(paragraph.trim()))
        }
}

private fun parseInlineSpans(text: String): List<InlineSpan> {
    val spans = mutableListOf<InlineSpan>()
    var pos = 0
    val plainBuffer = StringBuilder()

    fun flushPlain() {
        if (plainBuffer.isNotEmpty()) {
            spans.add(InlineSpan.Plain(plainBuffer.toString()))
            plainBuffer.clear()
        }
    }

    while (pos < text.length) {
        // 行内代码 `code`（优先级最高，内部不解析其他格式）
        if (text[pos] == '`') {
            flushPlain()
            val end = text.indexOf('`', pos + 1)
            if (end != -1) {
                spans.add(InlineSpan.Code(text.substring(pos + 1, end)))
                pos = end + 1
            } else {
                plainBuffer.append(text[pos])
                pos++
            }
        }
        // 粗体 **text**
        else if (pos + 1 < text.length && text[pos] == '*' && text[pos + 1] == '*') {
            flushPlain()
            val end = text.indexOf("**", pos + 2)
            if (end != -1) {
                spans.add(InlineSpan.Bold(text.substring(pos + 2, end)))
                pos = end + 2
            } else {
                plainBuffer.append(text[pos])
                pos++
            }
        }
        // 斜体 *text*
        else if (text[pos] == '*') {
            flushPlain()
            val end = text.indexOf('*', pos + 1)
            if (end != -1 && end != pos + 1) {
                spans.add(InlineSpan.Italic(text.substring(pos + 1, end)))
                pos = end + 1
            } else {
                plainBuffer.append(text[pos])
                pos++
            }
        }
        // 链接 [text](url)
        else if (text[pos] == '[') {
            val linkMatch = linkPattern.find(text, pos)
            if (linkMatch != null && linkMatch.range.first == pos) {
                flushPlain()
                spans.add(InlineSpan.Link(linkMatch.groupValues[1], linkMatch.groupValues[2]))
                pos = linkMatch.range.last + 1
            } else {
                plainBuffer.append(text[pos])
                pos++
            }
        }
        else {
            plainBuffer.append(text[pos])
            pos++
        }
    }

    flushPlain()
    return spans
}

private fun parseTextWithTables(text: String): List<MarkdownSegment> {
    val segments = mutableListOf<MarkdownSegment>()
    val lines = text.lines()
    var i = 0
    var plainTextStart = 0

    fun flushPlain(endLine: Int) {
        if (plainTextStart < endLine) {
            val plainText = lines.subList(plainTextStart, endLine).joinToString("\n")
            if (plainText.isNotBlank()) {
                segments.addAll(parseParagraphs(plainText))
            }
        }
    }

    while (i < lines.size) {
        val line = lines[i]
        if (line.trimStart().startsWith("|") && line.trimEnd().endsWith("|")) {
            val tableStart = i
            var j = i + 1
            while (j < lines.size) {
                val candidate = lines[j]
                if (candidate.trimStart().startsWith("|") && candidate.trimEnd().endsWith("|")) {
                    j++
                } else {
                    break
                }
            }
            val tableText = lines.subList(tableStart, j).joinToString("\n")
            val table = parseTable(tableText)
            if (table != null) {
                flushPlain(tableStart)
                segments.add(table)
                plainTextStart = j
                i = j
                continue
            }
        }
        i++
    }

    flushPlain(lines.size)
    return segments
}

private fun parseTable(text: String): MarkdownSegment.Table? {
    val lines = text.lines()
    if (lines.size < 3) return null

    val separatorPattern = Regex("""\|\s*[-:]+\s*(\|\s*[-:]+\s*)*\|""")
    if (!separatorPattern.matches(lines[1].trim())) return null

    val headers = parseTableRow(lines[0]) ?: return null
    val rows = lines.subList(2, lines.size).mapNotNull { parseTableRow(it) }
    if (rows.isEmpty()) return null

    return MarkdownSegment.Table(headers = headers, rows = rows)
}

private fun parseTableRow(line: String): List<String>? {
    val trimmed = line.trim()
    if (!trimmed.startsWith("|") || !trimmed.endsWith("|")) return null
    return trimmed.substring(1, trimmed.length - 1)
        .split("|")
        .map { it.trim() }
}

// === 渲染组件 ===

private object CodeHighlighter {
    private val keywords = setOf(
        "fun", "val", "var", "class", "object", "if", "else", "for", "while",
        "return", "import", "package", "data", "suspend", "when", "in", "is",
        "as", "true", "false", "null", "this", "super", "try", "catch",
        "finally", "throw", "interface", "enum", "sealed", "abstract", "open",
        "private", "public", "protected", "internal", "override", "lateinit",
        "companion", "const", "inline", "crossinline", "noinline", "reified",
        "out", "infix", "operator", "tailrec", "vararg", "typealias", "expect",
        "actual", "by", "where", "init", "get", "set", "field", "property",
        "file", "constructor", "delegate", "dynamic", "external", "annotation",
        "value", "context", "repeatable", "replaceWith", "Suppress", "OptIn",
        "Deprecated", "SuppressWarnings", "JvmName", "JvmStatic", "JvmOverloads",
        "JvmField", "Volatile", "Synchronized", "Transient", "Strictfp", "Native"
    )

    private val keywordPattern = Regex("""\b(${keywords.joinToString("|")})\b""")
    private val numberPattern = Regex("""\d+(\.\d+)?""")

    fun highlight(code: String, language: String): AnnotatedString {
        return buildAnnotatedString {
            var pos = 0
            while (pos < code.length) {
                val remaining = code.substring(pos)
                var matched = false

                // Strings first (they can contain // or /* */)
                if (!matched && (code[pos] == '"' || code[pos] == '\'' || code[pos] == '`')) {
                    val quote = code[pos]
                    var i = pos + 1
                    while (i < code.length) {
                        if (code[i] == '\\' && i + 1 < code.length) {
                            i += 2
                        } else if (code[i] == quote) {
                            i++
                            break
                        } else {
                            i++
                        }
                    }
                    withStyle(SpanStyle(color = Color(0xFF4CAF50))) {
                        append(code.substring(pos, i))
                    }
                    pos = i
                    matched = true
                }

                // Block comment /* */
                if (!matched && remaining.startsWith("/*")) {
                    val end = code.indexOf("*/", pos + 2)
                    if (end != -1) {
                        withStyle(SpanStyle(color = Color.Gray)) {
                            append(code.substring(pos, end + 2))
                        }
                        pos = end + 2
                    } else {
                        withStyle(SpanStyle(color = Color.Gray)) {
                            append(code.substring(pos))
                        }
                        pos = code.length
                    }
                    matched = true
                }

                // Line comment //
                if (!matched && remaining.startsWith("//")) {
                    val end = code.indexOf('\n', pos)
                    if (end != -1) {
                        withStyle(SpanStyle(color = Color.Gray)) {
                            append(code.substring(pos, end))
                        }
                        pos = end
                    } else {
                        withStyle(SpanStyle(color = Color.Gray)) {
                            append(code.substring(pos))
                        }
                        pos = code.length
                    }
                    matched = true
                }

                // Numbers
                if (!matched) {
                    val numMatch = numberPattern.find(remaining)
                    if (numMatch != null && numMatch.range.first == 0) {
                        withStyle(SpanStyle(color = Color(0xFF2196F3))) {
                            append(numMatch.value)
                        }
                        pos += numMatch.value.length
                        matched = true
                    }
                }

                // Keywords
                if (!matched) {
                    val kwMatch = keywordPattern.find(remaining)
                    if (kwMatch != null && kwMatch.range.first == 0) {
                        withStyle(SpanStyle(color = Color(0xFFCC785C))) {
                            append(kwMatch.value)
                        }
                        pos += kwMatch.value.length
                        matched = true
                    }
                }

                if (!matched) {
                    append(code[pos])
                    pos++
                }
            }
        }
    }
}

/**
 * 轻量级 Markdown 渲染组件，支持：
 * - 代码块（``` ... ```）：深色背景 + 等宽字体
 * - 行内代码（`code`）：等宽字体 + 浅色背景
 * - 粗体（**text**）
 * - 斜体（*text*）
 * - 链接（[text](url)）：下划线 + 主题色
 */
@Composable
fun MarkdownText(
    content: String,
    style: TextStyle = TextStyles.bodyMd,
    color: Color = MaterialTheme.colorScheme.onBackground,
    modifier: Modifier = Modifier
) {
    val segments = remember(content) { parseMarkdown(content) }

    if (segments.isEmpty()) {
        // 解析结果为空时回退为纯文本
        Text(text = content, style = style, color = color, modifier = modifier)
        return
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        segments.forEach { segment ->
            when (segment) {
                is MarkdownSegment.CodeBlock -> CodeBlockView(segment)
                is MarkdownSegment.Paragraph -> {
                    val annotatedString = buildInlineAnnotatedString(segment.spans, color)
                    Text(
                        text = annotatedString,
                        style = style,
                        color = color
                    )
                }
                is MarkdownSegment.Table -> TableView(segment)
            }
        }
    }
}

@Composable
private fun CodeBlockView(codeBlock: MarkdownSegment.CodeBlock) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = CodeBg,
                shape = AppShapes.md
            )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            if (codeBlock.language.isNotBlank()) {
                Text(
                    text = codeBlock.language,
                    style = TextStyles.caption,
                    color = CodeText.copy(alpha = 0.6f),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            val highlighted = remember(codeBlock.code, codeBlock.language) {
                CodeHighlighter.highlight(codeBlock.code, codeBlock.language)
            }
            Text(
                text = highlighted,
                style = TextStyles.code,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun TableView(table: MarkdownSegment.Table) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = AppShapes.md
            )
            .padding(8.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header row
            Row(modifier = Modifier.fillMaxWidth()) {
                table.headers.forEach { header ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = header,
                            style = TextStyles.bodyMd.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Divider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )
            // Data rows
            table.rows.forEachIndexed { index, row ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    row.forEach { cell ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = cell,
                                style = TextStyles.bodyMd,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                if (index < table.rows.size - 1) {
                    Divider(
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }
        }
    }
}

private fun buildInlineAnnotatedString(spans: List<InlineSpan>, baseColor: Color): AnnotatedString {
    return buildAnnotatedString {
        for (span in spans) {
            when (span) {
                is InlineSpan.Plain -> append(span.text)
                is InlineSpan.Bold -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(span.text)
                }
                is InlineSpan.Italic -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                    append(span.text)
                }
                is InlineSpan.Code -> withStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = baseColor.copy(alpha = 0.12f),
                    )
                ) {
                    // 用细空格模拟内边距
                    append("\u2009${span.text}\u2009")
                }
                is InlineSpan.Link -> {
                    pushStringAnnotation(tag = "URL", annotation = span.url)
                    withStyle(
                        SpanStyle(
                            color = Primary,
                            textDecoration = TextDecoration.Underline
                        )
                    ) {
                        append(span.text)
                    }
                    pop()
                }
            }
        }
    }
}
