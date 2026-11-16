package com.mira.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.net.Uri
import com.mira.memory.model.StudioImageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.cos
import kotlin.math.sin

/**
 * 100% offline, zero-cloud, Android 8.1 (API 27) compatible Portrait Studio composer.
 * Uses aggressive `inSampleSize` decoding + immediate `Bitmap.recycle()` so peak RAM stays
 * well under 45MB even on 3GB RAM devices.
 */
class LocalPortraitComposer(private val context: Context) : ImageGenerationEngine {

    private val galleryDir: File by lazy {
        File(context.filesDir, "studio_gallery").apply { mkdirs() }
    }

    override fun inspectDeviceCapabilities(): DeviceImageCapabilityReport =
        DeviceImageCapabilityInspector.inspect(context)

    override suspend fun generatePortrait(request: StudioGenerationRequest): Result<StudioImageEntity> =
        withContext(Dispatchers.Default) {
            if (!request.adultFictionalConsentConfirmed) {
                return@withContext Result.failure(
                    IllegalStateException("Please confirm this portrays a lawful adult fictional character.")
                )
            }

            var outputBitmap: Bitmap? = null
            var subjectBitmap: Bitmap? = null
            try {
                val cap = inspectDeviceCapabilities()
                val safeRes = if (cap.isLowRamDevice && request.resolution == StudioResolution.FULL_1024) {
                    StudioResolution.HD_768
                } else {
                    request.resolution
                }

                val w = safeRes.width
                val h = safeRes.height

                subjectBitmap = loadDownsampledSubject(
                    uri = request.referencePhotoUri,
                    fallbackResId = request.baseDrawableResId,
                    targetW = w,
                    targetH = h
                ) ?: return@withContext Result.failure(
                    IllegalStateException("Could not decode character portrait on low memory.")
                )

                outputBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(outputBitmap)

                // 1. Atmospheric background scene gradient
                drawAtmosphericBackground(canvas, w.toFloat(), h.toFloat(), request.background)

                // 2. Render styled portrait subject with selected Art Style filter
                drawStyledSubject(canvas, subjectBitmap, w.toFloat(), h.toFloat(), request.artStyle)

                // 3. Outfit color sash & traditional border accents
                drawOutfitAndHairAccents(canvas, w.toFloat(), h.toFloat(), request.outfit, request.hairstyle)

                // 4. Mood bokeh, fairy lights / rain / kashful / vignette & studio signature border
                drawSceneAtmosphere(canvas, w.toFloat(), h.toFloat(), request.background, request.pose)

                val fileName = "mira_studio_${System.currentTimeMillis()}.jpg"
                val outFile = File(galleryDir, fileName)
                FileOutputStream(outFile).use { fos ->
                    outputBitmap.compress(Bitmap.CompressFormat.JPEG, 88, fos)
                }

                val entity = StudioImageEntity(
                    characterId = request.characterId,
                    characterName = request.characterName,
                    outfit = request.outfit.labelEn,
                    hairstyle = request.hairstyle.labelEn,
                    background = request.background.labelEn,
                    pose = request.pose.labelEn,
                    artStyle = request.artStyle.labelEn,
                    resolutionLabel = "${w}×${h}",
                    filePath = outFile.absolutePath
                )
                Result.success(entity)
            } catch (oom: OutOfMemoryError) {
                System.gc()
                Result.failure(IllegalStateException("Low device memory prevented full resolution render. Try 512×640."))
            } catch (e: Exception) {
                Result.failure(e)
            } finally {
                subjectBitmap?.recycle()
                outputBitmap?.recycle()
            }
        }

    fun clearStudioCache(): Long {
        var reclaimed = 0L
        galleryDir.listFiles()?.forEach { file ->
            reclaimed += file.length()
            file.delete()
        }
        return reclaimed
    }

