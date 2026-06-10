package com.seina.chan.ui.components

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.seina.chan.data.remote.ConnectionState
import com.seina.chan.data.remote.GatewayEvent
import com.seina.chan.data.remote.HermesWsClient
import com.seina.chan.data.repository.ConnectionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class GlobalEventViewModel @Inject constructor(
    val connectionRepository: ConnectionRepository,
    private val wsClient: HermesWsClient
) : ViewModel() {
    val connectionState = connectionRepository.connectionState
    val events = wsClient.events
}

@Composable
fun GlobalEventHandler(
    snackbarHostState: SnackbarHostState,
    viewModel: GlobalEventViewModel = hiltViewModel()
) {
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    var previousState by remember { mutableStateOf<ConnectionState?>(null) }
    var hasBeenDisconnected by remember { mutableStateOf(false) }

    LaunchedEffect(connectionState) {
        when {
            previousState is ConnectionState.Connecting && connectionState is ConnectionState.Open -> {
                if (hasBeenDisconnected) {
                    snackbarHostState.showSnackbar("已恢复连接")
                    hasBeenDisconnected = false
                } else {
                    snackbarHostState.showSnackbar("已连接")
                }
            }
            previousState is ConnectionState.Open && (connectionState is ConnectionState.Error || connectionState is ConnectionState.Closed) -> {
                snackbarHostState.showSnackbar("连接断开")
                hasBeenDisconnected = true
            }
        }
        previousState = connectionState
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            if (event is GatewayEvent.ErrorEvent) {
                snackbarHostState.showSnackbar(event.message)
            }
        }
    }
}
