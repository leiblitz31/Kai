package com.inspiredandroid.kai.ninerouter

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inspiredandroid.kai.data.AppSettings
import com.inspiredandroid.kai.saveFileToDevice
import com.inspiredandroid.kai.ui.components.KaiChip
import com.inspiredandroid.kai.ui.handCursor
import com.inspiredandroid.kai.ui.kaiAdaptiveCardBorder
import com.inspiredandroid.kai.ui.kaiAdaptiveCardColors
import com.inspiredandroid.kai.ui.rememberCopyToClipboard
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.readBytes
import kotlin.time.Clock
import kotlinx.coroutines.launch

fun NineProviderMeta.getDisplayName(): String = when (id) {
    "cloudflare-ai" -> "Cloudflare Workers AI"
    "openai" -> "OpenAI"
    "deepseek" -> "DeepSeek"
    "anthropic" -> "Anthropic"
    "gemini" -> "Google Gemini"
    "groq" -> "Groq"
    "github" -> "GitHub Copilot"
    "kiro" -> "Kiro AI"
    "freebuff" -> "Freebuff"
    "opencode" -> "OpenCode Free"
    "antigravity" -> "Google Antigravity"
    "codex" -> "OpenAI Codex"
    "qwen" -> "Qwen Code"
    "openrouter" -> "OpenRouter"
    "mistral" -> "Mistral AI"
    "kimi" -> "Moonshot Kimi"
    "glm", "glm-cn" -> "Zhipu GLM"
    "minimax", "minimax-cn" -> "MiniMax"
    "nvidia" -> "NVIDIA NIM"
    "together" -> "Together AI"
    "cerebras" -> "Cerebras"
    "cohere" -> "Cohere"
    "perplexity", "perplexity-web" -> "Perplexity"
    "chutes" -> "Chutes AI"
    "sambanova" -> "SambaNova"
    "siliconflow" -> "SiliconFlow"
    "voyage-ai" -> "Voyage AI"
    "fireworks" -> "Fireworks AI"
    "deepinfra" -> "DeepInfra"
    "ollama", "ollama-local" -> "Ollama"
    "cursor" -> "Cursor IDE"
    "trae" -> "Trae IDE"
    "windsurf" -> "Windsurf IDE"
    "xai", "grok-cli", "grok-web" -> "xAI Grok"
    "kilocode" -> "Kilocode"
    "kimchi" -> "Kimchi AI"
    "poolside" -> "Poolside"
    "huggingface" -> "HuggingFace"
    "vertex", "vertex-partner" -> "Google Vertex AI"
    else -> if (displayName.isNotBlank()) displayName else id.split("-").joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NineRouterDashboard(
    modifier: Modifier = Modifier,
    appSettings: AppSettings = org.koin.compose.koinInject(),
) {
    val coroutineScope = rememberCoroutineScope()
    var config by remember { mutableStateOf(appSettings.getNineRouterConfig()) }
    LaunchedEffect(Unit) { config = appSettings.getNineRouterConfig() }

    // Dashboard Sub-tabs: 0 = Providers, 1 = Combos, 2 = Token Saver, 3 = Backup & Import
    var activeSubTab by remember { mutableStateOf(0) }

    // Selected provider for detail bottom sheet
    var activeProviderDetail by remember { mutableStateOf<NineProviderMeta?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Ping status cache: connectionId -> NineValidationResult
    val pingResults = remember { mutableStateMapOf<String, NineValidationResult>() }
    val isPingingMap = remember { mutableStateMapOf<String, Boolean>() }

    val allProviders = NineRouterRegistry.getAll(config)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Top 9Router Header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = kaiAdaptiveCardColors(),
            border = kaiAdaptiveCardBorder(),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "9Router",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            ) {
                                Text(
                                    text = "v4.0.0 Native",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                        Text(
                            text = "Universal AI Router • ${allProviders.size} Provider • Direct Upstream",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // Stats summary pills
                val connectedProvidersCount = config.connections
                    .filter { it.enabled && (it.apiKey.isNotBlank() || it.accessToken.isNotBlank() || it.provider == "opencode") }
                    .map { it.provider }
                    .distinct()
                    .size

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF10B981)))
                            Text("👥 ${config.connections.size} Akun Terhubung", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF3B82F6)))
                            Text("🏢 $connectedProvidersCount / ${allProviders.size} Provider Aktif", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF8B5CF6)))
                            Text("🔀 ${config.combos.size} Combo", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            val rtkDot = if (config.rtkEnabled) Color(0xFF10B981) else Color.Gray
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(rtkDot))
                            Text("⚡ RTK: ${if (config.rtkEnabled) "ON" else "OFF"}", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // Segmented Navigation Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    KaiChip(selected = (activeSubTab == 0), onClick = { activeSubTab = 0 }) {
                        Text("🏢 Providers", style = MaterialTheme.typography.labelMedium)
                    }
                    KaiChip(selected = (activeSubTab == 1), onClick = { activeSubTab = 1 }) {
                        Text("🔀 Combos", style = MaterialTheme.typography.labelMedium)
                    }
                    KaiChip(selected = (activeSubTab == 2), onClick = { activeSubTab = 2 }) {
                        Text("⚡ Token Saver", style = MaterialTheme.typography.labelMedium)
                    }
                    KaiChip(selected = (activeSubTab == 3), onClick = { activeSubTab = 3 }) {
                        Text("💾 Backup & Import", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        // Subtab Content
        when (activeSubTab) {
            0 -> {
                ProvidersSubTab(
                    config = config,
                    onOpenProviderDetail = { activeProviderDetail = it },
                    onQuickActivateOpenCode = {
                        val hasOc = config.connections.any { it.provider == "opencode" }
                        if (!hasOc) {
                            val ocConn = NineConnection(
                                id = "conn_opencode_" + Clock.System.now().toEpochMilliseconds(),
                                provider = "opencode",
                                name = "OpenCode Free (Zero Auth)",
                                apiKey = "",
                            )
                            appSettings.addOrUpdateNineConnection(ocConn)
                            config = appSettings.getNineRouterConfig()
                        }
                    },
                    onAddCustomProvider = { newMeta ->
                        appSettings.addCustomProvider(newMeta)
                        config = appSettings.getNineRouterConfig()
                    },
                    onRestoreAllProviders = {
                        appSettings.restoreAllProviders()
                        config = appSettings.getNineRouterConfig()
                    },
                )
            }
            1 -> {
                CombosSubTab(
                    config = config,
                    onSaveCombo = { name, models ->
                        val newCombo = NineCombo(
                            id = "combo_" + Clock.System.now().toEpochMilliseconds(),
                            name = name,
                            models = models,
                        )
                        appSettings.setNineCombos(config.combos + newCombo)
                        config = appSettings.getNineRouterConfig()
                    },
                    onDeleteCombo = { comboId ->
                        appSettings.removeNineCombo(comboId)
                        config = appSettings.getNineRouterConfig()
                    },
                )
            }
            2 -> {
                TokenSaverSubTab(
                    config = config,
                    onUpdateConfig = { updated ->
                        appSettings.setNineRouterConfig(updated)
                        config = appSettings.getNineRouterConfig()
                    },
                )
            }
            3 -> {
                BackupImportSubTab(
                    appSettings = appSettings,
                    onConfigChanged = { config = appSettings.getNineRouterConfig() },
                )
            }
        }
    }

    // Provider Detail Sheet (1:1 ConnectionsCard + ModelsCard in 9Router)
    activeProviderDetail?.let { meta ->
        ModalBottomSheet(
            onDismissRequest = { activeProviderDetail = null },
            sheetState = sheetState,
        ) {
            ProviderDetailSheetContent(
                meta = meta,
                config = config,
                pingResults = pingResults,
                isPingingMap = isPingingMap,
                onAddConnection = { newConn ->
                    appSettings.addOrUpdateNineConnection(newConn)
                    config = appSettings.getNineRouterConfig()
                },
                onToggleConnection = { connId, active ->
                    appSettings.toggleNineConnection(connId, active)
                    config = appSettings.getNineRouterConfig()
                },
                onDeleteConnection = { connId ->
                    appSettings.removeNineConnection(connId)
                    config = appSettings.getNineRouterConfig()
                },
                onPingConnection = { conn ->
                    coroutineScope.launch {
                        isPingingMap[conn.id] = true
                        val res = NineRouterEngine.pingConnection(meta, conn)
                        pingResults[conn.id] = res
                        isPingingMap[conn.id] = false
                    }
                },
                onBulkImportForProvider = { text ->
                    val lines = text.trim().lines()
                    var count = 0
                    for (line in lines) {
                        val trimmed = line.trim()
                        if (trimmed.isBlank()) continue
                        val parts = trimmed.split("|").map { it.trim() }
                        val id = "conn_" + Clock.System.now().toEpochMilliseconds() + "_" + count
                        if (meta.needsAccountId && parts.size >= 3) {
                            appSettings.addOrUpdateNineConnection(
                                NineConnection(id = id, provider = meta.id, name = parts[0], accountId = parts[1], apiKey = parts[2])
                            )
                            count++
                        } else if (parts.size >= 2) {
                            appSettings.addOrUpdateNineConnection(
                                NineConnection(id = id, provider = meta.id, name = parts[0], apiKey = parts[1])
                            )
                            count++
                        } else {
                            appSettings.addOrUpdateNineConnection(
                                NineConnection(id = id, provider = meta.id, name = "${meta.alias} #$count", apiKey = parts[0])
                            )
                            count++
                        }
                    }
                    config = appSettings.getNineRouterConfig()
                    count
                },
                onDeleteProvider = { providerId ->
                    appSettings.removeProvider(providerId)
                    config = appSettings.getNineRouterConfig()
                    activeProviderDetail = null
                },
                onAddModel = { customModel ->
                    appSettings.addCustomModel(customModel)
                    config = appSettings.getNineRouterConfig()
                },
                onDeleteModel = { modelId ->
                    appSettings.removeModel(modelId)
                    config = appSettings.getNineRouterConfig()
                },
                onRestoreModelsForProvider = {
                    val defaultModels = NineRouterRegistry.getDefaultModelsForProvider(meta.id).map { it.first }
                    val newDeleted = config.deletedModelIds.filterNot { it in defaultModels }.toSet()
                    appSettings.setNineRouterConfig(config.copy(deletedModelIds = newDeleted))
                    config = appSettings.getNineRouterConfig()
                },
            )
        }
    }
}

