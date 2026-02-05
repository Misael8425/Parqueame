package com.example.parqueame.data

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.parqueame.api.CloudinaryInstance
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

object CloudUploader {

    // ⚠️ Reemplázalo por tu unsigned preset de Cloudinary
    private const val UPLOAD_PRESET = "tu_unsigned_preset"

    private const val FOLDER_IMAGES = "parqueame/parking_lots/images"
    private const val FOLDER_DOCS   = "parqueame/parking_lots/infra"

    private fun getFileName(ctx: Context, uri: Uri): String {
        var name = "file-${System.currentTimeMillis()}"
        if ("content".equals(uri.scheme, ignoreCase = true)) {
            ctx.contentResolver.query(uri, null, null, null, null)?.use { c ->
                val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && c.moveToFirst()) name = c.getString(idx) ?: name
            }
        } else {
            uri.path?.let { name = File(it).name }
        }
        return name
    }

    private fun copyToCache(ctx: Context, uri: Uri): File {
        val fileName = getFileName(ctx, uri)
        val input = ctx.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("No se pudo abrir el archivo")
        val outFile = File(ctx.cacheDir, "up-${System.currentTimeMillis()}-$fileName")
        FileOutputStream(outFile).use { out -> input.copyTo(out) }
        return outFile
    }

    private fun guessMime(ctx: Context, uri: Uri, fallback: String): String {
        val cr: ContentResolver = ctx.contentResolver
        return cr.getType(uri) ?: fallback
    }

    private fun presetRB(): RequestBody =
        UPLOAD_PRESET.toRequestBody("text/plain".toMediaTypeOrNull())

    private fun folderRB(folder: String): RequestBody =
        folder.toRequestBody("text/plain".toMediaTypeOrNull())

    /** Sube imágenes y retorna secure_url */
    suspend fun uploadImage(ctx: Context, uri: Uri): String {
        val file = copyToCache(ctx, uri)
        val mime = guessMime(ctx, uri, "image/*")
        val reqFile = file.asRequestBody(mime.toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("file", file.name, reqFile)

        val resp = CloudinaryInstance.api.subirImagen(
            file = part,
            uploadPreset = presetRB(),
            folder = folderRB(FOLDER_IMAGES)
        )

        if (!resp.isSuccessful) {
            val body = resp.errorBody()?.string().orEmpty()
            throw IllegalStateException("Cloudinary imagen falló: ${resp.code()} $body")
        }
        return resp.body()?.secure_url ?: error("Cloudinary no devolvió secure_url")
    }

    /** Sube PDFs/Docs vía auto/upload (evita bloqueo de “original/raw”) */
    suspend fun uploadDocument(ctx: Context, uri: Uri): String {
        val file = copyToCache(ctx, uri)
        val mime = guessMime(ctx, uri, "application/octet-stream")
        val reqFile = file.asRequestBody(mime.toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("file", file.name, reqFile)

        val resp = CloudinaryInstance.api.subirArchivoAuto(
            file = part,
            uploadPreset = presetRB(),
            folder = folderRB(FOLDER_DOCS)
        )

        if (!resp.isSuccessful) {
            val body = resp.errorBody()?.string().orEmpty()
            throw IllegalStateException("Cloudinary (auto) falló: ${resp.code()} $body")
        }
        return resp.body()?.secure_url ?: error("Cloudinary no devolvió secure_url")
    }

    /* ===== utilidades ===== */

    /** true si el Uri ya es http/https (recurso remoto existente) */
    fun Uri.isRemoteUrl(): Boolean {
        val s = scheme?.lowercase()
        return s == "http" || s == "https"
    }

    /** Agrega fl=attachment si quieres forzar descarga del PDF */
    fun appendFlAttachment(url: String): String {
        return if (url.contains("?")) {
            if (url.contains("fl=attachment")) url else "$url&fl=attachment"
        } else {
            "$url?fl=attachment"
        }
    }
}
