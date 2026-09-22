package com.robopal.app

import android.app.Application
import com.robopal.app.agent.AgentEngine
import com.robopal.app.agent.ToolRegistry
import com.robopal.app.managers.DownloadManager
import com.robopal.app.managers.FFmpegManager
import com.robopal.app.managers.LlmManager
import com.robopal.app.managers.OcrManager
import com.robopal.app.managers.TtsManager
import com.robopal.app.managers.VoskManager

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

    override fun onCreate() {
        super.onCreate()
        instance = this

        llmManager = LlmManager()
        ttsManager = TtsManager(this)
        ocrManager = OcrManager()
        ffmpegManager = FFmpegManager()
        downloadManager = DownloadManager()
        voskManager = VoskManager(this)

        toolRegistry = ToolRegistry()
        agentEngine = AgentEngine(toolRegistry = toolRegistry, llmProvider = llmManager)
    }
}
