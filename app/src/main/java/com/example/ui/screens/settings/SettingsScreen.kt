package com.example.ui.screens.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberIndigo
import com.example.ui.theme.CyberRose
import com.example.ui.theme.CyberTeal
import com.example.ui.viewmodels.AppViewModel

@Composable
fun SettingsScreen(viewModel: AppViewModel) {
    val context = LocalContext.current
    val settings by viewModel.userSettings.collectAsState()
    val validation by viewModel.keyValidation.collectAsState()
    val isValidating by viewModel.isValidatingKey.collectAsState()
    val availableModels by viewModel.availableModels.collectAsState()

    var apiKeyInput by remember { mutableStateOf("") }
    var showExportDialog by remember { mutableStateOf(false) }
    var exportedJson by remember { mutableStateOf("") }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }

    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Settings & Configuration",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Manage your encrypted OpenRouter API key, model parameters, and agent loops.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // SECTION 1: OpenRouter API Key & Keystore
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = "OpenRouter API Key",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = CyberTeal, modifier = Modifier.size(14.dp))
                            Text(
                                text = "Encrypted in Android Keystore (Key material remains inaccessible to extraction)",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyberTeal
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (settings.hasApiKey) {
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Current Key (Encrypted):", style = MaterialTheme.typography.labelSmall)
                                        Text(
                                            text = settings.maskedApiKey,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = CyberTeal, modifier = Modifier.size(18.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        OutlinedTextField(
                            value = apiKeyInput,
                            onValueChange = { apiKeyInput = it },
                            label = { Text("New API Key (sk-or-v1-...)") },
                            placeholder = { Text("sk-or-v1-••••••••") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("api_key_input")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (apiKeyInput.isNotBlank()) {
                                        viewModel.saveApiKey(apiKeyInput.trim())
                                        apiKeyInput = ""
                                        Toast.makeText(context, "API Key saved & encrypted in Keystore", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                enabled = apiKeyInput.isNotBlank(),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("save_api_key_button")
                            ) {
                                Text("Save Key")
                            }

                            OutlinedButton(
                                onClick = { viewModel.validateApiKey() },
                                enabled = settings.hasApiKey && !isValidating,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("verify_api_key_button")
                            ) {
                                if (isValidating) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Testing...")
                                } else {
                                    Text("Verify with API")
                                }
                            }
                        }

                        // Validation result display
                        val res = validation
                        if (res != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                color = if (res.isValid) CyberTeal.copy(alpha = 0.15f) else CyberRose.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        if (res.isValid) Icons.Default.CheckCircle else Icons.Default.Error,
                                        contentDescription = null,
                                        tint = if (res.isValid) CyberTeal else CyberRose
                                    )
                                    Column {
                                        Text(
                                            text = if (res.isValid) "API Key Verified Successfully!" else "API Key Verification Failed",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (res.isValid) CyberTeal else CyberRose
                                        )
                                        if (res.label != null) {
                                            Text("Label: ${res.label}", style = MaterialTheme.typography.bodySmall)
                                        }
                                        if (res.errorMessage != null) {
                                            Text(res.errorMessage, style = MaterialTheme.typography.bodySmall, color = CyberRose)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // SECTION: Language Configuration
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = "Язык интерфейса / Language",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Выберите язык отображения интерфейса и подсказок.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = settings.language == "ru",
                                onClick = { viewModel.setLanguage("ru") },
                                label = { Text("Русский 🇷🇺") },
                                modifier = Modifier.testTag("lang_ru")
                            )
                            FilterChip(
                                selected = settings.language == "en",
                                onClick = { viewModel.setLanguage("en") },
                                label = { Text("English 🇬🇧") },
                                modifier = Modifier.testTag("lang_en")
                            )
                            FilterChip(
                                selected = settings.language == "system",
                                onClick = { viewModel.setLanguage("system") },
                                label = { Text("Системный") },
                                modifier = Modifier.testTag("lang_system")
                            )
                        }
                    }
                }
            }

            // SECTION 2: Model Configuration & Free Router
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = "Model Configuration",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // openrouter/free toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Use 'openrouter/free' Router", fontWeight = FontWeight.SemiBold)
                                Text(
                                    "Automatically routes to high-speed free models with tool calling support",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = settings.isOpenRouterFree,
                                onCheckedChange = { viewModel.settingsRepository.setOpenRouterFree(it) },
                                modifier = Modifier.testTag("free_router_switch")
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Default Model Picker
                        Text("Selected Model:", style = MaterialTheme.typography.labelSmall)
                        OutlinedTextField(
                            value = settings.selectedModel,
                            onValueChange = { viewModel.settingsRepository.setSelectedModel(it) },
                            singleLine = true,
                            enabled = !settings.isOpenRouterFree,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Temperature Slider
                        Text("Temperature: ${String.format("%.2f", settings.temperature)}", style = MaterialTheme.typography.bodySmall)
                        Slider(
                            value = settings.temperature,
                            onValueChange = { viewModel.settingsRepository.setTemperature(it) },
                            valueRange = 0.0f..1.5f,
                            steps = 15
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Max Tokens Slider
                        Text("Max Tokens: ${settings.maxTokens}", style = MaterialTheme.typography.bodySmall)
                        Slider(
                            value = settings.maxTokens.toFloat(),
                            onValueChange = { viewModel.settingsRepository.setMaxTokens(it.toInt()) },
                            valueRange = 256f..8192f,
                            steps = 30
                        )
                    }
                }
            }

            // SECTION 3: Agentic Execution Parameters
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Agentic Parameters & Privacy",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Max Agent Iterations
                        Text("Max Agent Iterations: ${settings.maxAgentIterations}", style = MaterialTheme.typography.bodySmall)
                        Slider(
                            value = settings.maxAgentIterations.toFloat(),
                            onValueChange = { viewModel.settingsRepository.setMaxAgentIterations(it.toInt()) },
                            valueRange = 1f..25f,
                            steps = 24
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Parallel Tool Calls
                        SettingSwitchRow(
                            title = "Parallel Tool Calls",
                            description = "Allows the model to propose multiple tool executions in a single step",
                            checked = settings.parallelToolCalls,
                            onCheckedChange = { viewModel.settingsRepository.setParallelToolCalls(it) }
                        )

                        // Confirm Dangerous Actions
                        SettingSwitchRow(
                            title = "Confirm Dangerous Actions",
                            description = "Requires explicit user approval before executing sensitive tools",
                            checked = settings.confirmDangerousActions,
                            onCheckedChange = { viewModel.settingsRepository.setConfirmDangerousActions(it) }
                        )

                        // Allow MCP
                        SettingSwitchRow(
                            title = "Allow MCP Servers",
                            description = "Includes tools exposed by configured MCP servers in agent registry",
                            checked = settings.allowMcp,
                            onCheckedChange = { viewModel.settingsRepository.setAllowMcp(it) }
                        )

                        // Allow Network Tools
                        SettingSwitchRow(
                            title = "Allow Network Tools",
                            description = "Enables web searching and external HTTP queries",
                            checked = settings.allowNetworkTools,
                            onCheckedChange = { viewModel.settingsRepository.setAllowNetworkTools(it) }
                        )

                        // Auto Send Tool Results
                        SettingSwitchRow(
                            title = "Auto-send Tool Results",
                            description = "Automatically resumes the agent loop after executing safe tools",
                            checked = settings.autoSendToolResults,
                            onCheckedChange = { viewModel.settingsRepository.setAutoSendToolResults(it) }
                        )
                    }
                }
            }

            // SECTION 4: Data Management & Export
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Data Management",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    exportedJson = viewModel.exportAllDataJson()
                                    showExportDialog = true
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Export Data")
                            }

                            OutlinedButton(
                                onClick = { showClearHistoryDialog = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Clear History")
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = { showDeleteConfirmDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Delete All Application Data")
                        }
                    }
                }
            }
        }
    }

    // Export Dialog
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Exported Data (JSON)") },
            text = {
                OutlinedTextField(
                    value = exportedJson,
                    onValueChange = {},
                    readOnly = true,
                    maxLines = 10,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Exported JSON", exportedJson))
                    Toast.makeText(context, "Exported JSON copied to clipboard", Toast.LENGTH_SHORT).show()
                    showExportDialog = false
                }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy JSON")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) { Text("Close") }
            }
        )
    }

    // Clear History Confirmation
    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = { Text("Clear Local Chat History?") },
            text = { Text("All local conversation threads and messages will be permanently deleted.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearLocalHistory()
                        showClearHistoryDialog = false
                        Toast.makeText(context, "Chat history cleared", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Delete All Confirmation
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete ALL Data?") },
            text = { Text("This will wipe all chats, projects, custom skills, MCP servers, and encrypted keys. This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAllData()
                        showDeleteConfirmDialog = false
                        Toast.makeText(context, "All data wiped", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun SettingSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
