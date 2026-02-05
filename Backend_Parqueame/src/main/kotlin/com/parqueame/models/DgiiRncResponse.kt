package com.parqueame.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DgiiRncResponse(
    val rnc: String,
    @SerialName("razon_social") val razonSocial: String,
    @SerialName("actividad_economica") val actividadEconomica: String? = null,
    val estado: String? = null,
    @SerialName("fecha_inicio_operaciones") val fechaInicioOperaciones: String? = null,
    @SerialName("regimen_pago") val regimenPago: String? = null
)