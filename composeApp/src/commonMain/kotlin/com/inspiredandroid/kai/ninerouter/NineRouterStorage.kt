package com.inspiredandroid.kai.ninerouter

import com.inspiredandroid.kai.data.AppSettings
import kotlin.time.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

@Serializable
data class NineConnection(
    val id: String,
    val provider: String,
    val name: String = "",
    val apiKey: String = "",
    val accountId: String = "",
    // Optional relay (Vercel/Cloudflare worker) to bypass IP rate limits.
    // When set, requests POST to relayUrl with x-relay-target/x-relay-path headers.
    val relayUrl: String = "",
    // OAuth session tokens (manual paste from browser login; no auto-refresh on device).
    val accessToken: String = "",
    val refreshToken: String = "",
    val expiresAt: Long = 0L,
    val priority: Int = 1,
    val enabled: Boolean = true,
    val consecutiveUseCount: Int = 0,
    val lastUsedAt: Long = 0L,
    val modelLocks: Map<String, Long> = emptyMap(),
)

@Serializable
data class NineCombo(
    val id: String,
    val name: String,
    val models: List<String>,
)

@Serializable
data class NineCustomModel(
    val id: String,
    val provider: String,
    val name: String = "",
)

@Serializable
data class NineRouterConfig(
    val connections: List<NineConnection> = emptyList(),
    val combos: List<NineCombo> = emptyList(),
    val customProviders: List<NineProviderMeta> = emptyList(),
    val deletedProviderIds: Set<String> = emptySet(),
    val customModels: List<NineCustomModel> = emptyList(),
    val deletedModelIds: Set<String> = emptySet(),
    // Token-saver pipeline (mirrors 9Router endpoint settings; all fail-open).
    val rtkEnabled: Boolean = true,
    val cavemanEnabled: Boolean = false,
    val cavemanLevel: String = "full",
    val ponytailEnabled: Boolean = false,
    val ponytailLevel: String = "full",
)

@Serializable
data class NineImportResult(
    val connectionsCount: Int = 0,
    val combosCount: Int = 0,
    val isSuccess: Boolean = true,
    val message: String = "",
)

private const val KEY_NINEROUTER_CONFIG = "ninerouter_config_json"
private val jsonLenient = Json { ignoreUnknownKeys = true; encodeDefaults = true; isLenient = true; prettyPrint = true }

fun AppSettings.getNineRouterConfig(): NineRouterConfig {
    val raw = settings.getString(KEY_NINEROUTER_CONFIG, "")
    if (raw.isBlank()) return NineRouterConfig()
    return try { jsonLenient.decodeFromString(raw) } catch (_: Exception) { NineRouterConfig() }
}

fun AppSettings.setNineRouterConfig(config: NineRouterConfig) {
    settings.putString(KEY_NINEROUTER_CONFIG, jsonLenient.encodeToString(config))
}

fun AppSettings.getNineConnections(): List<NineConnection> = getNineRouterConfig().connections

fun AppSettings.getNineConnectionsForProvider(provider: String): List<NineConnection> {
    return getNineConnections().filter { it.provider.equals(provider, ignoreCase = true) && it.enabled }
}

fun AppSettings.addOrUpdateNineConnection(conn: NineConnection) {
    val cur = getNineRouterConfig()
    val updated = cur.connections.filterNot { it.id == conn.id } + conn
    setNineRouterConfig(cur.copy(connections = updated))
}

fun AppSettings.removeNineConnection(connectionId: String) {
    val cur = getNineRouterConfig()
    setNineRouterConfig(cur.copy(connections = cur.connections.filterNot { it.id == connectionId }))
}

fun AppSettings.toggleNineConnection(connectionId: String, enabled: Boolean) {
    val cur = getNineRouterConfig()
    val updated = cur.connections.map {
        if (it.id == connectionId) it.copy(enabled = enabled) else it
    }
    setNineRouterConfig(cur.copy(connections = updated))
}

fun AppSettings.lockNineConnectionModel(connectionId: String, model: String, cooldownMs: Long) {
    val cur = getNineRouterConfig()
    val now = Clock.System.now().toEpochMilliseconds()
    val expiry = now + cooldownMs
    val updated = cur.connections.map { conn ->
        if (conn.id == connectionId) {
            val key = if (model.isBlank()) "__all" else model
            conn.copy(modelLocks = conn.modelLocks + (key to expiry))
        } else {
            conn
        }
    }
    setNineRouterConfig(cur.copy(connections = updated))
}

