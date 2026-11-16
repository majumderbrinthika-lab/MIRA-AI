package com.mira.ai

import android.app.ActivityManager
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.mira.ai.gguf.GgufFileInspector
import com.mira.ai.models.RecommendedGgufModel
import com.mira.ai.runtime.ChatGenerationResult
import com.mira.ai.runtime.NativeLlamaBridge
import com.mira.ai.runtime.OfflineContextualEngine
import com.mira.character.PersonaPromptBuilder
import com.mira.memory.model.CharacterProfileEntity
import com.mira.memory.model.ChatMessageEntity
import com.mira.memory.model.LocalModelEntity
import com.mira.memory.model.MemoryFactEntity
import com.mira.memory.repository.MemoryRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.concurrent.TimeUnit

sealed class ChatEngineStatus {
    data object Unloaded : ChatEngineStatus()
    data class Loading(val modelName: String) : ChatEngineStatus()
    data class Ready(
        val backendName: String,
        val modelName: String,
        val isNativeGgufActive: Boolean,
        val estimatedRamMb: Int
    ) : ChatEngineStatus()
    data class Error(val message: String) : ChatEngineStatus()
}

data class ModelDownloadState(
    val modelId: String? = null,
    val progressPercent: Int = 0,
    val downloadedMb: Int = 0,
    val totalMb: Int = 0,
    val isDownloading: Boolean = false,
    val error: String? = null
)

