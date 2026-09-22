package com.robopal.app.agent.tools

import com.robopal.app.agent.Tool

class SwipeTool : Tool {
    override val name: String = "swipe"
    override val description: String = "Realiza un gesto de deslizamiento desde (startX, startY) hasta (endX, endY)."
    override val parameterSchema: Map<String, Any> = mapOf(
        "type" to "object",
        "properties" to mapOf(
            "startX" to mapOf("type" to "number", "description" to "Coordenada X inicial"),
            "startY" to mapOf("type" to "number", "description" to "Coordenada Y inicial"),
            "endX" to mapOf("type" to "number", "description" to "Coordenada X final"),
            "endY" to mapOf("type" to "number", "description" to "Coordenada Y final"),
            "duration" to mapOf("type" to "number", "description" to "Duración del gesto en ms")
        ),
        "required" to listOf("startX", "startY", "endX", "endY")
    )

    override suspend fun execute(args: Map<String, Any>): String {
        return "Simulación exitosa: swipe ejecutada con argumentos: $args"
    }
}
