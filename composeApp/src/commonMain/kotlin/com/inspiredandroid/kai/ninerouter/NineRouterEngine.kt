package com.inspiredandroid.kai.ninerouter

import com.inspiredandroid.kai.httpClient
import com.inspiredandroid.kai.network.ServiceCredentials
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class NineValidationResult(
    val success: Boolean,
    val latencyMs: Long,
    val message: String,
    val statusCode: Int? = null,
)

data class NineRouterCandidate(
    val connectionId: String?,
    val effectiveCredentials: ServiceCredentials,
    val meta: NineProviderMeta?,
    val modelPart: String,
    val isStandalone: Boolean,
    val customHeaders: Map<String, String> = emptyMap(),
)

data class NineRouterResolvedTarget(
    val effectiveCredentials: ServiceCredentials,
    val meta: NineProviderMeta?,
    val isStandalone: Boolean,
    val customHeaders: Map<String, String> = emptyMap(),
    val connectionId: String? = null,
)

/**
 * Standalone in-app router engine — direct HTTP upstream client.
 * Multi-account pooling, combo fallback, relay routing, vision-aware
 * ordering, model-level locks, and RTK-style tool-output compression.
 */
object NineRouterEngine {
    private val jsonLenient = Json { ignoreUnknownKeys = true; isLenient = true }

    // Providers that work without any key/token (mirrors 9Router noAuth entries).
    private val NO_AUTH_PROVIDERS = setOf("opencode")

    // Muse Spark models go to /responses, everything else to /chat/completions (mirrors opencode.js)
    private val SPARK_MODELS = setOf("muse-spark-1.2-contributor-free", "muse-spark-1.3-contributor-free")
    private fun isSparkModel(modelPart: String): Boolean {
        val base = modelPart.substringAfter("/").substringBefore("(").trim().lowercase()
        return base in SPARK_MODELS || base.startsWith("muse-spark")
    }

    private var cachedOpencodeSession: String? = null
    @OptIn(ExperimentalUuidApi::class)
    private fun getOpencodeSession(): String = cachedOpencodeSession ?: ("ses_" + Uuid.random().toString().replace("-","")).also { cachedOpencodeSession = it }
    @OptIn(ExperimentalUuidApi::class)
    private fun genOpencodeRequestId(): String = "msg_" + Uuid.random().toString().replace("-","")
    private fun opencodeHeaders(): MutableMap<String,String> = mutableMapOf(
        "x-opencode-client" to "desktop",
        "x-opencode-session" to getOpencodeSession(),
        "x-opencode-request" to genOpencodeRequestId(),
        "x-opencode-project" to "global",
        "Authorization" to "Bearer public",
        "User-Agent" to "opencode",
        "Origin" to "https://opencode.ai",
        "Referer" to "https://opencode.ai/",
        "Accept" to "application/json",
    )

    // ── Combo expansion ────────────────────────────────────────────────
    /** Expand a combo name/id into its ordered model list, or null if not a combo. */
    fun resolveComboModels(rawModelId: String, config: NineRouterConfig): List<String>? {
        val key = rawModelId.trim()
        if (key.isEmpty()) return null
        val combo = config.combos.firstOrNull {
            it.name.equals(key, ignoreCase = true) || it.id.equals(key, ignoreCase = true)
        } ?: return null
        return combo.models.filter { it.isNotBlank() }
    }

    fun getAvailableCandidates(
        rawModelId: String,
        fallbackCredentials: ServiceCredentials,
        config: NineRouterConfig,
        hasImages: Boolean = false,
    ): List<NineRouterCandidate> {
        // Combo: concatenate per-model candidates in combo order so the caller's
        // existing failover loop walks the whole chain (model1 acc1..n, model2…).
        val comboModels = resolveComboModels(rawModelId, config)
        if (comboModels != null) {
            val out = mutableListOf<NineRouterCandidate>()
            for (m in comboModels) {
                out += candidatesForModel(m, fallbackCredentials, config, hasImages)
            }
            if (out.any { it.isStandalone }) return out
            return listOf(
                NineRouterCandidate(null, fallbackCredentials, null, rawModelId.trim(), isStandalone = false),
            )
        }
        return candidatesForModel(rawModelId, fallbackCredentials, config, hasImages)
    }

