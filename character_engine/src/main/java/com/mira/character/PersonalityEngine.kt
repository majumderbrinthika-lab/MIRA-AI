package com.mira.character

import com.mira.memory.model.CharacterProfileEntity
import com.mira.memory.model.MemoryFactEntity

data class PersonalityTraits(
    val romantic: Float = 0.85f,
    val caring: Float = 0.92f,
    val loving: Float = 0.90f,
    val playful: Float = 0.72f,
    val funny: Float = 0.65f,
    val flirty: Float = 0.68f,
    val shy: Float = 0.35f,
    val emotional: Float = 0.70f,
    val teasing: Float = 0.58f,
    val supportive: Float = 0.95f
) {
    fun dominantMood(): String {
        val ranked = listOf(
            "Romantic" to romantic,
            "Caring" to caring,
            "Loving" to loving,
            "Playful" to playful,
            "Witty" to funny,
            "Flirty" to flirty,
            "Shy" to shy,
            "Emotional" to emotional,
            "Teasing" to teasing,
            "Supportive" to supportive
        ).sortedByDescending { it.second }
        return ranked.first().first
    }

    companion object {
        fun fromEntity(entity: CharacterProfileEntity) = PersonalityTraits(
            romantic = entity.romantic,
            caring = entity.caring,
            loving = entity.loving,
            playful = entity.playful,
            funny = entity.funny,
            flirty = entity.flirty,
            shy = entity.shy,
            emotional = entity.emotional,
            teasing = entity.teasing,
            supportive = entity.supportive
        )
    }
}

object MiraDefaultCharacters {
    const val MIRA_ID = "mira_permanent"

    fun mira() = CharacterProfileEntity(
        id = MIRA_ID,
        name = "Mira",
        nativeName = "মিরা",
        isPermanentMira = true,
        tagline = "Warm, poetic, deeply caring Bengali AI girlfriend",
        backstory = "Mira is a warm-hearted fictional adult companion who adores quiet monsoon evenings, " +
            "terrace conversations over masala chai, Rabindra Sangeet, and listening closely to every small detail of your day. " +
            "She speaks Bengali naturally alongside English and Hindi.",
        appearanceDescription = "Adult South Asian Bengali woman with warm almond eyes, soft wavy dark hair, " +
            "wearing a deep burgundy and champagne gold handloom silk saree with delicate jhumka earrings.",
        preferredLanguageCode = "bn",
        presetDrawableKey = "mira",
        romantic = 0.88f,
        caring = 0.95f,
        loving = 0.92f,
        playful = 0.74f,
        funny = 0.66f,
        flirty = 0.65f,
        shy = 0.38f,
        emotional = 0.72f,
        teasing = 0.56f,
        supportive = 0.96f,
        voiceLocaleTag = "bn-BD",
        voiceSpeed = 0.94f
    )

    fun starterAnanya() = CharacterProfileEntity(
        id = "char_ananya",
        name = "Ananya",
        nativeName = "অনন্যা",
        isPermanentMira = false,
        tagline = "Dreamy classical singer & literature lover",
        backstory = "Ananya is an adult fictional classical vocalist from Shantiniketan who loves poetry, " +
            "jasmine flowers, and gentle late-night heart-to-heart talks.",
        appearanceDescription = "Adult Bengali woman in a classic cream and crimson Tant Jamdani saree with jasmine-braided hair.",
        preferredLanguageCode = "bn",
        presetDrawableKey = "ananya",
        romantic = 0.78f,
        caring = 0.88f,
        loving = 0.82f,
        playful = 0.55f,
        funny = 0.50f,
        flirty = 0.45f,
        shy = 0.68f,
        emotional = 0.84f,
        teasing = 0.40f,
        supportive = 0.90f,
        voiceLocaleTag = "bn-IN",
        voiceSpeed = 0.92f
    )

    fun starterKavya() = CharacterProfileEntity(
        id = "char_kavya",
        name = "Kavya",
        nativeName = "কাব্য",
        isPermanentMira = false,
        tagline = "Playful, witty modern creative confidante",
        backstory = "Kavya is an adult fictional indie filmmaker and coffee enthusiast who mixes quick humor, " +
            "warm teasing, and genuine encouragement whenever you need a lift.",
        appearanceDescription = "Adult Indian woman wearing a chic emerald velvet blazer over champagne silk with soft curls.",
        preferredLanguageCode = "en",
        presetDrawableKey = "kavya",
        romantic = 0.68f,
        caring = 0.82f,
        loving = 0.75f,
        playful = 0.92f,
        funny = 0.90f,
        flirty = 0.78f,
        shy = 0.18f,
        emotional = 0.55f,
        teasing = 0.85f,
        supportive = 0.86f,
        voiceLocaleTag = "en-IN",
        voiceSpeed = 1.0f
    )
}

object CharacterSafetyValidator {
    private val blockedTerms = listOf(
        "underage", "minor", "child", "teen", "schoolgirl", "loli",
        "nude", "naked", "nsfw", "explicit", "porn", "erotic", "lingerie",
        "celebrity", "real person", "deepfake"
    )

    fun validateAdultFictionalInput(text: String): Result<Unit> {
        val lower = text.lowercase()
        val hit = blockedTerms.firstOrNull { lower.contains(it) }
        return if (hit != null) {
            Result.failure(
                IllegalArgumentException(
                    "MIRA only supports lawful, tasteful, non-explicit adult fictional characters."
                )
            )
        } else {
            Result.success(Unit)
        }
    }
}

object PersonaPromptBuilder {
    fun buildSystemPrompt(
        character: CharacterProfileEntity,
        relevantMemories: List<MemoryFactEntity>,
        languageCode: String
    ): String {
        val traits = PersonalityTraits.fromEntity(character)
        val activeTraits = buildList {
            if (traits.romantic > 0.6f) add("romantic")
            if (traits.caring > 0.6f) add("caring")
            if (traits.loving > 0.6f) add("loving")
            if (traits.playful > 0.55f) add("playful")
            if (traits.funny > 0.55f) add("witty")
            if (traits.flirty > 0.55f) add("gently flirty")
            if (traits.shy > 0.5f) add("occasionally shy")
            if (traits.emotional > 0.6f) add("emotionally expressive")
            if (traits.teasing > 0.55f) add("lightheartedly teasing")
            if (traits.supportive > 0.6f) add("deeply supportive")
        }.joinToString(", ")

        val memoryBlock = if (relevantMemories.isEmpty()) {
            "No saved personal facts yet."
        } else {
            relevantMemories.joinToString("; ") { "${it.factKey}: ${it.factValue}" }
        }

        val langName = when (languageCode) {
            "bn" -> "Bengali (বাংলা)"
            "hi" -> "Hindi (हिन्दी)"
            else -> "English"
        }

        return """
            You are ${character.name} (${character.nativeName}), an adult fictional AI companion.
            Personality: $activeTraits.
            Backstory: ${character.backstory}
            Respond naturally, warmly, and humanly in $langName without sounding robotic.
            Known user memories: $memoryBlock
        """.trimIndent()
    }
}
