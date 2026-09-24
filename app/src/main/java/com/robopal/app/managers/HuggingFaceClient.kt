package com.robopal.app.managers

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray

data class HuggingFaceModel(
    val id: String,
    val downloads: Int = 0,
    val likes: Int = 0,
    val directTaskUrl: String? = null
)

class HuggingFaceClient {

    private val client = OkHttpClient()

    companion object {
        val LITERTI_RECOMMENDED_TASKS = listOf(
            HuggingFaceModel(
                id = "litert-community/Qwen2.5-1.5B-Instruct",
                downloads = 15200,
                likes = 890,
                directTaskUrl = "https://huggingface.co/litert-community/Qwen2.5-1.5B-Instruct/resolve/main/Qwen2.5-1.5B-Instruct_multi-prefill-seq_q8_ekv1280.task"
            ),
            HuggingFaceModel(
                id = "litert-community/Gemma-2-2B-IT",
                downloads = 8900,
                likes = 620,
                directTaskUrl = "https://huggingface.co/litert-community/Gemma-2-2B-IT/resolve/main/gemma2-2b-it-gpu-int4.task"
            ),
            HuggingFaceModel(
                id = "litert-community/Phi-3.5-mini-instruct",
                downloads = 11400,
                likes = 750,
                directTaskUrl = "https://huggingface.co/litert-community/Phi-3.5-mini-instruct/resolve/main/phi-3.5-mini-instruct-gpu-int4.task"
            )
        )
    }

    suspend fun searchModels(query: String): List<HuggingFaceModel> = withContext(Dispatchers.IO) {
        if (query.isBlank() || query.contains("qwen", ignoreCase = true) || query.contains("litert", ignoreCase = true)) {
            return@withContext LITERTI_RECOMMENDED_TASKS
        }

        val url = "https://huggingface.co/api/models?search=${UriEncode(query)}&limit=30"
        val request = Request.Builder().url(url).build()

        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext LITERTI_RECOMMENDED_TASKS

            val responseBody = response.body?.string() ?: return@withContext LITERTI_RECOMMENDED_TASKS
            val jsonArray = JSONArray(responseBody)
            val results = mutableListOf<HuggingFaceModel>()

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val id = obj.optString("id", "")
                val downloads = obj.optInt("downloads", 0)
                val likes = obj.optInt("likes", 0)
                if (id.isNotBlank() && (id.contains("litert", ignoreCase = true) || id.contains("task", ignoreCase = true))) {
                    results.add(HuggingFaceModel(id = id, downloads = downloads, likes = likes))
                }
            }
            if (results.isEmpty()) LITERTI_RECOMMENDED_TASKS else results
        } catch (e: Exception) {
            LITERTI_RECOMMENDED_TASKS
        }
    }

    suspend fun getTaskDownloadUrl(repoId: String): String? = withContext(Dispatchers.IO) {
        val recommended = LITERTI_RECOMMENDED_TASKS.firstOrNull { it.id.equals(repoId, ignoreCase = true) }
        if (recommended?.directTaskUrl != null) {
            return@withContext recommended.directTaskUrl
        }

        val url = "https://huggingface.co/api/models/$repoId/tree/main"
        val request = Request.Builder().url(url).build()

        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null

            val responseBody = response.body?.string() ?: return@withContext null
            val jsonArray = JSONArray(responseBody)

            var taskFileName: String? = null
            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)
                val path = item.optString("path", "")
                if (path.endsWith(".task", ignoreCase = true)) {
                    taskFileName = path
                    break
                }
            }

            if (taskFileName != null) {
                "https://huggingface.co/$repoId/resolve/main/$taskFileName"
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun UriEncode(text: String): String {
        return java.net.URLEncoder.encode(text, "UTF-8")
    }
}
