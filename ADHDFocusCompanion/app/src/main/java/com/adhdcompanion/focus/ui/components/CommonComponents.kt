package com.adhdcompanion.focus.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.adhdcompanion.focus.navigation.FocusDestination

@Composable
fun FocusBottomBar(
    current: FocusDestination,
    onNavigate: (FocusDestination) -> Unit
) {
    NavigationBar {
        NavigationBarItem(
            selected = current == FocusDestination.Now,
            onClick = { onNavigate(FocusDestination.Now) },
            icon = { Icon(Icons.Filled.Home, contentDescription = "Now") },
            label = { Text("Now") }
        )
        NavigationBarItem(
            selected = current == FocusDestination.Stuck,
            onClick = { onNavigate(FocusDestination.Stuck) },
            icon = { Icon(Icons.Filled.Bolt, contentDescription = "Stuck") },
            label = { Text("Stuck") }
        )
        NavigationBarItem(
            selected = current == FocusDestination.Planner,
            onClick = { onNavigate(FocusDestination.Planner) },
            icon = { Icon(Icons.Filled.EditNote, contentDescription = "Planner") },
            label = { Text("Planner") }
        )
        NavigationBarItem(
            selected = current == FocusDestination.BrainDump,
            onClick = { onNavigate(FocusDestination.BrainDump) },
            icon = { Icon(Icons.Filled.Settings, contentDescription = "Dump") },
            label = { Text("Dump") }
        )
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}
