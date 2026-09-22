package com.robopal.app.agent.tools

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.robopal.app.RoboPalApplication
import com.robopal.app.agent.Tool

class OpenUrlTool : Tool {
    override val name: String = "open_url"
    override val description: String = "Abre un enlace o URL especificada en el navegador del sistema Android."
    override val parameterSchema: Map<String, Any> = mapOf(
        "type" to "object",
        "properties" to mapOf(
            "url" to mapOf("type" to "string", "description" to "Dirección URL a abrir (ej. https://example.com)")
        ),
        "required" to listOf("url")
    )

    override suspend fun execute(args: Map<String, Any>): String {
        val url = args["url"] as? String
            ?: return "Error: Parámetro 'url' no especificado."

        val context: Context = RoboPalApplication.instance
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "URL '$url' abierta con éxito en el navegador."
        } catch (e: Exception) {
            "Error al abrir URL '$url': ${e.localizedMessage ?: e.message}"
        }
    }
}
