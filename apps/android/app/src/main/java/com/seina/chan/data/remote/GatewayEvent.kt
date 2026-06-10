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
    @SerialName("gateway.ready")
    data object GatewayReady : GatewayEvent()

    @Serializable
    @SerialName("session.info")
    data class SessionInfo(
        val id: String,
        val title: String? = null
    ) : GatewayEvent()

    @Serializable
    @SerialName("message.start")
    data class MessageStart(
        val id: String,
        val parentId: String? = null,
        val role: String
    ) : GatewayEvent()

    @Serializable
    @SerialName("message.delta")
    data class MessageDelta(
        val id: String,
        val delta: String
    ) : GatewayEvent()

    @Serializable
    @SerialName("message.complete")
    data class MessageComplete(
        val id: String
    ) : GatewayEvent()

    @Serializable
    @SerialName("thinking.delta")
    data class ThinkingDelta(
        val id: String,
        val delta: String
    ) : GatewayEvent()

    @Serializable
    @SerialName("tool.start")
    data class ToolStart(
        val id: String,
        val toolName: String,
        val input: Map<String, String> = emptyMap()
    ) : GatewayEvent()

    @Serializable
    @SerialName("tool.progress")
    data class ToolProgress(
        val id: String,
        val content: String
    ) : GatewayEvent()

    @Serializable
    @SerialName("tool.complete")
    data class ToolComplete(
        val id: String,
        val output: String? = null
    ) : GatewayEvent()

    @Serializable
    @SerialName("approval.request")
    data class ApprovalRequest(
        val id: String,
        val toolName: String,
        val input: Map<String, String> = emptyMap()
    ) : GatewayEvent()

    @Serializable
    @SerialName("clarify.request")
    data class ClarifyRequest(
        val id: String,
        val question: String
    ) : GatewayEvent()

    @Serializable
    @SerialName("secret.request")
    data class SecretRequest(
        val id: String,
        val prompt: String
    ) : GatewayEvent()

    @Serializable
    @SerialName("error")
    data class ErrorEvent(
        val message: String
    ) : GatewayEvent()
}

object GatewayEventSerializer : JsonContentPolymorphicSerializer<GatewayEvent>(GatewayEvent::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<GatewayEvent> {
        val event = element.jsonObject["event"]?.jsonPrimitive?.content
        return when (event) {
            "gateway.ready" -> GatewayEvent.GatewayReady.serializer()
            "session.info" -> GatewayEvent.SessionInfo.serializer()
            "message.start" -> GatewayEvent.MessageStart.serializer()
            "message.delta" -> GatewayEvent.MessageDelta.serializer()
            "message.complete" -> GatewayEvent.MessageComplete.serializer()
            "thinking.delta" -> GatewayEvent.ThinkingDelta.serializer()
            "tool.start" -> GatewayEvent.ToolStart.serializer()
            "tool.progress" -> GatewayEvent.ToolProgress.serializer()
            "tool.complete" -> GatewayEvent.ToolComplete.serializer()
            "approval.request" -> GatewayEvent.ApprovalRequest.serializer()
            "clarify.request" -> GatewayEvent.ClarifyRequest.serializer()
            "secret.request" -> GatewayEvent.SecretRequest.serializer()
            "error" -> GatewayEvent.ErrorEvent.serializer()
            else -> throw IllegalArgumentException("Unknown event type: $event")
        }
    }
}
