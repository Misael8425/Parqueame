package com.example.parqueame.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class GeoPoint(
    val lat: Double? = null,
    val lng: Double? = null
) : Parcelable

@Parcelize
@Serializable
data class ParkingCommentDto(
    val type: String,
    val text: String,
    val authorId: String? = null,
    val authorEmail: String? = null,
    val createdAt: Long
) : Parcelable

@Serializable
data class CreateCommentRequest(
    val type: String = "note",
    val text: String,
    val authorId: String? = null,
    val authorEmail: String? = null
)

/**
 * DTO que envías desde el front al backend cuando CREAS o EDITAS el parqueo
 * (lo usas en PUT también). Agregamos `solicitudTipo`.
 * Valores esperados por negocio: "Apertura" | "Edicion"
 */
@Serializable
data class CreateParkingLotRequestDto(
    val localName: String,
    val address: String,
    val capacity: Int,
    val priceHour: Int,
    val daysOfWeek: List<WeekDay>,
    val schedules: List<ScheduleRange>,
    val characteristics: List<String> = emptyList(),
    val photos: List<String> = emptyList(),
    val infraDocUrl: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    val status: String? = null,
    val solicitudTipo: String? = null           // <--- NUEVO
)

@Parcelize
@Serializable
data class ParkingLotDto(
    val id: String,
    val localName: String,
    val address: String,
    val capacity: Int,
    val priceHour: Int,
    val daysOfWeek: List<WeekDay>,
    val schedules: List<ScheduleRange>,
    val characteristics: List<String> = emptyList(),
    val photos: List<String> = emptyList(),
    val infraDocUrl: String? = null,
    val location: GeoPoint? = null,
    val status: String = "pending",
    val createdBy: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val rejectionReason: String? = null,
    val comments: List<ParkingCommentDto> = emptyList()
) : Parcelable

/**
 * Body para actualizar ESTADO. Agregamos `solicitudTipo`
 * para auditar acciones tipo "Activo"/"Inactivo".
 */
@Serializable
data class UpdateParkingStatusRequest(
    val status: String,
    val rejectionReason: String? = null,
    val solicitudTipo: String? = null          // <--- NUEVO
)