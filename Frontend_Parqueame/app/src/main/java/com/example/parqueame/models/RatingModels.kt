package com.example.parqueame.models

import kotlinx.serialization.Serializable

@Serializable
data class RateParkingRequest(
    val userId: String,
    val reservationId: String,
    val stars: Int
)

@Serializable
data class RateParkingResponse(
    val parkingId: String,
    val ratingCount: Int,
    val avgRating: Double
)

@Serializable
data class ApiResponseWithData<T>(
    val success: Boolean,
    val message: String,
    val data: T? = null
)
