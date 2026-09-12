package com.example.image

import android.content.Context
import android.graphics.*
import android.net.Uri
import androidx.core.graphics.drawable.toBitmap
import com.example.R
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.math.*
import kotlin.random.Random

object ImageProcessor {

    suspend fun renderIconBitmap(
        context: Context,
        project: IconProject,
        targetSize: Int = 512
    ): Bitmap = withContext(Dispatchers.Default) {
        val bitmap = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Draw Shape Mask Clipping Path
        val shapePath = createShapePath(targetSize.toFloat(), project.shape, project.cornerRadius)

        // Save canvas state before clipping
        canvas.save()
        canvas.clipPath(shapePath)

        // 2. Draw Background
        drawBackground(context, canvas, targetSize, project.background)

        // 3. Load & Process Source Artwork
        val sourceBitmap = loadSourceBitmap(context, project, targetSize)
        if (sourceBitmap != null) {
            val processedArt = applyEffectsAndFilters(sourceBitmap, project, targetSize)

            // Draw transformed artwork
            canvas.save()
            // Center-based transformation
            val cx = targetSize / 2f + (project.panX / 100f) * (targetSize / 2f)
            val cy = targetSize / 2f + (project.panY / 100f) * (targetSize / 2f)

            canvas.translate(cx, cy)
            canvas.rotate(project.rotation)
            canvas.scale(project.zoom, project.zoom)

            val artLeft = -targetSize / 2f
            val artTop = -targetSize / 2f
            canvas.drawBitmap(processedArt, artLeft, artTop, null)
            canvas.restore()
        }

        // 4. Draw Vignette & Grain if requested
        if (project.vignette > 0f) {
            drawVignette(canvas, targetSize, project.vignette)
        }
        if (project.grain > 0f || project.noise > 0f) {
            drawGrainAndNoise(canvas, targetSize, project.grain, project.noise)
        }
        if (project.halftone > 0f) {
            drawHalftoneOverlay(canvas, targetSize, project.halftone)
        }

        // 5. Draw App Symbol Overlay
        if (project.symbol != AppSymbol.NONE) {
            drawSymbol(canvas, targetSize, project)
        }

        // 6. Draw Text Overlay
        if (project.appText.isNotBlank()) {
            drawAppText(canvas, targetSize, project)
        }

        // Restore clipping
        canvas.restore()

        // 7. Draw Border (respecting shape)
        if (project.border != IconBorder.NONE && project.borderWidth > 0f) {
            drawBorder(canvas, targetSize, project, shapePath)
        }

        bitmap
    }

    private fun createShapePath(size: Float, shape: IconShape, cornerRadiusPercent: Float): Path {
        val path = Path()
        when (shape) {
            IconShape.SQUARE -> {
                path.addRect(0f, 0f, size, size, Path.Direction.CW)
            }
            IconShape.CIRCLE -> {
                path.addCircle(size / 2f, size / 2f, size / 2f, Path.Direction.CW)
            }
            IconShape.ROUNDED_SQUARE -> {
                val radius = (cornerRadiusPercent / 100f) * size
                val rect = RectF(0f, 0f, size, size)
                path.addRoundRect(rect, radius, radius, Path.Direction.CW)
            }
            IconShape.SQUIRCLE -> {
                // Continuous curved squircle
                val rect = RectF(0f, 0f, size, size)
                val radius = (cornerRadiusPercent / 100f * 1.3f).coerceIn(0.1f, 0.45f) * size
                path.addRoundRect(rect, radius, radius, Path.Direction.CW)
            }
        }
        return path
    }

