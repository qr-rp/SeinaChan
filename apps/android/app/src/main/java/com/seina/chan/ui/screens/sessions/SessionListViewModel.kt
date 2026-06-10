package com.seina.chan.ui.screens.sessions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seina.chan.data.model.Session
import com.seina.chan.data.remote.ConnectionState
import com.seina.chan.data.repository.ConnectionRepository
import com.seina.chan.data.repository.SessionRepository
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

@HiltViewModel
class SessionListViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val connectionRepository: ConnectionRepository
) : ViewModel() {

    private val _sessions = MutableStateFlow<List<Session>>(emptyList())
    val sessions: StateFlow<List<Session>> = _sessions.asStateFlow()

    val connectionState: StateFlow<ConnectionState> = connectionRepository.connectionState

    private val _navigateToSession = MutableSharedFlow<String>()
    val navigateToSession: SharedFlow<String> = _navigateToSession.asSharedFlow()

    private val _newSessionCreated = MutableSharedFlow<String>()
    val newSessionCreated: SharedFlow<String> = _newSessionCreated.asSharedFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadSessions() {
        viewModelScope.launch {
            FileLogger.i("SessionListViewModel", "loadSessions() started")
            _isLoading.value = true
            _error.value = null
            try {
                val result = sessionRepository.fetchSessions()
                FileLogger.i("SessionListViewModel", "loadSessions() succeeded, count=${result.size}")
                _sessions.value = result
            } catch (e: Exception) {
                FileLogger.e("SessionListViewModel", "loadSessions() failed", e)
                _sessions.value = emptyList()
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createNewSession() {
        viewModelScope.launch {
            FileLogger.i("SessionListViewModel", "createNewSession() started")
            try {
                val result = sessionRepository.createSession()
                FileLogger.i("SessionListViewModel", "createNewSession() succeeded, storedId=${result.storedSessionId}, sid=${result.sid}")
                _newSessionCreated.emit(result.storedSessionId)
                loadSessions()
            } catch (e: Exception) {
                FileLogger.e("SessionListViewModel", "createNewSession() failed", e)
            }
        }
    }

    fun selectSession(sessionId: String) {
        viewModelScope.launch {
            _navigateToSession.emit(sessionId)
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            connectionRepository.disconnect()
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            FileLogger.i("SessionListViewModel", "deleteSession() id=$sessionId")
            try {
                sessionRepository.deleteSession(sessionId)
                FileLogger.i("SessionListViewModel", "deleteSession() succeeded")
                loadSessions()
            } catch (e: Exception) {
                FileLogger.e("SessionListViewModel", "deleteSession() failed", e)
            }
        }
    }

    fun renameSession(sessionId: String, title: String) {
        viewModelScope.launch {
            FileLogger.i("SessionListViewModel", "renameSession() id=$sessionId, title=$title")
            try {
                sessionRepository.renameSession(sessionId, title)
                FileLogger.i("SessionListViewModel", "renameSession() succeeded")
                loadSessions()
            } catch (e: Exception) {
                FileLogger.e("SessionListViewModel", "renameSession() failed", e)
            }
        }
    }

    fun disconnectAndClear() {
        viewModelScope.launch {
            connectionRepository.disconnect()
            connectionRepository.clearConfig()
        }
    }
}
