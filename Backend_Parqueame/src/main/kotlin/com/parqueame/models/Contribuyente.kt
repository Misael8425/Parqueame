package com.parqueame.models

import kotlinx.serialization.Serializable
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.codecs.pojo.annotations.BsonProperty

@Serializable
data class Contribuyente(
    @BsonId
    val _id: String, // RNC como String (puede ser 9 o 11 dígitos)

    @BsonProperty("razon_social")
    val razonSocial: String,

    @BsonProperty("actividad_economica")
    val actividadEconomica: String? = null,

    @BsonProperty("fecha_inicio_operaciones")
    val fechaInicioOperaciones: String? = null,

    val estado: String? = null,

    @BsonProperty("regimen_pago")
    val regimenPago: String? = null
)