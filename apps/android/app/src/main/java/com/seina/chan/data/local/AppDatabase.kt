package com.seina.chan.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.seina.chan.data.local.dao.SentImageDao
import com.seina.chan.data.local.entity.SentImageEntity

@Database(entities = [SentImageEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sentImageDao(): SentImageDao
}
