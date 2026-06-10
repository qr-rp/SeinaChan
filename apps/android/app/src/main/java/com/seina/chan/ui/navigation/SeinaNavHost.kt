package com.seina.chan.ui.navigation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.seina.chan.ui.components.GlobalEventHandler
import com.seina.chan.ui.screens.chat.ChatScreen
import com.seina.chan.ui.screens.connect.ConnectScreen

object Routes {
    const val CONNECT = "connect"
    const val CHAT = "chat"
}

@Composable
fun SeinaNavHost(
    navController: NavHostController,
    snackbarHostState: SnackbarHostState,
    startDestination: String = Routes.CONNECT,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Routes.CONNECT) {
            ConnectScreen(
                onConnected = { navController.navigate(Routes.CHAT) }
            )
        }
        composable(Routes.CHAT) {
            ChatScreen(
                viewModel = hiltViewModel(),
                sessionId = "",
                onBack = { navController.popBackStack() },
                onReconfigure = {
                    navController.navigate(Routes.CONNECT) {
                        popUpTo(Routes.CONNECT) { inclusive = true }
                    }
                }
            )
        }
    }

    GlobalEventHandler(snackbarHostState = snackbarHostState)
}
