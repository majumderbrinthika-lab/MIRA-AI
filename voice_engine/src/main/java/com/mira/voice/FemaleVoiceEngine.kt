package com.mira.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

data class FemaleVoiceOption(
    val voiceName: String,
    val localeTag: String,
    val displayLabel: String,
    val isOfflineReady: Boolean,
    val isExplicitFemaleMatch: Boolean
)

data class VoiceAvailabilityReport(
    val isTtsInitialized: Boolean = false,
    val enginePackage: String = "",
    val offlineBengaliFemaleAvailable: Boolean = false,
    val offlineEnglishFemaleAvailable: Boolean = false,
    val offlineHindiFemaleAvailable: Boolean = false,
    val limitationMessageEn: String = "",
    val limitationMessageBn: String = ""
)

class FemaleVoiceEngine(private val context: Context) {

    private var tts: TextToSpeech? = null

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _femaleVoices = MutableStateFlow<List<FemaleVoiceOption>>(emptyList())
    val femaleVoices: StateFlow<List<FemaleVoiceOption>> = _femaleVoices.asStateFlow()

    private val _report = MutableStateFlow(VoiceAvailabilityReport())
    val report: StateFlow<VoiceAvailabilityReport> = _report.asStateFlow()

    private var currentSpeed: Float = 0.95f
    private var currentVolume: Float = 0.95f
    private var preferredVoiceName: String = ""

    private val maleMarkers = listOf(
        "male", "-m-", "_m_", "#male", "man", "boy",
        "rishi", "prabhat", "madhur", "hemant", "amit", "vikram",
        "david", "mark", "george", "daniel", "james", "ryan",
        "bn-in-x-bnm", "bn-bd-x-bqm", "en-in-x-ene", "en-us-x-iom", "en-us-x-tpd",
        "hi-in-x-hie", "hi-in-x-hid"
    )

    private val femaleMarkers = listOf(
        "female", "-f-", "_f_", "#female", "woman",
        "tanisha", "kalpana", "heera", "lekha", "neerja", "swara", "aditi", "raveena",
        "samantha", "victoria", "karen", "aria", "jenny", "sara",
        "bn-in-x-bnf", "bn-in-x-bin", "bn-bd-x-ban", "bn-bd-x-bdf",
        "en-us-x-sfg", "en-us-x-iob", "en-us-x-tpf", "en-in-x-ena", "en-in-x-enc",
        "hi-in-x-hia", "hi-in-x-hic"
    )

