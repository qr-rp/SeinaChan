package com.seina.chan.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.seina.chan.data.local.dao.MessageDao
import com.seina.chan.data.local.dao.SentImageDao
import com.seina.chan.data.local.entity.MessageEntity
import com.seina.chan.data.local.entity.SentImageEntity

@Database(entities = [SentImageEntity::class, MessageEntity::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sentImageDao(): SentImageDao
    abstract fun messageDao(): MessageDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS messages (
                        id TEXT PRIMARY KEY NOT NULL,
                        sessionId TEXT NOT NULL,
                        role TEXT NOT NULL,
                        content TEXT NOT NULL,
                        reasoningText TEXT NOT NULL DEFAULT '',
                        isReasoning INTEGER NOT NULL DEFAULT 0,
                        imageUrl TEXT,
                        toolCallsJson TEXT NOT NULL DEFAULT '[]',
                        systemEventsJson TEXT NOT NULL DEFAULT '[]',
                        isStreaming INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """)
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE messages ADD COLUMN parentId TEXT DEFAULT NULL")
            }
        }
    }
}
