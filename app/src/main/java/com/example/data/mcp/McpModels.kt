package com.example.data.mcp

import org.json.JSONArray
import org.json.JSONObject

data class McpToolDefinition(
    val name: String,
    val description: String,
    val inputSchemaJson: String = "{\"type\":\"object\"}",
    val isEnabled: Boolean = true,
    val isDangerous: Boolean = false
) {
    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("name", name)
        json.put("description", description)
        json.put("inputSchema", inputSchemaJson)
        json.put("isEnabled", isEnabled)
        json.put("isDangerous", isDangerous)
        return json
    }

    companion object {
        fun fromJson(obj: JSONObject): McpToolDefinition {
            return McpToolDefinition(
                name = obj.optString("name", "unknown_tool"),
                description = obj.optString("description", ""),
                inputSchemaJson = obj.optString("inputSchema", "{\"type\":\"object\"}"),
                isEnabled = obj.optBoolean("isEnabled", true),
                isDangerous = obj.optBoolean("isDangerous", false)
            )
        }

        fun parseList(jsonString: String): List<McpToolDefinition> {
            if (jsonString.isBlank()) return emptyList()
            return try {
                val arr = JSONArray(jsonString)
                val list = mutableListOf<McpToolDefinition>()
                for (i in 0 until arr.length()) {
                    list.add(fromJson(arr.getJSONObject(i)))
                }
                list
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
}

data class McpExecutionResult(
    val success: Boolean,
    val output: String,
    val isError: Boolean = !success
)
