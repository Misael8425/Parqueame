package com.parqueame.controllers

import com.parqueame.models.ErrorResponse
import com.parqueame.services.DgiiService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class RncValidationResponse(
    val rnc: String,
    val normalizedRnc: String,
    val valid: Boolean,
    val message: String
)

@Serializable
data class RncSearchResult(
    val success: Boolean,
    val rnc: String?,
    val razonSocial: String?,
    val message: String,
    val searchStrategies: List<String>
)

fun Route.dgiiController(service: DgiiService) {
    route("/dgii") {

        // GET /dgii/rnc/{rnc}
        safeGet("/rnc/{rnc}") {
            val digits = call.extractRncDigitsOr400() ?: return@safeGet

            application.log.info("🔍 Buscando RNC: $digits")

            val result = service.consultarRnc(digits)
            if (result != null) {
                application.log.info("✅ RNC encontrado: ${result.rnc} - ${result.razonSocial}")
                call.respond(HttpStatusCode.OK, result)
            } else {
                application.log.warn("❌ RNC no encontrado: $digits")
                call.respond(
                    HttpStatusCode.NotFound,
                    ErrorResponse("not_found", "RNC no encontrado en la base de datos")
                )
            }
        }

        // GET /dgii/validate/{rnc}
        safeGet("/validate/{rnc}") {
            val raw = call.parameters["rnc"] ?: ""
            val resp = buildValidationResponse(raw)
            call.respond(HttpStatusCode.OK, resp)
        }

        // GET /dgii/search/{rnc} (debug)
        safeGet("/search/{rnc}") {
            val raw = call.parameters["rnc"] ?: ""
            val digits = raw.filter { it.isDigit() }

            application.log.info("🔍 Búsqueda detallada para: '$raw' -> '$digits'")

            if (!isRncDigitsValid(digits)) {
                val response = RncSearchResult(
                    success = false,
                    rnc = null,
                    razonSocial = null,
                    message = "RNC inválido. Debe tener 9 u 11 dígitos numéricos",
                    searchStrategies = emptyList()
                )
                call.respond(HttpStatusCode.BadRequest, response)
                return@safeGet
            }

            val result = service.consultarRnc(digits)
            val response = if (result != null) {
                RncSearchResult(
                    success = true,
                    rnc = result.rnc,
                    razonSocial = result.razonSocial,
                    message = "RNC encontrado exitosamente",
                    searchStrategies = listOf(
                        "Búsqueda exacta",
                        "Normalización con ceros",
                        "Versión sin ceros iniciales"
                    )
                )
            } else {
                RncSearchResult(
                    success = false,
                    rnc = null,
                    razonSocial = null,
                    message = "RNC no encontrado después de búsqueda exhaustiva",
                    searchStrategies = listOf(
                        "Búsqueda exacta: NO",
                        "Normalización: NO",
                        "Sin ceros iniciales: NO"
                    )
                )
            }

            call.respond(HttpStatusCode.OK, response)
        }

        // GET /dgii/stats
        safeGet("/stats") {
            try {
                val stats = service.getDatabaseStats()
                call.respond(HttpStatusCode.OK, stats)
            } catch (e: Exception) {
                application.log.error("Error obteniendo estadísticas", e)
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ErrorResponse("internal_error", "Error obteniendo estadísticas")
                )
            }
        }

        // GET /dgii/test/{rnc}
        safeGet("/test/{rnc}") {
            val raw = call.parameters["rnc"] ?: ""
            val digits = raw.onlyDigits()

            application.log.info("🧪 Testing RNC: '$raw' -> '$digits'")

            val candidates = mutableSetOf<String>().apply {
                add(digits)
                if (digits.length == 9) {
                    add(digits.padStart(11, '0'))
                } else if (digits.length == 11) {
                    add(digits.trimStart('0'))
                }
            }.distinct()

            val result = service.consultarRnc(digits)

            val testResponse = mapOf(
                "input" to raw,
                "digits" to digits,
                "valid_format" to isRncDigitsValid(digits),
                "search_candidates" to candidates,
                "found" to (result != null),
                "result" to result,
                "timestamp" to System.currentTimeMillis()
            )

            call.respond(HttpStatusCode.OK, testResponse)
        }
    }
}

/* ===========================
 * Helpers
 * =========================== */

private fun String.onlyDigits(): String = filter(Char::isDigit)

private fun isRncDigitsValid(digits: String): Boolean =
    (digits.length == 9 || digits.length == 11) &&
            digits.all { it.isDigit() } &&
            digits.toLongOrNull() != null &&
            digits != "0".repeat(digits.length)

private suspend fun ApplicationCall.extractRncDigitsOr400(): String? {
    val raw = parameters["rnc"]
    if (raw.isNullOrBlank()) {
        respond(HttpStatusCode.BadRequest, ErrorResponse("bad_request", "RNC no proporcionado"))
        return null
    }

    val digits = raw.onlyDigits()
    if (!isRncDigitsValid(digits)) {
        respond(
            HttpStatusCode.BadRequest,
            ErrorResponse("invalid_rnc", "RNC inválido. Debe tener 9 u 11 dígitos numéricos")
        )
        return null
    }
    return digits
}

private fun buildValidationResponse(raw: String): RncValidationResponse {
    val digits = raw.onlyDigits()
    val valid = isRncDigitsValid(digits)
    val normalized = if (valid) digits.padStart(11, '0') else raw
    val msg = if (valid) "RNC válido" else "RNC inválido. Debe tener 9 u 11 dígitos numéricos"

    return RncValidationResponse(
        rnc = raw,
        normalizedRnc = normalized,
        valid = valid,
        message = msg
    )
}

/**
 * Wrapper GET con manejo uniforme de excepciones.
 * Usa RoutingContext (Ktor 2.x).
 */
private fun Route.safeGet(
    path: String,
    handler: suspend RoutingContext.() -> Unit
) {
    get(path) {
        val startTime = System.currentTimeMillis()
        try {
            handler()
            val duration = System.currentTimeMillis() - startTime
            application.log.info("✅ ${call.request.httpMethod.value} ${call.request.path()} - ${duration}ms")
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            application.log.error("❌ ${call.request.httpMethod.value} ${call.request.path()} - ${duration}ms", e)
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse("internal_error", "Error interno del servidor: ${e.message}")
            )
        }
    }
}
