package com.example.parqueame.models

import kotlinx.serialization.Serializable

// CORREGIDO: Estructura que coincide con el backend
@Serializable
data class ApiResponse(
    val success: Boolean,
    val message: String,
    val error: String? = null,
    val id: String? = null
)

// Solicitud para enviar código de recuperación
@Serializable
data class EmailRequest(
    val email: String
)

// Verificación del código recibido por correo
@Serializable
data class CodeVerificationRequest(
    val email: String,
    val code: String
)

// Solicitud para establecer nueva contraseña
@Serializable
data class ResetPasswordRequest(
    val email: String,
    val code: String,          // ← IMPORTANTE: no usar "token"
    val newPassword: String
)

// Petición de login
@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

/**
 * Respuesta de login que coincide exactamente con el backend.
 */
@Serializable
data class LoginResponse(
    val success: Boolean = false,
    val message: String = "",
    val user: LoginUserData = LoginUserData()
)

@Serializable
data class LoginUserData(
    val id: String = "",
    val correo: String = "",
    val nombre: String = "",
    val tipo: String = "",             // "CONDUCTOR" | "EMPRESA" | ...
    val tipoDocumento: String = "",
    val documento: String = "",
    val imagenesURL: String? = null,
    val estado: Boolean = true
)