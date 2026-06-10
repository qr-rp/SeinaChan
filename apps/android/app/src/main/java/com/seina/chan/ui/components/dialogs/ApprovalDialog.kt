package com.seina.chan.ui.components.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.seina.chan.data.remote.GatewayEvent.ApprovalRequest
import com.seina.chan.ui.components.SeinaButton
import com.seina.chan.ui.components.SeinaButtonVariant
import com.seina.chan.ui.theme.AppShapes
import com.seina.chan.ui.theme.Ink
import com.seina.chan.ui.theme.Spacing
import com.seina.chan.ui.theme.SurfaceCard
import com.seina.chan.ui.theme.TextStyles

@Composable
fun ApprovalDialog(
    request: ApprovalRequest?,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onDismiss: () -> Unit
) {
    if (request == null) return

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceCard, shape = AppShapes.lg)
                .padding(Spacing.lg)
        ) {
            Text(
                text = "工具调用请求",
                style = TextStyles.bodyLg.copy(fontWeight = FontWeight.Medium),
                color = Ink
            )

            Spacer(modifier = Modifier.height(Spacing.sm))

            Text(
                text = "助手请求执行工具：${request.toolName}",
                style = TextStyles.bodyMd,
                color = Ink
            )

            Spacer(modifier = Modifier.height(Spacing.sm))

            if (request.input.isNotEmpty()) {
                Text(
                    text = request.input.entries.joinToString("\n") { "${it.key}: ${it.value}" },
                    style = TextStyles.bodySm,
                    color = Ink
                )
                Spacer(modifier = Modifier.height(Spacing.md))
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                SeinaButton(
                    text = "拒绝",
                    onClick = onReject,
                    variant = SeinaButtonVariant.Secondary,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(Spacing.md))

                SeinaButton(
                    text = "批准",
                    onClick = onApprove,
                    variant = SeinaButtonVariant.Primary,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
