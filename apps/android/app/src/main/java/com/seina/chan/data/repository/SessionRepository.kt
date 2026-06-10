package com.seina.chan.data.repository

import com.seina.chan.data.model.ChatMessage
import com.seina.chan.data.model.Session
import com.seina.chan.data.remote.HermesApiService
import com.seina.chan.data.remote.HermesWsClient
import com.seina.chan.util.FileLogger
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

data class CreateSessionResult(
    val sid: String,
    val storedSessionId: String
)

/**
 * 会话分页结果
 */
data class SessionsPageResult(
    val sessions: List<Session>,
    val total: Int,
    val hasMore: Boolean
)

class SessionRepository(
    private val apiService: HermesApiService,
    private val wsClient: HermesWsClient
) {
    suspend fun fetchSessions(limit: Int = 20, offset: Int = 0): SessionsPageResult {
        val response = apiService.getSessions(limit = limit, offset = offset)
        val sessions = response.sessions.map {
            Session(
                id = it.id,
                title = it.title,
                preview = it.preview,
                messageCount = it.messageCount,
                lastActiveAt = it.lastActiveAt?.toString()
            )
        }
        // 根据返回的数据判断是否还有更多：当本次返回数量达到 limit 时认为可能还有更多
        val hasMore = sessions.size >= limit
        return SessionsPageResult(
            sessions = sessions,
            total = response.total,
            hasMore = hasMore
        )
    }

    suspend fun fetchMessages(sessionId: String): List<ChatMessage> {
        return apiService.getSessionMessages(sessionId).messages.map {
            val content = when (it.content) {
                is JsonPrimitive -> it.content.content
                null -> ""
                else -> it.content.toString()
            }
            ChatMessage(
                id = it.id.toString(),
                role = it.role,
                content = content,
                isStreaming = false,
                toolCalls = emptyList()
            )
        }
    }

    suspend fun createSession(): CreateSessionResult {
        val result = wsClient.request("session.create")
        val sid = when {
            result is JsonObject && result.containsKey("session_id") -> result["session_id"]!!.jsonPrimitive.content
            result is JsonObject && result.containsKey("id") -> result["id"]!!.jsonPrimitive.content
            result is JsonObject && result.containsKey("sessionId") -> result["sessionId"]!!.jsonPrimitive.content
            else -> result.toString()
        }
        val storedSessionId = if (result is JsonObject && result.containsKey("stored_session_id")) {
            result["stored_session_id"]!!.jsonPrimitive.content
        } else {
            sid
        }
        return CreateSessionResult(sid = sid, storedSessionId = storedSessionId)
    }

    suspend fun resumeSession(sessionId: String): String {
        val params = buildJsonObject {
            put("session_id", sessionId)
        }
        val result = wsClient.request("session.resume", params)
        return when {
            result is JsonObject && result.containsKey("session_id") -> result["session_id"]!!.jsonPrimitive.content
            else -> result.toString()
        }
    }

    suspend fun deleteSession(sessionId: String) {
        FileLogger.i("SessionRepository", "deleteSession() sessionId=$sessionId")
        val success = apiService.deleteSession(sessionId)
        if (!success) {
            FileLogger.e("SessionRepository", "deleteSession() failed")
            throw RuntimeException("Failed to delete session")
        }
        FileLogger.i("SessionRepository", "deleteSession() succeeded")
    }

    suspend fun renameSession(sessionId: String, title: String) {
        FileLogger.i("SessionRepository", "renameSession() sessionId=$sessionId, title=$title")
        apiService.renameSession(sessionId, title)
        FileLogger.i("SessionRepository", "renameSession() succeeded")
    }
}
