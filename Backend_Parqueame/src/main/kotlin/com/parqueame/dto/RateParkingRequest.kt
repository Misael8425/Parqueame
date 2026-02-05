package com.parqueame.dto

import kotlinx.serialization.Serializable

@Serializable
data class RateParkingRequest(
    val userId: String,
    val reservationId: String,
    val stars: Int,             // 1..5
)