    private fun candidatesForModel(
        rawModelId: String,
        fallbackCredentials: ServiceCredentials,
        config: NineRouterConfig,
        hasImages: Boolean,
    ): List<NineRouterCandidate> {
        val modelStr = rawModelId.trim()
        if (modelStr.isEmpty()) {
            return listOf(NineRouterCandidate(null, fallbackCredentials, null, "", isStandalone = false))
        }

        val providerKey: String?
        val modelPart: String

        if (modelStr.startsWith("@cf/")) {
            providerKey = "cloudflare-ai"
            modelPart = modelStr
        } else if (modelStr.contains("/")) {
            providerKey = modelStr.substringBefore("/")
            modelPart = modelStr.substringAfter("/")
        } else {
            providerKey = null
            modelPart = modelStr
        }

        val meta = if (providerKey != null) {
            NineRouterRegistry.find(providerKey, config) ?: NineRouterRegistry.find(providerKey.lowercase(), config)
        } else null

        // Providers without a direct HTTP endpoint (OAuth-only executors such as
        // antigravity/cursor代理) cannot be reached from the device — fall back.
        if (meta != null && meta.baseUrl.isBlank()) {
            return listOf(
                NineRouterCandidate(null, fallbackCredentials, meta, modelPart, isStandalone = false),
            )
        }

        val now = Clock.System.now().toEpochMilliseconds()

        val matching = if (meta != null) {
            config.connections.filter { conn ->
                conn.enabled &&
                    (conn.provider.equals(meta.id, ignoreCase = true) || conn.provider.equals(meta.alias, ignoreCase = true)) &&
                    (conn.apiKey.isNotBlank() || conn.accessToken.isNotBlank() || meta.id in NO_AUTH_PROVIDERS)
            }
        } else {
            emptyList()
        }

        // NO_AUTH providers (opencode) must work even with zero stored connections (mirrors registry noAuth:true)
        if (matching.isEmpty() && meta != null && meta.id in NO_AUTH_PROVIDERS) {
            val upstreamModelNoAuth = if (meta.id == "cloudflare-ai") {
                if (modelPart.startsWith("@cf/")) modelPart else "@cf/$modelPart"
            } else {
                modelPart
            }
            val isSparkNoAuth = meta.id == "opencode" && isSparkModel("$providerKey/$modelPart")
            val baseNoAuth = when {
                meta.id == "opencode" && isSparkNoAuth -> "https://opencode.ai/zen/v1/responses"
                meta.id == "opencode" -> "https://opencode.ai/zen/v1/chat/completions"
                else -> meta.baseUrl
            }
            val headersNoAuth = if (meta.id == "opencode") opencodeHeaders() else mutableMapOf()
            if (meta.format == "claude") headersNoAuth["anthropic-version"] = "2023-06-01"
            return listOf(
                NineRouterCandidate(
                    connectionId = null,
                    effectiveCredentials = ServiceCredentials(apiKey = "", modelId = upstreamModelNoAuth, baseUrl = baseNoAuth),
                    meta = meta,
                    modelPart = modelPart,
                    isStandalone = true,
                    customHeaders = headersNoAuth,
                )
            )
        }

        if (matching.isNotEmpty()) {
            val available = matching.filter { conn: NineConnection ->
                val lockModel: Long = conn.modelLocks[modelPart] ?: 0L
                val lockAll: Long = conn.modelLocks["__all"] ?: 0L
                lockModel <= now && lockAll <= now
            }.sortedWith(compareBy<NineConnection> { it.priority }.thenBy { it.lastUsedAt })

            val candidatesPool = if (available.isNotEmpty()) available else matching.sortedBy { conn: NineConnection ->
                val lockTime: Long = conn.modelLocks[modelPart] ?: conn.modelLocks["__all"] ?: 0L
                lockTime
            }

            val upstreamModel = if (meta!!.id == "cloudflare-ai") {
                if (modelPart.startsWith("@cf/")) modelPart else "@cf/$modelPart"
            } else {
                modelPart
            }

            // Vision guard: when the request carries images, prefer vision-capable
            // models first instead of failing on a text-only endpoint. Never drops
            // candidates — pure reorder, so nothing that worked before can break.
            val orderedPool = if (hasImages) {
                candidatesPool.sortedByDescending { looksLikeVisionModel(upstreamModel) }
            } else {
                candidatesPool
            }

            return orderedPool.map { conn ->
                var base = when {
                    meta!!.id == "opencode" && isSparkModel("$providerKey/$modelPart") -> "https://opencode.ai/zen/v1/responses"
                    meta!!.id == "opencode" -> "https://opencode.ai/zen/v1/chat/completions"
                    else -> meta!!.baseUrl ?: ""
                }
                if (meta!!.needsAccountId == true) {
                    base = base.replace("{accountId}", conn.accountId.trim())
                }

                val customHeaders = if (meta!!.id == "opencode") opencodeHeaders() else mutableMapOf()
                if (meta!!.format == "claude") {
                    customHeaders["anthropic-version"] = "2023-06-01"
                    effectiveKey(conn)?.let { customHeaders["x-api-key"] = it }
                }

                // Relay routing (mirrors 9Router proxyFetch vercel-relay):
                // POST to the relay, upstream target rides in headers.
                val relay = conn.relayUrl.trim().removeSuffix("/")
                val effectiveCreds = if (relay.isNotEmpty() && base.isNotEmpty()) {
                    customHeaders["x-relay-target"] = base.substringBefore("/", base).let {
                        // keep scheme+host only
                        val withoutScheme = base.substringAfter("://", base)
                        val scheme = if (base.contains("://")) base.substringBefore("://") + "://" else ""
                        scheme + withoutScheme.substringBefore("/")
                    }
                    customHeaders["x-relay-path"] = "/" + base.substringAfter("://", base).substringAfter("/", "").trimStart('/')
                    ServiceCredentials(
                        apiKey = effectiveKey(conn) ?: "",
                        modelId = upstreamModel,
                        baseUrl = relay,
                    )
                } else {
                    ServiceCredentials(
                        apiKey = effectiveKey(conn) ?: "",
                        modelId = upstreamModel,
                        baseUrl = base,
                    )
                }

                NineRouterCandidate(
                    connectionId = conn.id,
                    effectiveCredentials = effectiveCreds,
                    meta = meta,
                    modelPart = modelPart,
                    isStandalone = true,
                    customHeaders = customHeaders,
                )
            }
        }

        return listOf(
            NineRouterCandidate(
                connectionId = null,
                effectiveCredentials = fallbackCredentials,
                meta = meta,
                modelPart = modelPart,
                isStandalone = false,
            )
        )
    }

