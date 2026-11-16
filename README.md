# MIRA — Privacy-First Offline AI Companion (Android 8.1+ / 3GB RAM)

MIRA is a modular, privacy-focused native Android application built with Kotlin, Jetpack Compose, Material Design 3, and Room Database. Designed specifically for **Android 8.1 Oreo (API 27+)** and **3GB RAM ARM devices**, MIRA keeps all conversations, memories, portraits, and character profiles strictly on-device.

---

## Architecture Overview (6 Modules)

| Module | Responsibility |
| :--- | :--- |
| `:app` | Jetpack Compose Material 3 UI, bilingual localization (`English` & `বাংলা`), adaptive navigation across 7 screens, 4-digit PIN App Lock, `onTrimMemory` guard, and crash recovery. |
| `:memory_engine` | Room Database (`MiraDatabase`), DAOs, `MemoryRepository`, DataStore preferences, and `MemoryExtractor` (multilingual on-device fact extraction & relevance retrieval). |
| `:character_engine` | Permanent **Mira** identity, 10-trait `PersonalityTraits` engine, `PersonaPromptBuilder`, `CharacterSafetyValidator` (adult fictional characters only), and custom AI Girl creator. |
| `:ai_engine` | `LocalAiOrchestrator`, binary `GgufFileInspector` (GGUF v2/v3 header parser & 3GB RAM estimator), `ModelCatalog` (SmolLM2 / Qwen2.5 / Gemma-2), `NativeLlamaBridge` JNI hook, and `OfflineContextualEngine` (100% offline trilingual BN/EN/HI responder). |
| `:voice_engine` | `FemaleVoiceEngine` (Android `TextToSpeech` strictly filtered to female voices only; Bengali, English & Hindi; speed/volume tuning; Android 8.1 offline voice pack audit) + `OfflineSpeechRecognizer` (`EXTRA_PREFER_OFFLINE`). |
| `:image_engine` | Modular `ImageGenerationEngine`, `DeviceImageCapabilityInspector` (honest 3GB RAM feasibility check), and `LocalPortraitComposer` (memory-safe `inSampleSize` + `Canvas`/`ColorMatrix` offline South Asian portrait & scene studio). |

---

## Privacy & Offline Honesty Guarantees

- **Zero accounts, zero analytics, zero ads, zero cloud databases.**
- **Strict Offline Lock** (enabled by default in Settings) blocks all network requests—even optional model downloads.
- **No Fake Offline Claims:**
  - **Chat AI:** Works 100% offline out-of-the-box via the lightweight `OfflineContextualEngine` (<12MB RAM). You can also import `.gguf` files via Storage Access Framework (`OpenDocument`); `GgufFileInspector` parses real GGUF v2/v3 headers and enforces 3GB RAM limits. Running raw 1B+ neural tensor weights requires linking a native ARM `libmira_llama.so` (built from `llama.cpp` via Android NDK) into `ai_engine/src/main/jniLibs/`.
  - **Voice Engine:** Strictly exposes **female voices only** and verifies `!voice.isNetworkConnectionRequired`. If your Android 8.1 device lacks an offline Bengali voice pack, MIRA clearly explains the limitation and lets you open Android's system TTS installer rather than silently using online voices.
  - **AI Image Studio:** Full Stable Diffusion 1.5 / SDXL models exceed 3GB RAM and crash on Android 8.1 phones. MIRA explains this transparently and provides a fast, 100% offline South Asian Portrait & Scene Composer using `PickVisualMedia` reference photos or curated presets.

---

## Building the APK Locally & via GitHub Actions

### Automated GitHub Actions Build
Push this repository to GitHub. The included workflow at `.github/workflows/android.yml` automatically:
1. Sets up JDK 17, Android SDK 36, and Gradle `9.3.1`.
2. Runs unit & Robolectric tests across modules.
3. Builds `app/build/outputs/apk/debug/app-debug.apk` and uploads it as the downloadable artifact **`MIRA-offline-debug-apk`**.

### Local Build
```bash
gradle :app:testDebugUnitTest
gradle :app:assembleDebug
```
Install on an Android 8.1+ (API 27+) device via:
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```
