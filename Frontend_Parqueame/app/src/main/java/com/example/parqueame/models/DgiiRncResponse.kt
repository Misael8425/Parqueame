package com.example.parqueame.models

import com.google.gson.annotations.SerializedName

data class DgiiRncResponse(
    val rnc: String,
    @SerializedName("razon_social") val razonSocial: String,
    @SerializedName("actividad_economica") val actividadEconomica: String? = null,
    val estado: String? = null,
    @SerializedName("fecha_inicio_operaciones") val fechaInicioOperaciones: String? = null,
    @SerializedName("regimen_pago") val regimenPago: String? = null
)