    private fun effectiveKey(conn: NineConnection): String? {
        val k = conn.apiKey.trim().ifNotEmpty() ?: conn.accessToken.trim().ifNotEmpty()
        return k
    }

    private fun String.ifNotEmpty(): String? = if (isNotEmpty()) this else null

    fun resolveTarget(
        rawModelId: String,
        fallbackCredentials: ServiceCredentials,
        config: NineRouterConfig,
    ): NineRouterResolvedTarget {
        val candidate = getAvailableCandidates(rawModelId, fallbackCredentials, config).first()
        return NineRouterResolvedTarget(
            effectiveCredentials = candidate.effectiveCredentials,
            meta = candidate.meta,
            isStandalone = candidate.isStandalone,
            customHeaders = candidate.customHeaders,
            connectionId = candidate.connectionId,
        )
    }

    fun isFallbackError(e: Throwable): Boolean {
        val msg = (e.message ?: "").lowercase()
        return msg.contains("429") || msg.contains("rate limit") || msg.contains("quota") ||
            msg.contains("401") || msg.contains("unauthorized") || msg.contains("403") ||
            msg.contains("400") || msg.contains("overloaded") || msg.contains("exhausted")
    }

    // ── Vision detection (port of 9Router visionPatterns.js) ───────────
    private val NOT_VISION = Regex(
        "(^|[-_/:.])(image|img)([-_/:.]|$)|stable-image|gen[0-9]_image|nanobanana|imagine|" +
            "t2v|i2v|flux|dall|sdxl|diffusion|embed|rerank|guard|moderation|" +
            "tts|stt|whisper|voice|speech|audio",
        RegexOption.IGNORE_CASE,
    )
    private val VISION_NAME = Regex(
        "(^|[-_/:.])(vision|vl|vlm|multimodal|omni|visual)([-_/:.]|$)|[0-9]\\.[0-9]+v([-_/:.]|$)|" +
            "(^|[-_/:.])glm-[0-9]+v([-_/:.]|$)|(^|[-_/:.])(llava|pixtral|internvl|cogvlm|minicpm-v|moondream|idefics|fuyu)",
        RegexOption.IGNORE_CASE,
    )

