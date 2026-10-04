package com.example.data.remote.openrouter

import org.json.JSONArray
import org.json.JSONObject

data class OpenRouterMessage(
    val role: String,
    val content: String? = null,
    val toolCallId: String? = null,
    val name: String? = null,
    val toolCalls: List<OpenRouterToolCall>? = null
) {
    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("role", role)
        if (content != null) {
            json.put("content", content)
        } else {
            json.put("content", "")
        }
        if (toolCallId != null) {
            json.put("tool_call_id", toolCallId)
        }
        if (name != null) {
            json.put("name", name)
        }
        if (!toolCalls.isNullOrEmpty()) {
            val callsArray = JSONArray()
            for (tc in toolCalls) {
                callsArray.put(tc.toJson())
            }
            json.put("tool_calls", callsArray)
        }
        return json
    }
}

data class OpenRouterTool(
    val type: String = "function",
    val function: OpenRouterFunction
) {
    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("type", type)
        json.put("function", function.toJson())
        return json
    }
}

data class OpenRouterFunction(
    val name: String,
    val description: String,
    val parameters: String? = null // JSON schema string
) {
    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("name", name)
        json.put("description", description)
        if (!parameters.isNullOrBlank()) {
            try {
                json.put("parameters", JSONObject(parameters))
            } catch (e: Exception) {
                val dummy = JSONObject()
                dummy.put("type", "object")
                json.put("parameters", dummy)
            }
        } else {
            val dummy = JSONObject()
            dummy.put("type", "object")
            json.put("parameters", dummy)
        }
        return json
    }
}

data class OpenRouterToolCall(
    val index: Int = 0,
    val id: String = "",
    val type: String = "function",
    val functionName: String = "",
    val functionArguments: String = ""
) {
    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("id", id)
        json.put("type", type)
        val f = JSONObject()
        f.put("name", functionName)
        f.put("arguments", functionArguments)
        json.put("function", f)
        return json
    }
}

data class StreamChunk(
    val deltaText: String? = null,
    val toolCalls: List<OpenRouterToolCall>? = null,
    val finishReason: String? = null,
    val error: String? = null
)

data class KeyValidationResult(
    val isValid: Boolean,
    val label: String? = null,
    val usage: Double? = null,
    val limit: Double? = null,
    val isFreeTier: Boolean? = null,
    val errorMessage: String? = null
)

data class ModelOption(
    val id: String,
    val name: String,
    val isFree: Boolean,
    val contextLength: Int = 8192,
    val description: String = ""
)
