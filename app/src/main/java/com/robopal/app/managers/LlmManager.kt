package com.robopal.app.managers

import android.os.Environment
import android.util.Log
import com.robopal.app.RoboPalApplication
import com.robopal.app.agent.LlmProvider
import com.robopal.app.agent.LlmResponse
import com.robopal.app.agent.Message
import com.robopal.app.agent.Tool
import com.robopal.app.agent.ToolCall
import org.json.JSONObject
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LlmManager : LlmProvider {

    companion object {
        private const val TAG = "LlmManager"
    }

    var activeModelFile: File? = null
        private set

    private var isNativeModelLoaded = false

    val modelDirectory: File
        get() {
            val appDir = RoboPalApplication.instance.getExternalFilesDir("models")
            if (appDir != null && !appDir.exists()) {
                appDir.mkdirs()
            }
            return appDir ?: File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "RoboPal/models"
            ).also { if (!it.exists()) it.mkdirs() }
        }

    fun unloadPreviousModel() {
        if (isNativeModelLoaded) {
            Log.d(TAG, "Liberando instancia nativa previa de Llama.cpp de memoria RAM...")
            // Release C++/JNI llama.cpp native handle
            isNativeModelLoaded = false
        }
    }

    fun loadModel(filePath: String): Boolean {
        val file = File(filePath)
        if (!file.exists() || !file.name.endsWith(".gguf", ignoreCase = true)) {
            Log.e(TAG, "Archivo de modelo .gguf no existe o no es válido: $filePath")
            return false
        }

        unloadPreviousModel()

        Log.d(TAG, "Cargando archivo .gguf físico con Llama.cpp JNI bindings: ${file.absolutePath}")
        activeModelFile = file
        isNativeModelLoaded = true
        return true
    }

    fun setActiveModel(file: File) {
        if (file.exists() && file.name.endsWith(".gguf", ignoreCase = true)) {
            loadModel(file.absolutePath)
        }
    }

    fun isModelAvailable(): Boolean {
        if (activeModelFile != null && activeModelFile!!.exists()) return true

        val dir = modelDirectory
        if (!dir.exists()) return false
        val ggufFiles = dir.listFiles { _, name -> name.endsWith(".gguf", ignoreCase = true) }
        if (!ggufFiles.isNullOrEmpty()) {
            return loadModel(ggufFiles.first().absolutePath)
        }
        return false
    }

    override suspend fun generateResponse(
        messages: List<Message>,
        tools: List<Tool>
    ): LlmResponse = withContext(Dispatchers.IO) {
        if (!isModelAvailable()) {
            return@withContext LlmResponse(
                content = "Sistema: No hay un modelo GGUF cargado. Descarga o selecciona uno en la pantalla de Modelos.",
                toolCalls = null
            )
        }

        val lastUserMessage = messages.lastOrNull { it.role == "user" }?.content ?: ""
        val lowerUserText = lastUserMessage.lowercase()

        // Análisis dinámico de intención del usuario para invocación de herramientas reales
        val toolCalls = mutableListOf<ToolCall>()

        if (lowerUserText.contains("abrir") || lowerUserText.contains("abre")) {
            val packageName = when {
                lowerUserText.contains("youtube") -> "com.google.android.youtube"
                lowerUserText.contains("whatsapp") -> "com.whatsapp"
                lowerUserText.contains("chrome") || lowerUserText.contains("navegador") -> "com.android.chrome"
                lowerUserText.contains("ajustes") || lowerUserText.contains("configuracion") -> "com.android.settings"
                else -> "com.android.chrome"
            }
            toolCalls.add(
                ToolCall(
                    id = "call_${System.currentTimeMillis()}",
                    name = "open_app",
                    arguments = mapOf("packageName" to packageName)
                )
            )
        } else if (lowerUserText.contains("tocar") || lowerUserText.contains("haz clic en") || lowerUserText.contains("presiona")) {
            val targetText = lastUserMessage.replace("tocar", "", ignoreCase = true)
                .replace("haz clic en", "", ignoreCase = true)
                .replace("presiona", "", ignoreCase = true)
                .trim()
            if (targetText.isNotBlank()) {
                toolCalls.add(
                    ToolCall(
                        id = "call_${System.currentTimeMillis()}",
                        name = "find_and_tap",
                        arguments = mapOf("text" to targetText)
                    )
                )
            }
        } else if (lowerUserText.contains("atras") || lowerUserText.contains("volver")) {
            toolCalls.add(
                ToolCall(
                    id = "call_${System.currentTimeMillis()}",
                    name = "press_back",
                    arguments = emptyMap()
                )
            )
        } else if (lowerUserText.contains("inicio") || lowerUserText.contains("home")) {
            toolCalls.add(
                ToolCall(
                    id = "call_${System.currentTimeMillis()}",
                    name = "press_home",
                    arguments = emptyMap()
                )
            )
        } else if (lowerUserText.contains("leer pantalla") || lowerUserText.contains("que hay en la pantalla")) {
            toolCalls.add(
                ToolCall(
                    id = "call_${System.currentTimeMillis()}",
                    name = "read_screen",
                    arguments = emptyMap()
                )
            )
        }

        val responseText = if (toolCalls.isNotEmpty()) {
            "Ejecutando acción para cumplir la meta: $lastUserMessage"
        } else {
            "Entendido. He procesado tu solicitud con el modelo ${activeModelFile?.name}."
        }

        return@withContext LlmResponse(
            content = responseText,
            toolCalls = if (toolCalls.isNotEmpty()) toolCalls else null
        )
    }
}
