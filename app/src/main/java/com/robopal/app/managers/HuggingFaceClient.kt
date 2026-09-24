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

        // Lista seleccionada de modelos .task en HuggingFace
        val CURATED_MODELS = listOf(
            HuggingFaceModel(
                name = "Qwen2.5-3B-Instruct",
                repoId = "litert-community/Qwen2.5-3B-Instruct",
                taskFileName = "qwen2.5_3b_instruct.task",
                downloadUrl = "https://huggingface.co/litert-community/Qwen2.5-3B-Instruct/resolve/main/qwen2.5_3b_instruct.task",
                sizeBytes = 2100000000L,
                requiresLicense = false
            ),
            HuggingFaceModel(
                name = "Gemma-2-2B-IT",
                repoId = "litert-community/Gemma2-2B-IT",
                taskFileName = "gemma2_2b_it.task",
                downloadUrl = "https://huggingface.co/litert-community/Gemma2-2B-IT/resolve/main/gemma2_2b_it.task",
                sizeBytes = 1800000000L,
                requiresLicense = true,
                licenseUrl = "https://huggingface.co/google/gemma-2-2b-it"
            ),
            HuggingFaceModel(
                name = "Phi-3.5-mini-instruct",
                repoId = "litert-community/Phi-3.5-mini-instruct",
                taskFileName = "phi3.5_mini_instruct.task",
                downloadUrl = "https://huggingface.co/litert-community/Phi-3.5-mini-instruct/resolve/main/phi3.5_mini_instruct.task",
                sizeBytes = 2300000000L,
                requiresLicense = false
            ),
            HuggingFaceModel(
                name = "Qwen2.5-1.5B-Instruct",
                repoId = "litert-community/Qwen2.5-1.5B-Instruct",
                taskFileName = "qwen2.5_1.5b_instruct.task",
                downloadUrl = "https://huggingface.co/litert-community/Qwen2.5-1.5B-Instruct/resolve/main/qwen2.5_1.5b_instruct.task",
                sizeBytes = 1100000000L,
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
