package com.parqueame.models

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class LoginSuccessResponse(
    val success: Boolean = true,
    val message: String,
    val user: LoginUserData   // 👈 ahora usamos LoginUserData
)

@Serializable
data class LoginUserData(
    val id: String,
    val correo: String,
    val nombre: String,
    val tipo: String,           // 👈 agregado
    val tipoDocumento: String,
    val documento: String,
    val imagenesURL: String? = null,
    val estado: Boolean = true
)