package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Search
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
import com.example.ui.localization.AppLanguage
import com.mira.memory.model.MemoryCategory
import com.mira.memory.model.MemoryFactEntity

@Composable
fun MemoriesScreen(
    state: MiraUiState,
    onAddMemory: (String, MemoryCategory, String, String) -> Unit,
    onUpdateMemory: (MemoryFactEntity) -> Unit,
    onDeleteMemory: (Long) -> Unit,
    onClearConversation: (String) -> Unit
) {
    val strings = state.strings
    val isBn = state.preferences.uiLanguage == AppLanguage.BN.code

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<MemoryCategory?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingMemory by remember { mutableStateOf<MemoryFactEntity?>(null) }
    var confirmClearChat by remember { mutableStateOf(false) }

    val filtered = remember(state.memories, searchQuery, selectedCategory) {
        state.memories.filter { mem ->
            val matchesCat = selectedCategory == null || mem.category == selectedCategory?.name
            val matchesQuery = searchQuery.isBlank() ||
                mem.factKey.contains(searchQuery, ignoreCase = true) ||
                mem.factValue.contains(searchQuery, ignoreCase = true)
            matchesCat && matchesQuery
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("add_memory_fab")
            ) {
                Icon(Icons.Rounded.Add, contentDescription = strings.addMemoryBtn)
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(strings.searchMemoriesHint) },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { selectedCategory = null },
                            label = { Text(if (isBn) "সব" else "All") }
                        )
                    }
                    items(MemoryCategory.entries) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat.name.replace('_', ' ')) }
                        )
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = { confirmClearChat = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.DeleteSweep, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("${strings.clearChatHistoryBtn} (${state.activeCharacter.name})")
                }
            }

            if (filtered.isEmpty()) {
                item {
                    Text(
                        text = strings.emptyMemories,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(filtered, key = { it.id }) { mem ->
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = mem.factKey,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = mem.factValue,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = mem.category.replace('_', ' '),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { editingMemory = mem }) {
                                Icon(Icons.Rounded.Edit, contentDescription = "Edit memory")
                            }
                            IconButton(onClick = { onDeleteMemory(mem.id) }) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = strings.delete)
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(64.dp)) }
        }
    }

    if (showAddDialog) {
        MemoryEditorDialog(
            isBn = isBn,
            initialKey = "",
            initialValue = "",
            initialCategory = MemoryCategory.PREFERENCE,
            onDismiss = { showAddDialog = false },
            onConfirm = { cat, key, value ->
                onAddMemory(state.activeCharacter.id, cat, key, value)
                showAddDialog = false
            }
        )
    }

    editingMemory?.let { target ->
        MemoryEditorDialog(
            isBn = isBn,
            initialKey = target.factKey,
            initialValue = target.factValue,
            initialCategory = runCatching { MemoryCategory.valueOf(target.category) }
                .getOrDefault(MemoryCategory.PERSONAL_DETAIL),
            onDismiss = { editingMemory = null },
            onConfirm = { cat, key, value ->
                onUpdateMemory(target.copy(category = cat.name, factKey = key, factValue = value))
                editingMemory = null
            }
        )
    }

    if (confirmClearChat) {
        AlertDialog(
            onDismissRequest = { confirmClearChat = false },
            title = { Text(strings.clearChatHistoryBtn) },
            text = {
                Text(
                    if (isBn) "${state.activeCharacter.name}-এর সাথে সব চ্যাট মুছে ফেলতে চাও?"
                    else "Permanently delete all local chat messages with ${state.activeCharacter.name}?"
                )
            },
            confirmButton = {
                Button(onClick = {
                    onClearConversation(state.activeCharacter.id)
                    confirmClearChat = false
                }) {
                    Text(strings.delete)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClearChat = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}

@Composable
private fun MemoryEditorDialog(
    isBn: Boolean,
    initialKey: String,
    initialValue: String,
    initialCategory: MemoryCategory,
    onDismiss: () -> Unit,
    onConfirm: (MemoryCategory, String, String) -> Unit
) {
    var key by remember { mutableStateOf(initialKey) }
    var value by remember { mutableStateOf(initialValue) }
    var category by remember { mutableStateOf(initialCategory) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isBn) "লোকাল স্মৃতি" else "Local Memory Entry") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it },
                    label = { Text(if (isBn) "বিষয় (যেমন: প্রিয় গান)" else "Topic / Key") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text(if (isBn) "তথ্য" else "Memory Detail") },
                    maxLines = 3
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(MemoryCategory.entries) { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat.name.take(10)) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(category, key, value) },
                enabled = key.isNotBlank() && value.isNotBlank()
            ) {
                Text(if (isBn) "সংরক্ষণ" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isBn) "বাতিল" else "Cancel")
            }
        }
    )
}
