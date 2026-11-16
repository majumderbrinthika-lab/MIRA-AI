package com.mira.character

import com.mira.memory.model.CharacterProfileEntity
import com.mira.memory.model.MemoryCategory
import com.mira.memory.model.MemoryFactEntity
import com.mira.memory.repository.MemoryRepository
import java.util.UUID

class CharacterManager(private val repository: MemoryRepository) {

    suspend fun ensureSeeded() {
        if (repository.countCharacters() == 0) {
            val mira = MiraDefaultCharacters.mira()
            repository.upsertCharacter(mira)
            repository.upsertCharacter(MiraDefaultCharacters.starterAnanya())
            repository.upsertCharacter(MiraDefaultCharacters.starterKavya())

            repository.upsertMemory(
                MemoryFactEntity(
                    characterId = mira.id,
                    category = MemoryCategory.CONVERSATION_NOTE.name,
                    factKey = "mira_promise",
                    factValue = "Prefers warm Bengali terrace talks, evening tea, and remembering your small joys.",
                    importance = 5
                )
            )
            repository.upsertMemory(
                MemoryFactEntity(
                    characterId = mira.id,
                    category = MemoryCategory.PREFERENCE.name,
                    factKey = "preferred_language",
                    factValue = "Bengali (বাংলা) & English",
                    importance = 4
                )
            )
        } else if (repository.getCharacterOnce(MiraDefaultCharacters.MIRA_ID) == null) {
            repository.upsertCharacter(MiraDefaultCharacters.mira())
        }
    }

    suspend fun createAdultCharacter(
        name: String,
        nativeName: String,
        tagline: String,
        backstory: String,
        appearanceDescription: String,
        preferredLanguageCode: String,
        traits: PersonalityTraits,
        referencePhotoUri: String?
    ): Result<CharacterProfileEntity> {
        val combined = "$name $tagline $backstory $appearanceDescription"
        CharacterSafetyValidator.validateAdultFictionalInput(combined).onFailure {
            return Result.failure(it)
        }

        val cleanName = name.trim().ifBlank { "Rhea" }
        val character = CharacterProfileEntity(
            id = "char_${UUID.randomUUID().toString().take(8)}",
            name = cleanName,
            nativeName = nativeName.trim().ifBlank { cleanName },
            isPermanentMira = false,
            tagline = tagline.trim().ifBlank { "Warm fictional companion" },
            backstory = backstory.trim().ifBlank { "A caring adult fictional friend ready to talk anytime offline." },
            appearanceDescription = appearanceDescription.trim().ifBlank { "Warm portrait in South Asian contemporary attire." },
            preferredLanguageCode = preferredLanguageCode,
            referencePhotoUri = referencePhotoUri,
            presetDrawableKey = "kavya",
            romantic = traits.romantic,
            caring = traits.caring,
            loving = traits.loving,
            playful = traits.playful,
            funny = traits.funny,
            flirty = traits.flirty,
            shy = traits.shy,
            emotional = traits.emotional,
            teasing = traits.teasing,
            supportive = traits.supportive,
            voiceLocaleTag = when (preferredLanguageCode) {
                "bn" -> "bn-BD"
                "hi" -> "hi-IN"
                else -> "en-IN"
            }
        )
        repository.upsertCharacter(character)
        return Result.success(character)
    }

    suspend fun updatePersonalityTraits(characterId: String, traits: PersonalityTraits) {
        val existing = repository.getCharacterOnce(characterId) ?: return
        repository.updateCharacter(
            existing.copy(
                romantic = traits.romantic,
                caring = traits.caring,
                loving = traits.loving,
                playful = traits.playful,
                funny = traits.funny,
                flirty = traits.flirty,
                shy = traits.shy,
                emotional = traits.emotional,
                teasing = traits.teasing,
                supportive = traits.supportive
            )
        )
    }

    suspend fun updateReferencePhoto(characterId: String, uriString: String?) {
        val existing = repository.getCharacterOnce(characterId) ?: return
        repository.updateCharacter(existing.copy(referencePhotoUri = uriString))
    }

    suspend fun deleteCharacter(characterId: String): Boolean {
        if (characterId == MiraDefaultCharacters.MIRA_ID) return false
        repository.deleteNonPermanentCharacter(characterId)
        return true
    }
}
