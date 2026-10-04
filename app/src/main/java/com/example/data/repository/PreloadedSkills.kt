package com.example.data.repository

import com.example.data.local.entity.SkillEntity
import java.util.UUID

object PreloadedSkills {
    fun getBuiltInSkills(): List<SkillEntity> {
        return listOf(
            SkillEntity(
                id = "skill_ponytail",
                name = "Ponytail UI Architect",
                description = "Modern Android & Jetpack Compose UI component and layout generator based on DietrichGebert/ponytail.",
                systemPrompt = """
                    You are a specialized Jetpack Compose and Android UI expert.
                    - Always use Material 3 components, proper edge-to-edge window insets, and responsive layout constraints.
                    - Follow clean composable separation and dynamic typography scales.
                    - Provide beautiful, production-ready code with preview annotations.
                """.trimIndent(),
                toolsAllowedJson = "[\"calculate\",\"web_search\"]",
                sourceUrl = "https://github.com/DietrichGebert/ponytail.git",
                isBuiltIn = true
            ),
            SkillEntity(
                id = "skill_anthropic",
                name = "Prompt Engineering & Analysis",
                description = "Deep chain-of-thought, task decomposition, and structural validation inspired by Anthropics skills library.",
                systemPrompt = """
                    You are an analytical reasoning assistant.
                    - Break down user inquiries into structured stages: Problem Statement, Constraints, Hypothesis, Step-by-Step Solution, and Final Verification.
                    - Provide clarity, identify edge cases, and eliminate ambiguity before generating final deliverables.
                """.trimIndent(),
                toolsAllowedJson = "[\"calculate\",\"get_current_time\"]",
                sourceUrl = "https://github.com/anthropics/skills.git",
                isBuiltIn = true
            ),
            SkillEntity(
                id = "skill_freellm",
                name = "Free LLM Router Optimizer",
                description = "Optimizes tool execution and prompts for openrouter/free models and free tier rate limits.",
                systemPrompt = """
                    You are an optimization specialist for free LLM routers like openrouter/free.
                    - Keep context concise and compact to avoid exceeding token windows.
                    - Formulate clear, explicit tool call parameters to guarantee deterministic JSON parsing on open weights models.
                """.trimIndent(),
                toolsAllowedJson = "[\"calculate\",\"get_current_time\",\"web_search\"]",
                sourceUrl = "https://github.com/tashfeenahmed/freellmapi.git",
                isBuiltIn = true
            ),
            SkillEntity(
                id = "skill_superpowers",
                name = "Autonomous Coding Superpowers",
                description = "Self-correcting, multi-step agent coding and refactoring workflow based on obra/superpowers.",
                systemPrompt = """
                    You possess agentic coding superpowers.
                    - Carefully read requirements before jumping to conclusions.
                    - Formulate test cases for critical user journeys.
                    - Leverage local and MCP tools to inspect, verify, and complete complex software tasks incrementally.
                """.trimIndent(),
                toolsAllowedJson = "[\"calculate\",\"get_current_time\",\"web_search\",\"dangerous_system_action\"]",
                sourceUrl = "https://github.com/obra/superpowers.git",
                isBuiltIn = true
            ),
            SkillEntity(
                id = "skill_awesome_apps",
                name = "Awesome LLM App Architect",
                description = "Recipes and architectural patterns for LLM workflows, RAG, and agentic systems from Shubhamsaboo/awesome-llm-apps.",
                systemPrompt = """
                    You are an LLM system architect.
                    - Recommend optimal model routers, agent topologies, and tool registries.
                    - Design clean data structures and state machines for autonomous workflows.
                """.trimIndent(),
                toolsAllowedJson = "[\"calculate\",\"web_search\"]",
                sourceUrl = "https://github.com/Shubhamsaboo/awesome-llm-apps.git",
                isBuiltIn = true
            )
        )
    }
}
