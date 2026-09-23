package com.robopal.app.managers

import android.os.Environment
import com.robopal.app.RoboPalApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

data class DownloadProgress(
    val fileName: String,
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val percentage: Int,
    val isCompleted: Boolean,
    val error: String? = null
)

class DownloadManager {

    private val client = OkHttpClient()

    private val _downloadProgress = MutableStateFlow<DownloadProgress?>(null)
    val downloadProgress: StateFlow<DownloadProgress?> = _downloadProgress.asStateFlow()

    val downloadsDirectory: File
        get() {
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "RoboPal"
            )
            if (!dir.exists()) {
                dir.mkdirs()
            }
            return dir
        }

    suspend fun downloadFile(
        url: String,
        destinationPath: String,
        onProgress: ((bytesDownloaded: Long, totalBytes: Long, percentage: Int) -> Unit)? = null
    ): String = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                val errorMsg = "Error en la descarga. Código HTTP: ${response.code}"
                _downloadProgress.value = DownloadProgress(
                    fileName = destinationPath.substringAfterLast("/"),
                    bytesDownloaded = 0,
                    totalBytes = 0,
                    percentage = 0,
                    isCompleted = false,
                    error = errorMsg
                )
                return@withContext errorMsg
            }

            val body = response.body ?: return@withContext "Error: Cuerpo de respuesta vacío."
            val totalBytes = body.contentLength()
            val fileName = destinationPath.substringAfterLast("/")

            val targetFile = if (destinationPath.startsWith("/")) {
                File(destinationPath)
            } else {
                File(downloadsDirectory, destinationPath)
            }

            targetFile.parentFile?.mkdirs()

            var bytesDownloaded = 0L
            val buffer = ByteArray(8192)

            body.byteStream().use { input ->
                FileOutputStream(targetFile).use { output ->
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        bytesDownloaded += bytesRead
                        val percentage = if (totalBytes > 0) ((bytesDownloaded * 100) / totalBytes).toInt() else 0

                        val progress = DownloadProgress(
                            fileName = fileName,
                            bytesDownloaded = bytesDownloaded,
                            totalBytes = totalBytes,
                            percentage = percentage,
                            isCompleted = false
                        )
                        _downloadProgress.value = progress
                        onProgress?.invoke(bytesDownloaded, totalBytes, percentage)
                    }
                }
            }

            _downloadProgress.value = DownloadProgress(
                fileName = fileName,
                bytesDownloaded = bytesDownloaded,
                totalBytes = totalBytes,
                percentage = 100,
                isCompleted = true
            )

            "Archivo descargado con éxito en ${targetFile.absolutePath}"
        } catch (e: Exception) {
            val errorMsg = "Error al descargar archivo: ${e.localizedMessage ?: e.message}"
            _downloadProgress.value = DownloadProgress(
                fileName = destinationPath.substringAfterLast("/"),
                bytesDownloaded = 0,
                totalBytes = 0,
                percentage = 0,
                isCompleted = false,
                error = errorMsg
            )
            errorMsg
        }
    }
}
