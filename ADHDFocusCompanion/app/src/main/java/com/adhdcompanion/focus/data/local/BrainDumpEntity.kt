package com.adhdcompanion.focus.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.adhdcompanion.focus.model.BrainDumpEntry

@Entity(tableName = "brain_dumps")
data class BrainDumpEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val createdAtMillis: Long,
    val sentToPlanner: Boolean
)

fun BrainDumpEntity.toDomain() = BrainDumpEntry(
    id = id,
    text = text,
    createdAtMillis = createdAtMillis,
    sentToPlanner = sentToPlanner
)

fun BrainDumpEntry.toEntity() = BrainDumpEntity(
    id = id,
    text = text,
    createdAtMillis = createdAtMillis,
    sentToPlanner = sentToPlanner
)
