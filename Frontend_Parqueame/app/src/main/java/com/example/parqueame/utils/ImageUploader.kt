package com.example.parqueame.utils

import android.content.Context
import android.net.Uri
import com.example.parqueame.api.CloudinaryInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

suspend fun subirImagenACloudinary(uri: Uri, context: Context): String? {
    return withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val inputStream = contentResolver.openInputStream(uri) ?: return@withContext null
            val fileBytes = inputStream.readBytes()

            val requestFile = fileBytes.toRequestBody("image/*".toMediaTypeOrNull())
            val filePart = MultipartBody.Part.createFormData("file", "perfil.jpg", requestFile)

            val presetBody = "parqueame_preset".toRequestBody("text/plain".toMediaTypeOrNull())
            val folderBody = "perfiles".toRequestBody("text/plain".toMediaTypeOrNull())

            val response = CloudinaryInstance.api.subirImagen(filePart, presetBody, folderBody)

            if (response.isSuccessful) {
                response.body()?.secure_url
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
