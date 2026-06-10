package com.seina.chan.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.seina.chan.data.remote.HermesApiService
import com.seina.chan.data.remote.HermesWsClient
import com.seina.chan.data.repository.ChatRepository
import com.seina.chan.data.repository.ConnectionRepository
import com.seina.chan.data.repository.SessionRepository
import com.seina.chan.data.repository.SettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.ANDROID
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Provides
    @Singleton
    fun provideHttpClient(json: Json): HttpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(json)
        }
        install(WebSockets)
        install(Logging) {
            logger = Logger.ANDROID
            level = LogLevel.ALL
        }
        engine {
            config {
                connectTimeout(30_000, TimeUnit.MILLISECONDS)
                readTimeout(30_000, TimeUnit.MILLISECONDS)
            }
        }
    }

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create {
            context.preferencesDataStoreFile("seina_chan_prefs")
        }
    }

    @Provides
    @Singleton
    fun provideHermesWsClient(client: HttpClient, json: Json): HermesWsClient {
        return HermesWsClient(client, json)
    }

    @Provides
    @Singleton
    fun provideHermesApiService(client: HttpClient): HermesApiService {
        return HermesApiService(client)
    }

    @Provides
    @Singleton
    fun provideConnectionRepository(
        wsClient: HermesWsClient,
        apiService: HermesApiService,
        dataStore: DataStore<Preferences>,
        client: HttpClient
    ): ConnectionRepository {
        return ConnectionRepository(wsClient, apiService, dataStore, client)
    }

    @Provides
    @Singleton
    fun provideSessionRepository(
        apiService: HermesApiService,
        wsClient: HermesWsClient
    ): SessionRepository {
        return SessionRepository(apiService, wsClient)
    }

    @Provides
    @Singleton
    fun provideChatRepository(
        wsClient: HermesWsClient
    ): ChatRepository {
        return ChatRepository(wsClient)
    }

    @Provides
    @Singleton
    fun provideSettingsRepository(
        dataStore: DataStore<Preferences>
    ): SettingsRepository {
        return SettingsRepository(dataStore)
    }
}
