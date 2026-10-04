package com.example

import com.example.data.agent.AgentTool
import com.example.data.agent.LocalTools
import com.example.data.mcp.McpClient
import com.example.data.mcp.McpToolDefinition
import com.example.data.parser.SkillParseResult
import com.example.data.parser.SkillParser
import com.example.data.remote.openrouter.OpenRouterMessage
import com.example.data.remote.openrouter.OpenRouterToolCall
import com.example.data.security.KeystoreManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RouterAgentUnitTests {

    // --- API Key & Security Tests ---
    @Test
    fun testApiKeyIsEncryptedAndMasked() {
        val rawKey = "sk-or-v1-abcdef1234567890abcdef1234567890"
        val encrypted = KeystoreManager.encrypt(rawKey)

        // Raw key must never match the stored encrypted payload
        assertNotEquals(rawKey, encrypted)

        // Decryption should restore original key
        val decrypted = KeystoreManager.decrypt(encrypted)
        assertEquals(rawKey, decrypted)

        // Key masking for logs and UI: raw key must not appear
        val masked = KeystoreManager.maskApiKey(rawKey)
        assertFalse(masked.contains("abcdef1234567890"))
        assertTrue(masked.contains("••••••••"))
    }

    // --- Chat & Message Serialization Tests ---
    @Test
    fun testOpenRouterMessageSerialization() {
        val toolCall = OpenRouterToolCall(
            id = "call_99",
            functionName = "calculate",
            functionArguments = "{\"expression\":\"12 * 12\"}"
        )
        val msg = OpenRouterMessage(
            role = "assistant",
            content = "Calculating...",
            toolCalls = listOf(toolCall)
        )

        val json = msg.toJson()
        assertEquals("assistant", json.getString("role"))
        assertEquals("Calculating...", json.getString("content"))
        assertTrue(json.has("tool_calls"))
        val calls = json.getJSONArray("tool_calls")
        assertEquals(1, calls.length())
        assertEquals("call_99", calls.getJSONObject(0).getString("id"))
    }

    // --- Agent Tools & Local Execution Tests ---
    @Test
    fun testLocalToolsExecution() {
        // Test calculator
        val calcOutput = LocalTools.executeLocalTool("calculate", "{\"expression\":\"10 + 25\"}")
        assertFalse(calcOutput.isError)
        assertTrue(calcOutput.output.contains("35"))

        // Test current time
        val timeOutput = LocalTools.executeLocalTool("get_current_time", "{\"timezone\":\"UTC\"}")
        assertFalse(timeOutput.isError)
        assertTrue(timeOutput.output.contains("Current time:"))

        // Test unknown tool rejection
        val unknownOutput = LocalTools.executeLocalTool("unknown_system_tool", "{}")
        assertTrue(unknownOutput.isError)
        assertTrue(unknownOutput.output.contains("Unknown local tool"))
    }

    @Test
    fun testDangerousToolIsFlagged() {
        val tools = LocalTools.getLocalTools(allowNetwork = true)
        val dangerousTool = tools.firstOrNull { it.name == "dangerous_system_action" }
        assertTrue("Dangerous tool must be registered", dangerousTool != null)
        assertTrue("Dangerous tool must have isDangerous=true", dangerousTool!!.isDangerous)
    }

    // --- Skills Validation & Security Tests ---
    @Test
    fun testValidFrontmatterSkillParsing() {
        val validSkillMd = """
            ---
            name: Code Auditor
            description: Analyzes security patterns in Kotlin and Compose
            tools_allowed:
              - calculate
              - web_search
            ---
            You are a strict code auditor. Ensure best practices.
        """.trimIndent()

        val result = SkillParser.parse(validSkillMd, "SKILL.md")
        assertTrue(result is SkillParseResult.Success)
        val skill = (result as SkillParseResult.Success).skill
        assertEquals("Code Auditor", skill.name)
        assertEquals("Analyzes security patterns in Kotlin and Compose", skill.description)
        assertTrue(skill.toolsAllowed.contains("calculate"))
        assertTrue(skill.toolsAllowed.contains("web_search"))
        assertTrue(skill.systemPrompt.contains("strict code auditor"))
    }

    @Test
    fun testInvalidYamlOrMissingFieldsRejected() {
        // Missing name and description
        val missingFields = """
            ---
            foo: bar
            ---
            Some text
        """.trimIndent()

        val result = SkillParser.parse(missingFields, "SKILL.md")
        assertTrue(result is SkillParseResult.Error)
    }

    @Test
    fun testExecutableFilesAreBlockedForSafety() {
        val scriptContent = "#!/bin/bash\nrm -rf /"
        val result = SkillParser.parse(scriptContent, "run_exploit.sh")
        assertTrue(result is SkillParseResult.Error)
        assertTrue((result as SkillParseResult.Error).message.contains("prohibited"))
    }

    // --- MCP Tests ---
    @Test
    fun testMcpToolDefinitionParsing() {
        val jsonArrayStr = """
            [
              {
                "name": "mcp_weather",
                "description": "Fetch weather",
                "isEnabled": true,
                "isDangerous": false
              }
            ]
        """.trimIndent()

        val list = McpToolDefinition.parseList(jsonArrayStr)
        assertEquals(1, list.size)
        assertEquals("mcp_weather", list[0].name)
        assertTrue(list[0].isEnabled)
        assertFalse(list[0].isDangerous)
    }

    @Test
    fun testMcpMockToolExecution() = runBlocking {
        val client = McpClient()
        val result = client.executeTool(
            serverUrl = "mock://demo.local",
            authHeader = null,
            toolName = "mcp_weather_lookup",
            argumentsJson = "{\"city\":\"Berlin\"}"
        )
        assertTrue(result.success)
        assertTrue(result.output.contains("Berlin"))
    }
}
