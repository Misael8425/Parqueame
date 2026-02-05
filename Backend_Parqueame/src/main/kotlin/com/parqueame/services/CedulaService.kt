package com.parqueame.services

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.seconds

@Serializable
data class CedulaValidationResponse(val valid: Boolean)

@Serializable
data class CachedResponse(
    val response: CedulaValidationResponse,
    val timestamp: Long
)

// Cache en memoria con TTL de 5 minutos
private val cache = ConcurrentHashMap<String, CachedResponse>()
private val cacheMutex = Mutex()
private const val CACHE_TTL_MS = 5 * 60 * 1000L // 5 minutos

// Cliente HTTP optimizado con pool de conexiones y timeouts para Ktor 3.1.3
private val httpClient = HttpClient(CIO) {
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
        })
    }

    install(HttpTimeout) {
        requestTimeoutMillis = 10.seconds.inWholeMilliseconds
        connectTimeoutMillis = 5.seconds.inWholeMilliseconds
        socketTimeoutMillis = 10.seconds.inWholeMilliseconds
    }

    engine {
        // Pool de conexiones para reutilizar conexiones TCP
        maxConnectionsCount = 100
        endpoint {
            maxConnectionsPerRoute = 20
            pipelineMaxSize = 20
            keepAliveTime = 5000
            connectTimeout = 5000
            connectAttempts = 3
        }
    }
}

// Función para limpiar cache expirado
private suspend fun cleanExpiredCache() {
    cacheMutex.withLock {
        val now = System.currentTimeMillis()
        val expiredKeys = cache.entries
            .filter { now - it.value.timestamp > CACHE_TTL_MS }
            .map { it.key }

        expiredKeys.forEach { cache.remove(it) }
    }
}

// Función para obtener del cache
private suspend fun getCachedResponse(cedula: String): CedulaValidationResponse? {
    cacheMutex.withLock {
        val cached = cache[cedula]
        if (cached != null) {
            val isExpired = System.currentTimeMillis() - cached.timestamp > CACHE_TTL_MS
            if (!isExpired) {
                return cached.response
            } else {
                cache.remove(cedula)
            }
        }
        return null
    }
}

// Función para guardar en cache
private suspend fun cacheResponse(cedula: String, response: CedulaValidationResponse) {
    cacheMutex.withLock {
        cache[cedula] = CachedResponse(response, System.currentTimeMillis())

        // Limpieza periódica cada 100 inserciones
        if (cache.size % 100 == 0) {
            cleanExpiredCache()
        }
    }
}

fun Route.cedulaController() {
    route("/validar-cedula") {
        get("{id}") {
            val cedula = call.parameters["id"]?.trim()

            if (cedula.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Cédula no proporcionada"))
                return@get
            }

            // Validación básica de formato antes de hacer la llamada
            if (!isValidCedulaFormat(cedula)) {
                call.respond(HttpStatusCode.BadRequest, mapOf(
                    "error" to "Formato de cédula inválido",
                    "valid" to false
                ))
                return@get
            }

            try {
                // 1. Verificar cache primero
                val cachedResult = getCachedResponse(cedula)
                if (cachedResult != null) {
                    call.respond(HttpStatusCode.OK, cachedResult)
                    return@get
                }

                // 2. Hacer llamada a la API externa
                val response = httpClient.get("https://api.digital.gob.do/v3/cedulas/$cedula/validate") {
                    headers {
                        append(HttpHeaders.Accept, "application/json")
                        append(HttpHeaders.UserAgent, "ParqueameApp/1.0")
                        append(HttpHeaders.Connection, "keep-alive")
                    }
                }

                val result = response.body<CedulaValidationResponse>()

                // 3. Guardar en cache solo si es válida (evitar cache de errores temporales)
                if (result.valid) {
                    cacheResponse(cedula, result)
                }

                call.respond(HttpStatusCode.OK, result)

            } catch (e: Exception) {
                e.printStackTrace()

                // Respuesta rápida en caso de error
                call.respond(
                    HttpStatusCode.RequestTimeout,
                    mapOf(
                        "error" to "Servicio de validación no disponible temporalmente",
                        "valid" to false,
                        "cached" to false
                    )
                )
            }
        }
    }
}

// Validación básica de formato de cédula dominicana
private fun isValidCedulaFormat(cedula: String): Boolean {
    // Formato: 000-0000000-0 (con o sin guiones)
    val cleaned = cedula.replace("-", "")
    return cleaned.length == 11 && cleaned.all { it.isDigit() }
}