package com.example.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume

object OcrManager {

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    /**
     * Extracts text from Bitmap completely on-device offline.
     */
    suspend fun recognizeText(bitmap: Bitmap): Result<String> = withContext(Dispatchers.Default) {
        suspendCancellableCoroutine { continuation ->
            try {
                val inputImage = InputImage.fromBitmap(bitmap, 0)
                recognizer.process(inputImage)
                    .addOnSuccessListener { visionText ->
                        val text = visionText.text
                        continuation.resume(Result.success(text))
                    }
                    .addOnFailureListener { exception ->
                        continuation.resume(Result.failure(exception))
                    }
            } catch (e: Exception) {
                continuation.resume(Result.failure(e))
            }
        }
    }

    /**
     * Extracts text directly from image file on disk.
     */
    suspend fun recognizeTextFromFile(filePath: String): Result<String> = withContext(Dispatchers.IO) {
        val file = File(filePath)
        if (!file.exists()) {
            return@withContext Result.failure(IllegalArgumentException("File not found: $filePath"))
        }
        val bitmap = BitmapFactory.decodeFile(filePath)
            ?: return@withContext Result.failure(IllegalStateException("Could not decode image"))

        try {
            recognizeText(bitmap)
        } finally {
            bitmap.recycle()
        }
    }
}
