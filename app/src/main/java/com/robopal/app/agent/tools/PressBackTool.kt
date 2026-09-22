package com.robopal.app.agent.tools

import com.robopal.app.agent.Tool

class PressBackTool : Tool {
    override val name: String = "press_back"
    override val description: String = "Simula pulsar el botón Atrás del sistema Android."
    override val parameterSchema: Map<String, Any> = mapOf(
        "type" to "object",
        "properties" to emptyMap<String, Any>()
    )

    override suspend fun execute(args: Map<String, Any>): String {
        return "Simulación exitosa: press_back ejecutada con argumentos: $args"
    }
}
