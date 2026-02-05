package com.parqueame.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateRecentSearchRequest(
    val name: String,
    val address: String,
    val lat: Double,
    val lng: Double,
    val originLat: Double? = null,
    val originLng: Double? = null
)

@Serializable
data class RecentSearchResponse(
    val id: String,
    val name: String,
    val address: String,
    val lat: Double,
    val lng: Double,
    val distanceText: String?,
    val createdAt: Long
)
