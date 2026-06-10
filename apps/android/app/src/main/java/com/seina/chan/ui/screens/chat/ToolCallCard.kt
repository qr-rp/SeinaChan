package com.seina.chan.ui.screens.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.seina.chan.data.model.ToolCallDetail
import com.seina.chan.data.model.ToolCallStatus
import com.seina.chan.ui.theme.AppShapes
import com.seina.chan.ui.theme.ErrorColor
import com.seina.chan.ui.theme.Success
import com.seina.chan.ui.theme.TextStyles

@Composable
fun ToolCallCard(
    toolCall: ToolCallDetail,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, AppShapes.md)
            .clickable { expanded = !expanded },
        shape = AppShapes.md,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // 顶部行：工具名称 + 状态 + 展开图标
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = toolCall.name,
                    style = TextStyles.label,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.weight(1f))

                // 状态指示器
                when (toolCall.status) {
                    ToolCallStatus.Running -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "运行中...",
                                style = TextStyles.caption,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                strokeWidth = 2.dp
                            )
                        }
                    }

                    ToolCallStatus.Success -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "完成",
                                style = TextStyles.caption,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(color = Success, shape = CircleShape)
                            )
                        }
                    }

                    ToolCallStatus.Failed -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "失败",
                                style = TextStyles.caption,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(color = ErrorColor, shape = CircleShape)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "收起" else "展开",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            // 参数摘要（折叠时显示）
            if (!expanded && toolCall.args.isNotBlank()) {
                Text(
                    text = toolCall.summary.ifBlank { toolCall.args.take(100) },
                    style = TextStyles.caption,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // 耗时
            if (toolCall.duration != null) {
                Text(
                    text = "耗时: ${toolCall.duration}s",
                    style = TextStyles.caption,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // 展开后的详细内容
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    // 参数 JSON
                    if (toolCall.args.isNotBlank()) {
                        Text(
                            text = "参数:",
                            style = TextStyles.label,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                                .background(color = MaterialTheme.colorScheme.inverseSurface, shape = AppShapes.xs)
                                .padding(8.dp)
                        ) {
                            Text(
                                text = toolCall.args,
                                style = TextStyles.bodySm,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // 结果 JSON
                    if (toolCall.result.isNotBlank()) {
                        Text(
                            text = "结果:",
                            style = TextStyles.label,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                                .background(color = MaterialTheme.colorScheme.inverseSurface, shape = AppShapes.xs)
                                .padding(8.dp)
                        ) {
                            Text(
                                text = toolCall.result,
                                style = TextStyles.bodySm,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}
