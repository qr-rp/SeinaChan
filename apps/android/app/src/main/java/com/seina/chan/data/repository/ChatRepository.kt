package com.seina.chan.data.repository

import com.seina.chan.data.model.ChatMessage
import com.seina.chan.data.model.ToolCall
import com.seina.chan.data.model.ToolStatus
import com.seina.chan.data.remote.GatewayEvent
import com.seina.chan.data.remote.HermesWsClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class ChatRepository(
    private val wsClient: HermesWsClient
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    val events = wsClient.events

    init {
        wsClient.events.onEach { event ->
            handleEvent(event)
        }.launchIn(scope)
    }

    suspend fun sendMessage(text: String, sessionId: String) {
        val userMessage = ChatMessage(
            id = java.util.UUID.randomUUID().toString(),
            role = "user",
            content = text,
            isStreaming = false
        )
        _messages.value += userMessage

        val params = buildJsonObject {
            put("session_id", sessionId)
            put("text", text)
        }
        wsClient.request("prompt.submit", params)
    }

    suspend fun respondApproval(requestId: String, approved: Boolean) {
        val params = buildJsonObject {
            put("requestId", requestId)
            put("approved", approved)
        }
        wsClient.request("approval.respond", params)
    }

    suspend fun respondClarify(requestId: String, response: String) {
        val params = buildJsonObject {
            put("requestId", requestId)
            put("response", response)
        }
        wsClient.request("clarify.respond", params)
    }

    suspend fun respondSecret(requestId: String, secret: String) {
        val params = buildJsonObject {
            put("requestId", requestId)
            put("secret", secret)
        }
        wsClient.request("secret.respond", params)
    }

    fun clearMessages() {
        _messages.value = emptyList()
    }

    fun setMessages(messages: List<ChatMessage>) {
        _messages.value = messages
    }

    private fun handleEvent(event: GatewayEvent) {
        when (event) {
            is GatewayEvent.MessageStart -> {
                val msgId = event.id.ifBlank { "streaming" }
                val msg = ChatMessage(
                    id = msgId,
                    role = event.role.ifBlank { "assistant" },
                    content = "",
                    isStreaming = true
                )
                _messages.value += msg
            }
            is GatewayEvent.MessageDelta -> {
                _messages.value = _messages.value.mapIndexed { index, msg ->
                    if (index == _messages.value.lastIndex && msg.isStreaming && msg.role == "assistant") {
                        msg.copy(content = msg.content + event.delta)
                    } else msg
                }
            }
            is GatewayEvent.MessageComplete -> {
                _messages.value = _messages.value.mapIndexed { index, msg ->
                    if (index == _messages.value.lastIndex && msg.isStreaming && msg.role == "assistant") {
                        msg.copy(isStreaming = false)
                    } else msg
                }
            }
            is GatewayEvent.ThinkingDelta -> {
                // Thinking content is not stored separately in ChatMessage currently
            }
            is GatewayEvent.ToolStart -> {
                val toolCall = ToolCall(
                    id = event.id,
                    toolName = event.toolName,
                    status = ToolStatus.Running,
                    input = event.input.toString()
                )
                appendToolCallToStreamingMessage(toolCall)
            }
            is GatewayEvent.ToolProgress -> {
                updateToolCall(event.id) { it.copy(output = it.output + event.content) }
            }
            is GatewayEvent.ToolComplete -> {
                updateToolCall(event.id) {
                    it.copy(status = ToolStatus.Completed, output = event.output ?: it.output)
                }
            }
            is GatewayEvent.ApprovalRequest -> {
                val toolCall = ToolCall(
                    id = event.id,
                    toolName = event.toolName,
                    status = ToolStatus.Pending,
                    input = event.input.toString()
                )
                appendToolCallToStreamingMessage(toolCall)
            }
            else -> Unit
        }
    }

    private fun updateMessage(id: String, transform: (ChatMessage) -> ChatMessage) {
        _messages.value = _messages.value.map {
            if (it.id == id) transform(it) else it
        }
    }

    private fun appendToolCallToStreamingMessage(toolCall: ToolCall) {
        _messages.value = _messages.value.map { msg ->
            if (msg.isStreaming && msg.role == "assistant") {
                msg.copy(toolCalls = msg.toolCalls + toolCall)
            } else msg
        }
    }

    private fun updateToolCall(id: String, transform: (ToolCall) -> ToolCall) {
        _messages.value = _messages.value.map { msg ->
            msg.copy(toolCalls = msg.toolCalls.map {
                if (it.id == id) transform(it) else it
            })
        }
    }
}
