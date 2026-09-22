package com.robopal.app.agent.tools

import com.robopal.app.agent.Tool

class TypeTextTool : Tool {
    override val name: String = "type_text"
    override val description: String = "Escribe un texto en el campo enfocado usando el servicio IME o accesibilidad."
    override val parameterSchema: Map<String, Any> = mapOf(
        "type" to "object",
        "properties" to mapOf(
            "text" to mapOf("type" to "string", "description" to "Texto a escribir")
        ),
        "required" to listOf("text")
    )

    override suspend fun execute(args: Map<String, Any>): String {
        return "Simulación exitosa: type_text ejecutada con argumentos: $args"
    }
}
