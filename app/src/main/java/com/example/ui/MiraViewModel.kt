package com.example.ui

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.MiraApplication
import com.example.R
import com.example.ui.localization.AppLanguage
import com.example.ui.localization.AppStringsProvider
import com.example.ui.localization.MiraStrings
import com.mira.ai.ChatEngineStatus
import com.mira.ai.ModelDownloadState
import com.mira.ai.models.RecommendedGgufModel
import com.mira.character.MiraDefaultCharacters
import com.mira.character.PersonalityTraits
import com.mira.image.DeviceImageCapabilityReport
import com.mira.image.StudioArtStyle
import com.mira.image.StudioBackground
import com.mira.image.StudioGenerationRequest
import com.mira.image.StudioHairstyle
import com.mira.image.StudioOutfit
import com.mira.image.StudioPose
import com.mira.image.StudioResolution
import com.mira.memory.model.CharacterProfileEntity
import com.mira.memory.model.ChatMessageEntity
import com.mira.memory.model.LocalModelEntity
import com.mira.memory.model.MemoryCategory
import com.mira.memory.model.MemoryFactEntity
import com.mira.memory.model.StudioImageEntity
import com.mira.memory.nlp.MemoryExtractor
import com.mira.memory.preferences.UserPreferencesState
import com.mira.voice.FemaleVoiceOption
import com.mira.voice.SpeechListenState
import com.mira.voice.VoiceAvailabilityReport
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DeviceRamSnapshot(
    val totalMb: Int = 3072,
    val availableMb: Int = 1200,
    val appHeapUsedMb: Int = 48,
    val isLowMemory: Boolean = false
) {
    val usedPercent: Float
        get() = ((totalMb - availableMb).toFloat() / totalMb.toFloat().coerceAtLeast(1f)).coerceIn(0.05f, 0.98f)
}

data class MiraUiState(
    val preferences: UserPreferencesState = UserPreferencesState(),
    val strings: MiraStrings = AppStringsProvider.get(AppLanguage.EN),
    val characters: List<CharacterProfileEntity> = emptyList(),
    val activeCharacter: CharacterProfileEntity = MiraDefaultCharacters.mira(),
    val messages: List<ChatMessageEntity> = emptyList(),
    val memories: List<MemoryFactEntity> = emptyList(),
    val studioImages: List<StudioImageEntity> = emptyList(),
    val localModels: List<LocalModelEntity> = emptyList(),
    val engineStatus: ChatEngineStatus = ChatEngineStatus.Unloaded,
    val downloadState: ModelDownloadState = ModelDownloadState(),
    val voiceReport: VoiceAvailabilityReport = VoiceAvailabilityReport(),
    val femaleVoices: List<FemaleVoiceOption> = emptyList(),
    val isSpeaking: Boolean = false,
    val speechListenState: SpeechListenState = SpeechListenState.Idle,
    val micRmsLevel: Float = 0f,
    val imageCapability: DeviceImageCapabilityReport? = null,
    val ramSnapshot: DeviceRamSnapshot = DeviceRamSnapshot(),
    val isGeneratingReply: Boolean = false,
    val isComposingImage: Boolean = false,
    val isAppUnlocked: Boolean = true,
    val statusNotice: String? = null
)

class MiraViewModel(application: Application) : AndroidViewModel(application) {
    private val container = (application as MiraApplication).container
    private val memoryRepo = container.memoryRepository
    private val prefsRepo = container.preferencesRepository
    private val characterManager = container.characterManager
    private val aiOrchestrator = container.aiOrchestrator
    private val voiceEngine = container.voiceEngine
    private val speechRecognizer = container.speechRecognizer
    private val portraitComposer = container.portraitComposer

    private val _isGeneratingReply = MutableStateFlow(false)
    private val _isComposingImage = MutableStateFlow(false)
    private val _isAppUnlocked = MutableStateFlow(true)
    private val _statusNotice = MutableStateFlow<String?>(null)
    private val _ramSnapshot = MutableStateFlow(readDeviceRam())

    private var lockCheckedInitial = false

