package com.inspiredandroid.kai.ninerouter

import com.inspiredandroid.kai.data.AppSettings
import kotlin.time.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

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
data class NineRouterConfig(
    val connections: List<NineConnection> = emptyList(),
    val combos: List<NineCombo> = emptyList(),
    // Token-saver pipeline (mirrors 9Router endpoint settings; all fail-open).
    val rtkEnabled: Boolean = true,
    val cavemanEnabled: Boolean = false,
    val cavemanLevel: String = "full",
    val ponytailEnabled: Boolean = false,
    val ponytailLevel: String = "full",
)

private const val KEY_NINEROUTER_CONFIG = "ninerouter_config_json"
private val jsonLenient = Json { ignoreUnknownKeys = true; encodeDefaults = true }

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

fun AppSettings.toggleNineConnection(connectionId: String, enabled: Boolean) {
    val cur = getNineRouterConfig()
    val updated = cur.connections.map {
        if (it.id == connectionId) it.copy(enabled = enabled) else it
    }
    setNineRouterConfig(cur.copy(connections = updated))
}

fun AppSettings.removeNineCombo(comboId: String) {
    val cur = getNineRouterConfig()
    setNineRouterConfig(cur.copy(combos = cur.combos.filterNot { it.id == comboId }))
}
