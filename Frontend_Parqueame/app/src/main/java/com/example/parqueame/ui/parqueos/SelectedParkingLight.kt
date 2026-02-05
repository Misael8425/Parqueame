package com.example.parqueame.ui.parqueos

import java.io.Serializable

/**
 * Snapshot ligero del parqueo para navegar sin depender del backend.
 * Lo guarda/lee SavedStateHandle (Serializable).
 */
data class SelectedParkingLight(
    val id: String,
    val localName: String,
    val address: String,
    val priceHour: Int,
    val capacity: Int,
    val firstPhoto: String? = null
) : Serializable
