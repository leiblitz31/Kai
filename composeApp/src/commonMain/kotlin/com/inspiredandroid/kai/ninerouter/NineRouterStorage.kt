package com.inspiredandroid.kai.ninerouter

import com.inspiredandroid.kai.data.AppSettings
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class NineProviderCredentials(
    val apiKey: String = "",
    val accountId: String = "", // for Cloudflare-style {accountId} template
    val enabled: Boolean = true,
)

@Serializable
data class NineRouterConfig(
    val providers: Map<String, NineProviderCredentials> = emptyMap(),
    val combos: List<NineCombo> = emptyList(),
)

@Serializable
data class NineCombo(
    val id: String,
    val name: String,
    val models: List<String>, // e.g. ["cf/@cf/meta/llama-3.2-1b-instruct", "openai/gpt-4o"]
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

fun AppSettings.getNineProviderCredentials(providerId: String): NineProviderCredentials? =
    getNineRouterConfig().providers[providerId]

fun AppSettings.setNineProviderCredentials(providerId: String, creds: NineProviderCredentials) {
    val cur = getNineRouterConfig()
    setNineRouterConfig(cur.copy(providers = cur.providers + (providerId to creds)))
}

fun AppSettings.removeNineProvider(providerId: String) {
    val cur = getNineRouterConfig()
    setNineRouterConfig(cur.copy(providers = cur.providers - providerId))
}

fun AppSettings.getNineCombos(): List<NineCombo> = getNineRouterConfig().combos

fun AppSettings.setNineCombos(combos: List<NineCombo>) {
    val cur = getNineRouterConfig()
    setNineRouterConfig(cur.copy(combos = combos))
}

