package com.word2prompt.android.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.word2prompt.android.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

class Word2PromptViewModel(application: Application) : AndroidViewModel(application) {
    private val api = W2PApi(application)
    private val prefs = application.getSharedPreferences("w2p_library", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(W2PState())
    val state: StateFlow<W2PState> = _state.asStateFlow()

    init { loadLibrary(); refreshAccount(); refreshTrends() }

    fun refreshAccount() {
        viewModelScope.launch {
            runCatching { api.session() }.onSuccess { session ->
                _state.value = _state.value.copy(session = session)
                runCatching { api.usage() }.onSuccess { usage -> _state.value = _state.value.copy(usage = usage) }
            }
        }
    }

    fun login(email: String, password: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            runCatching { api.login(email.trim(), password) }
                .onSuccess { session -> _state.value = _state.value.copy(session = session, billingMessage = "Signed in successfully.", error = null); refreshUsage(); onDone() }
                .onFailure { _state.value = _state.value.copy(error = it.message ?: "Sign in failed") }
        }
    }

    fun register(email: String, password: String, name: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            runCatching { api.register(email.trim(), password, name.trim()) }
                .onSuccess { session -> _state.value = _state.value.copy(session = session, billingMessage = "Account created. Your free prompts are ready.", error = null); refreshUsage(); onDone() }
                .onFailure { _state.value = _state.value.copy(error = it.message ?: "Account creation failed") }
        }
    }

    fun logout(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            runCatching { api.logout() }.onSuccess { _state.value = _state.value.copy(session = SessionState(), usage = UsageState(), billingMessage = "Signed out.", error = null); onDone() }
                .onFailure { _state.value = _state.value.copy(error = it.message ?: "Sign out failed") }
        }
    }

    fun refreshTrends() {
        viewModelScope.launch {
            runCatching { api.trends() }.onSuccess { (updatedAt, items) ->
                _state.value = _state.value.copy(trends = items.take(6), trendsUpdatedAt = updatedAt)
            }
        }
    }

    fun refreshUsage() {
        viewModelScope.launch { runCatching { api.usage() }.onSuccess { _state.value = _state.value.copy(usage = it) } }
    }

    fun createBillingOrder(kind: String) {
        viewModelScope.launch {
            runCatching { api.createBillingOrder(kind) }.onSuccess { order ->
                val amount = "₹${order.amountPaise / 100}"
                _state.value = _state.value.copy(billingMessage = "Secure checkout order ${order.orderId.take(8)}… created for $amount. Connect the configured payment provider to complete payment.", error = null)
            }.onFailure { _state.value = _state.value.copy(error = it.message ?: "Checkout could not be started") }
        }
    }

    fun setIdea(value: String) {
        _state.value = _state.value.copy(idea = value, commandSuggestions = emptyList(), error = null, plan = null, finalPrompt = "", pipelineStage = PipelineStage.IDLE, pipelineMessage = "")
    }
    fun setOutput(value: String) { _state.value = _state.value.copy(output = value) }
    fun setDestination(value: String) { _state.value = _state.value.copy(destination = value) }
    fun setSourceImage(dataUrl: String?, fileName: String?) {
        _state.value = _state.value.copy(sourceImageDataUrl = dataUrl, sourceImageName = fileName, error = null)
    }
    fun clearSourceImage() { _state.value = _state.value.copy(sourceImageDataUrl = null, sourceImageName = null) }
    private fun stage(stage: PipelineStage, message: String) {
        val now = System.currentTimeMillis()
        val started = _state.value.pipelineStartedAt ?: now
        _state.value = _state.value.copy(pipelineStage = stage, pipelineMessage = message, pipelineStartedAt = started, pipelineElapsedMs = now - started)
    }
    fun setAnswer(key: String, value: String) {
        _state.value = _state.value.copy(answers = _state.value.answers + (key to value))
    }

    fun startFromTrend(trend: TrendingItem) {
        val output = when {
            trend.category.contains("video", true) -> "Video"
            trend.category.contains("writing", true) || trend.category.contains("content", true) -> "Writing"
            else -> "Image"
        }
        _state.value = _state.value.copy(idea = trend.hook, output = output, destination = "Let W2P choose", error = null)
    }

    fun saveCurrentToLibrary() {
        val s = _state.value
        if (s.finalPrompt.isBlank()) return
        val quality = s.promptQuality?.let { "${it.grade} • ${it.score}" } ?: "Ready"
        val item = LibraryItem(
            id = java.util.UUID.randomUUID().toString(),
            title = s.idea.trim().replace("\s+".toRegex(), " ").take(58).ifBlank { "W2P creation" },
            preview = s.finalPrompt.trim().take(260),
            prompt = s.finalPrompt,
            output = s.output,
            destination = s.destination,
            quality = quality,
            savedAt = System.currentTimeMillis()
        )
        val updated = listOf(item) + _state.value.library.filterNot { it.id == item.id }.take(19)
        _state.value = _state.value.copy(library = updated, error = null)
        persistLibrary(updated)
    }

    fun openLibraryItem(item: LibraryItem) {
        _state.value = _state.value.copy(
            idea = item.title,
            output = item.output,
            destination = item.destination,
            finalPrompt = item.prompt,
            promptQuality = null,
            error = null
        )
    }

    private fun loadLibrary() {
        val raw = prefs.getString("items", "").orEmpty()
        if (raw.isBlank()) return
        runCatching {
            val arr = org.json.JSONArray(raw)
            val items = buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(LibraryItem(o.optString("id"), o.optString("title"), o.optString("preview"), o.optString("prompt"), o.optString("output", "Let W2P choose"), o.optString("destination", "Let W2P choose"), o.optString("quality", "Ready"), o.optLong("savedAt")))
                }
            }
            _state.value = _state.value.copy(library = items)
        }
    }

    private fun persistLibrary(items: List<LibraryItem>) {
        val arr = org.json.JSONArray()
        items.take(20).forEach { item ->
            arr.put(org.json.JSONObject().apply {
                put("id", item.id); put("title", item.title); put("preview", item.preview); put("prompt", item.prompt); put("output", item.output); put("destination", item.destination); put("quality", item.quality); put("savedAt", item.savedAt)
            })
        }
        prefs.edit().putString("items", arr.toString()).apply()
    }

    fun suggest(prefix: String) {
        if (!prefix.startsWith("/")) return
        viewModelScope.launch {
            runCatching { api.commands(prefix.removePrefix("/")) }
                .onSuccess { _state.value = _state.value.copy(commandSuggestions = it.take(8)) }
        }
    }

    fun buildPlan(onDone: () -> Unit = {}) {
        val s0 = _state.value
        if (s0.idea.isBlank()) {
            _state.value = s0.copy(error = "Tell W2P what you want to accomplish first.")
            return
        }
        _state.value = s0.copy(error = null, pipelineStage = PipelineStage.PLANNING, pipelineMessage = "Locking intent and checking what is actually missing…")
        viewModelScope.launch {
            val s = _state.value
            runCatching { api.intentEngine(s.idea, s.output, s.destination, s.answers) }
                .onSuccess { analysis ->
                    _state.value = _state.value.copy(intentAnalysis = analysis, smartQuestions = analysis.questions, pipelineStage = PipelineStage.PLANNING, pipelineMessage = "Intent ${analysis.confidence}% • completeness ${analysis.completeness}%")
                }
                .onFailure { _state.value = _state.value.copy(smartQuestions = smartQuestions(s.idea, s.answers), error = "Intent Engine unavailable; using local question fallback.") }
            val latest = _state.value
            val p = api.plan(latest.idea, latest.output, latest.destination, latest.answers)
            if (p.ok) {
                _state.value = _state.value.copy(plan = p.plan, error = null)
                onDone()
            } else {
                _state.value = _state.value.copy(error = p.error ?: "Planning failed")
            }
        }
    }

    fun buildResult(onDone: () -> Unit = {}) {
        val s0 = _state.value
        if (s0.idea.isBlank()) {
            _state.value = s0.copy(error = "Tell W2P what you want to create first.", pipelineStage = PipelineStage.BLOCKED, pipelineMessage = "A clear requirement is needed before creation.")
            return
        }
        viewModelScope.launch {
            val started = System.currentTimeMillis()
            _state.value = s0.copy(pipelineStage = PipelineStage.CHECKING_USAGE, pipelineMessage = "Checking your creation allowance…", pipelineStartedAt = started, pipelineElapsedMs = 0, error = null)
            runCatching { api.consumePrompt() }.onFailure {
                _state.value = _state.value.copy(pipelineStage = PipelineStage.BLOCKED, pipelineMessage = "Usage check failed.", error = it.message ?: "We could not verify your usage allowance.", pipelineElapsedMs = System.currentTimeMillis() - started)
            }.onSuccess { gate ->
                if (!gate.ok) {
                    _state.value = _state.value.copy(usage = gate.usage, usageSource = gate.source, pipelineStage = PipelineStage.BLOCKED, pipelineMessage = "Creation allowance reached.", error = gate.message.ifBlank { "Your creation allowance is currently unavailable." }, pipelineElapsedMs = System.currentTimeMillis() - started)
                    return@onSuccess
                }
                _state.value = _state.value.copy(usage = gate.usage, usageSource = gate.source, pipelineStage = PipelineStage.BUILDING_CONTEXT, pipelineMessage = "Understanding intent and protecting what matters…", error = null)
                runCatching {
                    val s = _state.value
                    val intelligence = api.intelligence(s.idea, s.output, s.destination, s.answers, s.sourceImageDataUrl)
                    stage(PipelineStage.RESEARCHING, "Checking whether fresh research is actually useful…")
                    val research = runCatching { api.research(s.idea, s.output, s.destination, s.answers) }.getOrNull()
                    stage(PipelineStage.BUILDING_CREATIVE_PLAN, "Building the creation strategy…")
                    val creation = runCatching { api.creationIntelligence(s.idea, s.output, s.destination, s.answers, research, intelligence) }.getOrNull()
                    val base = guaranteedPrompt(s.idea, s.output, s.destination, s.answers)
                    stage(PipelineStage.GENERATING_PROMPT, "Generating the destination-ready prompt…")
                    val meta = runCatching { api.promptIntelligence(base, s.idea, s.output, s.destination, s.answers, research, intelligence, creation) }.getOrNull()
                    val final = meta?.prompt ?: base
                    stage(PipelineStage.QUALITY_CHECK, "Running prompt quality checks…")
                    val quality = runCatching { api.promptQuality(final, s.idea, s.output, s.destination) }.getOrNull()
                    _state.value = _state.value.copy(
                        intelligence = intelligence, research = research, creationPlan = creation,
                        promptMeta = meta ?: PromptMeta("local-fallback", "Local fallback", "Preserve customer intent.", listOf("Model-backed Prompt Intelligence was unavailable for this generation."), emptyList(), base),
                        promptQuality = quality, finalPrompt = final, pipelineStage = PipelineStage.READY,
                        pipelineMessage = "Creation package ready for your review.", pipelineElapsedMs = System.currentTimeMillis() - started, error = null
                    )
                }.onFailure {
                    val fallback = guaranteedPrompt(_state.value.idea, _state.value.output, _state.value.destination, _state.value.answers)
                    _state.value = _state.value.copy(finalPrompt = fallback, pipelineStage = PipelineStage.FAILED, pipelineMessage = "Generation completed with a local fallback.", pipelineElapsedMs = System.currentTimeMillis() - started, error = it.message ?: "Generation completed with fallback.")
                }
                onDone()
            }
        }
    }


    fun inspectResult(resultImageDataUrl: String, fileName: String, onDone: () -> Unit = {}) {
        val s = _state.value
        if (s.finalPrompt.isBlank()) return
        viewModelScope.launch {
            _state.value = _state.value.copy(inspectionBusy = true, error = null)
            runCatching {
                api.resultIntelligence(
                    prompt = s.finalPrompt,
                    idea = s.idea,
                    intent = s.intelligence?.intent ?: "create",
                    resultImage = resultImageDataUrl,
                    creativeContext = JSONObject().put("output", s.output).put("destination", s.destination)
                )
            }.onSuccess { report ->
                _state.value = _state.value.copy(resultIntelligence = report, inspectedImageName = fileName, inspectionBusy = false, error = null)
                runCatching { api.ghostCheck(s.idea, s.finalPrompt, s.output, s.destination, resultImageDataUrl, JSONObject().put("score", report.score).put("mismatches", org.json.JSONArray(report.mismatches)).put("uncertain", org.json.JSONArray(report.uncertain)).put("suggested_revision", report.suggestedRevision)) }.onSuccess { check -> _state.value = _state.value.copy(ghostCheck = check) }
                onDone()
            }.onFailure { _state.value = _state.value.copy(inspectionBusy = false, error = it.message ?: "Result inspection failed") }
        }
    }

    fun autoFixFromInspection(onDone: () -> Unit = {}) {
        val s = _state.value
        val report = s.resultIntelligence ?: return
        viewModelScope.launch {
            runCatching {
                api.autoFix(
                    prompt = s.finalPrompt,
                    mismatches = report.mismatches,
                    suggestedRevision = report.suggestedRevision,
                    creativeContext = JSONObject().put("output", s.output).put("destination", s.destination)
                )
            }.onSuccess { revised ->
                _state.value = _state.value.copy(
                    finalPrompt = revised,
                    promptQuality = null,
                    error = null
                )
                onDone()
            }.onFailure { _state.value = _state.value.copy(error = it.message ?: "Auto-fix failed") }
        }
    }

    fun runGhostCheck() {
        val s = _state.value
        viewModelScope.launch {
            runCatching { api.ghostCheck(s.idea, s.finalPrompt, s.output, s.destination, "", null) }
                .onSuccess { _state.value = _state.value.copy(ghostCheck = it, error = null) }
                .onFailure { _state.value = _state.value.copy(error = it.message ?: "Ghost check failed") }
        }
    }

    fun improve() {
        val s = _state.value
        if (s.resultIntelligence?.mismatches?.isNotEmpty() == true) {
            autoFixFromInspection()
            return
        }
        val add = """

QUALITY PASS
- Preserve the customer's exact core intent, named subjects and requested output.
- Strengthen only observable details that improve clarity and destination compatibility.
- Resolve contradictions before generation.
- Do not invent missing facts, claims, people, locations, prices, logos or story events.
""".trimIndent()
        _state.value = s.copy(finalPrompt = s.finalPrompt.trimEnd() + "\n\n" + add)
    }

    fun createJob(onDone: () -> Unit = {}) {
        val s = _state.value
        if (s.idea.isBlank() || s.finalPrompt.isBlank()) return
        viewModelScope.launch {
            runCatching { api.createJob(s.idea, s.finalPrompt, s.output, s.destination) }
                .onSuccess { obj ->
                    val job = obj.optJSONObject("job")
                    _state.value = _state.value.copy(creationJobId = job?.optString("id"), creationStatus = job?.optString("status"), error = null)
                    onDone()
                }
                .onFailure { _state.value = _state.value.copy(error = it.message ?: "Creation could not be prepared") }
        }
    }

    fun approveJob(onDone: () -> Unit = {}) {
        val id = _state.value.creationJobId ?: return
        viewModelScope.launch {
            runCatching { api.approveJob(id) }
                .onSuccess { obj -> _state.value = _state.value.copy(creationStatus = obj.optJSONObject("job")?.optString("status")); onDone() }
                .onFailure { _state.value = _state.value.copy(error = it.message) }
        }
    }

    fun executeJob(onDone: () -> Unit = {}) {
        val id = _state.value.creationJobId ?: return
        viewModelScope.launch {
            runCatching { api.executeJob(id) }
                .onSuccess { obj -> _state.value = _state.value.copy(creationStatus = obj.optJSONObject("job")?.optString("status")); onDone() }
                .onFailure { _state.value = _state.value.copy(error = it.message) }
        }
    }

    fun reset() { _state.value = W2PState() }

    private fun smartQuestions(idea: String, answers: Map<String, String>): List<SmartQuestion> {
        val t = idea.lowercase()
        fun known(key: String, vararg terms: String) = !answers[key].isNullOrBlank() || terms.any { t.contains(it) }
        val qs = mutableListOf<SmartQuestion>()
        if (!known("audience", "for customers", "for students", "children", "professionals", "audience")) qs += SmartQuestion("audience", "Who is this for?", "Audience changes language, visuals and calls to action.", listOf("General audience", "Customers", "Professionals", "Students", "Children", "Let W2P choose"))
        if (!known("platform", "instagram", "youtube", "google", "website", "facebook", "linkedin", "print")) qs += SmartQuestion("platform", "Where will this be used?", "Destination affects format and optimization.", listOf("Instagram", "YouTube", "Google", "Website", "Presentation", "Print", "Other"))
        if (!known("goal", "sell", "sales", "educate", "engage", "lead", "awareness", "promote")) qs += SmartQuestion("goal", "What result should it achieve?", "The success criterion changes how W2P structures the result.", listOf("Sell", "Inform", "Educate", "Engage", "Build awareness", "Generate leads", "Let W2P choose"))
        if (!known("style", "cinematic", "minimal", "premium", "professional", "bold", "playful", "luxury")) qs += SmartQuestion("style", "Any creative direction?", "Style materially affects the final creative output.", listOf("Premium", "Minimal", "Cinematic", "Playful", "Professional", "Bold", "Let W2P choose"))
        return qs.take(4)
    }

    private fun guaranteedPrompt(idea: String, output: String, destination: String, answers: Map<String, String>): String = buildString {
        appendLine("Create a high-quality result based strictly on the customer requirement below.")
        appendLine()
        appendLine("CUSTOMER REQUIREMENT")
        appendLine("Original idea: “$idea”.")
        if (output.isNotBlank()) appendLine("Requested output: $output.")
        answers.forEach { (k, v) -> if (v.isNotBlank()) appendLine("${k.replaceFirstChar { it.uppercase() }}: $v.") }
        if (destination != "Let W2P choose") appendLine("Destination: $destination.")
        appendLine()
        appendLine("INTENT")
        appendLine("Preserve the customer's requested subject, action, setting, identity and purpose. Do not silently change the idea.")
        appendLine()
        appendLine("QUALITY")
        appendLine("Use clear structure, coherent details, professional quality and restrained defaults only where necessary.")
        appendLine()
        appendLine("CONSTRAINTS")
        appendLine("Do not invent major people, subjects, locations, products, claims, dates, prices, story events, brands, text or capabilities. Respect every customer constraint.")
    }
}
