package com.seina.chan.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seina.chan.data.remote.ConnectionState
import com.seina.chan.data.remote.HermesWsClient
import com.seina.chan.data.repository.ChatRepository
import com.seina.chan.data.repository.ConnectionRepository
import com.seina.chan.data.repository.SessionRepository
import com.seina.chan.util.FileLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val sessionRepository: SessionRepository,
    private val connectionRepository: ConnectionRepository,
    private val wsClient: HermesWsClient
) : ViewModel() {

    private val _inputState = MutableStateFlow(ChatUiState())

    private var currentDbSessionId: String = ""
    private var currentWsSessionId: String = ""

    val events = chatRepository.events

    init {
        viewModelScope.launch {
            try {
                val lastId = connectionRepository.loadLastDbSessionId()
                if (!lastId.isNullOrEmpty()) {
                    currentDbSessionId = lastId
                    FileLogger.i("ChatViewModel", "Restored last dbSessionId=$lastId")
                }
            } catch (e: Exception) {
                FileLogger.w("ChatViewModel", "Failed to load last dbSessionId: ${e.message}")
            }
        }
        viewModelScope.launch {
            var previousState: ConnectionState = ConnectionState.Idle
            wsClient.state.collect { state ->
                if ((previousState is ConnectionState.Closed || previousState is ConnectionState.Error)
                    && state is ConnectionState.Open
                    && currentDbSessionId.isNotEmpty()
                ) {
                    FileLogger.i("ChatViewModel", "WebSocket reconnected, resuming session=$currentDbSessionId")
                    try {
                        val sid = sessionRepository.resumeSession(currentDbSessionId)
                        currentWsSessionId = sid
                        FileLogger.i("ChatViewModel", "Auto-resume after reconnect succeeded, sid=$sid")
                    } catch (e: Exception) {
                        FileLogger.w("ChatViewModel", "Auto-resume after reconnect failed: ${e.message}")
                    }
                }
                previousState = state
            }
        }
    }

    val uiState: StateFlow<ChatUiState> = combine(
        _inputState,
        chatRepository.messages
    ) { inputState, messages ->
        inputState.copy(
            messages = messages,
            canSend = inputState.currentInput.isNotBlank() && !inputState.isLoading
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ChatUiState()
    )

    fun onInputChange(text: String) {
        _inputState.update { it.copy(currentInput = text) }
    }

    suspend fun ensureSession(): String {
        if (currentDbSessionId.isNotEmpty()) return currentDbSessionId
        val result = sessionRepository.createSession()
        currentDbSessionId = result.storedSessionId
        currentWsSessionId = result.sid
        try {
            connectionRepository.saveLastDbSessionId(currentDbSessionId)
        } catch (e: Exception) {
            FileLogger.w("ChatViewModel", "ensureSession() failed to save dbSessionId: ${e.message}")
        }
        FileLogger.i("ChatViewModel", "ensureSession() created dbId=${result.storedSessionId}, sid=${result.sid}")
        return currentDbSessionId
    }

    suspend fun resumeSession(): Result<String> {
        if (currentDbSessionId.isEmpty()) {
            return Result.failure(Exception("No session to resume"))
        }
        return try {
            val sid = sessionRepository.resumeSession(currentDbSessionId)
            currentWsSessionId = sid
            FileLogger.i("ChatViewModel", "resumeSession() succeeded: sid=$sid")
            Result.success(sid)
        } catch (e: Exception) {
            FileLogger.e("ChatViewModel", "resumeSession() failed", e)
            Result.failure(e)
        }
    }

    suspend fun resumeSessionWithId(storedSessionId: String): Result<String> {
        currentDbSessionId = storedSessionId
        return try {
            connectionRepository.saveLastDbSessionId(currentDbSessionId)
            val sid = sessionRepository.resumeSession(storedSessionId)
            currentWsSessionId = sid
            FileLogger.i("ChatViewModel", "resumeSessionWithId() succeeded: sid=$sid")
            Result.success(sid)
        } catch (e: Exception) {
            FileLogger.e("ChatViewModel", "resumeSessionWithId() failed", e)
            Result.failure(e)
        }
    }

    fun sendMessage() {
        val text = _inputState.value.currentInput.trim()
        if (text.isEmpty()) return

        FileLogger.i("ChatViewModel", "sendMessage() dbSessionId=$currentDbSessionId, wsSessionId=$currentWsSessionId, textLength=${text.length}")
        _inputState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            if (currentDbSessionId.isEmpty()) {
                ensureSession()
            } else {
                try {
                    val sid = sessionRepository.resumeSession(currentDbSessionId)
                    currentWsSessionId = sid
                    FileLogger.i("ChatViewModel", "sendMessage() resumeSession succeeded, sid=$sid")
                } catch (e: Exception) {
                    FileLogger.w("ChatViewModel", "sendMessage() resumeSession failed, continuing: ${e.message}")
                }
            }
            try {
                chatRepository.sendMessage(text, currentWsSessionId)
                _inputState.update { it.copy(currentInput = "", isLoading = false) }
                FileLogger.i("ChatViewModel", "sendMessage() succeeded")
            } catch (e: Exception) {
                FileLogger.e("ChatViewModel", "sendMessage() failed", e)
                _inputState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun loadMessages(dbSessionId: String) {
        FileLogger.i("ChatViewModel", "loadMessages() dbSessionId=$dbSessionId")
        if (dbSessionId.isEmpty()) {
            _inputState.update { it.copy(error = "sessionId is empty, cannot load messages") }
            return
        }
        currentDbSessionId = dbSessionId
        viewModelScope.launch {
            try {
                connectionRepository.saveLastDbSessionId(currentDbSessionId)
            } catch (e: Exception) {
                FileLogger.w("ChatViewModel", "loadMessages() failed to save dbSessionId: ${e.message}")
            }
        }
        chatRepository.clearMessages()
        _inputState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val history = sessionRepository.fetchMessages(dbSessionId)
                FileLogger.i("ChatViewModel", "loadMessages() loaded ${history.size} messages")
                chatRepository.setMessages(history)
                if (history.isEmpty()) {
                    _inputState.update { it.copy(isLoading = false, error = "该会话暂无历史消息 (sessionId=$dbSessionId)") }
                } else {
                    _inputState.update { it.copy(isLoading = false, error = null) }
                }
            } catch (e: Exception) {
                FileLogger.e("ChatViewModel", "loadMessages() failed", e)
                _inputState.update { it.copy(isLoading = false, error = "加载历史消息失败: ${e.message}") }
            }
        }
    }

    fun getCurrentDbSessionId(): String = currentDbSessionId

    fun respondApproval(requestId: String, approved: Boolean) {
        FileLogger.i("ChatViewModel", "respondApproval() requestId=$requestId, approved=$approved")
        viewModelScope.launch {
            try {
                chatRepository.respondApproval(requestId, approved)
            } catch (e: Exception) {
                FileLogger.e("ChatViewModel", "respondApproval() failed", e)
                _inputState.update { it.copy(error = e.message) }
            }
        }
    }

    fun respondClarify(requestId: String, response: String) {
        FileLogger.i("ChatViewModel", "respondClarify() requestId=$requestId")
        viewModelScope.launch {
            try {
                chatRepository.respondClarify(requestId, response)
            } catch (e: Exception) {
                FileLogger.e("ChatViewModel", "respondClarify() failed", e)
                _inputState.update { it.copy(error = e.message) }
            }
        }
    }

    fun respondSecret(requestId: String, secret: String) {
        FileLogger.i("ChatViewModel", "respondSecret() requestId=$requestId")
        viewModelScope.launch {
            try {
                chatRepository.respondSecret(requestId, secret)
            } catch (e: Exception) {
                FileLogger.e("ChatViewModel", "respondSecret() failed", e)
                _inputState.update { it.copy(error = e.message) }
            }
        }
    }
}
