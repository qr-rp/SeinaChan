package com.seina.chan.data.repository

import com.seina.chan.data.local.dao.SentImageDao
import com.seina.chan.data.local.entity.SentImageEntity
import com.seina.chan.data.model.ChatMessage
import com.seina.chan.data.model.Session
import com.seina.chan.data.model.ToolCallDetail
import com.seina.chan.data.model.ToolCallStatus
import com.seina.chan.data.remote.HermesApiService
import com.seina.chan.data.remote.HermesMethods
import com.seina.chan.data.remote.HermesWsClient
import com.seina.chan.util.FileLogger
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
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
    private val wsClient: HermesWsClient,
    private val sentImageDao: SentImageDao
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
        val raw = apiService.getSessionMessages(sessionId).messages

        // 收集 tool 角色的结果（按 tool_call_id 索引）
        val toolResults = raw.filter { it.role == "tool" }.associate { dto ->
            val resultText = when (dto.content) {
                is JsonPrimitive -> dto.content.content
                else -> dto.content?.toString() ?: ""
            }
            (dto.toolCallId ?: dto.id.toString()) to resultText
        }

        // 解析消息（排除 tool 角色，其结果已提取到 toolResults）
        val parsed = raw.filter { it.role != "tool" }.flatMap { dto ->
            val content = when (dto.content) {
                is JsonPrimitive -> dto.content.content
                null -> ""
                else -> ""
            }
            val reasoningText = dto.reasoning ?: dto.reasoningContent ?: ""
            val (displayContent, imageUrls) = parseImageContent(content)

            // 解析 toolCalls 并尝试关联 tool 结果
            val toolCalls = parseToolCalls(dto.toolCalls).map { call ->
                val result = toolResults[call.id]
                if (result != null && call.result.isBlank()) {
                    call.copy(result = result)
                } else {
                    call
                }
            }

            if (imageUrls.size > 1 && dto.role == "user") {
                // 单条消息包含多张图片时拆分为多条，确保每条只带一张图
                imageUrls.mapIndexed { index, url ->
                    ChatMessage(
                        id = "${dto.id}_img_$index",
                        role = dto.role,
                        content = if (index == 0) displayContent else "",
                        isStreaming = false,
                        reasoningText = reasoningText,
                        isReasoning = false,
                        toolCalls = if (index == 0) toolCalls else emptyList(),
                        imageUrl = url
                    )
                }
            } else {
                listOf(
                    ChatMessage(
                        id = dto.id.toString(),
                        role = dto.role,
                        content = displayContent,
                        isStreaming = false,
                        reasoningText = reasoningText,
                        isReasoning = false,
                        toolCalls = toolCalls,
                        imageUrl = imageUrls.firstOrNull()
                    )
                )
            }
        }

        // 合并相邻的 assistant 消息（服务端可能将 reasoning/toolCall/content 拆成多条）
        val merged = mutableListOf<ChatMessage>()
        for (msg in parsed) {
            if (msg.role == "assistant" && merged.isNotEmpty() && merged.last().role == "assistant") {
                val last = merged.last()
                merged[merged.size - 1] = last.copy(
                    content = if (msg.content.isNotBlank()) msg.content else last.content,
                    reasoningText = when {
                        last.reasoningText.isNotBlank() && msg.reasoningText.isNotBlank() ->
                            last.reasoningText + "\n\n" + msg.reasoningText
                        msg.reasoningText.isNotBlank() -> msg.reasoningText
                        else -> last.reasoningText
                    },
                    toolCalls = last.toolCalls + msg.toolCalls
                )
            } else {
                merged.add(msg)
            }
        }

        return merged
    }

    private fun parseToolCalls(toolCallsElement: JsonElement?): List<ToolCallDetail> {
        if (toolCallsElement == null) return emptyList()
        return when (toolCallsElement) {
            is JsonArray -> toolCallsElement.mapNotNull { parseSingleToolCall(it) }
            is JsonObject -> listOfNotNull(parseSingleToolCall(toolCallsElement))
            else -> emptyList()
        }
    }

    private fun parseSingleToolCall(element: JsonElement): ToolCallDetail? {
        return try {
            val obj = element.jsonObject
            val id = obj["id"]?.jsonPrimitive?.content ?: ""

            // OpenAI 格式：function.name / function.arguments
            val function = obj["function"]?.jsonObject
            val name = function?.get("name")?.jsonPrimitive?.content
                ?: obj["toolName"]?.jsonPrimitive?.content
                ?: obj["name"]?.jsonPrimitive?.content ?: ""

            val args = function?.get("arguments")?.jsonPrimitive?.content
                ?: obj["input"]?.jsonPrimitive?.content
                ?: obj["args"]?.jsonPrimitive?.content ?: ""

            val result = obj["output"]?.jsonPrimitive?.content
                ?: obj["result"]?.jsonPrimitive?.content ?: ""
            val statusStr = obj["status"]?.jsonPrimitive?.content ?: ""
            val status = when (statusStr.lowercase()) {
                "success" -> ToolCallStatus.Success
                "failed", "error" -> ToolCallStatus.Failed
                else -> ToolCallStatus.Success
            }
            ToolCallDetail(id = id, name = name, args = args, result = result, status = status)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 解析消息内容中的图片残留文字，尝试从本地 Room 缓存查询对应的 content:// URI。
     * 服务端实际存储格式如：
     *   [You can examine it with vision_analyze using image_url: /path]
     *   [You can examine it with vision_analyze using image_urls: ["/path"]]
     *   [User sent an image at: /path]
     *   任何包含 .hermes/images/ 路径的文本
     * 有缓存则返回空内容 + imageUrl；无缓存则替换为 📷 图片 占位符。
     * 支持单条消息中包含多张图片，返回所有匹配的本地 URI 列表。
     */
    private suspend fun parseImageContent(content: String): Pair<String, List<String>> {
        if (content.isBlank()) return Pair(content, emptyList())

        val regex = Regex("""\[[^\]]*\.hermes/images/[^\]\s"]+[^\]]*\]|(?:image_url[s]?:\s*\[?"?|sent an image at:\s*)(/[^\]\s"]*\.hermes/images/[^\]\s"]+)""")
        val pathRegex = Regex("""(/[^\]\s"]*\.hermes/images/[^\]\s"]+)""")

        val localUris = mutableListOf<String>()
        var cleanedContent = content

        regex.findAll(content).forEach { match ->
            val serverPath = match.groupValues.getOrNull(1)?.ifBlank {
                pathRegex.find(match.value)?.groupValues?.get(1)
            }?.trim() ?: ""

            if (serverPath.isNotEmpty()) {
                val normalizedPath = serverPath.substring(serverPath.lastIndexOf(".hermes/images/"))
                val localUri = sentImageDao.getUriByServerPath(normalizedPath)
                if (localUri != null) {
                    localUris.add(localUri)
                    cleanedContent = cleanedContent.replace(match.value, "")
                } else {
                    cleanedContent = cleanedContent.replace(match.value, "📷 图片")
                }
            } else {
                val fallbackPath = pathRegex.find(match.value)?.groupValues?.get(1) ?: ""
                if (fallbackPath.isNotEmpty()) {
                    val normalizedPath = fallbackPath.substring(fallbackPath.lastIndexOf(".hermes/images/"))
                    val localUri = sentImageDao.getUriByServerPath(normalizedPath)
                    if (localUri != null) {
                        localUris.add(localUri)
                        cleanedContent = cleanedContent.replace(match.value, "")
                    } else {
                        cleanedContent = cleanedContent.replace(match.value, "📷 图片")
                    }
                }
            }
        }

        return Pair(cleanedContent.trim(), localUris)
    }

    suspend fun createSession(): CreateSessionResult {
        val result = wsClient.request(HermesMethods.SESSION_CREATE)
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
        val result = wsClient.request(HermesMethods.SESSION_RESUME, params)
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
