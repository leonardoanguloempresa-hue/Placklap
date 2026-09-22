package com.robopal.app.agent

data class Message(
    val role: String, // "system", "user", "assistant", "tool"
    val content: String,
    val toolCalls: List<ToolCall>? = null,
    val toolCallId: String? = null
)

data class ToolCall(
    val id: String,
    val name: String,
    val arguments: Map<String, Any>
)

data class LlmResponse(
    val content: String?,
    val toolCalls: List<ToolCall>? = null
)

interface LlmProvider {
    suspend fun generateResponse(
        messages: List<Message>,
        tools: List<Tool>
    ): LlmResponse
}

class MockLlmProvider : LlmProvider {
    override suspend fun generateResponse(
        messages: List<Message>,
        tools: List<Tool>
    ): LlmResponse {
        return LlmResponse(
            content = "Tarea procesada correctamente por el agente simulado.",
            toolCalls = null
        )
    }
}
