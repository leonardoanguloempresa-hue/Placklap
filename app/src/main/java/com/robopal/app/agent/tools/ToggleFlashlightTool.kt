package com.robopal.app.agent.tools

import android.content.Context
import android.hardware.camera2.CameraManager
import com.robopal.app.RoboPalApplication
import com.robopal.app.agent.Tool

class ToggleFlashlightTool : Tool {
    override val name: String = "toggle_flashlight"
    override val description: String = "Enciende o apaga la linterna/flash de la cámara trasera del dispositivo."
    override val parameterSchema: Map<String, Any> = mapOf(
        "type" to "object",
        "properties" to mapOf(
            "enabled" to mapOf("type" to "boolean", "description" to "true para encender la linterna, false para apagarla")
        ),
        "required" to listOf("enabled")
    )

    override suspend fun execute(args: Map<String, Any>): String {
        val enabled = args["enabled"] as? Boolean
            ?: return "Error: Parámetro 'enabled' (booleano) no especificado."

        val context: Context = RoboPalApplication.instance
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            ?: return "Error: No se pudo obtener el servicio CameraManager del sistema."

        return try {
            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                val characteristics = cameraManager.getCameraCharacteristics(id)
                val hasFlash = characteristics.get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE)
                val facing = characteristics.get(android.hardware.camera2.CameraCharacteristics.LENS_FACING)
                hasFlash == true && facing == android.hardware.camera2.CameraCharacteristics.LENS_FACING_BACK
            } ?: cameraManager.cameraIdList.firstOrNull()
            ?: return "Error: No se encontró cámara trasera con linterna disponible."

            cameraManager.setTorchMode(cameraId, enabled)
            val stateStr = if (enabled) "encendida" else "apagada"
            "Linterna $stateStr con éxito en el hardware real."
        } catch (e: Exception) {
            "Error al controlar la linterna del hardware: ${e.localizedMessage ?: e.message}"
        }
    }
}
