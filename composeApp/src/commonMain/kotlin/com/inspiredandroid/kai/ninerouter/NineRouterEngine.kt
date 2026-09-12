package com.inspiredandroid.kai.ninerouter

import com.inspiredandroid.kai.httpClient
import com.inspiredandroid.kai.network.ServiceCredentials
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlin.time.Clock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

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
 * Multi-account pooling, failover, and model-level locks.
 */
object NineRouterEngine {
    private val jsonLenient = Json { ignoreUnknownKeys = true; isLenient = true }

    fun getAvailableCandidates(
        rawModelId: String,
        fallbackCredentials: ServiceCredentials,
        config: NineRouterConfig,
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
            NineRouterRegistry.find(providerKey) ?: NineRouterRegistry.find(providerKey.lowercase())
        } else null

        val now = Clock.System.now().toEpochMilliseconds()

        val matching = if (meta != null) {
            config.connections.filter { conn ->
                conn.enabled && conn.apiKey.isNotBlank() &&
                (conn.provider.equals(meta.id, ignoreCase = true) || conn.provider.equals(meta.alias, ignoreCase = true))
            }
        } else {
            emptyList()
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

            val upstreamModel = if (meta?.id == "cloudflare-ai") {
                if (modelPart.startsWith("@cf/")) modelPart else "@cf/$modelPart"
            } else {
                modelPart
            }

            return candidatesPool.map { conn ->
                var base = meta?.baseUrl ?: ""
                if (meta?.needsAccountId == true) {
                    base = base.replace("{accountId}", conn.accountId.trim())
                }

                val customHeaders = mutableMapOf<String, String>()
                if (meta?.format == "claude") {
                    customHeaders["anthropic-version"] = "2023-06-01"
                    customHeaders["x-api-key"] = conn.apiKey.trim()
                }

                val effectiveCreds = ServiceCredentials(
                    apiKey = conn.apiKey.trim(),
                    modelId = upstreamModel,
                    baseUrl = base,
                )

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

    fun resolveBaseUrl(meta: NineProviderMeta, conn: NineConnection): String {
        var url = meta.baseUrl
        if (meta.needsAccountId) {
            val acc = conn.accountId.trim()
            if (acc.isNotEmpty()) url = url.replace("{accountId}", acc)
        }
        return url
    }

    fun resolveAuthHeader(meta: NineProviderMeta, conn: NineConnection): Pair<String, String>? {
        val key = conn.apiKey.trim()
        if (key.isEmpty()) return null
        return when (meta.format) {
            "claude" -> "x-api-key" to key
            else -> "Authorization" to "Bearer $key"
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
}
