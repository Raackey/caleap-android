package com.word2prompt.android.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

private class PersistentCookieJar(context: Context) : CookieJar {
    private val prefs: SharedPreferences = context.getSharedPreferences("w2p_http", Context.MODE_PRIVATE)
    private val key = "cookies"

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val serialized = cookies.filter { it.expiresAt > System.currentTimeMillis() }.joinToString("\n") { it.toString() }
        if (serialized.isNotBlank()) prefs.edit().putString(key, serialized).apply()
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val raw = prefs.getString(key, "").orEmpty()
        if (raw.isBlank()) return emptyList()
        return raw.lines().mapNotNull { runCatching { Cookie.parse(url, it) }.getOrNull() }.filter { it.expiresAt > System.currentTimeMillis() }
    }

    fun clear() { prefs.edit().remove(key).apply() }
}

class W2PApi(context: Context, private val baseUrl: String = "https://www.word2prompt.in") {
    private val cookieJar = PersistentCookieJar(context.applicationContext)
    private val http = OkHttpClient.Builder().cookieJar(cookieJar).build()
    private val jsonType = "application/json; charset=utf-8".toMediaType()

    private suspend fun request(method: String, path: String, body: JSONObject? = null): JSONObject = withContext(Dispatchers.IO) {
        val builder = Request.Builder().url(baseUrl.trimEnd('/') + path)
        if (method == "POST") builder.post((body ?: JSONObject()).toString().toRequestBody(jsonType)) else builder.get()
        http.newCall(builder.build()).execute().use { response ->
            val text = response.body?.string().orEmpty()
            val obj = runCatching { JSONObject(text) }.getOrElse { JSONObject().put("error", text.ifBlank { "Server returned an unreadable response." }) }
            if (!response.isSuccessful) error(obj.optString("error", "Request failed (${response.code})"))
            obj
        }
    }

    suspend fun session(): SessionState {
        val obj = request("GET", "/api/session")
        val u = obj.optJSONObject("user")
        return SessionState(
            authenticated = obj.optBoolean("authenticated", false),
            user = u?.let { AccountUser(it.optString("id"), it.optString("email"), it.optString("fullName"), it.optString("mobile"), it.optString("preferredLanguage", "en")) }
        )
    }

    suspend fun register(email: String, password: String, fullName: String = ""): SessionState {
        val obj = request("POST", "/api/auth/register", JSONObject().put("email", email).put("password", password).put("fullName", fullName))
        return parseSession(obj)
    }

    suspend fun login(email: String, password: String): SessionState {
        val obj = request("POST", "/api/auth/login", JSONObject().put("email", email).put("password", password))
        return parseSession(obj)
    }

    suspend fun logout() {
        request("POST", "/api/auth/logout")
        cookieJar.clear()
    }

    private fun parseSession(obj: JSONObject): SessionState {
        val u = obj.optJSONObject("user") ?: return SessionState(obj.optBoolean("authenticated", false), null)
        return SessionState(true, AccountUser(u.optString("id"), u.optString("email"), u.optString("fullName"), u.optString("mobile"), u.optString("preferredLanguage", "en")))
    }

    suspend fun usage(): UsageState {
        val obj = request("GET", "/api/usage")
        val sub = obj.optJSONObject("subscription")
        val prices = obj.optJSONObject("prices")
        return UsageState(
            authenticated = obj.optBoolean("authenticated", false),
            freeRemaining = obj.optInt("freeRemaining", 0),
            paidCredits = obj.optInt("paidCredits", 0),
            subscriptionPlan = sub?.optString("plan"),
            subscriptionRenewsAt = sub?.optString("renews_at"),
            monthlyPricePaise = prices?.optInt("monthlyPaise", 4900) ?: 4900,
            yearlyPricePaise = prices?.optInt("yearlyPaise", 49900) ?: 49900,
            paygPricePaise = prices?.optInt("paygPaise", 100) ?: 100,
            canUse = obj.optBoolean("canUse", false)
        )
    }

