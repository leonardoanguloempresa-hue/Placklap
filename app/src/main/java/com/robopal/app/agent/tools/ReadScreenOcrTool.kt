package com.robopal.app.agent.tools

import com.robopal.app.RoboPalApplication
import com.robopal.app.agent.Tool
import com.robopal.app.services.ScreenCaptureService

class ReadScreenOcrTool : Tool {
    override val name: String = "read_screen_ocr"
    override val description: String = "Captura la pantalla actual y realiza reconocimiento óptico de caracteres (OCR) mediante ML Kit."
    override val parameterSchema: Map<String, Any> = mapOf(
        "type" to "object",
        "properties" to emptyMap<String, Any>()
    )

    override suspend fun execute(args: Map<String, Any>): String {
        val captureService = ScreenCaptureService.instance
            ?: return "Error: ScreenCaptureService no está activo. Inicie la captura de pantalla en el servicio correspondiente."

        val bitmap = captureService.captureBitmap()
            ?: return "Error: No se pudo obtener la captura de pantalla de ScreenCaptureService."

        return RoboPalApplication.ocrManager.extractTextFromBitmap(bitmap)
    }
}
