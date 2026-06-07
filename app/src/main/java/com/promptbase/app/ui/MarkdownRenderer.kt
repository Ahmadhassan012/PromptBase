package com.promptbase.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

sealed class MarkdownBlock {
    data class Header(val text: String, val level: Int) : MarkdownBlock()
    data class CodeBlock(val text: String, val language: String?) : MarkdownBlock()
    data class BulletPoint(val text: String) : MarkdownBlock()
    data class Paragraph(val text: String) : MarkdownBlock()
}

fun parseMarkdownToBlocks(text: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    val lines = text.lines()
    var insideCodeBlock = false
    val currentCodeLines = mutableListOf<String>()
    var currentCodeLang: String? = null

    for (line in lines) {
        val trimmed = line.trim()
        if (trimmed.startsWith("```")) {
            if (insideCodeBlock) {
                blocks.add(MarkdownBlock.CodeBlock(currentCodeLines.joinToString("\n"), currentCodeLang))
                currentCodeLines.clear()
                currentCodeLang = null
                insideCodeBlock = false
            } else {
                val lang = trimmed.removePrefix("```").trim().takeIf { it.isNotEmpty() }
                currentCodeLang = lang
                insideCodeBlock = true
            }
            continue
        }

        if (insideCodeBlock) {
            currentCodeLines.add(line)
            continue
        }

        when {
            trimmed.startsWith("# ") -> {
                blocks.add(MarkdownBlock.Header(trimmed.removePrefix("# ").trim(), 1))
            }
            trimmed.startsWith("## ") -> {
                blocks.add(MarkdownBlock.Header(trimmed.removePrefix("## ").trim(), 2))
            }
            trimmed.startsWith("### ") -> {
                blocks.add(MarkdownBlock.Header(trimmed.removePrefix("### ").trim(), 3))
            }
            trimmed.startsWith("- ") -> {
                blocks.add(MarkdownBlock.BulletPoint(trimmed.removePrefix("- ").trim()))
            }
            trimmed.startsWith("* ") -> {
                blocks.add(MarkdownBlock.BulletPoint(trimmed.removePrefix("* ").trim()))
            }
            else -> {
                if (line.isNotEmpty()) {
                    blocks.add(MarkdownBlock.Paragraph(line))
                }
            }
        }
    }

    if (insideCodeBlock && currentCodeLines.isNotEmpty()) {
        blocks.add(MarkdownBlock.CodeBlock(currentCodeLines.joinToString("\n"), currentCodeLang))
    }

    return blocks
}

@Composable
fun MarkdownRenderer(
    content: String,
    variableColor: Color,
    onVariableColor: Color
) {
    val blocks = remember(content) { parseMarkdownToBlocks(content) }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Header -> {
                    val fontSize = when (block.level) {
                        1 -> 24.sp
                        2 -> 20.sp
                        else -> 18.sp
                    }
                    Text(
                        text = block.text,
                        style = TextStyle(
                            fontWeight = FontWeight.Black,
                            fontSize = fontSize,
                            fontFamily = FontFamily.SansSerif
                        ),
                        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                    )
                }
                is MarkdownBlock.CodeBlock -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceContainerLowest,
                                RoundedCornerShape(8.dp)
                            )
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Column {
                            block.language?.let { lang ->
                                Text(
                                    text = lang.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                            }
                            Text(
                                text = block.text,
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                is MarkdownBlock.BulletPoint -> {
                    Row(
                        modifier = Modifier.padding(start = 8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = renderAnnotatedText(block.text, variableColor, onVariableColor),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                is MarkdownBlock.Paragraph -> {
                    Text(
                        text = renderAnnotatedText(block.text, variableColor, onVariableColor),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun renderAnnotatedText(
    text: String,
    variableColor: Color,
    onVariableColor: Color
): AnnotatedString {
    val variableRegex = remember { """\{\{\s*([^:\}]+?)\s*(?::\s*([^:\}]+?)\s*)?\}\}""".toRegex() }
    val builder = RememberAnnotatedStringBuilder(text, variableRegex, variableColor, onVariableColor)
    return builder
}

@Composable
fun RememberAnnotatedStringBuilder(
    text: String,
    regex: Regex,
    variableColor: Color,
    onVariableColor: Color
): AnnotatedString {
    return remember(text, variableColor, onVariableColor) {
        val builder = AnnotatedString.Builder()
        var lastIndex = 0

        for (match in regex.findAll(text)) {
            if (match.range.first > lastIndex) {
                builder.append(text.substring(lastIndex, match.range.first))
            }

            val originalToken = match.value
            val start = builder.length
            builder.append(originalToken)
            val end = builder.length

            builder.addStyle(
                style = SpanStyle(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    background = variableColor,
                    color = onVariableColor
                ),
                start = start,
                end = end
            )

            lastIndex = match.range.last + 1
        }

        if (lastIndex < text.length) {
            builder.append(text.substring(lastIndex))
        }

        builder.toAnnotatedString()
    }
}
