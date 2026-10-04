package com.neurasamu.build.samu_chat.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.neurasamu.build.samu_chat.data.ApiConfig
import java.util.UUID

@Composable
fun ApiEditDialog(
    existing: ApiConfig?,
    onSave: (ApiConfig) -> Unit,
    onDismiss: () -> Unit,
    onDelete: ((ApiConfig) -> Unit)? = null
) {
    var label by remember { mutableStateOf(existing?.label ?: "") }
    var baseUrl by remember { mutableStateOf(existing?.baseUrl ?: "") }
    var apiKey by remember { mutableStateOf(existing?.apiKey ?: "") }
    var modelName by remember { mutableStateOf(existing?.modelName ?: "") }
    var systemPrompt by remember { mutableStateOf(existing?.systemPrompt ?: "") }
    var contextWindow by remember { mutableStateOf((existing?.contextWindow ?: 4096).toString()) }
    var maxTokensPerReply by remember { mutableStateOf((existing?.maxTokensPerReply ?: 1024).toString()) }
    var temperature by remember { mutableStateOf((existing?.temperature ?: 0.7f).toString()) }
    var confirmDelete by remember { mutableStateOf(false) }

    if (confirmDelete && existing != null && onDelete != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete API?") },
            text = { Text("Remove \"${existing.label}\"? Conversations stay but become orphaned.") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(existing)
                    confirmDelete = false
                }) { Text("Delete", color = MaterialTheme.colorScheme.primary) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add API" else "Edit API") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Label") },
                    placeholder = { Text("e.g. My Phone Server") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    label = { Text("Base URL") },
                    placeholder = { Text("http://192.168.1.5:8080") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API key (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = modelName,
                    onValueChange = { modelName = it },
                    label = { Text("Model name") },
                    placeholder = { Text("gemma-3-4b-it") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = systemPrompt,
                    onValueChange = { systemPrompt = it },
                    label = { Text("Custom instructions (system prompt)") },
                    placeholder = { Text("You are a helpful assistant…") },
                    minLines = 3,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))
                Text("Model settings", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = contextWindow,
                    onValueChange = { contextWindow = it.filter { c -> c.isDigit() } },
                    label = { Text("Context window (tokens)") },
                    placeholder = { Text("4096") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = maxTokensPerReply,
                    onValueChange = { maxTokensPerReply = it.filter { c -> c.isDigit() } },
                    label = { Text("Max reply tokens") },
                    placeholder = { Text("1024") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = temperature,
                    onValueChange = { temperature = it },
                    label = { Text("Temperature (0.0-2.0)") },
                    placeholder = { Text("0.7") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Tip: Match these with your model's real values. SaMu Lab shows real context in its self-test.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "URL note: /v1 and /chat/completions are appended automatically.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val cfg = ApiConfig(
                        id = existing?.id ?: UUID.randomUUID().toString(),
                        label = label.ifBlank { "My API" },
                        baseUrl = baseUrl.trim(),
                        apiKey = apiKey.trim(),
                        modelName = modelName.trim(),
                        systemPrompt = systemPrompt.trim(),
                        contextWindow = contextWindow.toIntOrNull()?.coerceIn(512, 131072) ?: 4096,
                        maxTokensPerReply = maxTokensPerReply.toIntOrNull()?.coerceIn(64, 8192) ?: 1024,
                        temperature = temperature.toFloatOrNull()?.coerceIn(0f, 2f) ?: 0.7f
                    )
                    onSave(cfg)
                },
                enabled = baseUrl.isNotBlank() && modelName.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = {
            Row {
                if (existing != null && onDelete != null) {
                    TextButton(onClick = { confirmDelete = true }) {
                        Text("Delete API", color = Color(0xFFEF4444))
                    }
                    Spacer(Modifier.width(4.dp))
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}