    suspend fun consumePrompt(): UsageConsumeResult {
        val obj = request("POST", "/api/usage/consume", JSONObject())
        val status = obj.optJSONObject("status") ?: JSONObject()
        return UsageConsumeResult(
            ok = obj.optBoolean("ok", false),
            source = obj.optString("source", ""),
            remaining = if (obj.has("remaining") && !obj.isNull("remaining")) obj.optInt("remaining") else null,
            message = obj.optString("message", ""),
            usage = UsageState(
                authenticated = status.optBoolean("authenticated", false),
                freeRemaining = status.optInt("freeRemaining", 0),
                paidCredits = status.optInt("paidCredits", 0),
                subscriptionPlan = status.optJSONObject("subscription")?.optString("plan"),
                subscriptionRenewsAt = status.optJSONObject("subscription")?.optString("renews_at"),
                monthlyPricePaise = status.optJSONObject("prices")?.optInt("monthlyPaise", 4900) ?: 4900,
                yearlyPricePaise = status.optJSONObject("prices")?.optInt("yearlyPaise", 49900) ?: 49900,
                paygPricePaise = status.optJSONObject("prices")?.optInt("paygPaise", 100) ?: 100,
                canUse = status.optBoolean("canUse", false)
            )
        )
    }

    suspend fun createBillingOrder(kind: String): BillingOrder {
        val obj = request("POST", "/api/billing/order", JSONObject().put("kind", kind))
        return BillingOrder(obj.optString("orderId"), obj.optString("kind", kind), obj.optInt("amountPaise"), obj.optString("currency", "INR"), obj.optString("status", "pending"), obj.optString("message"))
    }

    suspend fun trends(): Pair<String?, List<TrendingItem>> {
        val obj = request("GET", "/api/trends")
        val arr = obj.optJSONArray("trends") ?: JSONArray()
        val items = buildList {
            for (i in 0 until arr.length()) {
                val t = arr.optJSONObject(i) ?: continue
                add(TrendingItem(
                    id = t.optString("id"),
                    title = t.optString("title", "Trending creation"),
                    category = t.optString("category", "AI creation"),
                    hook = t.optString("hook", "Explore this creation direction with W2P."),
                    score = t.optInt("score", 0)
                ))
            }
        }
        return obj.optString("generatedAt", null) to items
    }

    // Existing W2P intelligence APIs remain unchanged below this point.
    suspend fun intentEngine(idea: String, output: String, destination: String, answers: Map<String, String>): IntentAnalysis {
        val obj = request("POST", "/api/intent-engine", JSONObject().apply {
            put("idea", idea); put("output", output); put("destination", destination); put("answers", JSONObject(answers))
        })
        val intent = obj.optJSONObject("intent") ?: JSONObject()
        val research = obj.optJSONObject("researchDecision") ?: JSONObject()
        val brief = obj.optJSONObject("creativeBrief") ?: JSONObject()
        val arr = obj.optJSONArray("questions") ?: JSONArray()
        val questions = buildList {
            for (i in 0 until arr.length()) {
                val q = arr.optJSONObject(i) ?: continue
                val options = q.optJSONArray("options")?.strings() ?: emptyList()
                add(SmartQuestion(q.optString("key"), q.optString("question"), q.optString("reason"), options))
            }
        }
        return IntentAnalysis(
            intentName = intent.optString("name", "create"),
            confidence = intent.optInt("confidence", 0),
            completeness = obj.optInt("completeness", 0),
            missing = obj.optJSONArray("missing")?.strings() ?: emptyList(),
            questions = questions,
            researchNeeded = research.optBoolean("needed", false),
            researchReason = research.optString("reason", ""),
            briefOutput = brief.optString("output", output),
            briefDestination = brief.optString("destination", destination),
            briefGoal = brief.optString("goal", null),
            briefAudience = brief.optString("audience", null)
        )
    }

    suspend fun status() = request("GET", "/api/v8-3/status")

    suspend fun commands(prefix: String): List<CommandSuggestion> {
        val obj = request("GET", "/api/v8-3/commands?q=${java.net.URLEncoder.encode(prefix, "UTF-8")}")
        val arr = obj.optJSONArray("commands") ?: JSONArray()
        return buildList { for (i in 0 until arr.length()) { val c = arr.optJSONObject(i); if (c != null) add(CommandSuggestion(c.optString("name"), c.optString("label", c.optString("name")), c.optString("description"))) else add(CommandSuggestion(arr.optString(i))) } }
    }

