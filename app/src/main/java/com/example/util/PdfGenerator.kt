package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import kotlin.math.min

object PdfGenerator {

    enum class PageSize(val title: String, val widthPt: Int, val heightPt: Int) {
        A4("A4 (210 x 297 mm)", 595, 842),
        A5("A5 (148 x 210 mm)", 420, 595),
        LETTER("US Letter", 612, 792),
        ORIGINAL("Tỷ lệ gốc", 0, 0)
    }

    enum class PdfQuality(val title: String, val compression: Int) {
        HIGH("Cao (Gốc)", 95),
        MEDIUM("Tiêu chuẩn", 80),
        COMPACT("Nén tiết kiệm", 60)
    }

    /**
     * Generates a multi-page PDF document from a list of image paths.
     */
    suspend fun generatePdf(
        context: Context,
        imagePaths: List<String>,
        outputFile: File,
        pageSize: PageSize = PageSize.A4,
        quality: PdfQuality = PdfQuality.HIGH,
        includePageNumbers: Boolean = true
    ): Boolean = withContext(Dispatchers.IO) {
        if (imagePaths.isEmpty()) return@withContext false

        val pdfDocument = PdfDocument()
        val textPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 10f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        val bitmapPaint = Paint().apply {
            isFilterBitmap = true
            isAntiAlias = true
        }

        try {
            outputFile.parentFile?.mkdirs()

            imagePaths.forEachIndexed { index, path ->
                val sourceBitmap = BitmapFactory.decodeFile(path) ?: return@forEachIndexed

                val pageW: Int
                val pageH: Int

                if (pageSize == PageSize.ORIGINAL) {
                    pageW = sourceBitmap.width.coerceAtLeast(300)
                    pageH = sourceBitmap.height.coerceAtLeast(300)
                } else {
                    pageW = pageSize.widthPt
                    pageH = pageSize.heightPt
                }

                val pageInfo = PdfDocument.PageInfo.Builder(pageW, pageH, index + 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas

                // Fill white background
                canvas.drawColor(Color.WHITE)

                val bottomMargin = if (includePageNumbers) 30f else 15f
                val topMargin = 15f
                val sideMargin = 15f

                val availW = pageW - (sideMargin * 2)
                val availH = pageH - topMargin - bottomMargin

                // Calculate aspect ratio fit
                val scale = min(availW / sourceBitmap.width.toFloat(), availH / sourceBitmap.height.toFloat())
                val destW = sourceBitmap.width * scale
                val destH = sourceBitmap.height * scale

                val destLeft = sideMargin + (availW - destW) / 2f
                val destTop = topMargin + (availH - destH) / 2f

                val destRect = Rect(
                    destLeft.toInt(),
                    destTop.toInt(),
                    (destLeft + destW).toInt(),
                    (destTop + destH).toInt()
                )

                // Optional quality downsampling
                if (quality != PdfQuality.HIGH) {
                    val compressedBmp = Bitmap.createScaledBitmap(
                        sourceBitmap,
                        (destW * 1.5f).toInt().coerceAtLeast(100),
                        (destH * 1.5f).toInt().coerceAtLeast(100),
                        true
                    )
                    canvas.drawBitmap(compressedBmp, null, destRect, bitmapPaint)
                    if (compressedBmp != sourceBitmap) {
                        compressedBmp.recycle()
                    }
                } else {
                    canvas.drawBitmap(sourceBitmap, null, destRect, bitmapPaint)
                }

                if (includePageNumbers) {
                    val pageText = "- Trang ${index + 1} / ${imagePaths.size} -"
                    canvas.drawText(pageText, pageW / 2f, pageH - 12f, textPaint)
                }

                pdfDocument.finishPage(page)
                sourceBitmap.recycle()
            }

            FileOutputStream(outputFile).use { out ->
                pdfDocument.writeTo(out)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            pdfDocument.close()
        }
    }

    /**
     * Exports PDF directly to a content URI (e.g., from SAF createDocument).
     */
    suspend fun writePdfToUri(
        context: Context,
        pdfFile: File,
        targetUri: Uri
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openOutputStream(targetUri)?.use { out ->
                pdfFile.inputStream().use { input ->
                    input.copyTo(out)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