    /** Name-signal only: only ever turns vision ON, never off. */
    fun looksLikeVisionModel(modelId: String?): Boolean {
        if (modelId.isNullOrBlank()) return false
        val id = modelId.lowercase()
        if (NOT_VISION.containsMatchIn(id)) return false
        return VISION_NAME.containsMatchIn(id)
    }

    // ── RTK-style tool-output compression (lossless, fail-open) ─────────
    /**
     * Compress tool_result text before it is sent upstream. Mirrors 9Router's
     * RTK filters for git diff/status/log, grep/find/ls/tree output, deduped
     * logs and smart truncation. Returns the original text when the filter
     * does not apply or would make output bigger — never throws.
     */
    fun compressToolText(text: String): String {
        return try {
            if (text.length < 500) return text
            val head = text.take(1024)
            val compressed = when {
                head.contains("diff --git") -> compressGitDiff(text)
                head.contains("Changes to be committed") || head.contains("Untracked files") -> compressGitStatus(text)
                Regex("^\\s*[0-9a-f]{7,40}\\s").containsMatchIn(head) && head.contains("Date:") -> compressGitLog(text)
                head.contains(".log:") || head.contains("ERROR") && hasRepeats(text) -> dedupLog(text)
                head.lines().firstOrNull()?.trim()?.startsWith("total ") == true -> compressLs(text)
                head.contains("├──") || head.contains("└──") -> compressTree(text)
                head.contains(":") && Regex("^[^:]+:\\d+:").containsMatchIn(head) -> compressGrep(text)
                else -> smartTruncate(text)
            }
            if (compressed.length < text.length) compressed else text
        } catch (_: Exception) {
            text
        }
    }

    private fun compressGitDiff(text: String): String {
        val out = StringBuilder()
        for (line in text.lines()) {
            val t = line.trimStart()
            if (t.startsWith("index ") || t.startsWith("--- a/") && false) continue
            if (t.startsWith("index ")) continue
            if (t.startsWith("similarity index") || t.startsWith("rename from") || t.startsWith("rename to")) continue
            out.appendLine(line)
        }
        return smartTruncate(out.toString())
    }

    private fun compressGitStatus(text: String): String {
        val keep = text.lines().filter { l ->
            val t = l.trim()
            t.isNotEmpty() && !t.startsWith("#") && t != "(use \"git add <file>...\" to update what will be committed)"
        }
        return keep.joinToString("\n")
    }

