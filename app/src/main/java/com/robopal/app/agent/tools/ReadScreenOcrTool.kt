package com.robopal.app.agent.tools

import com.robopal.app.agent.Tool

class ReadScreenOcrTool : Tool {
    override val name: String = "read_screen_ocr"
    override val description: String = "Captura la pantalla actual y realiza reconocimiento óptico de caracteres (OCR) mediante ML Kit."
    override val parameterSchema: Map<String, Any> = mapOf(
        "type" to "object",
        "properties" to emptyMap<String, Any>()
    )

    override suspend fun execute(args: Map<String, Any>): String {
        return "Simulación exitosa: read_screen_ocr ejecutada con argumentos: $args"
    }
}
