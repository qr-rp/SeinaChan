package com.seina.chan.ui.screens.chat

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.seina.chan.ui.theme.AppShapes
import com.seina.chan.ui.theme.Spacing

@Composable
fun Composer(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    sendEnabled: Boolean,
    inputEnabled: Boolean = true,
    selectedImages: List<Uri> = emptyList(),
    onImagesSelected: (List<Uri>) -> Unit = {},
    onRemoveImage: (Uri) -> Unit = {}
) {
    // 多图选择器启动器
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            onImagesSelected(uris)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 选中的图片缩略图预览
        if (selectedImages.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(selectedImages, key = { it.toString() }) { uri ->
                    Box(modifier = Modifier.padding(end = Spacing.sm)) {
                        AsyncImage(
                            model = uri,
                            contentDescription = "图片预览",
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.outline),
                            contentScale = ContentScale.Crop
                        )
                        // 移除按钮
                        IconButton(
                            onClick = { onRemoveImage(uri) },
                            modifier = Modifier
                                .size(20.dp)
                                .align(Alignment.TopEnd)
                                .padding(2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "移除图片",
                                tint = Color.White,
                                modifier = Modifier
                                    .size(16.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                    .padding(2.dp)
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = Spacing.md,
                    end = Spacing.md,
                    top = Spacing.sm,
                    bottom = Spacing.md
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 附件按钮
            IconButton(
                onClick = { imagePickerLauncher.launch("image/*") },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "添加图片",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.width(4.dp))

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
                    focusedContainerColor = MaterialTheme.colorScheme.background,
                    unfocusedContainerColor = MaterialTheme.colorScheme.background,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            FilledIconButton(
                onClick = onSend,
                enabled = sendEnabled,
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
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
}
