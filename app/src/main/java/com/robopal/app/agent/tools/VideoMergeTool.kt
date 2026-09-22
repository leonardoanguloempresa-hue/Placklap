package com.robopal.app.agent.tools

import com.robopal.app.RoboPalApplication
import com.robopal.app.agent.Tool

class VideoMergeTool : Tool {
    override val name: String = "video_merge"
    override val description: String = "Concatena y combina múltiples archivos de video en uno solo usando FFmpeg."
    override val parameterSchema: Map<String, Any> = mapOf(
        "type" to "object",
        "properties" to mapOf(
            "inputPaths" to mapOf(
                "type" to "array",
                "items" to mapOf("type" to "string"),
                "description" to "Lista de rutas de archivos de video a concatenar"
            ),
            "outputPath" to mapOf("type" to "string", "description" to "Ruta del archivo de salida")
        ),
        "required" to listOf("inputPaths", "outputPath")
    )

    @Suppress("UNCHECKED_CAST")
    override suspend fun execute(args: Map<String, Any>): String {
        val inputPaths = args["inputPaths"] as? List<String>
            ?: return "Error: Parámetro 'inputPaths' no válido."
        val outputPath = args["outputPath"] as? String
            ?: return "Error: Parámetro 'outputPath' no válido."

        return RoboPalApplication.ffmpegManager.mergeVideos(inputPaths, outputPath)
    }
}
