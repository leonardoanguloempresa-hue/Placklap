package com.robopal.app.agent.tools

import com.robopal.app.RoboPalApplication
import com.robopal.app.agent.Tool

class DownloadTool : Tool {
    override val name: String = "download"
    override val description: String = "Descarga un archivo desde una URL especificada utilizando el cliente OkHttp."
    override val parameterSchema: Map<String, Any> = mapOf(
        "type" to "object",
        "properties" to mapOf(
            "url" to mapOf("type" to "string", "description" to "URL remota del archivo a descargar"),
            "destinationPath" to mapOf("type" to "string", "description" to "Ruta local donde se guardará el archivo")
        ),
        "required" to listOf("url", "destinationPath")
    )

    override suspend fun execute(args: Map<String, Any>): String {
        val url = args["url"] as? String
            ?: return "Error: Parámetro 'url' no válido."
        val destinationPath = args["destinationPath"] as? String
            ?: return "Error: Parámetro 'destinationPath' no válido."

        return RoboPalApplication.downloadManager.downloadFile(url, destinationPath)
    }
}
