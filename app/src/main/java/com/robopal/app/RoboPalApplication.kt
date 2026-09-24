package com.robopal.app

import android.app.Application
import android.util.Log
import com.robopal.app.agent.AgentEngine
import com.robopal.app.agent.ToolRegistry
import com.robopal.app.managers.DownloadManager
import com.robopal.app.managers.FFmpegManager
import com.robopal.app.managers.LlmManager
import com.robopal.app.managers.OcrManager
import com.robopal.app.managers.TtsManager
import com.robopal.app.managers.VoskManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class RoboPalApplication : Application() {

    companion object {
        lateinit var instance: RoboPalApplication
            private set

        lateinit var llmManager: LlmManager
            private set

        lateinit var ttsManager: TtsManager
            private set

        lateinit var ocrManager: OcrManager
            private set

        lateinit var ffmpegManager: FFmpegManager
            private set

        lateinit var downloadManager: DownloadManager
            private set

        lateinit var voskManager: VoskManager
            private set

        lateinit var toolRegistry: ToolRegistry
            private set

        lateinit var agentEngine: AgentEngine
            private set
    }

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        instance = this

        llmManager = LlmManager(this)
        ttsManager = TtsManager(this)
        ocrManager = OcrManager()
        ffmpegManager = FFmpegManager()
        downloadManager = DownloadManager()
        voskManager = VoskManager(this)

        toolRegistry = ToolRegistry()
        agentEngine = AgentEngine(toolRegistry = toolRegistry, llmProvider = llmManager)

        // FIX 2: Diagnóstico al arrancar la app y carga automática del primer modelo .task
        applicationScope.launch {
            val dir = llmManager.modelDirectory
            Log.i("RoboPal", "Carpeta de modelos: ${dir.absolutePath}")
            val allFiles = dir.listFiles()?.joinToString { it.name } ?: "vacía"
            Log.i("RoboPal", "Archivos encontrados: $allFiles")
            val taskFiles = dir.listFiles { _, n -> n.endsWith(".task", ignoreCase = true) }
            if (!taskFiles.isNullOrEmpty()) {
                try {
                    llmManager.loadModel(taskFiles.first())
                    Log.i("RoboPal", "Modelo cargado automáticamente: ${taskFiles.first().name}")
                } catch (e: Exception) {
                    Log.e("RoboPal", "Error cargando modelo al arrancar: ${e.message}", e)
                }
            } else {
                Log.w("RoboPal", "No se encontró ningún modelo .task en ${dir.absolutePath}")
            }
        }
    }
}
