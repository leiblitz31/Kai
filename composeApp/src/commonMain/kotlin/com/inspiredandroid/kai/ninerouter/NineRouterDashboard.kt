package com.inspiredandroid.kai.ninerouter

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.material3.Checkbox
import com.inspiredandroid.kai.saveFileToDevice
import com.inspiredandroid.kai.ui.rememberCopyToClipboard
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.readBytes
import com.inspiredandroid.kai.ui.components.KaiChip
import com.inspiredandroid.kai.ui.handCursor
import com.inspiredandroid.kai.ui.kaiAdaptiveCardBorder
import com.inspiredandroid.kai.ui.kaiAdaptiveCardColors
import kotlin.time.Clock
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NineRouterDashboard(
    modifier: Modifier = Modifier,
    appSettings: AppSettings = org.koin.compose.koinInject(),
) {
    val coroutineScope = rememberCoroutineScope()
    var config by remember { mutableStateOf(appSettings.getNineRouterConfig()) }
    LaunchedEffect(Unit) { config = appSettings.getNineRouterConfig() }

    // Dashboard Sub-tabs: 0 = Providers, 1 = Combos, 2 = Token Saver, 3 = Bulk Import
    var activeSubTab by remember { mutableStateOf(0) }

    // Selected provider for detail bottom sheet
    var activeProviderDetail by remember { mutableStateOf<NineProviderMeta?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Ping status cache: connectionId -> NineValidationResult
    val pingResults = remember { mutableStateMapOf<String, NineValidationResult>() }
    val isPingingMap = remember { mutableStateMapOf<String, Boolean>() }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Top Dashboard Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = kaiAdaptiveCardColors(),
            border = kaiAdaptiveCardBorder(),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = "9Router Engine",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Universal AI Router • 142 Provider • Pool & Direct Upstream",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // Stats summary pills
                val connectedCount = config.connections.filter { it.enabled && it.apiKey.isNotBlank() || it.accessToken.isNotBlank() || it.provider == "opencode" }.map { it.provider }.distinct().size
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    KaiChip(selected = false) {
                        Text("👥 ${config.connections.size} Akun Terhubung", style = MaterialTheme.typography.labelSmall)
                    }
                    KaiChip(selected = false) {
                        Text("🏢 $connectedCount / 142 Provider Aktif", style = MaterialTheme.typography.labelSmall)
                    }
                    KaiChip(selected = false) {
                        Text("🔀 ${config.combos.size} Combo", style = MaterialTheme.typography.labelSmall)
                    }
                    KaiChip(selected = false) {
                        Text("⚡ RTK: ${if (config.rtkEnabled) "Aktif" else "Off"}", style = MaterialTheme.typography.labelSmall)
                    }
                }

                // Navigation Tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
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

        // Tab Content
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
                                name = "OpenCode Free (No-Auth)",
                                apiKey = "",
                            )
                            appSettings.addOrUpdateNineConnection(ocConn)
                            config = appSettings.getNineRouterConfig()
                        }
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

    // Provider Detail Sheet
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
            )
        }
    }
}

