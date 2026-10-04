package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.example.data.security.KeystoreManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UserSettings(
    val maskedApiKey: String = "",
    val hasApiKey: Boolean = false,
    val selectedModel: String = "openrouter/free",
    val isOpenRouterFree: Boolean = true,
    val temperature: Float = 0.7f,
    val maxTokens: Int = 2048,
    val maxAgentIterations: Int = 10,
    val parallelToolCalls: Boolean = true,
    val confirmDangerousActions: Boolean = true,
    val allowMcp: Boolean = true,
    val allowNetworkTools: Boolean = true,
    val autoSendToolResults: Boolean = true,
    val language: String = "ru"
)

class SettingsRepository(private val context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("router_agent_prefs", Context.MODE_PRIVATE)

    private val _settingsFlow = MutableStateFlow(loadSettings())
    val settingsFlow: StateFlow<UserSettings> = _settingsFlow.asStateFlow()

    private fun loadSettings(): UserSettings {
        val encryptedKey = prefs.getString(KEY_API_KEY_ENCRYPTED, "") ?: ""
        val decryptedKey = KeystoreManager.decrypt(encryptedKey)
        val masked = KeystoreManager.maskApiKey(decryptedKey)
        val hasKey = decryptedKey.isNotBlank()

        return UserSettings(
            maskedApiKey = masked,
            hasApiKey = hasKey,
            selectedModel = prefs.getString(KEY_MODEL, "openrouter/free") ?: "openrouter/free",
            isOpenRouterFree = prefs.getBoolean(KEY_ROUTER_FREE, true),
            temperature = prefs.getFloat(KEY_TEMPERATURE, 0.7f),
            maxTokens = prefs.getInt(KEY_MAX_TOKENS, 2048),
            maxAgentIterations = prefs.getInt(KEY_MAX_ITERATIONS, 10),
            parallelToolCalls = prefs.getBoolean(KEY_PARALLEL_TOOLS, true),
            confirmDangerousActions = prefs.getBoolean(KEY_CONFIRM_DANGEROUS, true),
            allowMcp = prefs.getBoolean(KEY_ALLOW_MCP, true),
            allowNetworkTools = prefs.getBoolean(KEY_ALLOW_NETWORK, true),
            autoSendToolResults = prefs.getBoolean(KEY_AUTO_SEND_TOOLS, true),
            language = prefs.getString(KEY_LANGUAGE, "ru") ?: "ru"
        )
    }

    fun getDecryptedApiKey(): String {
        val encrypted = prefs.getString(KEY_API_KEY_ENCRYPTED, "") ?: ""
        return KeystoreManager.decrypt(encrypted)
    }

    fun saveApiKey(rawKey: String) {
        val trimmed = rawKey.trim()
        val encrypted = KeystoreManager.encrypt(trimmed)
        prefs.edit().putString(KEY_API_KEY_ENCRYPTED, encrypted).apply()
        _settingsFlow.value = loadSettings()
    }

    fun setSelectedModel(model: String) {
        prefs.edit().putString(KEY_MODEL, model).apply()
        _settingsFlow.value = _settingsFlow.value.copy(selectedModel = model)
    }

    fun setOpenRouterFree(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ROUTER_FREE, enabled).apply()
        val model = if (enabled) "openrouter/free" else _settingsFlow.value.selectedModel
        if (enabled) {
            prefs.edit().putString(KEY_MODEL, "openrouter/free").apply()
        }
        _settingsFlow.value = _settingsFlow.value.copy(
            isOpenRouterFree = enabled,
            selectedModel = model
        )
    }

    fun setTemperature(temp: Float) {
        prefs.edit().putFloat(KEY_TEMPERATURE, temp).apply()
        _settingsFlow.value = _settingsFlow.value.copy(temperature = temp)
    }

    fun setMaxTokens(tokens: Int) {
        prefs.edit().putInt(KEY_MAX_TOKENS, tokens).apply()
        _settingsFlow.value = _settingsFlow.value.copy(maxTokens = tokens)
    }

    fun setMaxAgentIterations(iters: Int) {
        prefs.edit().putInt(KEY_MAX_ITERATIONS, iters).apply()
        _settingsFlow.value = _settingsFlow.value.copy(maxAgentIterations = iters)
    }

    fun setParallelToolCalls(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_PARALLEL_TOOLS, enabled).apply()
        _settingsFlow.value = _settingsFlow.value.copy(parallelToolCalls = enabled)
    }

    fun setConfirmDangerousActions(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_CONFIRM_DANGEROUS, enabled).apply()
        _settingsFlow.value = _settingsFlow.value.copy(confirmDangerousActions = enabled)
    }

    fun setAllowMcp(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ALLOW_MCP, enabled).apply()
        _settingsFlow.value = _settingsFlow.value.copy(allowMcp = enabled)
    }

    fun setAllowNetworkTools(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ALLOW_NETWORK, enabled).apply()
        _settingsFlow.value = _settingsFlow.value.copy(allowNetworkTools = enabled)
    }

    fun setAutoSendToolResults(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_SEND_TOOLS, enabled).apply()
        _settingsFlow.value = _settingsFlow.value.copy(autoSendToolResults = enabled)
    }

    fun setLanguage(lang: String) {
        prefs.edit().putString(KEY_LANGUAGE, lang).apply()
        _settingsFlow.value = _settingsFlow.value.copy(language = lang)
    }

    fun clearAllPreferences() {
        prefs.edit().clear().apply()
        _settingsFlow.value = loadSettings()
    }

    companion object {
        private const val KEY_API_KEY_ENCRYPTED = "api_key_enc"
        private const val KEY_MODEL = "selected_model"
        private const val KEY_ROUTER_FREE = "is_router_free"
        private const val KEY_TEMPERATURE = "temperature"
        private const val KEY_MAX_TOKENS = "max_tokens"
        private const val KEY_MAX_ITERATIONS = "max_iterations"
        private const val KEY_PARALLEL_TOOLS = "parallel_tools"
        private const val KEY_CONFIRM_DANGEROUS = "confirm_dangerous"
        private const val KEY_ALLOW_MCP = "allow_mcp"
        private const val KEY_ALLOW_NETWORK = "allow_network"
        private const val KEY_AUTO_SEND_TOOLS = "auto_send_tools"
        private const val KEY_LANGUAGE = "app_language"
    }
}
