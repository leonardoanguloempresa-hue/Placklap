package com.robopal.app.managers

import android.content.Context
import android.util.Log

class PorcupineManager(
    private val context: Context,
    private val onWakeWordDetected: () -> Unit
) {
    companion object {
        private const val TAG = "PorcupineManager"
    }

    private var isListening = false

    fun start() {
        Log.d(TAG, "Iniciando detección de hotword 'Robot' con Porcupine.")
        isListening = true
    }

    fun stop() {
        Log.d(TAG, "Deteniendo Porcupine.")
        isListening = false
    }

    fun triggerWakeWordForTesting() {
        if (isListening) {
            onWakeWordDetected()
        }
    }
}