    private fun compressGitLog(text: String): String {
        val out = StringBuilder()
        for (line in text.lines()) {
            val t = line.trimStart()
            if (t.startsWith("Author:") || t.startsWith("Date:") && out.isNotEmpty()) {
                if (t.startsWith("Author:")) continue
            }
            out.appendLine(line)
        }
        return smartTruncate(out.toString())
    }

    private fun compressLs(text: String): String {
        val lines = text.lines()
        if (lines.size < 30) return text
        // Collapse permission/owner/timestamp columns, keep name + size.
        val collapsed = lines.map { l ->
            val parts = l.trim().split(Regex("\\s+"))
            if (parts.size > 8) parts.last() else l
        }
        return collapsed.joinToString("\n")
    }

    private fun compressTree(text: String): String {
        val lines = text.lines()
        if (lines.size < 40) return text
        // Drop deep nesting beyond 4 levels.
        return lines.filter { l ->
            val depth = l.count { it == '─' || it == '│' }
            depth <= 8
        }.joinToString("\n")
    }

    private fun compressGrep(text: String): String {
        val lines = text.lines()
        if (lines.size < 30) return text
        // Truncate very long matched lines, keep file:line prefix.
        return lines.map { l ->
            if (l.length > 220) {
                val idx = l.indexOf(':')
                val idx2 = if (idx >= 0) l.indexOf(':', idx + 1) else -1
                if (idx2 > 0) l.take(idx2 + 180) + "…" else l.take(220) + "…"
            } else l
        }.joinToString("\n")
    }

    private fun hasRepeats(text: String): Boolean {
        val lines = text.lines()
        if (lines.size < 20) return false
        return lines.size - lines.toSet().size > lines.size / 4
    }

    private fun dedupLog(text: String): String {
        val seen = LinkedHashSet<String>()
        val out = StringBuilder()
        var dropped = 0
        for (line in text.lines()) {
            if (!seen.add(line)) {
                dropped++
                continue
            }
            out.appendLine(line)
        }
        if (dropped > 0) out.appendLine("… ($dropped duplicate lines removed)")
        return out.toString()
    }

    private fun smartTruncate(text: String, maxChars: Int = 12000): String {
        if (text.length <= maxChars) return text
        val head = text.take(maxChars * 2 / 3)
        val tail = text.takeLast(maxChars / 3)
        return head + "\n… [truncated ${text.length - maxChars} chars] …\n" + tail
    }

    // ── Token-saver system prompts (port of 9Router caveman/ponytail) ──
    private const val CAVEMAN_FULL =
        "Reply tersely. Short sentences. Technical substance preserved. No filler, no preamble, no hedging."
    private const val PONYTAIL_FULL =
        "Write minimal code (YAGNI): stdlib first, one-liners over abstractions, " +
            "deletion over addition. Never skip input validation, error handling that prevents " +
            "data loss, security, or anything explicitly requested."

    /** Build the effective system prompt with caveman/ponytail injections applied. */
    fun buildSystemPrompt(base: String?, config: NineRouterConfig): String? {
        if (config.cavemanEnabled.not() && config.ponytailEnabled.not()) return base
        val sb = StringBuilder()
        if (config.cavemanEnabled) sb.append(CAVEMAN_FULL).append('\n')
        if (config.ponytailEnabled) {
            sb.append(if (config.ponytailLevel == "lite") PONYTAIL_FULL.take(120) else PONYTAIL_FULL).append('\n')
        }
        if (!base.isNullOrBlank()) sb.append(base)
        val out = sb.toString().trim()
        return out.ifEmpty { null }
    }

    fun resolveBaseUrl(meta: NineProviderMeta, conn: NineConnection): String {
        var url = meta.baseUrl
        if (meta.needsAccountId) {
            val acc = conn.accountId.trim()
            if (acc.isNotEmpty()) url = url.replace("{accountId}", acc)
        }
        return url
    }

    fun resolveAuthHeader(meta: NineProviderMeta, conn: NineConnection): Pair<String, String>? {
        val key = effectiveKey(conn) ?: return null
        return when (meta.format) {
            "claude" -> "x-api-key" to key
            else -> "Authorization" to "Bearer $key"
        }
    }

