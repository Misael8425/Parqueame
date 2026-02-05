package com.example.parqueame.api

import com.example.parqueame.data.TokenManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

object RetrofitInstance {

    // ⚠️ Usa tu dominio HTTPS de Railway con slash final
    private const val BASE_URL = "https://parqueame-backend-production.up.railway.app/"
    //private const val BASE_URL = "http://192.168.1.217:8080/"

    private class RetryInterceptor(
        private val maxRetries: Int = 2,
        private val baseDelayMs: Long = 1200
    ) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            var tryCount = 0
            while (true) {
                try {
                    val resp = chain.proceed(chain.request())
                    if (resp.code in arrayOf(408, 429, 500, 502, 503, 504) && tryCount < maxRetries) {
                        resp.close()
                        Thread.sleep(baseDelayMs * (1L shl tryCount))
                        tryCount++
                        continue
                    }
                    return resp
                } catch (e: IOException) {
                    if (tryCount >= maxRetries) throw e
                    Thread.sleep(baseDelayMs * (1L shl tryCount))
                    tryCount++
                }
            }
        }
    }

    private fun baseLogging(): HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }

    private fun buildOkHttp(vararg extra: Interceptor): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(25, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .callTimeout(70, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .pingInterval(15, TimeUnit.SECONDS)
            .addInterceptor(RetryInterceptor())
            .addInterceptor(baseLogging())
            .apply { extra.forEach { addInterceptor(it) } }
            .build()

    private fun buildRetrofit(client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    // API sin token (login, registro, recovery, etc.)
    val apiService: ApiService by lazy {
        buildRetrofit(buildOkHttp()).create(ApiService::class.java)
    }

    // API con token (si lo usas en otras partes)
    fun create(tokenManager: TokenManager): ApiService {
        val client = buildOkHttp(AuthInterceptor(tokenManager))
        return buildRetrofit(client).create(ApiService::class.java)
    }
}
