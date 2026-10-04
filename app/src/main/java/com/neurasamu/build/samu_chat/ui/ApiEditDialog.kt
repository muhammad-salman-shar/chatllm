package com.neurasamu.build.samu_chat.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add API" else "Edit API") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Label (e.g. My Phone Server)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    label = { Text("Base URL") },
                    placeholder = { Text("http://192.168.1.5:8080/v1") },
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
                Spacer(Modifier.height(8.dp))
                Text(
                    "Tip: URL must include /v1 or end at the root — /v1 and /chat/completions are added automatically.",
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
                        modelName = modelName.trim()
                    )
                    onSave(cfg)
                },
                enabled = baseUrl.isNotBlank() && modelName.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = {
            Row {
                if (existing != null && onDelete != null) {
                    TextButton(onClick = { onDelete(existing) }) {
                        Text("Delete", color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.width(4.dp))
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}
