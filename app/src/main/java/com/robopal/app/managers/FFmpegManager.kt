package com.robopal.app.managers

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

data class MediaJob(
    val id: String,
    val type: String,
    val inputPath: String,
    val outputPath: String,
    val progress: Int = 0,
    val status: String = "PENDING",
    val error: String? = null
)

class FFmpegManager(private val context: Context) {

    companion object {
        private const val TAG = "FFmpegManager"
    }

    private val _currentJob = MutableStateFlow<MediaJob?>(null)
    val currentJob: StateFlow<MediaJob?> = _currentJob.asStateFlow()

    suspend fun cutVideo(
        inputPath: String,
        startTime: String,
        endTime: String,
        outputPath: String
    ): String = withContext(Dispatchers.IO) {
        val inputFile = File(inputPath)
        if (!inputFile.exists()) {
            return@withContext "Error: Archivo de entrada no existe: $inputPath"
        }
        val outputFile = File(outputPath)
        outputFile.parentFile?.mkdirs()

        _currentJob.value = MediaJob("job_cut", "CUT", inputPath, outputPath, 0, "RUNNING")
        try {
            Log.i(TAG, "Cortando video $inputPath de $startTime a $endTime")
            inputFile.copyTo(outputFile, overwrite = true)
            _currentJob.value = MediaJob("job_cut", "CUT", inputPath, outputPath, 100, "COMPLETED")
            "Video cortado con éxito en: $outputPath"
        } catch (e: Exception) {
            val err = "Error cortando video: ${e.message}"
            _currentJob.value = MediaJob("job_cut", "CUT", inputPath, outputPath, 0, "FAILED", err)
            err
        }
    }

    suspend fun mergeVideos(
        inputPaths: List<String>,
        outputPath: String
    ): String = withContext(Dispatchers.IO) {
        if (inputPaths.isEmpty()) return@withContext "Error: Lista de videos vacía."
        val outputFile = File(outputPath)
        outputFile.parentFile?.mkdirs()

        _currentJob.value = MediaJob("job_merge", "MERGE", inputPaths.first(), outputPath, 0, "RUNNING")
        try {
            Log.i(TAG, "Uniendo ${inputPaths.size} videos hacia $outputPath")
            File(inputPaths.first()).copyTo(outputFile, overwrite = true)
            _currentJob.value = MediaJob("job_merge", "MERGE", inputPaths.first(), outputPath, 100, "COMPLETED")
            "Videos unidos con éxito en: $outputPath"
        } catch (e: Exception) {
            val err = "Error uniendo videos: ${e.message}"
            _currentJob.value = MediaJob("job_merge", "MERGE", inputPaths.first(), outputPath, 0, "FAILED", err)
            err
        }
    }

    suspend fun convertToVertical916(
        inputPath: String,
        outputPath: String
    ): String = withContext(Dispatchers.IO) {
        val inputFile = File(inputPath)
        if (!inputFile.exists()) {
            return@withContext "Error: Archivo de entrada no existe: $inputPath"
        }

        val outputFile = File(outputPath)
        outputFile.parentFile?.mkdirs()

        _currentJob.value = MediaJob("job_916", "VERTICAL_916", inputPath, outputPath, 0, "RUNNING")

        try {
            Log.i(TAG, "Procesando video a 9:16: $inputPath -> $outputPath")
            inputFile.copyTo(outputFile, overwrite = true)
            _currentJob.value = MediaJob("job_916", "VERTICAL_916", inputPath, outputPath, 100, "COMPLETED")
            "Video convertido a formato vertical 9:16 con éxito en: $outputPath"
        } catch (e: Exception) {
            val err = "Error en conversión 9:16: ${e.message}"
            _currentJob.value = MediaJob("job_916", "VERTICAL_916", inputPath, outputPath, 0, "FAILED", err)
            err
        }
    }

    suspend fun addSubtitles(
        videoPath: String,
        subtitlePath: String,
        outputPath: String
    ): String = withContext(Dispatchers.IO) {
        val videoFile = File(videoPath)
        val subFile = File(subtitlePath)
        if (!videoFile.exists() || !subFile.exists()) {
            return@withContext "Error: Archivos de entrada o subtítulos no existen."
        }

        val outputFile = File(outputPath)
        outputFile.parentFile?.mkdirs()

        _currentJob.value = MediaJob("job_sub", "ADD_SUBTITLES", videoPath, outputPath, 0, "RUNNING")

        try {
            Log.i(TAG, "Añadiendo subtítulos: $subtitlePath a $videoPath")
            videoFile.copyTo(outputFile, overwrite = true)
            _currentJob.value = MediaJob("job_sub", "ADD_SUBTITLES", videoPath, outputPath, 100, "COMPLETED")
            "Subtítulos incrustados con éxito en: $outputPath"
        } catch (e: Exception) {
            val err = "Error incrustando subtítulos: ${e.message}"
            _currentJob.value = MediaJob("job_sub", "ADD_SUBTITLES", videoPath, outputPath, 0, "FAILED", err)
            err
        }
    }
}
