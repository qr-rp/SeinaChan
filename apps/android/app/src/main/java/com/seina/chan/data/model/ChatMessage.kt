package com.seina.chan.data.model

data class ChatMessage(
    val id: String,
    val role: String,
    val content: String,
    val isStreaming: Boolean = false,
    val toolCalls: List<ToolCall> = emptyList()
)

data class ToolCall(
    val id: String,
    val toolName: String,
    val status: ToolStatus,
    val input: String = "",
    val output: String = ""
)

enum class ToolStatus {
    Pending,
    Running,
    Completed,
    Failed
}
