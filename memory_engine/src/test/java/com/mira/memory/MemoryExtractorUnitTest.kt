package com.mira.memory

import com.mira.memory.nlp.MemoryExtractor
import org.junit.Assert.assertTrue
import org.junit.Test

class MemoryExtractorUnitTest {
    @Test
    fun extractsEnglishAndBengaliPreferences() {
        val facts = MemoryExtractor.extractFacts(
            "My name is Sayan and I love rainy Kolkata evenings",
            "mira_permanent"
        )
        assertTrue(facts.any { it.factKey == "user_name" && it.factValue == "Sayan" })
        assertTrue(facts.any { it.factValue.contains("rainy Kolkata evenings") })
    }
}
