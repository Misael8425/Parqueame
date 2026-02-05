package com.example.parqueame.models

import kotlinx.serialization.Serializable

@Serializable
data class Usuario(
    val nombre: String,
    val correo: String,
    val contrasena: String,
    val tipo: String,  // CONDUCTOR, ADMINISTRADOR, etc.
    val tipoDocumento: String,  // CEDULA, RNC
    val documento: String,
    val imagenesURL: String,
    val estado: Boolean
)
