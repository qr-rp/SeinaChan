package com.seina.chan

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.seina.chan.data.repository.SettingsRepository
import com.seina.chan.service.HermesConnectionService
import com.seina.chan.ui.components.SeinaSnackbarHost
import com.seina.chan.ui.navigation.SeinaNavHost
import com.seina.chan.ui.theme.SeinaChanTheme
import com.seina.chan.util.FileLogger
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var settingsRepository: SettingsRepository

    private var connectionService: HermesConnectionService? = null
    private var serviceBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as HermesConnectionService.LocalBinder
            connectionService = binder.getService()
            serviceBound = true
            FileLogger.i("MainActivity", "Service connected")
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            connectionService = null
            serviceBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindHermesService()
        setContent {
            val themeMode by settingsRepository.themeMode.collectAsStateWithLifecycle(initialValue = "system")
            val darkTheme = when (themeMode) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }
            SeinaChanTheme(darkTheme = darkTheme) {
                val snackbarHostState = remember { SnackbarHostState() }
                val navController = rememberNavController()

                Scaffold(
                    snackbarHost = { SeinaSnackbarHost(snackbarHostState) },
                    contentWindowInsets = WindowInsets.navigationBars
                ) { innerPadding ->
                    SeinaNavHost(
                        navController = navController,
                        snackbarHostState = snackbarHostState,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    private fun bindHermesService() {
        val intent = Intent(this, HermesConnectionService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onResume() {
        super.onResume()
        // 通知 Service 应用回到前台，并触发连接检查/立即重连
        val intent = Intent(this, HermesConnectionService::class.java).apply {
            action = HermesConnectionService.ACTION_APP_FOREGROUND
        }
        startService(intent)

        val ensureIntent = Intent(this, HermesConnectionService::class.java).apply {
            action = HermesConnectionService.ACTION_ENSURE_CONNECTION
        }
        startService(ensureIntent)
    }

    override fun onPause() {
        super.onPause()
        // 通知 Service 应用进入后台
        val intent = Intent(this, HermesConnectionService::class.java).apply {
            action = HermesConnectionService.ACTION_APP_BACKGROUND
        }
        startService(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (serviceBound) {
            unbindService(serviceConnection)
            serviceBound = false
        }
    }
}
