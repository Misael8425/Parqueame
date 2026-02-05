package com.parqueame.models

import kotlinx.serialization.Serializable
import org.bson.types.ObjectId
import kotlinx.serialization.Contextual
import org.bson.codecs.pojo.annotations.BsonId

@Serializable
data class ScheduleRange(
    val open: String,
    val close: String
)

@Serializable
enum class WeekDay { MON, TUE, WED, THU, FRI, SAT, SUN }

@Serializable
data class GeoPoint(
    val lat: Double? = null,
    val lng: Double? = null
)

/** NUEVO **/
@Serializable
data class ParkingComment(
    val type: String,                // "rejection" | "note" | "system"
    val text: String,
    val authorId: String? = null,
    val authorEmail: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class ParkingRating(
    @BsonId @Contextual
    val id: ObjectId = ObjectId(),

    val parkingId: String,        // _id del parqueo como string
    val userId: String,           // quien califica
    val reservationId: String,    // 1 rating por reserva
    val stars: Int,               // 1..5
    val comment: String? = null,

    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long? = null
)


@Serializable
data class ParkingLot(
    @BsonId @Contextual
    val _id: ObjectId = ObjectId(),
    val localName: String,
    val address: String,
    val capacity: Int,
    val priceHour: Int,
    val daysOfWeek: List<WeekDay>,
    val schedules: List<ScheduleRange>,
    val characteristics: List<String> = emptyList(),
    val photos: List<String> = emptyList(),
    val infraDocUrl: String? = null,
    val location: List<Double>? = null,
    val status: String = "pending",
    // 👇 NUEVO: auditoría de acción ("Apertura" | "Edicion" | "Activo" | "Inactivo")
    val solicitudTipo: String = "Apertura",
    val createdBy: String? = null,
    val createdByDocumento: String? = null,
    val createdByTipoDocumento: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),

    // ✅ Resumen de calificaciones (nuevo)
    val ratingCount: Int = 0,
    val ratingSum: Int = 0,
    val avgRating: Double = 0.0,

    /** NUEVO: comentarios **/
    val comments: List<ParkingComment> = emptyList()
)