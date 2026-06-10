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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.seina.chan.service.HermesConnectionService
import com.seina.chan.ui.components.SeinaSnackbarHost
import com.seina.chan.ui.navigation.SeinaNavHost
import com.seina.chan.ui.theme.SeinaChanTheme
import com.seina.chan.util.FileLogger
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
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
            SeinaChanTheme {
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

    override fun onDestroy() {
        super.onDestroy()
        if (serviceBound) {
            unbindService(serviceConnection)
            serviceBound = false
        }
    }
}
