package com.example.ui.screens.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.agent.AgentExecutionState
import com.example.data.local.entity.MessageEntity
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberBlue
import com.example.ui.theme.CyberIndigo
import com.example.ui.theme.CyberRose
import com.example.ui.theme.CyberTeal
import com.example.ui.viewmodels.AppViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    chatId: String,
    viewModel: AppViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(chatId) {
        viewModel.selectChat(chatId)
    }

    val activeChat by viewModel.activeChat.collectAsState()
    val messages by viewModel.activeChatMessages.collectAsState()
    val agentState by viewModel.agentState.collectAsState()
    val allSkills by viewModel.allSkills.collectAsState()
    val allProjects by viewModel.allProjects.collectAsState()
    val availableModels by viewModel.availableModels.collectAsState()

    var inputText by remember { mutableStateOf("") }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf("") }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showModelMenu by remember { mutableStateOf(false) }
    var showSkillMenu by remember { mutableStateOf(false) }
    var showProjectMenu by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    // Auto-scroll on new messages or streaming
    LaunchedEffect(messages.size, agentState) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val isRunning = agentState is AgentExecutionState.Streaming ||
            agentState is AgentExecutionState.ExecutingTool

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = activeChat?.title ?: "Chat",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = activeChat?.modelId ?: "openrouter/free",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (activeChat?.isAgentic == true) {
                                Surface(
                                    color = CyberTeal.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "Agentic",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CyberTeal,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("chat_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Agentic Mode Toggle
                    IconButton(
                        onClick = {
                            val current = activeChat?.isAgentic ?: true
                            viewModel.toggleActiveChatAgentic(!current)
                        },
                        modifier = Modifier.testTag("toggle_agentic_button")
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = "Toggle Agentic Mode",
                            tint = if (activeChat?.isAgentic == true) CyberTeal else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // More Menu
                    IconButton(onClick = { showMoreMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                    }
                    DropdownMenu(
                        expanded = showMoreMenu,
                        onDismissRequest = { showMoreMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Rename Chat") },
                            onClick = {
                                showMoreMenu = false
                                renameText = activeChat?.title ?: ""
                                showRenameDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Change Model") },
                            onClick = {
                                showMoreMenu = false
                                showModelMenu = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Attach Skill") },
                            onClick = {
                                showMoreMenu = false
                                showSkillMenu = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Bind to Project") },
                            onClick = {
                                showMoreMenu = false
                                showProjectMenu = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete Chat", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMoreMenu = false
                                viewModel.deleteChat(chatId)
                                onNavigateBack()
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Context bar: Model, Skill, Project chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Model Chip
                FilterChip(
                    selected = true,
                    onClick = { showModelMenu = true },
                    label = {
                        Text(
                            text = (activeChat?.modelId ?: "openrouter/free").substringAfterLast('/'),
                            maxLines = 1,
                            fontSize = 11.sp
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CyberIndigo.copy(alpha = 0.2f),
                        selectedLabelColor = CyberIndigo
                    )
                )

                // Skill Chip
                val activeSkill = allSkills.firstOrNull { it.id == activeChat?.activeSkillId }
                FilterChip(
                    selected = activeSkill != null,
                    onClick = { showSkillMenu = true },
                    label = {
                        Text(
                            text = activeSkill?.name ?: "No Skill",
                            maxLines = 1,
                            fontSize = 11.sp
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Extension, contentDescription = null, modifier = Modifier.size(14.dp))
                    }
                )

                // Project Chip
                val activeProj = allProjects.firstOrNull { it.id == activeChat?.projectId }
                FilterChip(
                    selected = activeProj != null,
                    onClick = { showProjectMenu = true },
                    label = {
                        Text(
                            text = activeProj?.name ?: "No Project",
                            maxLines = 1,
                            fontSize = 11.sp
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(14.dp))
                    }
                )
            }

            // Progress indicator if agent is busy
            if (isRunning) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = CyberTeal
                )
            }

            // Messages LazyColumn
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (messages.isEmpty() && agentState is AgentExecutionState.Idle) {
                    item {
                        EmptyChatState(
                            onSelectPrompt = { prompt ->
                                inputText = prompt
                            }
                        )
                    }
                }

                items(messages, key = { it.id }) { message ->
                    when (message.role) {
                        "user" -> UserMessageBubble(
                            message = message,
                            onCopy = {
                                copyToClipboard(context, message.content)
                            },
                            onEdit = {
                                inputText = message.content
                            }
                        )
                        "assistant" -> AssistantMessageBubble(
                            message = message,
                            onCopy = {
                                copyToClipboard(context, message.content)
                            },
                            onRetry = {
                                val lastUserMsg = messages.takeWhile { it.id != message.id }
                                    .lastOrNull { it.role == "user" }
                                val prompt = lastUserMsg?.content ?: message.content
                                viewModel.sendMessage(chatId, prompt)
                            }
                        )
                        "tool" -> ToolMessageCard(
                            message = message,
                            onApprove = { viewModel.approveDangerousAction(message.id) },
                            onDeny = { viewModel.denyDangerousAction(message.id) }
                        )
                    }
                }

                // Streaming partial message
                if (agentState is AgentExecutionState.Streaming) {
                    val partial = (agentState as AgentExecutionState.Streaming).partialText
                    item {
                        StreamingBubble(text = partial)
                    }
                }

                // Tool execution indicator in list
                if (agentState is AgentExecutionState.ExecutingTool) {
                    val exec = agentState as AgentExecutionState.ExecutingTool
                    item {
                        ToolExecutingBanner(
                            toolName = exec.toolName,
                            source = exec.source,
                            iteration = exec.iteration
                        )
                    }
                }

                // Error Banner
                if (agentState is AgentExecutionState.Error) {
                    val err = (agentState as AgentExecutionState.Error).message
                    item {
                        ErrorBanner(message = err)
                    }
                }
            }

            // Bottom Input Bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Ask anything or give a task...") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_input_field"),
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    if (isRunning) {
                        IconButton(
                            onClick = { viewModel.cancelStreaming() },
                            modifier = Modifier
                                .size(48.dp)
                                .background(MaterialTheme.colorScheme.errorContainer, CircleShape)
                                .testTag("stop_streaming_button")
                        ) {
                            Icon(
                                Icons.Default.Stop,
                                contentDescription = "Stop",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    } else {
                        IconButton(
                            onClick = {
                                if (inputText.isNotBlank()) {
                                    val text = inputText
                                    inputText = ""
                                    viewModel.sendMessage(chatId, text)
                                }
                            },
                            enabled = inputText.isNotBlank(),
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    if (inputText.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    CircleShape
                                )
                                .testTag("send_message_button")
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = if (inputText.isNotBlank()) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    // Rename Dialog
    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Chat") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (renameText.isNotBlank()) {
                        viewModel.renameChat(chatId, renameText.trim())
                    }
                    showRenameDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Model Picker Menu Dialog
    if (showModelMenu) {
        AlertDialog(
            onDismissRequest = { showModelMenu = false },
            title = { Text("Select OpenRouter Model") },
            text = {
                LazyColumn(modifier = Modifier.height(280.dp)) {
                    items(availableModels) { model ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateActiveChatModel(model.id)
                                    showModelMenu = false
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = model.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = model.id,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (model.isFree) {
                                Surface(
                                    color = CyberTeal.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "FREE",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CyberTeal,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showModelMenu = false }) { Text("Close") }
            }
        )
    }

    // Skill Picker Dialog
    if (showSkillMenu) {
        AlertDialog(
            onDismissRequest = { showSkillMenu = false },
            title = { Text("Bind Skill to Chat") },
            text = {
                LazyColumn(modifier = Modifier.height(280.dp)) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateActiveChatSkill(null)
                                    showSkillMenu = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("No Skill (Standard Mode)", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    items(allSkills) { skill ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateActiveChatSkill(skill.id)
                                    showSkillMenu = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = skill.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = skill.description,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSkillMenu = false }) { Text("Close") }
            }
        )
    }

    // Project Picker Dialog
    if (showProjectMenu) {
        AlertDialog(
            onDismissRequest = { showProjectMenu = false },
            title = { Text("Bind Project to Chat") },
            text = {
                LazyColumn(modifier = Modifier.height(240.dp)) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateActiveChatProject(null)
                                    showProjectMenu = false
                                }
                                .padding(vertical = 10.dp)
                        ) {
                            Text("No Project", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    items(allProjects) { proj ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateActiveChatProject(proj.id)
                                    showProjectMenu = false
                                }
                                .padding(vertical = 10.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = proj.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = proj.description,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showProjectMenu = false }) { Text("Close") }
            }
        )
    }
}

@Composable
fun UserMessageBubble(
    message: MessageEntity,
    onCopy: () -> Unit,
    onEdit: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(14.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = onCopy, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun AssistantMessageBubble(
    message: MessageEntity,
    onCopy: () -> Unit,
    onRetry: () -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
            modifier = Modifier.fillMaxWidth(0.92f)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.SmartToy,
                        contentDescription = "Assistant",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "RouterAgent",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Formatted content with code blocks detection
                RenderFormattedMarkdown(content = message.content)

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onRetry, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Refresh, contentDescription = "Повтор генерации", modifier = Modifier.size(15.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = onCopy, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun RenderFormattedMarkdown(content: String) {
    val context = LocalContext.current
    val parts = content.split("```")

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (i in parts.indices) {
            val segment = parts[i]
            if (i % 2 == 1) {
                // Code block
                val firstLineBreak = segment.indexOf('\n')
                val lang = if (firstLineBreak != -1) segment.substring(0, firstLineBreak).trim() else ""
                val code = if (firstLineBreak != -1) segment.substring(firstLineBreak + 1) else segment

                Surface(
                    color = Color(0xFF090D16),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (lang.isNotBlank()) lang else "code",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyberTeal,
                                fontFamily = FontFamily.Monospace
                            )
                            IconButton(
                                onClick = { copyToClipboard(context, code) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = "Copy Code",
                                    tint = CyberTeal,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = code,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            ),
                            color = Color(0xFFE2E8F0)
                        )
                    }
                }
            } else {
                // Standard text
                if (segment.isNotBlank()) {
                    Text(
                        text = segment.trim(),
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ToolMessageCard(
    message: MessageEntity,
    onApprove: () -> Unit,
    onDeny: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val isPending = message.toolStatus == "pending_approval"
    val isError = message.toolStatus == "error" || message.toolStatus == "rejected"
    val isMcp = message.toolSource == "MCP"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = if (isPending) CyberAmber.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (isPending) CyberAmber else if (isError) CyberRose else if (isMcp) CyberTeal else CyberIndigo
            )
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        if (isPending) Icons.Default.Warning else Icons.Default.Code,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (isPending) CyberAmber else if (isMcp) CyberTeal else CyberIndigo
                    )
                    Text(
                        text = message.toolName ?: "Tool Call",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Source Badge: Local vs MCP
                    Surface(
                        color = if (isMcp) CyberTeal.copy(alpha = 0.2f) else CyberIndigo.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = message.toolSource ?: "Local",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isMcp) CyberTeal else CyberIndigo,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // Status Badge
                    Surface(
                        color = when {
                            isPending -> CyberAmber.copy(alpha = 0.2f)
                            isError -> CyberRose.copy(alpha = 0.2f)
                            else -> CyberTeal.copy(alpha = 0.2f)
                        },
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = message.toolStatus ?: "success",
                            style = MaterialTheme.typography.labelSmall,
                            color = when {
                                isPending -> CyberAmber
                                isError -> CyberRose
                                else -> CyberTeal
                            },
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    IconButton(
                        onClick = { expanded = !expanded },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Toggle Details"
                        )
                    }
                }
            }

            // Dangerous Tool Confirmation Card
            if (isPending) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "⚠️ Dangerous Action Requires User Approval",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = CyberAmber
                )
                Text(
                    text = "The agent proposes to execute '${message.toolName}' with arguments: ${message.toolArgs}",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onApprove,
                        colors = ButtonDefaults.buttonColors(containerColor = CyberTeal)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Approve")
                    }
                    OutlinedButton(
                        onClick = onDeny,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberRose)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Deny")
                    }
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    if (!message.toolArgs.isNullOrBlank()) {
                        Text(
                            text = "Arguments:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            color = Color(0xFF090D16),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = message.toolArgs,
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                modifier = Modifier.padding(8.dp),
                                color = Color(0xFFCBD5E1)
                            )
                        }
                    }

                    if (!message.toolResult.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Result:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            color = Color(0xFF090D16),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = message.toolResult,
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                modifier = Modifier.padding(8.dp),
                                color = if (isError) CyberRose else CyberTeal
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StreamingBubble(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
        modifier = Modifier.fillMaxWidth(0.92f)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Default.SmartToy,
                    contentDescription = "Assistant",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Streaming response...",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = text + " ▍",
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
fun ToolExecutingBanner(toolName: String, source: String, iteration: Int) {
    Surface(
        color = CyberTeal.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(CyberTeal)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LinearProgressIndicator(modifier = Modifier.width(36.dp), color = CyberTeal)
            Text(
                text = "Executing tool '$toolName' (Step $iteration, $source)...",
                style = MaterialTheme.typography.bodySmall,
                color = CyberTeal,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun ErrorBanner(message: String) {
    Surface(
        color = CyberRose.copy(alpha = 0.15f),
        shape = RoundedCornerShape(8.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(CyberRose)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = CyberRose)
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = CyberRose
            )
        }
    }
}

@Composable
fun EmptyChatState(onSelectPrompt: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.AutoAwesome,
            contentDescription = null,
            tint = CyberTeal,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Autonomous Agent Ready",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Ask a question, execute code, or trigger tool-calling agent loops.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))

        val samplePrompts = listOf(
            "Calculate 145 * 28 and find current time in UTC",
            "Search web for latest openrouter free models update",
            "Plan a 3-step refactoring workflow using my active skill"
        )
        for (p in samplePrompts) {
            OutlinedButton(
                onClick = { onSelectPrompt(p) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(p, maxLines = 1, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Copied Text", text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
}
