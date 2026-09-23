package com.robopal.app.managers

import android.os.Environment
import com.robopal.app.RoboPalApplication
import com.robopal.app.agent.LlmProvider
import com.robopal.app.agent.LlmResponse
import com.robopal.app.agent.Message
import com.robopal.app.agent.Tool
import java.io.File

class LlmManager : LlmProvider {

    var activeModelFile: File? = null
        private set

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

    fun setActiveModel(file: File) {
        if (file.exists() && file.name.endsWith(".gguf", ignoreCase = true)) {
            activeModelFile = file
        }
    }

    fun isModelAvailable(): Boolean {
        if (activeModelFile != null && activeModelFile!!.exists()) return true

        val dir = modelDirectory
        if (!dir.exists()) return false
        val ggufFiles = dir.listFiles { _, name -> name.endsWith(".gguf", ignoreCase = true) }
        if (!ggufFiles.isNullOrEmpty()) {
            activeModelFile = ggufFiles.first()
            return true
        }
        return false
    }

    override suspend fun generateResponse(
        messages: List<Message>,
        tools: List<Tool>
    ): LlmResponse {
        if (!isModelAvailable()) {
            return LlmResponse(
                content = "Sistema: El modelo GGUF no está instalado. Por favor, descárgalo desde la pantalla de Configuración o Modelos.",
                toolCalls = null
            )
        }

        val modelName = activeModelFile?.name ?: "GGUF"
        return LlmResponse(
            content = "Respuesta procesada con el modelo $modelName.",
            toolCalls = null
        )
    }
}
