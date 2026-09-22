package com.robopal.app.managers

import android.os.Environment
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FFmpegManager {

    val moviesDirectory: File
        get() {
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                "RoboPal"
            )
            if (!dir.exists()) {
                dir.mkdirs()
            }
            return dir
        }

    suspend fun cutVideo(
        inputPath: String,
        outputPath: String,
        startTime: String,
        endTime: String
    ): String = withContext(Dispatchers.IO) {
        val command = "-ss $startTime -to $endTime -i \"$inputPath\" -c copy \"$outputPath\""
        val session = FFmpegKit.execute(command)
        return@withContext if (ReturnCode.isSuccess(session.returnCode)) {
            "Video cortado con éxito en $outputPath"
        } else {
            "Error al cortar video con FFmpeg. Código: ${session.returnCode}"
        }
    }

    suspend fun mergeVideos(
        inputPaths: List<String>,
        outputPath: String
    ): String = withContext(Dispatchers.IO) {
        if (inputPaths.isEmpty()) return@withContext "Error: No se especificaron archivos de entrada."

        val listFile = File(moviesDirectory, "concat_list.txt")
        listFile.writeText(inputPaths.joinToString("\n") { "file '$it'" })

        val command = "-f concat -safe 0 -i \"${listFile.absolutePath}\" -c copy \"$outputPath\""
        val session = FFmpegKit.execute(command)
        listFile.delete()

        return@withContext if (ReturnCode.isSuccess(session.returnCode)) {
            "Videos concatenados con éxito en $outputPath"
        } else {
            "Error al concatenar videos con FFmpeg."
        }
    }

    suspend fun addSubtitles(
        videoPath: String,
        subtitlePath: String,
        outputPath: String
    ): String = withContext(Dispatchers.IO) {
        val command = "-i \"$videoPath\" -vf subtitles=\"$subtitlePath\" \"$outputPath\""
        val session = FFmpegKit.execute(command)
        return@withContext if (ReturnCode.isSuccess(session.returnCode)) {
            "Subtítulos añadidos con éxito en $outputPath"
        } else {
            "Error al añadir subtítulos con FFmpeg."
        }
    }
}
