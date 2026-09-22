package com.robopal.app.agent.tools

import com.robopal.app.agent.Tool
import com.robopal.app.services.AgentAccessibilityService

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
        val service = AgentAccessibilityService.instance
            ?: return "Error: AgentAccessibilityService no está activo. Solicita al usuario que lo habilite en los ajustes de Accesibilidad."

        val x = (args["x"] as? Number)?.toFloat()
            ?: return "Error: Parámetro 'x' no válido."
        val y = (args["y"] as? Number)?.toFloat()
            ?: return "Error: Parámetro 'y' no válido."

        val success = service.tap(x, y)
        return if (success) {
            "Toque realizado con éxito en coordenadas ($x, $y)."
        } else {
            "Error al ejecutar toque en ($x, $y)."
        }
    }
}