    @OptIn(ExperimentalCoroutinesApi::class)
    private val activeMessagesFlow = prefsRepo.preferencesFlow.flatMapLatest { prefs ->
        memoryRepo.observeMessages(prefs.activeCharacterId)
    }

    private val coreDataFlow = combine(
        prefsRepo.preferencesFlow,
        memoryRepo.observeAllCharacters(),
        activeMessagesFlow,
        memoryRepo.observeAllMemories(),
        memoryRepo.observeStudioImages()
    ) { prefs, characters, messages, memories, images ->
        if (!lockCheckedInitial) {
            lockCheckedInitial = true
            if (prefs.appLockEnabled && prefs.appLockPinHash.isNotBlank()) {
                _isAppUnlocked.value = false
            }
        }
        CoreDataBundle(prefs, characters, messages, memories, images)
    }

    private val engineStateFlow = combine(
        memoryRepo.observeLocalModels(),
        aiOrchestrator.status,
        aiOrchestrator.downloadState,
        voiceEngine.report,
        voiceEngine.femaleVoices
    ) { models, chatStatus, dlState, voiceRep, femaleVoices ->
        EngineBundle(models, chatStatus, dlState, voiceRep, femaleVoices)
    }

    private val liveFlagsFlow = combine(
        voiceEngine.isSpeaking,
        speechRecognizer.state,
        speechRecognizer.rmsLevel,
        _isGeneratingReply,
        _isComposingImage
    ) { speaking, sttState, rms, genReply, compImage ->
        LiveFlagsBundle(speaking, sttState, rms, genReply, compImage)
    }

