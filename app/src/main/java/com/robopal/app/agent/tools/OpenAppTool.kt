package com.robopal.app.agent.tools

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.robopal.app.RoboPalApplication
import com.robopal.app.agent.Tool

class OpenAppTool : Tool {
    override val name: String = "open_app"
    override val description: String = "Abre una aplicación instalada especificando su nombre común o paquete de forma exacta o parcial."
    override val parameterSchema: Map<String, Any> = mapOf(
        "type" to "object",
        "properties" to mapOf(
            "appName" to mapOf("type" to "string", "description" to "Nombre o etiqueta común de la aplicación (ej. YouTube, Chrome, WhatsApp)"),
            "packageName" to mapOf("type" to "string", "description" to "Nombre de paquete (opcional, ej. com.whatsapp)")
        )
    )

    override suspend fun execute(args: Map<String, Any>): String {
        val context: Context = RoboPalApplication.instance
        val pm = context.packageManager

        val explicitPackage = args["packageName"] as? String
        val appNameArg = (args["appName"] as? String) ?: (args["packageName"] as? String) ?: ""

        if (explicitPackage.isNullOrBlank() && appNameArg.isBlank()) {
            return "Error: Se debe especificar un nombre de aplicación ('appName') o paquete."
        }

        // 1. Verificar por paquete explícito si se proporcionó
        if (!explicitPackage.isNullOrBlank()) {
            val launchIntent = pm.getLaunchIntentForPackage(explicitPackage)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                return "Aplicación con paquete '$explicitPackage' abierta con éxito."
            }
        }

        // 2. Producción estricta: Iterar sobre pm.getInstalledPackages(PackageManager.GET_META_DATA)
        val installedPackages = pm.getInstalledPackages(PackageManager.GET_META_DATA)
        var matchedPackageName: String? = null
        var matchedAppLabel: String? = null

        for (pkgInfo in installedPackages) {
            val appInfo = pkgInfo.applicationInfo ?: continue
            val label = appInfo.loadLabel(pm).toString()

            if (label.equals(appNameArg, ignoreCase = true) ||
                label.contains(appNameArg, ignoreCase = true) ||
                appNameArg.contains(label, ignoreCase = true)
            ) {
                matchedPackageName = pkgInfo.packageName
                matchedAppLabel = label
                break
            }
        }

        if (matchedPackageName != null) {
            val launchIntent = pm.getLaunchIntentForPackage(matchedPackageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                return "Aplicación '$matchedAppLabel' ($matchedPackageName) abierta con éxito."
            }
        }

        return "Error: No se encontró ninguna aplicación instalada que coincida con '$appNameArg'."
    }
}
