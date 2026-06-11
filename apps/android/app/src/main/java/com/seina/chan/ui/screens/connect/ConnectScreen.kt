package com.seina.chan.ui.screens.connect

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.MaterialTheme
import com.seina.chan.ui.theme.AppShapes
import com.seina.chan.ui.theme.ErrorColor
import com.seina.chan.ui.theme.Spacing
import com.seina.chan.ui.theme.Success
import com.seina.chan.ui.theme.TextStyles

@Composable
fun ConnectScreen(
    onConnected: () -> Unit = {},
    viewModel: ConnectViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.navigateToChat.collect { success ->
            if (success) {
                onConnected()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 400.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "口袋星奈 v0.1.0",
                style = TextStyles.displayLg
            )

            Spacer(modifier = Modifier.height(Spacing.xl))

            OutlinedTextField(
                value = uiState.ip,
                onValueChange = viewModel::onIpChange,
                label = { Text("服务器地址") },
                placeholder = { Text("127.0.0.1") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading,
                singleLine = true,
                shape = AppShapes.md,
                colors = outlinedTextFieldColors()
            )

            Spacer(modifier = Modifier.height(Spacing.md))

            OutlinedTextField(
                value = uiState.port,
                onValueChange = viewModel::onPortChange,
                label = { Text("端口") },
                placeholder = { Text("9119") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = AppShapes.md,
                colors = outlinedTextFieldColors()
            )

            Spacer(modifier = Modifier.height(Spacing.md))

            OutlinedTextField(
                value = uiState.token,
                onValueChange = viewModel::onTokenChange,
                label = { Text("Token (可选)") },
                placeholder = { Text("留空则使用默认认证") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading,
                singleLine = true,
                shape = AppShapes.md,
                colors = outlinedTextFieldColors()
            )

            Spacer(modifier = Modifier.height(Spacing.md))

            OutlinedButton(
                onClick = viewModel::testConnection,
                enabled = !uiState.isLoading,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onBackground,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Text("测试连接")
            }

            when (val status = uiState.testStatus) {
                is TestStatus.Testing -> {
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Text(
                        text = "测试中...",
                        style = TextStyles.bodySm,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                is TestStatus.Success -> {
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Text(
                        text = "✅ ${status.message}",
                        style = TextStyles.bodySm,
                        color = Success,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                is TestStatus.Error -> {
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Text(
                        text = status.message,
                        style = TextStyles.bodySm,
                        color = ErrorColor,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                else -> {}
            }

            if (uiState.error != null) {
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text(
                    text = uiState.error!!,
                    style = TextStyles.bodySm,
                    color = ErrorColor,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(Spacing.sm))
                TextButton(
                    onClick = viewModel::clearSavedConfig,
                    colors = ButtonDefaults.textButtonColors(contentColor = ErrorColor)
                ) {
                    Text("清除配置并重新输入")
                }
            }

            Spacer(modifier = Modifier.height(Spacing.lg))

            Button(
                onClick = viewModel::connect,
                enabled = !uiState.isLoading,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = AppShapes.md
            ) {
                Text(if (uiState.isLoading) "连接中..." else "保存并连接")
            }

            Spacer(modifier = Modifier.height(Spacing.xl))

            Text(
                text = "配置 Hermes Dashboard 连接信息",
                style = TextStyles.caption,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun outlinedTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.background,
    unfocusedContainerColor = MaterialTheme.colorScheme.background,
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
    focusedTextColor = MaterialTheme.colorScheme.onBackground,
    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
    cursorColor = MaterialTheme.colorScheme.primary,
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
)
