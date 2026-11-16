package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import com.example.ui.MiraUiState
import com.example.ui.components.CharacterAvatar
import com.example.ui.localization.AppLanguage
import com.mira.character.PersonalityTraits
import com.mira.memory.model.CharacterProfileEntity

@Composable
fun CharactersScreen(
    state: MiraUiState,
    onSelectAndChat: (String) -> Unit,
    onUpdateTraits: (String, PersonalityTraits) -> Unit,
    onUpdateReferencePhoto: (String, Uri?) -> Unit,
    onCreateCharacter: (String, String, String, String, String, String, PersonalityTraits, Uri?) -> Unit,
    onDeleteCharacter: (String) -> Unit
) {
    val strings = state.strings
    val isBn = state.preferences.uiLanguage == AppLanguage.BN.code
    var showCreateDialog by remember { mutableStateOf(false) }
    var photoTargetCharacterId by remember { mutableStateOf<String?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        val targetId = photoTargetCharacterId
        if (uri != null && targetId != null) {
            onUpdateReferencePhoto(targetId, uri)
        }
    }

    val mira = state.characters.firstOrNull { it.isPermanentMira } ?: state.activeCharacter
    val others = state.characters.filterNot { it.isPermanentMira }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("create_character_fab")
            ) {
                Icon(Icons.Rounded.Add, contentDescription = strings.createGirlBtn)
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                MiraPermanentCard(
                    mira = mira,
                    isBn = isBn,
                    permanentBadge = strings.permanentBadge,
                    customizeLabel = strings.customizeMiraTitle,
                    chatLabel = strings.chatWithHer,
                    photoLabel = strings.pickPortraitPhoto,
                    saveLabel = strings.save,
                    onChat = { onSelectAndChat(mira.id) },
                    onPickPhoto = {
                        photoTargetCharacterId = mira.id
                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    onSaveTraits = { traits -> onUpdateTraits(mira.id, traits) }
                )
            }

            item {
                Text(strings.otherGirlsTitle, style = MaterialTheme.typography.headlineSmall)
            }

            items(others, key = { it.id }) { girl ->
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CharacterAvatar(character = girl, modifier = Modifier.size(64.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${girl.name} (${girl.nativeName})", style = MaterialTheme.typography.titleLarge)
                                Text(girl.tagline, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { onDeleteCharacter(girl.id) }) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = strings.delete)
                            }
                        }
                        Text(girl.backstory, style = MaterialTheme.typography.bodyMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { onSelectAndChat(girl.id) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Rounded.ChatBubble, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(strings.chatWithHer)
                            }
                            OutlinedButton(
                                onClick = {
                                    photoTargetCharacterId = girl.id
                                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                }
                            ) {
                                Icon(Icons.Rounded.PhotoCamera, contentDescription = strings.pickPortraitPhoto, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(64.dp)) }
        }
    }

    if (showCreateDialog) {
        CreateCharacterDialog(
            isBn = isBn,
            onDismiss = { showCreateDialog = false },
            onCreate = { name, nativeName, tagline, backstory, appearance, lang ->
                onCreateCharacter(
                    name,
                    nativeName,
                    tagline,
                    backstory,
                    appearance,
                    lang,
                    PersonalityTraits(),
                    null
                )
                showCreateDialog = false
            }
        )
    }
}

