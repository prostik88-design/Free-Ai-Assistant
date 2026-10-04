package com.example.data.parser

import org.json.JSONArray
import org.json.JSONObject

data class ParsedSkill(
    val name: String,
    val description: String,
    val systemPrompt: String,
    val toolsAllowed: List<String> = emptyList(),
    val inputSchema: String? = null,
    val outputFormat: String? = null
)

sealed class SkillParseResult {
    data class Success(val skill: ParsedSkill) : SkillParseResult()
    data class Error(val message: String) : SkillParseResult()
}

object SkillParser {
    private const val MAX_FILE_SIZE = 500 * 1024 // 500 KB limit

    /**
     * Parses skill text content which can be SKILL.md, JSON, or YAML.
     */
    fun parse(content: String, fileName: String = ""): SkillParseResult {
        if (content.length > MAX_FILE_SIZE) {
            return SkillParseResult.Error("Skill file exceeds maximum size limit (500KB).")
        }

        val trimmed = content.trim()
        if (trimmed.isEmpty()) {
            return SkillParseResult.Error("Content is empty.")
        }

        // Check if executable file or script
        val lowerName = fileName.lowercase()
        val dangerousExtensions = listOf(".sh", ".exe", ".dex", ".apk", ".so", ".py", ".bin", ".bat")
        if (dangerousExtensions.any { lowerName.endsWith(it) }) {
            return SkillParseResult.Error("Executable files ($fileName) are strictly prohibited for safety.")
        }

        return when {
            trimmed.startsWith("{") -> parseJson(trimmed)
            trimmed.startsWith("---") -> parseFrontmatter(trimmed)
            else -> parseYamlOrMarkdown(trimmed)
        }
    }

    private fun parseJson(jsonString: String): SkillParseResult {
        return try {
            val json = JSONObject(jsonString)
            val name = json.optString("name", "").trim()
            val description = json.optString("description", "").trim()
            val systemPrompt = json.optString("system_prompt", json.optString("prompt", "")).trim()
            val outputFormat = if (json.has("output_format")) json.optString("output_format") else null
            val inputSchema = if (json.has("input_schema")) json.optString("input_schema") else null

            val toolsAllowed = mutableListOf<String>()
            val toolsArr = json.optJSONArray("tools_allowed") ?: json.optJSONArray("tools")
            if (toolsArr != null) {
                for (i in 0 until toolsArr.length()) {
                    toolsAllowed.add(toolsArr.getString(i).trim())
                }
            }

            validateAndBuild(name, description, systemPrompt, toolsAllowed, inputSchema, outputFormat)
        } catch (e: Exception) {
            SkillParseResult.Error("Invalid JSON format: ${e.localizedMessage}")
        }
    }

    private fun parseFrontmatter(text: String): SkillParseResult {
        // Form:
        // ---
        // key: val
        // ---
        // markdown body
        val parts = text.split("---", limit = 3)
        if (parts.size < 3) {
            return SkillParseResult.Error("Invalid SKILL.md frontmatter syntax. Missing closing '---'.")
        }

        val frontmatter = parts[1]
        val body = parts[2].trim()

        val parsedMap = parseSimpleYaml(frontmatter)
        if (parsedMap.containsKey("_error")) {
            return SkillParseResult.Error(parsedMap["_error"] as String)
        }

        val name = (parsedMap["name"] as? String).orEmpty().trim()
        val description = (parsedMap["description"] as? String).orEmpty().trim()
        var systemPrompt = (parsedMap["system_prompt"] as? String).orEmpty().trim()
        if (systemPrompt.isEmpty()) {
            systemPrompt = body
        }
        val outputFormat = parsedMap["output_format"] as? String
        val inputSchema = parsedMap["input_schema"] as? String

        @Suppress("UNCHECKED_CAST")
        val toolsAllowed = (parsedMap["tools_allowed"] as? List<String>)
            ?: (parsedMap["tools"] as? List<String>)
            ?: emptyList()

        return validateAndBuild(name, description, systemPrompt, toolsAllowed, inputSchema, outputFormat)
    }

