package com.parqueame.dto

import com.parqueame.models.Reservation
import com.parqueame.models.ReservationStatus
import com.parqueame.models.VehicleType
import kotlinx.serialization.Serializable

/* ============================================================
 *  Request: admite 24h (nuevo) y 12h (legacy) en el mismo DTO
 *  - Si llegan los 24h, se usan esos.
 *  - Si no, se intenta con los 12h.
 *  - Si faltan ambos, el servicio debe lanzar error.
 * ============================================================ */
@Serializable
data class CreateReservationRequest(
    val parkingId: String,

    // ---- Nuevo: 24h (sin AM/PM) ----
    val startHour24: Int? = null,
    val startMin: Int? = null,
    val endHour24: Int? = null,
    val endMin: Int? = null,

    // ---- Legacy: 12h (con AM/PM) ----
    val startHour12: Int? = null,
    val startPeriod: String? = null, // "AM" | "PM"
    val endHour12: Int? = null,
    val endPeriod: String? = null,

    // ---- Otros ----
    val vehicleType: VehicleType? = null,
    val localDate: String? = null,     // "YYYY-MM-DD"
    val timezone: String? = null       // Ej: "America/Santo_Domingo"
)

/* ============================================================
 *  DTOs de respuesta
 * ============================================================ */
@Serializable
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
    val status: ReservationStatus,
    val createdAt: Long,
    val updatedAt: Long
)

/*  Disponibilidad: ya incluía el campo message opcional  */
@Serializable
data class AvailabilityResponse(
    val parkingId: String,
    val startEpochMin: Long,
    val endEpochMin: Long,
    val capacity: Int,
    val activeReservations: Int,
    val available: Boolean,
    val message: String? = null
)

/* ============================================================
 *  Mappers / helpers
 * ============================================================ */

fun Reservation.toDto() = ReservationDto(
    id = _id.toHexString(),
    parkingId = parkingId,
    userId = userId,
    vehicleType = vehicleType,
    startEpochMin = startEpochMin,
    endEpochMin = endEpochMin,
    hoursBilled = hoursBilled,
    pricePerHour = pricePerHour,
    totalAmount = totalAmount,
    status = status,
    createdAt = createdAt,
    updatedAt = updatedAt
)

/**
 * Normaliza el request a 24h.
 * - Si vienen campos 24h completos → usa esos.
 * - Si vienen 12h completos → convierte a 24h.
 * - Si faltan → IllegalArgumentException (para que el servicio responda 400/422).
 */
data class Normalized24h(val startHour: Int, val startMin: Int, val endHour: Int, val endMin: Int)

fun CreateReservationRequest.require24h(): Normalized24h {
    // ¿Llegaron 24h?
    if (startHour24 != null && startMin != null && endHour24 != null && endMin != null) {
        return Normalized24h(
            startHour = startHour24.coerceIn(0, 23),
            startMin  = startMin.coerceIn(0, 59),
            endHour   = endHour24.coerceIn(0, 23),
            endMin    = endMin.coerceIn(0, 59)
        )
    }
    // ¿Llegaron 12h?
    if (startHour12 != null && startMin != null && startPeriod != null &&
        endHour12 != null && endMin != null && endPeriod != null) {

        val sH = hour12To24(startHour12, startPeriod)
        val eH = hour12To24(endHour12, endPeriod)

        return Normalized24h(
            startHour = sH,
            startMin  = startMin.coerceIn(0, 59),
            endHour   = eH,
            endMin    = endMin.coerceIn(0, 59)
        )
    }
    throw IllegalArgumentException("Solicitud inválida: faltan campos de hora (24h o 12h).")
}

/* Conversión 12h → 24h con límites seguros */
private fun hour12To24(hour12: Int, period: String): Int {
    val base = (hour12 % 12).let { if (it < 0) 0 else it } // 0..11 (12 -> 0)
    val pm = period.equals("PM", ignoreCase = true)
    val h = if (pm) base + 12 else base
    return h.coerceIn(0, 23)
}