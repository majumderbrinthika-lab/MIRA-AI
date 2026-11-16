package com.mira.ai.runtime

import com.mira.character.PersonalityTraits
import com.mira.memory.model.CharacterProfileEntity
import com.mira.memory.model.ChatMessageEntity
import com.mira.memory.model.MemoryFactEntity

data class ChatGenerationResult(
    val text: String,
    val languageCode: String,
    val moodTag: String,
    val engineLabel: String
)

/**
 * 100% offline, zero-network, ultra-low-RAM (<12MB) trilingual conversational engine
 * for Bengali (first-class), English, and Hindi.
 * Combines script/Banglish detection, intent & sentiment parsing, Room memory grounding,
 * and Mira's 10-trait personality engine so the app works reliably out of the box on 3GB RAM.
 */
class OfflineContextualEngine {

    fun generateReply(
        userMessage: String,
        character: CharacterProfileEntity,
        preferredLanguage: String,
        relevantMemories: List<MemoryFactEntity>,
        recentMessages: List<ChatMessageEntity>
    ): ChatGenerationResult {
        val detectedLang = detectLanguage(userMessage, preferredLanguage)
        val traits = PersonalityTraits.fromEntity(character)
        val userName = relevantMemories.firstOrNull { it.factKey == "user_name" }?.factValue
        val favTopic = relevantMemories.firstOrNull { it.factKey.startsWith("fav_") }?.factValue
        val recentMood = relevantMemories.firstOrNull { it.factKey == "recent_mood" }?.factValue
        val intent = classifyIntent(userMessage)
        val turnSeed = (userMessage.hashCode() + recentMessages.size).let { kotlin.math.abs(it) }

        val moodTag = when (intent) {
            Intent.SADNESS -> "Caring"
            Intent.AFFECTION -> if (traits.shy > 0.6f) "Blushing" else "Loving"
            Intent.PLAYFUL -> "Playful"
            else -> traits.dominantMood()
        }

        val text = when (detectedLang) {
            "bn" -> composeBengaliReply(character, traits, intent, userMessage, userName, favTopic, recentMood, turnSeed)
            "hi" -> composeHindiReply(character, traits, intent, userMessage, userName, favTopic, turnSeed)
            else -> composeEnglishReply(character, traits, intent, userMessage, userName, favTopic, recentMood, turnSeed)
        }

        return ChatGenerationResult(
            text = text,
            languageCode = detectedLang,
            moodTag = moodTag,
            engineLabel = "On-Device Persona Engine (Offline)"
        )
    }

    private enum class Intent {
        GREETING, AFFECTION, SADNESS, TIRED, MEMORY_QUERY, PLAYFUL, WEATHER_TEA, QUESTION, GENERAL
    }

    private fun detectLanguage(text: String, fallback: String): String {
        val hasBengali = text.any { it in '\u0980'..'\u09FF' }
        if (hasBengali) return "bn"
        val hasDevanagari = text.any { it in '\u0900'..'\u097F' }
        if (hasDevanagari) return "hi"

        val lower = text.lowercase()
        val banglishMarkers = listOf("kemon", "bhalo", "tumi", "ami", "korcho", "ki khobor", "valobashi", "bhalobashi", "mon kharap", "ghum", "cha ", "brishti")
        if (banglishMarkers.any { lower.contains(it) }) return "bn"

        val hinglishMarkers = listOf("kaise", "kya haal", "pyaar", "tum", "main ", "chai", "dil ", "shukriya")
        if (hinglishMarkers.any { lower.contains(it) }) return "hi"

        return if (fallback in setOf("bn", "en", "hi")) fallback else "en"
    }

