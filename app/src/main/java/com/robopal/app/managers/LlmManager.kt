package com.robopal.app.managers

import com.robopal.app.agent.LlmProvider
import com.robopal.app.agent.LlmResponse
import com.robopal.app.agent.Message
import com.robopal.app.agent.Tool
import java.io.File

class LlmManager(
    private val modelDirectoryPath: String = "/sdcard/RoboPal/models/"
) : LlmProvider {

    fun isModelAvailable(): Boolean {
        val dir = File(modelDirectoryPath)
        if (!dir.exists()) return false
        val ggufFiles = dir.listFiles { _, name -> name.endsWith(".gguf", ignoreCase = true) }
        return !ggufFiles.isNullOrEmpty()
    }

    override suspend fun generateResponse(
        messages: List<Message>,
        tools: List<Tool>
    ): LlmResponse {
        if (!isModelAvailable()) {
            return LlmResponse(
                content = "Sistema: El modelo GGUF no está instalado. Por favor, descárgalo desde la pantalla de Configuración.",
                toolCalls = null
            )
        }

        // Estructura preparada para inferencia JNI con llama.cpp Android
        return LlmResponse(
            content = "Respuesta procesada con el modelo GGUF local.",
            toolCalls = null
        )
    }
}
