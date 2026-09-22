package com.robopal.app.agent.tools

import android.accessibilityservice.AccessibilityService
import com.robopal.app.agent.Tool
import com.robopal.app.services.AgentAccessibilityService

class PressRecentTool : Tool {
    override val name: String = "press_recent"
    override val description: String = "Simula pulsar el botón de Aplicaciones Recientes (Recents) del sistema Android."
    override val parameterSchema: Map<String, Any> = mapOf(
        "type" to "object",
        "properties" to emptyMap<String, Any>()
    )

    override suspend fun execute(args: Map<String, Any>): String {
        val service = AgentAccessibilityService.instance
            ?: return "Error: AgentAccessibilityService no está activo. Solicita al usuario que lo habilite en los ajustes de Accesibilidad."

        val success = service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
        return if (success) {
            "Acción de Aplicaciones Recientes ejecutada con éxito."
        } else {
            "Error al ejecutar acción de Aplicaciones Recientes."
        }
    }
}