@Composable
private fun MiraPermanentCard(
    mira: CharacterProfileEntity,
    isBn: Boolean,
    permanentBadge: String,
    customizeLabel: String,
    chatLabel: String,
    photoLabel: String,
    saveLabel: String,
    onChat: () -> Unit,
    onPickPhoto: () -> Unit,
    onSaveTraits: (PersonalityTraits) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var traits by remember(mira) { mutableStateOf(PersonalityTraits.fromEntity(mira)) }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Verified, contentDescription = null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(6.dp))
                    Text(permanentBadge, style = MaterialTheme.typography.labelSmall)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                CharacterAvatar(character = mira, modifier = Modifier.size(76.dp), borderWidth = 2.5.dp)
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("${mira.name} (${mira.nativeName})", style = MaterialTheme.typography.headlineSmall)
                    Text(mira.tagline, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Text(mira.backstory, style = MaterialTheme.typography.bodyMedium)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onChat, modifier = Modifier.weight(1f)) {
                    Text(chatLabel)
                }
                OutlinedButton(onClick = onPickPhoto) {
                    Icon(Icons.Rounded.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(photoLabel)
                }
            }

            OutlinedButton(
                onClick = { expanded = !expanded },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("toggle_mira_personality")
            ) {
                Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(customizeLabel, modifier = Modifier.weight(1f))
                Icon(if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, contentDescription = null)
            }

            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    TraitSlider(if (isBn) "রোমান্টিক" else "Romantic", traits.romantic) { traits = traits.copy(romantic = it) }
                    TraitSlider(if (isBn) "যত্নশীল" else "Caring", traits.caring) { traits = traits.copy(caring = it) }
                    TraitSlider(if (isBn) "স্নেহশীল" else "Loving", traits.loving) { traits = traits.copy(loving = it) }
                    TraitSlider(if (isBn) "চঞ্চল" else "Playful", traits.playful) { traits = traits.copy(playful = it) }
                    TraitSlider(if (isBn) "রসিক" else "Funny", traits.funny) { traits = traits.copy(funny = it) }
                    TraitSlider(if (isBn) "খুনসুটিপূর্ণ" else "Flirty", traits.flirty) { traits = traits.copy(flirty = it) }
                    TraitSlider(if (isBn) "লাজুক" else "Shy", traits.shy) { traits = traits.copy(shy = it) }
                    TraitSlider(if (isBn) "আবেগপ্রবণ" else "Emotional", traits.emotional) { traits = traits.copy(emotional = it) }
                    TraitSlider(if (isBn) "দুষ্টুমিষ্টি" else "Teasing", traits.teasing) { traits = traits.copy(teasing = it) }
                    TraitSlider(if (isBn) "সহানুভূতিশীল" else "Supportive", traits.supportive) { traits = traits.copy(supportive = it) }

                    Button(
                        onClick = { onSaveTraits(traits) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(saveLabel)
                    }
                }
            }
        }
    }
}

@Composable
private fun TraitSlider(label: String, value: Float, onValueChange: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "$label (${(value * 100).toInt()}%)",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.width(136.dp)
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun CreateCharacterDialog(
    isBn: Boolean,
    onDismiss: () -> Unit,
    onCreate: (String, String, String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var nativeName by remember { mutableStateOf("") }
    var tagline by remember { mutableStateOf("") }
    var backstory by remember { mutableStateOf("") }
    var appearance by remember { mutableStateOf("") }
    var lang by remember { mutableStateOf("bn") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (isBn) "নতুন প্রাপ্তবয়স্ক কাল্পনিক চরিত্র" else "Create Adult Fictional AI Girl")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(if (isBn) "নাম (ইংরেজি)" else "Name") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = nativeName,
                    onValueChange = { nativeName = it },
                    label = { Text(if (isBn) "বাংলা নাম" else "Native Name (e.g. রিয়া)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = tagline,
                    onValueChange = { tagline = it },
                    label = { Text(if (isBn) "সংক্ষিপ্ত পরিচয়" else "Tagline") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = backstory,
                    onValueChange = { backstory = it },
                    label = { Text(if (isBn) "গল্প ও ব্যক্তিত্ব" else "Backstory & Personality") },
                    maxLines = 3
                )
                OutlinedTextField(
                    value = appearance,
                    onValueChange = { appearance = it },
                    label = { Text(if (isBn) "চেহারা ও পোশাক" else "Appearance Description") },
                    maxLines = 2
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("bn" to "বাংলা", "en" to "English", "hi" to "हिन्दी").forEach { (c, lbl) ->
                        FilterChip(selected = lang == c, onClick = { lang = c }, label = { Text(lbl) })
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(name, nativeName, tagline, backstory, appearance, lang) },
                enabled = name.isNotBlank()
            ) {
                Text(if (isBn) "তৈরি করুন" else "Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isBn) "বাতিল" else "Cancel")
            }
        }
    )
}