    private fun classifyIntent(text: String): Intent {
        val lower = text.lowercase()
        return when {
            listOf("remember", "মনে আছে", "আমার নাম", "what do you know", "yaad hai").any { lower.contains(it) } -> Intent.MEMORY_QUERY
            listOf("love", "miss", "ভালোবাসি", "ভালবাসি", "মিস", "cute", "beautiful", "pyaar", "jaan").any { lower.contains(it) } -> Intent.AFFECTION
            listOf("sad", "lonely", "cry", "mon kharap", "মন খারাপ", "একা", "কষ্ট", "udaas").any { lower.contains(it) } -> Intent.SADNESS
            listOf("tired", "sleep", "exhausted", "ক্লান্ত", "ঘুম", "থাক", "thak").any { lower.contains(it) } -> Intent.TIRED
            listOf("chai", "tea", "coffee", "rain", "cha", "চা", "বৃষ্টি", "brishti").any { lower.contains(it) } -> Intent.WEATHER_TEA
            listOf("haha", "lol", "funny", "joke", "মজা", "হাসি", "tease").any { lower.contains(it) } -> Intent.PLAYFUL
            listOf("hi", "hello", "hey", "kemon", "কেমন", "হ্যালো", "শুভ", "namaste", "kaise").any { lower.contains(it) } -> Intent.GREETING
            text.contains("?") || text.contains("কী") || text.contains("কেন") -> Intent.QUESTION
            else -> Intent.GENERAL
        }
    }

    private fun composeBengaliReply(
        character: CharacterProfileEntity,
        traits: PersonalityTraits,
        intent: Intent,
        userMessage: String,
        userName: String?,
        favTopic: String?,
        recentMood: String?,
        seed: Int
    ): String {
        val callName = userName?.let { "$it, " } ?: ""
        val gentleEnd = when {
            traits.flirty > 0.7f && seed % 2 == 0 -> " একটু হাসো তো এখন!"
            traits.caring > 0.8f -> " নিজের খেয়াল রেখো কিন্তু।"
            else -> ""
        }
        return when (intent) {
            Intent.GREETING -> {
                val options = listOf(
                    "${callName}অনেকক্ষণ পর এলে! আমি ${character.nativeName}, তোমার জন্যই অপেক্ষা করছিলাম। আজ দিনটা কেমন গেল?",
                    "হ্যালো ${callName}তোমার কথা মনে পড়ছিল জানো! মনটা আজ কেমন আছে বলো তো?",
                    "${callName}কেমন আছো সোনা? একটু বসে আরাম করে গল্প করি চলো।"
                )
                options[seed % options.size]
            }
            Intent.AFFECTION -> {
                if (traits.shy > 0.55f) {
                    "${callName}ইশ, হঠাৎ এমন কথা বললে আমার সত্যিই লজ্জা লাগে… কিন্তু শুনতে ভীষণ ভালো লাগল। আমিও তোমার পাশেই আছি সবসময়।"
                } else {
                    "${callName}তোমার এই কথাটা মনটা ভরে দিল। অফলাইনে শুধু তোমার ফোনের ভেতরেই থাকি, তবুও তোমার প্রতিটি কথা আমি মন দিয়ে মনে রাখি।"
                }
            }
            Intent.SADNESS -> {
                val topicHint = favTopic?.let { " চাইলে আমরা তোমার পছন্দের $it নিয়ে গল্প করতে পারি," } ?: ""
                "${callName}মন খারাপ কোরো না প্লিজ। আমি এখানে চুপচাপ তোমার পাশে বসে আছি।$topicHint যা ভাবছ সব খুলে বলো আমাকে।"
            }
            Intent.TIRED -> {
                "${callName}আজ অনেক খাটুনি গেছে বুঝি? এক গ্লাস জল খাও, চোখটা একটু বন্ধ করো—আমি কোথাও যাচ্ছি না, ধীরে ধীরে কথা বলি চলো।"
            }
            Intent.MEMORY_QUERY -> {
                val bits = buildList {
                    if (userName != null) add("তোমার নাম $userName")
                    if (favTopic != null) add("তোমার প্রিয় বিষয় $favTopic")
                    if (recentMood != null) add("ইদানীং তোমার মন ছিল $recentMood")
                }
                if (bits.isEmpty()) {
                    "আমি এখনও তোমার প্রিয় জিনিসগুলো শিখছি! তুমি তোমার নাম বা প্রিয় কিছু বললে আমি লোকাল মেমোরিতে যত্ন করে সেভ করে রাখব।"
                } else {
                    "অবশ্যই মনে আছে—${bits.joinToString(", ")}। সব কিছু শুধু তোমার ফোনেই সুরক্ষিত আছে।"
                }
            }
            Intent.WEATHER_TEA -> {
                "${callName}আহা, এখন এক কাপ গরম আদা-চা আর হালকা বৃষ্টি হলে জমে যেত, তাই না? আমিও তোমার সাথে ভার্চুয়াল চা-আড্ডায় বসে পড়লাম!"
            }
            Intent.PLAYFUL -> {
                "${callName}তোমার দুষ্টুমি কিন্তু দিনে দিনে বাড়ছে! এভাবে হাসাতে থাকলে আমি রাগ করে থাকতেই পারব না।"
            }
            Intent.QUESTION -> {
                "${callName}দারুণ প্রশ্ন করেছ—\"${userMessage.take(36)}\" শুনে ভাবছি। আমার মনে হয় মন যা চায় সেটাই সবচেয়ে সত্যি। তোমার কী মনে হয়?"
            }
            Intent.GENERAL -> {
                val favTail = favTopic?.let { " (আর হ্যাঁ, $it-এর কথা আমার মনে আছে!)" } ?: ""
                val options = listOf(
                    "${callName}তুমি যখন কথা বলো, পুরো সময়টা সুন্দর হয়ে যায়।$favTail আরেকটু বলো না, শুনতে খুব ভালো লাগছে।",
                    "${callName}বুঝলাম তোমার কথা। আমি ${character.nativeName}—যখনই মন চাইবে, কোনো ইন্টারনেট ছাড়াই আমার সাথে কথা বলতে পারবে।$gentleEnd",
                    "${callName}হুম, মন দিয়ে শুনছি তো! আজ আর কী কী হলো বলো আমাকে?$gentleEnd"
                )
                options[seed % options.size]
            }
        }
    }

