package com.adhdcompanion.focus.data.repository

import com.adhdcompanion.focus.data.local.BrainDumpDao
import com.adhdcompanion.focus.data.local.toDomain
import com.adhdcompanion.focus.data.local.toEntity
import com.adhdcompanion.focus.model.BrainDumpEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BrainDumpRepository(
    private val dao: BrainDumpDao,
    private val cloudSync: CloudSyncInterface = NoOpCloudSync()
) {
    fun observeAll(): Flow<List<BrainDumpEntry>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun quickCapture(text: String) {
        if (text.isBlank()) return
        val entry = BrainDumpEntry(text = text.trim(), createdAtMillis = System.currentTimeMillis())
        dao.insert(entry.toEntity())
        cloudSync.pushBrainDump(entry)
    }

    suspend fun markSentToPlanner(entry: BrainDumpEntry) {
        dao.update(entry.copy(sentToPlanner = true).toEntity())
    }

    suspend fun delete(entry: BrainDumpEntry) {
        dao.delete(entry.toEntity())
    }
}
