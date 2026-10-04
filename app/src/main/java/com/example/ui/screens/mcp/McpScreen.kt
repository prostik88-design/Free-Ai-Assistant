package com.example.ui.screens.mcp

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.McpServerEntity
import com.example.data.mcp.McpClient
import com.example.data.mcp.McpToolDefinition
import com.example.data.work.McpSyncWorker
import androidx.compose.ui.platform.LocalContext
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberTeal
import com.example.ui.theme.CyberRose
import com.example.ui.viewmodels.AppViewModel
import kotlinx.coroutines.launch

@Composable
fun McpScreen(viewModel: AppViewModel) {
    val context = LocalContext.current
    val servers by viewModel.allMcpServers.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    var serverName by remember { mutableStateOf("") }
    var serverUrl by remember { mutableStateOf("") }
    var transportType by remember { mutableStateOf("HTTP") }
    var authHeader by remember { mutableStateOf("") }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    serverName = ""
                    serverUrl = ""
                    transportType = "HTTP"
                    authHeader = ""
                    showAddDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_mcp_server_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add MCP Server")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Model Context Protocol (MCP)",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Подключение внешних серверов инструментов через JSON-RPC 2.0 для агентного режима.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            McpSyncWorker.enqueue(context)
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Фоновая синхронизация (WorkManager)", fontSize = 11.sp)
                    }
                }
            }

            if (servers.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Dns,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Нет подключенных MCP-серверов",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Подключите рабочий сервер по HTTP/JSON-RPC или добавьте демонстрационный сервер.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                viewModel.addMcpServer(
                                    name = "Cloud Services MCP Demo",
                                    url = "mock://cloud-services.local",
                                    transport = "MOCK",
                                    auth = null
                                )
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Sensors, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Подключить Demo MCP Server")
                        }
                    }
                }
            } else {
                items(servers, key = { it.id }) { srv ->
                    McpServerCard(
                        server = srv,
                        onToggle = { enabled -> viewModel.toggleMcpServer(srv.id, enabled) },
                        onSync = { viewModel.syncMcpServerTools(srv.id) },
                        onDelete = { viewModel.deleteMcpServer(srv.id) }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Подключение MCP Server") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = serverName,
                        onValueChange = { serverName = it },
                        label = { Text("Название сервера *") },
                        placeholder = { Text("Database MCP или Weather MCP") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = serverUrl,
                        onValueChange = { serverUrl = it },
                        label = { Text("Endpoint URL *") },
                        placeholder = { Text("http://10.0.2.2:8000/mcp или mock://...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = authHeader,
                        onValueChange = { authHeader = it },
                        label = { Text("Authorization Header (Опционально)") },
                        placeholder = { Text("Bearer token...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (serverName.isNotBlank() && serverUrl.isNotBlank()) {
                            viewModel.addMcpServer(
                                name = serverName.trim(),
                                url = serverUrl.trim(),
                                transport = transportType,
                                auth = if (authHeader.isBlank()) null else authHeader.trim()
                            )
                            showAddDialog = false
                        }
                    },
                    enabled = serverName.isNotBlank() && serverUrl.isNotBlank()
                ) {
                    Text("Подключить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
fun McpServerCard(
    server: McpServerEntity,
    onToggle: (Boolean) -> Unit,
    onSync: () -> Unit,
    onDelete: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var expanded by remember { mutableStateOf(false) }
    var testingTool by remember { mutableStateOf<McpToolDefinition?>(null) }
    var testArgsInput by remember { mutableStateOf("{\"city\":\"Berlin\"}") }
    var testResultOutput by remember { mutableStateOf<String?>(null) }
    var isTesting by remember { mutableStateOf(false) }

    val tools = remember(server.toolsJson) {
        McpToolDefinition.parseList(server.toolsJson)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.Dns,
                        contentDescription = null,
                        tint = if (server.isEnabled) CyberTeal else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Column {
                        Text(
                            text = server.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = server.serverUrl,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }

                Switch(
                    checked = server.isEnabled,
                    onCheckedChange = onToggle,
                    modifier = Modifier.testTag("mcp_toggle_${server.id}")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "${tools.size} exposed tools",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyberTeal,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(onClick = onSync, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Refresh, contentDescription = "Sync Tools", modifier = Modifier.size(16.dp))
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = { expanded = !expanded }, modifier = Modifier.size(28.dp)) {
                        Icon(
                            if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Expand"
                        )
                    }
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (tools.isEmpty()) {
                        Text("Инструменты не обнаружены. Нажмите обновить для синхронизации.", style = MaterialTheme.typography.bodySmall)
                    } else {
                        for (t in tools) {
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = t.name,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            if (t.isDangerous) {
                                                Surface(color = CyberAmber.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                                                    Text("DANGEROUS", color = CyberAmber, fontSize = 9.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                                }
                                            }

                                            OutlinedButton(
                                                onClick = {
                                                    testingTool = t
                                                    testArgsInput = if (t.name.contains("weather")) "{\"city\":\"Tokyo\"}" else "{}"
                                                    testResultOutput = null
                                                },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                modifier = Modifier.height(28.dp)
                                            ) {
                                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Тест", fontSize = 11.sp)
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = t.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Tool Interactive Sandbox Dialog
    if (testingTool != null) {
        val tool = testingTool!!
        AlertDialog(
            onDismissRequest = { testingTool = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = CyberTeal)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Тестирование: ${tool.name}")
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(tool.description, style = MaterialTheme.typography.bodySmall)

                    OutlinedTextField(
                        value = testArgsInput,
                        onValueChange = { testArgsInput = it },
                        label = { Text("Параметры вызова (JSON)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4
                    )

                    Button(
                        onClick = {
                            isTesting = true
                            testResultOutput = null
                            coroutineScope.launch {
                                val client = McpClient()
                                val res = client.executeTool(
                                    serverUrl = server.serverUrl,
                                    authHeader = server.authHeader,
                                    toolName = tool.name,
                                    argumentsJson = testArgsInput
                                )
                                testResultOutput = res.output
                                isTesting = false
                            }
                        },
                        enabled = !isTesting,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isTesting) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Выполнение...")
                        } else {
                            Text("Выполнить вызов JSON-RPC")
                        }
                    }

                    if (testResultOutput != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("Результат ответа:", style = MaterialTheme.typography.labelSmall, color = CyberTeal)
                                Text(
                                    text = testResultOutput!!,
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { testingTool = null }) {
                    Text("Закрыть")
                }
            }
        )
    }
}
