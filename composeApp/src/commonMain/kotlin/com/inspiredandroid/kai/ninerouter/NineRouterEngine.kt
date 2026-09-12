package com.inspiredandroid.kai.ninerouter

import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import com.inspiredandroid.kai.httpClient
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Standalone in-app engine — direct HTTP to upstream, no Mido/9Router server.
 * Phase 1: OpenAI-compatible + Cloudflare AI (template {accountId}) + Claude-format.
 */
object NineRouterEngine {
    private val jsonLenient = Json { ignoreUnknownKeys = true; isLenient = true }

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
                // Fallback: derive /models from baseUrl if possible
                val base = resolveBaseUrl(meta, creds)
                // Heuristic: replace trailing /chat/completions with /models
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
            // OpenAI format: { data: [{id: "..."}] } or array
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
                // Claude messages format — simplified (no tools)
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
            // Extract content (non-stream)
            try {
                val el = jsonLenient.parseToJsonElement(text)
                if (el is kotlinx.serialization.json.JsonObject) {
                    // OpenAI: choices[0].message.content
                    el["choices"]?.jsonArray?.firstOrNull()?.jsonObject?.get("message")?.jsonObject?.get("content")?.let {
                        return Result.success(it.jsonPrimitive.content)
                    }
                    // Claude: content[0].text
                    el["content"]?.jsonArray?.firstOrNull()?.jsonObject?.get("text")?.let {
                        return Result.success(it.jsonPrimitive.content)
                    }
                }
            } catch (_: Exception) { /* fallthrough to raw */ }
            Result.success(text.take(4000))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
