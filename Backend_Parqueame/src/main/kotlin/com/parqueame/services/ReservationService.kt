package com.parqueame.services

import com.parqueame.dto.*
import com.parqueame.models.Reservation
import com.parqueame.models.VehicleType
import com.parqueame.repositories.ParkingRepository
import com.parqueame.repositories.ReservationRepository
import com.parqueame.util.ReservationStatusUtil
import com.parqueame.util.ceilHours
import com.parqueame.util.toEpochMinutes24
import java.time.*

class ReservationService(
    private val reservations: ReservationRepository,
    private val parkings: ParkingRepository
) {
    // Fuente única de verdad para "estados activos"
    private val ACTIVE_STATUSES = ReservationStatusUtil.activeStatuses()

    // -------------------- CREATE --------------------
    fun create(userId: String, req: CreateReservationRequest): ReservationDto {
        val parking = parkings.getById(req.parkingId) ?: error("Parqueo no encontrado")
        if (parking.status != "approved") error("Parqueo no disponible: estado=${parking.status}")

        val tz: ZoneId = req.timezone?.let(ZoneId::of) ?: ZoneId.of("America/Santo_Domingo")
        val date: LocalDate = req.localDate?.let(LocalDate::parse) ?: LocalDate.now(tz)

        val norm = req.require24h()
        val sH = norm.startHour
        val sM = norm.startMin
        val eH = norm.endHour
        val eM = norm.endMin

        val startMin: Long = toEpochMinutes24(sH, sM, date, tz)

        val endIsNextDay = (eH * 60 + eM) <= (sH * 60 + sM)
        val endDate = if (endIsNextDay) date.plusDays(1) else date

        val endMin: Long = toEpochMinutes24(eH, eM, endDate, tz)

        // Validación de horario operativo (soporta franjas nocturnas)
        if (!parkings.isOpenInRange(parking, startMin, endMin, tz)) {
            error("Fuera del horario disponible para ese día")
        }

        // Disponibilidad: solapes activos < capacidad
        val overlaps = reservations.countOverlaps(parking._id.toHexString(), startMin, endMin)
        if (overlaps >= parking.capacity) error("No hay disponibilidad en ese rango")

        // Facturación
        val minutes = endMin - startMin
        val hours   = ceilHours(minutes)
        val price   = parking.priceHour
        val total   = hours * price

        val inserted = reservations.insert(
            Reservation(
                parkingId     = parking._id.toHexString(),
                userId        = userId,
                vehicleType   = (req.vehicleType ?: VehicleType.AUTOMOVIL),
                startEpochMin = startMin,
                endEpochMin   = endMin,
                hoursBilled   = hours,
                pricePerHour  = price,
                totalAmount   = total
            )
        )
        return inserted.toDto()
    }

    // -------------------- AVAILABILITY --------------------
    fun availability(parkingId: String, startMin: Long, endMin: Long): AvailabilityResponse {
        require(startMin > 0) { "startMin debe ser positivo" }
        require(endMin > 0) { "endMin debe ser positivo" }
        require(endMin > startMin) { "endMin debe ser mayor que startMin" }

        val maxMinutes = 7 * 24 * 60L
        require((endMin - startMin) <= maxMinutes) { "El rango no puede exceder 7 días" }

        val parking = parkings.getById(parkingId)
            ?: throw IllegalArgumentException("Parqueo no encontrado: $parkingId")

        if (parking.status != "approved") {
            return AvailabilityResponse(
                parkingId = parkingId,
                startEpochMin = startMin,
                endEpochMin = endMin,
                capacity = parking.capacity,
                activeReservations = 0,
                available = false,
                message = "Parqueo no disponible: estado=${parking.status}"
            )
        }

        val overlaps = reservations.countOverlaps(parkingId, startMin, endMin)
        val available = overlaps < parking.capacity

        return AvailabilityResponse(
            parkingId = parkingId,
            startEpochMin = startMin,
            endEpochMin = endMin,
            capacity = parking.capacity,
            activeReservations = overlaps,
            available = available,
            message = if (!available) "Capacidad completa" else null
        )
    }

    // -------------------- CRUD & LIST --------------------
    fun get(id: String): ReservationDto =
        reservations.findById(id)?.toDto() ?: error("Reserva no encontrada")

    fun listByUser(userId: String): List<ReservationDto> =
        reservations.listByUser(userId).map { it.toDto() }

    fun listByParking(parkingId: String): List<ReservationDto> =
        reservations.listByParking(parkingId).map { it.toDto() }

    fun cancel(id: String): Boolean = reservations.cancel(id)

    // -------------------- NUEVO: ACTIVAS --------------------
    fun listActiveByUser(userId: String): List<ReservationDto> =
        reservations.listByUser(userId)
            .asSequence()
            .filter { ReservationStatusUtil.isActive(it.status) }
            .map { it.toDto() }
            .toList()

    fun listActiveByParking(parkingId: String): List<ReservationDto> =
        reservations.listByParking(parkingId)
            .asSequence()
            .filter { ReservationStatusUtil.isActive(it.status) }
            .map { it.toDto() }
            .toList()

    fun isActiveReservation(reservationId: String): Boolean =
        ReservationStatusUtil.isActive(reservations.findById(reservationId)?.status)
}