fun AppSettings.getNineCombos(): List<NineCombo> = getNineRouterConfig().combos

fun AppSettings.setNineCombos(combos: List<NineCombo>) {
    val cur = getNineRouterConfig()
    setNineRouterConfig(cur.copy(combos = combos))
}

fun AppSettings.removeNineCombo(comboId: String) {
    val cur = getNineRouterConfig()
    setNineRouterConfig(cur.copy(combos = cur.combos.filterNot { it.id == comboId }))
}

fun AppSettings.addCustomProvider(meta: NineProviderMeta) {
    val cur = getNineRouterConfig()
    val updated = cur.customProviders.filterNot { it.id == meta.id } + meta.copy(isCustom = true)
    setNineRouterConfig(cur.copy(customProviders = updated, deletedProviderIds = cur.deletedProviderIds - meta.id))
}

fun AppSettings.removeProvider(providerId: String) {
    val cur = getNineRouterConfig()
    val isCustom = cur.customProviders.any { it.id == providerId }
    val updatedCustom = cur.customProviders.filterNot { it.id == providerId }
    val updatedDeleted = if (!isCustom) cur.deletedProviderIds + providerId else cur.deletedProviderIds
    val updatedConns = cur.connections.filterNot { it.provider.equals(providerId, ignoreCase = true) }
    setNineRouterConfig(cur.copy(
        customProviders = updatedCustom,
        deletedProviderIds = updatedDeleted,
        connections = updatedConns,
    ))
}

fun AppSettings.addCustomModel(model: NineCustomModel) {
    val cur = getNineRouterConfig()
    val updated = cur.customModels.filterNot { it.id == model.id } + model
    setNineRouterConfig(cur.copy(customModels = updated, deletedModelIds = cur.deletedModelIds - model.id))
}

fun AppSettings.removeModel(modelId: String) {
    val cur = getNineRouterConfig()
    val updatedCustom = cur.customModels.filterNot { it.id == modelId }
    setNineRouterConfig(cur.copy(
        customModels = updatedCustom,
        deletedModelIds = cur.deletedModelIds + modelId,
    ))
}

fun AppSettings.restoreAllProviders() {
    val cur = getNineRouterConfig()
    setNineRouterConfig(cur.copy(deletedProviderIds = emptySet()))
}

fun AppSettings.restoreAllModels() {
    val cur = getNineRouterConfig()
    setNineRouterConfig(cur.copy(deletedModelIds = emptySet()))
}

fun AppSettings.importNineConnectionsBulk(text: String): Int {
    val cur = getNineRouterConfig()
    val lines = text.trim().lines()
    val newConns = mutableListOf<NineConnection>()
    var count = 0

    for (line in lines) {
        val trimmed = line.trim()
        if (trimmed.isBlank() || trimmed.startsWith("#")) continue
        val parts = trimmed.split("|").map { it.trim() }
        val id = "conn_" + Clock.System.now().toEpochMilliseconds() + "_" + count

        if (parts.size >= 4 && parts[2].contains("accounts/")) {
            val name = parts[0]
            val apiUrl = parts[2]
            val apiKey = parts[3]
            val accRegex = Regex("""accounts/([^/]+)/ai""")
            val match = accRegex.find(apiUrl)
            val accId = match?.groupValues?.get(1) ?: ""
            newConns.add(NineConnection(id = id, provider = "cloudflare-ai", name = name, apiKey = apiKey, accountId = accId))
            count++
        } else if (parts.size == 4) {
            val prov = parts[0]
            val name = parts[1]
            val accId = parts[2]
            val apiKey = parts[3]
            newConns.add(NineConnection(id = id, provider = prov, name = name, apiKey = apiKey, accountId = accId))
            count++
        } else if (parts.size == 3) {
            val prov = parts[0]
            val name = parts[1]
            val apiKey = parts[2]
            newConns.add(NineConnection(id = id, provider = prov, name = name, apiKey = apiKey))
            count++
        } else if (parts.size == 2) {
            val prov = parts[0]
            val apiKey = parts[1]
            newConns.add(NineConnection(id = id, provider = prov, name = "$prov account", apiKey = apiKey))
            count++
        }
    }

    if (newConns.isNotEmpty()) {
        setNineRouterConfig(cur.copy(connections = cur.connections + newConns))
    }
    return count
}

