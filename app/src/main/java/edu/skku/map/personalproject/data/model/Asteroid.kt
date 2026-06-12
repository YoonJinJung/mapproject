package edu.skku.map.personalproject.data.model

// GSON이 JSON ↔ 객체 변환을 담당합니다 (@Parcelize 불필요)
data class Asteroid(
    val id: String = "",
    val name: String = "",
    val absoluteMagnitude: Double? = null,
    val estimatedDiameterMinKm: Double = 0.0,
    val estimatedDiameterMaxKm: Double = 0.0,
    val isPotentiallyHazardous: Boolean = false,
    val closeApproachDate: String = "",
    val relativeVelocityKmh: String = "0",
    val missDistanceKm: String = "0",
    val orbitingBody: String = "Earth",
    val nasaJplUrl: String? = null
)
