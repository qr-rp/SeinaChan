package com.seina.chan.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
        // 代码块之前的文本解析为段落
        if (match.range.first > lastIndex) {
            val textBefore = content.substring(lastIndex, match.range.first)
            if (textBefore.isNotBlank()) {
                segments.addAll(parseParagraphs(textBefore))
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
            segments.addAll(parseParagraphs(remaining))
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

// === 渲染组件 ===

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
            Text(
                text = codeBlock.code,
                style = TextStyles.code,
                color = CodeText
            )
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
