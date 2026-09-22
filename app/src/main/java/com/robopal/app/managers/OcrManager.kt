package com.robopal.app.managers

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class OcrManager {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun extractTextFromBitmap(bitmap: Bitmap): String =
        suspendCancellableCoroutine { continuation ->
            val image = InputImage.fromBitmap(bitmap, 0)
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    if (continuation.isActive) {
                        val resultText = visionText.text
                        if (resultText.isBlank()) {
                            continuation.resume("No se detectó texto en la captura de pantalla.")
                        } else {
                            continuation.resume(resultText)
                        }
                    }
                }
                .addOnFailureListener { e ->
                    if (continuation.isActive) {
                        continuation.resume("Error en el reconocimiento OCR: ${e.localizedMessage ?: e.message}")
                    }
                }
        }
}
