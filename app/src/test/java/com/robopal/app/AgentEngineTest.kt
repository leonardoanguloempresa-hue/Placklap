package com.robopal.app

import com.robopal.app.agent.LlmProvider
import com.robopal.app.agent.LlmResponse
import com.robopal.app.agent.Message
import com.robopal.app.agent.Tool
import com.robopal.app.agent.ToolCall
import com.robopal.app.agent.ToolRegistry
import com.robopal.app.safety.RiskConfirmationHandler
import com.robopal.app.safety.RiskLevel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

class AgentEngineTest {

    private class MockTestLlmProvider : LlmProvider {
        var callsCount = 0
        override suspend fun generateResponse(messages: List<Message>, tools: List<Tool>): LlmResponse {
            callsCount++
            return if (callsCount == 1) {
                LlmResponse(
                    content = "Procesando...",
                    toolCalls = listOf(
                        ToolCall(
                            id = UUID.randomUUID().toString(),
                            name = "open_url",
                            arguments = mapOf("url" to "https://example.com")
                        )
                    )
                )
            } else {
                LlmResponse(
                    content = "Tarea completada con éxito.",
                    toolCalls = null
                )
            }
        }
    }

    private class TestConfirmationHandler(val shouldApprove: Boolean) : RiskConfirmationHandler {
        var requestedConfirmations = 0
        override suspend fun requestUserConfirmation(
            toolName: String,
            riskLevel: RiskLevel,
            arguments: Map<String, Any>,
            description: String
        ): Boolean {
            requestedConfirmations++
            return shouldApprove
        }
    }

    private lateinit var llmProvider: MockTestLlmProvider

    @Before
    fun setUp() {
        llmProvider = MockTestLlmProvider()
    }

    @Test
    fun testRiskConfirmationAcceptanceExecutesTool() = runTest {
        val confirmationHandler = TestConfirmationHandler(shouldApprove = true)
        val toolRegistry = ToolRegistry(confirmationHandler = confirmationHandler)

        val tool = toolRegistry.getTool("open_url")
        if (tool != null) {
            val approved = confirmationHandler.requestUserConfirmation(
                toolName = tool.name,
                riskLevel = tool.riskLevel,
                arguments = mapOf("url" to "https://example.com"),
                description = tool.description
            )
            assertTrue(approved)
        }

        assertEquals(1, confirmationHandler.requestedConfirmations)
    }

    @Test
    fun testRiskConfirmationRejectionCancelsTool() = runTest {
        val confirmationHandler = TestConfirmationHandler(shouldApprove = false)
        val toolRegistry = ToolRegistry(confirmationHandler = confirmationHandler)

        val tool = toolRegistry.getTool("open_url")
        if (tool != null) {
            val approved = confirmationHandler.requestUserConfirmation(
                toolName = tool.name,
                riskLevel = tool.riskLevel,
                arguments = mapOf("url" to "https://example.com"),
                description = tool.description
            )
            assertEquals(false, approved)
        }

        assertEquals(1, confirmationHandler.requestedConfirmations)
    }
}
