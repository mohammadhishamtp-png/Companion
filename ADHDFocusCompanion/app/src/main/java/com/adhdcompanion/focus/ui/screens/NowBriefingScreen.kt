package com.adhdcompanion.focus.ui.screens

import android.Manifest
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.viewmodel.compose.viewModel
import com.adhdcompanion.focus.di.AppContainer
import com.adhdcompanion.focus.model.DayBlock
import com.adhdcompanion.focus.ui.components.SectionLabel
import com.adhdcompanion.focus.ui.viewmodel.ViewModelFactories

@Composable
fun NowBriefingScreen(container: AppContainer, onOpenSettings: () -> Unit) {
    val viewModel = viewModel(factory = ViewModelFactories.now(container))
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* no-op: notifications simply won't show if denied */ }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        viewModel.refresh()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Right now", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Filled.Settings, contentDescription = "Settings")
            }
        }

        Spacer(Modifier.height(20.dp))

        if (!container.prefs.hasApiKey()) {
            ApiKeyNudgeCard(onOpenSettings)
            Spacer(Modifier.height(16.dp))
        }

        when {
            state.isLoading -> Box(
                modifier = Modifier.fillMaxWidth().height(200.dp),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            state.currentBlock == null -> EmptyNowCard()

            else -> CurrentBlockCard(
                block = state.currentBlock!!,
                nextBlock = state.nextBlock,
                isSpeaking = state.isSpeaking,
                onSpeakBriefing = viewModel::speakBriefing,
                onSpeakStep = viewModel::speakMicroStepOnly,
                microStepSecondsRemaining = state.microStepSecondsRemaining,
                onStartMicroStep = viewModel::startFiveMinuteMicroStep,
                onStopMicroStep = viewModel::cancelMicroStepTimer,
                onComplete = viewModel::markCurrentBlockCompleted
            )
        }

        state.briefingScript?.let { script ->
            Spacer(Modifier.height(16.dp))
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.padding(16.dp)) {
                    SectionLabel("Briefing script")
                    Text(script, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        state.errorMessage?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun CurrentBlockCard(
    block: DayBlock,
    nextBlock: DayBlock?,
    isSpeaking: Boolean,
    onSpeakBriefing: () -> Unit,
    onSpeakStep: () -> Unit,
    microStepSecondsRemaining: Int?,
    onStartMicroStep: () -> Unit,
    onStopMicroStep: () -> Unit,
    onComplete: () -> Unit
) {
    // Buttons sit on a primary-colored card. Use onPrimary as the button fill and primary as
    // the label/icon color so contrast holds in both light and dark themes.
    val cardOn = MaterialTheme.colorScheme.onPrimary
    val cardBg = MaterialTheme.colorScheme.primary
    val filledOnCard = androidx.compose.material3.ButtonDefaults.buttonColors(
        containerColor = cardOn,
        contentColor = cardBg,
        disabledContainerColor = cardOn.copy(alpha = 0.55f),
        disabledContentColor = cardBg.copy(alpha = 0.75f)
    )
    val outlinedOnCard = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
        contentColor = cardOn,
        disabledContentColor = cardOn.copy(alpha = 0.6f)
    )
    val outlineBorder = androidx.compose.foundation.BorderStroke(1.5.dp, cardOn)

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(24.dp)) {
            Text(
                block.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(Modifier.height(8.dp))
            if (block.why.isNotBlank()) {
                Text(
                    block.why,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                )
            }

            Spacer(Modifier.height(20.dp))
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.14f))
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "TINY FIRST STEP",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        block.microStep.ifBlank { "Just open what you need and look at it. That's it." },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onSpeakBriefing,
                    enabled = !isSpeaking,
                    colors = filledOnCard
                ) {
                    Icon(Icons.Filled.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text(if (isSpeaking) "Speaking…" else "Speak briefing")
                }
                OutlinedButton(
                    onClick = onSpeakStep,
                    enabled = !isSpeaking,
                    colors = outlinedOnCard,
                    border = outlineBorder
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("Read next step")
                }
            }

            Spacer(Modifier.height(12.dp))
            if (microStepSecondsRemaining != null) {
                val mins = microStepSecondsRemaining / 60
                val secs = microStepSecondsRemaining % 60
                Text(
                    "%d:%02d".format(mins, secs),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = cardOn,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = onStopMicroStep,
                    modifier = Modifier.fillMaxWidth(),
                    colors = filledOnCard
                ) {
                    Icon(Icons.Filled.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("Stop timer")
                }
            } else {
                Button(
                    onClick = onStartMicroStep,
                    modifier = Modifier.fillMaxWidth(),
                    colors = filledOnCard
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("Start 5-Min Micro-Step")
                }
            }

            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onComplete,
                modifier = Modifier.fillMaxWidth(),
                colors = outlinedOnCard,
                border = outlineBorder
            ) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text("Mark block complete")
            }

            if (nextBlock != null) {
                Spacer(Modifier.height(16.dp))
                Text(
                    "Next up: ${nextBlock.title}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f)
                )
            }
        }
    }
}

@Composable
private fun EmptyNowCard() {
    Card(shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(24.dp)) {
            Text("Nothing scheduled right now", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                "Head to the Planner tab to lay out a low-friction day whenever you're ready — no pressure.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ApiKeyNudgeCard(onOpenSettings: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.18f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                Text("Add your Gemini key", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "The AI Planner and Supporter need it to generate plans and briefings.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(Modifier.size(8.dp))
            Button(onClick = onOpenSettings) { Text("Add") }
        }
    }
}
