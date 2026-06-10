package com.seina.chan.ui.screens.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.seina.chan.data.model.ChatMessage
import com.seina.chan.ui.theme.AppShapes
import com.seina.chan.ui.theme.Canvas
import com.seina.chan.ui.theme.Hairline
import com.seina.chan.ui.theme.Ink
import com.seina.chan.ui.theme.Primary
import com.seina.chan.ui.theme.SurfaceCard
import com.seina.chan.ui.theme.TextStyles

@Composable
fun MessageBubble(message: ChatMessage) {
    val isUser = message.role == "user"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Avatar(
                text = "★",
                backgroundColor = Primary,
                contentColor = Color.White
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(horizontalAlignment = if (isUser) Alignment.End else Alignment.Start) {
            Box(
                modifier = Modifier
                    .background(
                        color = if (isUser) Primary else SurfaceCard,
                        shape = AppShapes.lg
                    )
                    .padding(12.dp)
            ) {
                if (message.isStreaming && message.content.isEmpty() && message.imageUrl == null) {
                    TypingIndicator()
                } else {
                    Column {
                        if (message.imageUrl != null || isImageContent(message.content)) {
                            val imageModel = message.imageUrl ?: message.content
                            AsyncImage(
                                model = imageModel,
                                contentDescription = "图片",
                                modifier = Modifier
                                    .sizeIn(maxWidth = 240.dp, maxHeight = 240.dp)
                                    .clip(AppShapes.md),
                                contentScale = ContentScale.Crop
                            )
                        }
                        if (message.content.isNotBlank() && !isImageContent(message.content)) {
                            Text(
                                text = message.content,
                                style = TextStyles.bodyMd,
                                color = if (isUser) Color.White else Ink
                            )
                        }
                    }
                }
            }

            // 思考链面板（仅 assistant 消息显示）
            if (!isUser && (message.isReasoning || message.reasoningText.isNotBlank())) {
                ReasoningPanel(
                    reasoningText = message.reasoningText,
                    isReasoning = message.isReasoning,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // 工具调用卡片列表
            message.toolCalls.forEach { toolCall ->
                ToolCallCard(
                    toolCall = toolCall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        if (isUser) {
            Spacer(modifier = Modifier.width(8.dp))
            Avatar(
                text = "🙂",
                backgroundColor = Canvas,
                contentColor = Ink,
                borderColor = Hairline
            )
        }
    }
}

@Composable
private fun ReasoningPanel(
    reasoningText: String,
    isReasoning: Boolean,
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = AppShapes.md
            )
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { expanded = !expanded }
        ) {
            Text(
                text = if (isReasoning) "思考中..." else "思考完成",
                style = TextStyles.label,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.weight(1f))
            if (isReasoning) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(color = Primary, shape = CircleShape)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (expanded) "收起" else "展开",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }

        AnimatedVisibility(visible = expanded) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Text(
                    text = reasoningText,
                    style = TextStyles.bodySm,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

private fun isImageContent(content: String): Boolean {
    if (content.startsWith("data:image/")) return true
    val cleanUrl = content.substringBefore("?").substringBefore("#")
    return cleanUrl.startsWith("http") &&
           listOf(".jpg", ".jpeg", ".png", ".gif", ".webp").any { cleanUrl.endsWith(it, ignoreCase = true) }
}

@Composable
private fun Avatar(
    text: String,
    backgroundColor: Color,
    contentColor: Color,
    borderColor: Color? = null
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .background(backgroundColor, shape = CircleShape)
            .then(
                if (borderColor != null) {
                    Modifier.border(1.dp, borderColor, CircleShape)
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = contentColor,
            style = TextStyles.bodyMd
        )
    }
}
