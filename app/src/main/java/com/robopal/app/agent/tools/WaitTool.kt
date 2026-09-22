package com.robopal.app.agent.tools

import com.robopal.app.agent.Tool
import kotlinx.coroutines.delay

class WaitTool : Tool {
    override val name: String = "wait"
    override val description: String = "Pausa la ejecución del agente durante una cantidad especificada de milisegundos."
    override val parameterSchema: Map<String, Any> = mapOf(
        "type" to "object",
        "properties" to mapOf(
            "milliseconds" to mapOf(
                "type" to "number",
                "description" to "Tiempo a esperar en milisegundos"
            )
        ),
        "required" to listOf("milliseconds")
    )

    override suspend fun execute(args: Map<String, Any>): String {
        val millis = (args["milliseconds"] as? Number)?.toLong() ?: 1000L
        delay(millis)
        return "Espera completada por $millis ms."
    }
}
