package com.robopal.app.managers

import android.util.Log
import com.robopal.app.RoboPalApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

enum class DownloadStatus {
    IDLE,
    QUEUED,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED,
    CORRUPTED
}

data class ModelDownloadState(
    val fileName: String,
    val status: DownloadStatus = DownloadStatus.IDLE,
    val bytesDownloaded: Long = 0L,
    val totalBytes: Long = 0L,
    val percentage: Int = 0,
    val errorMessage: String? = null
)

open class DownloadManager {

    companion object {
        private const val TAG = "DownloadManager"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val _downloadStates = MutableStateFlow<Map<String, ModelDownloadState>>(emptyMap())
    val downloadStates: StateFlow<Map<String, ModelDownloadState>> = _downloadStates.asStateFlow()

    private val activeCalls = ConcurrentHashMap<String, Call>()

    open val downloadsDirectory: File
        get() = try {
            RoboPalApplication.llmManager.modelDirectory
        } catch (e: Exception) {
            File(System.getProperty("java.io.tmpdir") ?: ".", "models").apply { mkdirs() }
        }

    fun getDownloadState(fileName: String): ModelDownloadState {
        val current = _downloadStates.value[fileName]
        if (current != null) return current

        val targetFile = File(downloadsDirectory, fileName)
        val partFile = File(downloadsDirectory, "$fileName.part")

        return when {
            targetFile.exists() && targetFile.length() > 0 -> ModelDownloadState(
                fileName = fileName,
                status = DownloadStatus.COMPLETED,
                bytesDownloaded = targetFile.length(),
                totalBytes = targetFile.length(),
                percentage = 100
            )
            partFile.exists() && partFile.length() > 0 -> ModelDownloadState(
                fileName = fileName,
                status = DownloadStatus.PAUSED,
                bytesDownloaded = partFile.length()
            )
            else -> ModelDownloadState(fileName = fileName, status = DownloadStatus.IDLE)
        }
    }

    fun cancelDownload(fileName: String) {
        val call = activeCalls.remove(fileName)
        call?.cancel()

        val partFile = File(downloadsDirectory, "$fileName.part")
        if (partFile.exists()) {
            partFile.delete()
        }

        updateState(
            fileName,
            ModelDownloadState(
                fileName = fileName,
                status = DownloadStatus.CANCELLED,
                errorMessage = "Descarga cancelada por el usuario."
            )
        )
    }

    suspend fun downloadFile(
        url: String,
        destinationFileName: String,
        onProgress: ((bytesDownloaded: Long, totalBytes: Long, percentage: Int) -> Unit)? = null
    ): String = withContext(Dispatchers.IO) {
        val currentState = _downloadStates.value[destinationFileName]
        if (currentState?.status == DownloadStatus.DOWNLOADING) {
            return@withContext "Error: La descarga de $destinationFileName ya está en curso."
        }

        val partFile = File(downloadsDirectory, "$destinationFileName.part")
        val targetFile = File(downloadsDirectory, destinationFileName)

        updateState(
            destinationFileName,
            ModelDownloadState(
                fileName = destinationFileName,
                status = DownloadStatus.DOWNLOADING,
                bytesDownloaded = 0,
                totalBytes = 0,
                percentage = 0
            )
        )

        try {
            val request = Request.Builder().url(url).build()
            val call = client.newCall(request)
            activeCalls[destinationFileName] = call

            val response = call.execute()

            if (!response.isSuccessful) {
                val errorMsg = "HTTP error ${response.code}: ${response.message}"
                if (partFile.exists()) partFile.delete()
                updateState(
                    destinationFileName,
                    ModelDownloadState(
                        fileName = destinationFileName,
                        status = DownloadStatus.FAILED,
                        errorMessage = errorMsg
                    )
                )
                return@withContext errorMsg
            }

            val body = response.body ?: run {
                if (partFile.exists()) partFile.delete()
                val errorMsg = "Error: Respuesta vacía del servidor."
                updateState(
                    destinationFileName,
                    ModelDownloadState(
                        fileName = destinationFileName,
                        status = DownloadStatus.FAILED,
                        errorMessage = errorMsg
                    )
                )
                return@withContext errorMsg
            }

            val totalBytes = body.contentLength()
            partFile.parentFile?.mkdirs()

            var bytesDownloaded = 0L
            val buffer = ByteArray(16384)

            body.byteStream().use { input ->
                FileOutputStream(partFile).use { output ->
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        bytesDownloaded += bytesRead
                        val percentage = if (totalBytes > 0) ((bytesDownloaded * 100) / totalBytes).toInt() else -1

                        val progressState = ModelDownloadState(
                            fileName = destinationFileName,
                            status = DownloadStatus.DOWNLOADING,
                            bytesDownloaded = bytesDownloaded,
                            totalBytes = totalBytes,
                            percentage = if (percentage in 0..100) percentage else 0
                        )
                        updateState(destinationFileName, progressState)
                        onProgress?.invoke(bytesDownloaded, totalBytes, percentage)
                    }
                }
            }

            if (!partFile.exists() || partFile.length() <= 0) {
                if (partFile.exists()) partFile.delete()
                val errorMsg = "Error: Archivo descargado está vacío o corrupto."
                updateState(
                    destinationFileName,
                    ModelDownloadState(
                        fileName = destinationFileName,
                        status = DownloadStatus.CORRUPTED,
                        errorMessage = errorMsg
                    )
                )
                return@withContext errorMsg
            }

            val renamed = partFile.renameTo(targetFile)
            if (!renamed) {
                partFile.copyTo(targetFile, overwrite = true)
                partFile.delete()
            }

            updateState(
                destinationFileName,
                ModelDownloadState(
                    fileName = destinationFileName,
                    status = DownloadStatus.COMPLETED,
                    bytesDownloaded = targetFile.length(),
                    totalBytes = targetFile.length(),
                    percentage = 100
                )
            )

            "Archivo descargado y validado con éxito en ${targetFile.absolutePath}"
        } catch (e: IOException) {
            if (partFile.exists()) partFile.delete()
            val isCancelled = activeCalls[destinationFileName] == null
            val errorMsg = if (isCancelled) "Descarga cancelada." else "Error de red/timeout: ${e.localizedMessage ?: e.message}"
            val finalStatus = if (isCancelled) DownloadStatus.CANCELLED else DownloadStatus.FAILED

            updateState(
                destinationFileName,
                ModelDownloadState(
                    fileName = destinationFileName,
                    status = finalStatus,
                    errorMessage = errorMsg
                )
            )
            errorMsg
        } catch (e: Exception) {
            if (partFile.exists()) partFile.delete()
            val errorMsg = "Error inseperado en descarga: ${e.localizedMessage ?: e.message}"
            updateState(
                destinationFileName,
                ModelDownloadState(
                    fileName = destinationFileName,
                    status = DownloadStatus.FAILED,
                    errorMessage = errorMsg
                )
            )
            errorMsg
        } finally {
            activeCalls.remove(destinationFileName)
        }
    }

    private fun updateState(fileName: String, state: ModelDownloadState) {
        val currentMap = _downloadStates.value.toMutableMap()
        currentMap[fileName] = state
        _downloadStates.value = currentMap
    }
}
