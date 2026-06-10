package com.seina.chan.ui.screens.chat

import android.net.Uri
import com.seina.chan.data.model.ChatMessage

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val currentInput: String = "",
    val canSend: Boolean = true,
    val error: String? = null,
    val selectedImages: List<Uri> = emptyList()
)
