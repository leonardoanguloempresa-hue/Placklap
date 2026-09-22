package com.robopal.app.managers

import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

class DownloadManager {

    private val client = OkHttpClient()

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

    suspend fun downloadFile(url: String, destinationPath: String): String =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    return@withContext "Error en la descarga. Código HTTP: ${response.code}"
                }

                val body = response.body ?: return@withContext "Error: Cuerpo de respuesta vacío."
                val targetFile = if (destinationPath.startsWith("/")) {
                    File(destinationPath)
                } else {
                    File(downloadsDirectory, destinationPath)
                }

                targetFile.parentFile?.mkdirs()

                body.byteStream().use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                }

                "Archivo descargado con éxito en ${targetFile.absolutePath}"
            } catch (e: Exception) {
                "Error al descargar archivo: ${e.localizedMessage ?: e.message}"
            }
        }
}
