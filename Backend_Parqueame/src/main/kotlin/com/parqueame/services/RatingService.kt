// services/RatingService.kt
package com.parqueame.services

import com.parqueame.DatabaseFactory
import com.parqueame.dto.RateParkingRequest
import com.parqueame.models.ParkingLot
import com.parqueame.models.Reservation   // importa tu modelo real
import org.bson.types.ObjectId
import org.litote.kmongo.*
import java.lang.IllegalStateException

object RatingService {

    /**
     * Una sola calificación por reserva.
     * - Si la reserva ya tiene ratingStars != null => lanza IllegalStateException.
     * - Si es válida, setea ratingStars y actualiza el resumen del parqueo (+1 al count, +stars al sum).
     */
    suspend fun rateParkingUsingReservationOnce(
        parkingIdHex: String,
        req: RateParkingRequest
    ): Triple<Int, Int, Double> {
        require(req.stars in 1..5) { "stars must be 1..5" }

        val parkingOid = ObjectId(parkingIdHex)
        val reservationOid = ObjectId(req.reservationId)

        // 1) Traer reserva y validar pertenencia + parking
        val res = DatabaseFactory.reservationsCollection.findOne(
            and(
                Reservation::_id eq reservationOid,
                Reservation::userId eq req.userId
                // opcional: Reservation::status eq "COMPLETED"
            )
        ) ?: error("Reservation not found for this user")

        // Asegurar que la reserva corresponde a este parking
        val resParkingIdHex = when (val pid = res.parkingId) {
            is String -> pid
            is ObjectId -> pid.toHexString()
            else -> res.parkingId.toString() // si el tipo difiere
        }
        if (resParkingIdHex != parkingIdHex) {
            error("Reservation does not belong to this parking")
        }

        // 2) Bloquear segundo intento: si ya tiene rating -> 409
        if (res.ratingStars != null) {
            throw IllegalStateException("Reservation already rated")
        }

        // 3) Guardar rating en la reserva (primer y único intento)
        DatabaseFactory.reservationsCollection.updateOne(
            Reservation::_id eq reservationOid,
            set(
                Reservation::ratingStars setTo req.stars,
                Reservation::ratedAt setTo System.currentTimeMillis()
            )
        )

        // 4) Actualizar resumen del parqueo (count +1, sum +stars, avg = sum/count)
        val current = DatabaseFactory.parkingsCollection.findOne(ParkingLot::_id eq parkingOid)
            ?: error("Parking not found")

        val newCount = current.ratingCount + 1
        val newSum = current.ratingSum + req.stars
        val newAvg = newSum.toDouble() / newCount

        DatabaseFactory.parkingsCollection.updateOne(
            ParkingLot::_id eq parkingOid,
            set(
                ParkingLot::ratingCount setTo newCount,
                ParkingLot::ratingSum setTo newSum,
                ParkingLot::avgRating setTo newAvg,
                ParkingLot::updatedAt setTo System.currentTimeMillis()
            )
        )

        return Triple(newCount, newSum, newAvg)
    }
}
