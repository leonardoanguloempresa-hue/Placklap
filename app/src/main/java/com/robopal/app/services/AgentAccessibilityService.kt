package com.robopal.app.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class AgentAccessibilityService : AccessibilityService() {

    companion object {
        var instance: AgentAccessibilityService? = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Eventos procesados si se requieren
    }

    override fun onInterrupt() {
        // Interrupción del servicio
    }

    val activePackageName: String?
        get() = rootInActiveWindow?.packageName?.toString()

    suspend fun tap(x: Float, y: Float): Boolean = suspendCancellableCoroutine { continuation ->
        val path = Path().apply {
            moveTo(x, y)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, 100)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()

        val callback = object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                if (continuation.isActive) {
                    continuation.resume(true)
                }
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                if (continuation.isActive) {
                    continuation.resume(false)
                }
            }
        }

        val result = dispatchGesture(gesture, callback, null)
        if (!result && continuation.isActive) {
            continuation.resume(false)
        }
    }

    suspend fun swipe(x1: Float, y1: Float, x2: Float, y2: Float, durationMs: Long): Boolean =
        suspendCancellableCoroutine { continuation ->
            val path = Path().apply {
                moveTo(x1, y1)
                lineTo(x2, y2)
            }
            val stroke = GestureDescription.StrokeDescription(path, 0, durationMs.coerceAtLeast(100))
            val gesture = GestureDescription.Builder().addStroke(stroke).build()

            val callback = object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    if (continuation.isActive) {
                        continuation.resume(true)
                    }
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    if (continuation.isActive) {
                        continuation.resume(false)
                    }
                }
            }

            val result = dispatchGesture(gesture, callback, null)
            if (!result && continuation.isActive) {
                continuation.resume(false)
            }
        }

    fun readScreenState(): String {
        val root = rootInActiveWindow ?: return "Error: No se pudo obtener la ventana activa de la pantalla."
        val pkg = root.packageName?.toString() ?: "Desconocida"
        val builder = StringBuilder()
        builder.append("App Activa: $pkg\n")
        traverseNode(root, builder)
        return if (builder.length <= "App Activa: $pkg\n".length) {
            "Pantalla de app $pkg vacía o sin elementos de texto/accesibilidad detectables."
        } else {
            builder.toString().trimEnd()
        }
    }

    private fun traverseNode(node: AccessibilityNodeInfo?, builder: StringBuilder) {
        if (node == null) return

        if (node.isVisibleToUser) {
            val text = node.text?.toString()?.takeIf { it.isNotBlank() }
            val contentDesc = node.contentDescription?.toString()?.takeIf { it.isNotBlank() }

            if (text != null || contentDesc != null || node.isClickable) {
                val bounds = Rect()
                node.getBoundsInScreen(bounds)
                builder.append("[Bounds: (${bounds.left},${bounds.top},${bounds.right},${bounds.bottom})]")
                if (text != null) {
                    builder.append(" - [Texto: \"$text\"]")
                }
                if (contentDesc != null) {
                    builder.append(" - [ContentDescription: \"$contentDesc\"]")
                }
                builder.append(" - [Clickable: ${node.isClickable}]\n")
            }
        }

        for (i in 0 until node.childCount) {
            traverseNode(node.getChild(i), builder)
        }
    }
}
