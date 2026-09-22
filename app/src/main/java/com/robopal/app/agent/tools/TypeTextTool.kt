package com.robopal.app.agent.tools

import com.robopal.app.agent.Tool
import com.robopal.app.services.RobotImeService

class TypeTextTool : Tool {
    override val name: String = "type_text"
    override val description: String = "Escribe un texto en el campo enfocado usando el servicio IME de RoboPal."
    override val parameterSchema: Map<String, Any> = mapOf(
        "type" to "object",
        "properties" to mapOf(
            "text" to mapOf("type" to "string", "description" to "Texto a escribir")
        ),
        "required" to listOf("text")
    )

    override suspend fun execute(args: Map<String, Any>): String {
        val text = args["text"] as? String
            ?: return "Error: Parámetro 'text' no especificado."

        if (RobotImeService.instance == null) {
            return "Error: RobotImeService no está activo. Solicita al usuario que seleccione el teclado RoboPal en los ajustes del sistema."
        }

        RobotImeService.pendingText = text
        return "Texto '$text' preparado en RobotImeService para su escritura en el campo enfocado."
    }
}
