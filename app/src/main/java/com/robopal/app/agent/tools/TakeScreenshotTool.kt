package com.robopal.app.agent.tools

import com.robopal.app.agent.Tool

class TakeScreenshotTool : Tool {
    override val name: String = "take_screenshot"
    override val description: String = "Captura una imagen de la pantalla actual usando el servicio de captura o accesibilidad."
    override val parameterSchema: Map<String, Any> = mapOf(
        "type" to "object",
        "properties" to emptyMap<String, Any>()
    )

    override suspend fun execute(args: Map<String, Any>): String {
        return "Simulación exitosa: take_screenshot ejecutada con argumentos: $args"
    }
}
