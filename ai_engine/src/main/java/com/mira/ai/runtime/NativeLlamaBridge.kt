package com.mira.ai.runtime

/**
 * Optional JNI bridge to native llama.cpp ARMv7/ARM64 shared library (`libmira_llama.so`).
 * Designed for low-RAM Android 8.1+ devices: if the native .so is not packaged in jniLibs,
 * `isNativeLibraryAvailable()` returns false cleanly without crashing or faking neural output.
 */
object NativeLlamaBridge {
    private val nativeLoaded: Boolean = try {
        System.loadLibrary("mira_llama")
        true
    } catch (_: UnsatisfiedLinkError) {
        false
    }

    private var activeContextPtr: Long = 0L

    fun isNativeLibraryAvailable(): Boolean = nativeLoaded

    fun isModelLoadedInNative(): Boolean = nativeLoaded && activeContextPtr != 0L

    fun loadModel(path: String, contextSize: Int = 1024, threads: Int = 4): Boolean {
        if (!nativeLoaded) return false
        unloadModel()
        activeContextPtr = nativeLoadModel(path, contextSize, threads)
        return activeContextPtr != 0L
    }

    fun generate(prompt: String, maxTokens: Int = 160, temperature: Float = 0.72f): String? {
        if (!nativeLoaded || activeContextPtr == 0L) return null
        return nativeGenerate(activeContextPtr, prompt, maxTokens, temperature)
    }

    fun unloadModel() {
        if (nativeLoaded && activeContextPtr != 0L) {
            nativeFreeModel(activeContextPtr)
            activeContextPtr = 0L
        }
    }

    @JvmStatic
    private external fun nativeLoadModel(modelPath: String, nCtx: Int, nThreads: Int): Long

    @JvmStatic
    private external fun nativeGenerate(ctxPtr: Long, prompt: String, maxTokens: Int, temp: Float): String

    @JvmStatic
    private external fun nativeFreeModel(ctxPtr: Long)
}
