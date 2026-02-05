package com.example.parqueame.models

import kotlinx.serialization.Serializable

/**
 * Request que usa ApiService.crearParqueo (POST /parkings).
 * Debe espejar al DTO del backend y contener solicitudTipo.
 * Valores comunes: "Apertura" | "Edicion".
 */
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
    val solicitudTipo: String? = null   // <- IMPORTANTE
)
