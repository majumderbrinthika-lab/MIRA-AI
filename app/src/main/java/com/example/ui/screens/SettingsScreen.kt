package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.ui.MiraUiState
import com.example.ui.localization.AppLanguage

@Composable
fun SettingsScreen(
    state: MiraUiState,
    onSelectUiLanguage: (AppLanguage) -> Unit,
    onToggleDarkTheme: (Boolean) -> Unit,
    onToggleStrictOffline: (Boolean) -> Unit,
    onConfigureAppLock: (Boolean, String?) -> Unit,
    onOpenModelManager: () -> Unit,
    onClearStudioCache: () -> Unit,
    onFreeRam: () -> Unit,
    onClearCrashReport: () -> Unit
) {
    val strings = state.strings
    val isBn = state.preferences.uiLanguage == AppLanguage.BN.code
    var showPinDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Bilingual App Language
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(strings.uiLanguageTitle, style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilterChip(
                            selected = !isBn,
                            onClick = { onSelectUiLanguage(AppLanguage.EN) },
                            label = { Text("English") },
                            modifier = Modifier.testTag("lang_en_chip")
                        )
                        FilterChip(
                            selected = isBn,
                            onClick = { onSelectUiLanguage(AppLanguage.BN) },
                            label = { Text("বাংলা (Bengali)") },
                            modifier = Modifier.testTag("lang_bn_chip")
                        )
                    }
                }
            }
        }

        // Theme Mode
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(strings.themeModeTitle, style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (state.preferences.darkTheme) strings.darkThemeName else strings.lightThemeName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = state.preferences.darkTheme,
                        onCheckedChange = onToggleDarkTheme,
                        modifier = Modifier.testTag("theme_mode_switch")
                    )
                }
            }
        }

        // Strict Offline Lock & App Lock PIN
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(strings.strictOfflineTitle, style = MaterialTheme.typography.titleMedium)
                            Text(
                                strings.strictOfflineDesc,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = state.preferences.strictOfflineLock,
                            onCheckedChange = onToggleStrictOffline
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.width(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(strings.appLockTitle, style = MaterialTheme.typography.titleMedium)
                            }
                            Text(
                                strings.appLockDesc,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = state.preferences.appLockEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled) showPinDialog = true
                                else onConfigureAppLock(false, null)
                            }
                        )
                    }
                    OutlinedButton(onClick = { showPinDialog = true }) {
                        Text(strings.setPinBtn)
                    }
                }
            }
        }

        // Model Manager, Cache & RAM Controls
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = onOpenModelManager, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Rounded.Psychology, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(strings.modelManagerTitle)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = onFreeRam, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Rounded.Memory, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(strings.unloadModelsBtn)
                        }
                        OutlinedButton(onClick = onClearStudioCache, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Rounded.CleaningServices, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(strings.clearStudioCacheBtn)
                        }
                    }
                }
            }
        }

        // Crash Recovery Diagnostics
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(strings.crashRecoveryTitle, style = MaterialTheme.typography.titleMedium)
                    val crash = state.preferences.lastCrashReport
                    if (crash.isBlank()) {
                        Text(
                            text = if (isBn) "কোনো ক্র্যাশ শনাক্ত হয়নি • Android 8.1+ স্টেবল মোড"
                            else "No crash logs detected • Android 8.1+ stable session",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(crash, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = onClearCrashReport) {
                            Text(strings.delete)
                        }
                    }
                }
            }
        }

        // Privacy Charter
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text(strings.privacyCharterTitle, style = MaterialTheme.typography.titleMedium)
                    }
                    Text(
                        text = strings.privacyCharterBody,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showPinDialog) {
        var newPin by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text(strings.setPinBtn) },
            text = {
                OutlinedTextField(
                    value = newPin,
                    onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) newPin = it },
                    label = { Text("4-digit PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onConfigureAppLock(true, newPin)
                        showPinDialog = false
                    },
                    enabled = newPin.length == 4
                ) {
                    Text(strings.save)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}
