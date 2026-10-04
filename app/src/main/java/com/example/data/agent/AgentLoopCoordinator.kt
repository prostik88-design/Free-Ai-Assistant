package com.example.data.agent

import com.example.data.local.dao.ChatDao
import com.example.data.local.dao.MessageDao
import com.example.data.local.dao.ProjectDao
import com.example.data.local.dao.SkillDao
import com.example.data.local.entity.MessageEntity
import com.example.data.mcp.McpClient
import com.example.data.mcp.McpToolDefinition
import com.example.data.preferences.SettingsRepository
import com.example.data.remote.openrouter.OpenRouterClient
import com.example.data.remote.openrouter.OpenRouterMessage
import com.example.data.remote.openrouter.OpenRouterToolCall
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.util.UUID

sealed class AgentExecutionState {
    object Idle : AgentExecutionState()
    data class Streaming(val partialText: String) : AgentExecutionState()
    data class ExecutingTool(val toolName: String, val source: String, val iteration: Int) : AgentExecutionState()
    data class AwaitingApproval(
        val pendingMessageId: String,
        val toolName: String,
        val toolArgs: String,
        val source: String
    ) : AgentExecutionState()
    data class Error(val message: String) : AgentExecutionState()
}

class AgentLoopCoordinator(
    private val openRouterClient: OpenRouterClient,
    private val mcpClient: McpClient,
    private val settingsRepository: SettingsRepository,
    private val chatDao: ChatDao,
    private val messageDao: MessageDao,
    private val projectDao: ProjectDao,
    private val skillDao: SkillDao
) {
    private val _agentState = MutableStateFlow<AgentExecutionState>(AgentExecutionState.Idle)
    val agentState: StateFlow<AgentExecutionState> = _agentState.asStateFlow()

    private var currentJob: Job? = null

    fun cancelExecution() {
        currentJob?.cancel()
        currentJob = null
        _agentState.value = AgentExecutionState.Idle
    }

    /**
     * Executes the agent loop or standard streaming chat completion.
     */
    suspend fun executeTask(
        chatId: String,
        userPrompt: String,
        jobRef: Job? = null
    ) = withContext(Dispatchers.IO) {
        currentJob = jobRef

        val settings = settingsRepository.settingsFlow.value
        val apiKey = settingsRepository.getDecryptedApiKey()
        if (apiKey.isBlank()) {
            _agentState.value = AgentExecutionState.Error("OpenRouter API key is missing. Please enter your key in Settings.")
            return@withContext
        }

        val chat = chatDao.getChatByIdOnce(chatId) ?: return@withContext
        val isAgentic = chat.isAgentic

        // 1. Persist user message
        val userMessage = MessageEntity(
            id = UUID.randomUUID().toString(),
            chatId = chatId,
            role = "user",
            content = userPrompt,
            timestamp = System.currentTimeMillis()
        )
        messageDao.insertMessage(userMessage)

        // Update chat timestamp
        chatDao.updateChat(chat.copy(updatedAt = System.currentTimeMillis()))

        // 2 & 3 & 4. Assemble system prompt: Base + Skill + Project
        val systemPromptBuilder = StringBuilder()
        systemPromptBuilder.append(
            "You are RouterAgent, an expert autonomous AI agent running natively on Android with OpenRouter tool-calling capabilities. "
        )

        // Project Context
        var boundSkillId = chat.activeSkillId
        if (!chat.projectId.isNullOrBlank()) {
            val project = projectDao.getProjectByIdOnce(chat.projectId)
            if (project != null) {
                systemPromptBuilder.append("\n\n[Active Project: ${project.name}]\n")
                if (project.description.isNotBlank()) {
                    systemPromptBuilder.append("Description: ${project.description}\n")
                }
                if (project.systemPrompt.isNotBlank()) {
                    systemPromptBuilder.append("Project Instructions: ${project.systemPrompt}\n")
                }
                if (project.contextFilesJson.isNotBlank() && project.contextFilesJson != "[]") {
                    systemPromptBuilder.append("Project Files / Context:\n${project.contextFilesJson}\n")
                }
            }
        }

        // Skill Context & Tool Filtering
        var allowedToolFilter: List<String>? = null
        if (!boundSkillId.isNullOrBlank()) {
            val skill = skillDao.getSkillById(boundSkillId)
            if (skill != null) {
                systemPromptBuilder.append("\n\n[Active Skill: ${skill.name}]\n")
                systemPromptBuilder.append("Skill Guidance: ${skill.systemPrompt}\n")
                if (!skill.outputFormat.isNullOrBlank()) {
                    systemPromptBuilder.append("Desired Output Format: ${skill.outputFormat}\n")
                }
                if (skill.toolsAllowedJson.isNotBlank() && skill.toolsAllowedJson != "[]") {
                    try {
                        val arr = JSONArray(skill.toolsAllowedJson)
                        val list = mutableListOf<String>()
                        for (i in 0 until arr.length()) list.add(arr.getString(i))
                        if (list.isNotEmpty()) allowedToolFilter = list
                    } catch (e: Exception) {}
                }
            }
        }

        // 5. Build Unified Tool Registry
        val registry = buildToolRegistry(settings.allowNetworkTools, settings.allowMcp, allowedToolFilter)

        val modelToUse = if (settings.isOpenRouterFree) "openrouter/free" else chat.modelId
        val maxIterations = if (isAgentic) settings.maxAgentIterations else 1

        var iteration = 0
        var loopFinished = false

        while (!loopFinished && iteration < maxIterations) {
            iteration++

            // Fetch current conversation messages for prompt
            val dbMessages = messageDao.getMessagesForChatOnce(chatId)
            val openRouterMessages = mutableListOf<OpenRouterMessage>()

            openRouterMessages.add(
                OpenRouterMessage(role = "system", content = systemPromptBuilder.toString())
            )

            for (m in dbMessages) {
                when (m.role) {
                    "user" -> openRouterMessages.add(OpenRouterMessage(role = "user", content = m.content))
                    "assistant" -> {
                        val toolCalls = if (!m.toolCallId.isNullOrBlank() && !m.toolName.isNullOrBlank()) {
                            listOf(
                                OpenRouterToolCall(
                                    id = m.toolCallId,
                                    functionName = m.toolName,
                                    functionArguments = m.toolArgs ?: "{}"
                                )
                            )
                        } else null
                        openRouterMessages.add(
                            OpenRouterMessage(role = "assistant", content = m.content, toolCalls = toolCalls)
                        )
                    }
                    "tool" -> {
                        openRouterMessages.add(
                            OpenRouterMessage(
                                role = "tool",
                                content = m.toolResult ?: m.content,
                                toolCallId = m.toolCallId,
                                name = m.toolName
                            )
                        )
                    }
                }
            }

            val toolsToSend = if (isAgentic) registry.map { it.toOpenRouterTool() } else emptyList()

            // Assistant message placeholder
            val assistantMsgId = UUID.randomUUID().toString()
            var accumulatedText = ""
            var receivedToolCalls: List<OpenRouterToolCall>? = null

            _agentState.value = AgentExecutionState.Streaming("")

            try {
                openRouterClient.streamChatCompletion(
                    apiKey = apiKey,
                    model = modelToUse,
                    messages = openRouterMessages,
                    tools = toolsToSend,
                    temperature = settings.temperature,
                    maxTokens = settings.maxTokens
                ).collect { chunk ->
                    if (chunk.error != null) {
                        _agentState.value = AgentExecutionState.Error(chunk.error)
                        loopFinished = true
                        return@collect
                    }

                    if (!chunk.deltaText.isNullOrEmpty()) {
                        accumulatedText += chunk.deltaText
                        _agentState.value = AgentExecutionState.Streaming(accumulatedText)
                    }

                    if (!chunk.toolCalls.isNullOrEmpty()) {
                        receivedToolCalls = chunk.toolCalls
                    }
                }
            } catch (ce: CancellationException) {
                _agentState.value = AgentExecutionState.Idle
                return@withContext
            } catch (e: Exception) {
                _agentState.value = AgentExecutionState.Error(e.localizedMessage ?: "Network error during stream")
                loopFinished = true
                return@withContext
            }

            // Persist the assistant text if there was any
            if (accumulatedText.isNotBlank()) {
                messageDao.insertMessage(
                    MessageEntity(
                        id = assistantMsgId,
                        chatId = chatId,
                        role = "assistant",
                        content = accumulatedText,
                        timestamp = System.currentTimeMillis()
                    )
                )
            }

            // 7. Check if model returned tool calls
            val toolCalls = receivedToolCalls
            if (isAgentic && !toolCalls.isNullOrEmpty()) {
                for (call in toolCalls) {
                    val toolName = call.functionName
                    val toolArgs = call.functionArguments
                    val matchedTool = registry.firstOrNull { it.name == toolName }

                    if (matchedTool == null) {
                        // Page 4: Unknown tool is cleanly rejected
                        val rejectedResult = "Error: Unknown tool '$toolName'. Tool was rejected."
                        messageDao.insertMessage(
                            MessageEntity(
                                id = UUID.randomUUID().toString(),
                                chatId = chatId,
                                role = "tool",
                                content = rejectedResult,
                                toolCallId = call.id,
                                toolName = toolName,
                                toolArgs = toolArgs,
                                toolSource = "Local",
                                toolStatus = "error",
                                toolResult = rejectedResult,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                        continue
                    }

                    // Check if tool is allowed by active skill
                    if (allowedToolFilter != null && !allowedToolFilter.contains(toolName)) {
                        val forbiddenResult = "Error: Tool '$toolName' is forbidden by active skill."
                        messageDao.insertMessage(
                            MessageEntity(
                                id = UUID.randomUUID().toString(),
                                chatId = chatId,
                                role = "tool",
                                content = forbiddenResult,
                                toolCallId = call.id,
                                toolName = toolName,
                                toolArgs = toolArgs,
                                toolSource = matchedTool.source,
                                toolStatus = "error",
                                toolResult = forbiddenResult,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                        continue
                    }

                    // 8. Check dangerous tool approval requirement
                    if (matchedTool.isDangerous && settings.confirmDangerousActions) {
                        val pendingMsgId = UUID.randomUUID().toString()
                        messageDao.insertMessage(
                            MessageEntity(
                                id = pendingMsgId,
                                chatId = chatId,
                                role = "tool",
                                content = "Pending confirmation for dangerous action: $toolName",
                                toolCallId = call.id,
                                toolName = toolName,
                                toolArgs = toolArgs,
                                toolSource = matchedTool.source,
                                toolStatus = "pending_approval",
                                timestamp = System.currentTimeMillis()
                            )
                        )

                        _agentState.value = AgentExecutionState.AwaitingApproval(
                            pendingMessageId = pendingMsgId,
                            toolName = toolName,
                            toolArgs = toolArgs,
                            source = matchedTool.source
                        )

                        // Pause loop until approved/rejected by user
                        loopFinished = true
                        return@withContext
                    }

                    // 9. Execute tool
                    _agentState.value = AgentExecutionState.ExecutingTool(
                        toolName = toolName,
                        source = matchedTool.source,
                        iteration = iteration
                    )

                    val output = executeTool(matchedTool, toolArgs)

                    // 10. Persist tool result to database
                    messageDao.insertMessage(
                        MessageEntity(
                            id = UUID.randomUUID().toString(),
                            chatId = chatId,
                            role = "tool",
                            content = output.output,
                            toolCallId = call.id,
                            toolName = toolName,
                            toolArgs = toolArgs,
                            toolSource = matchedTool.source,
                            toolStatus = if (output.isError) "error" else "success",
                            toolResult = output.output,
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }

                // 11. Repeat loop iteration with tool results fed back to model
            } else {
                // No tool calls returned -> final answer reached!
                loopFinished = true
            }
        }

        // 12. Check max iterations limit reached
        if (iteration >= maxIterations && !loopFinished) {
            messageDao.insertMessage(
                MessageEntity(
                    id = UUID.randomUUID().toString(),
                    chatId = chatId,
                    role = "assistant",
                    content = "⚠️ Agent reached the maximum limit of $maxIterations iterations and concluded the execution loop.",
                    timestamp = System.currentTimeMillis()
                )
            )
        }

        _agentState.value = AgentExecutionState.Idle
    }

    /**
     * Resolves pending dangerous action approval.
     */
    suspend fun resolvePendingApproval(
        messageId: String,
        approved: Boolean
    ) = withContext(Dispatchers.IO) {
        val message = messageDao.getMessageById(messageId) ?: return@withContext
        val toolName = message.toolName ?: ""
        val toolArgs = message.toolArgs ?: "{}"
        val chatId = message.chatId

        if (approved) {
            val settings = settingsRepository.settingsFlow.value
            val registry = buildToolRegistry(settings.allowNetworkTools, settings.allowMcp, null)
            val matched = registry.firstOrNull { it.name == toolName }
                ?: AgentTool(
                    name = toolName,
                    description = "Dangerous action",
                    parametersJson = "{}",
                    source = message.toolSource ?: "Local",
                    isDangerous = true
                )

            val output = executeTool(matched, toolArgs)
            messageDao.updateMessage(
                message.copy(
                    content = output.output,
                    toolStatus = if (output.isError) "error" else "success",
                    toolResult = output.output
                )
            )

            // Continue loop with the approved tool result
            executeTask(chatId, "Approved execution: $toolName")
        } else {
            messageDao.updateMessage(
                message.copy(
                    content = "Action canceled by user.",
                    toolStatus = "rejected",
                    toolResult = "Action '$toolName' was rejected by the user."
                )
            )
            _agentState.value = AgentExecutionState.Idle
        }
    }

    private suspend fun buildToolRegistry(
        allowNetwork: Boolean,
        allowMcp: Boolean,
        filter: List<String>?
    ): List<AgentTool> {
        val list = mutableListOf<AgentTool>()

        // 1. Local tools
        list.addAll(LocalTools.getLocalTools(allowNetwork))

        // 2. MCP tools from database
        if (allowMcp) {
            // Note: we can load enabled servers and their discovered tools
            // from database or cache
        }

        // Apply skill filter if provided
        return if (filter != null) {
            list.filter { filter.contains(it.name) }
        } else {
            list
        }
    }

    private suspend fun executeTool(tool: AgentTool, argsJson: String): ToolExecutionOutput {
        return if (tool.source == "MCP" && tool.mcpServerUrl != null) {
            val mcpRes = mcpClient.executeTool(
                serverUrl = tool.mcpServerUrl,
                authHeader = tool.mcpAuthHeader,
                toolName = tool.name,
                argumentsJson = argsJson
            )
            ToolExecutionOutput(mcpRes.output, isError = mcpRes.isError)
        } else {
            LocalTools.executeLocalTool(tool.name, argsJson)
        }
    }
}
