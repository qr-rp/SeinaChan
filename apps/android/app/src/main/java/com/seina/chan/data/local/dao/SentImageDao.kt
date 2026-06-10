package com.seina.chan.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.seina.chan.data.local.entity.SentImageEntity

@Dao
interface SentImageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: SentImageEntity)

    @Query("SELECT localUri FROM sent_images WHERE serverPath = :serverPath LIMIT 1")
    suspend fun getUriByServerPath(serverPath: String): String?
}
