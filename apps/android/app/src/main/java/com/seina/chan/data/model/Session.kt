package com.seina.chan.data.model

data class Session(
    val id: String,
    val title: String?,
    val preview: String?,
    val messageCount: Int,
    val lastActiveAt: String?
)
