package com.adhdcompanion.focus.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.adhdcompanion.focus.alarm.AlarmScheduler
import com.adhdcompanion.focus.data.prefs.SecurePrefsManager
import com.adhdcompanion.focus.di.AppContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(container: AppContainer, onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = container.prefs

    var apiKey by remember { mutableStateOf(prefs.geminiApiKey.orEmpty()) }
    var keyVisible by remember { mutableStateOf(false) }
    var useFallback by remember { mutableStateOf(prefs.useFlashLiteFallbackModel) }
    var voicePref by remember { mutableStateOf(prefs.voicePreference) }
    var morningTime by remember { mutableStateOf(prefs.morningBriefingTime.orEmpty()) }
    var savedMessage by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Settings") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Text("Gemini API key", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(
                "Stored encrypted on this device only. Get a free key at aistudio.google.com.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text("API key") },
                singleLine = true,
                visualTransformation = if (keyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { keyVisible = !keyVisible }) {
                        Icon(
                            if (keyVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = "Toggle visibility"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Use lighter fallback model", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Switch planning/unblocking calls to gemini-3.5-flash-lite instead of gemini-4-hydrogen.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = useFallback, onCheckedChange = { useFallback = it })
            }

            Spacer(Modifier.height(24.dp))
            Divider()
            Spacer(Modifier.height(24.dp))

            Text("Voice", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            VoiceOptionRow(
                label = "Gemini expressive voice (falls back automatically if offline)",
                selected = voicePref == SecurePrefsManager.VOICE_GEMINI_WITH_FALLBACK,
                onSelect = { voicePref = SecurePrefsManager.VOICE_GEMINI_WITH_FALLBACK }
            )
            VoiceOptionRow(
                label = "Android built-in voice only",
                selected = voicePref == SecurePrefsManager.VOICE_ANDROID_ONLY,
                onSelect = { voicePref = SecurePrefsManager.VOICE_ANDROID_ONLY }
            )

            Spacer(Modifier.height(24.dp))
            Divider()
            Spacer(Modifier.height(24.dp))

            Text("Morning briefing alarm", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(
                "24-hour time, e.g. 07:30. Uses an exact AlarmManager alarm so it's never late.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = morningTime,
                onValueChange = { morningTime = it },
                label = { Text("HH:mm") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                !AlarmScheduler(context).canScheduleExactAlarms()
            ) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "Exact alarms are currently blocked by the system for this app.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(6.dp))
                Button(onClick = {
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                    context.startActivity(intent)
                }) {
                    Text("Allow exact alarms")
                }
            }

            Spacer(Modifier.height(32.dp))
            Button(
                onClick = {
                    prefs.geminiApiKey = apiKey.trim().ifBlank { null }
                    prefs.useFlashLiteFallbackModel = useFallback
                    prefs.voicePreference = voicePref
                    prefs.morningBriefingTime = morningTime.trim().ifBlank { null }

                    if (!morningTime.isBlank() && morningTime.matches(Regex("^\\d{1,2}:\\d{2}$"))) {
                        AlarmScheduler(context).scheduleNextMorningBriefing()
                    } else {
                        AlarmScheduler(context).cancelDailyAlarm()
                    }

                    savedMessage = "Saved."
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save settings")
            }

            savedMessage?.let {
                Spacer(Modifier.height(10.dp))
                Text(it, color = MaterialTheme.colorScheme.primary)
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun VoiceOptionRow(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(52.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}
