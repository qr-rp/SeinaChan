package com.seina.chan.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sent_images")
data class SentImageEntity(
    @PrimaryKey
    val serverPath: String,
    val localUri: String,
    val timestamp: Long = System.currentTimeMillis()
)
