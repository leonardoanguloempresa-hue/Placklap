package com.robopal.app.agent.tools

import com.robopal.app.agent.Tool
import com.robopal.app.services.AgentAccessibilityService

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
        val service = AgentAccessibilityService.instance
            ?: return "Error: AgentAccessibilityService no está activo. Solicita al usuario que lo habilite en los ajustes de Accesibilidad."

        val startX = (args["startX"] as? Number)?.toFloat()
            ?: return "Error: Parámetro 'startX' no válido."
        val startY = (args["startY"] as? Number)?.toFloat()
            ?: return "Error: Parámetro 'startY' no válido."
        val endX = (args["endX"] as? Number)?.toFloat()
            ?: return "Error: Parámetro 'endX' no válido."
        val endY = (args["endY"] as? Number)?.toFloat()
            ?: return "Error: Parámetro 'endY' no válido."
        val durationMs = (args["duration"] as? Number)?.toLong() ?: 300L

        val success = service.swipe(startX, startY, endX, endY, durationMs)
        return if (success) {
            "Deslizamiento realizado con éxito desde ($startX, $startY) hasta ($endX, $endY)."
        } else {
            "Error al ejecutar deslizamiento."
        }
    }
}
