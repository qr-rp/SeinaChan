package com.seina.chan.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.seina.chan.data.remote.GatewayEvent
import com.seina.chan.ui.components.dialogs.ApprovalDialog
import com.seina.chan.util.FileLogger
import com.seina.chan.ui.components.dialogs.ClarifyDialog
import com.seina.chan.ui.components.dialogs.SecretDialog
import com.seina.chan.ui.screens.sessions.SessionListScreen
import com.seina.chan.ui.theme.Canvas
import com.seina.chan.ui.theme.Ink
import com.seina.chan.ui.theme.Primary
import com.seina.chan.ui.theme.Spacing
import com.seina.chan.ui.theme.SurfaceCard
import com.seina.chan.ui.theme.TextStyles
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    sessionId: String,
    onBack: () -> Unit,
    onReconfigure: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var currentSessionId by rememberSaveable { mutableStateOf(sessionId) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val pendingApproval = remember { mutableStateOf<GatewayEvent.ApprovalRequest?>(null) }
    val pendingClarify = remember { mutableStateOf<GatewayEvent.ClarifyRequest?>(null) }
    val pendingSecret = remember { mutableStateOf<GatewayEvent.SecretRequest?>(null) }

    LaunchedEffect(currentSessionId) {
        if (currentSessionId.isNotEmpty()) {
            viewModel.loadMessages(currentSessionId)
        }
    }

    LaunchedEffect(Unit) {
        if (currentSessionId.isEmpty()) {
            scope.launch {
                currentSessionId = viewModel.ensureSession()
            }
        } else {
            scope.launch {
                val result = viewModel.resumeSessionWithId(currentSessionId)
                if (result.isFailure) {
                    FileLogger.w("ChatScreen", "resumeSession failed, will create new")
                    currentSessionId = viewModel.ensureSession()
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is GatewayEvent.ApprovalRequest -> pendingApproval.value = event
                is GatewayEvent.ClarifyRequest -> pendingClarify.value = event
                is GatewayEvent.SecretRequest -> pendingSecret.value = event
                else -> Unit
            }
        }
    }

    ApprovalDialog(
        request = pendingApproval.value,
        onApprove = {
            pendingApproval.value?.let {
                viewModel.respondApproval(it.id, approved = true)
                pendingApproval.value = null
            }
        },
        onReject = {
            pendingApproval.value?.let {
                viewModel.respondApproval(it.id, approved = false)
                pendingApproval.value = null
            }
        },
        onDismiss = {
            pendingApproval.value?.let {
                viewModel.respondApproval(it.id, approved = false)
                pendingApproval.value = null
            }
        }
    )

    ClarifyDialog(
        request = pendingClarify.value,
        onRespond = { response ->
            pendingClarify.value?.let {
                viewModel.respondClarify(it.id, response)
                pendingClarify.value = null
            }
        },
        onDismiss = { pendingClarify.value = null }
    )

    SecretDialog(
        request = pendingSecret.value,
        onRespond = { secret ->
            pendingSecret.value?.let {
                viewModel.respondSecret(it.id, secret)
                pendingSecret.value = null
            }
        },
        onDismiss = { pendingSecret.value = null }
    )

    val title = if (currentSessionId.isEmpty()) "口袋星奈" else currentSessionId.take(8)

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.8f)
                    .background(SurfaceCard)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "会话",
                        style = TextStyles.displaySm,
                        color = Ink,
                        modifier = Modifier.padding(Spacing.md)
                    )
                    SessionListScreen(
                        viewModel = hiltViewModel(),
                        onSessionSelected = { selectedId ->
                            scope.launch {
                                viewModel.resumeSessionWithId(selectedId)
                                currentSessionId = selectedId
                                drawerState.close()
                            }
                        },
                        onNewSession = { newSessionId ->
                            currentSessionId = newSessionId
                            scope.launch { drawerState.close() }
                        },
                        onReconfigure = {
                            scope.launch { drawerState.close() }
                            onReconfigure()
                        }
                    )
                }
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Canvas)
                .safeDrawingPadding()
                .imePadding()
        ) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "菜单",
                        tint = Ink
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = title,
                    style = TextStyles.bodyMd,
                    color = Ink,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onReconfigure) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "设置",
                        tint = Ink
                    )
                }
            }

            // Messages area
            if (uiState.messages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState.isLoading) {
                        androidx.compose.material3.CircularProgressIndicator(color = Primary)
                    } else {
                        Text(
                            text = uiState.error ?: "开始聊天吧",
                            style = TextStyles.bodyMd,
                            color = if (uiState.error != null) Primary else Ink
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = Spacing.md),
                    reverseLayout = false
                ) {
                    items(uiState.messages, key = { it.id }) { message ->
                        MessageBubble(message = message)
                    }
                }
            }

            // Error message
            uiState.error?.let { error ->
                Text(
                    text = error,
                    style = TextStyles.caption,
                    color = Primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.md)
                )
            }

            // Composer
            Composer(
                value = uiState.currentInput,
                onValueChange = viewModel::onInputChange,
                onSend = viewModel::sendMessage,
                sendEnabled = uiState.canSend
            )
        }
    }
}
