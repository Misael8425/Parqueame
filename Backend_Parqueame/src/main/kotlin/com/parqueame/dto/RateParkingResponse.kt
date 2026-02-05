package com.parqueame.dto

import kotlinx.serialization.Serializable

@Serializable
data class RateParkingResponse(
    val parkingId: String,
    val ratingCount: Int,
    val avgRating: Double
)

