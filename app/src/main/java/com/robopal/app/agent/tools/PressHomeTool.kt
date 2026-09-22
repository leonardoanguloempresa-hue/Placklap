package com.robopal.app.agent.tools

import com.robopal.app.agent.Tool

class PressHomeTool : Tool {
    override val name: String = "press_home"
    override val description: String = "Simula pulsar el botón Inicio (Home) del sistema Android."
    override val parameterSchema: Map<String, Any> = mapOf(
        "type" to "object",
        "properties" to emptyMap<String, Any>()
    )

    override suspend fun execute(args: Map<String, Any>): String {
        return "Simulación exitosa: press_home ejecutada con argumentos: $args"
    }
}