@Composable
private fun ProvidersSubTab(
    config: NineRouterConfig,
    onOpenProviderDetail: (NineProviderMeta) -> Unit,
    onQuickActivateOpenCode: () -> Unit,
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var onlyConnected by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // OpenCode Free Quick Banner (if not yet added)
        val hasOpenCode = config.connections.any { it.provider == "opencode" && it.enabled }
        if (!hasOpenCode) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                ),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("🆓 OpenCode Free (Zero Auth)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Text("Akses gratis model AI tanpa perlu registrasi atau API key.", style = MaterialTheme.typography.bodySmall)
                    }
                    Button(onClick = onQuickActivateOpenCode) {
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
        )

        // Category filter chips
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            val cats = listOf(
                null to "Semua (142)",
                "free" to "🆓 Free (6)",
                "freeTier" to "⚡ Free Tier (19)",
                "oauth" to "🔐 OAuth (20)",
                "apikey" to "🔑 API Key (95)",
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

        // Filter providers
        val filtered = NineRouterRegistry.all.filter { meta ->
            val matchQuery = searchQuery.isBlank() ||
                meta.id.contains(searchQuery, ignoreCase = true) ||
                meta.alias.contains(searchQuery, ignoreCase = true) ||
                meta.aliases.any { it.contains(searchQuery, ignoreCase = true) }
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

        // Provider cards list
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
private fun ProviderCardItem(
    meta: NineProviderMeta,
    connectionCount: Int,
    activeCount: Int,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.handCursor(),
        colors = kaiAdaptiveCardColors(),
        border = kaiAdaptiveCardBorder(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Circle avatar with first 2 chars
                val avatarColor = when (meta.category) {
                    "free" -> Color(0xFF10B981)
                    "freeTier" -> Color(0xFF3B82F6)
                    "oauth" -> Color(0xFF8B5CF6)
                    else -> MaterialTheme.colorScheme.primary
                }
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(avatarColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = meta.alias.take(2).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = avatarColor,
                        fontSize = 14.sp,
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = meta.id,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "${meta.alias}/",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    // Category badge
                    val catBadge = when (meta.category) {
                        "free" -> "🆓 Free"
                        "freeTier" -> "⚡ Free Tier"
                        "oauth" -> "🔐 OAuth"
                        else -> "🔑 API Key"
                    }
                    Text(catBadge, style = MaterialTheme.typography.labelSmall, color = avatarColor)
                }
            }

            // Status badge
            if (connectionCount > 0) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF10B981).copy(alpha = 0.15f),
                ) {
                    Text(
                        text = "🟢 $activeCount Akun",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF10B981),
                        fontWeight = FontWeight.Medium,
                    )
                }
            } else {
                Text(
                    text = "Belum diisi",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
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
) {
    var accountNameDraft by remember { mutableStateOf("") }
    var apiKeyDraft by remember { mutableStateOf("") }
    var accountIdDraft by remember { mutableStateOf("") }
    var relayDraft by remember { mutableStateOf("") }
    var accessTokenDraft by remember { mutableStateOf("") }
    var refreshTokenDraft by remember { mutableStateOf("") }
    var bulkText by remember { mutableStateOf("") }
    var importMessage by remember { mutableStateOf<String?>(null) }
    var detailTab by remember { mutableStateOf(0) } // 0 = Akun, 1 = Tambah, 2 = Bulk

    val connections = config.connections.filter { it.provider.equals(meta.id, ignoreCase = true) }
    val isOAuth = meta.category.equals("oauth", ignoreCase = true)
    val isNoAuth = meta.id == "opencode"

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = meta.id,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Prefix: ${meta.alias}/ • Kategori: ${meta.category}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Subtabs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            KaiChip(selected = (detailTab == 0), onClick = { detailTab = 0 }) {
                Text("Akun (${connections.size})", style = MaterialTheme.typography.labelSmall)
            }
            KaiChip(selected = (detailTab == 1), onClick = { detailTab = 1 }) {
                Text("+ Tambah", style = MaterialTheme.typography.labelSmall)
            }
            KaiChip(selected = (detailTab == 2), onClick = { detailTab = 2 }) {
                Text("📋 Bulk Import", style = MaterialTheme.typography.labelSmall)
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
                            text = "Belum ada akun tersimpan untuk provider ini. Pilih '+ Tambah' atau 'Bulk Import'.",
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
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                ),
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (conn.name.isNotBlank()) conn.name else "Akun ${conn.id.takeLast(4)}",
                                                fontWeight = FontWeight.SemiBold,
                                                style = MaterialTheme.typography.bodyMedium,
                                            )
                                            val masked = if (conn.apiKey.length > 8) conn.apiKey.take(6) + "…" + conn.apiKey.takeLast(4) else "••••••••"
                                            val accInfo = if (conn.accountId.isNotBlank()) " (Acc: ${conn.accountId.take(6)}…)" else ""
                                            val relayInfo = if (conn.relayUrl.isNotBlank()) " • Relay aktif" else ""
                                            Text(
                                                text = "$masked$accInfo$relayInfo",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }

                                        // Toggle active
                                        Switch(
                                            checked = conn.enabled,
                                            onCheckedChange = { onToggleConnection(conn.id, it) },
                                        )
                                    }

                                    // Action buttons: Test Ping & Delete
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        // Status badge
                                        if (isPinging) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                                Text("Menguji...", style = MaterialTheme.typography.labelSmall)
                                            }
                                        } else if (pingRes != null) {
                                            val statusColor = if (pingRes.success) Color(0xFF10B981) else MaterialTheme.colorScheme.error
                                            Text(
                                                text = pingRes.message,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = statusColor,
                                                fontWeight = FontWeight.Medium,
                                            )
                                        } else {
                                            Text(
                                                text = if (conn.enabled) "Aktif di Pool" else "Dinonaktifkan",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            OutlinedButton(
                                                onClick = { onPingConnection(conn) },
                                                enabled = !isPinging,
                                            ) {
                                                Text("⚡ Test", style = MaterialTheme.typography.labelSmall)
                                            }
                                            OutlinedButton(onClick = { onDeleteConnection(conn.id) }) {
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
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    )
                    OutlinedTextField(
                        value = apiKeyDraft,
                        onValueChange = { apiKeyDraft = it },
                        label = { Text(if (isNoAuth) "API Key (opsional — kosongkan untuk no-auth)" else "API Key / Token") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    if (isOAuth) {
                        OutlinedTextField(
                            value = accessTokenDraft,
                            onValueChange = { accessTokenDraft = it },
                            label = { Text("Access Token") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = refreshTokenDraft,
                            onValueChange = { refreshTokenDraft = it },
                            label = { Text("Refresh Token (opsional)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                    }
                    if (meta.needsAccountId) {
                        OutlinedTextField(
                            value = accountIdDraft,
                            onValueChange = { accountIdDraft = it },
                            label = { Text("Account ID (Cloudflare 32-char)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                    }
                    OutlinedTextField(
                        value = relayDraft,
                        onValueChange = { relayDraft = it },
                        label = { Text("Relay URL (opsional — bypass limit IP)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
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
                    ) {
                        Text("Simpan Akun")
                    }
                }
            }

            2 -> {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    ) {
                        Text("Import Akun")
                    }
                    importMessage?.let { msg ->
                        Text(msg, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
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

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            "Rantai Fallback Multi-Model: Otomatis berpindah model saat kuota atau limit tercapai.",
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
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                config.combos.forEach { combo ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = kaiAdaptiveCardColors(),
                        border = kaiAdaptiveCardBorder(),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
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
                            OutlinedButton(onClick = { onDeleteCombo(combo.id) }) {
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
            colors = kaiAdaptiveCardColors(),
            border = kaiAdaptiveCardBorder(),
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Buat Combo Baru", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = nameDraft,
                    onValueChange = { nameDraft = it },
                    label = { Text("Nama Combo (mis: MyCodingStack)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = modelsDraft,
                    onValueChange = { modelsDraft = it },
                    label = { Text("Model IDs (pisahkan koma/baris)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 5,
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
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            "Pipeline Token Saver 9Router: Hemat token input dan output tanpa merusak substansi jawaban. Semua kompresor fail-open.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // RTK
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = kaiAdaptiveCardColors(),
            border = kaiAdaptiveCardBorder(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
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
            colors = kaiAdaptiveCardColors(),
            border = kaiAdaptiveCardBorder(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
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
            colors = kaiAdaptiveCardColors(),
            border = kaiAdaptiveCardBorder(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
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
            colors = kaiAdaptiveCardColors(),
            border = kaiAdaptiveCardBorder(),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("📥 Import Config / Backup 9Router", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(
                    "Impor file backup database 9Router (9router-backup-*.json) atau format baris panen. Mendukung file JSON backup resmi 9Router, format pool akun, dan combo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(
                        onClick = { filePickerLauncher.launch() },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("📂 Pilih File Backup (.json / .txt)")
                    }
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
                    ) {
                        Text("Simpan File Backup (.json)")
                    }
                    OutlinedButton(
                        onClick = {
                            val jsonString = appSettings.exportNineRouterBackupJson()
                            copyToClipboard(jsonString)
                            exportNotice = "✓ JSON berhasil disalin ke clipboard!"
                        },
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
