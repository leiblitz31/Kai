@file:OptIn(ExperimentalMaterial3Api::class)

package com.inspiredandroid.kai.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.inspiredandroid.kai.data.Service
import com.inspiredandroid.kai.formatFileSize
import com.inspiredandroid.kai.inference.DevicePerformance
import com.inspiredandroid.kai.inference.DownloadError
import com.inspiredandroid.kai.inference.LocalModel
import com.inspiredandroid.kai.inference.ModelImportError
import com.inspiredandroid.kai.inference.calculateDevicePerformance
import com.inspiredandroid.kai.inference.estimateGpuMemoryMb
import com.inspiredandroid.kai.network.dtos.SponsorsResponseDto
import com.inspiredandroid.kai.ui.KaiClearableTextField
import com.inspiredandroid.kai.ui.components.KaiSlider
import com.inspiredandroid.kai.ui.components.VerticalScrollbarForScroll
import com.inspiredandroid.kai.ui.handCursor
import com.inspiredandroid.kai.ui.icons.DragIndicator
import com.inspiredandroid.kai.ui.kaiAdaptiveCardBorder
import com.inspiredandroid.kai.ui.kaiAdaptiveCardColors
import com.inspiredandroid.kai.ui.kaiAdaptiveCardSurface
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import kai.composeapp.generated.resources.Res
import kai.composeapp.generated.resources.ic_arrow_drop_down
import kai.composeapp.generated.resources.litert_cancel
import kai.composeapp.generated.resources.litert_context_size
import kai.composeapp.generated.resources.litert_download
import kai.composeapp.generated.resources.litert_error_checksum_mismatch
import kai.composeapp.generated.resources.litert_error_download_incomplete
import kai.composeapp.generated.resources.litert_error_import_failed
import kai.composeapp.generated.resources.litert_error_import_invalid
import kai.composeapp.generated.resources.litert_error_import_too_small
import kai.composeapp.generated.resources.litert_error_network
import kai.composeapp.generated.resources.litert_error_not_enough_disk_space
import kai.composeapp.generated.resources.litert_free_space
import kai.composeapp.generated.resources.litert_import
import kai.composeapp.generated.resources.litert_import_description
import kai.composeapp.generated.resources.litert_imported
import kai.composeapp.generated.resources.litert_importing
import kai.composeapp.generated.resources.litert_on_device_description
import kai.composeapp.generated.resources.litert_performance_good
import kai.composeapp.generated.resources.litert_performance_ok
import kai.composeapp.generated.resources.litert_performance_poor
import kai.composeapp.generated.resources.litert_recommended
import kai.composeapp.generated.resources.litert_tool_support
import kai.composeapp.generated.resources.settings_add_service
import kai.composeapp.generated.resources.settings_api_key_label
import kai.composeapp.generated.resources.settings_api_key_optional_label
import kai.composeapp.generated.resources.settings_base_url_label
import kai.composeapp.generated.resources.settings_become_sponsor
import kai.composeapp.generated.resources.settings_business_partnerships
import kai.composeapp.generated.resources.settings_business_partnerships_description
import kai.composeapp.generated.resources.settings_contact_sponsorship
import kai.composeapp.generated.resources.settings_custom_model_hint
import kai.composeapp.generated.resources.settings_custom_model_label
import kai.composeapp.generated.resources.settings_free_fallback
import kai.composeapp.generated.resources.settings_free_tier_description
import kai.composeapp.generated.resources.settings_free_tier_title
import kai.composeapp.generated.resources.settings_model_label
import kai.composeapp.generated.resources.settings_open_app_settings
import kai.composeapp.generated.resources.settings_openai_compatible_or_other_service
import kai.composeapp.generated.resources.settings_openai_compatible_providers
import kai.composeapp.generated.resources.settings_openai_compatible_setup_ollama
import kai.composeapp.generated.resources.settings_remove_service
import kai.composeapp.generated.resources.settings_reorder_content_description
import kai.composeapp.generated.resources.settings_sign_in_copy_api_key_from
import kai.composeapp.generated.resources.settings_sponsors
import kai.composeapp.generated.resources.settings_status_checking
import kai.composeapp.generated.resources.settings_status_connected
import kai.composeapp.generated.resources.settings_status_error
import kai.composeapp.generated.resources.settings_status_error_connection_failed
import kai.composeapp.generated.resources.settings_status_error_invalid_key
import kai.composeapp.generated.resources.settings_status_error_local_network
import kai.composeapp.generated.resources.settings_status_error_quota_exhausted
import kai.composeapp.generated.resources.settings_status_error_rate_limited
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import sh.calvin.reorderable.ReorderableColumn
import com.inspiredandroid.kai.data.AppSettings
import com.inspiredandroid.kai.ninerouter.NineRouterRegistry
import com.inspiredandroid.kai.ninerouter.NineConnection
import com.inspiredandroid.kai.ninerouter.getNineRouterConfig
import com.inspiredandroid.kai.ninerouter.setNineRouterConfig
import com.inspiredandroid.kai.ninerouter.setNineCombos
import com.inspiredandroid.kai.ninerouter.addOrUpdateNineConnection
import com.inspiredandroid.kai.ninerouter.removeNineConnection
import com.inspiredandroid.kai.ninerouter.importNineConnectionsBulk
import com.inspiredandroid.kai.ui.components.KaiChip
import kotlin.time.Clock
import org.koin.compose.koinInject
import kotlin.math.roundToInt

