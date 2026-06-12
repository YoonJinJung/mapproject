package edu.skku.map.personalproject.data.model

import com.google.gson.annotations.SerializedName

data class ApodResponse(
    val date: String = "",
    val title: String = "",
    val explanation: String = "",
    val url: String = "",
    val hdurl: String? = null,
    @SerializedName("mediaType") val mediaType: String = "image",
    val copyright: String? = null
)
