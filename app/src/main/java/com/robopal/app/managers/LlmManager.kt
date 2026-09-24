package com.robopal.app.managers

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInference.LlmInferenceOptions
import com.robopal.app.agent.LlmProvider
import com.robopal.app.agent.LlmResponse
import com.robopal.app.agent.Message
import com.robopal.app.agent.Tool
import com.robopal.app.agent.ToolCall
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.UUID
import kotlin.coroutines.coroutineContext

open class LlmManager(private val context: Context) : LlmProvider {

    companion object {
        private const val TAG = "LlmManager"
        private val UTF8 = StandardCharsets.UTF_8

        private val requiredArgs = mapOf(
            "tap" to listOf("x", "y"),
            "swipe" to listOf("x1", "y1", "x2", "y2", "durationMs"),
            "long_press" to listOf("x", "y", "durationMs"),
            "type_text" to listOf("text"),
            "open_app" to listOf("packageName"),
            "find_and_tap" to listOf("text"),
            "wait" to listOf("ms"),
            "download" to listOf("url", "outputPath"),
            "open_url" to listOf("url"),
            "video_cut" to listOf("input", "startSec", "durationSec", "output"),
            "video_merge" to listOf("inputs", "output"),
            "video_add_subtitles" to listOf("input", "srtPath", "output")
        )
    }

    private val llmMutex = Mutex()
    private var llmInference: LlmInference? = null
    var activeModelFile: File? = null
        private set

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    open val modelDirectory: File
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

    open fun isModelAvailable(): Boolean {
        if (activeModelFile != null && activeModelFile!!.exists() && _isReady.value) return true
        val dir = modelDirectory
        val files = dir.listFiles { _, name -> name.endsWith(".task", ignoreCase = true) }
        if (!files.isNullOrEmpty()) {
            activeModelFile = files.first()
            return true
        }
        return false
    }

    suspend fun loadModel(modelFile: File) = llmMutex.withLock {
        withContext(Dispatchers.IO) {
            coroutineContext.ensureActive()
            if (!modelFile.exists()) throw IllegalArgumentException("Modelo no encontrado: ${modelFile.absolutePath}")

            try {
                llmInference?.close()
            } catch (e: Exception) {
                Log.w(TAG, "Error cerrando la instancia previa de LlmInference: ${e.message}")
            }
            llmInference = null
            _isReady.value = false

            Log.i(TAG, "Cargando modelo MediaPipe .task en memoria (con Mutex): ${modelFile.absolutePath}")
            val options = LlmInferenceOptions.builder()
                .setModelPath(modelFile.absolutePath)
                .setMaxTokens(2048)
                .build()

            val newInference = LlmInference.createFromOptions(context, options)
            coroutineContext.ensureActive()

            llmInference = newInference
            activeModelFile = modelFile
            _isReady.value = true
            Log.i(TAG, "Modelo MediaPipe .task cargado con éxito.")
        }
    }

    suspend fun generateResponse(prompt: String): String = llmMutex.withLock {
        withContext(Dispatchers.IO) {
            coroutineContext.ensureActive()
            val llm = llmInference ?: throw IllegalStateException("Modelo no cargado o cerrado")
            _isGenerating.value = true
            try {
                coroutineContext.ensureActive()
                val rawString = llm.generateResponse(prompt)
                coroutineContext.ensureActive()
                val responseBytes = rawString.toByteArray(UTF8)
                String(responseBytes, UTF8)
            } catch (e: CancellationException) {
                Log.w(TAG, "Generación cancelada por el usuario o corrutina.")
                throw e
            } finally {
                _isGenerating.value = false
            }
        }
    }

