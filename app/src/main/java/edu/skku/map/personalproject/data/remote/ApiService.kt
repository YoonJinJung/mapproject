package edu.skku.map.personalproject.data.remote

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.gson.Gson
import edu.skku.map.personalproject.data.model.ApodResponse
import edu.skku.map.personalproject.data.model.FeedResponse
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

class ApiService {

    private val gson = Gson()

    val client = OkHttpClient.Builder()
        .connectTimeout(NetworkConfig.TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(NetworkConfig.TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(NetworkConfig.TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    @Throws(IOException::class)
    fun fetchFeed(startDate: String, endDate: String): FeedResponse {
        val url = "${NetworkConfig.BASE_URL}${NetworkConfig.ENDPOINT_FEED}" +
                "?start_date=$startDate&end_date=$endDate"
        return getJson(url, FeedResponse::class.java)
    }

    @Throws(IOException::class)
    fun fetchApod(): ApodResponse {
        val url = "${NetworkConfig.BASE_URL}${NetworkConfig.ENDPOINT_FEED}?type=apod"
        return getJson(url, ApodResponse::class.java)
    }

    fun loadBitmap(imageUrl: String): Bitmap? {
        return try {
            val request = Request.Builder().url(imageUrl).get().build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val bytes = response.body?.bytes() ?: return null
                val options = BitmapFactory.Options().apply { inSampleSize = 2 }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            }
        } catch (e: Exception) { null }
    }

    private fun <T> getJson(url: String, clazz: Class<T>): T {
        val request = Request.Builder().url(url).get().build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful)
                throw IOException("Server error: ${response.code} ${response.message}")
            val body = response.body?.string()
                ?: throw IOException("Empty response body")
            return gson.fromJson(body, clazz)
                ?: throw IOException("Failed to parse response")
        }
    }
}
