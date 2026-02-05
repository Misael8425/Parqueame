package com.example.parqueame.utils

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

fun strPart(value: String): RequestBody =
    value.toRequestBody("text/plain".toMediaTypeOrNull())

/**
 * Convierte un Uri del SAF en MultipartBody.Part preservando MIME y filename.
 * Copia el contenido a un archivo temporal para streaming seguro.
 */
fun uriToMultipart(
    resolver: ContentResolver,
    uri: Uri,
    formField: String = "file"
): MultipartBody.Part {
    val mime = resolver.getType(uri) ?: "application/octet-stream"

    val fileName = runCatching {
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { c ->
                val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (c.moveToFirst() && idx >= 0) c.getString(idx) else null
            }
    }.getOrNull() ?: "upload_${System.currentTimeMillis()}"

    val input = resolver.openInputStream(uri)
        ?: error("No se pudo abrir el stream del archivo")

    val cacheFile = File.createTempFile("upload_", "_$fileName")
    FileOutputStream(cacheFile).use { output -> input.copyTo(output) }

    val body = cacheFile.asRequestBody(mime.toMediaTypeOrNull())
    return MultipartBody.Part.createFormData(formField, fileName, body)
}