@Composable
private fun ProvidersSubTab(
    config: NineRouterConfig,
    onOpenProviderDetail: (NineProviderMeta) -> Unit,
    onQuickActivateOpenCode: () -> Unit,
    onAddCustomProvider: (NineProviderMeta) -> Unit,
    onRestoreAllProviders: () -> Unit,
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var onlyConnected by remember { mutableStateOf(false) }
    var showAddProviderDialog by remember { mutableStateOf(false) }

    val allProviders = NineRouterRegistry.getAll(config)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // OpenCode Free Quick Banner (if not yet connected)
        val hasOpenCode = config.connections.any { it.provider == "opencode" && it.enabled }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (hasOpenCode) Color(0xFF10B981).copy(alpha = 0.12f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
            ),
            border = BorderStroke(1.dp, if (hasOpenCode) Color(0xFF10B981).copy(alpha = 0.3f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("🆓 OpenCode Free", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (hasOpenCode) Color(0xFF10B981).copy(alpha = 0.2f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        ) {
                            Text(
                                text = if (hasOpenCode) "Aktif" else "Zero Auth",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (hasOpenCode) Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                    Text(
                        text = if (hasOpenCode) "Model AI publik aktif tanpa auth. Siap dipakai langsung di chat." else "Akses model AI gratis tanpa perlu registrasi atau API key.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (!hasOpenCode) {
                    Button(
                        onClick = onQuickActivateOpenCode,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    ) {
                        Text("Aktifkan 1-Tap", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Cari provider atau alias (mis: cf, qwen, deepseek, groq)...") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
        )

        // Category filter chips
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            val cats = listOf(
                null to "Semua (${allProviders.size})",
                "free" to "🆓 Free",
                "freeTier" to "⚡ Free Tier",
                "oauth" to "🔐 OAuth",
                "apikey" to "🔑 API Key",
            )
            cats.forEach { (cat, label) ->
                KaiChip(selected = (selectedCategory == cat), onClick = { selectedCategory = cat }) {
                    Text(label, style = MaterialTheme.typography.labelSmall)
                }
            }
            KaiChip(selected = onlyConnected, onClick = { onlyConnected = !onlyConnected }) {
                Text(if (onlyConnected) "✓ Hanya Terhubung" else "Hanya Terhubung", style = MaterialTheme.typography.labelSmall)
            }
        }

        // Action Buttons Row: Add Custom Provider & Restore
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = { showAddProviderDialog = true },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text("+ Tambah Provider Kustom", style = MaterialTheme.typography.labelSmall)
            }

            if (config.deletedProviderIds.isNotEmpty()) {
                TextButton(
                    onClick = onRestoreAllProviders,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text("Pulihkan Provider (${config.deletedProviderIds.size})", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        // Add Custom Provider Form Card
        if (showAddProviderDialog) {
            AddCustomProviderCard(
                onSave = { newMeta ->
                    onAddCustomProvider(newMeta)
                    showAddProviderDialog = false
                },
                onCancel = { showAddProviderDialog = false },
            )
        }

        // Filter providers
        val filtered = allProviders.filter { meta ->
            val matchQuery = searchQuery.isBlank() ||
                meta.id.contains(searchQuery, ignoreCase = true) ||
                meta.alias.contains(searchQuery, ignoreCase = true) ||
                meta.aliases.any { it.contains(searchQuery, ignoreCase = true) } ||
                meta.getDisplayName().contains(searchQuery, ignoreCase = true)
            val matchCat = selectedCategory == null || meta.category.equals(selectedCategory, ignoreCase = true)
            val conns = config.connections.filter { it.provider.equals(meta.id, ignoreCase = true) }
            val matchConnected = !onlyConnected || conns.isNotEmpty()
            matchQuery && matchCat && matchConnected
        }

        Text(
            text = "Menampilkan ${filtered.size} provider",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // Provider cards list (1:1 9Router UI)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            filtered.forEach { meta ->
                val conns = config.connections.filter { it.provider.equals(meta.id, ignoreCase = true) }
                val activeConns = conns.filter { it.enabled }
                ProviderCardItem(
                    meta = meta,
                    connectionCount = conns.size,
                    activeCount = activeConns.size,
                    onClick = { onOpenProviderDetail(meta) },
                )
            }
        }
    }
}

@Composable
private fun AddCustomProviderCard(
    onSave: (NineProviderMeta) -> Unit,
    onCancel: () -> Unit,
) {
    var idDraft by remember { mutableStateOf("") }
    var nameDraft by remember { mutableStateOf("") }
    var aliasDraft by remember { mutableStateOf("") }
    var baseUrlDraft by remember { mutableStateOf("") }
    var formatDraft by remember { mutableStateOf("openai") }
    var categoryDraft by remember { mutableStateOf("apikey") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = kaiAdaptiveCardColors(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Tambah Provider Kustom Baru", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text(
                "Hubungkan server OpenAI-compatible atau Claude-compatible sendiri (vLLM, Ollama VPS, Local Server, dll).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedTextField(
                value = idDraft,
                onValueChange = { idDraft = it.lowercase().replace(" ", "-") },
                label = { Text("Provider ID unik (mis: vllm-local, ollama-vps)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
            )

            OutlinedTextField(
                value = nameDraft,
                onValueChange = { nameDraft = it },
                label = { Text("Nama Tampilan (mis: Local vLLM Server)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
            )

            OutlinedTextField(
                value = aliasDraft,
                onValueChange = { aliasDraft = it.lowercase().trim() },
                label = { Text("Prefix Alias Model (mis: vllm, myllm)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
            )

            OutlinedTextField(
                value = baseUrlDraft,
                onValueChange = { baseUrlDraft = it.trim() },
                label = { Text("Base URL (mis: http://192.168.1.50:8000/v1/chat/completions)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
            )

            Text("Format API:", style = MaterialTheme.typography.labelSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KaiChip(selected = (formatDraft == "openai"), onClick = { formatDraft = "openai" }) {
                    Text("OpenAI-Compatible", style = MaterialTheme.typography.labelSmall)
                }
                KaiChip(selected = (formatDraft == "claude"), onClick = { formatDraft = "claude" }) {
                    Text("Claude (Messages)", style = MaterialTheme.typography.labelSmall)
                }
            }

            Text("Kategori:", style = MaterialTheme.typography.labelSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("apikey" to "🔑 API Key", "free" to "🆓 Free", "freeTier" to "⚡ Free Tier", "oauth" to "🔐 OAuth").forEach { (cat, label) ->
                    KaiChip(selected = (categoryDraft == cat), onClick = { categoryDraft = cat }) {
                        Text(label, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = {
                        val pid = idDraft.trim()
                        val alias = if (aliasDraft.isNotBlank()) aliasDraft.trim() else pid
                        if (pid.isNotBlank() && baseUrlDraft.isNotBlank()) {
                            val newMeta = NineProviderMeta(
                                id = pid,
                                alias = alias,
                                aliases = listOf(alias),
                                baseUrl = baseUrlDraft.trim(),
                                validateUrl = "",
                                category = categoryDraft,
                                needsAccountId = false,
                                format = formatDraft,
                                displayName = if (nameDraft.isNotBlank()) nameDraft.trim() else pid,
                                isCustom = true,
                            )
                            onSave(newMeta)
                        }
                    },
                    enabled = idDraft.isNotBlank() && baseUrlDraft.isNotBlank(),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Text("Simpan Provider")
                }
                OutlinedButton(onClick = onCancel, shape = RoundedCornerShape(10.dp)) {
                    Text("Batal")
                }
            }
        }
    }
}

@Composable
private fun ProviderCardItem(
    meta: NineProviderMeta,
    connectionCount: Int,
    activeCount: Int,
    onClick: () -> Unit,
) {
    val isConnected = connectionCount > 0
    val cardColor = if (isConnected) {
        MaterialTheme.colorScheme.surface
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }
    val borderColor = if (isConnected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .handCursor(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        border = BorderStroke(1.dp, borderColor),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Category avatar with distinct color branding
                val (avatarBg, avatarText) = when (meta.category) {
                    "free" -> Color(0xFF059669).copy(alpha = 0.15f) to Color(0xFF059669)
                    "freeTier" -> Color(0xFF2563EB).copy(alpha = 0.15f) to Color(0xFF2563EB)
                    "oauth" -> Color(0xFF7C3AED).copy(alpha = 0.15f) to Color(0xFF7C3AED)
                    else -> Color(0xFFD97706).copy(alpha = 0.15f) to Color(0xFFD97706)
                }

                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(avatarBg),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = meta.alias.take(2).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = avatarText,
                        fontSize = 15.sp,
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = meta.getDisplayName(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        ) {
                            Text(
                                text = "${meta.alias}/",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (meta.isCustom) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            ) {
                                Text(
                                    text = "Kustom",
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val catLabel = when (meta.category) {
                            "free" -> "🆓 Free"
                            "freeTier" -> "⚡ Free Tier"
                            "oauth" -> "🔐 OAuth"
                            else -> "🔑 API Key"
                        }
                        Text(
                            text = catLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = avatarText,
                            fontWeight = FontWeight.Medium,
                        )

                        Text("•", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        if (isConnected) {
                            Text(
                                text = "🟢 $activeCount Akun Aktif",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF059669),
                                fontWeight = FontWeight.SemiBold,
                            )
                        } else {
                            Text(
                                text = "Belum Terhubung",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            )
                        }
                    }
                }
            }

            // Right Chevron
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
                modifier = Modifier.size(28.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("➔", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun ProviderDetailSheetContent(
    meta: NineProviderMeta,
    config: NineRouterConfig,
    pingResults: Map<String, NineValidationResult>,
    isPingingMap: Map<String, Boolean>,
    onAddConnection: (NineConnection) -> Unit,
    onToggleConnection: (String, Boolean) -> Unit,
    onDeleteConnection: (String) -> Unit,
    onPingConnection: (NineConnection) -> Unit,
    onBulkImportForProvider: (String) -> Int,
    onDeleteProvider: (String) -> Unit,
    onAddModel: (NineCustomModel) -> Unit,
    onDeleteModel: (String) -> Unit,
    onRestoreModelsForProvider: () -> Unit,
) {
    var accountNameDraft by remember { mutableStateOf("") }
    var apiKeyDraft by remember { mutableStateOf("") }
    var accountIdDraft by remember { mutableStateOf("") }
    var relayDraft by remember { mutableStateOf("") }
    var accessTokenDraft by remember { mutableStateOf("") }
    var refreshTokenDraft by remember { mutableStateOf("") }
    var bulkText by remember { mutableStateOf("") }
    var importMessage by remember { mutableStateOf<String?>(null) }
    var detailTab by remember { mutableStateOf(0) } // 0 = Akun, 1 = Tambah, 2 = Bulk, 3 = Models

    val connections = config.connections.filter { it.provider.equals(meta.id, ignoreCase = true) }
    val isOAuth = meta.category.equals("oauth", ignoreCase = true)
    val isNoAuth = meta.id == "opencode"

    // Models for this provider
    val defaultModels = NineRouterRegistry.getDefaultModelsForProvider(meta.id)
    val customModelsForProv = config.customModels.filter { it.provider.equals(meta.id, ignoreCase = true) }
    val activeModelItems = (defaultModels.map { (id, name) -> id to name } +
        customModelsForProv.map { it.id to (if (it.name.isNotBlank()) it.name else "${meta.alias} kustom") })
        .filterNot { it.first in config.deletedModelIds }
        .distinctBy { it.first }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = meta.getDisplayName(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    if (meta.isCustom) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        ) {
                            Text(
                                text = "Kustom",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
                Text(
                    text = "Prefix Model: ${meta.alias}/ • Kategori: ${meta.category}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Subtabs: 0 = Akun, 1 = Tambah Akun, 2 = Bulk Import, 3 = Models
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            KaiChip(selected = (detailTab == 0), onClick = { detailTab = 0 }) {
                Text("Akun (${connections.size})", style = MaterialTheme.typography.labelSmall)
            }
            KaiChip(selected = (detailTab == 1), onClick = { detailTab = 1 }) {
                Text("+ Akun", style = MaterialTheme.typography.labelSmall)
            }
            KaiChip(selected = (detailTab == 2), onClick = { detailTab = 2 }) {
                Text("📋 Import", style = MaterialTheme.typography.labelSmall)
            }
            KaiChip(selected = (detailTab == 3), onClick = { detailTab = 3 }) {
                Text("🤖 Models (${activeModelItems.size})", style = MaterialTheme.typography.labelSmall)
            }
        }

        when (detailTab) {
            0 -> {
                if (connections.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Belum ada akun tersimpan untuk provider ini. Pilih '+ Akun' atau 'Import'.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        connections.forEach { conn ->
                            val pingRes = pingResults[conn.id]
                            val isPinging = isPingingMap[conn.id] == true

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f),
                                        ) {
                                            val dotColor = if (conn.enabled) Color(0xFF10B981) else Color.Gray
                                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(dotColor))
                                            Text(
                                                text = if (conn.name.isNotBlank()) conn.name else "Akun ${conn.id.takeLast(4)}",
                                                fontWeight = FontWeight.SemiBold,
                                                style = MaterialTheme.typography.bodyMedium,
                                            )
                                        }

                                        // Toggle active
                                        Switch(
                                            checked = conn.enabled,
                                            onCheckedChange = { onToggleConnection(conn.id, it) },
                                        )
                                    }

                                    // Credentials pill
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        val masked = if (conn.apiKey.length > 8) conn.apiKey.take(6) + "…" + conn.apiKey.takeLast(4) else if (conn.accessToken.isNotBlank()) "Token ${conn.accessToken.take(6)}…" else "(no key)"
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                        ) {
                                            Text(
                                                text = masked,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                        if (conn.accountId.isNotBlank()) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                            ) {
                                                Text(
                                                    text = "Acc: ${conn.accountId.take(6)}…",
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                        }
                                        if (conn.relayUrl.isNotBlank()) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFF3B82F6).copy(alpha = 0.15f),
                                            ) {
                                                Text(
                                                    text = "Relay Aktif",
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color(0xFF3B82F6),
                                                )
                                            }
                                        }
                                    }

                                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                                    // Action buttons: Test Ping & Delete
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        if (isPinging) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                                Text("Menguji koneksi...", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                            }
                                        } else if (pingRes != null) {
                                            val statusColor = if (pingRes.success) Color(0xFF10B981) else MaterialTheme.colorScheme.error
                                            Text(
                                                text = pingRes.message,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = statusColor,
                                                fontWeight = FontWeight.SemiBold,
                                            )
                                        } else {
                                            Text(
                                                text = if (conn.enabled) "● Siap di Pool" else "○ Dinonaktifkan",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (conn.enabled) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Button(
                                                onClick = { onPingConnection(conn) },
                                                enabled = !isPinging,
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                            ) {
                                                Text("⚡ Test Ping", style = MaterialTheme.typography.labelSmall)
                                            }
                                            OutlinedButton(
                                                onClick = { onDeleteConnection(conn.id) },
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                            ) {
                                                Text("Hapus", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            1 -> {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (isOAuth) {
                        Text(
                            "Provider OAuth: login di browser perangkat lain lalu paste Access Token di bawah.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    OutlinedTextField(
                        value = accountNameDraft,
                        onValueChange = { accountNameDraft = it },
                        label = { Text("Label / Nama Akun (opsional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                    )
                    OutlinedTextField(
                        value = apiKeyDraft,
                        onValueChange = { apiKeyDraft = it },
                        label = { Text(if (isNoAuth) "API Key (opsional — kosongkan untuk no-auth)" else "API Key / Token") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                    )
                    if (isOAuth) {
                        OutlinedTextField(
                            value = accessTokenDraft,
                            onValueChange = { accessTokenDraft = it },
                            label = { Text("Access Token") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                        )
                        OutlinedTextField(
                            value = refreshTokenDraft,
                            onValueChange = { refreshTokenDraft = it },
                            label = { Text("Refresh Token (opsional)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                        )
                    }
                    if (meta.needsAccountId) {
                        OutlinedTextField(
                            value = accountIdDraft,
                            onValueChange = { accountIdDraft = it },
                            label = { Text("Account ID (Cloudflare 32-char)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                        )
                    }
                    OutlinedTextField(
                        value = relayDraft,
                        onValueChange = { relayDraft = it },
                        label = { Text("Relay URL (opsional — bypass limit IP)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                    )

                    Button(
                        onClick = {
                            val connId = "conn_" + Clock.System.now().toEpochMilliseconds()
                            onAddConnection(
                                NineConnection(
                                    id = connId,
                                    provider = meta.id,
                                    name = accountNameDraft.trim(),
                                    apiKey = apiKeyDraft.trim(),
                                    accountId = accountIdDraft.trim(),
                                    relayUrl = relayDraft.trim(),
                                    accessToken = accessTokenDraft.trim(),
                                    refreshToken = refreshTokenDraft.trim(),
                                )
                            )
                            accountNameDraft = ""
                            apiKeyDraft = ""
                            accountIdDraft = ""
                            relayDraft = ""
                            accessTokenDraft = ""
                            refreshTokenDraft = ""
                            detailTab = 0
                        },
                        enabled = isNoAuth || apiKeyDraft.isNotBlank() || accessTokenDraft.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Text("Simpan Akun ke Pool")
                    }
                }
            }

            2 -> {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        if (meta.needsAccountId) "Format: name|accountId|apiKey (satu akun per baris)" else "Format: name|apiKey (satu akun per baris)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = bulkText,
                        onValueChange = { bulkText = it },
                        label = { Text("Daftar Akun") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4,
                        maxLines = 8,
                        shape = RoundedCornerShape(10.dp),
                    )
                    Button(
                        onClick = {
                            if (bulkText.isNotBlank()) {
                                val c = onBulkImportForProvider(bulkText)
                                importMessage = "Berhasil mengimpor $c akun ke ${meta.id}!"
                                bulkText = ""
                            }
                        },
                        enabled = bulkText.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Text("Import Akun")
                    }
                    importMessage?.let { msg ->
                        Text(msg, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            3 -> {
                // Models Tab (1:1 ModelsCard in 9Router)
                var newModelIdDraft by remember { mutableStateOf("") }
                var newModelNameDraft by remember { mutableStateOf("") }
                var modelNotice by remember { mutableStateOf<String?>(null) }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Model aktif untuk ${meta.getDisplayName()}:",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        val deletedForProv = config.deletedModelIds.filter { dId ->
                            defaultModels.any { it.first == dId }
                        }
                        if (deletedForProv.isNotEmpty()) {
                            TextButton(
                                onClick = onRestoreModelsForProvider,
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            ) {
                                Text("Pulihkan Bawaan (${deletedForProv.size})", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    if (activeModelItems.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "Belum ada model aktif. Tambahkan model di bawah.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            activeModelItems.forEach { (mId, mName) ->
                                val isCustom = customModelsForProv.any { it.id == mId }
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    ),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Text(
                                                    text = mName,
                                                    fontWeight = FontWeight.SemiBold,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                )
                                                if (isCustom) {
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                                    ) {
                                                        Text(
                                                            text = "Kustom",
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.primary,
                                                        )
                                                    }
                                                }
                                            }
                                            Text(
                                                text = mId,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }

                                        OutlinedButton(
                                            onClick = { onDeleteModel(mId) },
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                        ) {
                                            Text("Hapus", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Form Add Model
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = kaiAdaptiveCardColors(),
                        border = kaiAdaptiveCardBorder(),
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Tambah Model Baru",
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.titleSmall,
                            )
                            OutlinedTextField(
                                value = newModelIdDraft,
                                onValueChange = { newModelIdDraft = it.trim() },
                                label = { Text("Model ID (mis: deepseek-reasoner, gpt-4.5-preview)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                            )
                            OutlinedTextField(
                                value = newModelNameDraft,
                                onValueChange = { newModelNameDraft = it },
                                label = { Text("Nama Tampilan Model (opsional)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                            )
                            Button(
                                onClick = {
                                    val mId = newModelIdDraft.trim()
                                    if (mId.isNotBlank()) {
                                        val fullId = if (!mId.contains("/") && !mId.startsWith("@")) {
                                            "${meta.alias}/$mId"
                                        } else {
                                            mId
                                        }
                                        onAddModel(NineCustomModel(id = fullId, provider = meta.id, name = newModelNameDraft.trim()))
                                        modelNotice = "✓ Model $fullId berhasil ditambahkan!"
                                        newModelIdDraft = ""
                                        newModelNameDraft = ""
                                    }
                                },
                                enabled = newModelIdDraft.isNotBlank(),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                            ) {
                                Text("+ Simpan Model ke ${meta.alias}/")
                            }

                            modelNotice?.let {
                                Text(it, color = Color(0xFF10B981), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }

        // Delete Provider Action at bottom
        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (meta.isCustom) "Provider Kustom" else "Provider Bawaan",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(
                onClick = { onDeleteProvider(meta.id) },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(if (meta.isCustom) "🗑️ Hapus Provider" else "🗑️ Sembunyikan Provider", style = MaterialTheme.typography.labelSmall)
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun CombosSubTab(
    config: NineRouterConfig,
    onSaveCombo: (String, List<String>) -> Unit,
    onDeleteCombo: (String) -> Unit,
) {
    var nameDraft by remember { mutableStateOf("") }
    var modelsDraft by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Rantai Fallback Multi-Model: Otomatis berpindah model saat kuota atau limit tercapai dalam satu sesi chat.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // Presets quick add
        Text("Rekomendasi Preset:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            val presets = listOf(
                "Hermini" to "cf/@cf/qwen/qwen3.8-27b, deepseek/deepseek-chat, openai/gpt-4o-mini",
                "Vision Stack" to "gemini/gemini-2.0-flash, cf/@cf/meta/llama-3.2-11b-vision-instruct",
                "Fast & Free" to "oc/auto, groq/llama-3.3-70b-versatile, openrouter/auto",
            )
            presets.forEach { (pName, pModels) ->
                KaiChip(selected = false, onClick = {
                    nameDraft = pName
                    modelsDraft = pModels
                }) {
                    Text("+ $pName", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        // Active combos list
        if (config.combos.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                config.combos.forEach { combo ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = kaiAdaptiveCardColors(),
                        border = kaiAdaptiveCardBorder(),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(combo.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                                Text(
                                    combo.models.joinToString(" ➔ "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            OutlinedButton(
                                onClick = { onDeleteCombo(combo.id) },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            ) {
                                Text("Hapus", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }

        // Form add
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = kaiAdaptiveCardColors(),
            border = kaiAdaptiveCardBorder(),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Buat Combo Baru", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                OutlinedTextField(
                    value = nameDraft,
                    onValueChange = { nameDraft = it },
                    label = { Text("Nama Combo (mis: MyCodingStack)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                )
                OutlinedTextField(
                    value = modelsDraft,
                    onValueChange = { modelsDraft = it },
                    label = { Text("Model IDs (pisahkan koma atau baris)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 5,
                    shape = RoundedCornerShape(10.dp),
                )
                Button(
                    onClick = {
                        val models = modelsDraft.split(",", "\n").map { it.trim() }.filter { it.isNotBlank() }
                        if (nameDraft.isNotBlank() && models.isNotEmpty()) {
                            onSaveCombo(nameDraft.trim(), models)
                            nameDraft = ""
                            modelsDraft = ""
                        }
                    },
                    enabled = nameDraft.isNotBlank() && modelsDraft.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Text("Simpan Combo")
                }
            }
        }
    }
}

@Composable
private fun TokenSaverSubTab(
    config: NineRouterConfig,
    onUpdateConfig: (NineRouterConfig) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Pipeline Token Saver 9Router: Hemat token input dan output tanpa merusak substansi jawaban. Semua kompresor fail-open.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // RTK
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = kaiAdaptiveCardColors(),
            border = kaiAdaptiveCardBorder(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("🚀 RTK Token Saver", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Text("Kompresi lossless output tool (git diff, grep, log, ls). Menghemat 20-40% token input.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = config.rtkEnabled,
                    onCheckedChange = { onUpdateConfig(config.copy(rtkEnabled = it)) },
                )
            }
        }

        // Caveman
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = kaiAdaptiveCardColors(),
            border = kaiAdaptiveCardBorder(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("🪨 Caveman Mode", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Text("Jawaban super ringkas, tanpa basa-basi pengantar/penutup. Menghemat token output hingga 65%.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = config.cavemanEnabled,
                    onCheckedChange = { onUpdateConfig(config.copy(cavemanEnabled = it)) },
                )
            }
        }

        // Ponytail
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = kaiAdaptiveCardColors(),
            border = kaiAdaptiveCardBorder(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("🐴 Ponytail Mode (Lazy Senior Dev)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Text("Menulis kode seringkas mungkin (YAGNI): prioritaskan stdlib dan hapus kode tidak perlu.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = config.ponytailEnabled,
                    onCheckedChange = { onUpdateConfig(config.copy(ponytailEnabled = it)) },
                )
            }
        }
    }
}

@Composable
private fun BackupImportSubTab(
    appSettings: AppSettings,
    onConfigChanged: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val copyToClipboard = rememberCopyToClipboard()
    var rawText by remember { mutableStateOf("") }
    var replaceAll by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberFilePickerLauncher(
        type = FileKitType.File(extensions = listOf("json", "txt")),
    ) { file ->
        if (file != null) {
            scope.launch {
                try {
                    val bytes = file.readBytes()
                    val text = bytes.decodeToString()
                    val result = appSettings.importNineRouterConfig(text, replaceAll)
                    statusMessage = result.message
                    isError = !result.isSuccess
                    if (result.isSuccess) {
                        onConfigChanged()
                    }
                } catch (e: Exception) {
                    statusMessage = "Gagal membaca file: ${e.message}"
                    isError = true
                }
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Card 1: Import Backup
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = kaiAdaptiveCardColors(),
            border = kaiAdaptiveCardBorder(),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("📥 Import Config / Backup 9Router", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(
                    "Impor file backup database 9Router (9router-backup-*.json) atau format baris panen. Mendukung file JSON backup resmi 9Router, format pool akun, dan combo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Button(
                    onClick = { filePickerLauncher.launch() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Text("📂 Pilih File Backup (.json / .txt)")
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Checkbox(
                        checked = replaceAll,
                        onCheckedChange = { replaceAll = it },
                    )
                    Text(
                        text = "Timpa seluruh data yang ada (Replace All)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                Text(
                    "Atau paste teks / JSON langsung di bawah ini:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                OutlinedTextField(
                    value = rawText,
                    onValueChange = { rawText = it },
                    label = { Text("Paste JSON Backup 9Router atau baris akun di sini...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                    maxLines = 8,
                    shape = RoundedCornerShape(10.dp),
                )

                Button(
                    onClick = {
                        if (rawText.isNotBlank()) {
                            val result = appSettings.importNineRouterConfig(rawText, replaceAll)
                            statusMessage = result.message
                            isError = !result.isSuccess
                            if (result.isSuccess) {
                                onConfigChanged()
                                rawText = ""
                            }
                        }
                    },
                    enabled = rawText.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Text("Import dari Teks / JSON")
                }

                statusMessage?.let { msg ->
                    val color = if (isError) MaterialTheme.colorScheme.error else Color(0xFF10B981)
                    Text(
                        text = msg,
                        fontWeight = FontWeight.SemiBold,
                        color = color,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }

        // Card 2: Export Backup
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = kaiAdaptiveCardColors(),
            border = kaiAdaptiveCardBorder(),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("📤 Export Backup 9Router", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(
                    "Ekspor seluruh akun, combo, dan konfigurasi token saver ke format JSON resmi 9Router. File hasil ekspor kompatibel 100% dengan dashboard web 9Router.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                var exportNotice by remember { mutableStateOf<String?>(null) }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = {
                            scope.launch {
                                try {
                                    val jsonString = appSettings.exportNineRouterBackupJson()
                                    val stamp = Clock.System.now().toEpochMilliseconds()
                                    saveFileToDevice(
                                        jsonString.encodeToByteArray(),
                                        "9router-backup-$stamp",
                                        "json",
                                    )
                                    copyToClipboard(jsonString)
                                    exportNotice = "✓ File berhasil disimpan & disalin ke clipboard!"
                                } catch (e: Exception) {
                                    exportNotice = "Gagal menyimpan file: ${e.message}"
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Text("Simpan File Backup (.json)")
                    }
                    OutlinedButton(
                        onClick = {
                            val jsonString = appSettings.exportNineRouterBackupJson()
                            copyToClipboard(jsonString)
                            exportNotice = "✓ JSON berhasil disalin ke clipboard!"
                        },
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Text("Salin JSON")
                    }
                }

                exportNotice?.let { notice ->
                    Text(
                        text = notice,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF10B981),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}
