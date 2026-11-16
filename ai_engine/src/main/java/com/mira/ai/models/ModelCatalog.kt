package com.mira.ai.models

data class RecommendedGgufModel(
    val id: String,
    val displayName: String,
    val family: String,
    val quantization: String,
    val fileName: String,
    val downloadUrl: String,
    val approxSizeMb: Int,
    val estimatedRamMb: Int,
    val compatibilityNoteEn: String,
    val compatibilityNoteBn: String
)

object ModelCatalog {
    val recommendedModels = listOf(
        RecommendedGgufModel(
            id = "smollm2_360m_q8",
            displayName = "SmolLM2 360M Instruct",
            family = "SmolLM2",
            quantization = "Q8_0",
            fileName = "SmolLM2-360M-Instruct-Q8_0.gguf",
            downloadUrl = "https://huggingface.co/bartowski/SmolLM2-360M-Instruct-GGUF/resolve/main/SmolLM2-360M-Instruct-Q8_0.gguf",
            approxSizeMb = 386,
            estimatedRamMb = 620,
            compatibilityNoteEn = "Best fit for 3GB RAM Android 8.1 devices.",
            compatibilityNoteBn = "৩ জিবি র‍্যাম Android 8.1 ফোনের জন্য সবচেয়ে নিরাপদ।"
        ),
        RecommendedGgufModel(
            id = "qwen25_05b_q4km",
            displayName = "Qwen2.5 0.5B Instruct",
            family = "Qwen2.5",
            quantization = "Q4_K_M",
            fileName = "Qwen2.5-0.5B-Instruct-Q4_K_M.gguf",
            downloadUrl = "https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/qwen2.5-0.5b-instruct-q4_k_m.gguf",
            approxSizeMb = 491,
            estimatedRamMb = 760,
            compatibilityNoteEn = "Multilingual friendly (EN/BN/HI). Fits 3GB RAM with Image Studio unloaded.",
            compatibilityNoteBn = "বাংলা ও ইংরেজি সহায়ক; ইমেজ স্টুডিও বন্ধ রেখে ৩ জিবি র‍্যামে চলে।"
        ),
        RecommendedGgufModel(
            id = "qwen25_15b_q2k",
            displayName = "Qwen2.5 1.5B Instruct",
            family = "Qwen2.5",
            quantization = "Q2_K",
            fileName = "Qwen2.5-1.5B-Instruct-Q2_K.gguf",
            downloadUrl = "https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF/resolve/main/qwen2.5-1.5b-instruct-q2_k.gguf",
            approxSizeMb = 680,
            estimatedRamMb = 1050,
            compatibilityNoteEn = "Tight fit on 3GB RAM: close background apps first.",
            compatibilityNoteBn = "৩ জিবি র‍্যামে চাপ পড়তে পারে—অন্য অ্যাপ বন্ধ রাখুন।"
        ),
        RecommendedGgufModel(
            id = "gemma2_2b_iq2xs",
            displayName = "Gemma 2 2B Instruct",
            family = "Gemma-2",
            quantization = "IQ2_XS",
            fileName = "Gemma-2-2b-it-IQ2_XS.gguf",
            downloadUrl = "https://huggingface.co/bartowski/gemma-2-2b-it-GGUF/resolve/main/gemma-2-2b-it-IQ2_XS.gguf",
            approxSizeMb = 860,
            estimatedRamMb = 1320,
            compatibilityNoteEn = "Experimental on 3GB RAM (requires unloading TTS & Image buffers).",
            compatibilityNoteBn = "৩ জিবি র‍্যামে পরীক্ষামূলক—ভয়েস ও ইমেজ বাফার খালি করতে হবে।"
        )
    )
}
