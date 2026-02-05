package com.example.parqueame.models

data class ActualizarPerfilRequest(
    val correo: String,
    val nuevoNombre: String? = null,
    val nuevaFotoUrl: String? = null
)
