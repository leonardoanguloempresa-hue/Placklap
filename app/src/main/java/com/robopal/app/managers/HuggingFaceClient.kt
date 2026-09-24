package com.robopal.app.managers

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

data class HuggingFaceModel(
    val name: String,
    val repoId: String,
    val taskFileName: String,
    val downloadUrl: String,
    val sizeBytes: Long,
    val requiresLicense: Boolean = false,
    val licenseUrl: String? = null
)

class HuggingFaceClient {

    companion object {
        private const val TAG = "HuggingFaceClient"

        val CURATED_MODELS = listOf(
            HuggingFaceModel(
                name = "Qwen2.5-1.5B-Instruct (Recomendado)",
                repoId = "litert-community/Qwen2.5-1.5B-Instruct",
                taskFileName = "Qwen2.5-1.5B-Instruct_multi-prefill-seq_q8_ekv1280.task",
                downloadUrl = "https://huggingface.co/litert-community/Qwen2.5-1.5B-Instruct/resolve/main/Qwen2.5-1.5B-Instruct_multi-prefill-seq_q8_ekv1280.task",
                sizeBytes = 1597913616L,
                requiresLicense = false
            ),
            HuggingFaceModel(
                name = "Qwen2.5-1.5B-Instruct (Compacto)",
                repoId = "litert-community/Qwen2.5-1.5B-Instruct",
                taskFileName = "Qwen2.5-1.5B-Instruct_seq128_q8_ekv1280.task",
                downloadUrl = "https://huggingface.co/litert-community/Qwen2.5-1.5B-Instruct/resolve/main/Qwen2.5-1.5B-Instruct_seq128_q8_ekv1280.task",
                sizeBytes = 1567364648L,
                requiresLicense = false
            )
        )
    }

    suspend fun verifyModelUrl(model: HuggingFaceModel): Boolean {
        return try {
            val url = URL(model.downloadUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "HEAD"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            val responseCode = connection.responseCode
            connection.disconnect()
            responseCode in 200..308
        } catch (e: Exception) {
            Log.e(TAG, "Error verificando URL ${model.downloadUrl}: ${e.message}")
            false
        }
    }
}
