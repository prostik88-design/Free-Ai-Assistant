package com.example.data.remote.github

import com.example.data.parser.ParsedSkill
import com.example.data.parser.SkillParseResult
import com.example.data.parser.SkillParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

sealed class GitHubImportResult {
    data class Preview(
        val sourceUrl: String,
        val fileName: String,
        val rawContent: String,
        val parsedSkill: ParsedSkill
    ) : GitHubImportResult()

    data class Error(val message: String) : GitHubImportResult()
}

class GitHubSkillImporter(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
) {
    /**
     * Resolves and fetches skill file from a given GitHub or raw URL.
     */
    suspend fun fetchSkillFromUrl(inputUrl: String): GitHubImportResult = withContext(Dispatchers.IO) {
        val trimmed = inputUrl.trim()
        if (trimmed.isEmpty()) {
            return@withContext GitHubImportResult.Error("URL cannot be empty.")
        }

        // Convert GitHub web URLs to raw URLs
        val candidateUrls = resolveCandidateUrls(trimmed)
        if (candidateUrls.isEmpty()) {
            return@withContext GitHubImportResult.Error("Unsupported or malformed URL: $trimmed")
        }

        var lastError = "Could not fetch content."
        for ((url, candidateFileName) in candidateUrls) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "RouterAgent-Android")
                    .get()
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string().orEmpty()
                    if (body.isBlank()) {
                        lastError = "Response at $url was empty."
                        continue
                    }

                    // Security check: Never execute code; ensure safe file size & format
                    when (val parseResult = SkillParser.parse(body, candidateFileName)) {
                        is SkillParseResult.Success -> {
                            return@withContext GitHubImportResult.Preview(
                                sourceUrl = trimmed,
                                fileName = candidateFileName,
                                rawContent = body,
                                parsedSkill = parseResult.skill
                            )
                        }
                        is SkillParseResult.Error -> {
                            lastError = parseResult.message
                        }
                    }
                } else {
                    lastError = "HTTP ${response.code} fetching $url"
                }
            } catch (e: Exception) {
                lastError = e.localizedMessage ?: "Network connection failed"
            }
        }

        GitHubImportResult.Error("Failed to import skill: $lastError")
    }

    private fun resolveCandidateUrls(url: String): List<Pair<String, String>> {
        val list = mutableListOf<Pair<String, String>>()

        // If raw URL directly
        if (url.startsWith("https://raw.githubusercontent.com/")) {
            val fileName = url.substringAfterLast('/')
            list.add(url to fileName)
            return list
        }

        // If GitHub tree/blob URL:
        // https://github.com/user/repo/tree/main/skills/example
        // https://github.com/user/repo/blob/main/SKILL.md
        if (url.contains("github.com/") && (url.contains("/blob/") || url.contains("/tree/"))) {
            val rawBlob = url.replace("github.com/", "raw.githubusercontent.com/")
                .replace("/blob/", "/")
                .replace("/tree/", "/")

            if (url.endsWith(".md") || url.endsWith(".json") || url.endsWith(".yaml") || url.endsWith(".yml")) {
                list.add(rawBlob to url.substringAfterLast('/'))
            } else {
                list.add("$rawBlob/SKILL.md" to "SKILL.md")
                list.add("$rawBlob/skill.json" to "skill.json")
                list.add("$rawBlob/skill.yaml" to "skill.yaml")
                list.add("$rawBlob/README.md" to "README.md")
            }
            return list
        }

        // If base repository URL:
        // https://github.com/user/repo or https://github.com/user/repo.git
        if (url.contains("github.com/")) {
            val clean = url.removeSuffix(".git").removeSuffix("/")
            val parts = clean.substringAfter("github.com/").split("/")
            if (parts.size >= 2) {
                val owner = parts[0]
                val repo = parts[1]
                val branches = listOf("main", "master")
                for (b in branches) {
                    list.add("https://raw.githubusercontent.com/$owner/$repo/$b/SKILL.md" to "SKILL.md")
                    list.add("https://raw.githubusercontent.com/$owner/$repo/$b/skills/SKILL.md" to "SKILL.md")
                    list.add("https://raw.githubusercontent.com/$owner/$repo/$b/skill.json" to "skill.json")
                    list.add("https://raw.githubusercontent.com/$owner/$repo/$b/skill.yaml" to "skill.yaml")
                    list.add("https://raw.githubusercontent.com/$owner/$repo/$b/README.md" to "README.md")
                }
            }
        }

        return list
    }
}
