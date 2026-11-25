package com.adhdcompanion.focus.data.repository

import com.adhdcompanion.focus.model.BrainDumpEntry
import com.adhdcompanion.focus.model.DayBlock

/**
 * Pluggable cloud sync boundary. The app works fully offline/local-first
 * against Room; this interface is where a Firestore, REST, or any other
 * backend can be wired in later without touching the UI or ViewModels.
 * [NoOpCloudSync] is the default until a real implementation is supplied.
 */
interface CloudSyncInterface {
    suspend fun pushBlocks(dayEpoch: Long, blocks: List<DayBlock>): Result<Unit>
    suspend fun pullBlocks(dayEpoch: Long): Result<List<DayBlock>>
    suspend fun pushBrainDump(entry: BrainDumpEntry): Result<Unit>
}

/** Default no-op implementation so the app runs fully local-first out of the box. */
class NoOpCloudSync : CloudSyncInterface {
    override suspend fun pushBlocks(dayEpoch: Long, blocks: List<DayBlock>): Result<Unit> =
        Result.success(Unit)

    override suspend fun pullBlocks(dayEpoch: Long): Result<List<DayBlock>> =
        Result.success(emptyList())

    override suspend fun pushBrainDump(entry: BrainDumpEntry): Result<Unit> =
        Result.success(Unit)
}

/**
 * Example REST-based implementation sketch. Point baseUrl at your own
 * backend and implement the matching endpoints; left disabled (NoOp used)
 * by default so the app never requires a server to function.
 */
class RestCloudSync(private val baseUrl: String, private val authToken: String?) : CloudSyncInterface {
    override suspend fun pushBlocks(dayEpoch: Long, blocks: List<DayBlock>): Result<Unit> =
        Result.failure(NotImplementedError("Wire up your REST client here, posting to $baseUrl/blocks"))

    override suspend fun pullBlocks(dayEpoch: Long): Result<List<DayBlock>> =
        Result.failure(NotImplementedError("Wire up your REST client here, reading $baseUrl/blocks"))

    override suspend fun pushBrainDump(entry: BrainDumpEntry): Result<Unit> =
        Result.failure(NotImplementedError("Wire up your REST client here, posting to $baseUrl/brain-dumps"))
}
