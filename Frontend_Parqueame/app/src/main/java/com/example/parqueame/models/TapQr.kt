// com/example/parqueame/models/TapQr.kt
package com.example.parqueame.models

data class TapQrRequest(
    val reservationId: String,
    val parkingId: String,
    val userId: String
)

data class TapQrResponse(
    val ok: Boolean,
    val reservationId: String?,
    val message: String?
)
