package com.robopal.app.agent.tools

import com.robopal.app.agent.Tool

class PressRecentTool : Tool {
    override val name: String = "press_recent"
    override val description: String = "Simula pulsar el botón de Aplicaciones Recientes (Recents) del sistema Android."
    override val parameterSchema: Map<String, Any> = mapOf(
        "type" to "object",
        "properties" to emptyMap<String, Any>()
    )

    override suspend fun execute(args: Map<String, Any>): String {
        return "Simulación exitosa: press_recent ejecutada con argumentos: $args"
    }
}
