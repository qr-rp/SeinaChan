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
import androidx.compose.material3.MaterialTheme
import com.seina.chan.ui.theme.AppShapes
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
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.primary,
                    disabledContentColor = MaterialTheme.colorScheme.onPrimary
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
                border = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.outline),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onBackground,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onBackground
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
                    contentColor = MaterialTheme.colorScheme.primary,
                    disabledContentColor = MaterialTheme.colorScheme.primary
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
