package com.robopal.app.managers

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class TtsManager(context: Context) : TextToSpeech.OnInitListener {

    companion object {
        private const val TAG = "TtsManager"
    }

    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var isReady = false

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("es"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "El idioma español no está soportado para TextToSpeech.")
            } else {
                isReady = true
                Log.d(TAG, "TextToSpeech inicializado con éxito en español.")
            }
        } else {
            Log.e(TAG, "Error al inicializar TextToSpeech. Status: $status")
        }
    }

    fun speak(text: String, onComplete: (() -> Unit)? = null) {
        if (!isReady || text.isBlank()) {
            Log.w(TAG, "TTS no está listo o el texto está vacío.")
            onComplete?.invoke()
            return
        }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "RoboPalTTS")
        onComplete?.invoke()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