class LocalAiOrchestrator(
    private val context: Context,
    private val repository: MemoryRepository
) {
    private val modelsDir: File by lazy {
        File(context.filesDir, "models").apply { mkdirs() }
    }

    private val contextualEngine = OfflineContextualEngine()

    private val _status = MutableStateFlow<ChatEngineStatus>(
        ChatEngineStatus.Ready(
            backendName = "On-Device Persona Engine",
            modelName = "MIRA Compact NLP (Zero-RAM)",
            isNativeGgufActive = false,
            estimatedRamMb = 12
        )
    )
    val status: StateFlow<ChatEngineStatus> = _status.asStateFlow()

    private val _downloadState = MutableStateFlow(ModelDownloadState())
    val downloadState: StateFlow<ModelDownloadState> = _downloadState.asStateFlow()

    private var downloadJob: Job? = null

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    fun isNativeGgufRuntimePackaged(): Boolean = NativeLlamaBridge.isNativeLibraryAvailable()

    suspend fun generateReply(
        userMessage: String,
        character: CharacterProfileEntity,
        preferredLanguage: String,
        relevantMemories: List<MemoryFactEntity>,
        recentMessages: List<ChatMessageEntity>
    ): ChatGenerationResult = withContext(Dispatchers.Default) {
        val loadedModel = repository.getLoadedModelOnce()
        if (loadedModel != null && NativeLlamaBridge.isModelLoadedInNative()) {
            val sysPrompt = PersonaPromptBuilder.buildSystemPrompt(character, relevantMemories, preferredLanguage)
            val historySnippet = recentMessages.takeLast(4).joinToString("\n") {
                val who = if (it.isUser) "User" else character.name
                "$who: ${it.content}"
            }
            val fullPrompt = "<|system|>\n$sysPrompt\n$historySnippet\n<|user|>\n$userMessage\n<|assistant|>\n"
            val nativeReply = NativeLlamaBridge.generate(fullPrompt, maxTokens = 160, temperature = 0.72f)
            if (!nativeReply.isNullOrBlank()) {
                return@withContext ChatGenerationResult(
                    text = nativeReply.trim(),
                    languageCode = preferredLanguage,
                    moodTag = "GGUF Local",
                    engineLabel = "GGUF (${loadedModel.displayName})"
                )
            }
        }

        // Honest fallback: pure-Kotlin offline contextual persona engine (<12MB RAM)
        contextualEngine.generateReply(
            userMessage = userMessage,
            character = character,
            preferredLanguage = preferredLanguage,
            relevantMemories = relevantMemories,
            recentMessages = recentMessages
        )
    }

    suspend fun importGgufFromUri(uri: Uri): Result<LocalModelEntity> = withContext(Dispatchers.IO) {
        try {
            val fileName = resolveFileName(uri) ?: "imported_model_${System.currentTimeMillis()}.gguf"
            if (!fileName.endsWith(".gguf", ignoreCase = true)) {
                return@withContext Result.failure(IllegalArgumentException("Please select a .gguf model file."))
            }

            val destFile = File(modelsDir, fileName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                    }
                }
            } ?: return@withContext Result.failure(IllegalStateException("Unable to open selected file stream."))

            val inspection = GgufFileInspector.inspectFile(destFile)
            if (!inspection.isValidGguf) {
                destFile.delete()
                return@withContext Result.failure(
                    IllegalArgumentException(inspection.errorReason ?: "Invalid GGUF binary file.")
                )
            }

            val entity = LocalModelEntity(
                id = "gguf_${UUID.randomUUID().toString().take(8)}",
                displayName = inspection.modelName,
                family = inspection.architecture,
                quantization = inspection.quantizationLabel,
                fileName = destFile.name,
                filePath = destFile.absolutePath,
                fileSizeBytes = inspection.fileSizeBytes,
                estimatedRamMb = inspection.estimatedRamMb,
                architecture = inspection.architecture,
                contextLength = inspection.contextLength,
                isDownloaded = false,
                isImported = true,
                isLoaded = false
            )
            repository.upsertLocalModel(entity)
            Result.success(entity)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Downloads a user-approved GGUF model only when `strictOfflineLock == false` and the user taps Download.
     */
    suspend fun downloadApprovedModel(
        model: RecommendedGgufModel,
        strictOfflineLock: Boolean
    ): Result<LocalModelEntity> = withContext(Dispatchers.IO) {
        if (strictOfflineLock) {
            return@withContext Result.failure(
                IllegalStateException("Strict Offline Lock is enabled in Settings. Disable it temporarily if you wish to download a model.")
            )
        }
        val destFile = File(modelsDir, model.fileName)
        try {
            _downloadState.value = ModelDownloadState(
                modelId = model.id,
                progressPercent = 0,
                downloadedMb = 0,
                totalMb = model.approxSizeMb,
                isDownloading = true,
                error = null
            )

            val request = Request.Builder().url(model.downloadUrl).build()
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IllegalStateException("Server returned HTTP ${response.code}")
                }
                val body = response.body ?: throw IllegalStateException("Empty download response")
                val totalBytes = body.contentLength().takeIf { it > 0 } ?: (model.approxSizeMb * 1024L * 1024L)
                var downloaded = 0L

                body.byteStream().use { input ->
                    FileOutputStream(destFile).use { output ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            val read = input.read(buffer)
                            if (read < 0) break
                            output.write(buffer, 0, read)
                            downloaded += read
                            val pct = ((downloaded * 100) / totalBytes).toInt().coerceIn(0, 100)
                            _downloadState.value = ModelDownloadState(
                                modelId = model.id,
                                progressPercent = pct,
                                downloadedMb = (downloaded / (1024 * 1024)).toInt(),
                                totalMb = (totalBytes / (1024 * 1024)).toInt(),
                                isDownloading = true
                            )
                        }
                    }
                }
            }

            val inspection = GgufFileInspector.inspectFile(destFile)
            val entity = LocalModelEntity(
                id = model.id,
                displayName = model.displayName,
                family = model.family,
                quantization = model.quantization,
                fileName = destFile.name,
                filePath = destFile.absolutePath,
                fileSizeBytes = destFile.length(),
                estimatedRamMb = if (inspection.isValidGguf) inspection.estimatedRamMb else model.estimatedRamMb,
                architecture = if (inspection.isValidGguf) inspection.architecture else model.family,
                contextLength = if (inspection.isValidGguf) inspection.contextLength else 2048,
                isDownloaded = true,
                isImported = false,
                isLoaded = false
            )
            repository.upsertLocalModel(entity)
            _downloadState.value = ModelDownloadState(isDownloading = false)
            Result.success(entity)
        } catch (ce: CancellationException) {
            destFile.delete()
            _downloadState.value = ModelDownloadState(isDownloading = false)
            Result.failure(ce)
        } catch (e: Exception) {
            destFile.delete()
            _downloadState.value = ModelDownloadState(isDownloading = false, error = e.message)
            Result.failure(e)
        }
    }

    fun cancelDownload() {
        downloadJob?.cancel()
        _downloadState.value = ModelDownloadState(isDownloading = false)
    }

    suspend fun loadModel(model: LocalModelEntity): Result<Unit> = withContext(Dispatchers.IO) {
        val availMb = availableDeviceRamMb()
        if (availMb < model.estimatedRamMb) {
            val msg = "Insufficient free RAM (${availMb}MB free, needs ~${model.estimatedRamMb}MB). Unload Image Studio or close background apps."
            _status.value = ChatEngineStatus.Error(msg)
            return@withContext Result.failure(IllegalStateException(msg))
        }

        _status.value = ChatEngineStatus.Loading(model.displayName)
        if (!NativeLlamaBridge.isNativeLibraryAvailable()) {
            // Do NOT pretend GGUF weights are running if native libmira_llama.so is absent!
            repository.setLoadedModel(model.id)
            _status.value = ChatEngineStatus.Ready(
                backendName = "GGUF Verified (Native C++ .so not bundled — using Persona Engine)",
                modelName = model.displayName,
                isNativeGgufActive = false,
                estimatedRamMb = 12
            )
            return@withContext Result.success(Unit)
        }

        val ok = NativeLlamaBridge.loadModel(model.filePath, contextSize = model.contextLength.coerceAtMost(2048))
        if (ok) {
            repository.setLoadedModel(model.id)
            _status.value = ChatEngineStatus.Ready(
                backendName = "GGUF Native Runtime (llama.cpp)",
                modelName = model.displayName,
                isNativeGgufActive = true,
                estimatedRamMb = model.estimatedRamMb
            )
            Result.success(Unit)
        } else {
            val msg = "Failed to initialize GGUF context on this device."
            _status.value = ChatEngineStatus.Error(msg)
            Result.failure(IllegalStateException(msg))
        }
    }

    suspend fun unloadActiveModel() = withContext(Dispatchers.IO) {
        NativeLlamaBridge.unloadModel()
        repository.unloadAllModels()
        _status.value = ChatEngineStatus.Ready(
            backendName = "On-Device Persona Engine",
            modelName = "MIRA Compact NLP (Zero-RAM)",
            isNativeGgufActive = false,
            estimatedRamMb = 12
        )
    }

    suspend fun deleteModelFile(model: LocalModelEntity) = withContext(Dispatchers.IO) {
        if (model.isLoaded) {
            unloadActiveModel()
        }
        File(model.filePath).delete()
        repository.deleteLocalModel(model.id)
    }

    private fun availableDeviceRamMb(): Int {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return 1024
        val memInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memInfo)
        return (memInfo.availMem / (1024 * 1024)).toInt()
    }

    private fun resolveFileName(uri: Uri): String? {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && cursor.moveToFirst()) {
                return cursor.getString(idx)
            }
        }
        return uri.lastPathSegment?.substringAfterLast('/')
    }
}