data class NineParsedBackup(
    val connections: List<NineConnection> = emptyList(),
    val combos: List<NineCombo> = emptyList(),
    val customProviders: List<NineProviderMeta> = emptyList(),
    val deletedProviderIds: Set<String> = emptySet(),
    val customModels: List<NineCustomModel> = emptyList(),
    val deletedModelIds: Set<String> = emptySet(),
)

fun parseNineRouterJson(raw: String): Pair<List<NineConnection>, List<NineCombo>>? {
    return parseNineRouterJsonFull(raw)?.let { it.connections to it.combos }
}

fun parseNineRouterJsonFull(raw: String): NineParsedBackup? {
    val json = Json { ignoreUnknownKeys = true; isLenient = true }
    return try {
        val root = json.parseToJsonElement(raw.trim())
        val conns = mutableListOf<NineConnection>()
        val combos = mutableListOf<NineCombo>()
        val customProviders = mutableListOf<NineProviderMeta>()
        val deletedProviderIds = mutableSetOf<String>()
        val customModels = mutableListOf<NineCustomModel>()
        val deletedModelIds = mutableSetOf<String>()
        val now = Clock.System.now().toEpochMilliseconds()

        if (root is kotlinx.serialization.json.JsonObject) {
            val obj = root.jsonObject
            // 1. Check if 9Router backup format (has providerConnections)
            val pcArray = obj["providerConnections"]?.jsonArray
            if (pcArray != null) {
                pcArray.forEachIndexed { i, elem ->
                    val c = elem.jsonObject
                    val id = c["id"]?.jsonPrimitive?.content ?: ("conn_${now}_$i")
                    val provider = c["provider"]?.jsonPrimitive?.content ?: "openai"
                    val name = c["name"]?.jsonPrimitive?.content
                        ?: c["displayName"]?.jsonPrimitive?.content
                        ?: c["email"]?.jsonPrimitive?.content
                        ?: ""
                    val apiKey = c["apiKey"]?.jsonPrimitive?.content
                        ?: c["token"]?.jsonPrimitive?.content
                        ?: ""
                    val psd = c["providerSpecificData"]?.jsonObject
                    val accountId = psd?.get("accountId")?.jsonPrimitive?.content
                        ?: c["accountId"]?.jsonPrimitive?.content
                        ?: ""
                    val relayUrl = psd?.get("vercelRelayUrl")?.jsonPrimitive?.content
                        ?: psd?.get("connectionProxyUrl")?.jsonPrimitive?.content
                        ?: c["relayUrl"]?.jsonPrimitive?.content
                        ?: ""
                    val accessToken = c["accessToken"]?.jsonPrimitive?.content ?: ""
                    val refreshToken = c["refreshToken"]?.jsonPrimitive?.content ?: ""
                    val priority = c["priority"]?.jsonPrimitive?.intOrNull ?: 1
                    val enabled = c["isActive"]?.jsonPrimitive?.booleanOrNull
                        ?: c["enabled"]?.jsonPrimitive?.booleanOrNull
                        ?: true

                    conns.add(
                        NineConnection(
                            id = id,
                            provider = provider,
                            name = name,
                            apiKey = apiKey,
                            accountId = accountId,
                            relayUrl = relayUrl,
                            accessToken = accessToken,
                            refreshToken = refreshToken,
                            priority = priority,
                            enabled = enabled,
                        )
                    )
                }
            } else if (obj["connections"]?.jsonArray != null) {
                // Kai format
                val connArray = obj["connections"]!!.jsonArray
                connArray.forEachIndexed { i, elem ->
                    val c = elem.jsonObject
                    val id = c["id"]?.jsonPrimitive?.content ?: ("conn_${now}_$i")
                    val provider = c["provider"]?.jsonPrimitive?.content ?: "openai"
                    val name = c["name"]?.jsonPrimitive?.content ?: ""
                    val apiKey = c["apiKey"]?.jsonPrimitive?.content ?: ""
                    val accountId = c["accountId"]?.jsonPrimitive?.content ?: ""
                    val relayUrl = c["relayUrl"]?.jsonPrimitive?.content ?: ""
                    val accessToken = c["accessToken"]?.jsonPrimitive?.content ?: ""
                    val refreshToken = c["refreshToken"]?.jsonPrimitive?.content ?: ""
                    val priority = c["priority"]?.jsonPrimitive?.intOrNull ?: 1
                    val enabled = c["enabled"]?.jsonPrimitive?.booleanOrNull ?: true

                    conns.add(
                        NineConnection(
                            id = id,
                            provider = provider,
                            name = name,
                            apiKey = apiKey,
                            accountId = accountId,
                            relayUrl = relayUrl,
                            accessToken = accessToken,
                            refreshToken = refreshToken,
                            priority = priority,
                            enabled = enabled,
                        )
                    )
                }
            }

            // Parse combos if present
            val comboArray = obj["combos"]?.jsonArray
            if (comboArray != null) {
                comboArray.forEachIndexed { i, elem ->
                    val cb = elem.jsonObject
                    val id = cb["id"]?.jsonPrimitive?.content ?: ("combo_${now}_$i")
                    val name = cb["name"]?.jsonPrimitive?.content ?: "Combo $i"
                    val models = cb["models"]?.jsonArray?.mapNotNull { it.jsonPrimitive.content } ?: emptyList()
                    if (models.isNotEmpty()) {
                        combos.add(NineCombo(id = id, name = name, models = models))
                    }
                }
            }
            // Parse Kai extensions: customProviders / deletedProviderIds / customModels / deletedModelIds
            obj["customProviders"]?.jsonArray?.forEach { elem ->
                try { customProviders.add(json.decodeFromJsonElement<NineProviderMeta>(elem)) } catch (_: Exception) {}
            }
            obj["deletedProviderIds"]?.jsonArray?.forEach { elem ->
                try { deletedProviderIds.add(elem.jsonPrimitive.content) } catch (_: Exception) {}
            }
            obj["customModels"]?.jsonArray?.forEach { elem ->
                try { customModels.add(json.decodeFromJsonElement<NineCustomModel>(elem)) } catch (_: Exception) {}
            }
            obj["deletedModelIds"]?.jsonArray?.forEach { elem ->
                try { deletedModelIds.add(elem.jsonPrimitive.content) } catch (_: Exception) {}
            }
        } else if (root is kotlinx.serialization.json.JsonArray) {
            // Array of connections
            root.jsonArray.forEachIndexed { i, elem ->
                val c = elem.jsonObject
                val id = c["id"]?.jsonPrimitive?.content ?: ("conn_${now}_$i")
                val provider = c["provider"]?.jsonPrimitive?.content ?: "openai"
                val name = c["name"]?.jsonPrimitive?.content ?: ""
                val apiKey = c["apiKey"]?.jsonPrimitive?.content ?: ""
                val accountId = c["accountId"]?.jsonPrimitive?.content ?: ""
                val relayUrl = c["relayUrl"]?.jsonPrimitive?.content ?: ""
                val accessToken = c["accessToken"]?.jsonPrimitive?.content ?: ""
                val refreshToken = c["refreshToken"]?.jsonPrimitive?.content ?: ""
                val priority = c["priority"]?.jsonPrimitive?.intOrNull ?: 1
                val enabled = c["enabled"]?.jsonPrimitive?.booleanOrNull ?: true

                conns.add(
                    NineConnection(
                        id = id,
                        provider = provider,
                        name = name,
                        apiKey = apiKey,
                        accountId = accountId,
                        relayUrl = relayUrl,
                        accessToken = accessToken,
                        refreshToken = refreshToken,
                        priority = priority,
                        enabled = enabled,
                    )
                )
            }
        }

        val hasAny = conns.isNotEmpty() || combos.isNotEmpty() || customProviders.isNotEmpty() || deletedProviderIds.isNotEmpty() || customModels.isNotEmpty() || deletedModelIds.isNotEmpty()
        if (hasAny) NineParsedBackup(
            connections = conns,
            combos = combos,
            customProviders = customProviders,
            deletedProviderIds = deletedProviderIds,
            customModels = customModels,
            deletedModelIds = deletedModelIds,
        ) else null
    } catch (_: Exception) {
        null
    }
}

