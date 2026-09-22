package com.robopal.app.agent.tools

import com.robopal.app.agent.Tool

class TapTool : Tool {
    override val name: String = "tap"
    override val description: String = "Realiza un toque rápido en las coordenadas especificadas (x, y) de la pantalla."
    override val parameterSchema: Map<String, Any> = mapOf(
        "type" to "object",
        "properties" to mapOf(
            "x" to mapOf("type" to "number", "description" to "Coordenada X en píxeles"),
            "y" to mapOf("type" to "number", "description" to "Coordenada Y en píxeles")
        ),
        "required" to listOf("x", "y")
    )

    override suspend fun execute(args: Map<String, Any>): String {
        return "Simulación exitosa: tap ejecutada con argumentos: $args"
    }
}
