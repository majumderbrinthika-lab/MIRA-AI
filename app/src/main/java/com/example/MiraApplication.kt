package com.example

import android.app.Application
import android.content.ComponentCallbacks2
import android.content.Context
import com.mira.ai.LocalAiOrchestrator
import com.mira.character.CharacterManager
import com.mira.image.LocalPortraitComposer
import com.mira.memory.db.MiraDatabase
import com.mira.memory.preferences.AppPreferencesRepository
import com.mira.memory.repository.MemoryRepository
import com.mira.voice.FemaleVoiceEngine
import com.mira.voice.OfflineSpeechRecognizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppContainer(context: Context) {
    val database: MiraDatabase by lazy { MiraDatabase.getInstance(context) }
    val memoryRepository: MemoryRepository by lazy { MemoryRepository(database) }
    val preferencesRepository: AppPreferencesRepository by lazy { AppPreferencesRepository(context) }
    val characterManager: CharacterManager by lazy { CharacterManager(memoryRepository) }
    val aiOrchestrator: LocalAiOrchestrator by lazy { LocalAiOrchestrator(context, memoryRepository) }
    val voiceEngine: FemaleVoiceEngine by lazy { FemaleVoiceEngine(context) }
    val speechRecognizer: OfflineSpeechRecognizer by lazy { OfflineSpeechRecognizer(context) }
    val portraitComposer: LocalPortraitComposer by lazy { LocalPortraitComposer(context) }
}

class MiraApplication : Application() {
    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        installCrashRecoveryHandler()

        appScope.launch {
            container.characterManager.ensureSeeded()
            // Restore last crash note if one occurred in prior session
            val prefs = getSharedPreferences("mira_crash_guard", MODE_PRIVATE)
            val lastCrash = prefs.getString("last_crash", null)
            if (!lastCrash.isNullOrBlank()) {
                container.preferencesRepository.setLastCrashReport(lastCrash)
                prefs.edit().remove("last_crash").apply()
            }
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
            appScope.launch {
                container.aiOrchestrator.unloadActiveModel()
            }
        }
    }

    private fun installCrashRecoveryHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val summary = "${throwable::class.java.simpleName}: ${throwable.message ?: "Unexpected error"}"
                getSharedPreferences("mira_crash_guard", MODE_PRIVATE)
                    .edit()
                    .putString("last_crash", summary)
                    .commit()
            } catch (_: Exception) {
                // Ignore secondary logging failure
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
