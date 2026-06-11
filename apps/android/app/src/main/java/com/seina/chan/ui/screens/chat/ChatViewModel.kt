package com.seina.chan.ui.screens.chat

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seina.chan.data.remote.ConnectionState
import com.seina.chan.data.remote.HermesWsClient
import com.seina.chan.data.repository.ChatRepository
import com.seina.chan.data.repository.ConnectionRepository
import com.seina.chan.data.repository.SessionRepository
import com.seina.chan.data.repository.SettingsRepository
import com.seina.chan.util.FileLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    private val settingsRepository: SettingsRepository,
    private val wsClient: HermesWsClient,
    @ApplicationContext private val context: Context
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
        viewModelScope.launch {
            settingsRepository.showToolCalls.collect { value ->
                _inputState.update { it.copy(showToolCalls = value) }
            }
        }
        viewModelScope.launch {
            settingsRepository.showReasoning.collect { value ->
                _inputState.update { it.copy(showReasoning = value) }
            }
        }
        viewModelScope.launch {
            settingsRepository.hiddenToolNames.collect { value ->
                _inputState.update { it.copy(hiddenToolNames = value) }
            }
        }
    }

    val uiState: StateFlow<ChatUiState> = combine(
        _inputState,
        chatRepository.messages
    ) { inputState, messages ->
        inputState.copy(
            messages = messages,
            canSend = (inputState.currentInput.isNotBlank() || inputState.selectedImages.isNotEmpty()) && !inputState.isLoading
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
        val images = _inputState.value.selectedImages
        if (text.isEmpty() && images.isEmpty()) return

        FileLogger.i("ChatViewModel", "sendMessage() dbSessionId=$currentDbSessionId, wsSessionId=$currentWsSessionId, textLength=${text.length}, images=${images.size}")
        _inputState.update { it.copy(isLoading = true, error = null, selectedImages = emptyList()) }
        viewModelScope.launch {
            try {
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
                if (images.isNotEmpty()) {
                    sendImagesInternal(images)
                }
                if (text.isNotEmpty()) {
                    chatRepository.sendMessage(text, currentWsSessionId)
                } else if (images.isNotEmpty()) {
                    // 纯图片场景：发送空 prompt 触发 assistant 回复
                    chatRepository.submitPrompt(currentWsSessionId)
                }
                _inputState.update { it.copy(currentInput = "", isLoading = false) }
                FileLogger.i("ChatViewModel", "sendMessage() succeeded")
            } catch (e: Exception) {
                FileLogger.e("ChatViewModel", "sendMessage() failed", e)
                _inputState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    /**
     * 设置选中的图片列表
     */
    fun onImagesSelected(uris: List<Uri>) {
        _inputState.update { it.copy(selectedImages = uris) }
    }

    /**
     * 移除一张选中的图片
     */
    fun removeSelectedImage(uri: Uri) {
        _inputState.update { it.copy(selectedImages = it.selectedImages.filter { u -> u != uri }) }
    }

    private suspend fun sendImagesInternal(uris: List<Uri>) {
        for (uri in uris) {
            try {
                chatRepository.sendImage(uri, context.contentResolver, currentWsSessionId)
                FileLogger.i("ChatViewModel", "sendImage() succeeded for uri=$uri")
            } catch (e: Exception) {
                FileLogger.e("ChatViewModel", "sendImage() failed for uri=$uri", e)
                // 继续发送其余图片，不中断
            }
        }
    }

    /**
     * 发送图片消息
     * @param uri 选择的图片 URI
     */
    fun sendImage(uri: Uri) {
        FileLogger.i("ChatViewModel", "sendImage() dbSessionId=$currentDbSessionId, wsSessionId=$currentWsSessionId, uri=$uri")
        _inputState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                if (currentDbSessionId.isEmpty()) {
                    ensureSession()
                } else {
                    try {
                        val sid = sessionRepository.resumeSession(currentDbSessionId)
                        currentWsSessionId = sid
                        FileLogger.i("ChatViewModel", "sendImage() resumeSession succeeded, sid=$sid")
                    } catch (e: Exception) {
                        FileLogger.w("ChatViewModel", "sendImage() resumeSession failed, continuing: ${e.message}")
                    }
                }
                chatRepository.sendImage(uri, context.contentResolver, currentWsSessionId)
                _inputState.update { it.copy(isLoading = false) }
                FileLogger.i("ChatViewModel", "sendImage() succeeded")
            } catch (e: Exception) {
                FileLogger.e("ChatViewModel", "sendImage() failed", e)
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
        _inputState.update { it.copy(isLoading = true, error = null) }

        // 先从 Room 缓存加载，立即展示
        viewModelScope.launch {
            try {
                val cached = chatRepository.loadCachedMessages(dbSessionId)
                if (cached.isNotEmpty()) {
                    FileLogger.i("ChatViewModel", "loadMessages() showed ${cached.size} cached messages")
                    _inputState.update { it.copy(isLoading = false, error = null) }
                }
            } catch (e: Exception) {
                FileLogger.w("ChatViewModel", "loadMessages() cache load failed: ${e.message}")
            }
        }

        // 后台从服务端拉取最新消息
        viewModelScope.launch {
            try {
                val history = sessionRepository.fetchMessages(dbSessionId)
                FileLogger.i("ChatViewModel", "loadMessages() fetched ${history.size} messages from server")
                chatRepository.setMessages(history)
                _inputState.update { it.copy(isLoading = false, error = null) }
            } catch (e: Exception) {
                FileLogger.e("ChatViewModel", "loadMessages() server fetch failed", e)
                // 如果缓存为空且服务端也失败，显示错误
                if (chatRepository.messages.value.isEmpty()) {
                    _inputState.update { it.copy(isLoading = false, error = "加载历史消息失败: ${e.message}") }
                } else {
                    // 有缓存时仅降级提示，不覆盖已展示的内容
                    _inputState.update { it.copy(isLoading = false) }
                }
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
