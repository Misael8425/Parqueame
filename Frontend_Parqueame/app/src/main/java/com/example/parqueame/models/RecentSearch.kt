package com.example.parqueame.models

import com.google.gson.annotations.SerializedName

data class RecentSearchRequest(
    val name: String,
    val address: String,
    val lat: Double,
    val lng: Double,
    val originLat: Double? = null,
    val originLng: Double? = null
)

data class RecentSearchResponse(
    val id: String,
    val name: String,
    val address: String,
    val lat: Double,
    val lng: Double,
    val distanceText: String?,
    val createdAt: Long
)