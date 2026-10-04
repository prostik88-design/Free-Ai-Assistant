package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String = "New Chat",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val modelId: String = "openrouter/free",
    val activeSkillId: String? = null,
    val projectId: String? = null,
    val isAgentic: Boolean = true
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val chatId: String,
    val role: String, // "user", "assistant", "system", "tool"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val toolCallId: String? = null,
    val toolName: String? = null,
    val toolArgs: String? = null,
    val toolSource: String? = null, // "Local" or "MCP"
    val toolStatus: String? = null, // "pending_approval", "executing", "success", "error", "rejected"
    val toolResult: String? = null,
    val isStreaming: Boolean = false
)

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val systemPrompt: String = "",
    val contextFilesJson: String = "[]", // JSON array of {name, content}
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "skills")
data class SkillEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String,
    val systemPrompt: String,
    val inputSchema: String? = null,
    val toolsAllowedJson: String = "[]", // JSON array of allowed tool names (empty = all)
    val outputFormat: String? = null,
    val sourceUrl: String? = null,
    val isBuiltIn: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "mcp_servers")
data class McpServerEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val serverUrl: String,
    val transportType: String = "HTTP", // "HTTP", "SSE", "MOCK"
    val authHeader: String? = null,
    val isEnabled: Boolean = true,
    val toolsJson: String = "[]", // Cached discovered tools JSON
    val createdAt: Long = System.currentTimeMillis()
)