    override suspend fun generateResponse(
        messages: List<Message>,
        tools: List<Tool>
    ): LlmResponse = llmMutex.withLock {
        withContext(Dispatchers.IO) {
            coroutineContext.ensureActive()
            if (llmInference == null) {
                val dir = modelDirectory
                val taskFiles = dir.listFiles { _, name -> name.endsWith(".task", ignoreCase = true) }
                if (!taskFiles.isNullOrEmpty()) {
                    try {
                        val options = LlmInferenceOptions.builder()
                            .setModelPath(taskFiles.first().absolutePath)
                            .setMaxTokens(2048)
                            .build()
                        llmInference = LlmInference.createFromOptions(context, options)
                        activeModelFile = taskFiles.first()
                        _isReady.value = true
                    } catch (e: CancellationException) {
                        throw e
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
Eres RoboPal, asistente de Android. Para usar herramienta responde EXACTAMENTE:
<tool_call>{"name":"NOMBRE","arguments":{...}}</tool_call>

REGLAS:
1. "abre X" → open_app con el paquete.
2. "busca X" → open_url.
3. "toca X" → find_and_tap.
4. NUNCA uses open_url para abrir apps.
5. NUNCA dejes arguments vacío.
6. NUNCA inventes coordenadas. Usa find_and_tap.
7. Si no sabes el paquete, responde texto.

Paquetes reales:
YouTube=com.google.android.youtube, WhatsApp=com.whatsapp,
TikTok=com.zhiliaoapp.musically, Instagram=com.instagram.android,
Chrome=com.android.chrome, Gmail=com.google.android.gm,
Spotify=com.spotify.music, Telegram=org.telegram.messenger,
Calculadora=com.google.android.calculator, Reloj=com.google.android.deskclock

Ejemplos:
Usuario: abre YouTube
Tú: <tool_call>{"name":"open_app","arguments":{"packageName":"com.google.android.youtube"}}</tool_call>

Usuario: toca Iniciar
Tú: <tool_call>{"name":"find_and_tap","arguments":{"text":"Iniciar"}}</tool_call>

Usuario: ¿qué hora es?
Tú: Son las 3 de la tarde.

Herramientas: open_app, find_and_tap, type_text, press_back, press_home, press_recent, read_screen, read_screen_ocr, wait, open_url, take_screenshot.
""".trimIndent()

            val promptBuilder = StringBuilder()
            promptBuilder.append("<|im_start|>system\n$toolInstructionPrompt<|im_end|>\n")

            for (msg in messages) {
                promptBuilder.append("<|im_start|>${msg.role}\n${msg.content}<|im_end|>\n")
            }
            promptBuilder.append("<|im_start|>assistant\n")

            val rawResponseText = try {
                val llm = llmInference ?: throw IllegalStateException("Modelo no cargado o cerrado")
                _isGenerating.value = true
                try {
                    coroutineContext.ensureActive()
                    val rawString = llm.generateResponse(promptBuilder.toString())
                    coroutineContext.ensureActive()
                    val responseBytes = rawString.toByteArray(UTF8)
                    String(responseBytes, UTF8)
                } finally {
                    _isGenerating.value = false
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                return@withContext LlmResponse(
                    content = "Error en la inferencia del modelo MediaPipe: ${e.localizedMessage}",
                    toolCalls = null
                )
            }

            var limpio = rawResponseText.replace(
                Regex("<think>.*?</think>", RegexOption.DOT_MATCHES_ALL), ""
            )
            if (limpio.contains("<think>")) {
                limpio = limpio.substringBefore("<think>")
            }

            val toolCallsList = mutableListOf<ToolCall>()
            val xmlRegex = Regex("<tool_call>(.*?)</tool_call>", RegexOption.DOT_MATCHES_ALL)
            val xmlMatches = xmlRegex.findAll(limpio)

            for (match in xmlMatches) {
                val jsonContent = match.groupValues[1].trim()
                try {
                    val jsonObject = JSONObject(jsonContent)
                    val toolName = jsonObject.getString("name")
                    val argsObject = jsonObject.optJSONObject("arguments") ?: JSONObject()
                    val argsMap = jsonObjectToMap(argsObject)

                    val requeridos = requiredArgs[toolName] ?: emptyList()
                    val faltantes = requeridos.filter { argsMap[it] == null || argsMap[it].toString().isBlank() }
                    if (faltantes.isNotEmpty()) {
                        Log.w(TAG, "Tool '$toolName' RECHAZADA: faltan $faltantes")
                        continue
                    }

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

            var cleanedText = xmlRegex.replace(limpio, "").trim()

            if (toolCallsList.isEmpty()) {
                val pythonRegex = Regex("""(?:^|\n)\s*(\w+)\(([^)]*)\)""")
                val pythonMatches = pythonRegex.findAll(cleanedText)
                for (m in pythonMatches) {
                    val name = m.groupValues[1]
                    if (!requiredArgs.containsKey(name)) continue
                    val argsStr = m.groupValues[2]
                    val argsMap = mutableMapOf<String, Any>()

                    if (argsStr.isNotBlank()) {
                        argsStr.split(",").forEach { pair ->
                            val parts = pair.split("=", limit = 2)
                            if (parts.size == 2) {
                                val key = parts[0].trim()
                                val value = parts[1].trim().trim('"', '\'')
                                argsMap[key] = value
                            }
                        }
                    }

                    val requeridos = requiredArgs[name] ?: emptyList()
                    val faltantes = requeridos.filter { argsMap[it] == null || argsMap[it].toString().isBlank() }
                    if (faltantes.isEmpty()) {
                        toolCallsList.add(
                            ToolCall(
                                id = UUID.randomUUID().toString(),
                                name = name,
                                arguments = argsMap
                            )
                        )
                    }
                }
                if (toolCallsList.isNotEmpty()) {
                    cleanedText = pythonRegex.replace(cleanedText, "").trim()
                }
            }

            return@withContext LlmResponse(
                content = if (cleanedText.isNotBlank()) cleanedText else null,
                toolCalls = if (toolCallsList.isNotEmpty()) toolCallsList else null
            )
        }
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

    suspend fun unload() = llmMutex.withLock {
        withContext(Dispatchers.IO) {
            try {
                llmInference?.close()
            } catch (e: Exception) {
                Log.w(TAG, "Error cerrando LlmInference durante unload: ${e.message}")
            }
            llmInference = null
            _isReady.value = false
            _isGenerating.value = false
        }
    }
}
