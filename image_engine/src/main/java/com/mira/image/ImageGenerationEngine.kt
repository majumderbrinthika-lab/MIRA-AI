package com.mira.image

import android.app.ActivityManager
import android.content.Context
import android.net.Uri
import com.mira.memory.model.StudioImageEntity

enum class StudioOutfit(val labelEn: String, val labelBn: String) {
    JAMDANI_SAREE("Traditional Jamdani Saree", "ঐতিহ্যবাহী জামদানি শাড়ি"),
    BANARASI_SILK("Crimson Banarasi Silk", "লাল বেনারসি সিল্ক"),
    SALWAR_KAMEEZ("Pastel Salwar Kameez", "সালোয়ার কামিজ"),
    INDO_WESTERN("Modern Indo-Western", "আধুনিক ইন্দো-ওয়েস্টার্ন"),
    COZY_CASUAL("Evening Cozy Sweater", "সন্ধ্যার আরামদায়ক পোশাক")
}

enum class StudioHairstyle(val labelEn: String, val labelBn: String) {
    OPEN_WAVES("Long Flowing Waves", "খোলা ঢেউখেলানো চুল"),
    JASMINE_BRAID("Braided Bun with Jasmine", "বেলি ফুলের খোঁপা ও বেণি"),
    SOFT_CURLS("Soft Romantic Curls", "হালকা কোঁকড়া চুল"),
    SLEEK_PONY("Sleek Low Ponytail", "মার্জিত লো পনিটেইল")
}

enum class StudioBackground(val labelEn: String, val labelBn: String) {
    MOONLIT_TERRACE("Moonlit Kolkata Terrace", "জ্যোৎস্না রাতের ছাদ"),
    RAINY_BALCONY("Rainy Evening Balcony", "বৃষ্টির সন্ধ্যায় বারান্দা"),
    CANDLELIT_ROOM("Warm Candlelit Studio", "মোমবাতির উষ্ণ আলো"),
    KASHFUL_MEADOW("Autumn Kashful Meadow", "শরতের কাশবন"),
    FAIRY_LIGHTS("Dreamy Fairy-Light Corner", "ফেয়ারি লাইটের কোণ")
}

enum class StudioPose(val labelEn: String, val labelBn: String) {
    WARM_SMILE("Warm Gentle Smile", "মিষ্টি হাসি"),
    SHY_GLANCE("Shy Side Glance", "লাজুক চাহনি"),
    ROMANTIC_GAZE("Deep Romantic Gaze", "গভীর রোমান্টিক দৃষ্টি"),
    PLAYFUL_WINK("Playful Cheerful Mood", "দুষ্টু মিষ্টি মেজাজ")
}

enum class StudioArtStyle(val labelEn: String, val labelBn: String) {
    PORTRAIT_PHOTO("Portrait Photography", "পোর্ট্রেট ফটোগ্রাফি"),
    REALISTIC_3D("Realistic 3D Render", "বাস্তবসম্মত থ্রিডি রেন্ডার"),
    GLAMOUR_STUDIO("Glamour Editorial Glow", "গ্ল্যামার ফটোগ্রাফি"),
    SOFT_WATERCOLOR("Romantic Watercolor", "রোমান্টিক জলরঙ")
}

enum class StudioResolution(val width: Int, val height: Int, val label: String) {
    COMPACT_512(512, 640, "512×640 (3GB Safe)"),
    HD_768(768, 960, "768×960 (Balanced)"),
    FULL_1024(1024, 1280, "1024×1280 (High)")
}

data class StudioGenerationRequest(
    val characterId: String,
    val characterName: String,
    val baseDrawableResId: Int,
    val referencePhotoUri: Uri? = null,
    val outfit: StudioOutfit,
    val hairstyle: StudioHairstyle,
    val background: StudioBackground,
    val pose: StudioPose,
    val artStyle: StudioArtStyle,
    val resolution: StudioResolution,
    val adultFictionalConsentConfirmed: Boolean
)

data class DeviceImageCapabilityReport(
    val totalRamMb: Int,
    val availableRamMb: Int,
    val isLowRamDevice: Boolean,
    val supportsHeavyStableDiffusion: Boolean,
    val feasibilityTitleEn: String,
    val feasibilityTitleBn: String,
    val explanationEn: String,
    val explanationBn: String
)

interface ImageGenerationEngine {
    fun inspectDeviceCapabilities(): DeviceImageCapabilityReport
    suspend fun generatePortrait(request: StudioGenerationRequest): Result<StudioImageEntity>
}

object DeviceImageCapabilityInspector {
    fun inspect(context: Context): DeviceImageCapabilityReport {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am?.getMemoryInfo(memInfo)

        val totalMb = ((memInfo.totalMem) / (1024L * 1024L)).toInt().coerceAtLeast(1024)
        val availMb = ((memInfo.availMem) / (1024L * 1024L)).toInt().coerceAtLeast(256)
        val is3GbTier = totalMb <= 3400 || memInfo.lowMemory

        return DeviceImageCapabilityReport(
            totalRamMb = totalMb,
            availableRamMb = availMb,
            isLowRamDevice = is3GbTier,
            supportsHeavyStableDiffusion = !is3GbTier && availMb >= 3200,
            feasibilityTitleEn = if (is3GbTier) {
                "3GB RAM Hardware Mode: Local Portrait Composer"
            } else {
                "On-Device Portrait Studio Ready"
            },
            feasibilityTitleBn = if (is3GbTier) {
                "৩ জিবি র‍্যাম মোড: লোকাল পোর্ট্রেট কম্পোজার"
            } else {
                "অন-ডিভাইস পোর্ট্রেট স্টুডিও প্রস্তুত"
            },
            explanationEn = "Full Stable Diffusion 1.5 / SDXL weights require 2.5–6GB storage and 4GB+ peak RAM, " +
                "which causes out-of-memory crashes on 3GB Android 8.1 phones. MIRA never fakes offline generation with a hidden cloud API. " +
                "Instead, this modular engine runs a 100% offline, memory-safe Canvas & Shader Portrait Studio using your authorized reference photo or South Asian presets.",
            explanationBn = "পূর্ণাঙ্গ Stable Diffusion 1.5 / SDXL মডেল চালাতে ২.৫–৬ জিবি ফাইল ও ৪ জিবি+ র‍্যাম লাগে, যা ৩ জিবি র‍্যাম Android 8.1 ফোনে ক্র্যাশ ঘটায়। " +
                "MIRA কখনোই গোপনে ক্লাউড API ব্যবহার করে না। এর পরিবর্তে এটি ১০০% অফলাইন ও র‍্যাম-নিরাপদ লোকাল পোর্ট্রেট স্টুডিও ইঞ্জিন চালায়।"
        )
    }
}
