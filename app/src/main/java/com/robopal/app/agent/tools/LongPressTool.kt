package com.robopal.app.agent.tools

import com.robopal.app.agent.Tool

class LongPressTool : Tool {
    override val name: String = "long_press"
    override val description: String = "Mantiene presionada la pantalla en las coordenadas especificados (x, y)."
    override val parameterSchema: Map<String, Any> = mapOf(
        "type" to "object",
        "properties" to mapOf(
            "x" to mapOf("type" to "number", "description" to "Coordenada X"),
            "y" to mapOf("type" to "number", "description" to "Coordenada Y"),
            "duration" to mapOf("type" to "number", "description" to "Duración de la pulsación en ms")
        ),
        "required" to listOf("x", "y")
    )

    override suspend fun execute(args: Map<String, Any>): String {
        return "Simulación exitosa: long_press ejecutada con argumentos: $args"
    }
}
