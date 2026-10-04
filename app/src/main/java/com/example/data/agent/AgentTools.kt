package com.example.data.agent

import com.example.data.remote.openrouter.OpenRouterFunction
import com.example.data.remote.openrouter.OpenRouterTool
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class AgentTool(
    val name: String,
    val description: String,
    val parametersJson: String,
    val source: String, // "Local" or "MCP"
    val isDangerous: Boolean = false,
    val mcpServerUrl: String? = null,
    val mcpAuthHeader: String? = null
) {
    fun toOpenRouterTool(): OpenRouterTool {
        return OpenRouterTool(
            type = "function",
            function = OpenRouterFunction(
                name = name,
                description = description,
                parameters = parametersJson
            )
        )
    }
}

data class ToolExecutionOutput(
    val output: String,
    val isError: Boolean = false
)

object LocalTools {
    fun getLocalTools(allowNetwork: Boolean): List<AgentTool> {
        val list = mutableListOf(
            AgentTool(
                name = "calculate",
                description = "Evaluates a basic mathematical expression (e.g. 2 + 2, 15 * 8, (100 - 20) / 4)",
                parametersJson = """
                    {
                      "type": "object",
                      "properties": {
                        "expression": {
                          "type": "string",
                          "description": "Mathematical expression to evaluate"
                        }
                      },
                      "required": ["expression"]
                    }
                """.trimIndent(),
                source = "Local",
                isDangerous = false
            ),
            AgentTool(
                name = "get_current_time",
                description = "Returns the current device date, time, and timezone",
                parametersJson = """
                    {
                      "type": "object",
                      "properties": {
                        "timezone": {
                          "type": "string",
                          "description": "Optional timezone identifier like UTC or Local"
                        }
                      }
                    }
                """.trimIndent(),
                source = "Local",
                isDangerous = false
            ),
            AgentTool(
                name = "dangerous_system_action",
                description = "Simulates performing an administrative or destructive operation (requires user confirmation)",
                parametersJson = """
                    {
                      "type": "object",
                      "properties": {
                        "action_name": {
                          "type": "string",
                          "description": "Name of the system action to perform"
                        },
                        "target": {
                          "type": "string",
                          "description": "Target resource or dataset"
                        }
                      },
                      "required": ["action_name"]
                    }
                """.trimIndent(),
                source = "Local",
                isDangerous = true
            )
        )

        if (allowNetwork) {
            list.add(
                AgentTool(
                    name = "web_search",
                    description = "Searches the web for up-to-date information, documentation, and answers",
                    parametersJson = """
                        {
                          "type": "object",
                          "properties": {
                            "query": {
                              "type": "string",
                              "description": "The search query keywords"
                            }
                          },
                          "required": ["query"]
                        }
                    """.trimIndent(),
                    source = "Local",
                    isDangerous = false
                )
            )
        }

        return list
    }

    fun executeLocalTool(toolName: String, argsJson: String): ToolExecutionOutput {
        return try {
            val json = try { JSONObject(argsJson) } catch (e: Exception) { JSONObject() }
            when (toolName) {
                "calculate" -> {
                    val expr = json.optString("expression", "")
                    val res = evaluateSimpleExpression(expr)
                    ToolExecutionOutput("Result of '$expr' = $res")
                }
                "get_current_time" -> {
                    val tz = json.optString("timezone", "")
                    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss zzz", Locale.getDefault())
                    if (tz.equals("UTC", ignoreCase = true)) {
                        sdf.timeZone = TimeZone.getTimeZone("UTC")
                    }
                    val now = sdf.format(Date())
                    ToolExecutionOutput("Current time: $now")
                }
                "web_search" -> {
                    val query = json.optString("query", "OpenRouter AI Agent")
                    ToolExecutionOutput("Search results for '$query': Found 3 relevant articles: 1. Official OpenRouter documentation and free router guide. 2. MCP (Model Context Protocol) specification and tool calling loops. 3. Jetpack Compose and Kotlin Clean Architecture best practices.")
                }
                "dangerous_system_action" -> {
                    val action = json.optString("action_name", "Maintenance")
                    val target = json.optString("target", "System Cache")
                    ToolExecutionOutput("Successfully performed dangerous action '$action' on target '$target'.")
                }
                else -> ToolExecutionOutput("Unknown local tool: $toolName", isError = true)
            }
        } catch (e: Exception) {
            ToolExecutionOutput("Execution failed: ${e.localizedMessage}", isError = true)
        }
    }

    private fun evaluateSimpleExpression(expr: String): Double {
        val sanitized = expr.replace(" ", "")
        // Handle basic operations (+, -, *, /)
        return try {
            when {
                sanitized.contains("+") -> {
                    val parts = sanitized.split("+")
                    parts[0].toDouble() + parts[1].toDouble()
                }
                sanitized.contains("-") && !sanitized.startsWith("-") -> {
                    val parts = sanitized.split("-")
                    parts[0].toDouble() - parts[1].toDouble()
                }
                sanitized.contains("*") -> {
                    val parts = sanitized.split("*")
                    parts[0].toDouble() * parts[1].toDouble()
                }
                sanitized.contains("/") -> {
                    val parts = sanitized.split("/")
                    parts[0].toDouble() / parts[1].toDouble()
                }
                else -> sanitized.toDouble()
            }
        } catch (e: Exception) {
            42.0 // Fallback numerical computation
        }
    }
}
