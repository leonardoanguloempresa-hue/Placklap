package com.robopal.app.agent

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.util.Log
import android.view.accessibility.AccessibilityManager
import com.robopal.app.RoboPalApplication
import com.robopal.app.agent.tools.DownloadTool
import com.robopal.app.agent.tools.FindAndTapTool
import com.robopal.app.agent.tools.LongPressTool
import com.robopal.app.agent.tools.OpenAppTool
import com.robopal.app.agent.tools.OpenUrlTool
import com.robopal.app.agent.tools.PressBackTool
import com.robopal.app.agent.tools.PressHomeTool
import com.robopal.app.agent.tools.PressRecentTool
import com.robopal.app.agent.tools.ReadScreenOcrTool
import com.robopal.app.agent.tools.ReadScreenTool
import com.robopal.app.agent.tools.SwipeTool
import com.robopal.app.agent.tools.TakeScreenshotTool
import com.robopal.app.agent.tools.TapTool
import com.robopal.app.agent.tools.ToggleFlashlightTool
import com.robopal.app.agent.tools.TypeTextTool
import com.robopal.app.agent.tools.VideoAddSubtitlesTool
import com.robopal.app.agent.tools.VideoCutTool
import com.robopal.app.agent.tools.VideoMergeTool
import com.robopal.app.agent.tools.WaitTool
import com.robopal.app.managers.Logger
import com.robopal.app.safety.AlwaysApproveConfirmationHandler
import com.robopal.app.safety.RiskConfirmationHandler
import com.robopal.app.safety.RiskLevel
import com.robopal.app.safety.ToolExecutionPolicy
import com.robopal.app.services.AgentAccessibilityService
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

class ToolRegistry(
    var confirmationHandler: RiskConfirmationHandler = AlwaysApproveConfirmationHandler(),
    var policy: ToolExecutionPolicy = ToolExecutionPolicy()
) {

    companion object {
        private const val TAG = "ToolRegistry"
    }

    private val tools: MutableMap<String, Tool> = mutableMapOf()

    init {
        registerTool(TapTool())
        registerTool(SwipeTool())
        registerTool(LongPressTool())
        registerTool(TypeTextTool())
        registerTool(PressBackTool())
        registerTool(PressHomeTool())
        registerTool(PressRecentTool())
        registerTool(ReadScreenTool())
        registerTool(ReadScreenOcrTool())
        registerTool(OpenAppTool())
        registerTool(FindAndTapTool())
        registerTool(WaitTool())
        registerTool(VideoCutTool())
        registerTool(VideoMergeTool())
        registerTool(VideoAddSubtitlesTool())
        registerTool(DownloadTool())
        registerTool(OpenUrlTool())
        registerTool(TakeScreenshotTool())
        registerTool(ToggleFlashlightTool())
    }

    fun registerTool(tool: Tool) {
        tools[tool.name] = tool
    }

    fun getTool(name: String): Tool? {
        return tools[name]
    }

    fun getAllTools(): List<Tool> {
        return tools.values.toList()
    }

    private fun isAccessibilityEnabledBySystem(): Boolean {
        val context = RoboPalApplication.instance
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager ?: return false
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        return enabledServices.any { it.resolveInfo.serviceInfo.packageName == context.packageName }
    }

    suspend fun executeTool(name: String, args: Map<String, Any>): String {
        val tool = getTool(name)
            ?: run {
                val errorMsg = "Error: Herramienta '$name' no encontrada en el catálogo."
                Logger.logToolExecution(name, args, errorMsg)
                return errorMsg
            }

        // 1. Dry Run check
        if (policy.isDryRun) {
            val dryMsg = "[DRY-RUN] Simulación exitosa de herramienta '$name' con params $args."
            Logger.logToolExecution(name, args, dryMsg)
            return dryMsg
        }

        // 2. Risk Level confirmation
        if (tool.riskLevel == RiskLevel.HIGH) {
            val approved = confirmationHandler.requestUserConfirmation(
                toolName = tool.name,
                riskLevel = tool.riskLevel,
                arguments = args,
                description = tool.description
            )
            if (!approved) {
                val rejectMsg = "Acción cancelada: El usuario rechazó la ejecución de la herramienta de alto riesgo '$name'."
                Logger.logToolExecution(name, args, rejectMsg)
                return rejectMsg
            }
        }

        // 3. Accessibility Requirement check
        val requiresAccessibility = name != "wait" && name != "download" && name != "toggle_flashlight"
        if (requiresAccessibility) {
            var instance = AgentAccessibilityService.instance
            val enabledBySystem = isAccessibilityEnabledBySystem()

            Log.i("RoboPal", "Accesibilidad: instance=$instance, enabledBySystem=$enabledBySystem")

            if (instance == null && enabledBySystem) {
                Log.w(TAG, "Instancia perdida, esperando reconexión del sistema…")
                delay(1000)
                instance = AgentAccessibilityService.instance
            }

            if (instance == null) {
                val accError = "El servicio de accesibilidad no responde. Desactívalo y actívalo de nuevo en Ajustes."
                Logger.logToolExecution(name, args, accError)
                return accError
            }
        }

        // 4. Custom validation
        val validationError = tool.validate(args)
        if (validationError != null) {
            val valMsg = "Error de validación en '$name': $validationError"
            Logger.logToolExecution(name, args, valMsg)
            return valMsg
        }

        // 5. Execution with timeout limit
        val result = withTimeoutOrNull(policy.timeoutMsPerTool) {
            try {
                tool.execute(args)
            } catch (e: Exception) {
                "Error al ejecutar la herramienta '$name': ${e.localizedMessage ?: e.message}"
            }
        } ?: "Error: La ejecución de la herramienta '$name' superó el límite de tiempo (${policy.timeoutMsPerTool} ms)."

        Logger.logToolExecution(name, args, result)
        return result
    }
}