fun AppSettings.importNineRouterConfig(raw: String, replaceAll: Boolean = false): NineImportResult {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return NineImportResult(0, 0, false, "Input teks/file kosong")

    // Try parsing as JSON (9Router backup or Kai export) — full Kai round-trip via parseNineRouterJsonFull
    val parsed = parseNineRouterJsonFull(trimmed)
    if (parsed != null && (parsed.connections.isNotEmpty() || parsed.combos.isNotEmpty() || parsed.customProviders.isNotEmpty() || parsed.customModels.isNotEmpty() || parsed.deletedProviderIds.isNotEmpty() || parsed.deletedModelIds.isNotEmpty())) {
        val conns = parsed.connections
        val combos = parsed.combos
        val cur = getNineRouterConfig()
        val finalConns = if (replaceAll) conns else (cur.connections.filterNot { existing ->
            conns.any { it.id == existing.id || (it.provider == existing.provider && it.name == existing.name && it.name.isNotBlank()) }
        } + conns)

        val finalCombos = if (replaceAll) combos else (cur.combos.filterNot { existing ->
            combos.any { it.name.equals(existing.name, ignoreCase = true) }
        } + combos)

        val finalCustomProviders = if (replaceAll) parsed.customProviders else {
            val byId = (cur.customProviders + parsed.customProviders).groupBy { it.id }
            byId.values.map { it.last() }
        }
        val finalDeletedProviderIds = if (replaceAll) parsed.deletedProviderIds else cur.deletedProviderIds + parsed.deletedProviderIds
        val finalCustomModels = if (replaceAll) parsed.customModels else {
            val byId = (cur.customModels + parsed.customModels).groupBy { it.id }
            byId.values.map { it.last() }
        }
        val finalDeletedModelIds = if (replaceAll) parsed.deletedModelIds else cur.deletedModelIds + parsed.deletedModelIds

        setNineRouterConfig(cur.copy(
            connections = finalConns,
            combos = finalCombos,
            customProviders = finalCustomProviders,
            deletedProviderIds = finalDeletedProviderIds,
            customModels = finalCustomModels,
            deletedModelIds = finalDeletedModelIds,
        ))
        return NineImportResult(
            connectionsCount = conns.size,
            combosCount = combos.size,
            isSuccess = true,
            message = "Sukses mengimpor ${conns.size} akun dan ${combos.size} combo!",
        )
    }

    // Fallback: line-by-line (pipe/harvest format)
    val count = importNineConnectionsBulk(trimmed)
    return if (count > 0) {
        NineImportResult(
            connectionsCount = count,
            combosCount = 0,
            isSuccess = true,
            message = "Sukses mengimpor $count akun dari baris teks!",
        )
    } else {
        NineImportResult(0, 0, false, "Format tidak valid. Gunakan JSON backup 9Router atau baris teks akun.")
    }
}

