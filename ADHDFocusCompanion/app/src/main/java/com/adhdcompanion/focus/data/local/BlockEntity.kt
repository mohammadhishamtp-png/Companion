package com.adhdcompanion.focus.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.adhdcompanion.focus.model.BlockCategory
import com.adhdcompanion.focus.model.DayBlock

@Entity(tableName = "day_blocks")
data class BlockEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val why: String,
    val microStep: String,
    val startMinuteOfDay: Int,
    val durationMinutes: Int,
    val category: String,
    val isDecompressionBuffer: Boolean,
    val isCompleted: Boolean,
    val dayEpoch: Long // days since epoch, so each day has its own block set
)

fun BlockEntity.toDomain(): DayBlock = DayBlock(
    id = id,
    title = title,
    why = why,
    microStep = microStep,
    startMinuteOfDay = startMinuteOfDay,
    durationMinutes = durationMinutes,
    category = runCatching { BlockCategory.valueOf(category) }.getOrDefault(BlockCategory.OTHER),
    isDecompressionBuffer = isDecompressionBuffer,
    isCompleted = isCompleted
)

fun DayBlock.toEntity(dayEpoch: Long): BlockEntity = BlockEntity(
    id = id,
    title = title,
    why = why,
    microStep = microStep,
    startMinuteOfDay = startMinuteOfDay,
    durationMinutes = durationMinutes,
    category = category.name,
    isDecompressionBuffer = isDecompressionBuffer,
    isCompleted = isCompleted,
    dayEpoch = dayEpoch
)
