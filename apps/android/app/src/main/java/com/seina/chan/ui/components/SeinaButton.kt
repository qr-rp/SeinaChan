package com.seina.chan.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.seina.chan.ui.theme.AppShapes
import com.seina.chan.ui.theme.Hairline
import com.seina.chan.ui.theme.Ink
import com.seina.chan.ui.theme.Primary
import com.seina.chan.ui.theme.SurfaceCard
import com.seina.chan.ui.theme.TextStyles

@Composable
fun SeinaButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    variant: SeinaButtonVariant = SeinaButtonVariant.Primary,
    compact: Boolean = false
) {
    val contentPadding = if (compact) {
        PaddingValues(vertical = 8.dp, horizontal = 16.dp)
    } else {
        PaddingValues(vertical = 12.dp, horizontal = 24.dp)
    }
    val alpha = if (enabled) 1f else 0.5f

    when (variant) {
        SeinaButtonVariant.Primary -> {
            Button(
                onClick = onClick,
                modifier = modifier.alpha(alpha),
                enabled = enabled,
                shape = AppShapes.md,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary,
                    contentColor = androidx.compose.ui.graphics.Color.White,
                    disabledContainerColor = Primary,
                    disabledContentColor = androidx.compose.ui.graphics.Color.White
                ),
                contentPadding = contentPadding
            ) {
                Text(
                    text = text,
                    style = TextStyles.bodyMd
                )
            }
        }

        SeinaButtonVariant.Secondary -> {
            Button(
                onClick = onClick,
                modifier = modifier.alpha(alpha),
                enabled = enabled,
                shape = AppShapes.md,
                border = BorderStroke(width = 1.dp, color = Hairline),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SurfaceCard,
                    contentColor = Ink,
                    disabledContainerColor = SurfaceCard,
                    disabledContentColor = Ink
                ),
                contentPadding = contentPadding
            ) {
                Text(
                    text = text,
                    style = TextStyles.bodyMd
                )
            }
        }

        SeinaButtonVariant.TextLink -> {
            TextButton(
                onClick = onClick,
                modifier = modifier.alpha(alpha),
                enabled = enabled,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Primary,
                    disabledContentColor = Primary
                ),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text = text,
                    style = TextStyles.bodyMd
                )
            }
        }
    }
}

enum class SeinaButtonVariant {
    Primary,
    Secondary,
    TextLink
}
