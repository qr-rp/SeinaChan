package com.seina.chan.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepository(
    private val dataStore: DataStore<Preferences>
) {

    val pageSize: Flow<Int> = dataStore.data.map { it[PAGE_SIZE_KEY] ?: 20 }
    val showToolCalls: Flow<Boolean> = dataStore.data.map { it[SHOW_TOOL_CALLS_KEY] ?: true }
    val showReasoning: Flow<Boolean> = dataStore.data.map { it[SHOW_REASONING_KEY] ?: true }
    val themeMode: Flow<String> = dataStore.data.map { it[THEME_MODE_KEY] ?: "system" }
    val showTimestamps: Flow<Boolean> = dataStore.data.map { it[SHOW_TIMESTAMPS_KEY] ?: false }
    val autoExpandReasoning: Flow<Boolean> = dataStore.data.map { it[AUTO_EXPAND_REASONING_KEY] ?: false }
    val autoExpandTools: Flow<Boolean> = dataStore.data.map { it[AUTO_EXPAND_TOOLS_KEY] ?: false }
    val connectionIp: Flow<String> = dataStore.data.map { it[CONNECTION_IP_KEY] ?: "" }
    val connectionPort: Flow<String> = dataStore.data.map { it[CONNECTION_PORT_KEY] ?: "" }
    val connectionToken: Flow<String> = dataStore.data.map { it[CONNECTION_TOKEN_KEY] ?: "" }
    val hiddenToolNames: Flow<Set<String>> = dataStore.data.map { it[HIDDEN_TOOL_NAMES_KEY] ?: emptySet() }
    /** 自定义工具链，格式为 "category|tool_name" 的 Set */
    val customTools: Flow<Set<String>> = dataStore.data.map { it[CUSTOM_TOOLS_KEY] ?: emptySet() }

    suspend fun setPageSize(value: Int) {
        dataStore.edit { prefs ->
            prefs[PAGE_SIZE_KEY] = value
        }
    }

    suspend fun setShowToolCalls(value: Boolean) {
        dataStore.edit { prefs ->
            prefs[SHOW_TOOL_CALLS_KEY] = value
        }
    }

    suspend fun setShowReasoning(value: Boolean) {
        dataStore.edit { prefs ->
            prefs[SHOW_REASONING_KEY] = value
        }
    }

    suspend fun setThemeMode(value: String) {
        dataStore.edit { prefs ->
            prefs[THEME_MODE_KEY] = value
        }
    }

    suspend fun setShowTimestamps(value: Boolean) {
        dataStore.edit { prefs ->
            prefs[SHOW_TIMESTAMPS_KEY] = value
        }
    }

    suspend fun setAutoExpandReasoning(value: Boolean) {
        dataStore.edit { prefs ->
            prefs[AUTO_EXPAND_REASONING_KEY] = value
        }
    }

    suspend fun setAutoExpandTools(value: Boolean) {
        dataStore.edit { prefs ->
            prefs[AUTO_EXPAND_TOOLS_KEY] = value
        }
    }

    suspend fun setConnectionIp(value: String) {
        dataStore.edit { prefs ->
            prefs[CONNECTION_IP_KEY] = value
        }
    }

    suspend fun setConnectionPort(value: String) {
        dataStore.edit { prefs ->
            prefs[CONNECTION_PORT_KEY] = value
        }
    }

    suspend fun setConnectionToken(value: String) {
        dataStore.edit { prefs ->
            prefs[CONNECTION_TOKEN_KEY] = value
        }
    }

    suspend fun setHiddenToolNames(value: Set<String>) {
        dataStore.edit { prefs ->
            prefs[HIDDEN_TOOL_NAMES_KEY] = value
        }
    }

    suspend fun setCustomTools(value: Set<String>) {
        dataStore.edit { prefs ->
            prefs[CUSTOM_TOOLS_KEY] = value
        }
    }

    companion object {
        private val PAGE_SIZE_KEY = intPreferencesKey("page_size")
        private val SHOW_TOOL_CALLS_KEY = booleanPreferencesKey("show_tool_calls")
        private val SHOW_REASONING_KEY = booleanPreferencesKey("show_reasoning")
        private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
        private val SHOW_TIMESTAMPS_KEY = booleanPreferencesKey("show_timestamps")
        private val AUTO_EXPAND_REASONING_KEY = booleanPreferencesKey("auto_expand_reasoning")
        private val AUTO_EXPAND_TOOLS_KEY = booleanPreferencesKey("auto_expand_tools")
        private val CONNECTION_IP_KEY = stringPreferencesKey("ip")
        private val CONNECTION_PORT_KEY = stringPreferencesKey("port")
        private val CONNECTION_TOKEN_KEY = stringPreferencesKey("token")
        private val HIDDEN_TOOL_NAMES_KEY = stringSetPreferencesKey("hidden_tool_names")
        private val CUSTOM_TOOLS_KEY = stringSetPreferencesKey("custom_tools")
    }
}
