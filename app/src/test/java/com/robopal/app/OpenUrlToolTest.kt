package com.robopal.app

import com.robopal.app.agent.tools.OpenUrlTool
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class OpenUrlToolTest {

    private lateinit var tool: OpenUrlTool

    @Before
    fun setUp() {
        tool = OpenUrlTool()
    }

    @Test
    fun testValidHttpAndHttpsUrlsPassValidation() = runTest {
        val resHttp = tool.validate(mapOf("url" to "http://example.com"))
        val resHttps = tool.validate(mapOf("url" to "https://google.com/search?q=test"))

        assertNull(resHttp)
        assertNull(resHttps)
    }

    @Test
    fun testBlankAndMissingUrlFailsValidation() = runTest {
        val resEmpty = tool.validate(mapOf("url" to ""))
        val resMissing = tool.validate(emptyMap())

        assertNotNull(resEmpty)
        assertNotNull(resMissing)
        assertEquals("URL vacía no permitida.", resEmpty)
    }

    @Test
    fun testDangerousSchemesAreRejected() = runTest {
        val resFile = tool.validate(mapOf("url" to "file:///sdcard/secret.txt"))
        val resIntent = tool.validate(mapOf("url" to "intent://example#Intent;scheme=http;end"))
        val resJs = tool.validate(mapOf("url" to "javascript:alert(1)"))
        val resContent = tool.validate(mapOf("url" to "content://media/external/images"))

        assertNotNull(resFile)
        assertNotNull(resIntent)
        assertNotNull(resJs)
        assertNotNull(resContent)
    }
}
