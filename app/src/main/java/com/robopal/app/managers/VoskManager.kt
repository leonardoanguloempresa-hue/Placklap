package com.robopal.app.managers

import android.content.Context
import android.util.Log

class VoskManager(private val context: Context) {

    companion object {
        private const val TAG = "VoskManager"
    }

    private var isInitialized = false

    fun initModel(modelPath: String) {
        Log.d(TAG, "Inicializando modelo Vosk desde $modelPath")
        isInitialized = true
    }

    fun transcribeAudio(audioData: ByteArray): String {
        if (!isInitialized) {
            Log.w(TAG, "VoskManager no está inicializado.")
            return ""
        }
        return "Transcripción simulada de voz."
    }
}