    suspend fun pingConnection(meta: NineProviderMeta, conn: NineConnection): NineValidationResult {
        val start = Clock.System.now().toEpochMilliseconds()
        if (meta.id == "opencode") {
            return try {
                val h = opencodeHeaders()
                val resp = httpClient().get("https://opencode.ai/zen/v1/models") {
                    h.forEach { (k,v) -> header(k, v) }
                }
                val latency = Clock.System.now().toEpochMilliseconds() - start
                if (resp.status.isSuccess()) {
                    NineValidationResult(true, latency, "Active (${latency}ms)", resp.status.value)
                } else {
                    NineValidationResult(false, latency, "Status ${resp.status.value}", resp.status.value)
                }
            } catch (e: Exception) {
                val latency = Clock.System.now().toEpochMilliseconds() - start
                NineValidationResult(false, latency, e.message?.take(50) ?: "Error", null)
            }
        }

        val url = when {
            meta.validateUrl.isNotBlank() -> {
                var u = meta.validateUrl
                if (meta.needsAccountId) u = u.replace("{accountId}", conn.accountId.trim())
                u
            }
            meta.baseUrl.isNotBlank() -> {
                var base = resolveBaseUrl(meta, conn)
                if (base.contains("/chat/completions")) base.replace("/chat/completions", "/models")
                else if (base.contains("/messages")) base.replace("/messages", "/models")
                else base
            }
            else -> return NineValidationResult(false, 0L, "No validate URL for ${meta.id}", null)
        }

        return try {
            val relay = conn.relayUrl.trim().removeSuffix("/")
            val resp = if (relay.isNotEmpty()) {
                val targetHost = if (url.contains("://")) {
                    val proto = url.substringBefore("://") + "://"
                    val rest = url.substringAfter("://")
                    proto + rest.substringBefore("/")
                } else url
                val targetPath = "/" + url.substringAfter("://", url).substringAfter("/", "").trimStart('/')
                httpClient().get(relay) {
                    header("x-relay-target", targetHost)
                    header("x-relay-path", targetPath)
                    val auth = resolveAuthHeader(meta, conn)
                    if (auth != null) {
                        if (auth.first == "Authorization") bearerAuth(auth.second.removePrefix("Bearer ").trim())
                        else header(auth.first, auth.second)
                    }
                    if (meta.format == "claude") header("anthropic-version", "2023-06-01")
                }
            } else {
                httpClient().get(url) {
                    val auth = resolveAuthHeader(meta, conn)
                    if (auth != null) {
                        if (auth.first == "Authorization") bearerAuth(auth.second.removePrefix("Bearer ").trim())
                        else header(auth.first, auth.second)
                    }
                    if (meta.format == "claude") header("anthropic-version", "2023-06-01")
                }
            }

            val latency = Clock.System.now().toEpochMilliseconds() - start
            if (resp.status.isSuccess()) {
                NineValidationResult(true, latency, "Active (${latency}ms)", resp.status.value)
            } else {
                val code = resp.status.value
                val hint = when (code) {
                    401 -> "401 Invalid Key"
                    403 -> "403 Forbidden"
                    429 -> "429 Rate Limit"
                    404 -> "404 Not Found"
                    else -> "Status $code"
                }
                NineValidationResult(false, latency, "$hint (${latency}ms)", code)
            }
        } catch (e: Exception) {
            val latency = Clock.System.now().toEpochMilliseconds() - start
            NineValidationResult(false, latency, e.message?.take(50) ?: "Error", null)
        }
    }

