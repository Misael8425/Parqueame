package com.parqueame.dto

import com.parqueame.models.*
import kotlinx.serialization.Serializable

@Serializable
data class CreateParkingLotRequest(
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
    // 👇 NUEVO: tipo oculto de auditoría
    val solicitudTipo: String? = null // "Apertura" | "Edicion" | "Activo" | "Inactivo"
)

/** NUEVO: DTO de comentario para respuesta */
@Serializable
data class ParkingCommentDto(
    val type: String,
    val text: String,
    val authorId: String? = null,
    val authorEmail: String? = null,
    val createdAt: Long
)

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
    /** NUEVO: último motivo de rechazo (si existe) */
    val rejectionReason: String? = null,
    /** OPCIONAL: lista de comentarios (si quieres exponerlos) */
    val comments: List<ParkingCommentDto> = emptyList(),
    // 👇 OPCIONAL: si quieres exponerlo en responses (puedes quitarlo si no)
    val solicitudTipo: String? = null
)

private fun latestRejectionReason(comments: List<ParkingCommentDto>): String? =
    comments.filter { it.type == "rejection" }
        .maxByOrNull { it.createdAt }
        ?.text

fun ParkingLot.toDto(): ParkingLotDto {
    val commentsDto = comments.map {
        ParkingCommentDto(
            type = it.type,
            text = it.text,
            authorId = it.authorId,
            authorEmail = it.authorEmail,
            createdAt = it.createdAt
        )
    }
    return ParkingLotDto(
        id = _id.toHexString(),
        localName = localName,
        address = address,
        capacity = capacity,
        priceHour = priceHour,
        daysOfWeek = daysOfWeek,
        schedules = schedules,
        characteristics = characteristics,
        photos = photos,
        infraDocUrl = infraDocUrl,
        // BD: [lng,lat]  →  DTO: GeoPoint(lat,lng)
        location = location?.let { coords ->
            val lng = coords.getOrNull(0)
            val lat = coords.getOrNull(1)
            if (lat != null && lng != null) GeoPoint(lat = lat, lng = lng) else null
        },
        status = status,
        createdBy = createdBy,
        createdAt = createdAt,
        updatedAt = updatedAt,
        rejectionReason = latestRejectionReason(commentsDto),
        comments = commentsDto,
        solicitudTipo = this.solicitudTipo // si prefieres no exponerlo, quita este campo del DTO
    )
}

@Serializable
data class ParkingCreateResponse(
    val success: Boolean = true,
    val id: String,
    val message: String = "Parqueo creado correctamente"
)