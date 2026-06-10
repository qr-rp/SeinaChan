package com.seina.chan.ui.screens.connect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seina.chan.data.model.ConnectionConfig
import com.seina.chan.data.repository.ConnectionRepository
import com.seina.chan.util.FileLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class TestStatus {
    object None : TestStatus()
    object Testing : TestStatus()
    data class Success(val message: String) : TestStatus()
    data class Error(val message: String) : TestStatus()
}

@HiltViewModel
class ConnectViewModel @Inject constructor(
    private val connectionRepository: ConnectionRepository
) : ViewModel() {

    data class ConnectUiState(
        val ip: String = "",
        val port: String = "9119",
        val token: String = "",
        val isLoading: Boolean = false,
        val error: String? = null,
        val testStatus: TestStatus = TestStatus.None
    )

    private val _uiState = MutableStateFlow(ConnectUiState())
    val uiState: StateFlow<ConnectUiState> = _uiState.asStateFlow()

    private val _navigateToChat = MutableSharedFlow<Boolean>()
    val navigateToChat: SharedFlow<Boolean> = _navigateToChat.asSharedFlow()

    init {
        viewModelScope.launch {
            FileLogger.i("ConnectViewModel", "init - loading config")
            val config = connectionRepository.loadConfig()
            if (config != null) {
                val ip = config.ip
                val port = config.port.ifBlank { "9119" }
                _uiState.value = _uiState.value.copy(ip = ip, port = port, token = config.token)
                FileLogger.i("ConnectViewModel", "init - loaded config ip=$ip, port=$port")
            } else {
                FileLogger.i("ConnectViewModel", "init - no saved config")
            }
        }
    }

    fun onIpChange(ip: String) {
        _uiState.value = _uiState.value.copy(ip = ip, error = null, testStatus = TestStatus.None)
    }

    fun onPortChange(port: String) {
        _uiState.value = _uiState.value.copy(port = port, error = null, testStatus = TestStatus.None)
    }

    fun onTokenChange(token: String) {
        _uiState.value = _uiState.value.copy(token = token, error = null)
    }

    fun testConnection() {
        viewModelScope.launch {
            val ip = _uiState.value.ip.trim()
            val port = _uiState.value.port.trim()
            FileLogger.i("ConnectViewModel", "testConnection() ip=$ip, port=$port")

            if (ip.isBlank() || port.isBlank()) {
                _uiState.value = _uiState.value.copy(testStatus = TestStatus.Error("请输入 IP 和端口"))
                return@launch
            }

            _uiState.value = _uiState.value.copy(testStatus = TestStatus.Testing)
            val result = connectionRepository.testConnection(ip, port)
            if (result.isSuccess) {
                FileLogger.i("ConnectViewModel", "testConnection() succeeded")
                _uiState.value = _uiState.value.copy(testStatus = TestStatus.Success(result.getOrDefault("连接成功")))
            } else {
                val message = result.exceptionOrNull()?.message ?: "连接失败"
                FileLogger.e("ConnectViewModel", "testConnection() failed: $message")
                _uiState.value = _uiState.value.copy(testStatus = TestStatus.Error(message))
            }
        }
    }

    fun connect() {
        viewModelScope.launch {
            val ip = _uiState.value.ip.trim()
            val port = _uiState.value.port.trim()
            val token = _uiState.value.token.trim()
            FileLogger.i("ConnectViewModel", "connect() ip=$ip, port=$port, tokenPrefix=${token.take(4)}")

            if (ip.isBlank()) {
                _uiState.value = _uiState.value.copy(error = "请输入 WSL IP 地址")
                return@launch
            }
            if (port.isBlank()) {
                _uiState.value = _uiState.value.copy(error = "请输入端口")
                return@launch
            }

            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val config = ConnectionConfig(ip = ip, port = port, token = token)
            val result = connectionRepository.connect(config)

            if (result.isSuccess) {
                FileLogger.i("ConnectViewModel", "connect() succeeded")
                connectionRepository.saveConfig(config)
                _navigateToChat.emit(true)
            } else {
                val message = result.exceptionOrNull()?.message ?: "连接失败"
                FileLogger.e("ConnectViewModel", "connect() failed: $message")
                _uiState.value = _uiState.value.copy(isLoading = false, error = message)
            }
        }
    }

    fun clearSavedConfig() {
        viewModelScope.launch {
            FileLogger.i("ConnectViewModel", "clearSavedConfig() called")
            connectionRepository.disconnect()
            connectionRepository.clearConfig()
            _uiState.value = ConnectUiState()
        }
    }
}
