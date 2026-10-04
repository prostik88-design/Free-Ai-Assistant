package com.example.data.mcp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

class McpClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()
) {
    companion object {
        private val JSON_TYPE = "application/json; charset=utf-8".toMediaType()
        private const val DEFAULT_TIMEOUT_MS = 12000L
    }

    /**
     * Queries the MCP server for available tools via JSON-RPC 2.0 `tools/list`.
     * Does not block or crash if server is offline or unreachable.
     */
    suspend fun discoverTools(serverUrl: String, authHeader: String?): List<McpToolDefinition> =
        withContext(Dispatchers.IO) {
            val trimmedUrl = serverUrl.trim()
            if (trimmedUrl.isEmpty()) return@withContext emptyList()

            // Support mock/demo servers natively
            if (trimmedUrl.startsWith("mock://") || trimmedUrl.contains("example.com")) {
                return@withContext getMockToolsForUrl(trimmedUrl)
            }

            val result = withTimeoutOrNull(DEFAULT_TIMEOUT_MS) {
                try {
                    val rpcRequest = JSONObject().apply {
                        put("jsonrpc", "2.0")
                        put("id", 1)
                        put("method", "tools/list")
                        put("params", JSONObject())
                    }

                    val builder = Request.Builder()
                        .url(trimmedUrl)
                        .post(rpcRequest.toString().toRequestBody(JSON_TYPE))
                        .header("Content-Type", "application/json")

                    if (!authHeader.isNullOrBlank()) {
                        builder.header("Authorization", authHeader.trim())
                    }

                    val response = client.newCall(builder.build()).execute()
                    if (response.isSuccessful) {
                        val body = response.body?.string().orEmpty()
                        parseToolsFromRpcResponse(body)
                    } else {
                        emptyList()
                    }
                } catch (e: Exception) {
                    emptyList()
                }
            }

            result ?: emptyList()
        }

    /**
     * Executes an MCP tool via JSON-RPC `tools/call`.
     */
    suspend fun executeTool(
        serverUrl: String,
        authHeader: String?,
        toolName: String,
        argumentsJson: String
    ): McpExecutionResult = withContext(Dispatchers.IO) {
        val trimmedUrl = serverUrl.trim()
        if (trimmedUrl.isEmpty()) {
            return@withContext McpExecutionResult(false, "MCP Server URL is empty", isError = true)
        }

        // Mock server handling
        if (trimmedUrl.startsWith("mock://") || trimmedUrl.contains("example.com")) {
            return@withContext executeMockTool(toolName, argumentsJson)
        }

        val result = withTimeoutOrNull(DEFAULT_TIMEOUT_MS) {
            try {
                val argsObj = try {
                    JSONObject(argumentsJson)
                } catch (e: Exception) {
                    JSONObject()
                }

                val rpcRequest = JSONObject().apply {
                    put("jsonrpc", "2.0")
                    put("id", 2)
                    put("method", "tools/call")
                    put("params", JSONObject().apply {
                        put("name", toolName)
                        put("arguments", argsObj)
                    })
                }

                val builder = Request.Builder()
                    .url(trimmedUrl)
                    .post(rpcRequest.toString().toRequestBody(JSON_TYPE))
                    .header("Content-Type", "application/json")

                if (!authHeader.isNullOrBlank()) {
                    builder.header("Authorization", authHeader.trim())
                }

                val response = client.newCall(builder.build()).execute()
                val body = response.body?.string().orEmpty()

                if (response.isSuccessful && body.isNotEmpty()) {
                    val respJson = JSONObject(body)
                    if (respJson.has("error")) {
                        val err = respJson.getJSONObject("error")
                        McpExecutionResult(
                            success = false,
                            output = "MCP Error: ${err.optString("message", "Unknown error")}",
                            isError = true
                        )
                    } else {
                        val res = respJson.optJSONObject("result")
                        val content = res?.optJSONArray("content")
                        val outputText = if (content != null && content.length() > 0) {
                            val sb = StringBuilder()
                            for (i in 0 until content.length()) {
                                val item = content.getJSONObject(i)
                                sb.append(item.optString("text", "")).append("\n")
                            }
                            sb.toString().trim()
                        } else {
                            res?.toString(2) ?: "Execution completed successfully."
                        }
                        McpExecutionResult(success = true, output = outputText)
                    }
                } else {
                    McpExecutionResult(
                        success = false,
                        output = "HTTP ${response.code}: ${response.message}",
                        isError = true
                    )
                }
            } catch (e: SocketTimeoutException) {
                McpExecutionResult(
                    success = false,
                    output = "MCP Server request timed out after 12s.",
                    isError = true
                )
            } catch (e: Exception) {
                McpExecutionResult(
                    success = false,
                    output = "Failed to connect to MCP server: ${e.localizedMessage ?: "Connection error"}",
                    isError = true
                )
            }
        }

        result ?: McpExecutionResult(
            success = false,
            output = "MCP request timed out.",
            isError = true
        )
    }

    private fun parseToolsFromRpcResponse(responseBody: String): List<McpToolDefinition> {
        val list = mutableListOf<McpToolDefinition>()
        try {
            val json = JSONObject(responseBody)
            val result = json.optJSONObject("result")
            val toolsArr = result?.optJSONArray("tools") ?: json.optJSONArray("tools")
            if (toolsArr != null) {
                for (i in 0 until toolsArr.length()) {
                    val item = toolsArr.getJSONObject(i)
                    val name = item.optString("name")
                    val description = item.optString("description", "")
                    val schema = item.optJSONObject("inputSchema")?.toString() ?: "{\"type\":\"object\"}"
                    list.add(
                        McpToolDefinition(
                            name = name,
                            description = description,
                            inputSchemaJson = schema,
                            isEnabled = true
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // Ignore parse errors
        }
        return list
    }

    private fun getMockToolsForUrl(url: String): List<McpToolDefinition> {
        return listOf(
            McpToolDefinition(
                name = "mcp_weather_lookup",
                description = "Retrieves live weather data for a city",
                inputSchemaJson = "{\"type\":\"object\",\"properties\":{\"city\":{\"type\":\"string\"}},\"required\":[\"city\"]}"
            ),
            McpToolDefinition(
                name = "mcp_database_query",
                description = "Executes read-only SQL query against cloud database",
                inputSchemaJson = "{\"type\":\"object\",\"properties\":{\"query\":{\"type\":\"string\"}},\"required\":[\"query\"]}",
                isDangerous = false
            ),
            McpToolDefinition(
                name = "mcp_restart_service",
                description = "Restarts a production cloud service instance (Dangerous)",
                inputSchemaJson = "{\"type\":\"object\",\"properties\":{\"service_id\":{\"type\":\"string\"}},\"required\":[\"service_id\"]}",
                isDangerous = true
            )
        )
    }

    private fun executeMockTool(toolName: String, argumentsJson: String): McpExecutionResult {
        return when (toolName) {
            "mcp_weather_lookup" -> {
                val city = try { JSONObject(argumentsJson).optString("city", "Tokyo") } catch (e: Exception) { "Tokyo" }
                McpExecutionResult(
                    success = true,
                    output = "Weather in $city: 21°C, Partly Cloudy, Humidity 58%, Wind 4 km/h."
                )
            }
            "mcp_database_query" -> {
                McpExecutionResult(
                    success = true,
                    output = "Query executed: returned 3 records [status: 200, rowCount: 3, latency: 14ms]"
                )
            }
            "mcp_restart_service" -> {
                McpExecutionResult(
                    success = true,
                    output = "Service restarted successfully at ${System.currentTimeMillis()}."
                )
            }
            else -> {
                McpExecutionResult(
                    success = true,
                    output = "Tool '$toolName' invoked with arguments: $argumentsJson"
                )
            }
        }
    }
}
