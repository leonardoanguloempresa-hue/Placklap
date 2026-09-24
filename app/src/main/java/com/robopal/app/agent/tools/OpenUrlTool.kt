package com.robopal.app.agent.tools

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.robopal.app.RoboPalApplication
import com.robopal.app.agent.Tool
import com.robopal.app.safety.RiskLevel

class OpenUrlTool : Tool {
    override val name: String = "open_url"
    override val description: String = "Abre una URL web segura en el navegador después de la validación del esquema."
    override val riskLevel: RiskLevel = RiskLevel.MEDIUM
    override val parameterSchema: Map<String, Any> = mapOf(
        "type" to "object",
        "properties" to mapOf(
            "url" to mapOf("type" to "string", "description" to "URL a abrir (http o https)")
        ),
        "required" to listOf("url")
    )

    override suspend fun validate(args: Map<String, Any>): String? {
        val url = (args["url"] as? String)?.trim()
        if (url.isNullOrBlank()) {
            return "URL vacía no permitida."
        }

        val lowerUrl = url.lowercase()
        if (!lowerUrl.startsWith("http://") && !lowerUrl.startsWith("https://")) {
            return "Esquema no permitido. Solo se aceptan URLs que comiencen por http:// o https://."
        }

        if (lowerUrl.startsWith("file:") || lowerUrl.startsWith("intent:") || lowerUrl.startsWith("javascript:") || lowerUrl.startsWith("content:")) {
            return "Esquema peligroso rechazado."
        }

        return if (!url.contains(".") || url.contains(" ")) {
            "URL malformada."
        } else {
            null
        }
    }

    override suspend fun execute(args: Map<String, Any>): String {
        val url = (args["url"] as String).trim()
        val context: Context = RoboPalApplication.instance

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            context.startActivity(intent)
            "URL '$url' abierta con éxito en el navegador."
        } catch (e: Exception) {
            "Error al abrir la URL '$url': ${e.localizedMessage ?: e.message}"
        }
    }
}
