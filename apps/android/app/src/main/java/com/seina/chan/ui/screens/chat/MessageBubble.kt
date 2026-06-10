package com.seina.chan.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
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
                if (message.isStreaming && message.content.isEmpty()) {
                    TypingIndicator()
                } else {
                    Text(
                        text = message.content,
                        style = TextStyles.bodyMd,
                        color = if (isUser) Color.White else Ink
                    )
                }
            }

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
