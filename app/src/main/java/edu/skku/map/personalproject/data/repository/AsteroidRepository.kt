package edu.skku.map.personalproject.data.repository

import android.content.Context
import android.graphics.Bitmap
import edu.skku.map.personalproject.data.local.AppDatabase
import edu.skku.map.personalproject.data.local.WatchlistEntity
import edu.skku.map.personalproject.data.model.Asteroid
import edu.skku.map.personalproject.data.model.ApodResponse
import edu.skku.map.personalproject.data.model.FeedResponse
import edu.skku.map.personalproject.data.remote.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AsteroidRepository(context: Context) {

    private val apiService = ApiService()
    private val dao = AppDatabase.getInstance(context).asteroidDao()

    private fun todayString(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    suspend fun fetchFeed(date: String? = null): FeedResponse = withContext(Dispatchers.IO) {
        val d = date ?: todayString()
        apiService.fetchFeed(d, d)
    }

    suspend fun fetchApod(): ApodResponse = withContext(Dispatchers.IO) {
        apiService.fetchApod()
    }

    suspend fun loadApodBitmap(imageUrl: String): Bitmap? = withContext(Dispatchers.IO) {
        apiService.loadBitmap(imageUrl)
    }

    fun getWatchlist(): Flow<List<WatchlistEntity>> = dao.getAll()

    suspend fun isInWatchlist(id: String): Boolean = withContext(Dispatchers.IO) {
        dao.getById(id) != null
    }

    suspend fun addToWatchlist(asteroid: Asteroid) = withContext(Dispatchers.IO) {
        dao.insert(asteroid.toWatchlistEntity())
    }

    suspend fun removeFromWatchlist(id: String) = withContext(Dispatchers.IO) {
        dao.deleteById(id)
    }

    private fun Asteroid.toWatchlistEntity() = WatchlistEntity(
        id = id,
        name = name,
        estimatedDiameterMinKm = estimatedDiameterMinKm,
        estimatedDiameterMaxKm = estimatedDiameterMaxKm,
        isPotentiallyHazardous = isPotentiallyHazardous,
        closeApproachDate = closeApproachDate,
        relativeVelocityKmh = relativeVelocityKmh,
        missDistanceKm = missDistanceKm,
        orbitingBody = orbitingBody
    )
}
