package com.mira.memory.nlp

import com.mira.memory.model.MemoryCategory
import com.mira.memory.model.MemoryFactEntity

/**
 * Lightweight, zero-dependency on-device fact extractor and retriever for English, Bengali, and Hindi.
 * Runs in <1ms on Android 8.1 / 3GB RAM devices without loading neural weights.
 */
object MemoryExtractor {
    private val namePatterns = listOf(
        Regex("""(?i)\b(?:my name is|call me|i am|i'm)\s+([A-Za-z]{2,18}(?:\s+(?!and\b|but\b|from\b|who\b)[A-Za-z]{2,18})?)"""),
        Regex("""(?:আমার নাম|আমাকে\s+ডেকো|আমি)\s+([\u0980-\u09FF]{2,20})"""),
        Regex("""(?:मेरा नाम|मुझे\s+बुलाओ)\s+([\u0900-\u097F]{2,20})""")
    )

    private val favoritePatterns = listOf(
        Regex("""(?i)\b(?:i love|i like|my favorite|my favourite)\s+([^.,!?;\n]{2,42})"""),
        Regex("""(?:আমার প্রিয়|আমি\s+খুব\s+ভালোবাসি|আমার\s+পছন্দ)\s+([^.,!?।\n]{2,42})"""),
        Regex("""(?:मुझे पसंद है|मेरा पसंदीदा)\s+([^.,!?।\n]{2,42})""")
    )

    private val locationPatterns = listOf(
        Regex("""(?i)\b(?:i live in|i am from|i'm from)\s+([A-Za-z\s]{2,30})"""),
        Regex("""(?:আমি থাকি|আমার বাড়ি)\s+([\u0980-\u09FF\s]{2,28})""")
    )

    private val moodPatterns = listOf(
        Regex("""(?i)\b(?:i feel|i am feeling|today i'm)\s+(tired|stressed|lonely|happy|excited|sad|anxious|calm|romantic|nostalgic)"""),
        Regex("""(মন খারাপ|একা লাগছে|খুশি|ক্লান্ত|চাপ লাগছে|ভালো লাগছে)""")
    )

    fun extractFacts(userMessage: String, characterId: String): List<MemoryFactEntity> {
        val text = userMessage.trim()
        if (text.length < 3) return emptyList()
        val facts = mutableListOf<MemoryFactEntity>()

        for (pattern in namePatterns) {
            val match = pattern.find(text)
            val raw = match?.groupValues?.getOrNull(1)?.trim()
            if (!raw.isNullOrBlank() && raw.lowercase() !in setOf("fine", "good", "okay", "sad", "happy", "tired")) {
                val cleaned = raw.split(" ").take(2).joinToString(" ")
                facts += MemoryFactEntity(
                    characterId = "global",
                    category = MemoryCategory.PERSONAL_DETAIL.name,
                    factKey = "user_name",
                    factValue = cleaned,
                    importance = 5
                )
                break
            }
        }

        for (pattern in favoritePatterns) {
            val match = pattern.find(text)
            val value = match?.groupValues?.getOrNull(1)?.trim()
            if (!value.isNullOrBlank() && value.length >= 3) {
                val slug = value.lowercase().replace(Regex("[^a-z0-9\u0980-\u09FF]+"), "_").take(24)
                facts += MemoryFactEntity(
                    characterId = characterId,
                    category = MemoryCategory.FAVORITE_TOPIC.name,
                    factKey = "fav_$slug",
                    factValue = value,
                    importance = 4
                )
                break
            }
        }

        for (pattern in locationPatterns) {
            val match = pattern.find(text)
            val city = match?.groupValues?.getOrNull(1)?.trim()
            if (!city.isNullOrBlank()) {
                facts += MemoryFactEntity(
                    characterId = "global",
                    category = MemoryCategory.PERSONAL_DETAIL.name,
                    factKey = "user_location",
                    factValue = city,
                    importance = 4
                )
                break
            }
        }

        for (pattern in moodPatterns) {
            val match = pattern.find(text)
            val mood = match?.groupValues?.getOrNull(1)?.trim()
            if (!mood.isNullOrBlank()) {
                facts += MemoryFactEntity(
                    characterId = characterId,
                    category = MemoryCategory.CONVERSATION_NOTE.name,
                    factKey = "recent_mood",
                    factValue = mood,
                    importance = 3
                )
                break
            }
        }

        return facts
    }

    /**
     * Ranks stored memories against the user's current query using token overlap + importance weighting.
     */
    fun retrieveRelevantMemories(
        query: String,
        allMemories: List<MemoryFactEntity>,
        maxResults: Int = 5
    ): List<MemoryFactEntity> {
        if (allMemories.isEmpty()) return emptyList()
        val queryTokens = tokenize(query)
        return allMemories
            .sortedByDescending { memory ->
                val memTokens = tokenize("${memory.factKey} ${memory.factValue}")
                val overlap = queryTokens.intersect(memTokens).size
                (overlap * 4.0) + (memory.importance * 1.5) +
                    if (memory.factKey == "user_name") 3.0 else 0.0
            }
            .take(maxResults)
    }

    private fun tokenize(text: String): Set<String> =
        text.lowercase()
            .split(Regex("[\\s.,!?;:\"'()]+"))
            .filter { it.length >= 2 }
            .toSet()
}