    private fun loadDownsampledSubject(
        uri: Uri?,
        fallbackResId: Int,
        targetW: Int,
        targetH: Int
    ): Bitmap? {
        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        if (uri != null) {
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, boundsOpts)
            }
        } else {
            BitmapFactory.decodeResource(context.resources, fallbackResId, boundsOpts)
        }

        val sampleSize = calculateInSampleSize(boundsOpts.outWidth, boundsOpts.outHeight, targetW, targetH)
        val decodeOpts = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }

        return if (uri != null) {
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, decodeOpts)
            }
        } else {
            BitmapFactory.decodeResource(context.resources, fallbackResId, decodeOpts)
        }
    }

    private fun calculateInSampleSize(srcW: Int, srcH: Int, reqW: Int, reqH: Int): Int {
        var inSampleSize = 1
        if (srcH > reqH || srcW > reqW) {
            val halfH = srcH / 2
            val halfW = srcW / 2
            while ((halfH / inSampleSize) >= reqH && (halfW / inSampleSize) >= reqW) {
                inSampleSize *= 2
            }
        }
        return inSampleSize.coerceAtLeast(1)
    }

    private fun drawAtmosphericBackground(canvas: Canvas, w: Float, h: Float, bg: StudioBackground) {
        val (topColor, bottomColor) = when (bg) {
            StudioBackground.MOONLIT_TERRACE -> Color.rgb(18, 13, 36) to Color.rgb(48, 22, 46)
            StudioBackground.RAINY_BALCONY -> Color.rgb(16, 28, 42) to Color.rgb(34, 26, 44)
            StudioBackground.CANDLELIT_ROOM -> Color.rgb(42, 16, 22) to Color.rgb(88, 38, 24)
            StudioBackground.KASHFUL_MEADOW -> Color.rgb(44, 58, 84) to Color.rgb(122, 92, 86)
            StudioBackground.FAIRY_LIGHTS -> Color.rgb(26, 12, 32) to Color.rgb(74, 28, 56)
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, 0f, 0f, h, topColor, bottomColor, Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, 0f, w, h, paint)
    }

    private fun drawStyledSubject(
        canvas: Canvas,
        subject: Bitmap,
        w: Float,
        h: Float,
        artStyle: StudioArtStyle
    ) {
        val srcRatio = subject.width.toFloat() / subject.height.toFloat().coerceAtLeast(1f)
        val dstRatio = w / h
        val destRect = if (srcRatio > dstRatio) {
            val scaledW = h * srcRatio
            val left = (w - scaledW) / 2f
            RectF(left, 0f, left + scaledW, h)
        } else {
            val scaledH = w / srcRatio
            val top = (h - scaledH) / 2f
            RectF(0f, top, w, top + scaledH)
        }

        val matrix = ColorMatrix()
        when (artStyle) {
            StudioArtStyle.PORTRAIT_PHOTO -> {
                matrix.setSaturation(1.08f)
            }
            StudioArtStyle.REALISTIC_3D -> {
                val contrast = ColorMatrix(
                    floatArrayOf(
                        1.14f, 0f, 0f, 0f, -12f,
                        0f, 1.12f, 0f, 0f, -10f,
                        0f, 0f, 1.16f, 0f, -8f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                matrix.postConcat(contrast)
            }
            StudioArtStyle.GLAMOUR_STUDIO -> {
                val warmGlow = ColorMatrix(
                    floatArrayOf(
                        1.10f, 0f, 0f, 0f, 12f,
                        0f, 1.03f, 0f, 0f, 6f,
                        0f, 0f, 0.96f, 0f, 2f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                matrix.postConcat(warmGlow)
            }
            StudioArtStyle.SOFT_WATERCOLOR -> {
                matrix.setSaturation(0.84f)
                val pastel = ColorMatrix(
                    floatArrayOf(
                        0.96f, 0f, 0f, 0f, 18f,
                        0f, 0.95f, 0f, 0f, 14f,
                        0f, 0f, 0.98f, 0f, 20f,
                        0f, 0f, 0f, 0.94f, 0f
                    )
                )
                matrix.postConcat(pastel)
            }
        }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(matrix)
        }
        canvas.drawBitmap(subject, null, destRect, paint)
    }

    private fun drawOutfitAndHairAccents(
        canvas: Canvas,
        w: Float,
        h: Float,
        outfit: StudioOutfit,
        hairstyle: StudioHairstyle
    ) {
        // Subtle silk/chiffon lower mood tint matching selected South Asian outfit palette
        val accentColor = when (outfit) {
            StudioOutfit.JAMDANI_SAREE -> Color.argb(56, 232, 184, 146)
            StudioOutfit.BANARASI_SILK -> Color.argb(66, 194, 34, 68)
            StudioOutfit.SALWAR_KAMEEZ -> Color.argb(52, 236, 146, 178)
            StudioOutfit.INDO_WESTERN -> Color.argb(54, 42, 152, 126)
            StudioOutfit.COZY_CASUAL -> Color.argb(52, 184, 134, 108)
        }
        val sashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, h * 0.66f, 0f, h,
                Color.TRANSPARENT, accentColor,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, h * 0.66f, w, h, sashPaint)

        // Delicate gold Jamdani / Banarasi filigree border along bottom edge
        if (outfit == StudioOutfit.JAMDANI_SAREE || outfit == StudioOutfit.BANARASI_SILK) {
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(120, 245, 208, 138)
                style = Paint.Style.STROKE
                strokeWidth = 3f
            }
            canvas.drawLine(w * 0.06f, h - 22f, w * 0.94f, h - 22f, borderPaint)
        }

        // White jasmine (gajra) blossoms halo accent for Jasmine Braid hairstyle
        if (hairstyle == StudioHairstyle.JASMINE_BRAID) {
            val blossomPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(150, 255, 250, 240)
            }
            for (i in 0..4) {
                val cx = w * 0.72f + (i * 14f)
                val cy = h * 0.16f + sin(i * 0.8f).toFloat() * 10f
                canvas.drawCircle(cx, cy, 5f, blossomPaint)
            }
        }
    }

    private fun drawSceneAtmosphere(
        canvas: Canvas,
        w: Float,
        h: Float,
        bg: StudioBackground,
        pose: StudioPose
    ) {
        // Warm golden-rose radial vignette
        val vignettePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                w * 0.5f,
                h * 0.42f,
                w * 0.75f,
                intArrayOf(Color.TRANSPARENT, Color.argb(145, 16, 8, 16)),
                floatArrayOf(0.55f, 1.0f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, w, h, vignettePaint)

        // Soft bokeh / rain / stardust accents
        val particlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        when (bg) {
            StudioBackground.RAINY_BALCONY -> {
                particlePaint.color = Color.argb(80, 200, 224, 255)
                particlePaint.strokeWidth = 2f
                for (i in 0..18) {
                    val x = ((i * 53) % w.toInt()).toFloat()
                    val y = ((i * 89) % h.toInt()).toFloat()
                    canvas.drawLine(x, y, x - 8f, y + 24f, particlePaint)
                }
            }
            StudioBackground.FAIRY_LIGHTS, StudioBackground.CANDLELIT_ROOM, StudioBackground.MOONLIT_TERRACE -> {
                for (i in 0..14) {
                    val angle = i * 0.7f
                    val px = w * (0.1f + ((cos(angle) + 1f) * 0.4f).toFloat())
                    val py = h * (0.08f + (i * 0.06f))
                    particlePaint.color = Color.argb(72, 255, 214, 165)
                    canvas.drawCircle(px, py, (6f + (i % 4) * 4f), particlePaint)
                }
            }
            StudioBackground.KASHFUL_MEADOW -> {
                particlePaint.color = Color.argb(85, 255, 248, 240)
                val path = Path()
                for (i in 0..6) {
                    val bx = w * (0.12f + i * 0.13f)
                    path.reset()
                    path.moveTo(bx, h)
                    path.quadTo(bx - 12f, h * 0.88f, bx + 8f, h * 0.80f)
                    canvas.drawCircle(bx + 8f, h * 0.80f, 10f, particlePaint)
                }
            }
        }

        // Romantic mood rim halo
        if (pose == StudioPose.ROMANTIC_GAZE || pose == StudioPose.SHY_GLANCE) {
            val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(
                    w * 0.5f, h * 0.3f, w * 0.45f,
                    Color.argb(38, 255, 160, 180), Color.TRANSPARENT,
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, w, h, haloPaint)
        }
    }
}
