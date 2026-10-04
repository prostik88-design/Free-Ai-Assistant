package com.example.ui.screens.skills

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.SkillEntity
import com.example.data.remote.github.GitHubImportResult
import com.example.ui.theme.CyberIndigo
import com.example.ui.theme.CyberRose
import com.example.ui.theme.CyberTeal
import com.example.ui.viewmodels.AppViewModel
import org.json.JSONArray

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SkillsScreen(
    viewModel: AppViewModel,
    onBindSkillToChat: (String) -> Unit
) {
    val skills by viewModel.allSkills.collectAsState()
    val importResult by viewModel.importResult.collectAsState()
    val isImporting by viewModel.isImporting.collectAsState()

    var selectedTabIndex by remember { mutableStateOf(0) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var importUrlInput by remember { mutableStateOf("") }

    // Create Skill form state
    var customName by remember { mutableStateOf("") }
    var customDesc by remember { mutableStateOf("") }
    var customPrompt by remember { mutableStateOf("") }
    var customTools by remember { mutableStateOf("calculate, web_search") }

    val filteredSkills = when (selectedTabIndex) {
        1 -> skills.filter { it.isBuiltIn }
        2 -> skills.filter { !it.isBuiltIn }
        else -> skills
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Skills & Plugins",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Equip the AI agent with domain skills, system prompts, and tool access boundaries.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            importUrlInput = ""
                            viewModel.clearImportPreview()
                            showImportDialog = true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("import_skill_button"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Import GitHub")
                    }

                    OutlinedButton(
                        onClick = {
                            customName = ""
                            customDesc = ""
                            customPrompt = ""
                            customTools = "calculate, web_search"
                            showCreateDialog = true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("create_skill_button"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Create Skill")
                    }
                }
            }

            TabRow(selectedTabIndex = selectedTabIndex) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("All (${skills.size})") }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("Built-In") }
                )
                Tab(
                    selected = selectedTabIndex == 2,
                    onClick = { selectedTabIndex = 2 },
                    text = { Text("Custom") }
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredSkills, key = { it.id }) { skill ->
                    SkillCardItem(
                        skill = skill,
                        onUseInChat = {
                            viewModel.createNewChat { newId ->
                                viewModel.selectChat(newId)
                                viewModel.updateActiveChatSkill(skill.id)
                                onBindSkillToChat(newId)
                            }
                        },
                        onDelete = { viewModel.deleteSkill(skill.id) }
                    )
                }
            }
        }
    }

    // GitHub Import Dialog
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = {
                showImportDialog = false
                viewModel.clearImportPreview()
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Import Skill from GitHub")
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Enter a GitHub repo URL, tree path, or raw SKILL.md/YAML link:",
                        style = MaterialTheme.typography.bodySmall
                    )

                    OutlinedTextField(
                        value = importUrlInput,
                        onValueChange = { importUrlInput = it },
                        placeholder = { Text("https://github.com/user/repo") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("github_url_input")
                    )

                    // Quick Preset Buttons
                    Text("Presets:", style = MaterialTheme.typography.labelSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilterChip(
                            selected = false,
                            onClick = { importUrlInput = "https://github.com/DietrichGebert/ponytail.git" },
                            label = { Text("ponytail", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = false,
                            onClick = { importUrlInput = "https://github.com/anthropics/skills.git" },
                            label = { Text("anthropics", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = false,
                            onClick = { importUrlInput = "https://github.com/obra/superpowers.git" },
                            label = { Text("superpowers", fontSize = 11.sp) }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = {
                                if (importUrlInput.isNotBlank()) {
                                    viewModel.importSkillFromGitHub(importUrlInput.trim())
                                }
                            },
                            enabled = importUrlInput.isNotBlank() && !isImporting,
                            modifier = Modifier.testTag("fetch_skill_button")
                        ) {
                            if (isImporting) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Parsing...")
                            } else {
                                Text("Fetch & Validate")
                            }
                        }
                    }

                    // Preview Area
                    when (val res = importResult) {
                        is GitHubImportResult.Preview -> {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Security, contentDescription = null, tint = CyberTeal, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Safe Declarative Skill Preview", style = MaterialTheme.typography.labelSmall, color = CyberTeal)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(res.parsedSkill.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text(res.parsedSkill.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    if (res.parsedSkill.toolsAllowed.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Allowed Tools: ${res.parsedSkill.toolsAllowed.joinToString()}", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                        is GitHubImportResult.Error -> {
                            Surface(
                                color = CyberRose.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = CyberRose, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(res.message, style = MaterialTheme.typography.bodySmall, color = CyberRose)
                                }
                            }
                        }
                        null -> {}
                    }
                }
            },
            confirmButton = {
                val preview = importResult as? GitHubImportResult.Preview
                if (preview != null) {
                    Button(
                        onClick = {
                            viewModel.confirmImportSkill(preview) {
                                showImportDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberTeal),
                        modifier = Modifier.testTag("confirm_import_button")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Confirm & Save")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showImportDialog = false
                    viewModel.clearImportPreview()
                }) {
                    Text("Close")
                }
            }
        )
    }

    // Create Skill Dialog
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create Skill from Template") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Template Selector Chips
                    Text("Choose Template:", style = MaterialTheme.typography.labelSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilterChip(
                            selected = false,
                            onClick = {
                                customName = "Code Reviewer Pro"
                                customDesc = "Inspects code patterns and suggests security fixes"
                                customPrompt = "You are a senior software security auditor. Review code line by line and find vulnerabilities."
                                customTools = "calculate"
                            },
                            label = { Text("Coder", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = false,
                            onClick = {
                                customName = "Data Analyst"
                                customDesc = "Performs mathematical analysis and structured summary"
                                customPrompt = "You are a quantitative data analyst. Always express conclusions in concise tabular form."
                                customTools = "calculate, get_current_time"
                            },
                            label = { Text("Analyst", fontSize = 11.sp) }
                        )
                    }

                    OutlinedTextField(
                        value = customName,
                        onValueChange = { customName = it },
                        label = { Text("Skill Name *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = customDesc,
                        onValueChange = { customDesc = it },
                        label = { Text("Description *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = customPrompt,
                        onValueChange = { customPrompt = it },
                        label = { Text("System Prompt Guidance") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = customTools,
                        onValueChange = { customTools = it },
                        label = { Text("Allowed Tools (comma-separated)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (customName.isNotBlank() && customDesc.isNotBlank()) {
                            val toolsList = customTools.split(",")
                                .map { it.trim() }
                                .filter { it.isNotBlank() }
                            viewModel.createCustomSkill(
                                name = customName.trim(),
                                description = customDesc.trim(),
                                prompt = customPrompt.trim(),
                                tools = toolsList
                            )
                            showCreateDialog = false
                        }
                    },
                    enabled = customName.isNotBlank() && customDesc.isNotBlank()
                ) {
                    Text("Save Skill")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SkillCardItem(
    skill: SkillEntity,
    onUseInChat: () -> Unit,
    onDelete: () -> Unit
) {
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Extension,
                        contentDescription = null,
                        tint = if (skill.isBuiltIn) CyberIndigo else CyberTeal,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = skill.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = if (skill.isBuiltIn) CyberIndigo.copy(alpha = 0.2f) else CyberTeal.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = if (skill.isBuiltIn) "BUILT-IN" else "CUSTOM",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (skill.isBuiltIn) CyberIndigo else CyberTeal,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (!skill.isBuiltIn) {
                        IconButton(onClick = onDelete) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = skill.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Allowed tools chips
            val tools = try {
                val arr = JSONArray(skill.toolsAllowedJson)
                val list = mutableListOf<String>()
                for (i in 0 until arr.length()) list.add(arr.getString(i))
                list
            } catch (e: Exception) {
                emptyList<String>()
            }

            if (tools.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (t in tools) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = t,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onUseInChat,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Chat with this Skill")
                }
            }
        }
    }
}
