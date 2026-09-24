package com.robopal.app.agent.tools

import com.robopal.app.agent.Tool
import com.robopal.app.safety.RiskLevel
import java.io.File

class DownloadTool : Tool {
    override val name: String = "download"
    override val description: String = "Descarga un archivo desde una URL válida hacia una ruta de salida."
    override val riskLevel: RiskLevel = RiskLevel.MEDIUM
    override val parameterSchema: Map<String, Any> = mapOf(
        "type" to "object",
        "properties" to mapOf(
            "url" to mapOf("type" to "string", "description" to "URL de origen"),
            "outputPath" to mapOf("type" to "string", "description" to "Ruta local de destino")
        ),
        "required" to listOf("url", "outputPath")
    )

    override suspend fun validate(args: Map<String, Any>): String? {
        val url = args["url"] as? String
        val outputPath = args["outputPath"] as? String

        if (url.isNullOrBlank() || !url.startsWith("http")) {
            return "URL inválida o insegura. Debe comenzar por http/https."
        }
        if (outputPath.isNullOrBlank()) {
            return "Ruta de salida no especificada."
        }

        // Path Traversal Check
        val file = File(outputPath)
        if (file.canonicalPath.contains("..")) {
            return "Ruta insegura detectada (Path Traversal prohibido)."
        }

        return null
    }

    override suspend fun execute(args: Map<String, Any>): String {
        val url = args["url"] as String
        val outputPath = args["outputPath"] as String
        return com.robopal.app.RoboPalApplication.downloadManager.downloadFile(url, outputPath)
    }
}
