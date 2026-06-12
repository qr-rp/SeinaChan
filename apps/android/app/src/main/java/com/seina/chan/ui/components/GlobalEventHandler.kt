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
import androidx.lifecycle.viewModelScope
import com.seina.chan.data.remote.ConnectionState
import com.seina.chan.data.remote.GatewayEvent
import com.seina.chan.data.remote.HermesWsClient
import com.seina.chan.data.repository.ConnectionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GlobalEventViewModel @Inject constructor(
    val connectionRepository: ConnectionRepository,
    private val wsClient: HermesWsClient
) : ViewModel() {
    val connectionState = connectionRepository.connectionState
    val events = wsClient.events

    private val _stableConnectionState = MutableStateFlow<ConnectionState>(connectionRepository.connectionState.value)
    val stableConnectionState = _stableConnectionState.asStateFlow()

    private var debounceJob: Job? = null

    init {
        viewModelScope.launch {
            connectionState.collect { state ->
                debounceJob?.cancel()
                when (state) {
                    is ConnectionState.Open -> {
                        _stableConnectionState.value = ConnectionState.Open
                    }
                    is ConnectionState.Connecting -> {
                        debounceJob = launch {
                            delay(1000L)
                            _stableConnectionState.value = ConnectionState.Connecting
                        }
                    }
                    is ConnectionState.Closed, is ConnectionState.Error -> {
                        debounceJob = launch {
                            delay(2000L)
                            _stableConnectionState.value = state
                        }
                    }
                    else -> {
                        _stableConnectionState.value = state
                    }
                }
            }
        }
    }
}

@Composable
fun GlobalEventHandler(
    snackbarHostState: SnackbarHostState,
    viewModel: GlobalEventViewModel = hiltViewModel()
) {
    val connectionState by viewModel.stableConnectionState.collectAsStateWithLifecycle()
    var previousState by remember { mutableStateOf<ConnectionState?>(null) }

    LaunchedEffect(connectionState) {
        when {
            connectionState is ConnectionState.Open && previousState !is ConnectionState.Open -> {
                snackbarHostState.showSnackbar("已连接")
            }
            (connectionState is ConnectionState.Closed || connectionState is ConnectionState.Error) && previousState is ConnectionState.Open -> {
                snackbarHostState.showSnackbar("连接断开")
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
