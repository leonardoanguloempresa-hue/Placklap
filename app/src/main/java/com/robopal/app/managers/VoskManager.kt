package com.robopal.app.managers

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.vosk.LibVosk
import org.vosk.LogLevel
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import java.io.File

class VoskManager(private val context: Context) {

    companion object {
        private const val TAG = "VoskManager"
        val SILF_TRIGGERS = listOf("silf", "sil", "sylf", "self", "cilf")
    }

    private var model: Model? = null
    private var speechService: SpeechService? = null

    private val _partialText = MutableStateFlow("")
    val partialText: StateFlow<String> = _partialText.asStateFlow()

    private val _finalText = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val finalText: SharedFlow<String> = _finalText.asSharedFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    suspend fun initialize(modelDir: File) = withContext(Dispatchers.IO) {
        if (model != null) return@withContext
        if (!modelDir.exists()) throw IllegalArgumentException("Modelo Vosk no encontrado en ${modelDir.absolutePath}")
        LibVosk.setLogLevel(LogLevel.WARNINGS)
        model = Model(modelDir.absolutePath)
        Log.i(TAG, "Vosk modelo cargado")
    }

    fun startListening(grammar: List<String>? = null) {
        val m = model ?: run { Log.e(TAG, "Vosk no inicializado"); return }
        stop()
        val recognizer = if (!grammar.isNullOrEmpty()) {
            val jsonArray = JSONArray()
            grammar.forEach { jsonArray.put(it) }
            Recognizer(m, 16000f, jsonArray.toString())
        } else {
            Recognizer(m, 16000f)
        }
        val listener = object : RecognitionListener {
            override fun onPartialResult(hypothesis: String?) {
                val text = parseJson(hypothesis ?: return)
                _partialText.value = text
            }

            override fun onResult(hypothesis: String?) {
                val text = parseJson(hypothesis ?: return)
                if (text.isNotBlank()) _finalText.tryEmit(text)
            }

            override fun onFinalResult(hypothesis: String?) {
                val text = parseJson(hypothesis ?: return)
                if (text.isNotBlank()) _finalText.tryEmit(text)
            }

            override fun onError(e: Exception?) {
                Log.e(TAG, "Error Vosk: ${e?.message}")
                stop()
            }

            override fun onTimeout() {
                Log.w(TAG, "Timeout Vosk")
            }
        }

        try {
            speechService = SpeechService(recognizer, 16000f).apply {
                startListening(listener)
            }
            _isListening.value = true
        } catch (e: Exception) {
            Log.e(TAG, "Error iniciando escucha Vosk: ${e.message}")
        }
    }

    fun startContinuousListening(onCommandDetected: (prompt: String) -> Unit) {
        startListening()
    }

    fun stopContinuousListening() {
        stop()
    }

    fun stop() {
        speechService?.stop()
        speechService?.shutdown()
        speechService = null
        _isListening.value = false
    }

    private fun parseJson(json: String): String {
        return try {
            val obj = JSONObject(json)
            obj.optString("text", obj.optString("partial", ""))
        } catch (e: Exception) {
            ""
        }
    }
}