    private fun parseYamlOrMarkdown(text: String): SkillParseResult {
        val parsedMap = parseSimpleYaml(text)
        if (parsedMap.containsKey("_error")) {
            return SkillParseResult.Error(parsedMap["_error"] as String)
        }

        val name = (parsedMap["name"] as? String).orEmpty().trim()
        val description = (parsedMap["description"] as? String).orEmpty().trim()
        val systemPrompt = (parsedMap["system_prompt"] as? String).orEmpty().trim()
        val outputFormat = parsedMap["output_format"] as? String
        val inputSchema = parsedMap["input_schema"] as? String

        @Suppress("UNCHECKED_CAST")
        val toolsAllowed = (parsedMap["tools_allowed"] as? List<String>)
            ?: (parsedMap["tools"] as? List<String>)
            ?: emptyList()

        if (name.isEmpty() && description.isEmpty()) {
            return SkillParseResult.Error("Invalid YAML/Skill format. Could not find 'name' or 'description'.")
        }

        return validateAndBuild(name, description, systemPrompt, toolsAllowed, inputSchema, outputFormat)
    }

    private fun validateAndBuild(
        name: String,
        description: String,
        systemPrompt: String,
        toolsAllowed: List<String>,
        inputSchema: String?,
        outputFormat: String?
    ): SkillParseResult {
        if (name.isBlank()) {
            return SkillParseResult.Error("Validation Error: Skill 'name' is required and cannot be empty.")
        }
        if (description.isBlank()) {
            return SkillParseResult.Error("Validation Error: Skill 'description' is required and cannot be empty.")
        }

        val prompt = if (systemPrompt.isBlank()) {
            "You are a specialized agent assistant equipped with the skill '$name'. Follow these instructions:\n$description"
        } else {
            systemPrompt
        }

        return SkillParseResult.Success(
            ParsedSkill(
                name = name,
                description = description,
                systemPrompt = prompt,
                toolsAllowed = toolsAllowed.filter { it.isNotBlank() },
                inputSchema = inputSchema,
                outputFormat = outputFormat
            )
        )
    }

    /**
     * Robust key-value YAML parser for Skills.
     */
    private fun parseSimpleYaml(yaml: String): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        val lines = yaml.lines()
        var currentKey: String? = null
        var isMultiline = false
        val multilineBuffer = StringBuilder()
        var isList = false
        val listBuffer = mutableListOf<String>()

        fun flushCurrent() {
            if (currentKey != null) {
                if (isMultiline) {
                    result[currentKey!!] = multilineBuffer.toString().trim()
                    multilineBuffer.clear()
                    isMultiline = false
                } else if (isList) {
                    result[currentKey!!] = listBuffer.toList()
                    listBuffer.clear()
                    isList = false
                }
            }
        }

        for (rawLine in lines) {
            val line = rawLine.trimEnd()
            val trimmed = line.trim()

            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue

            if (isMultiline) {
                // If line starts with lower indentation or new key
                if (rawLine.startsWith("  ") || rawLine.startsWith("\t") || trimmed.startsWith("-")) {
                    multilineBuffer.append(trimmed).append("\n")
                    continue
                } else if (line.contains(":") && !trimmed.startsWith("-")) {
                    flushCurrent()
                } else {
                    multilineBuffer.append(trimmed).append("\n")
                    continue
                }
            }

            if (isList) {
                if (trimmed.startsWith("- ")) {
                    listBuffer.add(trimmed.removePrefix("- ").trim().trim('"', '\''))
                    continue
                } else if (line.contains(":")) {
                    flushCurrent()
                }
            }

            val colonIdx = line.indexOf(':')
            if (colonIdx != -1) {
                flushCurrent()
                val key = line.substring(0, colonIdx).trim()
                val value = line.substring(colonIdx + 1).trim()

                currentKey = key
                if (value == "|" || value == ">-" || value == ">") {
                    isMultiline = true
                    multilineBuffer.clear()
                } else if (value.isEmpty()) {
                    // Might be starting a list
                    isList = true
                    listBuffer.clear()
                } else if (value.startsWith("[") && value.endsWith("]")) {
                    // Inline JSON array
                    try {
                        val arr = JSONArray(value)
                        val items = mutableListOf<String>()
                        for (i in 0 until arr.length()) items.add(arr.getString(i))
                        result[key] = items
                        currentKey = null
                    } catch (e: Exception) {
                        result["_error"] = "Malformed list syntax in '$key': $value"
                        return result
                    }
                } else {
                    result[key] = value.trim('"', '\'')
                    currentKey = null
                }
            }
        }
        flushCurrent()
        return result
    }
}
