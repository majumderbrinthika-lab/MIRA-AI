package com.example.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.MiraUiState
import com.example.ui.localization.AppLanguage
import com.mira.ai.models.ModelCatalog
import com.mira.ai.models.RecommendedGgufModel
import com.mira.memory.model.LocalModelEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelManagerSheet(
    state: MiraUiState,
    onDismiss: () -> Unit,
    onImportUri: (android.net.Uri) -> Unit,
    onDownloadModel: (RecommendedGgufModel) -> Unit,
    onCancelDownload: () -> Unit,
    onLoadModel: (LocalModelEntity) -> Unit,
    onUnloadModel: () -> Unit,
    onDeleteModel: (LocalModelEntity) -> Unit
) {
    val strings = state.strings
    val isBn = state.preferences.uiLanguage == AppLanguage.BN.code
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val ggufPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) onImportUri(uri)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.modelManagerTitle,
                        style = MaterialTheme.typography.headlineSmall
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = strings.close)
                    }
                }
            }

            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.Memory,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "${strings.ramMonitorLabel}: ${state.ramSnapshot.availableMb} MB free / ${state.ramSnapshot.totalMb} MB",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Text(
                            text = strings.nativeRuntimeNote,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Button(
                    onClick = { ggufPicker.launch(arrayOf("*/*")) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("import_gguf_button")
                ) {
                    Icon(Icons.Rounded.FileOpen, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(strings.importGgufBtn)
                }
            }

            if (state.downloadState.isDownloading) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Downloading ${state.downloadState.downloadedMb} / ${state.downloadState.totalMb} MB (${state.downloadState.progressPercent}%)",
                                style = MaterialTheme.typography.labelLarge
                            )
                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { state.downloadState.progressPercent / 100f },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(onClick = onCancelDownload) {
                                Text(strings.cancel)
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = strings.installedModelsTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (state.localModels.isEmpty()) {
                item {
                    Text(
                        text = if (isBn) "কোনো GGUF ফাইল ইম্পোর্ট করা হয়নি—বর্তমানে জিরো-র‍্যাম অফলাইন পার্সোনা ইঞ্জিন সক্রিয়।"
                        else "No GGUF files imported yet. Using MIRA's Zero-RAM Offline Persona Engine.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(state.localModels, key = { it.id }) { model ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(model.displayName, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${model.architecture.uppercase()} • ${model.quantization} • ~${model.estimatedRamMb} MB RAM",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (model.isLoaded) {
                                    FilledTonalButton(onClick = onUnloadModel) {
                                        Text(strings.unloadModelBtn)
                                    }
                                } else {
                                    Button(onClick = { onLoadModel(model) }) {
                                        Text(strings.loadModelBtn)
                                    }
                                }
                                IconButton(onClick = { onDeleteModel(model) }) {
                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = strings.delete)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = strings.recommendedModelsTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                if (state.preferences.strictOfflineLock) {
                    Text(
                        text = if (isBn) "সেটিংসে 'সম্পূর্ণ অফলাইন লক' চালু আছে—ডাউনলোড করতে সেটি বন্ধ করুন।"
                        else "Strict Offline Lock is ON in Settings—disable it to enable user-approved downloads.",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            items(ModelCatalog.recommendedModels, key = { it.id }) { rec ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(rec.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                "${rec.quantization} • ${rec.approxSizeMb} MB • ~${rec.estimatedRamMb} MB RAM",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                if (isBn) rec.compatibilityNoteBn else rec.compatibilityNoteEn,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        FilledTonalButton(
                            onClick = { onDownloadModel(rec) },
                            enabled = !state.preferences.strictOfflineLock && !state.downloadState.isDownloading
                        ) {
                            Icon(Icons.Rounded.CloudDownload, contentDescription = strings.downloadModelBtn)
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}
