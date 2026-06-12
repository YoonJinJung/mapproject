package edu.skku.map.personalproject.data.remote

import com.google.gson.Gson
import edu.skku.map.personalproject.data.model.FeedResponse
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

class ApiService {

    private val gson = Gson()

    private val client = OkHttpClient.Builder()
        .connectTimeout(NetworkConfig.TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(NetworkConfig.TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(NetworkConfig.TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    @Throws(IOException::class)
    fun fetchFeed(startDate: String, endDate: String): FeedResponse {
        val url = "${NetworkConfig.BASE_URL}${NetworkConfig.ENDPOINT_FEED}" +
                "?start_date=$startDate&end_date=$endDate"

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Server error: ${response.code} ${response.message}")
            }
            val body = response.body?.string()
                ?: throw IOException("Empty response body from server")

            return gson.fromJson(body, FeedResponse::class.java)
                ?: throw IOException("Failed to parse server response")
        }
    }
}
