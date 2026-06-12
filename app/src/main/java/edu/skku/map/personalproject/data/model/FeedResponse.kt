package edu.skku.map.personalproject.data.model

data class AsteroidSummary(
    val totalCount: Int = 0,
    val hazardousCount: Int = 0,
    val startDate: String = "",
    val endDate: String = ""
)

data class FeedResponse(
    val fetchDate: String = "",
    val summary: AsteroidSummary = AsteroidSummary(),
    val asteroids: List<Asteroid> = emptyList()
)
