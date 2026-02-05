package com.example.parqueame.util

import android.content.ContentResolver
import android.net.Uri
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okio.BufferedSink
import okio.source

fun uriToMultipart(
    resolver: ContentResolver,
    uri: Uri,
    paramName: String,
    fileName: String? = null,
): MultipartBody.Part {
    val type = resolver.getType(uri) ?: "application/octet-stream"
    val name = fileName ?: queryDisplayName(resolver, uri) ?: "file"
    val requestBody = object : RequestBody() {
        override fun contentType() = type.toMediaTypeOrNull()
        override fun writeTo(sink: BufferedSink) {
            resolver.openInputStream(uri).use { input ->
                if (input != null) sink.writeAll(input.source())
            }
        }
    }
    return MultipartBody.Part.createFormData(paramName, name, requestBody)
}

private fun queryDisplayName(resolver: ContentResolver, uri: Uri): String? {
    val cursor = resolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)
    return cursor?.use {
        if (it.moveToFirst()) it.getString(0) else null
    }
}

fun strPart(value: String) = RequestBody.create("text/plain".toMediaTypeOrNull(), value)
