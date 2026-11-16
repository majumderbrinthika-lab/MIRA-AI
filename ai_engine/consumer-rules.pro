# Keep JNI native bridge methods intact for optional libmira_llama.so
-keepclasseswithmembers class com.mira.ai.runtime.NativeLlamaBridge {
    native <methods>;
}
