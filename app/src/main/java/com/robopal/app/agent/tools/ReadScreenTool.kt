package com.robopal.app.agent.tools

import com.robopal.app.agent.Tool
import com.robopal.app.services.AgentAccessibilityService

class ReadScreenTool : Tool {
    override val name: String = "read_screen"
    override val description: String = "Lee la jerarquía de nodos e información de la pantalla actual vía Accesibilidad."
    override val parameterSchema: Map<String, Any> = mapOf(
        "type" to "object",
        "properties" to emptyMap<String, Any>()
    )

    override suspend fun execute(args: Map<String, Any>): String {
        val service = AgentAccessibilityService.instance
            ?: return "Error: AgentAccessibilityService no está activo. Solicita al usuario que lo habilite en los ajustes de Accesibilidad."

        return service.readScreenState()
    }
}