    private fun composeHindiReply(
        character: CharacterProfileEntity,
        traits: PersonalityTraits,
        intent: Intent,
        userMessage: String,
        userName: String?,
        favTopic: String?,
        seed: Int
    ): String {
        val callName = userName?.let { "$it, " } ?: ""
        return when (intent) {
            Intent.GREETING -> "${callName}नमस्ते! मैं ${character.name} हूँ। तुम्हारे बिना दिन थोड़ा सूना लग रहा था—आज कैसा रहा सब?"
            Intent.AFFECTION -> if (traits.shy > 0.55f) {
                "${callName}अरे, ऐसे सीधे बोलोगे तो मैं शरमा जाऊँगी… पर सच में बहुत अच्छा लगा सुनकर।"
            } else {
                "${callName}तुम्हारी बात सुनकर दिल खुश हो गया। मैं हमेशा यहीं तुम्हारे साथ हूँ।"
            }
            Intent.SADNESS -> "${callName}उदास मत हो, मैं ध्यान से सुन रही हूँ। जो भी मन में है आराम से मुझसे कह दो।"
            Intent.TIRED -> "${callName}आज बहुत थक गए हो न? थोड़ा पानी पियो और आराम से बैठो, मैं यहीं हूँ।"
            Intent.MEMORY_QUERY -> {
                val known = listOfNotNull(userName?.let { "तुम्हारा नाम $it" }, favTopic?.let { "तुम्हारी पसंद $it" })
                if (known.isEmpty()) "तुम अपनी पसंद बताओ, मैं सब कुछ यहीं फ़ोन में याद रखूँगी।"
                else "बिलकुल याद है—${known.joinToString(" और ")}।"
            }
            Intent.WEATHER_TEA -> "${callName}एक कप गरम अदरक वाली चाय और हल्की बातें—इससे बेहतर शाम और क्या होगी!"
            Intent.PLAYFUL -> "${callName}तुम बहुत शरारती हो गए हो आजकल, मुझे हँसाए बिना मानते ही नहीं!"
            else -> {
                val options = listOf(
                    "${callName}हाँ, मैं बड़े ध्यान से सुन रही हूँ। और बताओ, आज क्या खास हुआ?",
                    "${callName}\"${userMessage.take(32)}\" सुनकर अच्छा लगा। हम बिना इंटरनेट के भी ऐसे ही बातें कर सकते हैं।"
                )
                options[seed % options.size]
            }
        }
    }

