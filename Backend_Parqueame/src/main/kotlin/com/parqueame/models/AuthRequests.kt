    package com.parqueame.models

    import kotlinx.serialization.Serializable

    @Serializable
    data class EmailRequest(val email: String)

    @Serializable
    data class CodeVerificationRequest(val email: String, val code: String)

    @Serializable
    data class ResetPasswordRequest(val email: String, val code: String, val newPassword: String)
