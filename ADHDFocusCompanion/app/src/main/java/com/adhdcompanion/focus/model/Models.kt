package com.adhdcompanion.focus.model

/**
 * A single block of the day: a study session, a meds reminder, a meditation
 * pause, a meal, a decompression buffer, etc. This is the core unit the
 * whole app revolves around.
 */
data class DayBlock(
    val id: Long = 0,
    val title: String,
    val why: String,
    val microStep: String,
    val startMinuteOfDay: Int,   // minutes since midnight, e.g. 9:30am = 570
    val durationMinutes: Int,
    val category: BlockCategory = BlockCategory.STUDY,
    val isDecompressionBuffer: Boolean = false,
    val isCompleted: Boolean = false
)

enum class BlockCategory {
    STUDY, CODING, MEDS, MEDITATION, MEAL, MOVEMENT, BUFFER, OTHER
}

data class BrainDumpEntry(
    val id: Long = 0,
    val text: String,
    val createdAtMillis: Long,
    val sentToPlanner: Boolean = false
)

/**
 * Input the user gives the AI Planner to generate a day.
 */
data class PlannerInput(
    val availableHours: String,
    val energyLevel: EnergyLevel,
    val goalsAndBlocks: String
)

enum class EnergyLevel { LOW, MEDIUM, HIGH }

/**
 * A single unblocker micro-step the AI supporter returns when the user is stuck.
 */
data class MicroStep(
    val id: Int,
    val text: String,
    val isChecked: Boolean = false
)

/**
 * The full response from the "I'm stuck" AI supporter flow.
 */
data class UnblockerResponse(
    val calmMessage: String,
    val whyReminder: String,
    val microSteps: List<MicroStep>
)
