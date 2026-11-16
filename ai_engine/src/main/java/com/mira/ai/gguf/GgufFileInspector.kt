package com.mira.ai.gguf

import java.io.BufferedInputStream
import java.io.EOFException
import java.io.File
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class GgufInspectionResult(
    val isValidGguf: Boolean,
    val version: Int,
    val tensorCount: Long,
    val metadataCount: Long,
    val architecture: String,
    val modelName: String,
    val contextLength: Int,
    val quantizationLabel: String,
    val fileSizeBytes: Long,
    val estimatedRamMb: Int,
    val isSafeFor3GbDevice: Boolean,
    val errorReason: String? = null
)

/**
 * Genuine binary GGUF v2/v3 header & KV metadata parser.
 * Uses streaming 64-bit little-endian reads with strict string length bounds so inspecting
 * a 500MB+ model on a 3GB RAM Android 8.1 device uses < 2MB heap memory.
 */
object GgufFileInspector {
    private const val GGUF_MAGIC = 0x46554747 // "GGUF" little-endian

    fun inspectFile(file: File): GgufInspectionResult {
        if (!file.exists() || !file.isFile) {
            return invalidResult(file.name, 0L, "File does not exist")
        }
        return file.inputStream().buffered(64 * 1024).use { stream ->
            inspectStream(stream, file.name, file.length())
        }
    }

    fun inspectStream(input: InputStream, fileName: String, fileSizeBytes: Long): GgufInspectionResult {
        val buffered = if (input is BufferedInputStream) input else BufferedInputStream(input, 64 * 1024)
        return try {
            val magic = readIntLE(buffered)
            if (magic != GGUF_MAGIC) {
                return invalidResult(
                    fileName,
                    fileSizeBytes,
                    "Invalid GGUF magic header (expected GGUF). Only binary .gguf files are supported."
                )
            }
            val version = readIntLE(buffered)
            if (version !in 2..3) {
                return invalidResult(fileName, fileSizeBytes, "Unsupported GGUF version $version (expected v2 or v3).")
            }

            val tensorCount = readLongLE(buffered)
            val kvCount = readLongLE(buffered)

            var architecture = inferArchFromName(fileName)
            var modelName = fileName.substringBeforeLast(".")
            var contextLength = 2048

            val safeKvLimit = kvCount.coerceAtMost(96)
            for (i in 0 until safeKvLimit) {
                val key = readGgufString(buffered, maxLen = 256) ?: break
                val valueType = readIntLE(buffered)
                when (valueType) {
                    8 -> { // GGUF_TYPE_STRING
                        val value = readGgufString(buffered, maxLen = 512) ?: break
                        when (key) {
                            "general.architecture" -> architecture = value
                            "general.name" -> modelName = value
                        }
                    }
                    4 -> { // UINT32
                        val v = readIntLE(buffered)
                        if (key.endsWith(".context_length")) contextLength = v
                    }
                    5 -> { // INT32
                        val v = readIntLE(buffered)
                        if (key.endsWith(".context_length")) contextLength = v
                    }
                    else -> {
                        if (!skipValue(buffered, valueType)) break
                    }
                }
            }

            val quant = inferQuantizationFromName(fileName)
            val fileMb = (fileSizeBytes / (1024.0 * 1024.0)).toInt().coerceAtLeast(1)
            val kvOverheadMb = ((contextLength.coerceAtMost(4096) / 1024.0) * 65).toInt()
            val estimatedRamMb = fileMb + kvOverheadMb + 140
            val safeFor3Gb = estimatedRamMb <= 1450

            GgufInspectionResult(
                isValidGguf = true,
                version = version,
                tensorCount = tensorCount,
                metadataCount = kvCount,
                architecture = architecture,
                modelName = modelName,
                contextLength = contextLength,
                quantizationLabel = quant,
                fileSizeBytes = fileSizeBytes,
                estimatedRamMb = estimatedRamMb,
                isSafeFor3GbDevice = safeFor3Gb
            )
        } catch (e: EOFException) {
            invalidResult(fileName, fileSizeBytes, "Truncated GGUF file header")
        } catch (e: Exception) {
            invalidResult(fileName, fileSizeBytes, e.message ?: "Failed to parse GGUF header")
        }
    }

