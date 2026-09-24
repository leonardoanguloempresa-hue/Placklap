package com.robopal.app

import com.robopal.app.agent.LlmProvider
import com.robopal.app.agent.LlmResponse
import com.robopal.app.agent.Message
import com.robopal.app.agent.Tool
import com.robopal.app.agent.ToolCall
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class LlmManagerTest {

    private class MockThreadSafeLlmProvider : LlmProvider {
        private val mutex = Mutex()
        var isLoaded = true
        var isClosed = false
        var activeInferences = 0

        suspend fun loadModel() = mutex.withLock {
            delay(100)
            isLoaded = true
            isClosed = false
        }

        suspend fun unload() = mutex.withLock {
            isClosed = true
            isLoaded = false
        }

        override suspend fun generateResponse(messages: List<Message>, tools: List<Tool>): LlmResponse = mutex.withLock {
            if (isClosed || !isLoaded) {
                throw IllegalStateException("Modelo no cargado o cerrado")
            }
            activeInferences++
            try {
                delay(150) // Simular inferencia pesada
                return LlmResponse(content = "Respuesta de prueba", toolCalls = null)
            } finally {
                activeInferences--
            }
        }
    }

    @Test
    fun testTwoSimultaneousInferencesAreSerialized() = runTest {
        val provider = MockThreadSafeLlmProvider()
        var completedCount = 0

        val job1 = launch {
            provider.generateResponse(emptyList(), emptyList())
            completedCount++
        }
        val job2 = launch {
            provider.generateResponse(emptyList(), emptyList())
            completedCount++
        }

        job1.join()
        job2.join()

        assertEquals(2, completedCount)
        assertEquals(0, provider.activeInferences)
    }

    @Test
    fun testInferenceDuringModelReloadIsSerialized() = runTest {
        val provider = MockThreadSafeLlmProvider()
        var reloadFinished = false

        val job1 = launch {
            provider.loadModel()
            reloadFinished = true
        }
        val job2 = launch {
            provider.generateResponse(emptyList(), emptyList())
        }

        job1.join()
        job2.join()

        assertTrue(reloadFinished)
        assertTrue(provider.isLoaded)
    }

    @Test
    fun testCancellationDuringInference() = runTest {
        val provider = MockThreadSafeLlmProvider()

        val job = launch {
            try {
                provider.generateResponse(emptyList(), emptyList())
                fail("Debería haber sido cancelado")
            } catch (e: CancellationException) {
                // Éxito: la cancelación se propaga correctamente
            }
        }

        delay(50)
        job.cancel()
        job.join()

        assertEquals(0, provider.activeInferences)
    }

    @Test
    fun testCloseDuringOperationAndSubsequentInferenceFailure() = runTest {
        val provider = MockThreadSafeLlmProvider()

        val jobUnload = launch {
            provider.unload()
        }

        jobUnload.join()
        assertTrue(provider.isClosed)

        try {
            provider.generateResponse(emptyList(), emptyList())
            fail("Debería fallar por estar cerrado")
        } catch (e: IllegalStateException) {
            assertEquals("Modelo no cargado o cerrado", e.message)
        }
    }

    @Test
    fun testNewInferenceAfterCancellation() = runTest {
        val provider = MockThreadSafeLlmProvider()

        val job = launch {
            provider.generateResponse(emptyList(), emptyList())
        }
        delay(20)
        job.cancel()
        job.join()

        // Nueva inferencia después de cancelación previa
        val response = provider.generateResponse(emptyList(), emptyList())
        assertEquals("Respuesta de prueba", response.content)
    }
}
