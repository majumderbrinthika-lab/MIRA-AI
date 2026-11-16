package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.mira.ai.gguf.GgufFileInspector
import com.mira.ai.runtime.OfflineContextualEngine
import com.mira.character.CharacterSafetyValidator
import com.mira.character.MiraDefaultCharacters
import com.mira.memory.nlp.MemoryExtractor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `app name is MIRA`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("MIRA", context.getString(R.string.app_name))
    }

    @Test
    fun `memory extractor parses English and Bengali facts offline`() {
        val enFacts = MemoryExtractor.extractFacts("My name is Arka and I love Rabindra Sangeet", "mira_permanent")
        assertTrue(enFacts.any { it.factKey == "user_name" && it.factValue == "Arka" })
        assertTrue(enFacts.any { it.factValue.contains("Rabindra Sangeet") })

        val bnFacts = MemoryExtractor.extractFacts("আমার নাম অনির্বাণ", "mira_permanent")
        assertTrue(bnFacts.any { it.factKey == "user_name" && it.factValue == "অনির্বাণ" })
    }

    @Test
    fun `offline contextual engine generates grounded Bengali reply`() {
        val engine = OfflineContextualEngine()
        val mira = MiraDefaultCharacters.mira()
        val memories = MemoryExtractor.extractFacts("My name is Arka", mira.id)
        val reply = engine.generateReply(
            userMessage = "কেমন আছো মিরা?",
            character = mira,
            preferredLanguage = "bn",
            relevantMemories = memories,
            recentMessages = emptyList()
        )
        assertEquals("bn", reply.languageCode)
        assertTrue(reply.text.contains("Arka"))
    }

    @Test
    fun `gguf inspector validates binary magic header and rejects invalid files`() {
        val invalid = GgufFileInspector.inspectStream(
            ByteArrayInputStream("NOT_GGUF".toByteArray()),
            "fake.gguf",
            100L
        )
        assertFalse(invalid.isValidGguf)

        // Build minimal valid GGUF v3 header (magic, version=3, tensors=1, kv=0)
        val header = ByteBuffer.allocate(24).order(ByteOrder.LITTLE_ENDIAN)
            .putInt(0x46554747)
            .putInt(3)
            .putLong(1L)
            .putLong(0L)
            .array()
        val valid = GgufFileInspector.inspectStream(
            ByteArrayInputStream(header),
            "Qwen2.5-0.5B-Instruct-Q4_K_M.gguf",
            400L * 1024L * 1024L
        )
        assertTrue(valid.isValidGguf)
        assertEquals("Q4_K_M", valid.quantizationLabel)
        assertTrue(valid.isSafeFor3GbDevice)
    }

    @Test
    fun `character safety validator rejects non-compliant prompts`() {
        assertTrue(CharacterSafetyValidator.validateAdultFictionalInput("Adult Bengali poet").isSuccess)
        assertTrue(CharacterSafetyValidator.validateAdultFictionalInput("nude celebrity").isFailure)
    }
}
