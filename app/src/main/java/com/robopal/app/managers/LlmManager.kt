package com.robopal.app.managers

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInference.LlmInferenceOptions
import com.robopal.app.RoboPalApplication
import com.robopal.app.agent.LlmProvider
import com.robopal.app.agent.LlmResponse
import com.robopal.app.agent.Message
import com.robopal.app.agent.Tool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

class LlmManager(private val context: Context) : LlmProvider {

    private var llmInference: LlmInference? = null
    var activeModelFile: File? = null
        private set

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    val modelDirectory: File
        get() {
            val dir = File(context.getExternalFilesDir(null), "models")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            return dir
        }

    fun setActiveModel(file: File) {
        if (file.exists()) {
            activeModelFile = file
        }
    }

    fun isModelAvailable(): Boolean {
        if (activeModelFile != null && activeModelFile!!.exists()) return true
        val dir = modelDirectory
        val files = dir.listFiles { _, name -> name.endsWith(".task", ignoreCase = true) || name.endsWith(".gguf", ignoreCase = true) }
        if (!files.isNullOrEmpty()) {
            activeModelFile = files.first()
            return true
        }
        return false
    }

    suspend fun loadModel(modelFile: File) = withContext(Dispatchers.IO) {
        if (!modelFile.exists()) throw IllegalArgumentException("Modelo no encontrado: ${modelFile.absolutePath}")
        llmInference?.close()
        val options = LlmInferenceOptions.builder()
            .setModelPath(modelFile.absolutePath)
            .setMaxTokens(2048)
            .build()
        llmInference = LlmInference.createFromOptions(context, options)
        activeModelFile = modelFile
        _isReady.value = true
    }

    suspend fun generateResponse(prompt: String): String = withContext(Dispatchers.IO) {
        val llm = llmInference ?: throw IllegalStateException("Modelo no cargado")
        _isGenerating.value = true
        try {
            llm.generateResponse(prompt)
        } finally {
            _isGenerating.value = false
        }
    }

    override suspend fun generateResponse(
        messages: List<Message>,
        tools: List<Tool>
    ): LlmResponse = withContext(Dispatchers.IO) {
        if (llmInference == null) {
            val dir = modelDirectory
            val taskFiles = dir.listFiles { _, name -> name.endsWith(".task", ignoreCase = true) }
            if (!taskFiles.isNullOrEmpty()) {
                try {
                    loadModel(taskFiles.first())
                } catch (e: Exception) {
                    return@withContext LlmResponse(
                        content = "Sistema: Error al cargar el modelo .task en MediaPipe: ${e.localizedMessage}",
                        toolCalls = null
                    )
                }
            } else {
                return@withContext LlmResponse(
                    content = "Sistema: No hay un modelo .task cargado en MediaPipe LLM. Descarga Qwen2.5-1.5B-Instruct en formato .task.",
                    toolCalls = null
                )
            }
        }

        val promptBuilder = StringBuilder()
        for (msg in messages) {
            promptBuilder.append("${msg.role.uppercase()}: ${msg.content}\n")
        }

        val responseText = try {
            generateResponse(promptBuilder.toString())
        } catch (e: Exception) {
            "Error en la inferencia del modelo MediaPipe: ${e.localizedMessage}"
        }

        return@withContext LlmResponse(
            content = responseText,
            toolCalls = null
        )
    }

    fun unload() {
        llmInference?.close()
        llmInference = null
        _isReady.value = false
    }
}
