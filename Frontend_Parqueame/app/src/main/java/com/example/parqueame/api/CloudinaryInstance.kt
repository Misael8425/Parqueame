package com.example.parqueame.api

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object CloudinaryInstance {
    private const val BASE_URL = "https://api.cloudinary.com/v1_1/dvct4evbl/"

    private val client = OkHttpClient.Builder()
        .callTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val api: CloudinaryService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client) // timeouts para uploads grandes
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CloudinaryService::class.java)
    }
}