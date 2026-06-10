package com.seina.chan.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.seina.chan.data.model.ConnectionConfig
import com.seina.chan.data.remote.ConnectionState
import com.seina.chan.data.remote.HermesApiService
import com.seina.chan.data.remote.HermesWsClient
import com.seina.chan.util.FileLogger
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first

class ConnectionRepository(
    private val wsClient: HermesWsClient,
    private val apiService: HermesApiService,
    private val dataStore: DataStore<Preferences>,
    private val client: HttpClient
) {
    val connectionState: StateFlow<ConnectionState> = wsClient.state

    suspend fun connect(config: ConnectionConfig): Result<Unit> {
        val wsUrl = "ws://${config.ip}:${config.port}/api/ws"
        val httpBaseUrl = "http://${config.ip}:${config.port}"
        FileLogger.i("ConnectionRepository", "connect() httpBaseUrl=$httpBaseUrl, wsUrl=$wsUrl")
        return try {
            apiService.setConfig(httpBaseUrl, config.token)
            val connected = wsClient.connect(wsUrl, config.token)
            if (connected) {
                wsClient.enableReconnect(true)
                FileLogger.i("ConnectionRepository", "connect() succeeded")
                Result.success(Unit)
            } else {
                FileLogger.e("ConnectionRepository", "connect() failed: WebSocket returned false")
                Result.failure(Exception("WebSocket 连接失败"))
            }
        } catch (e: Exception) {
            FileLogger.e("ConnectionRepository", "connect() exception", e)
            Result.failure(e)
        }
    }

    suspend fun testConnection(ip: String, port: String): Result<String> {
        val url = "http://$ip:$port/api/status"
        FileLogger.i("ConnectionRepository", "testConnection() url=$url")
        return try {
            val response = client.get(url)
            if (response.status.value in 200..299) {
                FileLogger.i("ConnectionRepository", "testConnection() succeeded")
                Result.success("连接成功")
            } else {
                FileLogger.e("ConnectionRepository", "testConnection() failed: HTTP ${response.status.value}")
                Result.failure(Exception("HTTP ${response.status.value}"))
            }
        } catch (e: Exception) {
            FileLogger.e("ConnectionRepository", "testConnection() exception", e)
            Result.failure(e)
        }
    }

    suspend fun disconnect() {
        FileLogger.i("ConnectionRepository", "disconnect() called")
        wsClient.enableReconnect(false)
        wsClient.disconnect()
    }

    suspend fun saveConfig(config: ConnectionConfig) {
        FileLogger.i("ConnectionRepository", "saveConfig() ip=${config.ip}, port=${config.port}")
        dataStore.edit { prefs ->
            prefs[IP_KEY] = config.ip
            prefs[PORT_KEY] = config.port
            prefs[TOKEN_KEY] = config.token
        }
    }

    suspend fun loadConfig(): ConnectionConfig? {
        FileLogger.i("ConnectionRepository", "loadConfig() called")
        val prefs = dataStore.data.first()
        val ip = prefs[IP_KEY]
        val port = prefs[PORT_KEY]
        val token = prefs[TOKEN_KEY] ?: ""
        return if (!ip.isNullOrBlank() && !port.isNullOrBlank()) {
            FileLogger.i("ConnectionRepository", "loadConfig() found new config")
            ConnectionConfig(ip, port, token)
        } else {
            val baseUrl = prefs[BASE_URL_KEY]
            if (!baseUrl.isNullOrBlank()) {
                FileLogger.i("ConnectionRepository", "loadConfig() found legacy baseUrl=$baseUrl, parsing...")
                val parsed = parseLegacyBaseUrl(baseUrl)
                if (parsed != null) {
                    ConnectionConfig(parsed.first, parsed.second, token)
                } else {
                    FileLogger.w("ConnectionRepository", "loadConfig() failed to parse legacy baseUrl")
                    null
                }
            } else {
                FileLogger.i("ConnectionRepository", "loadConfig() no config found")
                null
            }
        }
    }

    suspend fun clearConfig() {
        FileLogger.i("ConnectionRepository", "clearConfig() called")
        dataStore.edit { prefs ->
            prefs.remove(IP_KEY)
            prefs.remove(PORT_KEY)
            prefs.remove(TOKEN_KEY)
            prefs.remove(BASE_URL_KEY)
        }
    }

    private fun parseLegacyBaseUrl(baseUrl: String): Pair<String, String>? {
        val cleaned = baseUrl
            .removePrefix("ws://")
            .removePrefix("wss://")
            .removePrefix("http://")
            .removePrefix("https://")
            .removeSuffix("/api/ws")
            .removeSuffix("/")
        val parts = cleaned.split(":")
        return if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
            parts[0] to parts[1]
        } else null
    }

    suspend fun saveLastDbSessionId(sessionId: String) {
        dataStore.edit { prefs ->
            prefs[LAST_DB_SESSION_ID_KEY] = sessionId
        }
    }

    suspend fun loadLastDbSessionId(): String? {
        val prefs = dataStore.data.first()
        return prefs[LAST_DB_SESSION_ID_KEY]
    }

    suspend fun clearLastDbSessionId() {
        dataStore.edit { prefs ->
            prefs.remove(LAST_DB_SESSION_ID_KEY)
        }
    }

    companion object {
        private val IP_KEY = stringPreferencesKey("ip")
        private val PORT_KEY = stringPreferencesKey("port")
        private val TOKEN_KEY = stringPreferencesKey("token")
        private val BASE_URL_KEY = stringPreferencesKey("base_url")
        private val LAST_DB_SESSION_ID_KEY = stringPreferencesKey("last_db_session_id")
    }
}
