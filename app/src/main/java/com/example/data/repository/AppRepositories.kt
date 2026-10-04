package com.example.data.repository

import com.example.data.local.dao.ChatDao
import com.example.data.local.dao.McpServerDao
import com.example.data.local.dao.MessageDao
import com.example.data.local.dao.ProjectDao
import com.example.data.local.dao.SkillDao
import com.example.data.local.entity.ChatEntity
import com.example.data.local.entity.McpServerEntity
import com.example.data.local.entity.MessageEntity
import com.example.data.local.entity.ProjectEntity
import com.example.data.local.entity.SkillEntity
import com.example.data.mcp.McpClient
import com.example.data.mcp.McpToolDefinition
import com.example.data.parser.SkillParser
import com.example.data.remote.github.GitHubImportResult
import com.example.data.remote.github.GitHubSkillImporter
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class ChatRepository(
    private val chatDao: ChatDao,
    private val messageDao: MessageDao
) {
    val allChats: Flow<List<ChatEntity>> = chatDao.getAllChats()

    fun getChat(chatId: String): Flow<ChatEntity?> = chatDao.getChatById(chatId)

    fun getMessagesForChat(chatId: String): Flow<List<MessageEntity>> =
        messageDao.getMessagesForChat(chatId)

    suspend fun createChat(
        title: String = "New Chat",
        modelId: String = "openrouter/free",
        activeSkillId: String? = null,
        projectId: String? = null,
        isAgentic: Boolean = true
    ): String {
        val chat = ChatEntity(
            id = UUID.randomUUID().toString(),
            title = title,
            modelId = modelId,
            activeSkillId = activeSkillId,
            projectId = projectId,
            isAgentic = isAgentic
        )
        chatDao.insertChat(chat)
        return chat.id
    }

    suspend fun updateChat(chat: ChatEntity) = chatDao.updateChat(chat)

    suspend fun renameChat(chatId: String, newTitle: String) {
        val chat = chatDao.getChatByIdOnce(chatId)
        if (chat != null) {
            chatDao.updateChat(chat.copy(title = newTitle, updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun deleteChat(chatId: String) {
        messageDao.deleteMessagesForChat(chatId)
        chatDao.deleteChatById(chatId)
    }

    suspend fun clearAllHistory() {
        messageDao.deleteAllMessages()
        chatDao.deleteAllChats()
    }
}

class ProjectRepository(private val projectDao: ProjectDao) {
    val allProjects: Flow<List<ProjectEntity>> = projectDao.getAllProjects()

    fun getProject(id: String): Flow<ProjectEntity?> = projectDao.getProjectById(id)

    suspend fun createProject(
        name: String,
        description: String = "",
        systemPrompt: String = "",
        contextFiles: String = "[]"
    ): String {
        val project = ProjectEntity(
            id = UUID.randomUUID().toString(),
            name = name,
            description = description,
            systemPrompt = systemPrompt,
            contextFilesJson = contextFiles
        )
        projectDao.insertProject(project)
        return project.id
    }

    suspend fun updateProject(project: ProjectEntity) {
        projectDao.updateProject(project.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteProject(id: String) = projectDao.deleteProjectById(id)
}

class SkillRepository(
    private val skillDao: SkillDao,
    private val githubImporter: GitHubSkillImporter = GitHubSkillImporter()
) {
    val allSkills: Flow<List<SkillEntity>> = skillDao.getAllSkills()

    suspend fun ensureBuiltInSkillsSeeded() {
        if (skillDao.getSkillCount() == 0) {
            skillDao.insertSkills(PreloadedSkills.getBuiltInSkills())
        }
    }

    suspend fun importFromUrl(url: String): GitHubImportResult {
        return githubImporter.fetchSkillFromUrl(url)
    }

    suspend fun saveSkill(
        name: String,
        description: String,
        systemPrompt: String,
        toolsAllowed: List<String> = emptyList(),
        inputSchema: String? = null,
        outputFormat: String? = null,
        sourceUrl: String? = null
    ): String {
        val toolsJson = JSONArray(toolsAllowed).toString()
        val skill = SkillEntity(
            id = UUID.randomUUID().toString(),
            name = name,
            description = description,
            systemPrompt = systemPrompt,
            inputSchema = inputSchema,
            toolsAllowedJson = toolsJson,
            outputFormat = outputFormat,
            sourceUrl = sourceUrl,
            isBuiltIn = false
        )
        skillDao.insertSkill(skill)
        return skill.id
    }

    suspend fun deleteSkill(id: String) = skillDao.deleteSkillById(id)
}

class McpRepository(
    private val mcpServerDao: McpServerDao,
    private val mcpClient: McpClient
) {
    val allServers: Flow<List<McpServerEntity>> = mcpServerDao.getAllServers()

    suspend fun addServer(
        name: String,
        serverUrl: String,
        transportType: String = "HTTP",
        authHeader: String? = null
    ): String {
        val tools = mcpClient.discoverTools(serverUrl, authHeader)
        val toolsJson = JSONArray().apply {
            for (t in tools) put(t.toJson())
        }.toString()

        val server = McpServerEntity(
            id = UUID.randomUUID().toString(),
            name = name,
            serverUrl = serverUrl,
            transportType = transportType,
            authHeader = authHeader,
            isEnabled = true,
            toolsJson = toolsJson
        )
        mcpServerDao.insertServer(server)
        return server.id
    }

    suspend fun syncTools(serverId: String): List<McpToolDefinition> {
        val server = mcpServerDao.getServerById(serverId) ?: return emptyList()
        val discovered = mcpClient.discoverTools(server.serverUrl, server.authHeader)
        val jsonArray = JSONArray().apply {
            for (t in discovered) put(t.toJson())
        }
        mcpServerDao.updateServer(server.copy(toolsJson = jsonArray.toString()))
        return discovered
    }

    suspend fun toggleServer(serverId: String, enabled: Boolean) {
        val server = mcpServerDao.getServerById(serverId) ?: return
        mcpServerDao.updateServer(server.copy(isEnabled = enabled))
    }

    suspend fun deleteServer(serverId: String) = mcpServerDao.deleteServerById(serverId)
}