    suspend fun plan(idea: String, output: String, destination: String, answers: Map<String, String>): PlanResponse = try {
        val obj = request("POST", "/api/v8-3/plan", JSONObject().put("idea", idea).put("output", output).put("destination", destination).put("answers", JSONObject(answers)))
        val p = obj.optJSONObject("plan") ?: return PlanResponse(false, null, obj.optString("error")); val task = p.optJSONObject("task") ?: JSONObject(); val graph = p.optJSONObject("graph") ?: JSONObject(); val nodes = graph.optJSONArray("nodes") ?: JSONArray()
        val nodeList = buildList { for (i in 0 until nodes.length()) { val n = nodes.optJSONObject(i) ?: continue; add(TaskNode(n.optString("id"), n.optString("type"), n.optString("title"), n.optString("action"), n.optBoolean("requiresApproval"))) } }
        val caps = p.optJSONArray("capabilities")?.let { arr -> buildList { for (i in 0 until arr.length()) add(arr.optJSONObject(i)?.optString("label").orEmpty()) } } ?: emptyList()
        val context = p.optJSONObject("context") ?: JSONObject(); val angles = context.optJSONArray("researchAngles")?.strings() ?: emptyList()
        PlanResponse(true, PlanData(task.optString("taskType"), task.optString("depth"), task.optString("goal"), p.optString("nextAction"), caps, nodeList, angles), null)
    } catch (e: Exception) { PlanResponse(false, null, e.message ?: "Planning failed") }

    suspend fun intelligence(idea: String, output: String, destination: String, answers: Map<String, String>, sourceImageDataUrl: String? = null): IntelligenceData {
        val obj = request("POST", "/api/intelligence", JSONObject().apply { put("idea", idea); put("intent", "create"); put("output", output); put("destination", destination); put("answers", JSONObject(answers)); sourceImageDataUrl?.let { put("sourceImage", it) } }); val intent = obj.optJSONObject("intent") ?: JSONObject(); val decisions = obj.optJSONObject("decisions") ?: JSONObject()
        return IntelligenceData(obj.optString("summary", "One canonical context is ready."), intent.optString("name", "create"), (intent.optDouble("confidence", 0.0) * 100).toInt(), obj.optString("output", output), obj.optString("destination", destination), obj.optJSONArray("assets")?.length() ?: 0, obj.stringList("protect"), decisions.stringList("open"))
    }

    suspend fun research(idea: String, output: String, destination: String, answers: Map<String, String>): ResearchData {
        val obj = request("POST", "/api/research-intelligence", JSONObject().apply { put("idea", idea); put("intent", "create"); put("output", output); put("destination", destination); put("answers", JSONObject(answers)) }); val sources = obj.optJSONArray("sources") ?: JSONArray(); val sourceList = buildList { for (i in 0 until sources.length()) { val s = sources.optJSONObject(i) ?: continue; add(s.optString("title", "Source") to s.optString("url", "")) } }
        return ResearchData(obj.optBoolean("needed"), obj.optString("confidence", "medium"), obj.stringList("topics"), obj.stringList("queries"), sourceList)
    }

    suspend fun creationIntelligence(idea: String, output: String, destination: String, answers: Map<String, String>, research: ResearchData?, intelligence: IntelligenceData?): CreationPlanData {
        val obj = request("POST", "/api/creation-intelligence", JSONObject().apply { put("idea", idea); put("intent", "create"); put("output", output); put("destination", destination); put("answers", JSONObject(answers)); put("research", JSONObject().put("needed", research?.needed ?: false).put("queries", JSONArray(research?.queries ?: emptyList()))); put("creativeContext", JSONObject().put("summary", intelligence?.summary ?: "")) }); val steps = obj.optJSONArray("steps") ?: JSONArray()
        return CreationPlanData(obj.optString("summary", "Creation workflow prepared."), obj.optString("readiness", "review"), obj.optString("nextAction", "review"), buildList { for (i in 0 until steps.length()) add(steps.optJSONObject(i)?.optString("title").orEmpty().ifBlank { "Step ${i+1}" }) }, obj.optJSONObject("qualityGate")?.stringList("blocking") ?: emptyList(), obj.stringList("risks"), obj.stringList("capabilities"), obj.stringList("successCriteria"))
    }

