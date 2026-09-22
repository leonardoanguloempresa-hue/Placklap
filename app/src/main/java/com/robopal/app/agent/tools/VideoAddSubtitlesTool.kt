package com.robopal.app.agent.tools

import com.robopal.app.RoboPalApplication
import com.robopal.app.agent.Tool

class VideoAddSubtitlesTool : Tool {
    override val name: String = "video_add_subtitles"
    override val description: String = "Incrusta subtítulos en un archivo de video mediante FFmpeg."
    override val parameterSchema: Map<String, Any> = mapOf(
        "type" to "object",
        "properties" to mapOf(
            "videoPath" to mapOf("type" to "string", "description" to "Ruta del video de entrada"),
            "subtitlePath" to mapOf("type" to "string", "description" to "Ruta del archivo de subtítulos (.srt o .ass)"),
            "outputPath" to mapOf("type" to "string", "description" to "Ruta del video de salida con subtítulos")
        ),
        "required" to listOf("videoPath", "subtitlePath", "outputPath")
    )

    override suspend fun execute(args: Map<String, Any>): String {
        val videoPath = args["videoPath"] as? String
            ?: return "Error: Parámetro 'videoPath' no válido."
        val subtitlePath = args["subtitlePath"] as? String
            ?: return "Error: Parámetro 'subtitlePath' no válido."
        val outputPath = args["outputPath"] as? String
            ?: return "Error: Parámetro 'outputPath' no válido."

        return RoboPalApplication.ffmpegManager.addSubtitles(videoPath, subtitlePath, outputPath)
    }
}
