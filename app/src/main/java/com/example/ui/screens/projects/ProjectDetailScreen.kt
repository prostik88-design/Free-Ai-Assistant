package com.example.ui.screens.projects

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.ProjectEntity
import com.example.ui.theme.CyberTeal
import com.example.ui.viewmodels.AppViewModel
import org.json.JSONArray
import org.json.JSONObject

data class ContextFile(
    val name: String,
    val content: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailScreen(
    projectId: String,
    viewModel: AppViewModel,
    onNavigateBack: () -> Unit,
    onOpenChat: (String) -> Unit
) {
    val allProjects by viewModel.allProjects.collectAsState()
    val allChats by viewModel.allChats.collectAsState()
    val allSkills by viewModel.allSkills.collectAsState()

    val project = allProjects.firstOrNull { it.id == projectId }

    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddFileDialog by remember { mutableStateOf(false) }
    var newFileName by remember { mutableStateOf("") }
    var newFileContent by remember { mutableStateOf("") }

    // Parse files from JSON
    val filesList = remember(project?.contextFilesJson) {
        val list = mutableListOf<ContextFile>()
        try {
            val jsonStr = project?.contextFilesJson.orEmpty()
            if (jsonStr.startsWith("[")) {
                val arr = JSONArray(jsonStr)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(ContextFile(obj.optString("name", "file_$i"), obj.optString("content", "")))
                }
            } else if (jsonStr.isNotBlank()) {
                list.add(ContextFile("scratchpad.txt", jsonStr))
            }
        } catch (e: Exception) {
            if (!project?.contextFilesJson.isNullOrBlank()) {
                list.add(ContextFile("notes.txt", project?.contextFilesJson!!))
            }
        }
        list
    }

    val projectChats = allChats.filter { it.projectId == projectId }

    if (project == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Проект не найден")
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = project.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Рабочее пространство проекта",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = {
                        newFileName = ""
                        newFileContent = ""
                        showAddFileDialog = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("add_project_file_fab")
                ) {
                    Icon(Icons.Default.NoteAdd, contentDescription = "Добавить файл")
                }
            } else if (selectedTab == 1) {
                FloatingActionButton(
                    onClick = {
                        viewModel.createNewChat { newId ->
                            viewModel.selectChat(newId)
                            viewModel.updateActiveChatProject(projectId)
                            onOpenChat(newId)
                        }
                    },
                    containerColor = CyberTeal,
                    modifier = Modifier.testTag("new_project_chat_fab")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Новый диалог в проекте")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Файлы контекста (${filesList.size})") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Диалоги (${projectChats.size})") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Инструкции") }
                )
            }

            when (selectedTab) {
                0 -> {
                    // TAB 1: FILES LIST
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Text(
                                text = "Файлы и заметки проекта (автоматически передаются в контекст агента):",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (filesList.isEmpty()) {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 40.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Default.Description,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text("Нет добавленных файлов", style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        "Нажмите + чтобы прикрепить спецификацию, схему или текст",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            items(filesList) { file ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(Icons.Default.Description, contentDescription = null, tint = CyberTeal)
                                                Text(file.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                            }
                                            IconButton(
                                                onClick = {
                                                    val updated = filesList.filter { it.name != file.name }
                                                    val jsonArr = JSONArray()
                                                    for (f in updated) {
                                                        jsonArr.put(JSONObject().apply {
                                                            put("name", f.name)
                                                            put("content", f.content)
                                                        })
                                                    }
                                                    viewModel.createProject(
                                                        name = project.name,
                                                        desc = project.description,
                                                        systemPrompt = project.systemPrompt,
                                                        filesJson = jsonArr.toString()
                                                    )
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))
                                        Surface(
                                            color = MaterialTheme.colorScheme.surface,
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = file.content,
                                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                                modifier = Modifier.padding(8.dp),
                                                maxLines = 6
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // TAB 2: PROJECT CHATS
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Text(
                                text = "Диалоги с привязкой к контексту этого проекта:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (projectChats.isEmpty()) {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 40.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Chat,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text("В этом проекте пока нет диалогов", style = MaterialTheme.typography.bodyMedium)
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(
                                        onClick = {
                                            viewModel.createNewChat { newId ->
                                                viewModel.selectChat(newId)
                                                viewModel.updateActiveChatProject(projectId)
                                                onOpenChat(newId)
                                            }
                                        }
                                    ) {
                                        Text("Начать диалог")
                                    }
                                }
                            }
                        } else {
                            items(projectChats) { chat ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.selectChat(chat.id)
                                            onOpenChat(chat.id)
                                        },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(chat.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                            Text(chat.modelId, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                        }
                                        Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // TAB 3: SYSTEM PROMPT & INFO
                    var promptText by remember { mutableStateOf(project.systemPrompt) }
                    var descText by remember { mutableStateOf(project.description) }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            OutlinedTextField(
                                value = descText,
                                onValueChange = { descText = it },
                                label = { Text("Описание проекта") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = promptText,
                                onValueChange = { promptText = it },
                                label = { Text("Системный промпт / Инструкции для агента") },
                                maxLines = 8,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        item {
                            Button(
                                onClick = {
                                    viewModel.createProject(
                                        name = project.name,
                                        desc = descText.trim(),
                                        systemPrompt = promptText.trim(),
                                        filesJson = project.contextFilesJson
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Сохранить инструкции проекта")
                            }
                        }
                    }
                }
            }
        }
    }

    // Add File Dialog
    if (showAddFileDialog) {
        AlertDialog(
            onDismissRequest = { showAddFileDialog = false },
            title = { Text("Добавить файл контекста") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newFileName,
                        onValueChange = { newFileName = it },
                        label = { Text("Имя файла") },
                        placeholder = { Text("spec.md или architecture.txt") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newFileContent,
                        onValueChange = { newFileContent = it },
                        label = { Text("Содержимое файла / Данные") },
                        maxLines = 6,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newFileName.isNotBlank()) {
                            val updated = filesList.toMutableList()
                            updated.add(ContextFile(newFileName.trim(), newFileContent.trim()))
                            val jsonArr = JSONArray()
                            for (f in updated) {
                                jsonArr.put(JSONObject().apply {
                                    put("name", f.name)
                                    put("content", f.content)
                                })
                            }
                            viewModel.createProject(
                                name = project.name,
                                desc = project.description,
                                systemPrompt = project.systemPrompt,
                                filesJson = jsonArr.toString()
                            )
                            showAddFileDialog = false
                        }
                    },
                    enabled = newFileName.isNotBlank()
                ) {
                    Text("Добавить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddFileDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}
