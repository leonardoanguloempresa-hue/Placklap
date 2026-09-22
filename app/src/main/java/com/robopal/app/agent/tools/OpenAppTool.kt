package com.robopal.app.agent.tools

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.robopal.app.RoboPalApplication
import com.robopal.app.agent.Tool

class OpenAppTool : Tool {
    override val name: String = "open_app"
    override val description: String = "Abre una aplicación instalada especificando su nombre de paquete."
    override val parameterSchema: Map<String, Any> = mapOf(
        "type" to "object",
        "properties" to mapOf(
            "packageName" to mapOf("type" to "string", "description" to "Nombre de paquete de la aplicación (ej. com.whatsapp)"),
            "appName" to mapOf("type" to "string", "description" to "Nombre común de la aplicación")
        )
    )

    override suspend fun execute(args: Map<String, Any>): String {
        val packageName = args["packageName"] as? String
            ?: return "Error: Parámetro 'packageName' no especificado."

        val context: Context = RoboPalApplication.instance
        val pm = context.packageManager
        val launchIntent = pm.getLaunchIntentForPackage(packageName)

        return if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            "Aplicación '$packageName' abierta con éxito."
        } else {
            "Error: No se encontró la aplicación instalada con el paquete '$packageName'."
        }
    }
}
