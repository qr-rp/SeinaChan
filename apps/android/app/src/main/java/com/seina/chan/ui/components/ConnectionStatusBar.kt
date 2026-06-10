package com.seina.chan.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.seina.chan.ui.theme.AppShapes
import com.seina.chan.ui.theme.Ink
import com.seina.chan.ui.theme.Primary
import com.seina.chan.ui.theme.Spacing
import com.seina.chan.ui.theme.Success
import com.seina.chan.ui.theme.SurfaceCard
import com.seina.chan.ui.theme.TextStyles

@Composable
fun ConnectionStatusBar(
    status: ConnectionStatus,
    modifier: Modifier = Modifier,
    backgroundColor: Color = SurfaceCard
) {
    val label = when (status) {
        ConnectionStatus.Connected -> "已连接"
        ConnectionStatus.Connecting -> "连接中..."
        is ConnectionStatus.Disconnected -> status.message ?: "未连接"
    }

    Row(
        modifier = modifier
            .height(32.dp)
            .clip(AppShapes.lg)
            .background(backgroundColor)
            .padding(horizontal = Spacing.sm),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (status) {
            ConnectionStatus.Connected -> {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Success)
                )
            }

            ConnectionStatus.Connecting -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    color = Primary,
                    strokeWidth = 2.dp
                )
            }

            is ConnectionStatus.Disconnected -> {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Primary)
                )
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        Text(
            text = label,
            style = TextStyles.bodySm,
            color = Ink
        )
    }
}

sealed class ConnectionStatus {
    data object Connected : ConnectionStatus()
    data object Connecting : ConnectionStatus()
    data class Disconnected(val message: String? = null) : ConnectionStatus()
}
