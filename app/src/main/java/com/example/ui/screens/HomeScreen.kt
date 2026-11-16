package com.example.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.MiraUiState
import com.example.ui.components.CharacterAvatar
import com.example.ui.theme.OfflineEmerald

@Composable
fun HomeScreen(
    state: MiraUiState,
    onOpenChat: () -> Unit,
    onOpenVoice: () -> Unit,
    onOpenStudio: () -> Unit,
    onOpenGirls: () -> Unit,
    onOpenMemories: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenModelManager: () -> Unit,
    onFreeRam: () -> Unit
) {
    val strings = state.strings
    val mira = state.activeCharacter

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Atmospheric Terrace Hero Card
        item {
            Card(
                shape = RoundedCornerShape(26.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(R.drawable.img_hero_terrace),
                        contentDescription = "Kolkata romantic terrace evening",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0x55120A10),
                                        Color(0xEE1A0E17)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                            .align(Alignment.BottomStart)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = OfflineEmerald.copy(alpha = 0.20f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Rounded.Shield,
                                    contentDescription = null,
                                    tint = OfflineEmerald,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = strings.offlineBadge,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = OfflineEmerald
                                )
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CharacterAvatar(
                                character = mira,
                                modifier = Modifier.size(74.dp),
                                borderWidth = 2.5.dp
                            )
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${mira.name} (${mira.nativeName})",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = Color.White
                                )
                                Text(
                                    text = mira.tagline,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFFE7D3DC),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = onOpenChat,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("home_chat_button")
                            ) {
                                Icon(Icons.Rounded.ChatBubble, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(strings.startChatting, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            FilledTonalButton(
                                onClick = onOpenVoice,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("home_voice_button")
                            ) {
                                Icon(Icons.Rounded.GraphicEq, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(strings.talkByVoice, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }

        // Primary Feature Grid (Studio, Models, Other AI Girls, Memories, Settings)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    HomeActionCard(
                        title = strings.openStudio,
                        subtitle = "${state.studioImages.size} saved",
                        icon = Icons.Rounded.AutoAwesome,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_studio_card"),
                        onClick = onOpenStudio
                    )
                    HomeActionCard(
                        title = strings.manageModels,
                        subtitle = "${state.localModels.size} GGUF",
                        icon = Icons.Rounded.Psychology,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_models_card"),
                        onClick = onOpenModelManager
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    HomeActionCard(
                        title = strings.navGirls,
                        subtitle = "${state.characters.size} profiles",
                        icon = Icons.Rounded.Groups,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_girls_card"),
                        onClick = onOpenGirls
                    )
                    HomeActionCard(
                        title = strings.navMemories,
                        subtitle = "${state.memories.size} facts",
                        icon = Icons.Rounded.Favorite,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_memories_card"),
                        onClick = onOpenMemories
                    )
                }
            }
        }

        // Remembered About You
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(strings.quickMemoriesTitle, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = strings.navSettings,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onOpenSettings)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                if (state.memories.isEmpty()) {
                    Text(
                        text = strings.emptyMemories,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.memories.take(6), key = { it.id }) { mem ->
                            AssistChip(
                                onClick = onOpenMemories,
                                label = { Text("${mem.factKey}: ${mem.factValue}", maxLines = 1) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Rounded.Favorite,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }

        // 3GB RAM & Android 8.1 Performance Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.Memory,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(strings.deviceHealthTitle, style = MaterialTheme.typography.titleMedium)
                        }
                        OutlinedButton(onClick = onFreeRam) {
                            Text(strings.unloadModelsBtn)
                        }
                    }

                    LinearProgressIndicator(
                        progress = { state.ramSnapshot.usedPercent },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(50))
                    )
                    Text(
                        text = "${state.ramSnapshot.availableMb} MB free of ${state.ramSnapshot.totalMb} MB • App heap ${state.ramSnapshot.appHeapUsedMb} MB",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onOpenSettings)
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.Settings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "${strings.navSettings} • ${strings.uiLanguageTitle}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
