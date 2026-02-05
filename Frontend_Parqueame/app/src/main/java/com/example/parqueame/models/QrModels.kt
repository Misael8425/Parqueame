package com.example.parqueame.models

data class QrCreateRequest(
    val reservationId: String,
    val parkingId: String,
    val userId: String,
    val ttlMinutes: Int = 30
)

data class QrCreateResponse(
    val token: String,
    val url: String
)

data class QrStatusResponse(
    val token: String,
    val validated: Boolean,
    val message: String,
    val reservationId: String? = null
)
