package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Female
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.ui.MiraUiState
import com.example.ui.components.CharacterAvatar
import com.example.ui.localization.AppLanguage
import com.mira.voice.SpeechListenState

@Composable
fun VoiceConversationScreen(
    state: MiraUiState,
    onStartListening: (String) -> Unit,
    onStopListening: () -> Unit,
    onStopSpeaking: () -> Unit,
    onUpdateVoiceTuning: (Float, Float, String?) -> Unit,
    onSelectLanguage: (String) -> Unit,
    onInstallTtsData: () -> Unit,
    onRefreshVoices: () -> Unit
) {
    val context = LocalContext.current
    val strings = state.strings
    val isBn = state.preferences.uiLanguage == AppLanguage.BN.code
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
        if (granted) {
            onStartListening(state.preferences.preferredChatLanguage)
        }
    }

    val isListening = state.speechListenState is SpeechListenState.Listening ||
        state.speechListenState is SpeechListenState.Partial
    val targetScale = when {
        state.isSpeaking -> 1.18f
        isListening -> 1.08f + (state.micRmsLevel * 0.28f)
        else -> 1.0f
    }
    val orbScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(dampingRatio = 0.55f),
        label = "voice_orb_scale"
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.Female,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(strings.voiceFemaleOnlyBadge, style = MaterialTheme.typography.labelLarge)
                    }
                }

                // Language selector
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("bn" to "বাংলা", "en" to "English", "hi" to "हिन्दी").forEach { (code, label) ->
                        FilterChip(
                            selected = state.preferences.preferredChatLanguage == code,
                            onClick = { onSelectLanguage(code) },
                            label = { Text(label) }
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))

                // Pulsing Romantic Voice Orb
                Box(
                    modifier = Modifier.size(188.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(172.dp)
                            .scale(orbScale)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.42f),
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.04f)
                                    )
                                )
                            )
                    )
                    CharacterAvatar(
                        character = state.activeCharacter,
                        modifier = Modifier.size(124.dp),
                        borderWidth = 3.dp
                    )
                }

                val statusText = when (val st = state.speechListenState) {
                    SpeechListenState.Listening -> strings.listeningNow
                    is SpeechListenState.Partial -> st.text
                    is SpeechListenState.Error -> if (isBn) st.messageBn else st.messageEn
                    else -> if (state.isSpeaking) strings.speakingNow else strings.tapToSpeak
                }
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Latest Mira Reply preview
                state.messages.lastOrNull { !it.isUser }?.let { lastReply ->
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = lastReply.content,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }

                // Mic / Stop Button
                if (!hasMicPermission) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(strings.micPermissionNeeded, style = MaterialTheme.typography.bodyMedium)
                            Button(
                                onClick = { micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                                modifier = Modifier.testTag("grant_mic_button")
                            ) {
                                Text(strings.grantMicPermission)
                            }
                        }
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Button(
                            onClick = {
                                if (isListening) onStopListening()
                                else onStartListening(state.preferences.preferredChatLanguage)
                            },
                            modifier = Modifier
                                .height(54.dp)
                                .testTag("voice_mic_button")
                        ) {
                            Icon(
                                if (isListening) Icons.Rounded.Stop else Icons.Rounded.Mic,
                                contentDescription = strings.tapToSpeak
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(if (isListening) strings.stopVoice else strings.tapToSpeak)
                        }
                        if (state.isSpeaking) {
                            OutlinedButton(
                                onClick = onStopSpeaking,
                                modifier = Modifier.height(54.dp)
                            ) {
                                Icon(Icons.Rounded.Stop, contentDescription = strings.stopVoice)
                                Spacer(Modifier.width(6.dp))
                                Text(strings.stopVoice)
                            }
                        }
                    }
                }
            }
        }

        // Android 8.1 Offline Bengali Female Voice Compatibility Report
        item {
            val report = state.voiceReport
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.WarningAmber,
                            contentDescription = null,
                            tint = if (report.offlineBengaliFemaleAvailable) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.secondary
                            }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (isBn) "Android 8.1 অফলাইন ভয়েস অবস্থা" else "Android 8.1 Offline Voice Check",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Text(
                        text = if (isBn) report.limitationMessageBn else report.limitationMessageEn,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onInstallTtsData) {
                            Text(strings.installOfflineVoiceBtn)
                        }
                        OutlinedButton(onClick = onRefreshVoices) {
                            Text(if (isBn) "রিফ্রেশ" else "Re-scan")
                        }
                    }
                }
            }
        }

        // Speed & Volume Controls
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "${strings.speedLabel}: ${"%.2f".format(state.preferences.ttsSpeed)}x",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Slider(
                        value = state.preferences.ttsSpeed,
                        onValueChange = { newSpeed ->
                            onUpdateVoiceTuning(newSpeed, state.preferences.ttsVolume, null)
                        },
                        valueRange = 0.65f..1.45f
                    )
                    Text(
                        "${strings.volumeLabel}: ${(state.preferences.ttsVolume * 100).toInt()}%",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Slider(
                        value = state.preferences.ttsVolume,
                        onValueChange = { newVol ->
                            onUpdateVoiceTuning(state.preferences.ttsSpeed, newVol, null)
                        },
                        valueRange = 0.1f..1.0f
                    )
                }
            }
        }

        // Female-Only Voice Picker (Never lists male voices)
        item {
            Text(strings.selectFemaleVoice, style = MaterialTheme.typography.titleMedium)
        }

        if (state.femaleVoices.isEmpty()) {
            item {
                Text(
                    strings.noOfflineVoicesFound,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(state.femaleVoices, key = { it.voiceName }) { voice ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            onUpdateVoiceTuning(
                                state.preferences.ttsSpeed,
                                state.preferences.ttsVolume,
                                voice.voiceName
                            )
                        }
                        .padding(vertical = 6.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = state.preferences.selectedFemaleVoiceName == voice.voiceName,
                        onClick = {
                            onUpdateVoiceTuning(
                                state.preferences.ttsSpeed,
                                state.preferences.ttsVolume,
                                voice.voiceName
                            )
                        }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(voice.displayLabel, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
