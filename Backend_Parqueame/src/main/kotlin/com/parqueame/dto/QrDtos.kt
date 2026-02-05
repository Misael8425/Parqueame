package com.parqueame.dto

import kotlinx.serialization.Serializable

@Serializable
data class QrCreateRequest(
    val reservationId: String,
    val parkingId: String,
    val userId: String,
    val ttlMinutes: Int = 30          // tiempo de vida del token
)

@Serializable
data class QrCreateResponse(
    val token: String,
    val url: String                   // URL pública para el QR
)

@Serializable
data class QrStatusResponse(
    val token: String,
    val validated: Boolean,
    val message: String,
    val reservationId: String? = null
)

@Serializable
data class QrValidateResponse(
    val valid: Boolean,
    val message: String
)
