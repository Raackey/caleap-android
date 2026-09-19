package com.word2prompt.android.data

import org.json.JSONArray
import org.json.JSONObject

data class CommandSuggestion(val name: String, val label: String = name, val description: String = "")
data class TaskNode(val id: String, val type: String, val title: String, val action: String, val requiresApproval: Boolean)
data class PlanData(
    val taskType: String,
    val depth: String,
    val goal: String,
    val nextAction: String,
    val capabilities: List<String>,
    val nodes: List<TaskNode>,
    val researchAngles: List<String>
)
data class IntelligenceData(
    val summary: String,
    val intent: String,
    val intentConfidence: Int,
    val output: String,
    val destination: String,
    val assets: Int,
    val protect: List<String>,
    val openDecisions: List<String>
)
data class CreationPlanData(
    val summary: String,
    val readiness: String,
    val nextAction: String,
    val steps: List<String>,
    val blockers: List<String>,
    val risks: List<String>,
    val capabilities: List<String> = emptyList(),
    val successCriteria: List<String> = emptyList()
)
data class PromptMeta(
    val mode: String,
    val summary: String,
    val strategy: String,
    val warnings: List<String>,
    val assumptions: List<String>,
    val prompt: String
)
data class ResearchData(
    val needed: Boolean,
    val confidence: String,
    val topics: List<String>,
    val queries: List<String>,
    val sources: List<Pair<String, String>>
)
data class GhostCheckData(
    val ok: Boolean,
    val status: String,
    val blockers: List<String>,
    val warnings: List<String>,
    val nextAction: String
)
data class ResultIntelligenceData(
    val summary: String,
    val score: Int?,
    val confidence: String,
    val mismatches: List<String>,
    val protected: List<String>,
    val uncertain: List<String>,
    val suggestedRevision: String,
    val overall: String
)
data class PromptQualityData(
    val score: Int,
    val grade: String,
    val strengths: List<String>,
    val issues: List<String>
)
data class SmartQuestion(
    val key: String,
    val question: String,
    val reason: String,
    val options: List<String>
)


data class IntentAnalysis(
    val intentName: String,
    val confidence: Int,
    val completeness: Int,
    val missing: List<String>,
    val questions: List<SmartQuestion>,
    val researchNeeded: Boolean,
    val researchReason: String,
    val briefOutput: String,
    val briefDestination: String,
    val briefGoal: String?,
    val briefAudience: String?
)

data class PlanResponse(val ok: Boolean, val plan: PlanData?, val error: String?)
data class LibraryItem(
    val id: String,
    val title: String,
    val preview: String,
    val prompt: String,
    val output: String,
    val destination: String,
    val quality: String,
    val savedAt: Long
)

data class TrendingItem(
    val id: String,
    val title: String,
    val category: String,
    val hook: String,
    val score: Int
)

enum class PipelineStage { IDLE, CHECKING_USAGE, PLANNING, BUILDING_CONTEXT, RESEARCHING, BUILDING_CREATIVE_PLAN, GENERATING_PROMPT, QUALITY_CHECK, READY, BLOCKED, FAILED }

data class W2PState(
    val idea: String = "",
    val output: String = "Let W2P choose",
    val destination: String = "Let W2P choose",
    val answers: Map<String, String> = emptyMap(),
    val commandSuggestions: List<CommandSuggestion> = emptyList(),
    val plan: PlanData? = null,
    val smartQuestions: List<SmartQuestion> = emptyList(),
    val intentAnalysis: IntentAnalysis? = null,
    val intelligence: IntelligenceData? = null,
    val creationPlan: CreationPlanData? = null,
    val promptMeta: PromptMeta? = null,
    val research: ResearchData? = null,
    val promptQuality: PromptQualityData? = null,
    val resultIntelligence: ResultIntelligenceData? = null,
    val ghostCheck: GhostCheckData? = null,
    val inspectionBusy: Boolean = false,
    val inspectedImageName: String? = null,
    val finalPrompt: String = "",
    val creationJobId: String? = null,
    val creationStatus: String? = null,
    val error: String? = null,
    val session: SessionState = SessionState(),
    val usage: UsageState = UsageState(),
    val billingMessage: String? = null,
    val trends: List<TrendingItem> = emptyList(),
    val trendsUpdatedAt: String? = null,
    val library: List<LibraryItem> = emptyList(),
    val pipelineStage: PipelineStage = PipelineStage.IDLE,
    val pipelineMessage: String = "",
    val pipelineStartedAt: Long? = null,
    val pipelineElapsedMs: Long? = null,
    val sourceImageDataUrl: String? = null,
    val sourceImageName: String? = null,
    val usageSource: String? = null
)

fun JSONArray.strings(): List<String> = buildList {
    for (i in 0 until length()) optString(i).trim().takeIf { it.isNotEmpty() }?.let(::add)
}