    private fun invalidResult(fileName: String, size: Long, reason: String) = GgufInspectionResult(
        isValidGguf = false,
        version = 0,
        tensorCount = 0L,
        metadataCount = 0L,
        architecture = "unknown",
        modelName = fileName,
        contextLength = 0,
        quantizationLabel = inferQuantizationFromName(fileName),
        fileSizeBytes = size,
        estimatedRamMb = 0,
        isSafeFor3GbDevice = false,
        errorReason = reason
    )

    private fun skipValue(stream: BufferedInputStream, type: Int): Boolean {
        return when (type) {
            0, 1, 7 -> skipFully(stream, 1) // UINT8, INT8, BOOL
            2, 3 -> skipFully(stream, 2) // UINT16, INT16
            4, 5, 6 -> skipFully(stream, 4) // UINT32, INT32, FLOAT32
            10, 11, 12 -> skipFully(stream, 8) // UINT64, INT64, FLOAT64
            9 -> { // ARRAY (skip small arrays, abort huge tokenizer arrays after reading metadata)
                val elemType = readIntLE(stream)
                val count = readLongLE(stream)
                if (count > 2048 || elemType == 8) {
                    false // Stop scanning before huge vocab arrays to keep inspection instant
                } else {
                    for (j in 0 until count) {
                        if (!skipValue(stream, elemType)) return false
                    }
                    true
                }
            }
            else -> false
        }
    }

    private fun skipFully(stream: BufferedInputStream, bytes: Long): Boolean {
        var remaining = bytes
        while (remaining > 0) {
            val skipped = stream.skip(remaining)
            if (skipped <= 0) {
                if (stream.read() == -1) return false
                remaining--
            } else {
                remaining -= skipped
            }
        }
        return true
    }

    private fun readGgufString(stream: BufferedInputStream, maxLen: Int): String? {
        val lenLong = readLongLE(stream)
        if (lenLong < 0 || lenLong > 65536) return null
        val len = lenLong.toInt()
        if (len > maxLen) {
            if (!skipFully(stream, len.toLong())) return null
            return ""
        }
        val buf = ByteArray(len)
        var read = 0
        while (read < len) {
            val n = stream.read(buf, read, len - read)
            if (n < 0) throw EOFException()
            read += n
        }
        return String(buf, Charsets.UTF_8)
    }

    private fun readIntLE(stream: BufferedInputStream): Int {
        val buf = ByteArray(4)
        readExact(stream, buf)
        return ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN).int
    }

    private fun readLongLE(stream: BufferedInputStream): Long {
        val buf = ByteArray(8)
        readExact(stream, buf)
        return ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN).long
    }

    private fun readExact(stream: BufferedInputStream, buf: ByteArray) {
        var read = 0
        while (read < buf.size) {
            val n = stream.read(buf, read, buf.size - read)
            if (n < 0) throw EOFException()
            read += n
        }
    }

    private fun inferArchFromName(name: String): String {
        val lower = name.lowercase()
        return when {
            "qwen" in lower -> "qwen2"
            "gemma" in lower -> "gemma2"
            "smollm" in lower -> "llama"
            "phi" in lower -> "phi3"
            else -> "llama"
        }
    }

    fun inferQuantizationFromName(name: String): String {
        val upper = name.uppercase()
        val known = listOf("Q4_K_M", "Q4_K_S", "Q3_K_S", "Q3_K_M", "Q2_K", "IQ2_XS", "Q4_0", "Q8_0", "F16")
        return known.firstOrNull { upper.contains(it) } ?: "GGUF"
    }
}
