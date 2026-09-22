package com.robopal.app.agent.tools

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import com.robopal.app.agent.Tool
import com.robopal.app.services.AgentAccessibilityService

class FindAndTapTool : Tool {
    override val name: String = "find_and_tap"
    override val description: String = "Busca un elemento en la pantalla por su texto o descripción y realiza un toque en él."
    override val parameterSchema: Map<String, Any> = mapOf(
        "type" to "object",
        "properties" to mapOf(
            "text" to mapOf("type" to "string", "description" to "Texto o descripción accesible a buscar")
        ),
        "required" to listOf("text")
    )

    override suspend fun execute(args: Map<String, Any>): String {
        val service = AgentAccessibilityService.instance
            ?: return "Error: AgentAccessibilityService no está activo. Solicita al usuario que lo habilite en los ajustes de Accesibilidad."

        val searchText = args["text"] as? String
            ?: return "Error: Parámetro 'text' no especificado."

        val root = service.rootInActiveWindow
            ?: return "Error: No se pudo obtener la ventana activa de la pantalla."

        val targetNode = findNodeByText(root, searchText)
            ?: return "No se encontró ningún elemento en la pantalla con el texto/descripción '$searchText'."

        val bounds = Rect()
        targetNode.getBoundsInScreen(bounds)
        val centerX = bounds.exactCenterX()
        val centerY = bounds.exactCenterY()

        val success = service.tap(centerX, centerY)
        return if (success) {
            "Elemento '$searchText' encontrado y tocado con éxito en ($centerX, $centerY)."
        } else {
            "Error al tocar el elemento '$searchText' en ($centerX, $centerY)."
        }
    }

    private fun findNodeByText(node: AccessibilityNodeInfo?, target: String): AccessibilityNodeInfo? {
        if (node == null) return null

        if (node.isVisibleToUser) {
            val text = node.text?.toString()
            val desc = node.contentDescription?.toString()
            if ((text != null && text.contains(target, ignoreCase = true)) ||
                (desc != null && desc.contains(target, ignoreCase = true))
            ) {
                return node
            }
        }

        for (i in 0 until node.childCount) {
            val match = findNodeByText(node.getChild(i), target)
            if (match != null) return match
        }

        return null
    }
}
