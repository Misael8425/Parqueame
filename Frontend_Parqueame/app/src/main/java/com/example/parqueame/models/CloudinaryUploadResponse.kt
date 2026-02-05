package com.example.parqueame.models

data class CloudinaryUploadResponse(
    val secure_url: String?,
    val public_id: String?,
    val resource_type: String? = null,
    val bytes: Long? = null,
    val original_filename: String? = null
)