    suspend fun promptQuality(prompt: String, idea: String, output: String, destination: String): PromptQualityData { val obj = request("POST", "/api/prompt-quality", JSONObject().apply { put("prompt", prompt); put("idea", idea); put("output", output); put("destination", destination) }); return PromptQualityData(obj.optInt("score", 0), obj.optString("grade", "Needs review"), obj.stringList("strengths"), obj.stringList("issues")) }
    suspend fun resultIntelligence(prompt: String, idea: String, intent: String, resultImage: String, creativeContext: JSONObject = JSONObject()): ResultIntelligenceData { val obj = request("POST", "/api/result-intelligence", JSONObject().apply { put("prompt", prompt); put("idea", idea); put("intent", intent); put("resultImage", resultImage); put("creativeContext", creativeContext) }); return ResultIntelligenceData(obj.optString("summary", "Result inspection completed."), if (obj.has("score") && !obj.isNull("score")) obj.optInt("score") else null, obj.optString("confidence", "low"), obj.stringList("mismatches"), obj.stringList("protected"), obj.stringList("uncertain"), obj.optString("suggested_revision", ""), obj.optString("overall", "needs_review")) }
    suspend fun ghostCheck(idea: String, prompt: String, output: String, destination: String, resultImage: String = "", report: JSONObject? = null): GhostCheckData { val obj = request("POST", "/api/ghost-check", JSONObject().apply { put("idea", idea); put("prompt", prompt); put("output", output); put("destination", destination); put("resultImage", resultImage); report?.let { put("resultReport", it) } }); return GhostCheckData(obj.optBoolean("ok", false), obj.optString("status", "review"), obj.stringList("blockers"), obj.stringList("warnings"), obj.optString("nextAction", "review")) }
    suspend fun autoFix(prompt: String, mismatches: List<String>, suggestedRevision: String, creativeContext: JSONObject = JSONObject()): String { val obj = request("POST", "/api/auto-fix", JSONObject().apply { put("prompt", prompt); put("mismatches", JSONArray(mismatches)); put("suggested_revision", suggestedRevision); put("creativeContext", creativeContext) }); return obj.optString("revisedPrompt").ifBlank { prompt } }
    suspend fun createJob(idea: String, prompt: String, output: String, destination: String): JSONObject { val payload = JSONObject().put("idea", idea).put("prompt", prompt).put("output", output).put("destination", destination); val preflight = request("POST", "/api/creation-preflight", payload); if (!(preflight.optJSONObject("preflight")?.optBoolean("ok", false) ?: false)) error(preflight.optJSONObject("preflight")?.optJSONArray("blockers")?.strings()?.joinToString(" ").orEmpty().ifBlank { preflight.optString("error", "Creation preflight failed.") }); return request("POST", "/api/creation-jobs", payload) }
    suspend fun approveJob(id: String): JSONObject = request("POST", "/api/creation-jobs/${java.net.URLEncoder.encode(id, "UTF-8")}?action=approve")
    suspend fun executeJob(id: String): JSONObject = request("POST", "/api/creation-jobs/${java.net.URLEncoder.encode(id, "UTF-8")}?action=execute")
    suspend fun promptIntelligence(basePrompt: String, idea: String, output: String, destination: String, answers: Map<String, String>, research: ResearchData?, intelligence: IntelligenceData?, creation: CreationPlanData?): PromptMeta { val obj = request("POST", "/api/prompt-intelligence", JSONObject().apply { put("idea", idea); put("output", output); put("destination", destination); put("answers", JSONObject(answers)); put("creativeBrief", JSONObject().put("summary", creation?.summary ?: "").put("steps", JSONArray(creation?.steps ?: emptyList()))); put("research", JSONObject().put("needed", research?.needed ?: false).put("topics", JSONArray(research?.topics ?: emptyList())).put("queries", JSONArray(research?.queries ?: emptyList()))); put("creativeContext", JSONObject().put("summary", intelligence?.summary ?: "")) }); return PromptMeta(obj.optString("mode", "model"), obj.optString("summary", "Prompt optimized."), obj.optString("destinationStrategy", "Intent-preserving destination optimization."), obj.stringList("warnings"), obj.stringList("assumptions"), obj.optString("prompt").ifBlank { basePrompt }) }
}
