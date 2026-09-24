package com.robopal.app.agent

import android.util.Log
import com.robopal.app.RoboPalApplication
import com.robopal.app.managers.Logger
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.coroutineContext

class AgentEngine(
    private val toolRegistry: ToolRegistry = ToolRegistry(),
    private val llmProvider: LlmProvider = MockLlmProvider()
) {
    private val _state = MutableStateFlow(AgentState.IDLE)
    val state: StateFlow<AgentState> = _state.asStateFlow()

    private val _agentMessages = MutableStateFlow<List<Message>>(emptyList())
    val agentMessages: StateFlow<List<Message>> = _agentMessages.asStateFlow()

    private val messages = mutableListOf<Message>()

    companion object {
        private const val TAG = "AgentEngine"
        const val SYSTEM_PROMPT =
            "Eres RoboPal, un agente de automatización de Android. Tu trabajo es cumplir la meta del usuario usando tus herramientas."
        private const val MAX_ITERATIONS = 15
        private const val AGENT_TIMEOUT_MS = 90_000L
    }

    fun clearHistory() {
        messages.clear()
        _agentMessages.value = emptyList()
        _state.value = AgentState.IDLE
        Logger.clearLogs()
    }

    suspend fun agentLoop(goal: String) {
        try {
            val result = withTimeoutOrNull(AGENT_TIMEOUT_MS) {
                agentLoopInternal(goal)
                true
            }

            if (result == null) {
                _state.value = AgentState.ERROR
                val timeoutMsg = "Timeout: no pude completar la tarea."
                val currentMsgs = _agentMessages.value.toMutableList()
                currentMsgs.add(Message("assistant", timeoutMsg))
                _agentMessages.value = currentMsgs
                RoboPalApplication.ttsManager.speak(timeoutMsg)
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Crash en agentLoop: ${t.message}", t)
            _state.value = AgentState.ERROR
            val internalErrorMsg = "Error interno. Intenta de nuevo."
            val currentMsgs = _agentMessages.value.toMutableList()
            currentMsgs.add(Message("assistant", internalErrorMsg))
            _agentMessages.value = currentMsgs
            RoboPalApplication.ttsManager.speak(internalErrorMsg)
        }
    }

    private suspend fun agentLoopInternal(goal: String) {
        val trimmedGoal = goal.trim()
        if (trimmedGoal.length < 3) {
            val shortMsg = "No entendí, ¿puedes repetir?"
            val currentMsgs = _agentMessages.value.toMutableList()
            currentMsgs.add(Message(role = "assistant", content = shortMsg))
            _agentMessages.value = currentMsgs
            RoboPalApplication.ttsManager.speak(shortMsg)
            return
        }

        if (!RoboPalApplication.llmManager.isReady.value) {
            _state.value = AgentState.ERROR
            val notReadyMsg = "No hay modelo cargado. Ve a Modelos y descarga un modelo .task."
            val currentMsgs = _agentMessages.value.toMutableList()
            currentMsgs.add(Message(role = "assistant", content = notReadyMsg))
            _agentMessages.value = currentMsgs
            RoboPalApplication.ttsManager.speak(notReadyMsg)
            return
        }

        _state.value = AgentState.WORKING

        if (messages.isEmpty()) {
            val sysMsg = Message(role = "system", content = SYSTEM_PROMPT)
            messages.add(sysMsg)
        }
        val userMsg = Message(role = "user", content = trimmedGoal)
        messages.add(userMsg)
        _agentMessages.value = messages.toList()

        var iterations = 0
        var completed = false
        var lastAssistantResponse: String? = null
        var fallosConsecutivos = 0

        while (iterations < MAX_ITERATIONS && !completed) {
            coroutineContext.ensureActive()

            iterations++
            _state.value = AgentState.THINKING

            if (!RoboPalApplication.llmManager.isReady.value) {
                _state.value = AgentState.ERROR
                return
            }

            // 1. Truncar historial: system + últimos 6 mensajes, sin huérfanos
            val ultimos = if (messages.size > 8) messages.takeLast(6) else messages.drop(1)
            val historial = if (ultimos.firstOrNull()?.role == "tool") ultimos.drop(1) else ultimos
            val historialCompleto = listOf(messages.first()) + historial

            // 2. Truncar resultados de herramientas grandes
            val messagesFiltrados = historialCompleto.map { msg ->
                if (msg.role == "tool" && msg.content.length > 1500) {
                    msg.copy(content = msg.content.take(1500) + "\n...[truncado]")
                } else msg
            }

            val response = try {
                llmProvider.generateResponse(messagesFiltrados, toolRegistry.getAllTools())
            } catch (e: Exception) {
                val errMsg = Message(
                    role = "assistant",
                    content = "Error al comunicarse con el LLM: ${e.localizedMessage ?: e.message}"
                )
                messages.add(errMsg)
                _agentMessages.value = messages.toList()
                _state.value = AgentState.ERROR
                return
            }

            val assistantContent = response.content ?: ""
            val toolCalls = response.toolCalls

            if (toolCalls.isNullOrEmpty()) {
                fallosConsecutivos++
                if (fallosConsecutivos >= 3 && assistantContent.isBlank()) {
                    _state.value = AgentState.ERROR
                    val errorMsg = Message("assistant", "No pude procesar tu solicitud.")
                    messages.add(errorMsg)
                    _agentMessages.value = messages.toList()
                    return
                }
            } else {
                fallosConsecutivos = 0
            }

            if (assistantContent.isNotBlank()) {
                lastAssistantResponse = assistantContent
            }

            val assistMsg = Message(
                role = "assistant",
                content = assistantContent,
                toolCalls = toolCalls
            )
            messages.add(assistMsg)
            _agentMessages.value = messages.toList()

            if (!toolCalls.isNullOrEmpty()) {
                _state.value = AgentState.WORKING
                for (toolCall in toolCalls) {
                    coroutineContext.ensureActive()

                    val bannerMsg = Message(
                        role = "assistant",
                        content = "Ejecutando: ${toolCall.name} con parámetros: ${toolCall.arguments}"
                    )
                    messages.add(bannerMsg)
                    _agentMessages.value = messages.toList()

                    val result = toolRegistry.executeTool(toolCall.name, toolCall.arguments)

                    val toolResultMsg = Message(
                        role = "tool",
                        content = result,
                        toolCallId = toolCall.id
                    )
                    messages.add(toolResultMsg)
                    _agentMessages.value = messages.toList()
                }
            } else {
                completed = true
            }
        }

        if (_state.value != AgentState.ERROR) {
            _state.value = AgentState.IDLE
            val finalSpeech = lastAssistantResponse?.takeIf { it.isNotBlank() } ?: "No pude completar la tarea."
            RoboPalApplication.ttsManager.speak(finalSpeech)
        }
    }
}
