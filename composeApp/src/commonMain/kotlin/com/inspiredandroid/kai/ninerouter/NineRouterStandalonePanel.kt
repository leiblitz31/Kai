@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.inspiredandroid.kai.ninerouter

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.inspiredandroid.kai.data.AppSettings
import kai.composeapp.generated.resources.Res
import kai.composeapp.generated.resources.ninerouter_add_provider
import kai.composeapp.generated.resources.ninerouter_no_providers
import kai.composeapp.generated.resources.ninerouter_provider_label
import org.jetbrains.compose.resources.stringResource

@Composable
fun NineRouterStandaloneSettings(
    appSettings: AppSettings,
    modifier: Modifier = Modifier,
) {
    var config by remember { mutableStateOf(appSettings.getNineRouterConfig()) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var apiKeyDraft by remember { mutableStateOf("") }
    var accountIdDraft by remember { mutableStateOf("") }

    fun refresh() { config = appSettings.getNineRouterConfig() }

    Column(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(Res.string.ninerouter_provider_label), style = MaterialTheme.typography.titleMedium)
        if (config.providers.isEmpty()) {
            Text(stringResource(Res.string.ninerouter_no_providers), style = MaterialTheme.typography.bodyMedium)
        } else {
            LazyColumn(modifier = Modifier.heightIn(max = 260.dp)) {
                items(config.providers.entries.toList()) { (id, creds) ->
                    val meta = NineRouterRegistry.find(id)
                    ElevatedCard(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("${meta?.alias ?: id} — ${meta?.id ?: id}", style = MaterialTheme.typography.titleSmall)
                            Text("Key: ${if (creds.apiKey.isBlank()) "(empty)" else creds.apiKey.take(8) + "…"}", style = MaterialTheme.typography.bodySmall)
                            if (meta?.needsAccountId == true) Text("AccountId: ${creds.accountId.take(12)}", style = MaterialTheme.typography.bodySmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = {
                                    selectedId = id
                                    apiKeyDraft = creds.apiKey
                                    accountIdDraft = creds.accountId
                                }) { Text("Edit") }
                                OutlinedButton(onClick = {
                                    appSettings.removeNineProvider(id); refresh()
                                }) { Text("Hapus") }
                            }
                        }
                    }
                }
            }
        }
        Divider()
        Text("Tambah / Edit Provider", style = MaterialTheme.typography.titleSmall)
        // Simple picker: show first 30 as example; full searchable picker via dialog in next iteration
        var pickerExpanded by remember { mutableStateOf(false) }
        val allIds = remember { NineRouterRegistry.all.map { it.id }.sorted() }
        ExposedDropdownMenuBox(expanded = pickerExpanded, onExpandedChange = { pickerExpanded = !pickerExpanded }) {
            OutlinedTextField(
                value = selectedId ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text("Provider ID") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = pickerExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = pickerExpanded, onDismissRequest = { pickerExpanded = false }) {
                allIds.take(60).forEach { pid ->
                    DropdownMenuItem(text = { Text(pid) }, onClick = {
                        selectedId = pid
                        val existing = config.providers[pid]
                        apiKeyDraft = existing?.apiKey ?: ""
                        accountIdDraft = existing?.accountId ?: ""
                        pickerExpanded = false
                    })
                }
            }
        }
        OutlinedTextField(value = apiKeyDraft, onValueChange = { apiKeyDraft = it }, label = { Text("API Key / Token") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        val needAcc = selectedId?.let { NineRouterRegistry.find(it)?.needsAccountId } == true
        if (needAcc) {
            OutlinedTextField(value = accountIdDraft, onValueChange = { accountIdDraft = it }, label = { Text("Account ID (Cloudflare)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                val pid = selectedId?.trim().orEmpty()
                if (pid.isBlank()) return@Button
                appSettings.setNineProviderCredentials(pid, NineProviderCredentials(apiKey = apiKeyDraft.trim(), accountId = accountIdDraft.trim()))
                refresh()
            }, enabled = (selectedId?.isNotBlank() == true && apiKeyDraft.isNotBlank())) { Text(stringResource(Res.string.ninerouter_add_provider)) }
            OutlinedButton(onClick = { selectedId = null; apiKeyDraft = ""; accountIdDraft = "" }) { Text("Batal") }
        }
        Text("Total provider terdaftar: ${config.providers.size} / ${NineRouterRegistry.all.size} tersedia", style = MaterialTheme.typography.bodySmall)
    }
}