    fun initialize() {
        if (tts != null) return
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                configureListener()
                auditAvailableFemaleVoices()
            } else {
                _report.value = VoiceAvailabilityReport(
                    isTtsInitialized = false,
                    limitationMessageEn = "Android Text-to-Speech engine failed to initialize on this device.",
                    limitationMessageBn = "এই ডিভাইসে অ্যান্ড্রয়েড টেক্সট-টু-স্পিচ ইঞ্জিন চালু করা যায়নি।"
                )
            }
        }
    }

    private fun configureListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _isSpeaking.value = true
            }

            override fun onDone(utteranceId: String?) {
                _isSpeaking.value = false
            }

            @Deprecated("Deprecated in API 21 but kept for safety")
            override fun onError(utteranceId: String?) {
                _isSpeaking.value = false
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                _isSpeaking.value = false
            }
        })
    }

    fun auditAvailableFemaleVoices() {
        val engine = tts ?: return
        val allVoices = try {
            engine.voices.orEmpty()
        } catch (_: Exception) {
            emptySet()
        }

        val supportedLangs = setOf("bn", "en", "hi")
        val filtered = allVoices
            .filter { it.locale != null && it.locale.language in supportedLangs }
            .filter { voice -> !isMaleVoice(voice) }
            .map { voice ->
                val explicitFemale = isExplicitFemaleVoice(voice)
                val langLabel = when (voice.locale.language) {
                    "bn" -> "বাংলা"
                    "hi" -> "हिन्दी"
                    else -> "English"
                }
                val offlineTag = if (!voice.isNetworkConnectionRequired) "Offline" else "Network-Only"
                FemaleVoiceOption(
                    voiceName = voice.name,
                    localeTag = voice.locale.toLanguageTag(),
                    displayLabel = "$langLabel (${voice.locale.country.ifBlank { "Global" }}) • Female • $offlineTag",
                    isOfflineReady = !voice.isNetworkConnectionRequired,
                    isExplicitFemaleMatch = explicitFemale
                )
            }
            .sortedWith(
                compareByDescending<FemaleVoiceOption> { it.isOfflineReady }
                    .thenByDescending { it.isExplicitFemaleMatch }
                    .thenBy { it.localeTag }
            )

        _femaleVoices.value = filtered

        val hasOfflineBn = filtered.any { it.localeTag.startsWith("bn") && it.isOfflineReady }
        val hasOfflineEn = filtered.any { it.localeTag.startsWith("en") && it.isOfflineReady }
        val hasOfflineHi = filtered.any { it.localeTag.startsWith("hi") && it.isOfflineReady }

        val limitEn = if (!hasOfflineBn) {
            "No offline Bengali (bn-BD / bn-IN) female voice pack is installed in this device's Android 8.1 TTS data. " +
                "MIRA will NOT silently switch to online cloud voices. Please install offline Bengali voice data in Android TTS Settings."
        } else {
            "Offline female voice packs detected and ready for local playback."
        }

        val limitBn = if (!hasOfflineBn) {
            "এই Android 8.1 ডিভাইসে অফলাইন বাংলা (bn-BD / bn-IN) নারী কণ্ঠের প্যাক ইনস্টল করা নেই। " +
                "MIRA গোপনে কোনো অনলাইন সার্ভিস ব্যবহার করবে না। অনুগ্রহ করে অ্যান্ড্রয়েড TTS সেটিংস থেকে অফলাইন বাংলা ভয়েস ডাটা ডাউনলোড করুন।"
        } else {
            "অফলাইন নারী কণ্ঠের প্যাক প্রস্তুত রয়েছে এবং সম্পূর্ণ লোকালভাবে কাজ করছে।"
        }

        _report.value = VoiceAvailabilityReport(
            isTtsInitialized = true,
            enginePackage = engine.defaultEngine ?: "System TTS",
            offlineBengaliFemaleAvailable = hasOfflineBn,
            offlineEnglishFemaleAvailable = hasOfflineEn,
            offlineHindiFemaleAvailable = hasOfflineHi,
            limitationMessageEn = limitEn,
            limitationMessageBn = limitBn
        )
    }

    fun updateTuning(speed: Float, volume: Float, voiceName: String) {
        currentSpeed = speed.coerceIn(0.6f, 1.5f)
        currentVolume = volume.coerceIn(0f, 1f)
        preferredVoiceName = voiceName
    }

    /**
     * Speaks text strictly using an offline female voice when available.
     * Returns false with a clear reason if offline voice data for that language is unavailable,
     * never silently falling back to a network-required voice.
     */
    fun speakOfflineFemale(text: String, languageCode: String): Result<Unit> {
        val engine = tts ?: return Result.failure(IllegalStateException("TTS engine not ready yet."))
        val targetLang = when (languageCode) {
            "bn" -> "bn"
            "hi" -> "hi"
            else -> "en"
        }

        val allVoices = try {
            engine.voices.orEmpty()
        } catch (_: Exception) {
            emptySet()
        }

        val offlineFemaleVoices = allVoices.filter { v ->
            v.locale?.language == targetLang &&
                !v.isNetworkConnectionRequired &&
                !isMaleVoice(v)
        }

        val chosenVoice = offlineFemaleVoices.firstOrNull { it.name == preferredVoiceName }
            ?: offlineFemaleVoices.firstOrNull { isExplicitFemaleVoice(it) }
            ?: offlineFemaleVoices.firstOrNull()

        if (chosenVoice != null) {
            engine.voice = chosenVoice
            engine.language = chosenVoice.locale
        } else {
            // Check legacy Android 8.1 locale availability without network
            val locale = when (targetLang) {
                "bn" -> Locale.Builder().setLanguage("bn").setRegion("BD").build()
                "hi" -> Locale.Builder().setLanguage("hi").setRegion("IN").build()
                else -> Locale.US
            }
            val avail = engine.isLanguageAvailable(locale)
            if (avail < TextToSpeech.LANG_AVAILABLE) {
                return Result.failure(
                    IllegalStateException(
                        "Offline $targetLang female voice is not installed on this Android 8.1 device."
                    )
                )
            }
            engine.language = locale
        }

        // Warm feminine pitch & natural pacing
        val femininePitch = if (chosenVoice != null && isExplicitFemaleVoice(chosenVoice)) 1.08f else 1.18f
        engine.setPitch(femininePitch)
        engine.setSpeechRate(currentSpeed)

        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, currentVolume)
        }
        val utteranceId = UUID.randomUUID().toString()
        val result = engine.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        return if (result == TextToSpeech.SUCCESS) {
            Result.success(Unit)
        } else {
            Result.failure(IllegalStateException("Android TTS speak() failed with code $result"))
        }
    }

    fun stopSpeaking() {
        tts?.stop()
        _isSpeaking.value = false
    }

    fun buildInstallTtsDataIntent(): Intent =
        Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    fun release() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        _isSpeaking.value = false
    }

    private fun isMaleVoice(voice: Voice): Boolean {
        val lower = voice.name.lowercase()
        val featureStr = voice.features?.joinToString(" ")?.lowercase().orEmpty()
        return maleMarkers.any { lower.contains(it) || featureStr.contains(it) }
    }

    private fun isExplicitFemaleVoice(voice: Voice): Boolean {
        val lower = voice.name.lowercase()
        val featureStr = voice.features?.joinToString(" ")?.lowercase().orEmpty()
        return femaleMarkers.any { lower.contains(it) || featureStr.contains(it) }
    }
}
