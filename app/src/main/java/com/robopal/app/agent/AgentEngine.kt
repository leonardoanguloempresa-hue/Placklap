package com.robopal.app.agent

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AgentEngine(
    private val toolRegistry: ToolRegistry = ToolRegistry(),
    private val llmProvider: LlmProvider = MockLlmProvider()
) {
    private val _state = MutableStateFlow(AgentState.IDLE)
    val state: StateFlow<AgentState> = _state.asStateFlow()

    companion object {
        const val SYSTEM_PROMPT =
            "Eres RoboPal, un agente de automatización de Android. Tu trabajo es cumplir la meta del usuario usando tus herramientas. Planifica pasos cortos, ejecuta UNA herramienta a la vez, observa el resultado y decide el siguiente paso. Cuando termines, responde con un resumen breve en español. NUNCA inventes resultados de herramientas."
        private const val MAX_ITERATIONS = 15
    }

    suspend fun agentLoop(goal: String) {
        _state.value = AgentState.WORKING

        val messages = mutableListOf<Message>()
        messages.add(Message(role = "system", content = SYSTEM_PROMPT))
        messages.add(Message(role = "user", content = goal))

        var iterations = 0
        var completed = false

        while (iterations < MAX_ITERATIONS && !completed) {
            iterations++
            _state.value = AgentState.THINKING

            val response = try {
                llmProvider.generateResponse(messages, toolRegistry.getAllTools())
            } catch (e: Exception) {
                messages.add(
                    Message(
                        role = "assistant",
                        content = "Error al comunicarse con el LLM: ${e.localizedMessage ?: e.message}"
                    )
                )
                _state.value = AgentState.ERROR
                return
            }

            val assistantContent = response.content ?: ""
            val toolCalls = response.toolCalls

            messages.add(
                Message(
                    role = "assistant",
                    content = assistantContent,
                    toolCalls = toolCalls
                )
            )

            if (!toolCalls.isNullOrEmpty()) {
                _state.value = AgentState.WORKING
                for (toolCall in toolCalls) {
                    val result = toolRegistry.executeTool(toolCall.name, toolCall.arguments)
                    messages.add(
                        Message(
                            role = "tool",
                            content = result,
                            toolCallId = toolCall.id
                        )
                    )
                }
            } else {
                completed = true
            }
        }

        if (_state.value != AgentState.ERROR) {
            _state.value = AgentState.IDLE
        }
    }
}
