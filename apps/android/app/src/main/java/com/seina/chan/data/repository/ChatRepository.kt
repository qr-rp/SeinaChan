package com.seina.chan.data.repository

import android.content.ContentResolver
import android.net.Uri
import android.util.Base64
import com.seina.chan.data.local.dao.SentImageDao
import com.seina.chan.data.local.entity.SentImageEntity
import com.seina.chan.data.model.ChatMessage
import com.seina.chan.data.model.ToolCallDetail
import com.seina.chan.data.model.ToolCallStatus
import com.seina.chan.data.remote.GatewayEvent
import com.seina.chan.data.remote.HermesWsClient
import com.seina.chan.util.FileLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

class ChatRepository(
    private val wsClient: HermesWsClient,
    private val sentImageDao: SentImageDao
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
        // 结束所有未完成的 assistant 消息，避免积累空消息
        _messages.value = _messages.value.map {
            if (it.isStreaming && it.role == "assistant") {
                it.copy(isStreaming = false, isReasoning = false)
            } else it
        }

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
        // 结束所有未完成的 assistant 消息，避免积累空消息
        _messages.value = _messages.value.map {
            if (it.isStreaming && it.role == "assistant") {
                it.copy(isStreaming = false, isReasoning = false)
            } else it
        }

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
        val result = wsClient.request("image.attach_bytes", params)
        if (result is JsonObject) {
            val serverPath = result["path"]?.jsonPrimitive?.content
            if (serverPath != null) {
                sentImageDao.insert(
                    SentImageEntity(
                        serverPath = serverPath,
                        localUri = imageUri.toString()
                    )
                )
            }
        }
    }

    /**
     * 提交空 prompt 以触发 assistant 回复（用于纯图片发送场景）
     */
    suspend fun submitPrompt(sessionId: String) {
        val params = buildJsonObject {
            put("session_id", sessionId)
            put("text", "")
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
        try {
            when (event) {
                is GatewayEvent.MessageStart -> {
                    // 结束所有已有的 assistant streaming 消息，避免服务器发送多个 message.start 时积累空消息
                    // 同时将该消息中仍在 Running 的工具调用标记为完成（兼容 Server 未发送 tool.complete 的情况）
                    _messages.value = _messages.value.map {
                        if (it.isStreaming && it.role == "assistant") {
                            finalizeToolCallsInMessage(
                                it.copy(isStreaming = false, isReasoning = false)
                            )
                        } else it
                    }
                    val msgId = event.id.ifBlank { java.util.UUID.randomUUID().toString() }
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
                    updateLastStreamingAssistantMessage { msg ->
                        msg.copy(content = msg.content + event.delta)
                    }
                }
                is GatewayEvent.MessageComplete -> {
                    val messages = _messages.value.toMutableList()
                    val index = messages.indexOfLast { it.isStreaming && it.role == "assistant" }
                    if (index >= 0) {
                        val msg = messages[index]
                        messages[index] = finalizeToolCallsInMessage(
                            msg.copy(
                                isStreaming = false,
                                isReasoning = false,
                                reasoningText = event.reasoning.ifBlank { msg.reasoningText }
                            )
                        )
                        _messages.value = messages
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
                    val displayArgs = event.context.ifBlank { event.args }.ifBlank { event.name }
                    val toolCall = ToolCallDetail(
                        id = event.toolId,
                        name = event.name,
                        args = displayArgs,
                        status = ToolCallStatus.Running
                    )
                    appendToolCallToStreamingMessage(toolCall)
                }
                is GatewayEvent.ToolProgress -> {
                    updateToolCall(event.toolId) { it.copy(result = it.result + event.text) }
                }
                is GatewayEvent.ToolComplete -> {
                    val resultText = when (val r = event.result) {
                        is kotlinx.serialization.json.JsonPrimitive -> r.content
                        is kotlinx.serialization.json.JsonArray -> r.joinToString("\n") { it.toString() }
                        is kotlinx.serialization.json.JsonObject -> r.entries.joinToString("\n") { "${it.key}: ${it.value}" }
                        else -> r?.toString() ?: ""
                    }
                    val found = _messages.value.any { msg -> msg.toolCalls.any { it.id == event.toolId } }
                    if (found) {
                        updateToolCall(event.toolId) {
                            it.copy(
                                status = ToolCallStatus.Success,
                                result = resultText,
                                duration = event.duration,
                                summary = event.summary
                            )
                        }
                    } else {
                        // Server 可能未发送 tool.start（progress disabled），直接创建并追加
                        val toolCall = ToolCallDetail(
                            id = event.toolId,
                            name = event.name,
                            args = event.name,
                            status = ToolCallStatus.Success,
                            result = resultText,
                            duration = event.duration,
                            summary = event.summary
                        )
                        appendToolCallToStreamingMessage(toolCall)
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
                is GatewayEvent.ReviewSummary -> {
                    val messages = _messages.value.toMutableList()
                    val index = messages.indexOfLast { it.role == "assistant" }
                    if (index >= 0) {
                        val msg = messages[index]
                        messages[index] = msg.copy(
                            systemEvents = msg.systemEvents + event.text
                        )
                        _messages.value = messages
                        FileLogger.i("ChatRepository", "ReviewSummary appended: ${event.text}")
                    } else {
                        FileLogger.w("ChatRepository", "ReviewSummary received but no assistant message found")
                    }
                }
                else -> Unit
            }
        } catch (e: Exception) {
            FileLogger.e("ChatRepository", "handleEvent failed for ${event::class.simpleName}", e)
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
        val messages = _messages.value.toMutableList()
        val index = messages.indexOfLast { it.isStreaming && it.role == "assistant" }
        if (index >= 0) {
            messages[index] = messages[index].copy(toolCalls = messages[index].toolCalls + toolCall)
            _messages.value = messages
        }
    }

    private fun updateToolCall(id: String, transform: (ToolCallDetail) -> ToolCallDetail) {
        _messages.value = _messages.value.map { msg ->
            msg.copy(toolCalls = msg.toolCalls.map {
                if (it.id == id) transform(it) else it
            })
        }
    }

    /**
     * 将消息中所有仍在 Running 状态的工具调用标记为完成。
     * 用于兼容 Server 在 tool_progress 关闭时不发送 tool.complete 的情况。
     */
    private fun finalizeToolCallsInMessage(msg: ChatMessage): ChatMessage {
        val finalized = msg.toolCalls.map {
            if (it.status == ToolCallStatus.Running) {
                it.copy(
                    status = ToolCallStatus.Success,
                    result = it.result.ifBlank { "已完成" }
                )
            } else it
        }
        return msg.copy(toolCalls = finalized)
    }
}
