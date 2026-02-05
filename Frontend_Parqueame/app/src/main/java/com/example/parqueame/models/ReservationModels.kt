package com.example.parqueame.models

// ------------------------------------------------------------
// ENUM: Tipo de vehículo (por si el backend lo utiliza)
// ------------------------------------------------------------
enum class VehicleType {
    AUTOMOVIL,
    MOTOCICLETA,
    CAMIONETA
}

// ------------------------------------------------------------
// REQUEST: Crear reserva (formato 24h, sin AM/PM)
// ------------------------------------------------------------
data class CreateReservationRequest(
    val parkingId: String,
    val startHour24: Int,
    val startMin: Int,
    val endHour24: Int,
    val endMin: Int,
    val localDate: String? = null,   // "YYYY-MM-DD" (opcional)
    val timezone: String? = null     // Ej: "America/Santo_Domingo" (opcional)
)

// ------------------------------------------------------------
// DTO: Reserva devuelta por el backend
// (el backend responde en epoch MINUTOS para start/end)
// ------------------------------------------------------------
data class ReservationDto(
    val id: String,
    val parkingId: String,
    val userId: String,
    val vehicleType: VehicleType,
    val startEpochMin: Long,
    val endEpochMin: Long,
    val hoursBilled: Int,
    val pricePerHour: Int,
    val totalAmount: Int,
    val status: String,
    val createdAt: Long,
    val updatedAt: Long
)

// ------------------------------------------------------------
// RESPONSE: Disponibilidad del parqueo
// (el backend espera/retorna epoch en MINUTOS)
// ------------------------------------------------------------
data class ParkingAvailabilityResponse(
    val parkingId: String,
    val startEpochMin: Long,
    val endEpochMin: Long,
    val capacity: Int,
    val activeReservations: Int,
    val available: Boolean,
    val message: String? = null // Mensaje descriptivo opcional
)