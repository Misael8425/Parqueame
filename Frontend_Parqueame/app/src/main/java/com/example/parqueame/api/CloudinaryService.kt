package com.example.parqueame.api

import com.example.parqueame.models.CloudinaryUploadResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface CloudinaryService {
    // Imágenes
    @Multipart
    @POST("image/upload")
    suspend fun subirImagen(
        @Part file: MultipartBody.Part,
        @Part("upload_preset") uploadPreset: RequestBody,
        @Part("folder") folder: RequestBody
    ): Response<CloudinaryUploadResponse>

    // (Compat) RAW – puede ser bloqueado si la cuenta está "untrusted"
    @Multipart
    @POST("raw/upload")
    suspend fun subirArchivoRaw(
        @Part file: MultipartBody.Part,
        @Part("upload_preset") uploadPreset: RequestBody,
        @Part("folder") folder: RequestBody
    ): Response<CloudinaryUploadResponse>

    // ✅ Recomendado para PDFs: evita "show_original_customer_untrusted"
    @Multipart
    @POST("auto/upload")
    suspend fun subirArchivoAuto(
        @Part file: MultipartBody.Part,
        @Part("upload_preset") uploadPreset: RequestBody,
        @Part("folder") folder: RequestBody
    ): Response<CloudinaryUploadResponse>
}