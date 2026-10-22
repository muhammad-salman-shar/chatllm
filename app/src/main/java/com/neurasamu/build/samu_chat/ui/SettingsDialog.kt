package com.neurasamu.build.samu_chat.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.neurasamu.build.samu_chat.data.AppSettings

@Composable
fun SettingsDialog(
    current: AppSettings,
    onSave: (AppSettings) -> Unit,
    onDismiss: () -> Unit
) {
    var theme by remember { mutableStateOf(current.theme) }
    var fontScale by remember { mutableStateOf(current.fontSizeMultiplier) }
    var ci by remember { mutableStateOf(current.customInstructions) }
    var ciEnabled by remember { mutableStateOf(current.customInstructionsEnabled) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings", fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()).heightIn(max = 500.dp)) {
                // Theme
                Text("Theme", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(4.dp))
                listOf("light" to "Light", "dark" to "Dark", "system" to "System").forEach { (k, label) ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        RadioButton(selected = theme == k, onClick = { theme = k })
                        Text(label)
                    }
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))

                // Font size
                Text("Font size: ${"%.1f".format(fontScale)}x", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(4.dp))
                Slider(
                    value = fontScale,
                    onValueChange = { fontScale = it },
                    valueRange = 0.8f..1.4f,
                    steps = 5
                )

                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))

                // Custom instructions (app-level)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Universal Custom Instructions", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    Switch(checked = ciEnabled, onCheckedChange = { ciEnabled = it })
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "Applied to all APIs. Sent as system prompt before every request.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = ci,
                    onValueChange = { ci = it },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
                    placeholder = { Text("e.g. You are a helpful assistant. Reply in Urdu.") },
                    maxLines = 8,
                    enabled = ciEnabled
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(AppSettings(
                    theme = theme,
                    fontSizeMultiplier = fontScale,
                    customInstructions = ci,
                    customInstructionsEnabled = ciEnabled
                ))
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
