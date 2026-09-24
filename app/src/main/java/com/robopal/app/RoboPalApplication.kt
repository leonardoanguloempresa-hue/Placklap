package com.robopal.app

import android.app.Application
import androidx.room.Room
import com.robopal.app.agent.AgentEngine
import com.robopal.app.data.memory.AppDatabase
import com.robopal.app.data.memory.MemoryDao
import com.robopal.app.managers.DownloadManager
import com.robopal.app.managers.FFmpegManager
import com.robopal.app.managers.HuggingFaceClient
import com.robopal.app.managers.LlmManager
import com.robopal.app.managers.OcrManager
import com.robopal.app.managers.TtsManager
import com.robopal.app.managers.VoskManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RoboPalApplication : Application() {

    companion object {
        lateinit var instance: RoboPalApplication
            private set

        lateinit var llmManager: LlmManager
            private set

        lateinit var voskManager: VoskManager
            private set

        lateinit var ttsManager: TtsManager
            private set

        lateinit var ocrManager: OcrManager
            private set

        lateinit var ffmpegManager: FFmpegManager
            private set

        lateinit var downloadManager: DownloadManager
            private set

        lateinit var huggingFaceClient: HuggingFaceClient
            private set

        lateinit var agentEngine: AgentEngine
            private set

        lateinit var database: AppDatabase
            private set

        val memoryDao: MemoryDao
            get() = database.memoryDao()
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "robopal_memory_db"
        ).fallbackToDestructiveMigration().build()

        llmManager = LlmManager(this)
        voskManager = VoskManager(this)
        ttsManager = TtsManager(this)
        ocrManager = OcrManager()
        ffmpegManager = FFmpegManager(this)
        downloadManager = DownloadManager()
        huggingFaceClient = HuggingFaceClient()

        agentEngine = AgentEngine(llmProvider = llmManager)

        CoroutineScope(Dispatchers.IO).launch {
            val modelDir = llmManager.modelDirectory
            val taskFiles = modelDir.listFiles { _, name -> name.endsWith(".task", ignoreCase = true) }
            if (!taskFiles.isNullOrEmpty()) {
                try {
                    llmManager.loadModel(taskFiles.first())
                } catch (_: Exception) {
                }
            }
        }
    }
}
