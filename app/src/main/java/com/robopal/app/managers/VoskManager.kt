package com.robopal.app.managers

import android.content.Context
import android.util.Log

class VoskManager(private val context: Context) {

    companion object {
        private const val TAG = "VoskManager"
        val SILF_TRIGGERS = listOf("silf", "sil", "sylf", "self", "cilf")
    }

    private var isListening = false

    fun startContinuousListening(onCommandDetected: (prompt: String) -> Unit) {
        Log.d(TAG, "Iniciando escucha continua con VoskManager.")
        isListening = true
    }

    fun stopContinuousListening() {
        Log.d(TAG, "Deteniendo escucha continua.")
        isListening = false
    }

    fun processAudioText(transcribedText: String): String? {
        val trimmed = transcribedText.trim()
        val lowercase = trimmed.lowercase()

        for (trigger in SILF_TRIGGERS) {
            if (lowercase.startsWith(trigger)) {
                val prompt = trimmed.substring(trigger.length).trim()
                return if (prompt.isBlank()) "hola" else prompt
            }
        }
        return null
    }
}
