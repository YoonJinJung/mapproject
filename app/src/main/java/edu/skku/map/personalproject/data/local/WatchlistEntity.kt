package edu.skku.map.personalproject.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watchlist")
data class WatchlistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val estimatedDiameterMinKm: Double,
    val estimatedDiameterMaxKm: Double,
    val isPotentiallyHazardous: Boolean,
    val closeApproachDate: String,
    val relativeVelocityKmh: String,
    val missDistanceKm: String,
    val orbitingBody: String,
    val addedAt: Long = System.currentTimeMillis()
)