@Composable
internal fun FreeSettings(
    showFallbackToggle: Boolean = false,
    isFreeFallbackEnabled: Boolean = true,
    onToggleFreeFallback: (Boolean) -> Unit = {},
    currentSponsors: ImmutableList<SponsorsResponseDto.Sponsor> = persistentListOf(),
    pastSponsors: ImmutableList<SponsorsResponseDto.Sponsor> = persistentListOf(),
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = kaiAdaptiveCardColors(),
        border = kaiAdaptiveCardBorder(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(Res.string.settings_free_tier_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )

            if (showFallbackToggle) {
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onToggleFreeFallback(!isFreeFallbackEnabled) }
                        .handCursor(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(Res.string.settings_free_fallback),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = isFreeFallbackEnabled,
                        onCheckedChange = onToggleFreeFallback,
                    )
                }
                Spacer(Modifier.height(6.dp))
            }

            Spacer(Modifier.height(6.dp))

            Text(
                text = stringResource(Res.string.settings_free_tier_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(12.dp))

            val uriHandler = LocalUriHandler.current
            Button(
                onClick = {
                    uriHandler.openUri("https://github.com/sponsors/SimonSchubert")
                },
                Modifier
                    .align(CenterHorizontally)
                    .handCursor(),
            ) {
                Icon(Icons.Default.Favorite, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(Res.string.settings_become_sponsor))
            }

            val allSponsors = remember(currentSponsors, pastSponsors) {
                val activeUsernames = currentSponsors.map { it.username }.toSet()
                (currentSponsors + pastSponsors.filter { it.username !in activeUsernames })
                    .toImmutableList()
            }

            if (allSponsors.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(thickness = 0.5.dp)
                Spacer(Modifier.height(16.dp))
                SponsorList(
                    title = stringResource(Res.string.settings_sponsors),
                    sponsors = allSponsors,
                )
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(thickness = 0.5.dp)
            Spacer(Modifier.height(16.dp))

            Text(
                text = stringResource(Res.string.settings_business_partnerships),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(Res.string.settings_business_partnerships_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            TextButton(
                onClick = {
                    uriHandler.openUri("https://schubert-simon.de")
                },
                Modifier
                    .handCursor(),
            ) {
                Text(stringResource(Res.string.settings_contact_sponsorship))
            }
        }
    }
}

@Composable
private fun SponsorList(
    title: String,
    sponsors: ImmutableList<SponsorsResponseDto.Sponsor>,
) {
    val uriHandler = LocalUriHandler.current
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(8.dp))
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // The tile width is sized for the username at the default font scale; without
        // scaling it, names are cut to three characters at the largest one.
        val tileWidth = 72.dp * LocalDensity.current.fontScale
        sponsors.forEach { sponsor ->
            Column(
                horizontalAlignment = CenterHorizontally,
                modifier = Modifier
                    .width(tileWidth)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { uriHandler.openUri("https://github.com/${sponsor.username}") }
                    .handCursor()
                    .padding(4.dp),
            ) {
                coil3.compose.AsyncImage(
                    model = sponsor.avatar,
                    contentDescription = sponsor.username,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = sponsor.username,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
internal fun ServicesContent(uiState: SettingsUiState, actions: SettingsActions) {
    var showAddServiceSheet by remember { mutableStateOf(false) }
    val appSettings: AppSettings = koinInject()

    // 9Router Engine — Multi-account pool & direct connection
    val ninerouterConfig = androidx.compose.runtime.remember { appSettings.getNineRouterConfig() }
    var ninerouterConfigState by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(ninerouterConfig) }
    androidx.compose.runtime.LaunchedEffect(Unit) { ninerouterConfigState = appSettings.getNineRouterConfig() }
    var selectedTab by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(0) }
    var selectedId by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
    var accountNameDraft by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    var apiKeyDraft by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    var accountIdDraft by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    var bulkImportText by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    var importStatusMsg by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
    var categoryFilter by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
    var relayDraft by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    var accessTokenDraft by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    var refreshTokenDraft by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    var comboNameDraft by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    var comboModelsDraft by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }

    androidx.compose.foundation.layout.Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
        androidx.compose.material3.Card(
            modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
            colors = kaiAdaptiveCardColors(),
            border = kaiAdaptiveCardBorder(),
        ) {
            androidx.compose.foundation.layout.Column(
                modifier = androidx.compose.ui.Modifier.padding(16.dp),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp),
            ) {
                androidx.compose.foundation.layout.Row(
                    modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                ) {
                    androidx.compose.foundation.layout.Column {
                        androidx.compose.material3.Text("9Router Engine", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                        androidx.compose.material3.Text(
                            "Multi-akun pool & direct upstream untuk 142 provider",
                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                androidx.compose.foundation.layout.Row(
                    modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                ) {
                    KaiChip(selected = (selectedTab == 0), onClick = { selectedTab = 0 }) {
                        androidx.compose.material3.Text("Akun (${ninerouterConfigState.connections.size})", style = androidx.compose.material3.MaterialTheme.typography.labelMedium)
                    }
                    KaiChip(selected = (selectedTab == 1), onClick = { selectedTab = 1 }) {
                        androidx.compose.material3.Text("+ Tambah", style = androidx.compose.material3.MaterialTheme.typography.labelMedium)
                    }
                    KaiChip(selected = (selectedTab == 2), onClick = { selectedTab = 2 }) {
                        androidx.compose.material3.Text("📋 Bulk Import", style = androidx.compose.material3.MaterialTheme.typography.labelMedium)
                    }
                    KaiChip(selected = (selectedTab == 3), onClick = { selectedTab = 3 }) {
                        androidx.compose.material3.Text("🔀 Combo (${ninerouterConfigState.combos.size})", style = androidx.compose.material3.MaterialTheme.typography.labelMedium)
                    }
                    KaiChip(selected = (selectedTab == 4), onClick = { selectedTab = 4 }) {
                        androidx.compose.material3.Text("⚡ Hemat", style = androidx.compose.material3.MaterialTheme.typography.labelMedium)
                    }
                }

                when (selectedTab) {
                    0 -> {
                        if (ninerouterConfigState.connections.isEmpty()) {
                            androidx.compose.foundation.layout.Box(
                                modifier = androidx.compose.ui.Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                contentAlignment = androidx.compose.ui.Alignment.Center,
                            ) {
                                androidx.compose.material3.Text(
                                    "Belum ada akun tersimpan. Gunakan '+ Tambah' atau 'Bulk Import'.",
                                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        } else {
                            androidx.compose.foundation.layout.Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)) {
                                ninerouterConfigState.connections.forEach { conn ->
                                    val meta = NineRouterRegistry.find(conn.provider)
                                    val provDisplay = meta?.alias ?: conn.provider
                                    androidx.compose.material3.Card(
                                        modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                                        colors = androidx.compose.material3.CardDefaults.cardColors(
                                            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        ),
                                    ) {
                                        androidx.compose.foundation.layout.Row(
                                            modifier = androidx.compose.ui.Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                        ) {
                                            androidx.compose.foundation.layout.Column(modifier = androidx.compose.ui.Modifier.weight(1f)) {
                                                androidx.compose.foundation.layout.Row(
                                                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp),
                                                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                                ) {
                                                    androidx.compose.material3.Text(provDisplay, style = androidx.compose.material3.MaterialTheme.typography.labelMedium, color = androidx.compose.material3.MaterialTheme.colorScheme.primary)
                                                    if (conn.name.isNotBlank()) {
                                                        androidx.compose.material3.Text("• ${conn.name}", style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                                                    }
                                                }
                                                val maskedKey = if (conn.apiKey.length > 10) conn.apiKey.take(6) + "…" + conn.apiKey.takeLast(4) else "••••••••"
                                                val detail = if (conn.accountId.isNotBlank()) "$maskedKey (Acc: ${conn.accountId.take(6)}…)" else maskedKey
                                                androidx.compose.material3.Text(detail, style = androidx.compose.material3.MaterialTheme.typography.bodySmall, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            androidx.compose.material3.OutlinedButton(
                                                onClick = {
                                                    appSettings.removeNineConnection(conn.id)
                                                    ninerouterConfigState = appSettings.getNineRouterConfig()
                                                },
                                                modifier = androidx.compose.ui.Modifier.padding(start = 8.dp),
                                            ) {
                                                androidx.compose.material3.Text("Hapus", style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        val cats = listOf(
                            null to "Semua",
                            "free" to "🆓 Free",
                            "freeTier" to "⚡ Free Tier",
                            "oauth" to "🔐 OAuth",
                            "apikey" to "🔑 API Key",
                        )
                        androidx.compose.foundation.layout.FlowRow(
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp),
                            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp),
                        ) {
                            cats.forEach { (cat, label) ->
                                KaiChip(selected = (categoryFilter == cat), onClick = { categoryFilter = cat }) {
                                    androidx.compose.material3.Text(label, style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                        androidx.compose.material3.Text("Pilih provider:", style = androidx.compose.material3.MaterialTheme.typography.labelSmall, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
                        androidx.compose.foundation.layout.FlowRow(
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp),
                            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp),
                        ) {
                            val popular = NineRouterRegistry.all
                                .filter { p ->
                                    categoryFilter == null ||
                                        p.category.equals(categoryFilter, ignoreCase = true) ||
                                        (categoryFilter == "apikey" && p.category.equals("apikey", ignoreCase = true))
                                }
                                .sortedWith(compareBy({ it.category }, { it.id }))
                                .take(24)
                            popular.forEach { meta ->
                                val badge = when {
                                    meta.category.equals("free", ignoreCase = true) -> " 🆓"
                                    meta.category.equals("freeTier", ignoreCase = true) -> " ⚡"
                                    meta.category.equals("oauth", ignoreCase = true) -> " 🔐"
                                    else -> ""
                                }
                                KaiChip(selected = (selectedId == meta.id), onClick = { selectedId = meta.id }) {
                                    androidx.compose.material3.Text(meta.id + badge, style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                        androidx.compose.material3.OutlinedTextField(
                            value = selectedId ?: "",
                            onValueChange = { selectedId = it },
                            label = { androidx.compose.material3.Text("Provider ID (mis: cloudflare-ai, openai, deepseek)") },
                            modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                        val selMeta = selectedId?.let { NineRouterRegistry.find(it) }
                        val isOAuth = selMeta?.category.equals("oauth", ignoreCase = true)
                        val isNoAuth = selMeta?.id == "opencode"
                        if (isOAuth) {
                            androidx.compose.material3.Text(
                                "Provider OAuth: login via browser di perangkat lain, lalu paste token sesi di bawah (tanpa auto-refresh di HP).",
                                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        androidx.compose.material3.OutlinedTextField(
                            value = accountNameDraft,
                            onValueChange = { accountNameDraft = it },
                            label = { androidx.compose.material3.Text("Nama Akun / Label (opsional)") },
                            modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                        androidx.compose.material3.OutlinedTextField(
                            value = apiKeyDraft,
                            onValueChange = { apiKeyDraft = it },
                            label = { androidx.compose.material3.Text(if (isNoAuth) "API Key (opsional — kosongkan untuk tanpa auth)" else "API Key / Token") },
                            modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                        if (isOAuth) {
                            androidx.compose.material3.OutlinedTextField(
                                value = accessTokenDraft,
                                onValueChange = { accessTokenDraft = it },
                                label = { androidx.compose.material3.Text("Access Token (hasil login browser)") },
                                modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                                singleLine = true,
                            )
                            androidx.compose.material3.OutlinedTextField(
                                value = refreshTokenDraft,
                                onValueChange = { refreshTokenDraft = it },
                                label = { androidx.compose.material3.Text("Refresh Token (opsional)") },
                                modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                                singleLine = true,
                            )
                        }
                        val needAcc = selectedId?.let { NineRouterRegistry.find(it)?.needsAccountId } == true
                        if (needAcc) {
                            androidx.compose.material3.OutlinedTextField(
                                value = accountIdDraft,
                                onValueChange = { accountIdDraft = it },
                                label = { androidx.compose.material3.Text("Account ID (Cloudflare 32-char)") },
                                modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                                singleLine = true,
                            )
                        }
                        androidx.compose.material3.OutlinedTextField(
                            value = relayDraft,
                            onValueChange = { relayDraft = it },
                            label = { androidx.compose.material3.Text("Relay URL (opsional — bypass limit IP)") },
                            modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                        androidx.compose.foundation.layout.Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                            androidx.compose.material3.Button(
                                onClick = {
                                    val pid = selectedId?.trim().orEmpty()
                                    val hasKey = apiKeyDraft.isNotBlank() || accessTokenDraft.isNotBlank() || isNoAuth
                                    if (pid.isBlank() || !hasKey) return@Button
                                    val connId = "conn_" + Clock.System.now().toEpochMilliseconds()
                                    val newConn = NineConnection(
                                        id = connId,
                                        provider = pid,
                                        name = accountNameDraft.trim(),
                                        apiKey = apiKeyDraft.trim(),
                                        accountId = accountIdDraft.trim(),
                                        relayUrl = relayDraft.trim(),
                                        accessToken = accessTokenDraft.trim(),
                                        refreshToken = refreshTokenDraft.trim(),
                                    )
                                    appSettings.addOrUpdateNineConnection(newConn)
                                    ninerouterConfigState = appSettings.getNineRouterConfig()
                                    selectedId = null
                                    accountNameDraft = ""
                                    apiKeyDraft = ""
                                    accountIdDraft = ""
                                    relayDraft = ""
                                    accessTokenDraft = ""
                                    refreshTokenDraft = ""
                                    selectedTab = 0
                                },
                                enabled = (selectedId?.isNotBlank() == true && (apiKeyDraft.isNotBlank() || accessTokenDraft.isNotBlank() || isNoAuth)),
                            ) {
                                androidx.compose.material3.Text("Simpan Akun")
                            }
                            androidx.compose.material3.OutlinedButton(onClick = {
                                selectedId = null
                                accountNameDraft = ""
                                apiKeyDraft = ""
                                accountIdDraft = ""
                                relayDraft = ""
                                accessTokenDraft = ""
                                refreshTokenDraft = ""
                            }) {
                                androidx.compose.material3.Text("Reset")
                            }
                        }
                    }

                    2 -> {
                        androidx.compose.material3.Text(
                            "Paste daftar akun per baris. Format didukung:\n• provider|name|apiKey\n• provider|name|accountId|apiKey\n• name|email|apiUrl|apiKey",
                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        androidx.compose.material3.OutlinedTextField(
                            value = bulkImportText,
                            onValueChange = { bulkImportText = it },
                            label = { androidx.compose.material3.Text("Daftar Akun (banyak baris)") },
                            modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                            minLines = 4,
                            maxLines = 8,
                        )
                        androidx.compose.foundation.layout.Row(
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        ) {
                            androidx.compose.material3.Button(
                                onClick = {
                                    if (bulkImportText.isNotBlank()) {
                                        val count = appSettings.importNineConnectionsBulk(bulkImportText)
                                        ninerouterConfigState = appSettings.getNineRouterConfig()
                                        importStatusMsg = "Berhasil mengimpor $count akun!"
                                        bulkImportText = ""
                                    }
                                },
                                enabled = bulkImportText.isNotBlank(),
                            ) {
                                androidx.compose.material3.Text("Import Semua")
                            }
                            importStatusMsg?.let { msg ->
                                androidx.compose.material3.Text(msg, style = androidx.compose.material3.MaterialTheme.typography.bodySmall, color = androidx.compose.material3.MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    3 -> {
                        androidx.compose.material3.Text(
                            "Rantai fallback model: jika model pertama kena limit/kuota, otomatis lompat ke berikutnya dalam satu chat.",
                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (ninerouterConfigState.combos.isEmpty()) {
                            androidx.compose.material3.Text(
                                "Belum ada combo. Contoh: Hermini = cf/@cf/qwen/qwen3.8-27b, deepseek/deepseek-chat",
                                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            androidx.compose.foundation.layout.Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)) {
                                ninerouterConfigState.combos.forEach { combo ->
                                    androidx.compose.material3.Card(
                                        modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                                        colors = androidx.compose.material3.CardDefaults.cardColors(
                                            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        ),
                                    ) {
                                        androidx.compose.foundation.layout.Row(
                                            modifier = androidx.compose.ui.Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                        ) {
                                            androidx.compose.foundation.layout.Column(modifier = androidx.compose.ui.Modifier.weight(1f)) {
                                                androidx.compose.material3.Text(combo.name, style = androidx.compose.material3.MaterialTheme.typography.labelMedium, color = androidx.compose.material3.MaterialTheme.colorScheme.primary)
                                                androidx.compose.material3.Text(
                                                    combo.models.joinToString(" → "),
                                                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                                                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                            androidx.compose.material3.OutlinedButton(
                                                onClick = {
                                                    appSettings.setNineCombos(ninerouterConfigState.combos.filterNot { it.id == combo.id })
                                                    ninerouterConfigState = appSettings.getNineRouterConfig()
                                                },
                                                modifier = androidx.compose.ui.Modifier.padding(start = 8.dp),
                                            ) {
                                                androidx.compose.material3.Text("Hapus", style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        androidx.compose.material3.OutlinedTextField(
                            value = comboNameDraft,
                            onValueChange = { comboNameDraft = it },
                            label = { androidx.compose.material3.Text("Nama Combo (mis: Hermini)") },
                            modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                        androidx.compose.material3.OutlinedTextField(
                            value = comboModelsDraft,
                            onValueChange = { comboModelsDraft = it },
                            label = { androidx.compose.material3.Text("Model (pisahkan koma/baris)") },
                            modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 6,
                        )
                        androidx.compose.material3.Button(
                            onClick = {
                                val models = comboModelsDraft.split(",", "\n").map { it.trim() }.filter { it.isNotBlank() }
                                if (comboNameDraft.isBlank() || models.isEmpty()) return@Button
                                val newCombo = com.inspiredandroid.kai.ninerouter.NineCombo(
                                    id = "combo_" + Clock.System.now().toEpochMilliseconds(),
                                    name = comboNameDraft.trim(),
                                    models = models,
                                )
                                appSettings.setNineCombos(ninerouterConfigState.combos + newCombo)
                                ninerouterConfigState = appSettings.getNineRouterConfig()
                                comboNameDraft = ""
                                comboModelsDraft = ""
                            },
                            enabled = (comboNameDraft.isNotBlank() && comboModelsDraft.isNotBlank()),
                        ) {
                            androidx.compose.material3.Text("Simpan Combo")
                        }
                    }

                    4 -> {
                        androidx.compose.material3.Text(
                            "Pipeline hemat token (fail-open — gagal kompres = kirim asli, chat tidak pernah rusak).",
                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        androidx.compose.foundation.layout.Row(
                            modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        ) {
                            androidx.compose.foundation.layout.Column(modifier = androidx.compose.ui.Modifier.weight(1f)) {
                                androidx.compose.material3.Text("RTK — kompres output tool", style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
                                androidx.compose.material3.Text("Pangkas git diff/log/grep sebelum dikirim (hemat 20–40%).", style = androidx.compose.material3.MaterialTheme.typography.bodySmall, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            androidx.compose.material3.Switch(
                                checked = ninerouterConfigState.rtkEnabled,
                                onCheckedChange = {
                                    appSettings.setNineRouterConfig(ninerouterConfigState.copy(rtkEnabled = it))
                                    ninerouterConfigState = appSettings.getNineRouterConfig()
                                },
                            )
                        }
                        androidx.compose.foundation.layout.Row(
                            modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        ) {
                            androidx.compose.foundation.layout.Column(modifier = androidx.compose.ui.Modifier.weight(1f)) {
                                androidx.compose.material3.Text("Caveman — jawaban padat", style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
                                androidx.compose.material3.Text("Tanpa basa-basi, substansi tetap (hemat output).", style = androidx.compose.material3.MaterialTheme.typography.bodySmall, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            androidx.compose.material3.Switch(
                                checked = ninerouterConfigState.cavemanEnabled,
                                onCheckedChange = {
                                    appSettings.setNineRouterConfig(ninerouterConfigState.copy(cavemanEnabled = it))
                                    ninerouterConfigState = appSettings.getNineRouterConfig()
                                },
                            )
                        }
                        androidx.compose.foundation.layout.Row(
                            modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        ) {
                            androidx.compose.foundation.layout.Column(modifier = androidx.compose.ui.Modifier.weight(1f)) {
                                androidx.compose.material3.Text("Ponytail — kode minimal", style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
                                androidx.compose.material3.Text("Gaya senior dev malas: YAGNI, stdlib dulu.", style = androidx.compose.material3.MaterialTheme.typography.bodySmall, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            androidx.compose.material3.Switch(
                                checked = ninerouterConfigState.ponytailEnabled,
                                onCheckedChange = {
                                    appSettings.setNineRouterConfig(ninerouterConfigState.copy(ponytailEnabled = it))
                                    ninerouterConfigState = appSettings.getNineRouterConfig()
                                },
                            )
                        }
                    }
                }
            }
        }
        androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(12.dp))
    }

    // Configured services list
    val entries = uiState.configuredServices
    ReorderableColumn(
        list = entries,
        onSettle = { fromIndex, toIndex ->
            val ids = entries.map { it.instanceId }.toMutableList()
            ids.add(toIndex, ids.removeAt(fromIndex))
            actions.onReorderServices(ids)
        },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) { _, entry, isDragging ->
        key(entry.instanceId) {
            ReorderableItem {
                ConfiguredServiceCardContent(
                    entry = entry,
                    isExpanded = uiState.expandedServiceId == entry.instanceId,
                    onExpand = { actions.onExpandService(if (uiState.expandedServiceId == entry.instanceId) null else entry.instanceId) },
                    onChangeApiKey = { apiKey -> actions.onChangeApiKey(entry.instanceId, apiKey) },
                    onChangeBaseUrl = { baseUrl -> actions.onChangeBaseUrl(entry.instanceId, baseUrl) },
                    onSelectModel = { modelId -> actions.onSelectModel(entry.instanceId, modelId) },
                    onToggleUseCustomModel = { use -> actions.onToggleUseCustomModel(entry.instanceId, use) },
                    onChangeCustomModelId = { id -> actions.onChangeCustomModelId(entry.instanceId, id) },
                    onRemove = { actions.onRemoveService(entry.instanceId) },
                    isDragging = isDragging,
                    dragHandleModifier = if (entries.size >= 2) Modifier.draggableHandle() else null,
                    localAvailableModels = uiState.localAvailableModels,
                    localImportedModels = uiState.localImportedModels,
                    totalDeviceMemoryBytes = uiState.totalDeviceMemoryBytes,
                    localFreeSpaceBytes = uiState.localFreeSpaceBytes,
                    localDownloadingModelId = uiState.localDownloadingModelId,
                    localDownloadProgress = uiState.localDownloadProgress,
                    localDownloadError = uiState.localDownloadError,
                    localImportingFileName = uiState.localImportingFileName,
                    localImportProgress = uiState.localImportProgress,
                    localImportError = uiState.localImportError,
                    onDownloadLocalModel = actions.onDownloadLocalModel,
                    onCancelLocalModelDownload = actions.onCancelLocalModelDownload,
                    onImportLocalModel = actions.onImportLocalModel,
                    onCancelLocalModelImport = actions.onCancelLocalModelImport,
                    onDeleteLocalModel = actions.onDeleteLocalModel,
                    onChangeModelContextTokens = actions.onChangeModelContextTokens,
                    modelContextTokens = uiState.modelContextTokens,
                    onOpenAppPermissionSettings = actions.onOpenAppPermissionSettings,
                    onRecheckLocalNetworkPermission = { actions.onRecheckLocalNetworkPermission(entry.instanceId) },
                )
            }
        }
    }

    if (uiState.availableServicesToAdd.isNotEmpty()) {
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = { showAddServiceSheet = true }, modifier = Modifier.handCursor()) {
            Text(stringResource(Res.string.settings_add_service))
        }
    }

    // Free tier card (always at bottom)
    Spacer(Modifier.height(16.dp))
    FreeSettings(
        showFallbackToggle = entries.isNotEmpty(),
        isFreeFallbackEnabled = uiState.isFreeFallbackEnabled,
        onToggleFreeFallback = actions.onToggleFreeFallback,
        currentSponsors = uiState.currentSponsors,
        pastSponsors = uiState.pastSponsors,
    )

    // Add service bottom sheet
    if (showAddServiceSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddServiceSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            val addServiceScrollState = rememberScrollState()
            Box {
                Column(modifier = Modifier.verticalScroll(addServiceScrollState).padding(16.dp)) {
                    val services = uiState.availableServicesToAdd
                    services.forEachIndexed { index, service ->
                        val isFirst = index == 0
                        val isLast = index == services.lastIndex
                        val itemShape = RoundedCornerShape(
                            topStart = if (isFirst) 12.dp else 0.dp,
                            topEnd = if (isFirst) 12.dp else 0.dp,
                            bottomStart = if (isLast) 12.dp else 0.dp,
                            bottomEnd = if (isLast) 12.dp else 0.dp,
                        )
                        val isSpecial = service.isOnDevice || service is Service.OpenAICompatible || service is Service.NineRouter || service is Service.AtlasCloud
                        Surface(
                            onClick = {
                                actions.onAddService(service)
                                showAddServiceSheet = false
                            },
                            modifier = Modifier.fillMaxWidth().handCursor(),
                            shape = itemShape,
                            color = MaterialTheme.colorScheme.surfaceContainer,
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .then(
                                            if (isSpecial) {
                                                Modifier.background(
                                                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                                    shape = RoundedCornerShape(8.dp),
                                                )
                                            } else {
                                                Modifier
                                            },
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = vectorResource(service.icon),
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.onBackground,
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = service.displayName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onBackground,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
                VerticalScrollbarForScroll(
                    scrollState = addServiceScrollState,
                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                )
            }
        }
    }
}

@Composable
private fun ConfiguredServiceCardContent(
    entry: ConfiguredServiceEntry,
    isExpanded: Boolean,
    onExpand: () -> Unit,
    onChangeApiKey: (String) -> Unit,
    onChangeBaseUrl: (String) -> Unit,
    onSelectModel: (String) -> Unit,
    onToggleUseCustomModel: (Boolean) -> Unit = {},
    onChangeCustomModelId: (String) -> Unit = {},
    onRemove: () -> Unit,
    isDragging: Boolean = false,
    dragHandleModifier: Modifier? = null,
    localAvailableModels: ImmutableList<LocalModel> = persistentListOf(),
    localImportedModels: ImmutableList<LocalModel> = persistentListOf(),
    totalDeviceMemoryBytes: Long = Long.MAX_VALUE,
    localFreeSpaceBytes: Long = 0L,
    localDownloadingModelId: String? = null,
    localDownloadProgress: Float? = null,
    localDownloadError: DownloadError? = null,
    localImportingFileName: String? = null,
    localImportProgress: Float? = null,
    localImportError: ModelImportError? = null,
    onDownloadLocalModel: (LocalModel) -> Unit = {},
    onCancelLocalModelDownload: () -> Unit = {},
    onImportLocalModel: (PlatformFile) -> Unit = {},
    onCancelLocalModelImport: () -> Unit = {},
    onDeleteLocalModel: (String) -> Unit = {},
    onChangeModelContextTokens: (String, Int) -> Unit = { _, _ -> },
    modelContextTokens: ImmutableMap<String, Int> = persistentMapOf(),
    onOpenAppPermissionSettings: () -> Unit = {},
    onRecheckLocalNetworkPermission: () -> Unit = {},
) {
    // Clear a stale denied status when the user returns from granting the permission in
    // system settings; the recheck never re-prompts, so this is a no-op while still denied.
    if (entry.connectionStatus == ConnectionStatus.ErrorLocalNetworkDenied) {
        LifecycleResumeEffect(entry.instanceId) {
            onRecheckLocalNetworkPermission()
            onPauseOrDispose { }
        }
    }
    Column(
        modifier = Modifier
            .kaiAdaptiveCardSurface()
            .fillMaxWidth()
            .clickable { onExpand() }
            .handCursor(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Drag handle
                if (dragHandleModifier != null) {
                    Icon(
                        imageVector = Icons.Rounded.DragIndicator,
                        contentDescription = stringResource(Res.string.settings_reorder_content_description),
                        modifier = dragHandleModifier.handCursor(),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(8.dp))
                }

                // Connection status dot
                val dotColor = when (entry.connectionStatus) {
                    ConnectionStatus.Connected -> StatusColorConnected
                    ConnectionStatus.Checking -> StatusColorChecking
                    ConnectionStatus.Unknown -> StatusColorUnknown
                    else -> StatusColorError
                }
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(dotColor),
                )

                Spacer(Modifier.width(12.dp))

                // Service name and model
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.service.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    val displayModelId = when {
                        entry.useCustomModel && entry.customModelId.isNotBlank() -> entry.customModelId
                        entry.selectedModel != null -> entry.selectedModel.id
                        else -> null
                    }
                    if (displayModelId != null) {
                        Text(
                            text = displayModelId,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                // Expand/collapse chevron
                Icon(
                    imageVector = vectorResource(Res.drawable.ic_arrow_drop_down),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Expanded content
        if (isExpanded) {
            Column(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp)) {
                if (entry.service.isOnDevice) {
                    LiteRTSettings(
                        selectedModel = entry.selectedModel,
                        downloadedModels = entry.models,
                        availableModels = localAvailableModels,
                        importedModels = localImportedModels,
                        totalDeviceMemoryBytes = totalDeviceMemoryBytes,
                        freeSpaceBytes = localFreeSpaceBytes,
                        downloadingModelId = localDownloadingModelId,
                        downloadProgress = localDownloadProgress,
                        downloadError = localDownloadError,
                        importingFileName = localImportingFileName,
                        importProgress = localImportProgress,
                        importError = localImportError,
                        onSelectModel = onSelectModel,
                        onDownloadModel = onDownloadLocalModel,
                        onCancelDownload = onCancelLocalModelDownload,
                        onImportModel = onImportLocalModel,
                        onCancelImport = onCancelLocalModelImport,
                        onDeleteModel = onDeleteLocalModel,
                        onChangeModelContextTokens = onChangeModelContextTokens,
                        modelContextTokens = modelContextTokens,
                    )
                } else if (entry.service is Service.OpenAICompatible || entry.service is Service.NineRouter) {
                    OpenAICompatibleSettings(
                        baseUrl = entry.baseUrl,
                        onChangeBaseUrl = onChangeBaseUrl,
                        apiKey = entry.apiKey,
                        onChangeApiKey = onChangeApiKey,
                        selectedModel = entry.selectedModel,
                        models = entry.models,
                        onSelectModel = onSelectModel,
                        useCustomModel = entry.useCustomModel,
                        customModelId = entry.customModelId,
                        onToggleUseCustomModel = onToggleUseCustomModel,
                        onChangeCustomModelId = onChangeCustomModelId,
                        connectionStatus = entry.connectionStatus,
                        onOpenAppPermissionSettings = onOpenAppPermissionSettings,
                    )
                } else {
                    ServiceSettings(
                        apiKey = entry.apiKey,
                        onChangeApiKey = onChangeApiKey,
                        apiKeyUrl = entry.service.apiKeyUrl ?: "",
                        apiKeyUrlDisplay = entry.service.apiKeyUrlDisplay ?: "",
                        selectedModel = entry.selectedModel,
                        models = entry.models,
                        onSelectModel = onSelectModel,
                        connectionStatus = entry.connectionStatus,
                        onOpenAppPermissionSettings = onOpenAppPermissionSettings,
                    )
                }

                Spacer(Modifier.height(12.dp))

                // Remove action
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        onClick = onRemove,
                        modifier = Modifier.handCursor(),
                    ) {
                        Text(
                            text = stringResource(Res.string.settings_remove_service),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ServiceSettings(
    apiKey: String,
    onChangeApiKey: (String) -> Unit,
    apiKeyUrl: String,
    apiKeyUrlDisplay: String,
    selectedModel: SettingsModel?,
    models: ImmutableList<SettingsModel>,
    onSelectModel: (String) -> Unit,
    connectionStatus: ConnectionStatus,
    testTag: String? = null,
    onOpenAppPermissionSettings: () -> Unit = {},
) {
    ApiKeyField(
        apiKey = apiKey,
        onChangeApiKey = onChangeApiKey,
        labelText = stringResource(Res.string.settings_api_key_label),
        testTag = testTag,
    )

    Spacer(Modifier.height(8.dp))

    ConnectionStatusIndicator(connectionStatus, onOpenAppPermissionSettings)

    Spacer(Modifier.height(8.dp))

    val linkColor = MaterialTheme.colorScheme.primary

    val copyApiKeyPromptString = stringResource(Res.string.settings_sign_in_copy_api_key_from)
    val annotatedString = remember(apiKeyUrl, apiKeyUrlDisplay) {
        buildAnnotatedString {
            append(copyApiKeyPromptString)
            append(" ")
            withLink(LinkAnnotation.Url(url = apiKeyUrl)) {
                withStyle(style = SpanStyle(color = linkColor)) {
                    append(apiKeyUrlDisplay)
                }
            }
        }
    }
    Text(
        annotatedString,
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.onBackground,
    )

    Spacer(Modifier.height(16.dp))

    if (connectionStatus == ConnectionStatus.Connected || models.isNotEmpty()) {
        ModelSelection(selectedModel, models, onSelectModel)
    }
}

@Composable
private fun OpenAICompatibleSettings(
    baseUrl: String,
    onChangeBaseUrl: (String) -> Unit,
    apiKey: String,
    onChangeApiKey: (String) -> Unit,
    selectedModel: SettingsModel?,
    models: ImmutableList<SettingsModel>,
    onSelectModel: (String) -> Unit,
    useCustomModel: Boolean,
    customModelId: String,
    onToggleUseCustomModel: (Boolean) -> Unit,
    onChangeCustomModelId: (String) -> Unit,
    connectionStatus: ConnectionStatus,
    onOpenAppPermissionSettings: () -> Unit = {},
) {
    KaiClearableTextField(
        value = baseUrl,
        onValueChange = onChangeBaseUrl,
        label = {
            Text(
                stringResource(Res.string.settings_base_url_label),
                color = MaterialTheme.colorScheme.onBackground,
            )
        },
        singleLine = true,
    )
    if (baseUrl.isNotBlank()) {
        Text(
            text = "${baseUrl.trimEnd('/')}${Service.OpenAICompatible.chatUrl}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 4.dp),
        )
    }

    Spacer(Modifier.height(8.dp))

    ApiKeyField(
        apiKey = apiKey,
        onChangeApiKey = onChangeApiKey,
        labelText = stringResource(Res.string.settings_api_key_optional_label),
        singleLine = true,
    )

    Spacer(Modifier.height(8.dp))

    ConnectionStatusIndicator(connectionStatus, onOpenAppPermissionSettings)

    Spacer(Modifier.height(8.dp))

    val linkColor = MaterialTheme.colorScheme.primary
    val setupOllamaText = stringResource(Res.string.settings_openai_compatible_setup_ollama)
    val orOtherServiceText = stringResource(Res.string.settings_openai_compatible_or_other_service)
    val providersText = stringResource(Res.string.settings_openai_compatible_providers)
    val annotatedString = remember(setupOllamaText, orOtherServiceText, providersText, linkColor) {
        buildAnnotatedString {
            append(setupOllamaText)
            append(" ")
            withLink(LinkAnnotation.Url(url = "https://github.com/ollama/ollama")) {
                withStyle(style = SpanStyle(color = linkColor)) {
                    append("github.com/ollama/ollama")
                }
            }
            append(" ")
            append(orOtherServiceText)
            append(" ")
            withLink(LinkAnnotation.Url(url = "https://docs.litellm.ai/docs/providers")) {
                withStyle(style = SpanStyle(color = linkColor)) {
                    append(providersText)
                }
            }
        }
    }
    Text(
        annotatedString,
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.onBackground,
    )

    Spacer(Modifier.height(16.dp))

    if (connectionStatus == ConnectionStatus.Connected || models.isNotEmpty()) {
        ModelSelection(selectedModel, models, onSelectModel)
        Spacer(Modifier.height(8.dp))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleUseCustomModel(!useCustomModel) }
            .handCursor(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = useCustomModel,
            onCheckedChange = onToggleUseCustomModel,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(Res.string.settings_custom_model_label),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }

    if (useCustomModel) {
        Spacer(Modifier.height(8.dp))
        KaiClearableTextField(
            value = customModelId,
            onValueChange = onChangeCustomModelId,
            label = {
                Text(
                    stringResource(Res.string.settings_model_label),
                    color = MaterialTheme.colorScheme.onBackground,
                )
            },
            singleLine = true,
        )
        Text(
            text = stringResource(Res.string.settings_custom_model_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 4.dp),
        )
    }
}

@Composable
private fun LiteRTSettings(
    selectedModel: SettingsModel?,
    downloadedModels: ImmutableList<SettingsModel>,
    availableModels: ImmutableList<LocalModel>,
    importedModels: ImmutableList<LocalModel>,
    totalDeviceMemoryBytes: Long,
    freeSpaceBytes: Long,
    downloadingModelId: String?,
    downloadProgress: Float?,
    downloadError: DownloadError?,
    importingFileName: String?,
    importProgress: Float?,
    importError: ModelImportError?,
    onSelectModel: (String) -> Unit,
    onDownloadModel: (LocalModel) -> Unit,
    onCancelDownload: () -> Unit,
    onImportModel: (PlatformFile) -> Unit,
    onCancelImport: () -> Unit,
    onDeleteModel: (String) -> Unit,
    onChangeModelContextTokens: (String, Int) -> Unit,
    modelContextTokens: ImmutableMap<String, Int>,
) {
    val downloadedIds = remember(downloadedModels) { downloadedModels.map { it.id }.toSet() }
    val isBusy = downloadingModelId != null || importingFileName != null
    val isPreview = LocalInspectionMode.current

    val filePickerLauncher = if (!isPreview) {
        rememberFilePickerLauncher(
            type = FileKitType.File(extensions = listOf("litertlm")),
        ) { file ->
            if (file != null) onImportModel(file)
        }
    } else {
        null
    }

    Text(
        text = stringResource(Res.string.litert_on_device_description),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Spacer(Modifier.height(4.dp))

    Text(
        text = stringResource(Res.string.litert_tool_support),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Spacer(Modifier.height(12.dp))

    Text(
        text = stringResource(Res.string.litert_import_description),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(8.dp))
    OutlinedButton(
        onClick = { filePickerLauncher?.launch() },
        modifier = Modifier.handCursor(),
        enabled = !isBusy && filePickerLauncher != null,
    ) {
        Text(stringResource(Res.string.litert_import))
    }

    if (importingFileName != null) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(Res.string.litert_importing) + " $importingFileName",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (importProgress != null) {
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { importProgress },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${(importProgress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(
                    onClick = onCancelImport,
                    modifier = Modifier.handCursor(),
                ) {
                    Text(
                        text = stringResource(Res.string.litert_cancel),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        } else {
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
    }

    if (importError != null && importingFileName == null) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(
                when (importError) {
                    ModelImportError.INVALID_EXTENSION -> Res.string.litert_error_import_invalid
                    ModelImportError.NOT_ENOUGH_DISK_SPACE -> Res.string.litert_error_not_enough_disk_space
                    ModelImportError.FILE_TOO_SMALL -> Res.string.litert_error_import_too_small
                    ModelImportError.COPY_FAILED -> Res.string.litert_error_import_failed
                    ModelImportError.CANCELLED -> Res.string.litert_error_import_failed
                },
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }

    Spacer(Modifier.height(12.dp))

    availableModels.forEach { model ->
        LocalModelCard(
            model = model,
            isDownloaded = model.id in downloadedIds,
            isSelected = selectedModel?.id == model.id,
            isDownloading = downloadingModelId == model.id,
            downloadProgress = downloadProgress,
            isBusy = isBusy,
            totalDeviceMemoryBytes = totalDeviceMemoryBytes,
            modelContextTokens = modelContextTokens,
            onSelectModel = onSelectModel,
            onDownloadModel = onDownloadModel,
            onCancelDownload = onCancelDownload,
            onDeleteModel = onDeleteModel,
            onChangeModelContextTokens = onChangeModelContextTokens,
            showImportedBadge = false,
        )
    }

    if (importedModels.isNotEmpty()) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(Res.string.litert_imported),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(4.dp))
        importedModels.forEach { model ->
            LocalModelCard(
                model = model,
                isDownloaded = true,
                isSelected = selectedModel?.id == model.id,
                isDownloading = false,
                downloadProgress = null,
                isBusy = isBusy,
                totalDeviceMemoryBytes = totalDeviceMemoryBytes,
                modelContextTokens = modelContextTokens,
                onSelectModel = onSelectModel,
                onDownloadModel = onDownloadModel,
                onCancelDownload = onCancelDownload,
                onDeleteModel = onDeleteModel,
                onChangeModelContextTokens = onChangeModelContextTokens,
                showImportedBadge = true,
            )
        }
    }

    if (downloadError != null) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(
                when (downloadError) {
                    DownloadError.NOT_ENOUGH_DISK_SPACE -> Res.string.litert_error_not_enough_disk_space
                    DownloadError.NETWORK_ERROR -> Res.string.litert_error_network
                    DownloadError.DOWNLOAD_INCOMPLETE -> Res.string.litert_error_download_incomplete
                    DownloadError.CHECKSUM_MISMATCH -> Res.string.litert_error_checksum_mismatch
                },
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }

    Spacer(Modifier.height(8.dp))

    Text(
        text = stringResource(Res.string.litert_free_space, formatFileSize(freeSpaceBytes)),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun LocalModelCard(
    model: LocalModel,
    isDownloaded: Boolean,
    isSelected: Boolean,
    isDownloading: Boolean,
    downloadProgress: Float?,
    isBusy: Boolean,
    totalDeviceMemoryBytes: Long,
    modelContextTokens: ImmutableMap<String, Int>,
    onSelectModel: (String) -> Unit,
    onDownloadModel: (LocalModel) -> Unit,
    onCancelDownload: () -> Unit,
    onDeleteModel: (String) -> Unit,
    onChangeModelContextTokens: (String, Int) -> Unit,
    showImportedBadge: Boolean,
) {
    val steps = (model.maxContextTokens - model.defaultContextTokens) / 1024
    val storedContextTokens = modelContextTokens[model.id] ?: model.defaultContextTokens
    var contextSliderValue by remember(storedContextTokens) {
        mutableStateOf(((storedContextTokens - model.defaultContextTokens) / 1024).toFloat())
    }
    val contextTokens = model.defaultContextTokens + (contextSliderValue.roundToInt() * 1024)
    val estimatedMemoryMb = estimateGpuMemoryMb(model, contextTokens)
    val performance = calculateDevicePerformance(totalDeviceMemoryBytes, estimatedMemoryMb)

    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        tonalElevation = if (isSelected) 3.dp else 1.dp,
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isDownloaded) {
                    RadioButton(
                        selected = isSelected,
                        onClick = { onSelectModel(model.id) },
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when {
                            model.isRecommended ->
                                "${model.displayName} (${stringResource(Res.string.litert_recommended)})"

                            showImportedBadge ->
                                "${model.displayName} (${stringResource(Res.string.litert_imported)})"

                            else -> model.displayName
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = formatFileSize(model.sizeBytes),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.width(8.dp))
                        DevicePerformanceLabel(performance)
                    }
                }
                if (isDownloaded) {
                    IconButton(
                        onClick = { onDeleteModel(model.id) },
                        modifier = Modifier.handCursor(),
                        enabled = !isBusy,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else if (!isDownloading) {
                    TextButton(
                        onClick = { onDownloadModel(model) },
                        modifier = Modifier.handCursor(),
                        enabled = !isBusy,
                    ) {
                        Text(stringResource(Res.string.litert_download))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(Res.string.litert_context_size, "${contextTokens / 1024}K"),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            // A model whose export tops out at its own default (LFM2.5) has nothing to
            // drag: a 0f..0f range divides by zero working out the thumb fraction. Show
            // the fixed size as a label and leave the slider out.
            if (steps > 0) {
                KaiSlider(
                    value = contextSliderValue,
                    onValueChange = { contextSliderValue = it },
                    onValueChangeFinished = {
                        onChangeModelContextTokens(model.id, contextTokens)
                    },
                    valueRange = 0f..steps.toFloat(),
                    steps = (steps - 1).coerceAtLeast(0),
                )
            }
            if (isDownloading && downloadProgress != null) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { downloadProgress },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${(downloadProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(
                        onClick = onCancelDownload,
                        modifier = Modifier.handCursor(),
                    ) {
                        Text(
                            text = stringResource(Res.string.litert_cancel),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DevicePerformanceLabel(performance: DevicePerformance) {
    when (performance) {
        DevicePerformance.GOOD -> Text(
            text = stringResource(Res.string.litert_performance_good),
            style = MaterialTheme.typography.labelSmall,
            color = StatusColorConnected,
        )

        DevicePerformance.OK -> Text(
            text = stringResource(Res.string.litert_performance_ok),
            style = MaterialTheme.typography.labelSmall,
            color = StatusColorChecking,
        )

        DevicePerformance.POOR -> Text(
            text = stringResource(Res.string.litert_performance_poor),
            style = MaterialTheme.typography.labelSmall,
            color = StatusColorError,
        )
    }
}

@Composable
private fun ConnectionStatusIndicator(status: ConnectionStatus, onOpenAppPermissionSettings: () -> Unit = {}) {
    when (status) {
        ConnectionStatus.Unknown -> return

        ConnectionStatus.Checking -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(Res.string.settings_status_checking),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        ConnectionStatus.Connected -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(Res.string.settings_status_connected),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        ConnectionStatus.ErrorQuotaExhausted -> {
            val warningColor = Color(0xFFFF9800)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = warningColor,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(Res.string.settings_status_error_quota_exhausted),
                    style = MaterialTheme.typography.bodySmall,
                    color = warningColor,
                )
            }
        }

        ConnectionStatus.ErrorInvalidKey,
        ConnectionStatus.ErrorRateLimited,
        ConnectionStatus.ErrorConnectionFailed,
        ConnectionStatus.ErrorLocalNetworkDenied,
        ConnectionStatus.Error,
        -> {
            val errorMessage = when (status) {
                ConnectionStatus.ErrorInvalidKey -> stringResource(Res.string.settings_status_error_invalid_key)
                ConnectionStatus.ErrorRateLimited -> stringResource(Res.string.settings_status_error_rate_limited)
                ConnectionStatus.ErrorConnectionFailed -> stringResource(Res.string.settings_status_error_connection_failed)
                ConnectionStatus.ErrorLocalNetworkDenied -> stringResource(Res.string.settings_status_error_local_network)
                else -> stringResource(Res.string.settings_status_error)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.error,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            if (status == ConnectionStatus.ErrorLocalNetworkDenied) {
                TextButton(
                    onClick = onOpenAppPermissionSettings,
                    modifier = Modifier.handCursor(),
                ) {
                    Text(stringResource(Res.string.settings_open_app_settings))
                }
            }
        }
    }
}

@Composable
private fun ApiKeyField(
    apiKey: String,
    onChangeApiKey: (String) -> Unit,
    labelText: String,
    testTag: String? = null,
    singleLine: Boolean = false,
) {
    KaiClearableTextField(
        modifier = if (testTag != null) Modifier.testTag(testTag) else Modifier,
        value = apiKey,
        onValueChange = onChangeApiKey,
        label = {
            Text(
                labelText,
                color = MaterialTheme.colorScheme.onBackground,
            )
        },
        singleLine = singleLine,
    )
}
