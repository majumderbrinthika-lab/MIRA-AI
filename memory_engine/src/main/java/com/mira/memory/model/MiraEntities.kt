package com.mira.memory.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
enum class MemoryCategory {
    PREFERENCE,
    FAVORITE_TOPIC,
    PERSONAL_DETAIL,
    CONVERSATION_NOTE
}

@Entity(tableName = "characters")
data class CharacterProfileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val nativeName: String,
    val isPermanentMira: Boolean = false,
    val tagline: String,
    val backstory: String,
    val appearanceDescription: String,
    val preferredLanguageCode: String = "bn",
    val referencePhotoUri: String? = null,
    val presetDrawableKey: String = "mira",
    // 10 Personality Traits (0.0f .. 1.0f)
    val romantic: Float = 0.85f,
    val caring: Float = 0.92f,
    val loving: Float = 0.90f,
    val playful: Float = 0.72f,
    val funny: Float = 0.65f,
    val flirty: Float = 0.68f,
    val shy: Float = 0.35f,
    val emotional: Float = 0.70f,
    val teasing: Float = 0.58f,
    val supportive: Float = 0.95f,
    // Female voice preferences
    val voiceLocaleTag: String = "bn-BD",
    val voiceSpeed: Float = 0.95f,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val characterId: String,
    val isUser: Boolean,
    val content: String,
    val languageCode: String = "bn",
    val moodTag: String = "Warm",
    val engineUsed: String = "Offline Persona",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "memory_facts")
data class MemoryFactEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val characterId: String,
    val category: String = MemoryCategory.PERSONAL_DETAIL.name,
    val factKey: String,
    val factValue: String,
    val importance: Int = 3,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "studio_images")
data class StudioImageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val characterId: String,
    val characterName: String,
    val outfit: String,
    val hairstyle: String,
    val background: String,
    val pose: String,
    val artStyle: String,
    val resolutionLabel: String,
    val filePath: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "local_models")
data class LocalModelEntity(
    @PrimaryKey val id: String,
    val displayName: String,
    val family: String,
    val quantization: String,
    val fileName: String,
    val filePath: String,
    val fileSizeBytes: Long,
    val estimatedRamMb: Int,
    val architecture: String,
    val contextLength: Int,
    val isDownloaded: Boolean = false,
    val isImported: Boolean = false,
    val isLoaded: Boolean = false,
    val addedAt: Long = System.currentTimeMillis()
)
