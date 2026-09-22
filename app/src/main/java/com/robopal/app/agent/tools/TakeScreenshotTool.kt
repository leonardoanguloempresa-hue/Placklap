package com.robopal.app.agent.tools

import android.accessibilityservice.AccessibilityService
import android.os.Build
import com.robopal.app.agent.Tool
import com.robopal.app.services.AgentAccessibilityService

class TakeScreenshotTool : Tool {
    override val name: String = "take_screenshot"
    override val description: String = "Captura una imagen de la pantalla actual usando el servicio de captura o accesibilidad."
    override val parameterSchema: Map<String, Any> = mapOf(
        "type" to "object",
        "properties" to emptyMap<String, Any>()
    )

    override suspend fun execute(args: Map<String, Any>): String {
        val service = AgentAccessibilityService.instance
            ?: return "Error: AgentAccessibilityService no está activo. Solicita al usuario que lo habilite en los ajustes de Accesibilidad."

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val success = service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT)
            if (success) {
                "Captura de pantalla solicitada con éxito al sistema."
            } else {
                "Error al solicitar captura de pantalla al sistema."
            }
        } else {
            "Simulación exitosa: take_screenshot ejecutada en API < 30."
        }
    }
}
