package com.seina.chan.data.remote

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable(GatewayEventSerializer::class)
sealed class GatewayEvent {
    @Serializable
    @SerialName(HermesEventTypes.GATEWAY_READY)
    data object GatewayReady : GatewayEvent()

    @Serializable
    @SerialName(HermesEventTypes.SESSION_INFO)
    data class SessionInfo(
        val id: String,
        val title: String? = null
    ) : GatewayEvent()

    @Serializable
    @SerialName("message.start")
    data class MessageStart(
        val id: String = "",
        @SerialName("parent_id") val parentId: String? = null,
        val role: String = "assistant"
    ) : GatewayEvent()

    @Serializable
    @SerialName(HermesEventTypes.MESSAGE_DELTA)
    data class MessageDelta(
        val id: String = "",
        @SerialName("text") val delta: String
    ) : GatewayEvent()

    @Serializable
    @SerialName(HermesEventTypes.MESSAGE_COMPLETE)
    data class MessageComplete(
        val id: String = "",
        val reasoning: String = ""
    ) : GatewayEvent()

    @Serializable
    @SerialName(HermesEventTypes.REASONING_DELTA)
    data class ReasoningDelta(
        val text: String
    ) : GatewayEvent()

    @Serializable
    @SerialName(HermesEventTypes.THINKING_DELTA)
    data class ThinkingDelta(
        val text: String
    ) : GatewayEvent()

    @Serializable
    @SerialName(HermesEventTypes.REASONING_AVAILABLE)
    data class ReasoningAvailable(
        val text: String
    ) : GatewayEvent()

    @Serializable
    @SerialName(HermesEventTypes.TOOL_START)
    data class ToolStart(
        @SerialName("tool_id") val toolId: String,
        val name: String,
        @SerialName("args_text") val args: String = "",
        @SerialName("context") val context: String = ""
    ) : GatewayEvent()

    @Serializable
    @SerialName(HermesEventTypes.TOOL_PROGRESS)
    data class ToolProgress(
        @SerialName("tool_id") val toolId: String,
        val text: String
    ) : GatewayEvent()

    @Serializable
    @SerialName(HermesEventTypes.TOOL_COMPLETE)
    data class ToolComplete(
        @SerialName("tool_id") val toolId: String,
        val name: String,
        val result: JsonElement? = null,
        @SerialName("duration_s") val duration: Float? = null,
        val summary: String = ""
    ) : GatewayEvent()

    @Serializable
    @SerialName(HermesEventTypes.APPROVAL_REQUEST)
    data class ApprovalRequest(
        val id: String,
        @SerialName("tool_name") val toolName: String,
        val input: Map<String, String> = emptyMap()
    ) : GatewayEvent()

    @Serializable
    @SerialName(HermesEventTypes.CLARIFY_REQUEST)
    data class ClarifyRequest(
        val id: String,
        val question: String
    ) : GatewayEvent()

    @Serializable
    @SerialName(HermesEventTypes.SECRET_REQUEST)
    data class SecretRequest(
        val id: String,
        val prompt: String
    ) : GatewayEvent()

    @Serializable
    @SerialName(HermesEventTypes.REVIEW_SUMMARY)
    data class ReviewSummary(
        val text: String
    ) : GatewayEvent()

    @Serializable
    @SerialName(HermesEventTypes.ERROR)
    data class ErrorEvent(
        val message: String
    ) : GatewayEvent()
}

/**
 * 统一事件反序列化器，根据 JSON 中的 "type" 字段路由到具体的 GatewayEvent 子类。
 * 输入格式：{"type": "message.delta", "id": "...", "text": "..."}
 */
object GatewayEventSerializer : JsonContentPolymorphicSerializer<GatewayEvent>(GatewayEvent::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<GatewayEvent> {
        val eventType = element.jsonObject["type"]?.jsonPrimitive?.content
        return when (eventType) {
            HermesEventTypes.GATEWAY_READY -> GatewayEvent.GatewayReady.serializer()
            HermesEventTypes.SESSION_INFO -> GatewayEvent.SessionInfo.serializer()
            HermesEventTypes.MESSAGE_START -> GatewayEvent.MessageStart.serializer()
            HermesEventTypes.MESSAGE_DELTA -> GatewayEvent.MessageDelta.serializer()
            HermesEventTypes.MESSAGE_COMPLETE -> GatewayEvent.MessageComplete.serializer()
            HermesEventTypes.REASONING_DELTA -> GatewayEvent.ReasoningDelta.serializer()
            HermesEventTypes.THINKING_DELTA -> GatewayEvent.ThinkingDelta.serializer()
            HermesEventTypes.REASONING_AVAILABLE -> GatewayEvent.ReasoningAvailable.serializer()
            HermesEventTypes.TOOL_START -> GatewayEvent.ToolStart.serializer()
            HermesEventTypes.TOOL_PROGRESS -> GatewayEvent.ToolProgress.serializer()
            HermesEventTypes.TOOL_COMPLETE -> GatewayEvent.ToolComplete.serializer()
            HermesEventTypes.APPROVAL_REQUEST -> GatewayEvent.ApprovalRequest.serializer()
            HermesEventTypes.CLARIFY_REQUEST -> GatewayEvent.ClarifyRequest.serializer()
            HermesEventTypes.SECRET_REQUEST -> GatewayEvent.SecretRequest.serializer()
            HermesEventTypes.REVIEW_SUMMARY -> GatewayEvent.ReviewSummary.serializer()
            HermesEventTypes.ERROR -> GatewayEvent.ErrorEvent.serializer()
            else -> throw IllegalArgumentException("Unknown event type: $eventType")
        }
    }
}
