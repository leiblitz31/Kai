package com.inspiredandroid.kai.ninerouter

import com.inspiredandroid.kai.httpClient
import com.inspiredandroid.kai.network.ServiceCredentials
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class NineRouterResolvedTarget(
    val effectiveCredentials: ServiceCredentials,
    val meta: NineProviderMeta?,
    val isStandalone: Boolean,
    val customHeaders: Map<String, String> = emptyMap(),
)

/**
 * Standalone in-app engine — direct HTTP to upstream without Mido server.
 * Handles 142 providers from NineRouterRegistry.
 */
object NineRouterEngine {
    private val jsonLenient = Json { ignoreUnknownKeys = true; isLenient = true }

    fun resolveTarget(
        rawModelId: String,
        fallbackCredentials: ServiceCredentials,
        config: NineRouterConfig,
    ): NineRouterResolvedTarget {
        val modelStr = rawModelId.trim()
        if (modelStr.isEmpty()) {
            return NineRouterResolvedTarget(fallbackCredentials, null, isStandalone = false)
        }

        // 1. Resolve alias or provider prefix
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

        // 2. Check if standalone credentials exist for this provider
        val creds = if (meta != null) {
            config.providers[meta.id]
                ?: config.providers[meta.alias]
                ?: config.providers.entries.firstOrNull { 
                    it.key.equals(meta.id, ignoreCase = true) || it.key.equals(meta.alias, ignoreCase = true) 
                }?.value
        } else {
            // If bare model name, check if any enabled standalone provider has an API key
            config.providers.values.firstOrNull { it.enabled && it.apiKey.isNotBlank() }
        }

        if (meta != null && creds != null && creds.enabled && creds.apiKey.isNotBlank()) {
            // Standalone match!
            val upstreamModel = if (meta.id == "cloudflare-ai") {
                if (modelPart.startsWith("@cf/")) modelPart else "@cf/$modelPart"
            } else {
                modelPart
            }

            var base = meta.baseUrl
            if (meta.needsAccountId) {
                base = base.replace("{accountId}", creds.accountId.trim())
            }
            // Strip /chat/completions or /messages from the end so Kai's resolveUrl appends /chat/completions cleanly
            val customHeaders = mutableMapOf<String, String>()
            if (meta.format == "claude") {
                customHeaders["anthropic-version"] = "2023-06-01"
                customHeaders["x-api-key"] = creds.apiKey.trim()
            }

            val effectiveCreds = ServiceCredentials(
                apiKey = creds.apiKey.trim(),
                modelId = upstreamModel,
                baseUrl = base,
            )

            return NineRouterResolvedTarget(
                effectiveCredentials = effectiveCreds,
                meta = meta,
                isStandalone = true,
                customHeaders = customHeaders,
            )
        }

        // 3. Fallback to bridge (Mido)
        return NineRouterResolvedTarget(
            effectiveCredentials = fallbackCredentials,
            meta = meta,
            isStandalone = false,
        )
    }

    fun resolveBaseUrl(meta: NineProviderMeta, creds: NineProviderCredentials): String {
        var url = meta.baseUrl
        if (meta.needsAccountId) {
            val acc = creds.accountId.trim()
            if (acc.isNotEmpty()) url = url.replace("{accountId}", acc)
        }
        return url
    }

    fun resolveAuthHeader(meta: NineProviderMeta, creds: NineProviderCredentials): Pair<String, String>? {
        val key = creds.apiKey.trim()
        if (key.isEmpty()) return null
        return when (meta.format) {
            "claude" -> "x-api-key" to key
            else -> "Authorization" to "Bearer $key"
        }
    }

