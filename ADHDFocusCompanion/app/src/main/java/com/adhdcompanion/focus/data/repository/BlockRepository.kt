package com.adhdcompanion.focus.data.repository

import com.adhdcompanion.focus.data.local.BlockDao
import com.adhdcompanion.focus.data.local.toDomain
import com.adhdcompanion.focus.data.local.toEntity
import com.adhdcompanion.focus.model.DayBlock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId

/**
 * Local-first repository: every read/write goes through Room immediately,
 * and cloud sync (if configured) happens best-effort alongside it.
 */
class BlockRepository(
    private val dao: BlockDao,
    private val cloudSync: CloudSyncInterface = NoOpCloudSync()
) {
    fun todayEpoch(): Long = LocalDate.now(ZoneId.systemDefault()).toEpochDay()

    fun observeBlocksForDay(dayEpoch: Long = todayEpoch()): Flow<List<DayBlock>> =
        dao.observeBlocksForDay(dayEpoch).map { list -> list.map { it.toDomain() } }

    suspend fun getBlocksForDay(dayEpoch: Long = todayEpoch()): List<DayBlock> =
        dao.getBlocksForDay(dayEpoch).map { it.toDomain() }

    /** Finds the block that contains "now" (by minute-of-day), if any. */
    suspend fun getCurrentBlock(nowMinuteOfDay: Int, dayEpoch: Long = todayEpoch()): DayBlock? =
        getBlocksForDay(dayEpoch).firstOrNull {
            nowMinuteOfDay in it.startMinuteOfDay until (it.startMinuteOfDay + it.durationMinutes)
        }

    suspend fun getNextBlock(nowMinuteOfDay: Int, dayEpoch: Long = todayEpoch()): DayBlock? =
        getBlocksForDay(dayEpoch)
            .filter { it.startMinuteOfDay > nowMinuteOfDay }
            .minByOrNull { it.startMinuteOfDay }

    suspend fun replaceDayPlan(blocks: List<DayBlock>, dayEpoch: Long = todayEpoch()) {
        dao.clearDay(dayEpoch)
        dao.insertAll(blocks.map { it.toEntity(dayEpoch) })
        cloudSync.pushBlocks(dayEpoch, blocks)
    }

    suspend fun upsertBlock(block: DayBlock, dayEpoch: Long = todayEpoch()) {
        if (block.id == 0L) {
            dao.insert(block.toEntity(dayEpoch))
        } else {
            dao.update(block.toEntity(dayEpoch))
        }
    }

    suspend fun markCompleted(block: DayBlock, dayEpoch: Long = todayEpoch()) {
        dao.update(block.copy(isCompleted = true).toEntity(dayEpoch))
    }

    suspend fun deleteBlock(block: DayBlock, dayEpoch: Long = todayEpoch()) {
        dao.delete(block.toEntity(dayEpoch))
    }
}
