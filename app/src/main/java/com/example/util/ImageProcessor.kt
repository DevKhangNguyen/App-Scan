package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

object ImageProcessor {

    enum class FilterMode {
        ORIGINAL,
        AUTO_ENHANCE,
        DOCUMENT,
        BLACK_AND_WHITE,
        GRAYSCALE,
        COLOR_BOOST
    }

    /**
     * Loads a bitmap from file path or URI safely with downsampling to avoid OOM.
     */
    suspend fun loadBitmap(
        context: Context,
        path: String,
        reqWidth: Int = 2048,
        reqHeight: Int = 2048
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            if (path.startsWith("content://") || path.startsWith("file://")) {
                context.contentResolver.openInputStream(Uri.parse(path))?.use {
                    BitmapFactory.decodeStream(it, null, options)
                }
            } else {
                BitmapFactory.decodeFile(path, options)
            }

            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
            options.inJustDecodeBounds = false
            options.inPreferredConfig = Bitmap.Config.ARGB_8888

            if (path.startsWith("content://") || path.startsWith("file://")) {
                context.contentResolver.openInputStream(Uri.parse(path))?.use {
                    BitmapFactory.decodeStream(it, null, options)
                }
            } else {
                BitmapFactory.decodeFile(path, options)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    /**
     * Automatic 4-corner document detection.
     * Analyzes image edges and luminance gradients to locate the document paper boundary.
     * Falls back to a clean 6% inset quadrilateral.
     */
    suspend fun detectCorners(bitmap: Bitmap): List<PointF> = withContext(Dispatchers.Default) {
        val w = bitmap.width.toFloat()
        val h = bitmap.height.toFloat()

        // Sample downscaled luminance grid to find contrast boundary
        val sampleW = min(200, bitmap.width)
        val sampleH = min(200, bitmap.height)
        val scaled = Bitmap.createScaledBitmap(bitmap, sampleW, sampleH, true)

        var minX = (sampleW * 0.08f).toInt()
        var maxX = (sampleW * 0.92f).toInt()
        var minY = (sampleH * 0.08f).toInt()
        var maxY = (sampleH * 0.92f).toInt()

        // Calculate average background luminance at four outer edges
        try {
            var edgeLumSum = 0L
            var edgeCount = 0
            for (x in 0 until sampleW step 5) {
                edgeLumSum += getLuminance(scaled.getPixel(x, 2))
                edgeLumSum += getLuminance(scaled.getPixel(x, sampleH - 3))
                edgeCount += 2
            }
            for (y in 0 until sampleH step 5) {
                edgeLumSum += getLuminance(scaled.getPixel(2, y))
                edgeLumSum += getLuminance(scaled.getPixel(sampleW - 3, y))
                edgeCount += 2
            }
            val edgeLum = if (edgeCount > 0) edgeLumSum / edgeCount else 100

            // Look for transition from dark table to bright document paper
            var foundMinY = minY
            for (y in 4 until sampleH / 2) {
                var rowLum = 0L
                for (x in sampleW / 4 until 3 * sampleW / 4 step 4) {
                    rowLum += getLuminance(scaled.getPixel(x, y))
                }
                val avgRow = rowLum / (sampleW / 8)
                if (kotlin.math.abs(avgRow - edgeLum) > 28) {
                    foundMinY = y
                    break
                }
            }

            var foundMaxY = maxY
            for (y in sampleH - 5 downTo sampleH / 2) {
                var rowLum = 0L
                for (x in sampleW / 4 until 3 * sampleW / 4 step 4) {
                    rowLum += getLuminance(scaled.getPixel(x, y))
                }
                val avgRow = rowLum / (sampleW / 8)
                if (kotlin.math.abs(avgRow - edgeLum) > 28) {
                    foundMaxY = y
                    break
                }
            }

            val scaleX = w / sampleW
            val scaleY = h / sampleH

            val left = (minX * scaleX).coerceAtLeast(w * 0.04f)
            val right = (maxX * scaleX).coerceAtMost(w * 0.96f)
            val top = (foundMinY * scaleY).coerceIn(h * 0.04f, h * 0.25f)
            val bottom = (foundMaxY * scaleY).coerceIn(h * 0.75f, h * 0.96f)

            listOf(
                PointF(left, top),       // Top-Left
                PointF(right, top),      // Top-Right
                PointF(right, bottom),   // Bottom-Right
                PointF(left, bottom)     // Bottom-Left
            )
        } catch (e: Exception) {
            // Default 6% inset
            listOf(
                PointF(w * 0.06f, h * 0.06f),
                PointF(w * 0.94f, h * 0.06f),
                PointF(w * 0.94f, h * 0.94f),
                PointF(w * 0.06f, h * 0.94f)
            )
        }
    }

    private fun getLuminance(pixel: Int): Int {
        val r = Color.red(pixel)
        val g = Color.green(pixel)
        val b = Color.blue(pixel)
        return (0.299 * r + 0.587 * g + 0.114 * b).toInt()
    }

    /**
     * Perspective crop using hardware accelerated Matrix.setPolyToPoly.
     * Takes any arbitrary 4 points [TL, TR, BR, BL] and straightens to a rectangular document.
     */
    suspend fun cropAndWarpPerspective(
        source: Bitmap,
        corners: List<PointF>
    ): Bitmap = withContext(Dispatchers.Default) {
        require(corners.size == 4) { "Must provide exactly 4 corner points" }

        val pTL = corners[0]
        val pTR = corners[1]
        val pBR = corners[2]
        val pBL = corners[3]

        // Calculate output dimensions based on max width and height of the quadrilateral
        val topWidth = hypot((pTR.x - pTL.x).toDouble(), (pTR.y - pTL.y).toDouble())
        val bottomWidth = hypot((pBR.x - pBL.x).toDouble(), (pBR.y - pBL.y).toDouble())
        val leftHeight = hypot((pBL.x - pTL.x).toDouble(), (pBL.y - pTL.y).toDouble())
        val rightHeight = hypot((pBR.x - pTR.x).toDouble(), (pBR.y - pTR.y).toDouble())

        val targetWidth = max(topWidth, bottomWidth).toFloat().coerceIn(100f, 4000f)
        val targetHeight = max(leftHeight, rightHeight).toFloat().coerceIn(100f, 4000f)

        val srcPoints = floatArrayOf(
            pTL.x, pTL.y,
            pTR.x, pTR.y,
            pBR.x, pBR.y,
            pBL.x, pBL.y
        )

        val dstPoints = floatArrayOf(
            0f, 0f,
            targetWidth, 0f,
            targetWidth, targetHeight,
            0f, targetHeight
        )

        val matrix = Matrix()
        matrix.setPolyToPoly(srcPoints, 0, dstPoints, 0, 4)

        val outputBitmap = Bitmap.createBitmap(
            targetWidth.toInt(),
            targetHeight.toInt(),
            Bitmap.Config.ARGB_8888
        )

        val canvas = Canvas(outputBitmap)
        val paint = Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
            isDither = true
        }

        canvas.drawBitmap(source, matrix, paint)
        outputBitmap
    }

    /**
     * Applies enhancement filters locally and offline.
     */
    suspend fun applyFilter(
        source: Bitmap,
        filterMode: FilterMode
    ): Bitmap = withContext(Dispatchers.Default) {
        if (filterMode == FilterMode.ORIGINAL) {
            return@withContext source
        }

        val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
        }

        val colorMatrix = when (filterMode) {
            FilterMode.ORIGINAL -> ColorMatrix()

            FilterMode.AUTO_ENHANCE -> {
                // Moderate contrast boost + slight saturation
                val cm = ColorMatrix()
                val contrast = 1.25f
                val brightness = 8f
                val scale = contrast
                val translate = (-0.5f * contrast + 0.5f) * 255f + brightness
                cm.set(
                    floatArrayOf(
                        scale, 0f, 0f, 0f, translate,
                        0f, scale, 0f, 0f, translate,
                        0f, 0f, scale, 0f, translate,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                cm
            }

            FilterMode.DOCUMENT -> {
                // High contrast paper brightening: washes out grey paper to white and sharpens ink
                val cm = ColorMatrix()
                val contrast = 1.6f
                val brightness = 24f
                val scale = contrast
                val translate = (-0.5f * contrast + 0.5f) * 255f + brightness
                cm.set(
                    floatArrayOf(
                        scale, 0f, 0f, 0f, translate,
                        0f, scale, 0f, 0f, translate,
                        0f, 0f, scale, 0f, translate,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                cm
            }

            FilterMode.BLACK_AND_WHITE -> {
                // First desaturate to grayscale, then high contrast thresholding
                val cm = ColorMatrix()
                cm.setSaturation(0f)
                val bwMatrix = ColorMatrix(
                    floatArrayOf(
                        2.8f, 0f, 0f, 0f, -220f,
                        0f, 2.8f, 0f, 0f, -220f,
                        0f, 0f, 2.8f, 0f, -220f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                cm.postConcat(bwMatrix)
                cm
            }

            FilterMode.GRAYSCALE -> {
                val cm = ColorMatrix()
                cm.setSaturation(0f)
                cm
            }

            FilterMode.COLOR_BOOST -> {
                val cm = ColorMatrix()
                cm.setSaturation(1.4f)
                val contrastMatrix = ColorMatrix(
                    floatArrayOf(
                        1.15f, 0f, 0f, 0f, -10f,
                        0f, 1.15f, 0f, 0f, -10f,
                        0f, 0f, 1.15f, 0f, -10f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                cm.postConcat(contrastMatrix)
                cm
            }
        }

        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(source, 0f, 0f, paint)
        result
    }

    /**
     * Rotates a bitmap by multiples of 90 degrees.
     */
    suspend fun rotateBitmap(source: Bitmap, degrees: Float): Bitmap = withContext(Dispatchers.Default) {
        if (degrees % 360f == 0f) return@withContext source
        val matrix = Matrix().apply { postRotate(degrees) }
        Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    /**
     * Saves bitmap to a private file.
     */
    suspend fun saveBitmapToFile(
        bitmap: Bitmap,
        file: File,
        format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG,
        quality: Int = 92
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            file.parentFile?.mkdirs()
            FileOutputStream(file).use { out ->
                bitmap.compress(format, quality, out)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