    val uiState: StateFlow<MiraUiState> = combine(
        coreDataFlow,
        engineStateFlow,
        liveFlagsFlow,
        _isAppUnlocked,
        _statusNotice,
        _ramSnapshot
    ) { flows ->
        val core = flows[0] as CoreDataBundle
        val engine = flows[1] as EngineBundle
        val flags = flows[2] as LiveFlagsBundle
        val unlocked = flows[3] as Boolean
        val notice = flows[4] as String?
        val ram = flows[5] as DeviceRamSnapshot

        val activeChar = core.characters.firstOrNull { it.id == core.prefs.activeCharacterId }
            ?: core.characters.firstOrNull { it.isPermanentMira }
            ?: MiraDefaultCharacters.mira()

        val lang = AppLanguage.fromCode(core.prefs.uiLanguage)
        voiceEngine.updateTuning(
            speed = core.prefs.ttsSpeed,
            volume = core.prefs.ttsVolume,
            voiceName = core.prefs.selectedFemaleVoiceName
        )

        MiraUiState(
            preferences = core.prefs,
            strings = AppStringsProvider.get(lang),
            characters = core.characters,
            activeCharacter = activeChar,
            messages = core.messages,
            memories = core.memories,
            studioImages = core.images,
            localModels = engine.models,
            engineStatus = engine.chatStatus,
            downloadState = engine.dlState,
            voiceReport = engine.voiceRep,
            femaleVoices = engine.femaleVoices,
            isSpeaking = flags.speaking,
            speechListenState = flags.sttState,
            micRmsLevel = flags.rms,
            imageCapability = portraitComposer.inspectDeviceCapabilities(),
            ramSnapshot = ram,
            isGeneratingReply = flags.genReply,
            isComposingImage = flags.compImage,
            isAppUnlocked = unlocked,
            statusNotice = notice
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MiraUiState(imageCapability = portraitComposer.inspectDeviceCapabilities())
    )

    init {
        voiceEngine.initialize()
        observeVoiceTranscriptResults()
    }

    private fun observeVoiceTranscriptResults() {
        viewModelScope.launch {
            speechRecognizer.state.collect { stt ->
                if (stt is SpeechListenState.ResultReady) {
                    val spoken = stt.text
                    speechRecognizer.resetState()
                    sendMessage(spoken, speakReplyAfter = true)
                }
            }
        }
    }

    fun sendMessage(text: String, speakReplyAfter: Boolean = false) {
        val trimmed = text.trim()
        if (trimmed.isBlank() || _isGeneratingReply.value) return
        val current = uiState.value
        val activeChar = current.activeCharacter
        val chatLang = current.preferences.preferredChatLanguage

        viewModelScope.launch {
            _isGeneratingReply.value = true
            try {
                memoryRepo.addMessage(
                    ChatMessageEntity(
                        characterId = activeChar.id,
                        isUser = true,
                        content = trimmed,
                        languageCode = chatLang,
                        moodTag = "User"
                    )
                )

                val extracted = MemoryExtractor.extractFacts(trimmed, activeChar.id)
                extracted.forEach { fact -> memoryRepo.upsertMemory(fact) }
                if (extracted.isNotEmpty()) {
                    _statusNotice.value = "${current.strings.memorySavedHint}: ${extracted.first().factValue}"
                }

                val allMemories = memoryRepo.getMemoriesOnce(activeChar.id)
                val relevantMemories = MemoryExtractor.retrieveRelevantMemories(trimmed, allMemories)
                val recentHistory = memoryRepo.getRecentMessagesOnce(activeChar.id, limit = 12)

                val reply = aiOrchestrator.generateReply(
                    userMessage = trimmed,
                    character = activeChar,
                    preferredLanguage = chatLang,
                    relevantMemories = relevantMemories,
                    recentMessages = recentHistory
                )

                memoryRepo.addMessage(
                    ChatMessageEntity(
                        characterId = activeChar.id,
                        isUser = false,
                        content = reply.text,
                        languageCode = reply.languageCode,
                        moodTag = reply.moodTag,
                        engineUsed = reply.engineLabel
                    )
                )

                if (speakReplyAfter) {
                    speakText(reply.text, reply.languageCode)
                }
                refreshRamSnapshot()
            } finally {
                _isGeneratingReply.value = false
            }
        }
    }

    fun speakText(text: String, languageCode: String) {
        val res = voiceEngine.speakOfflineFemale(text, languageCode)
        res.onFailure { err ->
            _statusNotice.value = err.message
        }
    }

    fun stopSpeaking() = voiceEngine.stopSpeaking()

    fun startVoiceListening(languageCode: String) {
        voiceEngine.stopSpeaking()
        speechRecognizer.startListening(languageCode)
    }

    fun stopVoiceListening() = speechRecognizer.stopListening()

    fun refreshVoiceAudit() {
        voiceEngine.auditAvailableFemaleVoices()
    }

    fun buildInstallTtsDataIntent() = voiceEngine.buildInstallTtsDataIntent()

    fun updateVoiceTuning(speed: Float, volume: Float, voiceName: String? = null) {
        viewModelScope.launch {
            prefsRepo.setVoiceTuning(speed, volume, voiceName)
        }
    }

    fun setUiLanguage(lang: AppLanguage) {
        viewModelScope.launch { prefsRepo.setUiLanguage(lang.code) }
    }

    fun setDarkTheme(dark: Boolean) {
        viewModelScope.launch { prefsRepo.setDarkTheme(dark) }
    }

    fun setPreferredChatLanguage(langCode: String) {
        viewModelScope.launch { prefsRepo.setPreferredChatLanguage(langCode) }
    }

    fun selectActiveCharacter(characterId: String) {
        viewModelScope.launch { prefsRepo.setActiveCharacterId(characterId) }
    }

    fun updateCharacterTraits(characterId: String, traits: PersonalityTraits) {
        viewModelScope.launch {
            characterManager.updatePersonalityTraits(characterId, traits)
            _statusNotice.value = "Personality updated locally"
        }
    }

    fun updateCharacterReferencePhoto(characterId: String, uri: Uri?) {
        viewModelScope.launch {
            characterManager.updateReferencePhoto(characterId, uri?.toString())
        }
    }

    fun createNewCharacter(
        name: String,
        nativeName: String,
        tagline: String,
        backstory: String,
        appearance: String,
        preferredLang: String,
        traits: PersonalityTraits,
        referencePhotoUri: Uri?
    ) {
        viewModelScope.launch {
            val result = characterManager.createAdultCharacter(
                name = name,
                nativeName = nativeName,
                tagline = tagline,
                backstory = backstory,
                appearanceDescription = appearance,
                preferredLanguageCode = preferredLang,
                traits = traits,
                referencePhotoUri = referencePhotoUri?.toString()
            )
            result.onSuccess { created ->
                prefsRepo.setActiveCharacterId(created.id)
                _statusNotice.value = "${created.name} created offline!"
            }.onFailure { err ->
                _statusNotice.value = err.message
            }
        }
    }

    fun deleteCharacter(characterId: String) {
        viewModelScope.launch {
            val deleted = characterManager.deleteCharacter(characterId)
            if (deleted && uiState.value.preferences.activeCharacterId == characterId) {
                prefsRepo.setActiveCharacterId(MiraDefaultCharacters.MIRA_ID)
            }
        }
    }

    fun addManualMemory(characterId: String, category: MemoryCategory, key: String, value: String) {
        if (key.isBlank() || value.isBlank()) return
        viewModelScope.launch {
            memoryRepo.upsertMemory(
                MemoryFactEntity(
                    characterId = characterId,
                    category = category.name,
                    factKey = key.trim(),
                    factValue = value.trim(),
                    importance = 4
                )
            )
        }
    }

    fun updateMemory(memory: MemoryFactEntity) {
        viewModelScope.launch { memoryRepo.updateMemory(memory) }
    }

    fun deleteMemory(id: Long) {
        viewModelScope.launch { memoryRepo.deleteMemoryById(id) }
    }

    fun clearConversation(characterId: String) {
        viewModelScope.launch {
            memoryRepo.clearConversation(characterId)
            _statusNotice.value = "Conversation history cleared."
        }
    }

    fun composeStudioPortrait(
        character: CharacterProfileEntity,
        outfit: StudioOutfit,
        hairstyle: StudioHairstyle,
        background: StudioBackground,
        pose: StudioPose,
        artStyle: StudioArtStyle,
        resolution: StudioResolution,
        referencePhotoUri: Uri?,
        consentConfirmed: Boolean
    ) {
        if (_isComposingImage.value) return
        viewModelScope.launch {
            _isComposingImage.value = true
            try {
                // Mutual exclusion for 3GB RAM devices: unload LLM weights before bitmap pipeline
                aiOrchestrator.unloadActiveModel()
                val baseDrawable = when (character.presetDrawableKey) {
                    "ananya" -> R.drawable.img_char_ananya
                    "kavya" -> R.drawable.img_char_kavya
                    else -> R.drawable.img_mira_portrait
                }
                val req = StudioGenerationRequest(
                    characterId = character.id,
                    characterName = character.name,
                    baseDrawableResId = baseDrawable,
                    referencePhotoUri = referencePhotoUri,
                    outfit = outfit,
                    hairstyle = hairstyle,
                    background = background,
                    pose = pose,
                    artStyle = artStyle,
                    resolution = resolution,
                    adultFictionalConsentConfirmed = consentConfirmed
                )
                val result = portraitComposer.generatePortrait(req)
                result.onSuccess { saved ->
                    memoryRepo.saveStudioImage(saved)
                    _statusNotice.value = "Saved portrait (${saved.resolutionLabel}) locally."
                }.onFailure { err ->
                    _statusNotice.value = err.message
                }
                refreshRamSnapshot()
            } finally {
                _isComposingImage.value = false
            }
        }
    }

    fun deleteStudioImage(image: StudioImageEntity) {
        viewModelScope.launch {
            java.io.File(image.filePath).delete()
            memoryRepo.deleteStudioImage(image.id)
        }
    }

    fun setStudioImageAsCharacterPortrait(image: StudioImageEntity) {
        viewModelScope.launch {
            val uri = Uri.fromFile(java.io.File(image.filePath))
            characterManager.updateReferencePhoto(image.characterId, uri.toString())
            _statusNotice.value = "Updated ${image.characterName}'s portrait!"
        }
    }

    fun importGgufModel(uri: Uri) {
        viewModelScope.launch {
            val res = aiOrchestrator.importGgufFromUri(uri)
            res.onSuccess { model ->
                _statusNotice.value = "Imported ${model.displayName} (${model.quantization})"
            }.onFailure { err ->
                _statusNotice.value = err.message
            }
        }
    }

    fun downloadRecommendedModel(model: RecommendedGgufModel) {
        viewModelScope.launch {
            val strict = uiState.value.preferences.strictOfflineLock
            val res = aiOrchestrator.downloadApprovedModel(model, strict)
            res.onSuccess {
                _statusNotice.value = "Downloaded ${model.displayName} locally!"
            }.onFailure { err ->
                _statusNotice.value = err.message
            }
        }
    }

    fun cancelModelDownload() = aiOrchestrator.cancelDownload()

    fun loadModel(model: LocalModelEntity) {
        viewModelScope.launch {
            val res = aiOrchestrator.loadModel(model)
            res.onFailure { err -> _statusNotice.value = err.message }
            refreshRamSnapshot()
        }
    }

    fun unloadModel() {
        viewModelScope.launch {
            aiOrchestrator.unloadActiveModel()
            refreshRamSnapshot()
        }
    }

    fun deleteModel(model: LocalModelEntity) {
        viewModelScope.launch {
            aiOrchestrator.deleteModelFile(model)
            refreshRamSnapshot()
        }
    }

    fun setStrictOfflineLock(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setStrictOfflineLock(enabled) }
    }

