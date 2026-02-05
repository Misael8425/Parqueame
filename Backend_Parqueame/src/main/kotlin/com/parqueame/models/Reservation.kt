package com.parqueame.models

import kotlinx.serialization.Serializable
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId
import kotlinx.serialization.Contextual
import java.time.Instant

@Serializable
enum class ReservationStatus { PENDING, CONFIRMED, CANCELLED, COMPLETED }

@Serializable
enum class VehicleType { AUTOMOVIL, MOTOCICLETA, CAMIONETA }

@Serializable
data class Reservation(
    @BsonId @Contextual
    val _id: ObjectId = ObjectId(),

    val parkingId: String,
    val userId: String,
    val vehicleType: VehicleType,

    // Tiempos en epoch minutes (UTC)
    val startEpochMin: Long,
    val endEpochMin: Long,

    val hoursBilled: Int,
    val pricePerHour: Int,
    val totalAmount: Int, // DOP

    val ratingStars: Int? = null,
    val ratedAt: Long? = null,

    val status: ReservationStatus = ReservationStatus.PENDING,
    val createdAt: Long = Instant.now().toEpochMilli(),
    val updatedAt: Long = Instant.now().toEpochMilli()
)