    private fun drawBackground(context: Context, canvas: Canvas, size: Int, bg: IconBackground) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        when (bg) {
            IconBackground.BLACK -> {
                canvas.drawColor(Color.BLACK)
            }
            IconBackground.WHITE -> {
                canvas.drawColor(Color.WHITE)
            }
            IconBackground.DARK_GRAY -> {
                canvas.drawColor(Color.parseColor("#18181A"))
            }
            IconBackground.LIGHT_GRAY -> {
                canvas.drawColor(Color.parseColor("#DEDEDE"))
            }
            IconBackground.TRANSPARENT -> {
                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
            }
            IconBackground.NEWSPAPER_TEXTURE -> {
                try {
                    val opts = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.RGB_565 }
                    val newsBmp = BitmapFactory.decodeResource(context.resources, R.drawable.img_tex_news, opts)
                    if (newsBmp != null) {
                        val src = Rect(0, 0, newsBmp.width, newsBmp.height)
                        val dst = Rect(0, 0, size, size)
                        canvas.drawBitmap(newsBmp, src, dst, paint)
                    } else {
                        drawFallbackNewspaper(canvas, size)
                    }
                } catch (e: Exception) {
                    drawFallbackNewspaper(canvas, size)
                }
            }
            IconBackground.COMIC_TEXTURE -> {
                canvas.drawColor(Color.parseColor("#111111"))
                // Dot matrix texture
                paint.color = Color.parseColor("#22FFFFFF")
                val step = size / 24f
                for (x in 0..24) {
                    for (y in 0..24) {
                        canvas.drawCircle(x * step, y * step, step * 0.15f, paint)
                    }
                }
            }
        }
    }

    private fun drawFallbackNewspaper(canvas: Canvas, size: Int) {
        canvas.drawColor(Color.parseColor("#E5E2DC"))
        val paint = Paint().apply {
            color = Color.parseColor("#22000000")
            strokeWidth = 2f
        }
        for (i in 0..size step 16) {
            canvas.drawLine(0f, i.toFloat(), size.toFloat(), i.toFloat(), paint)
        }
    }

    private fun loadSourceBitmap(context: Context, project: IconProject, targetSize: Int): Bitmap? {
        if (!project.customImageUri.isNullOrEmpty()) {
            try {
                val uri = Uri.parse(project.customImageUri)
                val inputStream = context.contentResolver.openInputStream(uri)
                val opts = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeStream(inputStream, null, opts)
                inputStream?.close()

                var inSampleSize = 1
                while (opts.outWidth / (inSampleSize * 2) >= targetSize &&
                    opts.outHeight / (inSampleSize * 2) >= targetSize
                ) {
                    inSampleSize *= 2
                }

                val decodeOpts = BitmapFactory.Options().apply {
                    this.inSampleSize = inSampleSize
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                val stream2 = context.contentResolver.openInputStream(uri)
                val decoded = BitmapFactory.decodeStream(stream2, null, decodeOpts)
                stream2?.close()

                if (decoded != null) {
                    // Make square
                    val minDim = min(decoded.width, decoded.height)
                    val cropX = (decoded.width - minDim) / 2
                    val cropY = (decoded.height - minDim) / 2
                    val square = Bitmap.createBitmap(decoded, cropX, cropY, minDim, minDim)
                    return Bitmap.createScaledBitmap(square, targetSize, targetSize, true)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Fallback or built-in artwork
        return generateBuiltInArtwork(context, project.builtInArtwork, targetSize)
    }

    private fun generateBuiltInArtwork(context: Context, artwork: BuiltInArtwork, size: Int): Bitmap {
        when (artwork) {
            BuiltInArtwork.DARK_HERO -> {
                try {
                    val bmp = BitmapFactory.decodeResource(context.resources, R.drawable.img_art_hero)
                    if (bmp != null) {
                        return Bitmap.createScaledBitmap(bmp, size, size, true)
                    }
                } catch (_: Exception) {}
            }
            BuiltInArtwork.NEWSPAPER_COLLAGE -> {
                try {
                    val bmp = BitmapFactory.decodeResource(context.resources, R.drawable.img_tex_news)
                    if (bmp != null) {
                        return Bitmap.createScaledBitmap(bmp, size, size, true)
                    }
                } catch (_: Exception) {}
            }
            else -> {}
        }

        // Procedural vector illustration for built-in types
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }

        when (artwork) {
            BuiltInArtwork.BAT_SILHOUETTE -> {
                drawBatPath(canvas, size, paint)
            }
            BuiltInArtwork.COMIC_CAMERA -> {
                drawCameraGraphic(canvas, size, paint)
            }
            BuiltInArtwork.VINTAGE_CLOCK -> {
                drawClockGraphic(canvas, size, paint)
            }
            BuiltInArtwork.NOIR_MUSIC -> {
                drawMusicGraphic(canvas, size, paint)
            }
            BuiltInArtwork.DARK_CALENDAR -> {
                drawCalendarGraphic(canvas, size, paint)
            }
            BuiltInArtwork.COMIC_GAME -> {
                drawControllerGraphic(canvas, size, paint)
            }
            BuiltInArtwork.MINIMAL_NOTES -> {
                drawNotesGraphic(canvas, size, paint)
            }
            BuiltInArtwork.COMIC_BROWSER -> {
                drawBrowserGraphic(canvas, size, paint)
            }
            BuiltInArtwork.PHONE_DIALER -> {
                drawPhoneGraphic(canvas, size, paint)
            }
            BuiltInArtwork.SETTINGS_COG -> {
                drawCogGraphic(canvas, size, paint)
            }
            else -> {
                drawBatPath(canvas, size, paint)
            }
        }

        return bmp
    }

    private fun applyEffectsAndFilters(
        src: Bitmap,
        project: IconProject,
        targetSize: Int
    ): Bitmap {
        val result = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        // Color Matrix calculations
        val cm = ColorMatrix()

        // 1. Grayscale base
        cm.setSaturation(0f)

        // 2. Contrast & Brightness
        // contrast slider: -100 to 100 -> scale 0.2 to 3.0
        var contrastFactor = 1.0f + (project.contrast / 100f) * 1.5f
        if (project.filter == IconFilter.PURE_BW) contrastFactor *= 1.8f
        if (project.filter == IconFilter.COMIC_INK) contrastFactor *= 2.2f
        if (project.filter == IconFilter.DARK_COMIC) contrastFactor *= 2.5f

        val brightnessOffset = (project.brightness / 100f) * 100f + (project.exposure / 100f) * 60f

        val contrastMatrix = ColorMatrix(
            floatArrayOf(
                contrastFactor, 0f, 0f, 0f, brightnessOffset - (128f * (contrastFactor - 1f)),
                0f, contrastFactor, 0f, 0f, brightnessOffset - (128f * (contrastFactor - 1f)),
                0f, 0f, contrastFactor, 0f, brightnessOffset - (128f * (contrastFactor - 1f)),
                0f, 0f, 0f, 1f, 0f
            )
        )
        cm.postConcat(contrastMatrix)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(cm)
        }

        canvas.drawBitmap(src, 0f, 0f, paint)

        // Special filter post-processing (Posterization / Halftone / Threshold)
        when (project.filter) {
            IconFilter.COMIC_INK -> {
                applyPosterizeOrInk(result, project.threshold)
            }
            IconFilter.DARK_COMIC -> {
                applyDarkComicThreshold(result, project.threshold)
            }
            IconFilter.SKETCH -> {
                applySketchLineArt(result)
            }
            IconFilter.VINTAGE -> {
                applyVintageWash(result)
            }
            else -> {}
        }

        return result
    }

    private fun applyPosterizeOrInk(bitmap: Bitmap, thresholdVal: Float) {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val cutoff = (thresholdVal / 100f * 255f).toInt()

        for (i in pixels.indices) {
            val pixel = pixels[i]
            val a = (pixel shr 24) and 0xff
            if (a < 20) continue

            val r = (pixel shr 16) and 0xff
            val g = (pixel shr 8) and 0xff
            val b = pixel and 0xff
            val gray = (r * 0.299 + g * 0.587 + b * 0.114).toInt()

            // 3-step comic ink posterization
            val newGray = when {
                gray < cutoff * 0.5f -> 0
                gray < cutoff -> 60
                gray < cutoff * 1.3f -> 190
                else -> 255
            }
            pixels[i] = (a shl 24) or (newGray shl 16) or (newGray shl 8) or newGray
        }

        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
    }

    private fun applyDarkComicThreshold(bitmap: Bitmap, thresholdVal: Float) {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val threshold = (thresholdVal / 100f * 255f).toInt().coerceIn(40, 220)

        for (i in pixels.indices) {
            val pixel = pixels[i]
            val a = (pixel shr 24) and 0xff
            if (a < 20) continue

            val r = (pixel shr 16) and 0xff
            val g = (pixel shr 8) and 0xff
            val b = pixel and 0xff
            val gray = (r * 0.299 + g * 0.587 + b * 0.114).toInt()

            // Stark binary threshold with soft edge
            val out = if (gray > threshold) 255 else 0
            pixels[i] = (a shl 24) or (out shl 16) or (out shl 8) or out
        }
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
    }

    private fun applySketchLineArt(bitmap: Bitmap) {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        val copy = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        System.arraycopy(pixels, 0, copy, 0, pixels.size)

        // Simple Sobel edge gradient
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val idx = y * width + x
                val a = (pixels[idx] shr 24) and 0xff
                if (a < 20) continue

                val left = (pixels[idx - 1] and 0xff)
                val right = (pixels[idx + 1] and 0xff)
                val top = (pixels[idx - width] and 0xff)
                val bottom = (pixels[idx + width] and 0xff)

                val diff = abs(right - left) + abs(bottom - top)
                val edge = (255 - diff * 3).coerceIn(0, 255)
                copy[idx] = (a shl 24) or (edge shl 16) or (edge shl 8) or edge
            }
        }
        bitmap.setPixels(copy, 0, width, 0, 0, width, height)
    }

    private fun applyVintageWash(bitmap: Bitmap) {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        for (i in pixels.indices) {
            val pixel = pixels[i]
            val a = (pixel shr 24) and 0xff
            if (a < 20) continue

            val r = (pixel shr 16) and 0xff
            val g = (pixel shr 8) and 0xff
            val b = pixel and 0xff
            val gray = (r * 0.299 + g * 0.587 + b * 0.114).toInt()

            // Compress dynamic range for faded print look
            val faded = (35 + gray * 0.8f).toInt().coerceIn(0, 255)
            pixels[i] = (a shl 24) or (faded shl 16) or (faded shl 8) or faded
        }
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
    }

    private fun drawVignette(canvas: Canvas, size: Int, intensity: Float) {
        val radius = size * 0.75f
        val alpha = ((intensity / 100f) * 220f).toInt().coerceIn(0, 255)
        val shader = RadialGradient(
            size / 2f, size / 2f, radius,
            intArrayOf(Color.TRANSPARENT, Color.argb(alpha / 2, 0, 0, 0), Color.argb(alpha, 0, 0, 0)),
            floatArrayOf(0.4f, 0.75f, 1.0f),
            Shader.TileMode.CLAMP
        )
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.shader = shader
        }
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)
    }

    private fun drawGrainAndNoise(canvas: Canvas, size: Int, grain: Float, noise: Float) {
        val total = (grain + noise).coerceIn(0f, 100f)
        val count = (size * size * (total / 100f) * 0.04f).toInt()
        val paint = Paint().apply {
            strokeWidth = if (grain > 40f) 2f else 1f
        }
        val rand = Random(42)
        for (i in 0 until count) {
            val x = rand.nextFloat() * size
            val y = rand.nextFloat() * size
            val isWhite = rand.nextBoolean()
            val alpha = rand.nextInt(30, 110)
            paint.color = if (isWhite) Color.argb(alpha, 255, 255, 255) else Color.argb(alpha, 0, 0, 0)
            canvas.drawPoint(x, y, paint)
        }
    }

    private fun drawHalftoneOverlay(canvas: Canvas, size: Int, amount: Float) {
        val step = (size / 36f).coerceAtLeast(6f)
        val maxR = step * 0.45f * (amount / 100f)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(80, 0, 0, 0)
            style = Paint.Style.FILL
        }
        for (x in 0..36) {
            for (y in 0..36) {
                val cx = x * step
                val cy = y * step
                canvas.drawCircle(cx, cy, maxR, paint)
            }
        }
    }

    private fun drawSymbol(canvas: Canvas, size: Int, project: IconProject) {
        val symbol = project.symbol
        if (symbol == AppSymbol.NONE) return

        canvas.save()

        val scale = (project.symbolSize / 100f)
        val baseDim = size * scale

        // Calculate center based on position
        val padding = size * 0.22f
        val (cx, cy) = when (project.symbolPosition) {
            SymbolPosition.CENTER -> Pair(size / 2f, size / 2f)
            SymbolPosition.TOP_RIGHT -> Pair(size - padding, padding)
            SymbolPosition.BOTTOM_RIGHT -> Pair(size - padding, size - padding)
            SymbolPosition.BOTTOM_LEFT -> Pair(padding, size - padding)
            SymbolPosition.TOP_LEFT -> Pair(padding, padding)
        }

        canvas.translate(cx, cy)
        canvas.rotate(project.symbolRotation)

        val alpha = (project.symbolOpacity * 255).toInt().coerceIn(0, 255)
        val color = if (project.isSymbolWhite) Color.argb(alpha, 255, 255, 255) else Color.argb(alpha, 10, 10, 10)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.FILL
        }

        // Draw symbol centered
        canvas.save()
        canvas.translate(-baseDim / 2f, -baseDim / 2f)
        drawSymbolGraphic(canvas, symbol, baseDim.toInt(), paint)
        canvas.restore()

        canvas.restore()
    }

    private fun drawSymbolGraphic(canvas: Canvas, symbol: AppSymbol, size: Int, paint: Paint) {
        when (symbol) {
            AppSymbol.BAT -> drawBatPath(canvas, size, paint)
            AppSymbol.CAMERA -> drawCameraGraphic(canvas, size, paint)
            AppSymbol.CALENDAR -> drawCalendarGraphic(canvas, size, paint)
            AppSymbol.MAIL -> drawMailGraphic(canvas, size, paint)
            AppSymbol.MUSIC -> drawMusicGraphic(canvas, size, paint)
            AppSymbol.PHONE -> drawPhoneGraphic(canvas, size, paint)
            AppSymbol.SETTINGS -> drawCogGraphic(canvas, size, paint)
            AppSymbol.NOTES -> drawNotesGraphic(canvas, size, paint)
            AppSymbol.CALCULATOR -> drawCalcGraphic(canvas, size, paint)
            AppSymbol.BROWSER -> drawBrowserGraphic(canvas, size, paint)
            AppSymbol.GAME -> drawControllerGraphic(canvas, size, paint)
            AppSymbol.USER -> drawUserGraphic(canvas, size, paint)
            AppSymbol.STAR -> drawStarGraphic(canvas, size, paint)
            AppSymbol.HEART -> drawHeartGraphic(canvas, size, paint)
            AppSymbol.CHAT -> drawChatGraphic(canvas, size, paint)
            AppSymbol.CODE -> drawCodeGraphic(canvas, size, paint)
            AppSymbol.SHIELD -> drawShieldGraphic(canvas, size, paint)
            AppSymbol.BOLT -> drawBoltGraphic(canvas, size, paint)
            else -> {}
        }
    }

    private fun drawAppText(canvas: Canvas, size: Int, project: IconProject) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = (project.fontSize / 100f) * size * 0.9f
            typeface = Typeface.create(Typeface.SANS_SERIF, if (project.isTextBold) Typeface.BOLD else Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
            alpha = (project.textOpacity * 255).toInt().coerceIn(0, 255)
            letterSpacing = project.letterSpacing * 0.05f
        }

        val text = if (project.isUppercase) project.appText.uppercase() else project.appText

        // Drop shadow for crisp legibility over ink art
        val shadowPaint = Paint(paint).apply {
            color = Color.BLACK
            alpha = (project.textOpacity * 220).toInt()
        }

        val x = size / 2f
        val y = size * 0.88f

        canvas.drawText(text, x + 2f, y + 2f, shadowPaint)
        canvas.drawText(text, x, y, paint)
    }

    private fun drawBorder(canvas: Canvas, size: Int, project: IconProject, shapePath: Path) {
        val border = project.border
        val strokeW = (project.borderWidth / 100f) * size * 0.4f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = strokeW
        }

        when (border) {
            IconBorder.THIN_WHITE -> {
                paint.color = Color.WHITE
                canvas.drawPath(shapePath, paint)
            }
            IconBorder.THIN_BLACK -> {
                paint.color = Color.BLACK
                canvas.drawPath(shapePath, paint)
            }
            IconBorder.DOUBLE -> {
                // Outer
                paint.color = Color.WHITE
                paint.strokeWidth = strokeW * 0.5f
                canvas.drawPath(shapePath, paint)
                // Inner
                val innerInset = strokeW * 1.5f
                val innerPath = createShapePath(size - innerInset * 2, project.shape, project.cornerRadius)
                canvas.save()
                canvas.translate(innerInset, innerInset)
                canvas.drawPath(innerPath, paint)
                canvas.restore()
            }
            IconBorder.COMIC_INK -> {
                // Thick rough comic panel border
                paint.color = Color.WHITE
                paint.strokeWidth = strokeW * 1.4f
                canvas.drawPath(shapePath, paint)

                val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.BLACK
                    style = Paint.Style.STROKE
                    strokeWidth = strokeW * 0.4f
                }
                canvas.drawPath(shapePath, accentPaint)
            }
            IconBorder.ROUNDED -> {
                paint.color = Color.argb(180, 255, 255, 255)
                canvas.drawPath(shapePath, paint)
            }
            else -> {}
        }
    }

    // --- Vector Graphics for Symbols & Built-in Artwork ---

    private fun drawBatPath(canvas: Canvas, size: Int, paint: Paint) {
        val s = size.toFloat()
        val path = Path().apply {
            moveTo(s * 0.5f, s * 0.36f)
            lineTo(s * 0.52f, s * 0.28f)
            lineTo(s * 0.54f, s * 0.35f)
            cubicTo(s * 0.62f, s * 0.33f, s * 0.72f, s * 0.26f, s * 0.96f, s * 0.36f)
            cubicTo(s * 0.88f, s * 0.46f, s * 0.84f, s * 0.60f, s * 0.88f, s * 0.74f)
            cubicTo(s * 0.74f, s * 0.68f, s * 0.64f, s * 0.72f, s * 0.58f, s * 0.82f)
            cubicTo(s * 0.54f, s * 0.76f, s * 0.52f, s * 0.74f, s * 0.50f, s * 0.86f)
            cubicTo(s * 0.48f, s * 0.74f, s * 0.46f, s * 0.76f, s * 0.42f, s * 0.82f)
            cubicTo(s * 0.36f, s * 0.72f, s * 0.26f, s * 0.68f, s * 0.12f, s * 0.74f)
            cubicTo(s * 0.16f, s * 0.60f, s * 0.12f, s * 0.46f, s * 0.04f, s * 0.36f)
            cubicTo(s * 0.28f, s * 0.26f, s * 0.38f, s * 0.33f, s * 0.46f, s * 0.35f)
            lineTo(s * 0.48f, s * 0.28f)
            close()
        }
        canvas.drawPath(path, paint)
    }

    private fun drawCameraGraphic(canvas: Canvas, size: Int, paint: Paint) {
        val s = size.toFloat()
        // Body
        val rect = RectF(s * 0.15f, s * 0.32f, s * 0.85f, s * 0.82f)
        canvas.drawRoundRect(rect, s * 0.08f, s * 0.08f, paint)
        // Flash top
        val topRect = RectF(s * 0.36f, s * 0.22f, s * 0.64f, s * 0.32f)
        canvas.drawRoundRect(topRect, s * 0.04f, s * 0.04f, paint)
        // Lens hole
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            style = Paint.Style.FILL
        }
        canvas.drawCircle(s * 0.5f, s * 0.57f, s * 0.18f, bgPaint)
        canvas.drawCircle(s * 0.5f, s * 0.57f, s * 0.09f, paint)
    }

    private fun drawCalendarGraphic(canvas: Canvas, size: Int, paint: Paint) {
        val s = size.toFloat()
        val rect = RectF(s * 0.18f, s * 0.24f, s * 0.82f, s * 0.82f)
        canvas.drawRoundRect(rect, s * 0.08f, s * 0.08f, paint)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            style = Paint.Style.FILL
        }
        // Top rings
        canvas.drawCircle(s * 0.35f, s * 0.20f, s * 0.05f, paint)
        canvas.drawCircle(s * 0.65f, s * 0.20f, s * 0.05f, paint)

        // Banner bar
        canvas.drawRect(s * 0.22f, s * 0.38f, s * 0.78f, s * 0.42f, bgPaint)

        // Date grid dots
        val step = s * 0.13f
        for (row in 0..1) {
            for (col in 0..3) {
                canvas.drawCircle(s * 0.30f + col * step, s * 0.54f + row * step, s * 0.035f, bgPaint)
            }
        }
    }

    private fun drawMailGraphic(canvas: Canvas, size: Int, paint: Paint) {
        val s = size.toFloat()
        val rect = RectF(s * 0.15f, s * 0.30f, s * 0.85f, s * 0.75f)
        canvas.drawRoundRect(rect, s * 0.06f, s * 0.06f, paint)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = s * 0.04f
        }
        val path = Path().apply {
            moveTo(s * 0.18f, s * 0.34f)
            lineTo(s * 0.50f, s * 0.55f)
            lineTo(s * 0.82f, s * 0.34f)
        }
        canvas.drawPath(path, bgPaint)
    }

    private fun drawMusicGraphic(canvas: Canvas, size: Int, paint: Paint) {
        val s = size.toFloat()
        // Two connected musical notes
        canvas.drawCircle(s * 0.32f, s * 0.72f, s * 0.12f, paint)
        canvas.drawCircle(s * 0.70f, s * 0.62f, s * 0.12f, paint)

        val strokePaint = Paint(paint).apply {
            style = Paint.Style.STROKE
            strokeWidth = s * 0.06f
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawLine(s * 0.40f, s * 0.72f, s * 0.40f, s * 0.26f, strokePaint)
        canvas.drawLine(s * 0.78f, s * 0.62f, s * 0.78f, s * 0.18f, strokePaint)
        canvas.drawLine(s * 0.40f, s * 0.26f, s * 0.78f, s * 0.18f, strokePaint)
    }

    private fun drawPhoneGraphic(canvas: Canvas, size: Int, paint: Paint) {
        val s = size.toFloat()
        val path = Path().apply {
            moveTo(s * 0.26f, s * 0.42f)
            cubicTo(s * 0.26f, s * 0.32f, s * 0.36f, s * 0.24f, s * 0.46f, s * 0.24f)
            lineTo(s * 0.50f, s * 0.36f)
            lineTo(s * 0.42f, s * 0.44f)
            cubicTo(s * 0.46f, s * 0.54f, s * 0.54f, s * 0.62f, s * 0.64f, s * 0.66f)
            lineTo(s * 0.72f, s * 0.58f)
            lineTo(s * 0.84f, s * 0.62f)
            cubicTo(s * 0.84f, s * 0.72f, s * 0.76f, s * 0.82f, s * 0.66f, s * 0.82f)
            cubicTo(s * 0.42f, s * 0.82f, s * 0.26f, s * 0.66f, s * 0.26f, s * 0.42f)
            close()
        }
        canvas.drawPath(path, paint)
    }

    private fun drawCogGraphic(canvas: Canvas, size: Int, paint: Paint) {
        val s = size.toFloat()
        val cx = s / 2f
        val cy = s / 2f
        val outerR = s * 0.36f
        val innerR = s * 0.28f

        val path = Path()
        val teeth = 8
        for (i in 0 until teeth) {
            val angle = (i * 2 * Math.PI / teeth).toFloat()
            val step = (Math.PI / teeth).toFloat()
            val x1 = cx + outerR * cos(angle)
            val y1 = cy + outerR * sin(angle)
            val x2 = cx + innerR * cos(angle + step * 0.5f)
            val y2 = cy + innerR * sin(angle + step * 0.5f)
            if (i == 0) path.moveTo(x1, y1) else path.lineTo(x1, y1)
            path.lineTo(x2, y2)
        }
        path.close()
        canvas.drawPath(path, paint)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, s * 0.12f, bgPaint)
    }

    private fun drawNotesGraphic(canvas: Canvas, size: Int, paint: Paint) {
        val s = size.toFloat()
        val rect = RectF(s * 0.22f, s * 0.18f, s * 0.78f, s * 0.82f)
        canvas.drawRoundRect(rect, s * 0.06f, s * 0.06f, paint)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            strokeWidth = s * 0.04f
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawLine(s * 0.32f, s * 0.34f, s * 0.68f, s * 0.34f, bgPaint)
        canvas.drawLine(s * 0.32f, s * 0.48f, s * 0.68f, s * 0.48f, bgPaint)
        canvas.drawLine(s * 0.32f, s * 0.62f, s * 0.54f, s * 0.62f, bgPaint)
    }

    private fun drawCalcGraphic(canvas: Canvas, size: Int, paint: Paint) {
        val s = size.toFloat()
        val rect = RectF(s * 0.20f, s * 0.18f, s * 0.80f, s * 0.82f)
        canvas.drawRoundRect(rect, s * 0.08f, s * 0.08f, paint)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            style = Paint.Style.FILL
        }
        // Screen
        canvas.drawRoundRect(RectF(s * 0.28f, s * 0.26f, s * 0.72f, s * 0.40f), s * 0.04f, s * 0.04f, bgPaint)
        // Buttons
        for (r in 0..2) {
            for (c in 0..2) {
                canvas.drawCircle(s * 0.34f + c * s * 0.16f, s * 0.50f + r * s * 0.11f, s * 0.04f, bgPaint)
            }
        }
    }

    private fun drawBrowserGraphic(canvas: Canvas, size: Int, paint: Paint) {
        val s = size.toFloat()
        val rect = RectF(s * 0.16f, s * 0.22f, s * 0.84f, s * 0.78f)
        canvas.drawRoundRect(rect, s * 0.08f, s * 0.08f, paint)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            style = Paint.Style.FILL
        }
        canvas.drawRect(s * 0.16f, s * 0.36f, s * 0.84f, s * 0.78f, bgPaint)

        // Three window buttons
        canvas.drawCircle(s * 0.26f, s * 0.29f, s * 0.03f, bgPaint)
        canvas.drawCircle(s * 0.35f, s * 0.29f, s * 0.03f, bgPaint)
        canvas.drawCircle(s * 0.44f, s * 0.29f, s * 0.03f, bgPaint)

        // Globe / Compass mark inside
        val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = s * 0.035f
        }
        canvas.drawCircle(s * 0.50f, s * 0.57f, s * 0.12f, innerPaint)
        canvas.drawLine(s * 0.50f, s * 0.45f, s * 0.50f, s * 0.69f, innerPaint)
        canvas.drawLine(s * 0.38f, s * 0.57f, s * 0.62f, s * 0.57f, innerPaint)
    }

    private fun drawControllerGraphic(canvas: Canvas, size: Int, paint: Paint) {
        val s = size.toFloat()
        val rect = RectF(s * 0.15f, s * 0.32f, s * 0.85f, s * 0.72f)
        canvas.drawRoundRect(rect, s * 0.20f, s * 0.20f, paint)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            style = Paint.Style.FILL
        }
        // D-Pad left
        canvas.drawRect(s * 0.26f, s * 0.48f, s * 0.40f, s * 0.56f, bgPaint)
        canvas.drawRect(s * 0.30f, s * 0.42f, s * 0.36f, s * 0.62f, bgPaint)
        // Buttons right
        canvas.drawCircle(s * 0.72f, s * 0.46f, s * 0.04f, bgPaint)
        canvas.drawCircle(s * 0.64f, s * 0.54f, s * 0.04f, bgPaint)
        canvas.drawCircle(s * 0.72f, s * 0.60f, s * 0.04f, bgPaint)
    }

    private fun drawUserGraphic(canvas: Canvas, size: Int, paint: Paint) {
        val s = size.toFloat()
        // Head
        canvas.drawCircle(s * 0.50f, s * 0.36f, s * 0.14f, paint)
        // Shoulders
        val rect = RectF(s * 0.22f, s * 0.56f, s * 0.78f, s * 0.90f)
        canvas.drawRoundRect(rect, s * 0.25f, s * 0.25f, paint)
    }

    private fun drawStarGraphic(canvas: Canvas, size: Int, paint: Paint) {
        val s = size.toFloat()
        val cx = s / 2f
        val cy = s / 2f
        val path = Path()
        val points = 5
        val outerR = s * 0.38f
        val innerR = s * 0.18f

        for (i in 0 until points * 2) {
            val r = if (i % 2 == 0) outerR else innerR
            val angle = (i * Math.PI / points - Math.PI / 2).toFloat()
            val x = cx + r * cos(angle)
            val y = cy + r * sin(angle)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        canvas.drawPath(path, paint)
    }

    private fun drawHeartGraphic(canvas: Canvas, size: Int, paint: Paint) {
        val s = size.toFloat()
        val path = Path().apply {
            moveTo(s * 0.5f, s * 0.78f)
            cubicTo(s * 0.15f, s * 0.55f, s * 0.12f, s * 0.25f, s * 0.32f, s * 0.25f)
            cubicTo(s * 0.42f, s * 0.25f, s * 0.48f, s * 0.32f, s * 0.5f, s * 0.40f)
            cubicTo(s * 0.52f, s * 0.32f, s * 0.58f, s * 0.25f, s * 0.68f, s * 0.25f)
            cubicTo(s * 0.88f, s * 0.25f, s * 0.85f, s * 0.55f, s * 0.5f, s * 0.78f)
            close()
        }
        canvas.drawPath(path, paint)
    }

    private fun drawChatGraphic(canvas: Canvas, size: Int, paint: Paint) {
        val s = size.toFloat()
        val rect = RectF(s * 0.18f, s * 0.24f, s * 0.82f, s * 0.68f)
        canvas.drawRoundRect(rect, s * 0.15f, s * 0.15f, paint)

        val tail = Path().apply {
            moveTo(s * 0.30f, s * 0.65f)
            lineTo(s * 0.22f, s * 0.82f)
            lineTo(s * 0.45f, s * 0.68f)
            close()
        }
        canvas.drawPath(tail, paint)
    }

    private fun drawCodeGraphic(canvas: Canvas, size: Int, paint: Paint) {
        val s = size.toFloat()
        val strokePaint = Paint(paint).apply {
            style = Paint.Style.STROKE
            strokeWidth = s * 0.06f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        // <
        val path1 = Path().apply {
            moveTo(s * 0.38f, s * 0.35f)
            lineTo(s * 0.24f, s * 0.50f)
            lineTo(s * 0.38f, s * 0.65f)
        }
        canvas.drawPath(path1, strokePaint)

        // >
        val path2 = Path().apply {
            moveTo(s * 0.62f, s * 0.35f)
            lineTo(s * 0.76f, s * 0.50f)
            lineTo(s * 0.62f, s * 0.65f)
        }
        canvas.drawPath(path2, strokePaint)

        // /
        canvas.drawLine(s * 0.55f, s * 0.30f, s * 0.45f, s * 0.70f, strokePaint)
    }

    private fun drawShieldGraphic(canvas: Canvas, size: Int, paint: Paint) {
        val s = size.toFloat()
        val path = Path().apply {
            moveTo(s * 0.50f, s * 0.20f)
            lineTo(s * 0.80f, s * 0.30f)
            cubicTo(s * 0.80f, s * 0.62f, s * 0.65f, s * 0.76f, s * 0.50f, s * 0.84f)
            cubicTo(s * 0.35f, s * 0.76f, s * 0.20f, s * 0.62f, s * 0.20f, s * 0.30f)
            close()
        }
        canvas.drawPath(path, paint)
    }

    private fun drawBoltGraphic(canvas: Canvas, size: Int, paint: Paint) {
        val s = size.toFloat()
        val path = Path().apply {
            moveTo(s * 0.56f, s * 0.18f)
            lineTo(s * 0.30f, s * 0.52f)
            lineTo(s * 0.48f, s * 0.52f)
            lineTo(s * 0.42f, s * 0.82f)
            lineTo(s * 0.72f, s * 0.44f)
            lineTo(s * 0.54f, s * 0.44f)
            close()
        }
        canvas.drawPath(path, paint)
    }

    private fun drawClockGraphic(canvas: Canvas, size: Int, paint: Paint) {
        val s = size.toFloat()
        val cx = s / 2f
        val cy = s / 2f
        canvas.drawCircle(cx, cy, s * 0.34f, paint)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = s * 0.05f
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawLine(cx, cy, cx, cy - s * 0.18f, bgPaint)
        canvas.drawLine(cx, cy, cx + s * 0.14f, cy, bgPaint)
        canvas.drawCircle(cx, cy, s * 0.04f, Paint(bgPaint).apply { style = Paint.Style.FILL })
    }

    // Save Bitmap as exported PNG to file and return File
    suspend fun exportToPngFile(context: Context, bitmap: Bitmap, fileName: String): File = withContext(Dispatchers.IO) {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(exportDir, "$fileName.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        file
    }
}
