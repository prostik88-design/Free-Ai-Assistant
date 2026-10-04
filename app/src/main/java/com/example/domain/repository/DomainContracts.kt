package com.example.domain.repository

import com.example.data.local.entity.ChatEntity
import com.example.data.local.entity.McpServerEntity
import com.example.data.local.entity.MessageEntity
import com.example.data.local.entity.ProjectEntity
import com.example.data.local.entity.SkillEntity
import com.example.data.mcp.McpToolDefinition
import com.example.data.remote.github.GitHubImportResult
import kotlinx.coroutines.flow.Flow

/**
 * Domain Contracts and Abstractions for Clean Architecture.
 */

interface IChatRepository {
    val allChats: Flow<List<ChatEntity>>
    fun getChat(chatId: String): Flow<ChatEntity?>
    fun getMessagesForChat(chatId: String): Flow<List<MessageEntity>>
    suspend fun createChat(
        title: String = "New Chat",
        modelId: String = "openrouter/free",
        activeSkillId: String? = null,
        projectId: String? = null,
        isAgentic: Boolean = true
    ): String
    suspend fun updateChat(chat: ChatEntity)
    suspend fun renameChat(chatId: String, newTitle: String)
    suspend fun deleteChat(chatId: String)
    suspend fun clearAllHistory()
}

interface IProjectRepository {
    val allProjects: Flow<List<ProjectEntity>>
    fun getProject(id: String): Flow<ProjectEntity?>
    suspend fun createProject(
        name: String,
        description: String = "",
        systemPrompt: String = "",
        contextFiles: String = "[]"
    ): String
    suspend fun updateProject(project: ProjectEntity)
    suspend fun deleteProject(id: String)
}

interface ISkillRepository {
    val allSkills: Flow<List<SkillEntity>>
    suspend fun ensureBuiltInSkillsSeeded()
    suspend fun importFromUrl(url: String): GitHubImportResult
    suspend fun saveSkill(
        name: String,
        description: String,
        systemPrompt: String,
        toolsAllowed: List<String> = emptyList(),
        inputSchema: String? = null,
        outputFormat: String? = null,
        sourceUrl: String? = null
    ): String
    suspend fun deleteSkill(id: String)
}

interface IMcpRepository {
    val allServers: Flow<List<McpServerEntity>>
    suspend fun addServer(
        name: String,
        serverUrl: String,
        transportType: String = "HTTP",
        authHeader: String? = null
    ): String
    suspend fun syncTools(serverId: String): List<McpToolDefinition>
    suspend fun toggleServer(serverId: String, enabled: Boolean)
    suspend fun deleteServer(serverId: String)
}
