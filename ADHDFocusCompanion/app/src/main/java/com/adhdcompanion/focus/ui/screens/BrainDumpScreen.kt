package com.adhdcompanion.focus.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.adhdcompanion.focus.di.AppContainer
import com.adhdcompanion.focus.model.BrainDumpEntry
import com.adhdcompanion.focus.ui.viewmodel.ViewModelFactories
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BrainDumpScreen(container: AppContainer) {
    val viewModel = viewModel(factory = ViewModelFactories.brainDump(container))
    val draft by viewModel.draftText.collectAsState()
    val entries by viewModel.entries.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text("Brain dump", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(
            "Capture the stray thought in two seconds, then get right back to your block.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = draft,
                onValueChange = viewModel::updateDraft,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Quick thought…") },
                singleLine = true
            )
            Spacer(Modifier.width(8.dp))
            FloatingActionButton(onClick = viewModel::capture) {
                Icon(Icons.Filled.Send, contentDescription = "Capture")
            }
        }

        Spacer(Modifier.height(20.dp))

        if (entries.isEmpty()) {
            Text(
                "Nothing captured yet. Whenever a thought pulls at your attention, drop it here instead of chasing it.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn {
                items(entries, key = { it.id }) { entry ->
                    BrainDumpRow(
                        entry = entry,
                        onSendToPlanner = { viewModel.sendToPlanner(entry) },
                        onDelete = { viewModel.delete(entry) }
                    )
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
private fun BrainDumpRow(entry: BrainDumpEntry, onSendToPlanner: () -> Unit, onDelete: () -> Unit) {
    val formatter = remember(entry.id) { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }
    Card(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(entry.text, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(2.dp))
                Text(
                    formatter.format(Date(entry.createdAtMillis)) +
                        if (entry.sentToPlanner) " · sent to planner" else "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!entry.sentToPlanner) {
                IconButton(onClick = onSendToPlanner) {
                    Icon(Icons.Filled.Send, contentDescription = "Send to planner")
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete")
            }
        }
    }
}
