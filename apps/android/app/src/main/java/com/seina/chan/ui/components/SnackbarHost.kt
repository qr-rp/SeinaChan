package com.seina.chan.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.seina.chan.ui.theme.AppShapes
import com.seina.chan.ui.theme.Canvas
import com.seina.chan.ui.theme.Spacing
import com.seina.chan.ui.theme.SurfaceDark

@Composable
fun SeinaSnackbarHost(
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    SnackbarHost(
        hostState = snackbarHostState,
        modifier = modifier
    ) { data ->
        Snackbar(
            snackbarData = data,
            containerColor = SurfaceDark,
            contentColor = Canvas,
            shape = AppShapes.lg,
            modifier = Modifier.padding(Spacing.md)
        )
    }
}