    fun configureAppLock(enabled: Boolean, pin: String?) {
        viewModelScope.launch {
            prefsRepo.setAppLock(enabled, pin)
            if (!enabled) _isAppUnlocked.value = true
            _statusNotice.value = if (enabled) "4-digit App Lock enabled." else "App Lock disabled."
        }
    }

    fun attemptUnlock(pin: String): Boolean {
        val storedHash = uiState.value.preferences.appLockPinHash
        val ok = prefsRepo.verifyPin(pin, storedHash)
        if (ok) _isAppUnlocked.value = true
        return ok
    }

    fun clearStudioCache() {
        viewModelScope.launch {
            val bytes = portraitComposer.clearStudioCache()
            _statusNotice.value = "Cleared ${bytes / 1024} KB studio cache."
        }
    }

    fun freeRamNow() {
        viewModelScope.launch {
            aiOrchestrator.unloadActiveModel()
            voiceEngine.stopSpeaking()
            System.gc()
            refreshRamSnapshot()
            _statusNotice.value = "Unloaded heavy buffers and freed RAM."
        }
    }

    fun clearNotice() {
        _statusNotice.value = null
    }

    fun clearCrashReport() {
        viewModelScope.launch { prefsRepo.setLastCrashReport("") }
    }

    fun refreshRamSnapshot() {
        _ramSnapshot.value = readDeviceRam()
    }

