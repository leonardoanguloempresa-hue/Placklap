package com.robopal.app.providers

import com.robopal.app.agent.LlmProvider
import com.robopal.app.agent.LlmResponse
import com.robopal.app.agent.Message
import com.robopal.app.agent.Tool

enum class ProviderType {
    LOCAL_MEDIAPIPE,
    REMOTE_OPENAI,
    REMOTE_ANTHROPIC
}

interface AiProvider : LlmProvider {
    val providerType: ProviderType
    val isAvailable: Boolean
    suspend fun initialize(): Boolean
}

class RemoteOpenAiProvider(private val apiKey: String) : AiProvider {
    override val providerType: ProviderType = ProviderType.REMOTE_OPENAI
    override val isAvailable: Boolean = apiKey.isNotBlank()

    override suspend fun initialize(): Boolean = isAvailable

    override suspend fun generateResponse(messages: List<Message>, tools: List<Tool>): LlmResponse {
        return LlmResponse(
            content = "Proveedor remoto OpenAI configurado. Inferencia vía API segura.",
            toolCalls = null
        )
    }
}
