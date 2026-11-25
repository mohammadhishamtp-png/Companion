package com.adhdcompanion.focus.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockDao {
    @Query("SELECT * FROM day_blocks WHERE dayEpoch = :dayEpoch ORDER BY startMinuteOfDay ASC")
    fun observeBlocksForDay(dayEpoch: Long): Flow<List<BlockEntity>>

    @Query("SELECT * FROM day_blocks WHERE dayEpoch = :dayEpoch ORDER BY startMinuteOfDay ASC")
    suspend fun getBlocksForDay(dayEpoch: Long): List<BlockEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(blocks: List<BlockEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(block: BlockEntity): Long

    @Update
    suspend fun update(block: BlockEntity)

    @Delete
    suspend fun delete(block: BlockEntity)

    @Query("DELETE FROM day_blocks WHERE dayEpoch = :dayEpoch")
    suspend fun clearDay(dayEpoch: Long)
}
