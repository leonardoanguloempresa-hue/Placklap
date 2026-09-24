package com.robopal.app.managers

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInference.LlmInferenceOptions
import com.robopal.app.RoboPalApplication
import com.robopal.app.agent.LlmProvider
import com.robopal.app.agent.LlmResponse
import com.robopal.app.agent.Message
import com.robopal.app.agent.Tool
import com.robopal.app.agent.ToolCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

class LlmManager(private val context: Context) : LlmProvider {

    companion object {
        private const val TAG = "LlmManager"
    }

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

    suspend fun setActiveModelAndLoad(file: File) = withContext(Dispatchers.IO) {
        if (!file.exists()) throw IllegalArgumentException("Modelo no existe: ${file.absolutePath}")
        loadModel(file)
    }

    fun setActiveModel(file: File) {
        if (file.exists()) {
            activeModelFile = file
        }
    }

    fun isModelAvailable(): Boolean {
        if (activeModelFile != null && activeModelFile!!.exists() && _isReady.value) return true
        val dir = modelDirectory
        val files = dir.listFiles { _, name -> name.endsWith(".task", ignoreCase = true) }
        if (!files.isNullOrEmpty()) {
            activeModelFile = files.first()
            return true
        }
        return false
    }

    suspend fun loadModel(modelFile: File) = withContext(Dispatchers.IO) {
        if (!modelFile.exists()) throw IllegalArgumentException("Modelo no encontrado: ${modelFile.absolutePath}")
        llmInference?.close()
        Log.i(TAG, "Cargando modelo MediaPipe .task en memoria: ${modelFile.absolutePath}")
        val options = LlmInferenceOptions.builder()
            .setModelPath(modelFile.absolutePath)
            .setMaxTokens(2048)
            .build()
        llmInference = LlmInference.createFromOptions(context, options)
        activeModelFile = modelFile
        _isReady.value = true
        Log.i(TAG, "Modelo MediaPipe .task cargado con éxito.")
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

        val toolInstructionPrompt = """
Para usar una herramienta, responde EXACTAMENTE:
<tool_call>{"name":"nombre","arguments":{...}}</tool_call>
Herramientas disponibles:
- tap(x, y)
- swipe(x1, y1, x2, y2, durationMs)
- long_press(x, y, durationMs)
- type_text(text)
- press_back()
- press_home()
- press_recent()
- read_screen()
- read_screen_ocr()
- open_app(packageName)
- find_and_tap(text)
- wait(ms)
- download(url, outputPath)
- open_url(url)
- take_screenshot()
""".trimIndent()

        val promptBuilder = StringBuilder()
        promptBuilder.append("SYSTEM: $toolInstructionPrompt\n")
        for (msg in messages) {
            promptBuilder.append("${msg.role.uppercase()}: ${msg.content}\n")
        }

        val rawResponseText = try {
            generateResponse(promptBuilder.toString())
        } catch (e: Exception) {
            return@withContext LlmResponse(
                content = "Error en la inferencia del modelo MediaPipe: ${e.localizedMessage}",
                toolCalls = null
            )
        }

        // Extracción de bloques <tool_call>...</tool_call> mediante Regex
        val regex = Regex("<tool_call>(.*?)</tool_call>", RegexOption.DOT_MATCHES_ALL)
        val matches = regex.findAll(rawResponseText)
        val toolCallsList = mutableListOf<ToolCall>()

        for (match in matches) {
            val jsonContent = match.groupValues[1].trim()
            try {
                val jsonObject = JSONObject(jsonContent)
                val toolName = jsonObject.getString("name")
                val argsObject = jsonObject.optJSONObject("arguments") ?: JSONObject()
                val argsMap = jsonObjectToMap(argsObject)

                toolCallsList.add(
                    ToolCall(
                        id = UUID.randomUUID().toString(),
                        name = toolName,
                        arguments = argsMap
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error al parsear JSON de <tool_call>: ${e.message}", e)
            }
        }

        val cleanContent = regex.replace(rawResponseText, "").trim()

        return@withContext LlmResponse(
            content = if (cleanContent.isNotBlank()) cleanContent else null,
            toolCalls = if (toolCallsList.isNotEmpty()) toolCallsList else null
        )
    }

    private fun jsonObjectToMap(jsonObject: JSONObject): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        val keys = jsonObject.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val value = jsonObject.get(key)
            when (value) {
                is JSONArray -> map[key] = jsonArrayToList(value)
                is JSONObject -> map[key] = jsonObjectToMap(value)
                else -> map[key] = value
            }
        }
        return map
    }

    private fun jsonArrayToList(jsonArray: JSONArray): List<Any> {
        val list = mutableListOf<Any>()
        for (i in 0 until jsonArray.length()) {
            val value = jsonArray.get(i)
            when (value) {
                is JSONArray -> list.add(jsonArrayToList(value))
                is JSONObject -> list.add(jsonObjectToMap(value))
                else -> list.add(value)
            }
        }
        return list
    }

    fun unload() {
        llmInference?.close()
        llmInference = null
        _isReady.value = false
    }
}
