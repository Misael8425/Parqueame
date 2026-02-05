package com.parqueame.models

import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse(
    val success: Boolean,
    val message: String
)

@Serializable
data class ApiResponseWithData<T>(
    val success: Boolean,
    val message: String,
    val data: T
)
