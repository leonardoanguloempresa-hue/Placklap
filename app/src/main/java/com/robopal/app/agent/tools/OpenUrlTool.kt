package com.robopal.app.agent.tools

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
        return "Simulación exitosa: open_url ejecutada con argumentos: $args"
    }
}
