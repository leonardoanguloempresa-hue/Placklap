package com.robopal.app.agent.tools

import com.robopal.app.agent.Tool
import com.robopal.app.services.AgentAccessibilityService

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
        val service = AgentAccessibilityService.instance
            ?: return "Error: AgentAccessibilityService no está activo. Solicita al usuario que lo habilite en los ajustes de Accesibilidad."

        val x = (args["x"] as? Number)?.toFloat()
            ?: return "Error: Parámetro 'x' no válido."
        val y = (args["y"] as? Number)?.toFloat()
            ?: return "Error: Parámetro 'y' no válido."
        val durationMs = (args["duration"] as? Number)?.toLong() ?: 1000L

        val success = service.swipe(x, y, x, y, durationMs)
        return if (success) {
            "Pulsación larga realizada con éxito en ($x, $y) durante $durationMs ms."
        } else {
            "Error al ejecutar pulsación larga en ($x, $y)."
        }
    }
}
