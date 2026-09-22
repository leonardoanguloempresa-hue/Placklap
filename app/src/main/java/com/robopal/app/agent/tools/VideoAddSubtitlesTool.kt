package com.robopal.app.agent.tools

import com.robopal.app.agent.Tool

class VideoAddSubtitlesTool : Tool {
    override val name: String = "video_add_subtitles"
    override val description: String = "Incrusta o incrusta subtítulos (subtítulo SRT/ASS) en un archivo de video mediante FFmpeg."
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
        return "Simulación exitosa: video_add_subtitles ejecutada con argumentos: $args"
    }
}
