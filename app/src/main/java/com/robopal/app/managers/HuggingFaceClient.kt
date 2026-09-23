package com.robopal.app.managers

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

data class HuggingFaceModel(
    val id: String,
    val downloads: Int = 0,
    val likes: Int = 0
)

class HuggingFaceClient {

    private val client = OkHttpClient()

    suspend fun searchModels(query: String): List<HuggingFaceModel> = withContext(Dispatchers.IO) {
        val url = "https://huggingface.co/api/models?search=${UriEncode(query)}&filter=gguf&limit=30"
        val request = Request.Builder().url(url).build()

        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext emptyList()

            val responseBody = response.body?.string() ?: return@withContext emptyList()
            val jsonArray = JSONArray(responseBody)
            val results = mutableListOf<HuggingFaceModel>()

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val id = obj.optString("id", "")
                val downloads = obj.optInt("downloads", 0)
                val likes = obj.optInt("likes", 0)
                if (id.isNotBlank()) {
                    results.add(HuggingFaceModel(id = id, downloads = downloads, likes = likes))
                }
            }
            results
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getGgufDownloadUrl(repoId: String): String? = withContext(Dispatchers.IO) {
        val url = "https://huggingface.co/api/models/$repoId/tree/main"
        val request = Request.Builder().url(url).build()

        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null

            val responseBody = response.body?.string() ?: return@withContext null
            val jsonArray = JSONArray(responseBody)

            var ggufFileName: String? = null
            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)
                val path = item.optString("path", "")
                if (path.endsWith(".gguf", ignoreCase = true)) {
                    // Preferir modelos cuantizados ligeros si hay múltiples
                    if (path.contains("q4_k_m", ignoreCase = true) || ggufFileName == null) {
                        ggufFileName = path
                    }
                }
            }

            if (ggufFileName != null) {
                "https://huggingface.co/$repoId/resolve/main/$ggufFileName"
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