fun AppSettings.exportNineRouterBackupJson(): String {
    val config = getNineRouterConfig()
    val exportObj = buildJsonObject {
        put("connections", jsonLenient.encodeToJsonElement(config.connections))
        put("combos", jsonLenient.encodeToJsonElement(config.combos))
        put("customProviders", jsonLenient.encodeToJsonElement(config.customProviders))
        put("deletedProviderIds", jsonLenient.encodeToJsonElement(config.deletedProviderIds.toList()))
        put("customModels", jsonLenient.encodeToJsonElement(config.customModels))
        put("deletedModelIds", jsonLenient.encodeToJsonElement(config.deletedModelIds.toList()))
        put("providerConnections", buildJsonArray {
            config.connections.forEach { conn ->
                add(buildJsonObject {
                    put("id", conn.id)
                    put("provider", conn.provider)
                    put("name", conn.name)
                    put("apiKey", conn.apiKey)
                    put("accessToken", conn.accessToken)
                    put("refreshToken", conn.refreshToken)
                    put("priority", conn.priority)
                    put("isActive", conn.enabled)
                    if (conn.accountId.isNotBlank() || conn.relayUrl.isNotBlank()) {
                        put("providerSpecificData", buildJsonObject {
                            if (conn.accountId.isNotBlank()) put("accountId", conn.accountId)
                            if (conn.relayUrl.isNotBlank()) put("vercelRelayUrl", conn.relayUrl)
                        })
                    }
                })
            }
        })
        put("settings", buildJsonObject {
            put("rtkEnabled", config.rtkEnabled)
            put("cavemanEnabled", config.cavemanEnabled)
            put("ponytailEnabled", config.ponytailEnabled)
        })
    }
    return jsonLenient.encodeToString(exportObj)
}