    suspend fun validateProvider(meta: NineProviderMeta, creds: NineProviderCredentials): Result<Unit> {
        val url = when {
            meta.validateUrl.isNotBlank() -> meta.validateUrl.let {
                if (meta.needsAccountId) it.replace("{accountId}", creds.accountId.trim()) else it
            }
            meta.baseUrl.isNotBlank() -> {
                val base = resolveBaseUrl(meta, creds)
                if (base.contains("/chat/completions")) base.replace("/chat/completions", "/models")
                else base
            }
            else -> return Result.failure(IllegalStateException("No validate URL for ${meta.id}"))
        }
        return try {
            val resp = httpClient().get(url) {
                val auth = resolveAuthHeader(meta, creds)
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

    suspend fun fetchModels(meta: NineProviderMeta, creds: NineProviderCredentials): Result<List<String>> {
        val validateUrl = when {
            meta.validateUrl.isNotBlank() -> meta.validateUrl.let {
                if (meta.needsAccountId) it.replace("{accountId}", creds.accountId.trim()) else it
            }
            meta.baseUrl.isNotBlank() -> {
                val base = resolveBaseUrl(meta, creds)
                if (base.contains("/chat/completions")) base.replace("/chat/completions", "/models") else base
            }
            else -> return Result.success(emptyList())
        }
        return try {
            val resp = httpClient().get(validateUrl) {
                val auth = resolveAuthHeader(meta, creds)
                if (auth != null) {
                    if (auth.first == "Authorization") bearerAuth(auth.second.removePrefix("Bearer ").trim())
                    else header(auth.first, auth.second)
                }
                if (meta.format == "claude") header("anthropic-version", "2023-06-01")
            }
            if (!resp.status.isSuccess()) return Result.failure(IllegalStateException("Fetch models ${resp.status}: ${resp.bodyAsText().take(400)}"))
            val text = resp.bodyAsText()
            val el = jsonLenient.parseToJsonElement(text)
            val ids = mutableListOf<String>()
            val arr = when {
                el is kotlinx.serialization.json.JsonArray -> el
                el is kotlinx.serialization.json.JsonObject && el["data"] is kotlinx.serialization.json.JsonArray -> el["data"]!!.jsonArray
                else -> null
            }
            if (arr != null) {
                for (item in arr) {
                    val obj = item.jsonObject
                    obj["id"]?.jsonPrimitive?.content?.let { ids.add(it) }
                }
            }
            Result.success(ids)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun chat(
        meta: NineProviderMeta,
        creds: NineProviderCredentials,
        modelId: String,
        messages: List<Map<String, String>>,
        stream: Boolean = false,
    ): Result<String> {
        val base = resolveBaseUrl(meta, creds)
        if (base.isBlank()) return Result.failure(IllegalStateException("No baseUrl for ${meta.id}"))
        return try {
            val isClaude = meta.format == "claude"
            val url = base
            val resp = if (isClaude) {
                httpClient().post(url) {
                    contentType(ContentType.Application.Json)
                    val auth = resolveAuthHeader(meta, creds) ?: return Result.failure(IllegalStateException("Missing API key for ${meta.id}"))
                    header(auth.first, auth.second)
                    header("anthropic-version", "2023-06-01")
                    setBody(mapOf(
                        "model" to modelId,
                        "max_tokens" to 1024,
                        "messages" to messages.map { mapOf("role" to (it["role"] ?: "user"), "content" to (it["content"] ?: "")) }
                    ))
                }
            } else {
                httpClient().post(url) {
                    contentType(ContentType.Application.Json)
                    val auth = resolveAuthHeader(meta, creds)
                    if (auth != null) {
                        if (auth.first == "Authorization") bearerAuth(auth.second.removePrefix("Bearer ").trim())
                        else header(auth.first, auth.second)
                    } else if (meta.category == "apikey") {
                        return Result.failure(IllegalStateException("Missing API key for ${meta.id}"))
                    }
                    setBody(mapOf(
                        "model" to modelId,
                        "messages" to messages.map { mapOf("role" to (it["role"] ?: "user"), "content" to (it["content"] ?: "")) },
                        "stream" to stream,
                    ))
                }
            }
            if (!resp.status.isSuccess()) return Result.failure(IllegalStateException("Chat ${resp.status}: ${resp.bodyAsText().take(600)}"))
            val text = resp.bodyAsText()
            try {
                val el = jsonLenient.parseToJsonElement(text)
                if (el is kotlinx.serialization.json.JsonObject) {
                    el["choices"]?.jsonArray?.firstOrNull()?.jsonObject?.get("message")?.jsonObject?.get("content")?.let {
                        return Result.success(it.jsonPrimitive.content)
                    }
                    el["content"]?.jsonArray?.firstOrNull()?.jsonObject?.get("text")?.let {
                        return Result.success(it.jsonPrimitive.content)
                    }
                }
            } catch (_: Exception) { }
            Result.success(text.take(4000))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
