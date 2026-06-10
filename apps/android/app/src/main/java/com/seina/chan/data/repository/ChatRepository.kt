package com.seina.chan.data.repository

import android.content.ContentResolver
import android.net.Uri
import android.util.Base64
import com.seina.chan.data.model.ChatMessage
import com.seina.chan.data.model.ToolCallDetail
import com.seina.chan.data.model.ToolCallStatus
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
import kotlinx.coroutines.withContext
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

    /**
     * 发送图片消息
     * @param imageUri 图片 URI
     * @param contentResolver 用于读取图片内容
     * @param sessionId 会话 ID
     */
    suspend fun sendImage(imageUri: Uri, contentResolver: ContentResolver, sessionId: String) {
        // 读取图片并转为 base64
        val base64Data = withContext(Dispatchers.IO) {
            contentResolver.openInputStream(imageUri)?.use { inputStream ->
                val bytes = inputStream.readBytes()
                Base64.encodeToString(bytes, Base64.NO_WRAP)
            } ?: throw IllegalArgumentException("无法读取图片")
        }

        // 在本地消息列表添加用户图片消息占位
        val userMessage = ChatMessage(
            id = java.util.UUID.randomUUID().toString(),
            role = "user",
            content = "",
            isStreaming = false,
            imageUrl = imageUri.toString()
        )
        _messages.value += userMessage

        // 构造 data URI
        val dataUri = "data:image/jpeg;base64,$base64Data"
        val params = buildJsonObject {
            put("session_id", sessionId)
            put("data", dataUri)
            put("name", "image.jpg")
        }
        wsClient.request("image.attach_bytes", params)
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
                    isStreaming = true,
                    isReasoning = true
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
                        msg.copy(
                            isStreaming = false,
                            isReasoning = false,
                            reasoningText = event.reasoning.ifBlank { msg.reasoningText }
                        )
                    } else msg
                }
            }
            is GatewayEvent.ReasoningDelta -> {
                updateLastStreamingAssistantMessage { it.copy(reasoningText = it.reasoningText + event.text) }
            }
            is GatewayEvent.ThinkingDelta -> {
                updateLastStreamingAssistantMessage { it.copy(reasoningText = it.reasoningText + event.text) }
            }
            is GatewayEvent.ReasoningAvailable -> {
                updateLastStreamingAssistantMessage { it.copy(reasoningText = event.text) }
            }
            is GatewayEvent.ToolStart -> {
                val toolCall = ToolCallDetail(
                    id = event.toolId,
                    name = event.name,
                    args = event.args,
                    status = ToolCallStatus.Running
                )
                appendToolCallToStreamingMessage(toolCall)
            }
            is GatewayEvent.ToolProgress -> {
                updateToolCall(event.toolId) { it.copy(result = it.result + event.text) }
            }
            is GatewayEvent.ToolComplete -> {
                updateToolCall(event.toolId) {
                    it.copy(
                        status = ToolCallStatus.Success,
                        result = event.result,
                        duration = event.duration,
                        summary = event.summary
                    )
                }
            }
            is GatewayEvent.ApprovalRequest -> {
                val toolCall = ToolCallDetail(
                    id = event.id,
                    name = event.toolName,
                    args = event.input.toString(),
                    status = ToolCallStatus.Running
                )
                appendToolCallToStreamingMessage(toolCall)
            }
            else -> Unit
        }
    }

    private fun updateLastStreamingAssistantMessage(transform: (ChatMessage) -> ChatMessage) {
        _messages.value = _messages.value.mapIndexed { index, msg ->
            if (index == _messages.value.lastIndex && msg.role == "assistant") {
                transform(msg)
            } else msg
        }
    }

    private fun appendToolCallToStreamingMessage(toolCall: ToolCallDetail) {
        _messages.value = _messages.value.map { msg ->
            if (msg.isStreaming && msg.role == "assistant") {
                msg.copy(toolCalls = msg.toolCalls + toolCall)
            } else msg
        }
    }

    private fun updateToolCall(id: String, transform: (ToolCallDetail) -> ToolCallDetail) {
        _messages.value = _messages.value.map { msg ->
            msg.copy(toolCalls = msg.toolCalls.map {
                if (it.id == id) transform(it) else it
            })
        }
    }
}
