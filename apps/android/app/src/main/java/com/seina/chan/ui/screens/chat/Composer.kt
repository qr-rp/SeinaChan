package com.seina.chan.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.seina.chan.ui.theme.AppShapes
import com.seina.chan.ui.theme.Canvas
import com.seina.chan.ui.theme.Hairline
import com.seina.chan.ui.theme.Ink
import com.seina.chan.ui.theme.MutedSoft
import com.seina.chan.ui.theme.Primary
import com.seina.chan.ui.theme.Spacing

@Composable
fun Composer(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    sendEnabled: Boolean,
    inputEnabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Canvas)
            .padding(Spacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp),
            enabled = inputEnabled,
            placeholder = { Text("说点什么…") },
            maxLines = 5,
            shape = AppShapes.md,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { if (sendEnabled) onSend() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Canvas,
                unfocusedContainerColor = Canvas,
                focusedBorderColor = Primary,
                unfocusedBorderColor = Hairline,
                focusedTextColor = Ink,
                unfocusedTextColor = Ink,
                cursorColor = Primary,
                focusedPlaceholderColor = MutedSoft,
                unfocusedPlaceholderColor = MutedSoft,
            )
        )

        Spacer(modifier = Modifier.width(8.dp))

        FilledIconButton(
            onClick = onSend,
            enabled = sendEnabled,
            modifier = Modifier.size(48.dp),
            shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = Primary,
                contentColor = Color.White,
                disabledContainerColor = Primary.copy(alpha = 0.5f)
            )
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = "发送",
                tint = Color.White
            )
        }
    }
}