    private fun readDeviceRam(): DeviceRamSnapshot {
        val ctx = getApplication<Application>()
        val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val info = ActivityManager.MemoryInfo()
        am?.getMemoryInfo(info)
        val total = (info.totalMem / (1024L * 1024L)).toInt().coerceAtLeast(1024)
        val avail = (info.availMem / (1024L * 1024L)).toInt().coerceAtLeast(128)
        val runtime = Runtime.getRuntime()
        val heapMb = ((runtime.totalMemory() - runtime.freeMemory()) / (1024L * 1024L)).toInt()
        return DeviceRamSnapshot(
            totalMb = total,
            availableMb = avail,
            appHeapUsedMb = heapMb,
            isLowMemory = info.lowMemory
        )
    }

    override fun onCleared() {
        super.onCleared()
        voiceEngine.release()
        speechRecognizer.stopListening()
    }

    private data class CoreDataBundle(
        val prefs: UserPreferencesState,
        val characters: List<CharacterProfileEntity>,
        val messages: List<ChatMessageEntity>,
        val memories: List<MemoryFactEntity>,
        val images: List<StudioImageEntity>
    )

    private data class EngineBundle(
        val models: List<LocalModelEntity>,
        val chatStatus: ChatEngineStatus,
        val dlState: ModelDownloadState,
        val voiceRep: VoiceAvailabilityReport,
        val femaleVoices: List<FemaleVoiceOption>
    )

    private data class LiveFlagsBundle(
        val speaking: Boolean,
        val sttState: SpeechListenState,
        val rms: Float,
        val genReply: Boolean,
        val compImage: Boolean
    )
}