    suspend fun validateConnection(meta: NineProviderMeta, conn: NineConnection): Result<Unit> {
        val url = when {
            meta.validateUrl.isNotBlank() -> meta.validateUrl.let {
                if (meta.needsAccountId) it.replace("{accountId}", conn.accountId.trim()) else it
            }
            meta.baseUrl.isNotBlank() -> {
                val base = resolveBaseUrl(meta, conn)
                if (base.contains("/chat/completions")) base.replace("/chat/completions", "/models")
                else base
            }
            else -> return Result.failure(IllegalStateException("No validate URL for ${meta.id}"))
        }
        return try {
            val resp = httpClient().get(url) {
                val auth = resolveAuthHeader(meta, conn)
                if (auth != null) {
                    if (auth.first == "Authorization") bearerAuth(auth.second.removePrefix("Bearer ").trim())
                    else header(auth.first, auth.second)
                }
                if (meta.format == "claude") header("anthropic-version", "2023-06-01")
            }
            if (resp.status.isSuccess()) Result.success(Unit)
            else Result.failure(IllegalStateException("Validate failed ${resp.status}: ${resp.bodyAsText().take(300)}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchModels(meta: NineProviderMeta, conn: NineConnection): Result<List<String>> {
        val validateUrl = when {
            meta.id == "opencode" -> "https://opencode.ai/zen/v1/models"
            meta.validateUrl.isNotBlank() -> meta.validateUrl.let {
                if (meta.needsAccountId) it.replace("{accountId}", conn.accountId.trim()) else it
            }
            meta.baseUrl.isNotBlank() -> {
                val base = resolveBaseUrl(meta, conn)
                if (base.contains("/chat/completions")) base.replace("/chat/completions", "/models") else base
            }
            else -> return Result.success(emptyList())
        }
        return try {
            val resp = httpClient().get(validateUrl) {
                val auth = resolveAuthHeader(meta, conn)
                if (auth != null) {
                    if (auth.first == "Authorization") bearerAuth(auth.second.removePrefix("Bearer ").trim())
                    else header(auth.first, auth.second)
                }
                if (meta.format == "claude") header("anthropic-version", "2023-06-01")
                if (meta.id == "opencode") {
                    opencodeHeaders().forEach { (k,v) -> header(k, v) }
                }
            }
            if (!resp.status.isSuccess()) return Result.failure(IllegalStateException("Failed ${resp.status}"))
            val text = resp.bodyAsText()
            val parsed = jsonLenient.parseToJsonElement(text).jsonObject
            val data = parsed["data"]?.jsonArray
                ?: parsed["models"]?.jsonArray
                ?: return Result.success(emptyList())
            val models = data.mapNotNull { item ->
                val obj = item.jsonObject
                obj["id"]?.jsonPrimitive?.content ?: obj["name"]?.jsonPrimitive?.content
            }
            Result.success(models)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Free-only subset from live opencode.ai/zen/v1/models (mirrors registry hasFree + passthroughModels) */
    suspend fun fetchOpencodeFreeModels(): Result<List<Pair<String,String>>> {
        return try {
            val h2 = opencodeHeaders()
            val resp = httpClient().get("https://opencode.ai/zen/v1/models") {
                h2.forEach { (k,v) -> header(k, v) }
            }
            if (!resp.status.isSuccess()) return Result.failure(IllegalStateException("Failed ${resp.status}: ${resp.bodyAsText().take(200)}"))
            val text = resp.bodyAsText()
            val parsed = jsonLenient.parseToJsonElement(text).jsonObject
            val data = parsed["data"]?.jsonArray ?: return Result.success(emptyList())
            val freeIds = data.mapNotNull { it.jsonObject["id"]?.jsonPrimitive?.content }
                .filter { id -> id.endsWith("-free") || id in setOf("muse-spark-1.2", "muse-spark-1.3", "big-pickle") }
                .distinct()
            // Normalize to oc/ prefix + display name
            val pairs = freeIds.map { id ->
                val full = if (id.startsWith("oc/")) id else "oc/$id"
                val name = id.replace("-free","").replace("-"," ").replaceFirstChar { it.uppercase() } + " (Free)"
                full to name
            }
            Result.success(pairs)
        } catch (e: Exception) { Result.failure(e) }
    }
}
