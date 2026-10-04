package com.example.data.remote.firestore

import android.util.Log
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
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

sealed interface FirestoreSyncState {
    object Idle : FirestoreSyncState
    object Syncing : FirestoreSyncState
    data class Success(val message: String, val timestamp: Long = System.currentTimeMillis()) : FirestoreSyncState
    data class Error(val error: String) : FirestoreSyncState
}

@Singleton
class FirestoreSyncManager @Inject constructor(
    private val chatDao: ChatDao,
    private val messageDao: MessageDao,
    private val projectDao: ProjectDao,
    private val skillDao: SkillDao,
    private val mcpServerDao: McpServerDao
) {
    private val _syncState = MutableStateFlow<FirestoreSyncState>(FirestoreSyncState.Idle)
    val syncState: StateFlow<FirestoreSyncState> = _syncState.asStateFlow()

    private val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }

    /**
     * Backup all local data (Projects, Files, Chats, Skills, MCP Servers) to Cloud Firestore.
     */
    suspend fun backupAllToCloud(userId: String = "default_user"): Result<String> = withContext(Dispatchers.IO) {
        _syncState.value = FirestoreSyncState.Syncing
        try {
            val userDoc = firestore.collection("users").document(userId)

            // 1. Sync Projects
            val projects = projectDao.getAllProjectsList()
            for (p in projects) {
                val projData = hashMapOf(
                    "id" to p.id,
                    "name" to p.name,
                    "description" to p.description,
                    "systemPrompt" to p.systemPrompt,
                    "contextFiles" to p.contextFiles,
                    "activeSkillIds" to p.activeSkillIds,
                    "mcpServerIds" to p.mcpServerIds,
                    "createdAt" to p.createdAt,
                    "updatedAt" to p.updatedAt
                )
                userDoc.collection("projects").document(p.id).set(projData, SetOptions.merge()).await()
            }

            // 2. Sync Chats & Messages
            val chats = chatDao.getAllChatsList()
            for (c in chats) {
                val chatData = hashMapOf(
                    "id" to c.id,
                    "title" to c.title,
                    "modelId" to c.modelId,
                    "activeSkillId" to c.activeSkillId,
                    "projectId" to c.projectId,
                    "isAgentic" to c.isAgentic,
                    "createdAt" to c.createdAt,
                    "updatedAt" to c.updatedAt
                )
                userDoc.collection("chats").document(c.id).set(chatData, SetOptions.merge()).await()

                val messages = messageDao.getMessagesForChatList(c.id)
                for (m in messages) {
                    val msgData = hashMapOf(
                        "id" to m.id,
                        "chatId" to m.chatId,
                        "role" to m.role,
                        "content" to m.content,
                        "toolCallsJson" to m.toolCallsJson,
                        "toolCallId" to m.toolCallId,
                        "stepIndex" to m.stepIndex,
                        "isApprovalPending" to m.isApprovalPending,
                        "createdAt" to m.createdAt
                    )
                    userDoc.collection("chats").document(c.id).collection("messages").document(m.id).set(msgData, SetOptions.merge()).await()
                }
            }

            // 3. Sync Skills
            val skills = skillDao.getAllSkillsList()
            for (s in skills) {
                val skillData = hashMapOf(
                    "id" to s.id,
                    "name" to s.name,
                    "description" to s.description,
                    "systemPrompt" to s.systemPrompt,
                    "toolsAllowed" to s.toolsAllowed,
                    "inputSchema" to s.inputSchema,
                    "outputFormat" to s.outputFormat,
                    "sourceUrl" to s.sourceUrl,
                    "isBuiltIn" to s.isBuiltIn,
                    "createdAt" to s.createdAt
                )
                userDoc.collection("skills").document(s.id).set(skillData, SetOptions.merge()).await()
            }

            // 4. Sync MCP Server metadata
            val servers = mcpServerDao.getAllServersList()
            for (srv in servers) {
                val srvData = hashMapOf(
                    "id" to srv.id,
                    "name" to srv.name,
                    "serverUrl" to srv.serverUrl,
                    "transportType" to srv.transportType,
                    "isEnabled" to srv.isEnabled,
                    "cachedToolsJson" to srv.cachedToolsJson,
                    "lastPingTime" to srv.lastPingTime,
                    "createdAt" to srv.createdAt
                )
                userDoc.collection("mcp_servers").document(srv.id).set(srvData, SetOptions.merge()).await()
            }

            val successMsg = "Successfully synced ${projects.size} projects, ${chats.size} chats, and ${skills.size} skills to Firestore."
            _syncState.value = FirestoreSyncState.Success(successMsg)
            Result.success(successMsg)
        } catch (e: Exception) {
            Log.e("FirestoreSync", "Backup failed", e)
            val errorMsg = e.localizedMessage ?: "Unknown Firestore error during backup"
            _syncState.value = FirestoreSyncState.Error(errorMsg)
            Result.failure(e)
        }
    }

    /**
     * Restore all data from Firestore into local Room Database.
     */
    suspend fun restoreAllFromCloud(userId: String = "default_user"): Result<String> = withContext(Dispatchers.IO) {
        _syncState.value = FirestoreSyncState.Syncing
        try {
            val userDoc = firestore.collection("users").document(userId)

            // 1. Restore Projects
            val projSnap = userDoc.collection("projects").get().await()
            var restoredProjects = 0
            for (doc in projSnap.documents) {
                val project = ProjectEntity(
                    id = doc.getString("id") ?: doc.id,
                    name = doc.getString("name") ?: "Restored Project",
                    description = doc.getString("description") ?: "",
                    systemPrompt = doc.getString("systemPrompt") ?: "",
                    contextFiles = doc.getString("contextFiles") ?: "[]",
                    activeSkillIds = doc.getString("activeSkillIds") ?: "[]",
                    mcpServerIds = doc.getString("mcpServerIds") ?: "[]",
                    createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                    updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                )
                projectDao.insertProject(project)
                restoredProjects++
            }

            // 2. Restore Skills
            val skillSnap = userDoc.collection("skills").get().await()
            var restoredSkills = 0
            for (doc in skillSnap.documents) {
                val skill = SkillEntity(
                    id = doc.getString("id") ?: doc.id,
                    name = doc.getString("name") ?: "Restored Skill",
                    description = doc.getString("description") ?: "",
                    systemPrompt = doc.getString("systemPrompt") ?: "",
                    toolsAllowed = doc.getString("toolsAllowed") ?: "[]",
                    inputSchema = doc.getString("inputSchema"),
                    outputFormat = doc.getString("outputFormat"),
                    sourceUrl = doc.getString("sourceUrl"),
                    isBuiltIn = doc.getBoolean("isBuiltIn") ?: false,
                    createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                )
                skillDao.insertSkill(skill)
                restoredSkills++
            }

            // 3. Restore Chats & Messages
            val chatSnap = userDoc.collection("chats").get().await()
            var restoredChats = 0
            for (doc in chatSnap.documents) {
                val chat = ChatEntity(
                    id = doc.getString("id") ?: doc.id,
                    title = doc.getString("title") ?: "Restored Chat",
                    modelId = doc.getString("modelId") ?: "openrouter/free",
                    activeSkillId = doc.getString("activeSkillId"),
                    projectId = doc.getString("projectId"),
                    isAgentic = doc.getBoolean("isAgentic") ?: true,
                    createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                    updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                )
                chatDao.insertChat(chat)
                restoredChats++

                // Restore messages
                val msgSnap = doc.reference.collection("messages").get().await()
                for (mDoc in msgSnap.documents) {
                    val msg = MessageEntity(
                        id = mDoc.getString("id") ?: mDoc.id,
                        chatId = mDoc.getString("chatId") ?: chat.id,
                        role = mDoc.getString("role") ?: "user",
                        content = mDoc.getString("content") ?: "",
                        toolCallsJson = mDoc.getString("toolCallsJson"),
                        toolCallId = mDoc.getString("toolCallId"),
                        stepIndex = mDoc.getLong("stepIndex")?.toInt() ?: 0,
                        isApprovalPending = mDoc.getBoolean("isApprovalPending") ?: false,
                        createdAt = mDoc.getLong("createdAt") ?: System.currentTimeMillis()
                    )
                    messageDao.insertMessage(msg)
                }
            }

            val msg = "Restored $restoredProjects projects, $restoredChats chats, $restoredSkills skills from Firestore."
            _syncState.value = FirestoreSyncState.Success(msg)
            Result.success(msg)
        } catch (e: Exception) {
            Log.e("FirestoreSync", "Restore failed", e)
            val errorMsg = e.localizedMessage ?: "Unknown Firestore error during restore"
            _syncState.value = FirestoreSyncState.Error(errorMsg)
            Result.failure(e)
        }
    }
}
