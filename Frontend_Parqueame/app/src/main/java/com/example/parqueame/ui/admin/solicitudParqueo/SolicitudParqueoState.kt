package com.example.parqueame.ui.admin.solicitudParqueo

import com.example.parqueame.ui.admin.DaysOTWeek

data class SolicitudParqueoState(
    val id: String = java.util.UUID.randomUUID().toString(),
    val start: String,
    val end: String,
    val days: Set<DaysOTWeek> = emptySet()
)