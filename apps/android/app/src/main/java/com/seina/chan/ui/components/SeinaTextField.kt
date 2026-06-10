package com.seina.chan.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.seina.chan.ui.theme.AppShapes
import com.seina.chan.ui.theme.Hairline
import com.seina.chan.ui.theme.Ink
import com.seina.chan.ui.theme.InkLight
import com.seina.chan.ui.theme.Primary
import com.seina.chan.ui.theme.Spacing
import com.seina.chan.ui.theme.SurfaceCard
import com.seina.chan.ui.theme.TextStyles

@Composable
fun SeinaTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    enabled: Boolean = true,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    val isFocused by interactionSource.collectIsFocusedAsState()

    val borderColor by animateColorAsState(
        targetValue = if (isFocused) Primary else Hairline,
        label = "borderColor"
    )

    val borderWidth by animateDpAsState(
        targetValue = if (isFocused) 2.dp else 1.dp,
        label = "borderWidth"
    )

    val outerBorderColor = if (isFocused) Primary.copy(alpha = 0.2f) else Color.Transparent

    Box(
        modifier = modifier
            .clip(AppShapes.md)
            .border(width = 3.dp, color = outerBorderColor, shape = AppShapes.md)
            .padding(3.dp)
            .clip(AppShapes.md)
            .background(SurfaceCard)
            .border(width = borderWidth, color = borderColor, shape = AppShapes.md)
            .padding(horizontal = Spacing.md, vertical = 12.dp)
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            readOnly = readOnly,
            textStyle = TextStyles.bodyMd.copy(color = Ink),
            cursorBrush = SolidColor(Primary),
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            singleLine = singleLine,
            maxLines = maxLines,
            minLines = minLines,
            interactionSource = interactionSource,
            decorationBox = { innerTextField ->
                if (value.isEmpty() && placeholder.isNotEmpty()) {
                    Text(
                        text = placeholder,
                        style = TextStyles.bodyMd,
                        color = InkLight
                    )
                }
                innerTextField()
            }
        )
    }
}
