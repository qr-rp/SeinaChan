package com.seina.chan.data.remote

import com.seina.chan.util.FileLogger
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.pow

sealed class ConnectionState {
    data object Idle : ConnectionState()
    data object Connecting : ConnectionState()
    data object Open : ConnectionState()
    data object Closed : ConnectionState()
    data class Error(val reason: String) : ConnectionState()
}

class HermesWsClient(
    private val client: HttpClient,
    private val json: Json = Json { ignoreUnknownKeys = true }
) {
    private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
    val state: StateFlow<ConnectionState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<GatewayEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<GatewayEvent> = _events.asSharedFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val pendingRequests = ConcurrentHashMap<Int, CompletableDeferred<JsonElement>>()

    private val reqId = AtomicInteger(0)

    private var session: WebSocketSession? = null

    private var shouldReconnect = false
    private var reconnectAttempts = 0
    private var reconnectJob: Job? = null
    private var lastUrl: String? = null
    private var lastToken: String? = null

    init {
        scope.launch {
            state.collect { s ->
                if ((s is ConnectionState.Closed || s is ConnectionState.Error) && shouldReconnect) {
                    scheduleReconnect()
                }
            }
        }
    }

    fun enableReconnect(enabled: Boolean) {
        shouldReconnect = enabled
        if (enabled) {
            reconnectAttempts = 0
            if (_state.value is ConnectionState.Closed || _state.value is ConnectionState.Error) {
                scheduleReconnect()
            }
        } else {
            reconnectJob?.cancel()
            reconnectJob = null
        }
    }

    suspend fun connect(url: String, token: String): Boolean {
        FileLogger.i("HermesWsClient", "connect() called, url=$url, tokenPrefix=${token.take(4)}")
        if (_state.value == ConnectionState.Open || _state.value == ConnectionState.Connecting) {
            FileLogger.i("HermesWsClient", "Already connected or connecting, state=${_state.value}")
            return true
        }
        lastUrl = url
        lastToken = token
        return doConnect(url, token)
    }

    private suspend fun doConnect(url: String, token: String): Boolean {
        _state.value = ConnectionState.Connecting
        FileLogger.i("HermesWsClient", "doConnect() starting, url=$url")
        return try {
            val wsUrl = if (url.contains("?")) "$url&token=$token" else "$url?token=$token"
            val newSession = client.webSocketSession(wsUrl)
            session = newSession
            _state.value = ConnectionState.Open
            reconnectAttempts = 0
            FileLogger.i("HermesWsClient", "WebSocket handshake succeeded")

            scope.launch {
                try {
                    for (frame in newSession.incoming) {
                        if (frame is Frame.Text) {
                            val text = frame.readText()
                            FileLogger.d("HermesWsClient", "Frame received: ${text.take(500)}")
                            handleFrame(text)
                        }
                    }
                } catch (e: Exception) {
                    FileLogger.e("HermesWsClient", "incoming loop exception", e)
                } finally {
                    FileLogger.w("HermesWsClient", "incoming loop ended, state=${_state.value}")
                    if (_state.value == ConnectionState.Open) {
                        _state.value = ConnectionState.Closed
                    }
                    clearPending("Connection closed")
                }
            }
            true
        } catch (e: Exception) {
            FileLogger.e("HermesWsClient", "doConnect() failed", e)
            _state.value = ConnectionState.Error(e.message ?: "Unknown error")
            clearPending(e.message ?: "Connection error")
            false
        }
    }

    private fun scheduleReconnect() {
        if (reconnectJob?.isActive == true) return
        if (reconnectAttempts >= 5) {
            shouldReconnect = false
            _state.value = ConnectionState.Error("重连失败，已达到最大尝试次数")
            return
        }
        reconnectJob = scope.launch {
            val delayMs = (1000 * 2.0.pow(reconnectAttempts.toDouble())).toLong().coerceAtMost(30000)
            delay(delayMs)
            reconnectAttempts++
            val url = lastUrl ?: return@launch
            val token = lastToken ?: return@launch
            doConnect(url, token)
        }
    }

    suspend fun request(method: String, params: JsonObject? = null): JsonElement {
        val id = reqId.incrementAndGet()
        FileLogger.d("HermesWsClient", "sendRequest method=$method, id=$id")
        val request = JsonRpcRequest(id = id, method = method, params = params)
        val deferred = CompletableDeferred<JsonElement>()
        pendingRequests[id] = deferred

        val text = json.encodeToString(JsonRpcRequest.serializer(), request)
        val s = session ?: throw IllegalStateException("WebSocket not connected")
        s.send(Frame.Text(text))

        return try {
            withTimeout(120_000) {
                deferred.await()
            }
        } catch (e: Exception) {
            FileLogger.e("HermesWsClient", "request timeout/exception for method=$method, id=$id", e)
            pendingRequests.remove(id)
            throw e
        }
    }

    fun disconnect() {
        FileLogger.i("HermesWsClient", "disconnect() called")
        shouldReconnect = false
        reconnectJob?.cancel()
        reconnectJob = null
        scope.launch {
            session?.close()
            session = null
            _state.value = ConnectionState.Closed
            clearPending("Disconnected")
        }
    }

    private fun clearPending(reason: String) {
        val entries = pendingRequests.toMap()
        pendingRequests.clear()
        entries.values.forEach { it.completeExceptionally(Exception(reason)) }
    }

    private fun handleFrame(text: String) {
        try {
            val element = json.parseToJsonElement(text)
            val obj = element.jsonObject

            // JSON-RPC 响应（有 id 和 result/error）
            if (obj.containsKey("id") && (obj.containsKey("result") || obj.containsKey("error"))) {
                try {
                    val response = json.decodeFromJsonElement(JsonRpcResponse.serializer(), element)
                    val deferred = pendingRequests.remove(response.id)
                    if (deferred != null) {
                        if (response.error != null) {
                            FileLogger.w("HermesWsClient", "JSON-RPC error id=${response.id}: ${response.error.code} ${response.error.message}")
                            deferred.completeExceptionally(
                                Exception("JSON-RPC error ${response.error.code}: ${response.error.message}")
                            )
                        } else {
                            deferred.complete(response.result ?: JsonObject(emptyMap()))
                        }
                    }
                } catch (e: Exception) {
                    FileLogger.e("HermesWsClient", "Failed to parse JSON-RPC response", e)
                }
                return
            }

            // 事件通知（method == "event"）
            val method = obj["method"]?.jsonPrimitive?.content
            if (method == "event") {
                val params = obj["params"]?.jsonObject ?: return
                val eventType = params["type"]?.jsonPrimitive?.content ?: return
                val sessionId = params["session_id"]?.jsonPrimitive?.content
                val payload = params["payload"]?.jsonObject
                val event = parseEvent(eventType, sessionId, payload)
                if (event != null) {
                    FileLogger.d("HermesWsClient", "Event parsed: ${event::class.simpleName}")
                    _events.tryEmit(event)
                } else {
                    FileLogger.w("HermesWsClient", "Unknown event type: $eventType")
                }
            }
        } catch (e: Exception) {
            FileLogger.e("HermesWsClient", "Failed to handle frame", e)
        }
    }

    private fun parseEvent(eventType: String, sessionId: String?, payload: JsonObject?): GatewayEvent? {
        return when (eventType) {
            "gateway.ready" -> GatewayEvent.GatewayReady
            "session.info" -> GatewayEvent.SessionInfo(
                id = payload?.get("id")?.jsonPrimitive?.content ?: "",
                title = payload?.get("title")?.jsonPrimitive?.content
            )
            "message.start" -> GatewayEvent.MessageStart(
                id = payload?.get("id")?.jsonPrimitive?.content ?: "",
                parentId = payload?.get("parent_id")?.jsonPrimitive?.content,
                role = payload?.get("role")?.jsonPrimitive?.content ?: "assistant"
            )
            "message.delta" -> GatewayEvent.MessageDelta(
                id = payload?.get("id")?.jsonPrimitive?.content ?: "",
                delta = payload?.get("text")?.jsonPrimitive?.content ?: ""
            )
            "message.complete" -> {
                val reasoning = payload?.get("reasoning")?.jsonPrimitive?.content ?: ""
                GatewayEvent.MessageComplete(
                    id = payload?.get("id")?.jsonPrimitive?.content ?: "",
                    reasoning = reasoning
                )
            }
            "reasoning.delta" -> GatewayEvent.ReasoningDelta(
                text = payload?.get("text")?.jsonPrimitive?.content ?: ""
            )
            "thinking.delta" -> GatewayEvent.ThinkingDelta(
                text = payload?.get("text")?.jsonPrimitive?.content ?: ""
            )
            "reasoning.available" -> GatewayEvent.ReasoningAvailable(
                text = payload?.get("text")?.jsonPrimitive?.content ?: ""
            )
            "tool.start" -> {
                val argsElement = payload?.get("args")
                val argsString = when {
                    argsElement == null -> ""
                    argsElement is JsonPrimitive -> argsElement.jsonPrimitive.content
                    else -> argsElement.toString()
                }
                GatewayEvent.ToolStart(
                    toolId = payload?.get("tool_id")?.jsonPrimitive?.content ?: "",
                    name = payload?.get("name")?.jsonPrimitive?.content ?: "",
                    args = argsString
                )
            }
            "tool.progress" -> GatewayEvent.ToolProgress(
                toolId = payload?.get("tool_id")?.jsonPrimitive?.content ?: "",
                text = payload?.get("text")?.jsonPrimitive?.content ?: ""
            )
            "tool.complete" -> GatewayEvent.ToolComplete(
                toolId = payload?.get("tool_id")?.jsonPrimitive?.content ?: "",
                name = payload?.get("name")?.jsonPrimitive?.content ?: "",
                result = payload?.get("result")?.jsonPrimitive?.content ?: "",
                duration = payload?.get("duration")?.jsonPrimitive?.content?.toFloatOrNull(),
                summary = payload?.get("summary")?.jsonPrimitive?.content ?: ""
            )
            "approval.request" -> GatewayEvent.ApprovalRequest(
                id = payload?.get("id")?.jsonPrimitive?.content ?: "",
                toolName = payload?.get("tool_name")?.jsonPrimitive?.content ?: "",
                input = emptyMap()
            )
            "clarify.request" -> GatewayEvent.ClarifyRequest(
                id = payload?.get("id")?.jsonPrimitive?.content ?: "",
                question = payload?.get("question")?.jsonPrimitive?.content ?: ""
            )
            "secret.request" -> GatewayEvent.SecretRequest(
                id = payload?.get("id")?.jsonPrimitive?.content ?: "",
                prompt = payload?.get("prompt")?.jsonPrimitive?.content ?: ""
            )
            "error" -> GatewayEvent.ErrorEvent(
                message = payload?.get("message")?.jsonPrimitive?.content ?: ""
            )
            else -> null
        }
    }
}
