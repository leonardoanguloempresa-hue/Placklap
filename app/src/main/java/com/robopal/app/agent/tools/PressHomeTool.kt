package com.robopal.app.agent.tools

import android.accessibilityservice.AccessibilityService
import com.robopal.app.agent.Tool
import com.robopal.app.services.AgentAccessibilityService

class PressHomeTool : Tool {
    override val name: String = "press_home"
    override val description: String = "Simula pulsar el botón Inicio (Home) del sistema Android."
    override val parameterSchema: Map<String, Any> = mapOf(
        "type" to "object",
        "properties" to emptyMap<String, Any>()
    )

    override suspend fun execute(args: Map<String, Any>): String {
        val service = AgentAccessibilityService.instance
            ?: return "Error: AgentAccessibilityService no está activo. Solicita al usuario que lo habilite en los ajustes de Accesibilidad."

        val success = service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
        return if (success) {
            "Acción de botón Inicio (Home) ejecutada con éxito."
        } else {
            "Error al ejecutar acción de botón Inicio."
        }
    }
}
