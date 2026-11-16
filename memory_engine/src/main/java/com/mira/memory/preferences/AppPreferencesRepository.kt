package com.mira.memory.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.security.MessageDigest

private val Context.miraPrefsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "mira_local_settings"
)

data class UserPreferencesState(
    val uiLanguage: String = "en", // "en" or "bn"
    val darkTheme: Boolean = true,
    val appLockEnabled: Boolean = false,
    val appLockPinHash: String = "",
    val strictOfflineLock: Boolean = true,
    val activeCharacterId: String = "mira_permanent",
    val preferredChatLanguage: String = "bn", // "bn", "en", "hi"
    val ttsSpeed: Float = 0.95f,
    val ttsVolume: Float = 0.95f,
    val selectedFemaleVoiceName: String = "",
    val lastCrashReport: String = ""
)

class AppPreferencesRepository(private val context: Context) {
    private object Keys {
        val UI_LANGUAGE = stringPreferencesKey("ui_language")
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        val APP_LOCK_PIN_HASH = stringPreferencesKey("app_lock_pin_hash")
        val STRICT_OFFLINE_LOCK = booleanPreferencesKey("strict_offline_lock")
        val ACTIVE_CHARACTER_ID = stringPreferencesKey("active_character_id")
        val PREFERRED_CHAT_LANGUAGE = stringPreferencesKey("preferred_chat_language")
        val TTS_SPEED = floatPreferencesKey("tts_speed")
        val TTS_VOLUME = floatPreferencesKey("tts_volume")
        val SELECTED_FEMALE_VOICE = stringPreferencesKey("selected_female_voice")
        val LAST_CRASH_REPORT = stringPreferencesKey("last_crash_report")
    }

    val preferencesFlow: Flow<UserPreferencesState> = context.miraPrefsDataStore.data.map { prefs ->
        UserPreferencesState(
            uiLanguage = prefs[Keys.UI_LANGUAGE] ?: "en",
            darkTheme = prefs[Keys.DARK_THEME] ?: true,
            appLockEnabled = prefs[Keys.APP_LOCK_ENABLED] ?: false,
            appLockPinHash = prefs[Keys.APP_LOCK_PIN_HASH] ?: "",
            strictOfflineLock = prefs[Keys.STRICT_OFFLINE_LOCK] ?: true,
            activeCharacterId = prefs[Keys.ACTIVE_CHARACTER_ID] ?: "mira_permanent",
            preferredChatLanguage = prefs[Keys.PREFERRED_CHAT_LANGUAGE] ?: "bn",
            ttsSpeed = prefs[Keys.TTS_SPEED] ?: 0.95f,
            ttsVolume = prefs[Keys.TTS_VOLUME] ?: 0.95f,
            selectedFemaleVoiceName = prefs[Keys.SELECTED_FEMALE_VOICE] ?: "",
            lastCrashReport = prefs[Keys.LAST_CRASH_REPORT] ?: ""
        )
    }

    suspend fun setUiLanguage(lang: String) {
        context.miraPrefsDataStore.edit { it[Keys.UI_LANGUAGE] = lang }
    }

    suspend fun setDarkTheme(enabled: Boolean) {
        context.miraPrefsDataStore.edit { it[Keys.DARK_THEME] = enabled }
    }

    suspend fun setAppLock(enabled: Boolean, pin: String?) {
        context.miraPrefsDataStore.edit { prefs ->
            prefs[Keys.APP_LOCK_ENABLED] = enabled
            if (!pin.isNullOrBlank()) {
                prefs[Keys.APP_LOCK_PIN_HASH] = hashPin(pin)
            }
        }
    }

    suspend fun setStrictOfflineLock(enabled: Boolean) {
        context.miraPrefsDataStore.edit { it[Keys.STRICT_OFFLINE_LOCK] = enabled }
    }

    suspend fun setActiveCharacterId(characterId: String) {
        context.miraPrefsDataStore.edit { it[Keys.ACTIVE_CHARACTER_ID] = characterId }
    }

    suspend fun setPreferredChatLanguage(lang: String) {
        context.miraPrefsDataStore.edit { it[Keys.PREFERRED_CHAT_LANGUAGE] = lang }
    }

    suspend fun setVoiceTuning(speed: Float, volume: Float, voiceName: String? = null) {
        context.miraPrefsDataStore.edit { prefs ->
            prefs[Keys.TTS_SPEED] = speed.coerceIn(0.6f, 1.5f)
            prefs[Keys.TTS_VOLUME] = volume.coerceIn(0f, 1f)
            if (voiceName != null) {
                prefs[Keys.SELECTED_FEMALE_VOICE] = voiceName
            }
        }
    }

    suspend fun setLastCrashReport(report: String) {
        context.miraPrefsDataStore.edit { it[Keys.LAST_CRASH_REPORT] = report }
    }

    fun verifyPin(pin: String, storedHash: String): Boolean {
        if (storedHash.isBlank()) return true
        return hashPin(pin) == storedHash
    }

    private fun hashPin(pin: String): String {
        val salted = "MIRA_LOCAL_SALT_27:$pin"
        val digest = MessageDigest.getInstance("SHA-256").digest(salted.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
