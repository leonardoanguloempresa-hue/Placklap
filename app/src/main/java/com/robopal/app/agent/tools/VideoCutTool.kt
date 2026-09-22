package com.robopal.app.agent.tools

import com.robopal.app.RoboPalApplication
import com.robopal.app.agent.Tool

class VideoCutTool : Tool {
    override val name: String = "video_cut"
    override val description: String = "Corta un fragmento de video desde un tiempo inicial hasta un tiempo final usando FFmpeg."
    override val parameterSchema: Map<String, Any> = mapOf(
        "type" to "object",
        "properties" to mapOf(
            "inputPath" to mapOf("type" to "string", "description" to "Ruta del archivo de video de entrada"),
            "outputPath" to mapOf("type" to "string", "description" to "Ruta del archivo de salida"),
            "startTime" to mapOf("type" to "string", "description" to "Tiempo de inicio en formato HH:MM:SS o segundos"),
            "endTime" to mapOf("type" to "string", "description" to "Tiempo final en formato HH:MM:SS o segundos")
        ),
        "required" to listOf("inputPath", "outputPath", "startTime", "endTime")
    )

    override suspend fun execute(args: Map<String, Any>): String {
        val inputPath = args["inputPath"] as? String
            ?: return "Error: Parámetro 'inputPath' no válido."
        val outputPath = args["outputPath"] as? String
            ?: return "Error: Parámetro 'outputPath' no válido."
        val startTime = args["startTime"] as? String
            ?: return "Error: Parámetro 'startTime' no válido."
        val endTime = args["endTime"] as? String
            ?: return "Error: Parámetro 'endTime' no válido."

        return RoboPalApplication.ffmpegManager.cutVideo(inputPath, outputPath, startTime, endTime)
    }
}
