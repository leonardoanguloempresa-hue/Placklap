package com.robopal.app

import com.robopal.app.managers.DownloadManager
import com.robopal.app.managers.DownloadStatus
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class DownloadManagerTest {

    private lateinit var downloadManager: DownloadManager
    private lateinit var testDir: File

    @Before
    fun setUp() {
        testDir = File(System.getProperty("java.io.tmpdir") ?: ".", "test_models_${System.currentTimeMillis()}").apply { mkdirs() }
        downloadManager = object : DownloadManager() {
            override val downloadsDirectory: File
                get() = testDir
        }
    }

    @Test
    fun testAtomicPartRenameAndValidation() = runTest {
        val fileName = "qwen2.5_test.task"
        val partFile = File(testDir, "$fileName.part")
        val targetFile = File(testDir, fileName)

        assertFalse(partFile.exists())
        assertFalse(targetFile.exists())

        partFile.writeText("Dummy model binary content for MediaPipe .task")
        assertTrue(partFile.exists())
        assertTrue(partFile.length() > 0)

        val renamed = partFile.renameTo(targetFile)
        assertTrue(renamed)
        assertFalse(partFile.exists())
        assertTrue(targetFile.exists())
        assertEquals("Dummy model binary content for MediaPipe .task", targetFile.readText())
    }

    @Test
    fun testCancellationCleansPartFileAndResetsState() = runTest {
        val fileName = "gemma_test.task"
        val partFile = File(testDir, "$fileName.part")
        partFile.writeText("partial data")
        assertTrue(partFile.exists())

        downloadManager.cancelDownload(fileName)

        assertFalse(partFile.exists())
        val state = downloadManager.getDownloadState(fileName)
        assertEquals(DownloadStatus.CANCELLED, state.status)
    }

    @Test
    fun testInitialStateForExistingCompletedFile() = runTest {
        val fileName = "installed_model.task"
        val targetFile = File(testDir, fileName)
        targetFile.writeText("completed binary")

        val state = downloadManager.getDownloadState(fileName)

        assertEquals(DownloadStatus.COMPLETED, state.status)
        assertEquals(100, state.percentage)
    }
}
