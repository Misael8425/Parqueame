package com.example.parqueame.models

data class UsuarioPropietario(
    val usuarioId: String,
    val usuarioTipo: String // Siempre debe ser "propietarios"
)

data class Parqueo(
    val id: String,
    val usuario: UsuarioPropietario,
    val nombre: String,
    val direccion: String,
    val capacidad: Int,
    val ubicacion: Pair<Double, Double>, // latitud, longitud
    val tarifaHora: Float,
    val imagenesURL: List<String>,
    val estado: Boolean
)