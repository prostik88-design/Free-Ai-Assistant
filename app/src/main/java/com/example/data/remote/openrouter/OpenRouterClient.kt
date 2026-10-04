package com.example.data.remote.openrouter

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

class OpenRouterClient(
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
) {
    companion object {
        private const val BASE_URL = "https://openrouter.ai/api/v1"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        val DEFAULT_FREE_MODELS = listOf(
            ModelOption(
                id = "openrouter/free",
                name = "OpenRouter Free Auto-Router",
                isFree = true,
                description = "Automatically routes to available high-speed free models with tool calling"
            ),
            ModelOption(
                id = "google/gemini-2.0-flash-exp:free",
                name = "Gemini 2.0 Flash (Free)",
                isFree = true,
                description = "Fast multimodal model with tool calling & structured outputs"
            ),
            ModelOption(
                id = "meta-llama/llama-3.3-70b-instruct:free",
                name = "Llama 3.3 70B Instruct (Free)",
                isFree = true,
                description = "Powerful open weights flagship model with tool calling"
            ),
            ModelOption(
                id = "deepseek/deepseek-r1:free",
                name = "DeepSeek R1 (Free)",
                isFree = true,
                description = "High capability reasoning model"
            ),
            ModelOption(
                id = "qwen/qwen-2.5-coder-32b-instruct:free",
                name = "Qwen 2.5 Coder 32B (Free)",
                isFree = true,
                description = "Specialized coding and agent execution model"
            ),
            ModelOption(
                id = "anthropic/claude-3.5-sonnet",
                name = "Claude 3.5 Sonnet",
                isFree = false,
                description = "Industry benchmark for complex reasoning, coding, and agents"
            ),
            ModelOption(
                id = "openai/gpt-4o",
                name = "GPT-4o",
                isFree = false,
                description = "OpenAI multimodal flagship"
            )
        )
    }

    suspend fun validateApiKey(apiKey: String): KeyValidationResult = withContext(Dispatchers.IO) {
        val trimmed = apiKey.trim()
        if (trimmed.isEmpty()) {
            return@withContext KeyValidationResult(
                isValid = false,
                errorMessage = "API key is empty"
            )
        }

        try {
            val request = Request.Builder()
                .url("$BASE_URL/auth/key")
                .header("Authorization", "Bearer $trimmed")
                .header("HTTP-Referer", "https://routeragent.example.com")
                .header("X-Title", "RouterAgent Android")
                .get()
                .build()

            val response = okHttpClient.newCall(request).execute()
            val code = response.code
            val bodyString = response.body?.string().orEmpty()

            if (response.isSuccessful && bodyString.isNotEmpty()) {
                val json = JSONObject(bodyString)
                val data = json.optJSONObject("data")
                val label = data?.optString("label") ?: "OpenRouter Key"
                val usage = data?.optDouble("usage")
                val limit = if (data != null && !data.isNull("limit")) data.optDouble("limit") else null
                val isFreeTier = data?.optBoolean("is_free_tier", true) ?: true

                KeyValidationResult(
                    isValid = true,
                    label = label,
                    usage = usage,
                    limit = limit,
                    isFreeTier = isFreeTier
                )
            } else if (code == 401 || code == 403) {
                KeyValidationResult(
                    isValid = false,
                    errorMessage = "Invalid API key or unauthorized ($code)"
                )
            } else {
                // If /auth/key is not supported or returns error, fallback check /models
                val modelsRequest = Request.Builder()
                    .url("$BASE_URL/models")
                    .header("Authorization", "Bearer $trimmed")
                    .get()
                    .build()
                val modelsResponse = okHttpClient.newCall(modelsRequest).execute()
                if (modelsResponse.isSuccessful) {
                    KeyValidationResult(
                        isValid = true,
                        label = "Verified via Models API",
                        isFreeTier = true
                    )
                } else {
                    KeyValidationResult(
                        isValid = false,
                        errorMessage = "API returned code $code: ${response.message}"
                    )
                }
            }
        } catch (e: Exception) {
            KeyValidationResult(
                isValid = false,
                errorMessage = "Connection error: ${e.localizedMessage ?: "Unknown network failure"}"
            )
        }
    }

    suspend fun fetchModels(apiKey: String): List<ModelOption> = withContext(Dispatchers.IO) {
        val trimmed = apiKey.trim()
        val requestBuilder = Request.Builder()
            .url("$BASE_URL/models")
            .header("HTTP-Referer", "https://routeragent.example.com")
            .header("X-Title", "RouterAgent Android")

        if (trimmed.isNotEmpty()) {
            requestBuilder.header("Authorization", "Bearer $trimmed")
        }

        try {
            val response = okHttpClient.newCall(requestBuilder.build()).execute()
            if (response.isSuccessful) {
                val body = response.body?.string().orEmpty()
                val json = JSONObject(body)
                val data = json.optJSONArray("data") ?: JSONArray()
                val list = mutableListOf<ModelOption>()

                // Put openrouter/free first
                list.add(DEFAULT_FREE_MODELS[0])

                for (i in 0 until data.length()) {
                    val obj = data.getJSONObject(i)
                    val id = obj.optString("id")
                    val name = obj.optString("name", id)
                    val pricing = obj.optJSONObject("pricing")
                    val promptPrice = pricing?.optDouble("prompt", 0.0) ?: 0.0
                    val isFree = id.contains(":free") || id.startsWith("openrouter/free") || promptPrice == 0.0

                    if (id != "openrouter/free") {
                        list.add(
                            ModelOption(
                                id = id,
                                name = name,
                                isFree = isFree,
                                contextLength = obj.optInt("context_length", 8192),
                                description = obj.optString("description", "")
                            )
                        )
                    }
                }
                if (list.size > 1) {
                    return@withContext list
                }
            }
        } catch (e: Exception) {
            // Handled below
        }
        DEFAULT_FREE_MODELS
    }

    /**
     * Streams chat completion via SSE.
     */
    fun streamChatCompletion(
        apiKey: String,
        model: String,
        messages: List<OpenRouterMessage>,
        tools: List<OpenRouterTool> = emptyList(),
        temperature: Float = 0.7f,
        maxTokens: Int = 2048
    ): Flow<StreamChunk> = callbackFlow {
        val trimmedKey = apiKey.trim()
        val requestJson = JSONObject()
        requestJson.put("model", model)
        requestJson.put("temperature", temperature)
        requestJson.put("max_tokens", maxTokens)
        requestJson.put("stream", true)

        val msgsArray = JSONArray()
        for (m in messages) {
            msgsArray.put(m.toJson())
        }
        requestJson.put("messages", msgsArray)

        if (tools.isNotEmpty()) {
            val toolsArray = JSONArray()
            for (t in tools) {
                toolsArray.put(t.toJson())
            }
            requestJson.put("tools", toolsArray)
        }

        val request = Request.Builder()
            .url("$BASE_URL/chat/completions")
            .header("Authorization", "Bearer $trimmedKey")
            .header("HTTP-Referer", "https://routeragent.example.com")
            .header("X-Title", "RouterAgent Android")
            .header("Accept", "text/event-stream")
            .post(requestJson.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        val call = okHttpClient.newCall(request)

        try {
            val response: Response = call.execute()
            if (!response.isSuccessful) {
                val errBody = response.body?.string().orEmpty()
                trySend(
                    StreamChunk(
                        error = "OpenRouter Error (${response.code}): $errBody"
                    )
                )
                close()
                return@callbackFlow
            }

            val body = response.body
            if (body == null) {
                trySend(StreamChunk(error = "Empty response body from OpenRouter"))
                close()
                return@callbackFlow
            }

            val reader = BufferedReader(InputStreamReader(body.byteStream(), Charsets.UTF_8))
            var line: String?

            // Accumulate tool calls across streaming chunks
            val toolCallMap = mutableMapOf<Int, MutableMap<String, String>>()

            while (reader.readLine().also { line = it } != null) {
                val l = line?.trim().orEmpty()
                if (l.isEmpty() || l.startsWith(":")) continue // Ping or comment

                if (l.startsWith("data: ")) {
                    val data = l.removePrefix("data: ").trim()
                    if (data == "[DONE]") {
                        // Finish streaming: emit accumulated tool calls if any
                        val finalCalls = if (toolCallMap.isNotEmpty()) {
                            toolCallMap.toSortedMap().map { (index, map) ->
                                OpenRouterToolCall(
                                    index = index,
                                    id = map["id"] ?: "call_$index",
                                    type = "function",
                                    functionName = map["name"] ?: "",
                                    functionArguments = map["arguments"] ?: ""
                                )
                            }
                        } else null

                        trySend(
                            StreamChunk(
                                toolCalls = finalCalls,
                                finishReason = "stop"
                            )
                        )
                        break
                    }

                    try {
                        val chunkObj = JSONObject(data)
                        val choices = chunkObj.optJSONArray("choices")
                        if (choices != null && choices.length() > 0) {
                            val choice = choices.getJSONObject(0)
                            val delta = choice.optJSONObject("delta")
                            val finishReason = if (choice.has("finish_reason") && !choice.isNull("finish_reason")) choice.getString("finish_reason") else null

                            var textDelta: String? = null
                            if (delta != null) {
                                if (delta.has("content") && !delta.isNull("content")) {
                                    textDelta = delta.getString("content")
                                }

                                if (delta.has("tool_calls")) {
                                    val tcArr = delta.getJSONArray("tool_calls")
                                    for (i in 0 until tcArr.length()) {
                                        val tc = tcArr.getJSONObject(i)
                                        val idx = tc.optInt("index", i)
                                        val cur = toolCallMap.getOrPut(idx) { mutableMapOf() }
                                        if (tc.has("id")) {
                                            cur["id"] = tc.getString("id")
                                        }
                                        val fn = tc.optJSONObject("function")
                                        if (fn != null) {
                                            if (fn.has("name")) {
                                                cur["name"] = (cur["name"] ?: "") + fn.getString("name")
                                            }
                                            if (fn.has("arguments")) {
                                                cur["arguments"] = (cur["arguments"] ?: "") + fn.getString("arguments")
                                            }
                                        }
                                    }
                                }
                            }

                            if (!textDelta.isNullOrEmpty() || finishReason != null) {
                                trySend(
                                    StreamChunk(
                                        deltaText = textDelta,
                                        finishReason = finishReason
                                    )
                                )
                            }
                        }
                    } catch (e: Exception) {
                        // Skip malformed chunk
                    }
                }
            }
            reader.close()
        } catch (e: Exception) {
            trySend(StreamChunk(error = "Stream error: ${e.localizedMessage ?: "Connection interrupted"}"))
        }

        awaitClose {
            call.cancel()
        }
    }.flowOn(Dispatchers.IO)
}
