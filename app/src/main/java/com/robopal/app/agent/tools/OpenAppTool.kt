package com.robopal.app.agent.tools

import com.robopal.app.agent.Tool

class OpenAppTool : Tool {
    override val name: String = "open_app"
    override val description: String = "Abre una aplicación instalada especificando su nombre o paquete."
    override val parameterSchema: Map<String, Any> = mapOf(
        "type" to "object",
        "properties" to mapOf(
            "packageName" to mapOf("type" to "string", "description" to "Nombre de paquete de la aplicación (ej. com.whatsapp)"),
            "appName" to mapOf("type" to "string", "description" to "Nombre común de la aplicación")
        )
    )

    override suspend fun execute(args: Map<String, Any>): String {
        return "Simulación exitosa: open_app ejecutada con argumentos: $args"
    }
}
