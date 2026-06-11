package com.seina.chan.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.seina.chan.MainActivity
import com.seina.chan.data.remote.ConnectionState
import com.seina.chan.data.remote.GatewayEvent
import com.seina.chan.data.remote.HermesWsClient
import com.seina.chan.util.FileLogger
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@AndroidEntryPoint
class HermesConnectionService : Service() {

    @Inject
    lateinit var wsClient: HermesWsClient

    private val binder = LocalBinder()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // 应用是否在前台
    private var isAppInForeground = false

    // 协程 Job 引用，用于防止重复启动
    private var stateCollectJob: Job? = null
    private var eventsCollectJob: Job? = null

    inner class LocalBinder : Binder() {
        fun getService(): HermesConnectionService = this@HermesConnectionService
    }

    override fun onCreate() {
        super.onCreate()
        FileLogger.i("HermesConnectionService", "onCreate")
        createNotificationChannels()
        startForeground(NOTIFICATION_ID_KEEPALIVE, buildKeepAliveNotification())
        startCollectors()
    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        FileLogger.i("HermesConnectionService", "onStartCommand action=${intent?.action}")
        when (intent?.action) {
            ACTION_APP_FOREGROUND -> {
                isAppInForeground = true
                FileLogger.i("HermesConnectionService", "应用回到前台")
            }
            ACTION_APP_BACKGROUND -> {
                isAppInForeground = false
                FileLogger.i("HermesConnectionService", "应用进入后台")
                wsClient.enableReconnect(true)
            }
            ACTION_ENSURE_CONNECTION -> {
                ensureConnection()
            }
        }
        return START_STICKY
    }

    private fun ensureConnection() {
        val currentState = wsClient.state.value
        if (currentState is ConnectionState.Closed || currentState is ConnectionState.Error) {
            FileLogger.i("HermesConnectionService", "连接已断开，触发立即重连")
            wsClient.reconnectImmediately()
        } else {
            FileLogger.i("HermesConnectionService", "连接状态正常，无需重连: $currentState")
        }
    }

    private fun startCollectors() {
        // 启动连接状态收集（防止重复）
        if (stateCollectJob?.isActive != true) {
            stateCollectJob = wsClient.state.onEach { state ->
                FileLogger.i("HermesConnectionService", "WebSocket 状态: $state")
            }.launchIn(scope)
        }
        // 启动事件收集，用于后台消息通知（防止重复）
        if (eventsCollectJob?.isActive != true) {
            eventsCollectJob = wsClient.events.onEach { event ->
                if (event is GatewayEvent.MessageStart && !isAppInForeground) {
                    showNewMessageNotification()
                }
            }.launchIn(scope)
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java) ?: return

            val keepAliveChannel = NotificationChannel(
                CHANNEL_ID_KEEPALIVE,
                "连接保持",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "保持 WebSocket 连接 alive"
            }
            manager.createNotificationChannel(keepAliveChannel)

            val aiChannel = NotificationChannel(
                CHANNEL_ID_AI_MESSAGES,
                "AI 消息提醒",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "当 AI 助手返回新消息时提醒"
            }
            manager.createNotificationChannel(aiChannel)
        }
    }

    private fun showNewMessageNotification() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID_AI_MESSAGES)
            .setContentTitle("口袋星奈")
            .setContentText("收到新的回复")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(NOTIFICATION_ID_MESSAGE, notification)
    }

    private fun buildKeepAliveNotification(): android.app.Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID_KEEPALIVE)
            .setContentTitle("口袋星奈")
            .setContentText("连接保持中...")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        FileLogger.i("HermesConnectionService", "onDestroy")
        stateCollectJob?.cancel()
        eventsCollectJob?.cancel()
    }

    companion object {
        private const val CHANNEL_ID_KEEPALIVE = "keepalive"
        private const val NOTIFICATION_ID_KEEPALIVE = 1
        private const val CHANNEL_ID_AI_MESSAGES = "ai_messages"
        private const val NOTIFICATION_ID_MESSAGE = 2

        const val ACTION_APP_FOREGROUND = "APP_FOREGROUND"
        const val ACTION_APP_BACKGROUND = "APP_BACKGROUND"
        const val ACTION_ENSURE_CONNECTION = "ENSURE_CONNECTION"
    }
}
