package com.seina.chan.ui.screens.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.seina.chan.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    navController: NavController
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "设置",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    // 返回按钮仅执行返回操作，避免任何副作用导致意外导航
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 显示设置
            item {
                Text(
                    text = "显示设置",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(
                        start = Spacing.md,
                        top = Spacing.md,
                        bottom = Spacing.xs
                    )
                )
            }
            item {
                SwitchSettingItem(
                    title = "显示工具调用",
                    description = "在对话中展示工具调用过程",
                    checked = uiState.showToolCalls,
                    onCheckedChange = viewModel::setShowToolCalls
                )
            }
            item {
                SwitchSettingItem(
                    title = "显示思考链",
                    description = "在对话中展示 AI 的思考过程",
                    checked = uiState.showReasoning,
                    onCheckedChange = viewModel::setShowReasoning
                )
            }
            item {
                SwitchSettingItem(
                    title = "显示消息时间戳",
                    description = "在消息旁显示发送时间",
                    checked = uiState.showTimestamps,
                    onCheckedChange = viewModel::setShowTimestamps
                )
            }
            item {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = Spacing.sm),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }

            // 行为设置
            item {
                Text(
                    text = "行为设置",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(
                        start = Spacing.md,
                        top = Spacing.md,
                        bottom = Spacing.xs
                    )
                )
            }
            item {
                DropdownSettingItem(
                    title = "每页会话数",
                    description = "会话列表每页显示的会话数量",
                    value = "${uiState.pageSize}",
                    options = listOf("10", "20", "50"),
                    onOptionSelected = {
                        viewModel.setPageSize(it.toInt())
                    }
                )
            }
            item {
                SwitchSettingItem(
                    title = "自动展开思考链",
                    description = "自动展开 AI 的思考链内容",
                    checked = uiState.autoExpandReasoning,
                    onCheckedChange = viewModel::setAutoExpandReasoning
                )
            }
            item {
                SwitchSettingItem(
                    title = "自动展开工具结果",
                    description = "自动展开工具调用的返回结果",
                    checked = uiState.autoExpandTools,
                    onCheckedChange = viewModel::setAutoExpandTools
                )
            }
            item {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = Spacing.sm),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }

            // 外观设置
            item {
                Text(
                    text = "外观设置",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(
                        start = Spacing.md,
                        top = Spacing.md,
                        bottom = Spacing.xs
                    )
                )
            }
            item {
                val themeLabel = when (uiState.themeMode) {
                    "light" -> "浅色"
                    "dark" -> "深色"
                    else -> "跟随系统"
                }
                DropdownSettingItem(
                    title = "主题模式",
                    description = "应用的主题外观",
                    value = themeLabel,
                    options = listOf("跟随系统", "浅色", "深色"),
                    optionValues = listOf("system", "light", "dark"),
                    onOptionSelected = { viewModel.setThemeMode(it) }
                )
            }
            item {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = Spacing.sm),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }

            // 连接设置
            item {
                Text(
                    text = "连接设置",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(
                        start = Spacing.md,
                        top = Spacing.md,
                        bottom = Spacing.xs
                    )
                )
            }
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.md)
                ) {
                    var ipText by remember { mutableStateOf(uiState.connectionIp) }
                    var portText by remember { mutableStateOf(uiState.connectionPort) }
                    var tokenText by remember { mutableStateOf(uiState.connectionToken) }

                    OutlinedTextField(
                        value = ipText,
                        onValueChange = { ipText = it },
                        label = {
                            Text(
                                text = "IP 地址",
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    )
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    OutlinedTextField(
                        value = portText,
                        onValueChange = { portText = it },
                        label = {
                            Text(
                                text = "端口",
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    )
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    OutlinedTextField(
                        value = tokenText,
                        onValueChange = { tokenText = it },
                        label = {
                            Text(
                                text = "Token",
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    )
                    Spacer(modifier = Modifier.height(Spacing.md))
                    Button(
                        onClick = {
                            viewModel.setConnectionIp(ipText)
                            viewModel.setConnectionPort(portText)
                            viewModel.setConnectionToken(tokenText)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = "保存连接配置",
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Button(
                        onClick = { viewModel.disconnect() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = "断开当前连接",
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SwitchSettingItem(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        },
        supportingContent = {
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground
            )
        },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    )
}

@Composable
private fun DropdownSettingItem(
    title: String,
    description: String,
    value: String,
    options: List<String>,
    optionValues: List<String> = emptyList(),
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val actualValues = if (optionValues.isEmpty()) options else optionValues

    Box {
        ListItem(
            headlineContent = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            },
            supportingContent = {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
            },
            trailingContent = {
                TextButton(onClick = { expanded = true }) {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEachIndexed { index, option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    },
                    onClick = {
                        onOptionSelected(actualValues[index])
                        expanded = false
                    }
                )
            }
        }
    }
}
