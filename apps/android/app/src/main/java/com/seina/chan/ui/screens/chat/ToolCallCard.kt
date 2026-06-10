package com.seina.chan.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.seina.chan.data.model.ToolCall
import com.seina.chan.data.model.ToolStatus
import com.seina.chan.ui.theme.AppShapes
import com.seina.chan.ui.theme.CodeText
import com.seina.chan.ui.theme.Primary
import com.seina.chan.ui.theme.Success
import com.seina.chan.ui.theme.SurfaceDark
import com.seina.chan.ui.theme.TextStyles

@Composable
fun ToolCallCard(
    toolCall: ToolCall,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(color = SurfaceDark, shape = AppShapes.md)
            .padding(12.dp)
    ) {
        // Top row: tool name + status
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = toolCall.toolName,
                style = TextStyles.label,
                color = Color(0xFFE8E6E1)
            )
            Spacer(modifier = Modifier.weight(1f))
            when (toolCall.status) {
                ToolStatus.Pending, ToolStatus.Running -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "运行中...",
                            style = TextStyles.caption,
                            color = Color(0xFFE8E6E1)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            color = Color(0xFFE8E6E1),
                            strokeWidth = 2.dp
                        )
                    }
                }

                ToolStatus.Completed -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "完成",
                            style = TextStyles.caption,
                            color = Color(0xFFE8E6E1)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(color = Success, shape = CircleShape)
                        )
                    }
                }

                ToolStatus.Failed -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "失败",
                            style = TextStyles.caption,
                            color = Color(0xFFE8E6E1)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(color = Primary, shape = CircleShape)
                        )
                    }
                }
            }
        }

        // Input summary
        if (toolCall.input.isNotBlank()) {
            Text(
                text = toolCall.input,
                style = TextStyles.caption,
                color = Color(0xFF8F8F8F),
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // Output result
        if (toolCall.status == ToolStatus.Completed && toolCall.output.isNotBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .background(color = Color(0xFF0F0E0D), shape = AppShapes.xs)
                    .padding(8.dp)
            ) {
                Text(
                    text = toolCall.output,
                    style = TextStyles.bodySm,
                    color = CodeText
                )
            }
        }
    }
}
