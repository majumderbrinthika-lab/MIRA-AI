package com.mira.ai

import com.mira.ai.gguf.GgufFileInspector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class GgufInspectorUnitTest {
    @Test
    fun parsesValidGgufV3Header() {
        val header = ByteBuffer.allocate(24).order(ByteOrder.LITTLE_ENDIAN)
            .putInt(0x46554747)
            .putInt(3)
            .putLong(2L)
            .putLong(0L)
            .array()
        val res = GgufFileInspector.inspectStream(
            ByteArrayInputStream(header),
            "SmolLM2-360M-Instruct-Q8_0.gguf",
            386L * 1024L * 1024L
        )
        assertTrue(res.isValidGguf)
        assertEquals("Q8_0", res.quantizationLabel)
        assertTrue(res.isSafeFor3GbDevice)
    }
}
