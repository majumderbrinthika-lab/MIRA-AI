package com.mira.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class SpeechListenState {
    data object Idle : SpeechListenState()
    data object Listening : SpeechListenState()
    data class Partial(val text: String) : SpeechListenState()
    data class ResultReady(val text: String) : SpeechListenState()
    data class Error(val messageEn: String, val messageBn: String) : SpeechListenState()
}

/**
 * Android 8.1+ (API 27) compatible SpeechRecognizer wrapper preferring offline recognition.
 */
class OfflineSpeechRecognizer(private val context: Context) {

    private var recognizer: SpeechRecognizer? = null

    private val _state = MutableStateFlow<SpeechListenState>(SpeechListenState.Idle)
    val state: StateFlow<SpeechListenState> = _state.asStateFlow()

    private val _rmsLevel = MutableStateFlow(0f)
    val rmsLevel: StateFlow<Float> = _rmsLevel.asStateFlow()

    fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    fun startListening(languageCode: String) {
        if (!isAvailable()) {
            _state.value = SpeechListenState.Error(
                messageEn = "No speech recognition service is installed on this Android 8.1 device.",
                messageBn = "এই Android 8.1 ডিভাইসে স্পিচ রিকগনিশন সার্ভিস ইনস্টল করা নেই।"
            )
            return
        }

        stopListening()
        val sr = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer = sr

        val localeTag = when (languageCode) {
            "bn" -> "bn-BD"
            "hi" -> "hi-IN"
            else -> "en-US"
        }

        sr.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _state.value = SpeechListenState.Listening
            }

            override fun onBeginningOfSpeech() {}

            override fun onRmsChanged(rmsdB: Float) {
                _rmsLevel.value = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                _rmsLevel.value = 0f
            }

            override fun onError(error: Int) {
                _rmsLevel.value = 0f
                val (en, bn) = mapError(error)
                _state.value = SpeechListenState.Error(en, bn)
            }

            override fun onResults(results: Bundle?) {
                _rmsLevel.value = 0f
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val best = matches?.firstOrNull().orEmpty().trim()
                if (best.isNotEmpty()) {
                    _state.value = SpeechListenState.ResultReady(best)
                } else {
                    _state.value = SpeechListenState.Idle
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (!partial.isNullOrBlank()) {
                    _state.value = SpeechListenState.Partial(partial)
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, localeTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        sr.startListening(intent)
    }

    fun stopListening() {
        recognizer?.stopListening()
        recognizer?.destroy()
        recognizer = null
        _rmsLevel.value = 0f
        if (_state.value is SpeechListenState.Listening) {
            _state.value = SpeechListenState.Idle
        }
    }

    fun resetState() {
        _state.value = SpeechListenState.Idle
    }

    private fun mapError(error: Int): Pair<String, String> = when (error) {
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
            "Microphone permission is required for voice conversation." to
                "ভয়েস কথোপকথনের জন্য মাইক্রোফোন অনুমতি প্রয়োজন।"
        SpeechRecognizer.ERROR_NO_MATCH ->
            "I couldn't catch that clearly—please try speaking again." to
                "কথাটা স্পষ্ট শোনা যায়নি, আবার একটু বলো তো।"
        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
            "Offline speech pack for this language is missing. Download the offline voice pack in Android Settings." to
                "এই ভাষার অফলাইন স্পিচ প্যাক অনুপস্থিত। অ্যান্ড্রয়েড সেটিংস থেকে অফলাইন প্যাক ডাউনলোড করুন।"
        else ->
            "Speech recognizer paused (code $error)." to
                "স্পিচ রিকগনিশন সাময়িকভাবে থেমেছে (কোড $error)।"
    }
}
