package com.adhdcompanion.focus.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BrainDumpDao {
    @Query("SELECT * FROM brain_dumps ORDER BY createdAtMillis DESC")
    fun observeAll(): Flow<List<BrainDumpEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: BrainDumpEntity): Long

    @Update
    suspend fun update(entry: BrainDumpEntity)

    @Delete
    suspend fun delete(entry: BrainDumpEntity)
}
