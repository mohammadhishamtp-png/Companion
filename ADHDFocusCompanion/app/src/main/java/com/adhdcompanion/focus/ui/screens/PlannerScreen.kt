package com.adhdcompanion.focus.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.adhdcompanion.focus.di.AppContainer
import com.adhdcompanion.focus.model.DayBlock
import com.adhdcompanion.focus.model.EnergyLevel
import com.adhdcompanion.focus.ui.viewmodel.ViewModelFactories

@Composable
fun PlannerScreen(container: AppContainer) {
    val viewModel = viewModel(factory = ViewModelFactories.planner(container))
    val state by viewModel.uiState.collectAsState()
    val blocks by viewModel.todaysBlocks.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text("Day planner", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(
            "Tell the AI your hours, energy, and goals — it'll build a low-friction day with built-in decompression buffers.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value = state.availableHours,
            onValueChange = viewModel::updateHours,
            label = { Text("Available hours today") },
            placeholder = { Text("e.g. 9am-1pm, 3pm-6pm") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))
        Text("Energy level", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EnergyLevel.values().forEach { level ->
                AssistChip(
                    onClick = { viewModel.updateEnergy(level) },
                    label = { Text(level.name.lowercase().replaceFirstChar { it.uppercase() }) },
                    colors = if (state.energyLevel == level) {
                        androidx.compose.material3.AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            labelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        androidx.compose.material3.AssistChipDefaults.assistChipColors()
                    }
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = state.goalsAndBlocks,
            onValueChange = viewModel::updateGoals,
            label = { Text("Goals / required blocks") },
            placeholder = { Text("e.g. meds at 9, meditation, study Chapter 4, meal at 1") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = viewModel::generatePlan,
            enabled = !state.isGenerating,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.isGenerating) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.size(8.dp))
            } else {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
            }
            Text(if (state.isGenerating) "Generating…" else "Generate today's plan")
        }

        state.errorMessage?.let {
            Spacer(Modifier.height(10.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }

        if (blocks.isNotEmpty()) {
            Spacer(Modifier.height(28.dp))
            Text("Today's blocks", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            blocks.sortedBy { it.startMinuteOfDay }.forEach { block ->
                BlockRow(
                    block = block,
                    onToggle = { viewModel.toggleCompleted(block) },
                    onDelete = { viewModel.deleteBlock(block) }
                )
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun BlockRow(block: DayBlock, onToggle: () -> Unit, onDelete: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (block.isDecompressionBuffer)
                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onToggle) {
                Icon(
                    if (block.isCompleted) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                    contentDescription = "Toggle complete",
                    tint = if (block.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    "${formatMinutes(block.startMinuteOfDay)} · ${block.title}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (block.isCompleted) TextDecoration.LineThrough else null
                )
                if (block.why.isNotBlank()) {
                    Text(
                        block.why,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Close, contentDescription = "Remove block")
            }
        }
    }
}

private fun formatMinutes(minuteOfDay: Int): String {
    val hour24 = minuteOfDay / 60
    val minute = minuteOfDay % 60
    val amPm = if (hour24 < 12) "AM" else "PM"
    val hour12 = when {
        hour24 == 0 -> 12
        hour24 > 12 -> hour24 - 12
        else -> hour24
    }
    return "%d:%02d %s".format(hour12, minute, amPm)
}
