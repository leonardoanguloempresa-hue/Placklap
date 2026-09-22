package com.robopal.app.agent.tools

import com.robopal.app.agent.Tool

class ReadScreenTool : Tool {
    override val name: String = "read_screen"
    override val description: String = "Lee la jerarquía de nodos e información de la pantalla actual vía Accesibilidad."
    override val parameterSchema: Map<String, Any> = mapOf(
        "type" to "object",
        "properties" to emptyMap<String, Any>()
    )

    override suspend fun execute(args: Map<String, Any>): String {
        return "Simulación exitosa: read_screen ejecutada con argumentos: $args"
    }
}
