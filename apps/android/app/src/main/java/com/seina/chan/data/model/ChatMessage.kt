package com.seina.chan.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ChatMessage(
    val id: String,
    val role: String,
    val content: String,
    val isStreaming: Boolean = false,
    val reasoningText: String = "",
    val isReasoning: Boolean = false,
    val toolCalls: List<ToolCallDetail> = emptyList(),
    val imageUrl: String? = null,
    val systemEvents: List<String> = emptyList(),
    val parentId: String? = null
)

@Serializable
data class ToolCallDetail(
    val id: String,
    val name: String,
    val args: String = "",
    val result: String = "",
    val duration: Float? = null,
    val status: ToolCallStatus,
    val summary: String = ""
)

@Serializable
enum class ToolCallStatus {
    Running,
    Success,
    Failed
}
