package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.PersonPin
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.ui.MiraUiState
import com.example.ui.localization.AppLanguage
import com.mira.image.StudioArtStyle
import com.mira.image.StudioBackground
import com.mira.image.StudioHairstyle
import com.mira.image.StudioOutfit
import com.mira.image.StudioPose
import com.mira.image.StudioResolution
import com.mira.memory.model.CharacterProfileEntity
import com.mira.memory.model.StudioImageEntity
import java.io.File

@Composable
fun ImageStudioScreen(
    state: MiraUiState,
    onComposePortrait: (
        CharacterProfileEntity,
        StudioOutfit,
        StudioHairstyle,
        StudioBackground,
        StudioPose,
        StudioArtStyle,
        StudioResolution,
        Uri?,
        Boolean
    ) -> Unit,
    onDeleteImage: (StudioImageEntity) -> Unit,
    onSetAsPortrait: (StudioImageEntity) -> Unit
) {
    val strings = state.strings
    val isBn = state.preferences.uiLanguage == AppLanguage.BN.code

    var selectedCharacter by remember(state.activeCharacter.id) { mutableStateOf(state.activeCharacter) }
    var selectedOutfit by remember { mutableStateOf(StudioOutfit.JAMDANI_SAREE) }
    var selectedHair by remember { mutableStateOf(StudioHairstyle.OPEN_WAVES) }
    var selectedBg by remember { mutableStateOf(StudioBackground.MOONLIT_TERRACE) }
    var selectedPose by remember { mutableStateOf(StudioPose.WARM_SMILE) }
    var selectedStyle by remember { mutableStateOf(StudioArtStyle.PORTRAIT_PHOTO) }
    var selectedResolution by remember { mutableStateOf(StudioResolution.COMPACT_512) }
    var referenceUri by remember { mutableStateOf<Uri?>(null) }
    var consentConfirmed by remember { mutableStateOf(true) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) referenceUri = uri
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Honest 3GB RAM Hardware Feasibility Banner
        state.imageCapability?.let { cap ->
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Memory, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (isBn) cap.feasibilityTitleBn else cap.feasibilityTitleEn,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Text(
                            text = if (isBn) cap.explanationBn else cap.explanationEn,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Character & User-Authorized Photo Picker
        item {
            Text(strings.activeCompanionLabel, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.characters, key = { it.id }) { char ->
                    FilterChip(
                        selected = selectedCharacter.id == char.id,
                        onClick = { selectedCharacter = char },
                        label = { Text("${char.name} (${char.nativeName})") }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier.testTag("studio_pick_photo_button")
                ) {
                    Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(strings.studioPickReference)
                }
                if (referenceUri != null) {
                    OutlinedButton(onClick = { referenceUri = null }) {
                        Text(strings.studioClearReference)
                    }
                }
            }
        }

        // Selectors
        item {
            SelectorSection(strings.studioOutfitLabel, StudioOutfit.entries, selectedOutfit, { selectedOutfit = it }) {
                if (isBn) it.labelBn else it.labelEn
            }
        }
        item {
            SelectorSection(strings.studioHairstyleLabel, StudioHairstyle.entries, selectedHair, { selectedHair = it }) {
                if (isBn) it.labelBn else it.labelEn
            }
        }
        item {
            SelectorSection(strings.studioBackgroundLabel, StudioBackground.entries, selectedBg, { selectedBg = it }) {
                if (isBn) it.labelBn else it.labelEn
            }
        }
        item {
            SelectorSection(strings.studioPoseLabel, StudioPose.entries, selectedPose, { selectedPose = it }) {
                if (isBn) it.labelBn else it.labelEn
            }
        }
        item {
            SelectorSection(strings.studioStyleLabel, StudioArtStyle.entries, selectedStyle, { selectedStyle = it }) {
                if (isBn) it.labelBn else it.labelEn
            }
        }
        item {
            SelectorSection(strings.studioResolutionLabel, StudioResolution.entries, selectedResolution, { selectedResolution = it }) {
                it.label
            }
        }

        // Adult Fictional Consent & Compose Button
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = consentConfirmed,
                    onCheckedChange = { consentConfirmed = it }
                )
                Text(
                    text = strings.studioConsentLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    val effectiveUri = referenceUri ?: selectedCharacter.referencePhotoUri?.let { Uri.parse(it) }
                    onComposePortrait(
                        selectedCharacter,
                        selectedOutfit,
                        selectedHair,
                        selectedBg,
                        selectedPose,
                        selectedStyle,
                        selectedResolution,
                        effectiveUri,
                        consentConfirmed
                    )
                },
                enabled = consentConfirmed && !state.isComposingImage,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("compose_portrait_button")
            ) {
                if (state.isComposingImage) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                } else {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                }
                Text(strings.studioComposeBtn)
            }
        }

        // Saved Studio Gallery
        item {
            Text(strings.studioGalleryTitle, style = MaterialTheme.typography.headlineSmall)
        }

        if (state.studioImages.isEmpty()) {
            item {
                Text(
                    text = strings.studioEmptyGallery,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(state.studioImages, key = { it.id }) { img ->
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        AsyncImage(
                            model = File(img.filePath),
                            contentDescription = "${img.characterName} ${img.outfit}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                                .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                        )
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                "${img.characterName} • ${img.artStyle}",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                "${img.outfit} • ${img.hairstyle} • ${img.background} (${img.resolutionLabel})",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(onClick = { onSetAsPortrait(img) }) {
                                    Icon(Icons.Rounded.PersonPin, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(if (isBn) "প্রোফাইল ছবি করুন" else "Set Portrait")
                                }
                                IconButton(onClick = { onDeleteImage(img) }) {
                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = strings.delete)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun <T> SelectorSection(
    title: String,
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    labelOf: (T) -> String
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(options) { opt ->
                FilterChip(
                    selected = opt == selected,
                    onClick = { onSelect(opt) },
                    label = { Text(labelOf(opt)) }
                )
            }
        }
    }
}
