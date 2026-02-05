// com/example/parqueame/models/RatingSummaryDto.kt
package com.example.parqueame.models

data class RatingSummaryDto(
    val parkingId: String,
    val average: Double?,   // null si aún no hay ratings
    val count: Int
)