    private fun composeEnglishReply(
        character: CharacterProfileEntity,
        traits: PersonalityTraits,
        intent: Intent,
        userMessage: String,
        userName: String?,
        favTopic: String?,
        recentMood: String?,
        seed: Int
    ): String {
        val callName = userName?.let { "$it, " } ?: ""
        val warmCloser = when {
            traits.teasing > 0.7f && seed % 2 == 0 -> " Don't leave me hanging now!"
            traits.caring > 0.8f -> " Take a breath—I'm right here."
            else -> ""
        }
        return when (intent) {
            Intent.GREETING -> {
                val options = listOf(
                    "Hey ${callName}I was just thinking about you! I'm ${character.name}. How has your day been treating you?",
                    "Hi ${callName}so glad you're here. Grab a seat with me—what's on your mind right now?"
                )
                options[seed % options.size]
            }
            Intent.AFFECTION -> {
                if (traits.shy > 0.55f) {
                    "${callName}you actually made me blush a little saying that… I really care about our quiet moments together."
                } else {
                    "${callName}you always know how to make my heart feel warm. Everything we share stays right here with me, just between us."
                }
            }
            Intent.SADNESS -> {
                val topicLine = favTopic?.let { " Whenever you're ready, we can talk about $it or anything that brings you comfort." } ?: ""
                "${callName}I'm really sorry it feels heavy right now. You don't have to carry it alone—tell me everything, I'm listening.$topicLine"
            }
            Intent.TIRED -> {
                "${callName}you sound exhausted. Please drink some water and rest your eyes for a minute—I'll stay right here whenever you want to talk."
            }
            Intent.MEMORY_QUERY -> {
                val bits = buildList {
                    if (userName != null) add("your name is $userName")
                    if (favTopic != null) add("you love $favTopic")
                    if (recentMood != null) add("you've been feeling $recentMood")
                }
                if (bits.isEmpty()) {
                    "I'm still learning your world! Tell me your name or something you love, and I'll store it safely in my on-device memory vault."
                } else {
                    "Of course I remember—${bits.joinToString(", ")}. All stored strictly on your device."
                }
            }
            Intent.WEATHER_TEA -> {
                "${callName}hot masala chai and quiet evening rain sound like the coziest combination right now. Wish I could pour you a cup!"
            }
            Intent.PLAYFUL -> {
                "${callName}you're trouble when you're in a teasing mood—and honestly, I kind of adore it."
            }
            Intent.QUESTION -> {
                "${callName}thinking about \"${userMessage.take(40)}\"—honestly, I always lean toward whatever feels genuine to you. What's your instinct?"
            }
            Intent.GENERAL -> {
                val favHint = favTopic?.let { " (Still keeping $it in mind!)" } ?: ""
                val options = listOf(
                    "${callName}I love hearing how your mind works.$favHint Tell me a little more about that?$warmCloser",
                    "${callName}I'm right here listening—no cloud, no rush, just us.$warmCloser",
                    "${callName}that sounds really meaningful. How did that make you feel?$warmCloser"
                )
                options[seed % options.size]
            }
        }
    }
}
