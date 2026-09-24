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
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import org.vosk.LibVosk
import org.vosk.LogLevel
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

class VoskManager(private val context: Context) {

    companion object {
        private const val TAG = "VoskManager"
        val SILF_TRIGGERS = listOf("silf", "sil", "sylf", "self", "cilf")
    }

    private var model: Model? = null
    private var speechService: SpeechService? = null
    private val httpClient = OkHttpClient()

    private val _partialText = MutableStateFlow("")
    val partialText: StateFlow<String> = _partialText.asStateFlow()

    private val _finalText = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val finalText: SharedFlow<String> = _finalText.asSharedFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    suspend fun ensureHotwordModel(): File = withContext(Dispatchers.IO) {
        val targetDir = File(context.filesDir, "vosk-small")
        if (targetDir.exists() && targetDir.listFiles()?.isNotEmpty() == true) {
            val subFiles = targetDir.listFiles()
            val modelFolder = subFiles?.firstOrNull { it.isDirectory && File(it, "am").exists() } ?: targetDir
            return@withContext modelFolder
        }

        val zipUrl = "https://alphacephei.com/vosk/models/vosk-model-small-es-0.42.zip"
        val zipFile = File(context.cacheDir, "vosk-small.zip")

        Log.i(TAG, "Descargando modelo Vosk pequeño desde $zipUrl...")
        val request = Request.Builder().url(zipUrl).build()
        val response = httpClient.newCall(request).execute()

        if (!response.isSuccessful) {
            throw java.io.IOException("Error descargando modelo Vosk. Código HTTP: ${response.code}")
        }

        response.body?.byteStream()?.use { input ->
            FileOutputStream(zipFile).use { output ->
                input.copyTo(output)
            }
        }

        Log.i(TAG, "Descomprimiendo modelo Vosk...")
        targetDir.mkdirs()
        ZipInputStream(zipFile.inputStream()).use { zipInput ->
            var entry = zipInput.nextEntry
            while (entry != null) {
                val newFile = File(targetDir, entry.name)
                if (entry.isDirectory) {
                    newFile.mkdirs()
                } else {
                    newFile.parentFile?.mkdirs()
                    FileOutputStream(newFile).use { out ->
                        zipInput.copyTo(out)
                    }
                }
                zipInput.closeEntry()
                entry = zipInput.nextEntry
            }
        }

        zipFile.delete()

        val subFiles = targetDir.listFiles()
        val modelFolder = subFiles?.firstOrNull { it.isDirectory && File(it, "am").exists() } ?: targetDir
        modelFolder
    }

    suspend fun initialize(modelDir: File) = withContext(Dispatchers.IO) {
        if (model != null) return@withContext
        if (!modelDir.exists()) throw IllegalArgumentException("Modelo Vosk no encontrado en ${modelDir.absolutePath}")
        LibVosk.setLogLevel(LogLevel.WARNINGS)
        model = Model(modelDir.absolutePath)
        Log.i(TAG, "Vosk modelo cargado desde ${modelDir.absolutePath}")
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
