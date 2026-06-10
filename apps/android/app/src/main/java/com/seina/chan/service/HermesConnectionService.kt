package com.seina.chan.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.seina.chan.MainActivity
import com.seina.chan.data.remote.ConnectionState
import com.seina.chan.data.remote.HermesWsClient
import com.seina.chan.util.FileLogger
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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

    // 前台服务状态标志
    private var isForeground = false

    // 熄屏后保持 CPU 运行的 WakeLock
    private var wakeLock: PowerManager.WakeLock? = null

    inner class LocalBinder : Binder() {
        fun getService(): HermesConnectionService = this@HermesConnectionService
    }

    override fun onCreate() {
        super.onCreate()
        FileLogger.i("HermesConnectionService", "onCreate")
        createNotificationChannel()
        startForegroundWithNotification()
        acquireWakeLock()
    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        FileLogger.i("HermesConnectionService", "onStartCommand action=${intent?.action}")
        when (intent?.action) {
            ACTION_START_FOREGROUND -> startForegroundWithNotification()
            ACTION_STOP_FOREGROUND -> stopForegroundNotification()
            else -> {
                // 系统重启服务或首次启动，默认恢复前台状态
                startForegroundWithNotification()
            }
        }
        wsClient.state.onEach { state ->
            updateNotification(state)
        }.launchIn(scope)
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Hermes 连接",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "保持与 Hermes 后端的 WebSocket 连接"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun startForegroundWithNotification() {
        if (isForeground) return
        val notification = buildNotification("连接中...")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        isForeground = true
    }

    /**
     * 移除前台服务状态及通知
     */
    private fun stopForegroundNotification() {
        if (!isForeground) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        isForeground = false
    }

    /**
     * 获取 WakeLock，防止熄屏后 CPU 休眠导致连接断开
     */
    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SeinaChan::WsWakeLock")
        wakeLock?.acquire()
        FileLogger.i("HermesConnectionService", "WakeLock 已获取")
    }

    private fun buildNotification(contentText: String): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("口袋星奈")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun updateNotification(state: ConnectionState) {
        // 仅在前台状态下更新通知，避免 stopForeground 后通知被重新弹出
        if (!isForeground) return
        val text = when (state) {
            is ConnectionState.Open -> "已连接"
            is ConnectionState.Connecting -> "连接中..."
            is ConnectionState.Closed -> "已断开"
            is ConnectionState.Error -> "连接错误"
            else -> "连接中..."
        }
        val notification = buildNotification(text)
        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        FileLogger.i("HermesConnectionService", "onDestroy")
        // 释放 WakeLock，避免内存泄漏
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
            FileLogger.i("HermesConnectionService", "WakeLock 已释放")
        }
    }

    companion object {
        private const val CHANNEL_ID = "hermes_connection"
        private const val NOTIFICATION_ID = 1

        const val ACTION_START_FOREGROUND = "START_FOREGROUND"
        const val ACTION_STOP_FOREGROUND = "STOP_FOREGROUND"
    }
}
