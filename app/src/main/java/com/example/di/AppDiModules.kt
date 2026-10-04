package com.example.di

import android.content.Context
import com.example.data.agent.AgentLoopCoordinator
import com.example.data.local.AppDatabase
import com.example.data.local.dao.ChatDao
import com.example.data.local.dao.McpServerDao
import com.example.data.local.dao.MessageDao
import com.example.data.local.dao.ProjectDao
import com.example.data.local.dao.SkillDao
import com.example.data.mcp.McpClient
import com.example.data.preferences.SettingsRepository
import com.example.data.remote.github.GitHubSkillImporter
import com.example.data.remote.openrouter.OpenRouterClient
import com.example.data.repository.ChatRepository
import com.example.data.repository.McpRepository
import com.example.data.repository.ProjectRepository
import com.example.data.repository.SkillRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Dependency Injection container and module providers.
 * Defines object graph for Database, Network, Security, Repositories, and Agent Loop Coordinator.
 */

@Singleton
class DatabaseModule @Inject constructor(private val context: Context) {
    val database: AppDatabase by lazy { AppDatabase.getInstance(context) }
    val chatDao: ChatDao by lazy { database.chatDao() }
    val messageDao: MessageDao by lazy { database.messageDao() }
    val projectDao: ProjectDao by lazy { database.projectDao() }
    val skillDao: SkillDao by lazy { database.skillDao() }
    val mcpServerDao: McpServerDao by lazy { database.mcpServerDao() }
}

@Singleton
class NetworkModule @Inject constructor() {
    val openRouterClient: OpenRouterClient by lazy { OpenRouterClient() }
    val mcpClient: McpClient by lazy { McpClient() }
    val githubSkillImporter: GitHubSkillImporter by lazy { GitHubSkillImporter() }
}

@Singleton
class SecurityModule @Inject constructor(private val context: Context) {
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(context) }
}

@Singleton
class RepositoryModule @Inject constructor(
    private val databaseModule: DatabaseModule,
    private val networkModule: NetworkModule,
    private val securityModule: SecurityModule
) {
    val chatRepository: ChatRepository by lazy {
        ChatRepository(databaseModule.chatDao, databaseModule.messageDao)
    }

    val projectRepository: ProjectRepository by lazy {
        ProjectRepository(databaseModule.projectDao)
    }

    val skillRepository: SkillRepository by lazy {
        SkillRepository(databaseModule.skillDao, networkModule.githubSkillImporter)
    }

    val mcpRepository: McpRepository by lazy {
        McpRepository(databaseModule.mcpServerDao, networkModule.mcpClient)
    }
}

@Singleton
class FirestoreModule @Inject constructor(
    private val databaseModule: DatabaseModule
) {
    val firestoreSyncManager: com.example.data.remote.firestore.FirestoreSyncManager by lazy {
        com.example.data.remote.firestore.FirestoreSyncManager(
            chatDao = databaseModule.chatDao,
            messageDao = databaseModule.messageDao,
            projectDao = databaseModule.projectDao,
            skillDao = databaseModule.skillDao,
            mcpServerDao = databaseModule.mcpServerDao
        )
    }
}

@Singleton
class AgentModule @Inject constructor(
    private val databaseModule: DatabaseModule,
    private val networkModule: NetworkModule,
    private val securityModule: SecurityModule
) {
    val agentLoopCoordinator: AgentLoopCoordinator by lazy {
        AgentLoopCoordinator(
            openRouterClient = networkModule.openRouterClient,
            mcpClient = networkModule.mcpClient,
            settingsRepository = securityModule.settingsRepository,
            chatDao = databaseModule.chatDao,
            messageDao = databaseModule.messageDao,
            projectDao = databaseModule.projectDao,
            skillDao = databaseModule.skillDao
        )
    }
}
