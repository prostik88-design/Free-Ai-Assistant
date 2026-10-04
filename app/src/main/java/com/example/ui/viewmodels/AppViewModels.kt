package com.example.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.agent.AgentExecutionState
import com.example.data.agent.AgentLoopCoordinator
import com.example.data.local.AppDatabase
import com.example.data.local.entity.ChatEntity
import com.example.data.local.entity.MessageEntity
import com.example.data.local.entity.ProjectEntity
import com.example.data.local.entity.SkillEntity
import com.example.data.mcp.McpClient
import com.example.data.mcp.McpToolDefinition
import com.example.data.preferences.SettingsRepository
import com.example.data.preferences.UserSettings
import com.example.data.remote.github.GitHubImportResult
import com.example.data.remote.openrouter.KeyValidationResult
import com.example.data.remote.openrouter.ModelOption
import com.example.data.remote.openrouter.OpenRouterClient
import com.example.data.repository.ChatRepository
import com.example.data.repository.McpRepository
import com.example.data.repository.ProjectRepository
import com.example.data.repository.SkillRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getInstance(application)
    val settingsRepository = SettingsRepository(application)
    private val openRouterClient = OpenRouterClient()
    private val mcpClient = McpClient()

    val chatRepository = ChatRepository(database.chatDao(), database.messageDao())
    val projectRepository = ProjectRepository(database.projectDao())
    val skillRepository = SkillRepository(database.skillDao())
    val mcpRepository = McpRepository(database.mcpServerDao(), mcpClient)

    val agentCoordinator = AgentLoopCoordinator(
        openRouterClient = openRouterClient,
        mcpClient = mcpClient,
        settingsRepository = settingsRepository,
        chatDao = database.chatDao(),
        messageDao = database.messageDao(),
        projectDao = database.projectDao(),
        skillDao = database.skillDao()
    )

    // Flow states
    val userSettings: StateFlow<UserSettings> = settingsRepository.settingsFlow

    val allChats: StateFlow<List<ChatEntity>> = chatRepository.allChats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allProjects: StateFlow<List<ProjectEntity>> = projectRepository.allProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSkills: StateFlow<List<SkillEntity>> = skillRepository.allSkills
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMcpServers = mcpRepository.allServers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Chat State
    private val _activeChatId = MutableStateFlow<String?>(null)
    val activeChatId: StateFlow<String?> = _activeChatId.asStateFlow()

    private val _activeChatMessages = MutableStateFlow<List<MessageEntity>>(emptyList())
    val activeChatMessages: StateFlow<List<MessageEntity>> = _activeChatMessages.asStateFlow()

    private val _activeChat = MutableStateFlow<ChatEntity?>(null)
    val activeChat: StateFlow<ChatEntity?> = _activeChat.asStateFlow()

    val agentState: StateFlow<AgentExecutionState> = agentCoordinator.agentState

    // API Key Validation State
    private val _keyValidation = MutableStateFlow<KeyValidationResult?>(null)
    val keyValidation: StateFlow<KeyValidationResult?> = _keyValidation.asStateFlow()

    private val _isValidatingKey = MutableStateFlow(false)
    val isValidatingKey: StateFlow<Boolean> = _isValidatingKey.asStateFlow()

    // GitHub Import State
    private val _importResult = MutableStateFlow<GitHubImportResult?>(null)
    val importResult: StateFlow<GitHubImportResult?> = _importResult.asStateFlow()

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    // Available models
    private val _availableModels = MutableStateFlow(OpenRouterClient.DEFAULT_FREE_MODELS)
    val availableModels: StateFlow<List<ModelOption>> = _availableModels.asStateFlow()

    private var activeMessagesJob: Job? = null
    private var activeChatJob: Job? = null
    private var currentExecutionJob: Job? = null

    init {
        viewModelScope.launch {
            skillRepository.ensureBuiltInSkillsSeeded()
        }
    }

    fun selectChat(chatId: String) {
        _activeChatId.value = chatId
        activeMessagesJob?.cancel()
        activeMessagesJob = viewModelScope.launch {
            chatRepository.getMessagesForChat(chatId).collect { msgs ->
                _activeChatMessages.value = msgs
            }
        }
        activeChatJob?.cancel()
        activeChatJob = viewModelScope.launch {
            chatRepository.getChat(chatId).collect { c ->
                _activeChat.value = c
            }
        }
    }

    fun createNewChat(onCreated: (String) -> Unit = {}) {
        viewModelScope.launch {
            val model = if (userSettings.value.isOpenRouterFree) "openrouter/free" else userSettings.value.selectedModel
            val id = chatRepository.createChat(
                title = "New Conversation",
                modelId = model,
                isAgentic = true
            )
            selectChat(id)
            onCreated(id)
        }
    }

    fun renameChat(chatId: String, newTitle: String) {
        viewModelScope.launch {
            chatRepository.renameChat(chatId, newTitle)
        }
    }

    fun deleteChat(chatId: String) {
        viewModelScope.launch {
            chatRepository.deleteChat(chatId)
            if (_activeChatId.value == chatId) {
                _activeChatId.value = null
                _activeChatMessages.value = emptyList()
                _activeChat.value = null
            }
        }
    }

    fun updateActiveChatModel(modelId: String) {
        val chat = _activeChat.value ?: return
        viewModelScope.launch {
            chatRepository.updateChat(chat.copy(modelId = modelId))
        }
    }

    fun updateActiveChatSkill(skillId: String?) {
        val chat = _activeChat.value ?: return
        viewModelScope.launch {
            chatRepository.updateChat(chat.copy(activeSkillId = skillId))
        }
    }

    fun updateActiveChatProject(projectId: String?) {
        val chat = _activeChat.value ?: return
        viewModelScope.launch {
            chatRepository.updateChat(chat.copy(projectId = projectId))
        }
    }

    fun toggleActiveChatAgentic(isAgentic: Boolean) {
        val chat = _activeChat.value ?: return
        viewModelScope.launch {
            chatRepository.updateChat(chat.copy(isAgentic = isAgentic))
        }
    }

    fun sendMessage(chatId: String, text: String) {
        if (text.isBlank()) return
        currentExecutionJob?.cancel()
        currentExecutionJob = viewModelScope.launch {
            agentCoordinator.executeTask(chatId, text.trim(), currentExecutionJob)
        }
    }

    fun cancelStreaming() {
        currentExecutionJob?.cancel()
        currentExecutionJob = null
        agentCoordinator.cancelExecution()
    }

    fun approveDangerousAction(messageId: String) {
        viewModelScope.launch {
            agentCoordinator.resolvePendingApproval(messageId, approved = true)
        }
    }

    fun denyDangerousAction(messageId: String) {
        viewModelScope.launch {
            agentCoordinator.resolvePendingApproval(messageId, approved = false)
        }
    }

    // Projects
    fun createProject(name: String, desc: String, systemPrompt: String, filesJson: String) {
        viewModelScope.launch {
            projectRepository.createProject(name, desc, systemPrompt, filesJson)
        }
    }

    fun deleteProject(id: String) {
        viewModelScope.launch {
            projectRepository.deleteProject(id)
        }
    }

    // Skills
    fun importSkillFromGitHub(url: String) {
        _isImporting.value = true
        _importResult.value = null
        viewModelScope.launch {
            val result = skillRepository.importFromUrl(url)
            _importResult.value = result
            _isImporting.value = false
        }
    }

    fun confirmImportSkill(preview: GitHubImportResult.Preview, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            skillRepository.saveSkill(
                name = preview.parsedSkill.name,
                description = preview.parsedSkill.description,
                systemPrompt = preview.parsedSkill.systemPrompt,
                toolsAllowed = preview.parsedSkill.toolsAllowed,
                inputSchema = preview.parsedSkill.inputSchema,
                outputFormat = preview.parsedSkill.outputFormat,
                sourceUrl = preview.sourceUrl
            )
            _importResult.value = null
            onSaved()
        }
    }

    fun clearImportPreview() {
        _importResult.value = null
    }

    fun createCustomSkill(
        name: String,
        description: String,
        prompt: String,
        tools: List<String>
    ) {
        viewModelScope.launch {
            skillRepository.saveSkill(name, description, prompt, tools)
        }
    }

    fun deleteSkill(id: String) {
        viewModelScope.launch {
            skillRepository.deleteSkill(id)
        }
    }

    // MCP
    fun addMcpServer(name: String, url: String, transport: String, auth: String?) {
        viewModelScope.launch {
            mcpRepository.addServer(name, url, transport, auth)
        }
    }

    fun syncMcpServerTools(serverId: String) {
        viewModelScope.launch {
            mcpRepository.syncTools(serverId)
        }
    }

    fun toggleMcpServer(serverId: String, enabled: Boolean) {
        viewModelScope.launch {
            mcpRepository.toggleServer(serverId, enabled)
        }
    }

    fun deleteMcpServer(serverId: String) {
        viewModelScope.launch {
            mcpRepository.deleteServer(serverId)
        }
    }

    // Settings
    fun saveApiKey(rawKey: String) {
        settingsRepository.saveApiKey(rawKey)
        validateApiKey(rawKey)
    }

    fun setLanguage(lang: String) {
        settingsRepository.setLanguage(lang)
    }

    fun validateApiKey(keyToTest: String? = null) {
        val key = keyToTest ?: settingsRepository.getDecryptedApiKey()
        _isValidatingKey.value = true
        _keyValidation.value = null
        viewModelScope.launch {
            val result = openRouterClient.validateApiKey(key)
            _keyValidation.value = result
            _isValidatingKey.value = false
            if (result.isValid) {
                val fetched = openRouterClient.fetchModels(key)
                _availableModels.value = fetched
            }
        }
    }

    fun exportAllDataJson(): String {
        val root = JSONObject()
        val chats = allChats.value
        val projects = allProjects.value
        val skills = allSkills.value
        val servers = allMcpServers.value

        val chatsArr = JSONArray()
        for (c in chats) {
            chatsArr.put(JSONObject().apply {
                put("id", c.id)
                put("title", c.title)
                put("modelId", c.modelId)
            })
        }
        root.put("chats", chatsArr)

        val projArr = JSONArray()
        for (p in projects) {
            projArr.put(JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("description", p.description)
            })
        }
        root.put("projects", projArr)

        val skillsArr = JSONArray()
        for (s in skills) {
            skillsArr.put(JSONObject().apply {
                put("id", s.id)
                put("name", s.name)
                put("description", s.description)
            })
        }
        root.put("skills", skillsArr)

        return root.toString(2)
    }

    fun clearLocalHistory() {
        viewModelScope.launch {
            chatRepository.clearAllHistory()
            _activeChatId.value = null
            _activeChatMessages.value = emptyList()
            _activeChat.value = null
        }
    }

    fun deleteAllData() {
        viewModelScope.launch {
            chatRepository.clearAllHistory()
            database.projectDao().deleteAllProjects()
            database.skillDao().deleteAllSkills()
            database.mcpServerDao().deleteAllServers()
            settingsRepository.clearAllPreferences()
            skillRepository.ensureBuiltInSkillsSeeded()
            _activeChatId.value = null
            _activeChatMessages.value = emptyList()
            _activeChat.value = null
        }
    }
}
