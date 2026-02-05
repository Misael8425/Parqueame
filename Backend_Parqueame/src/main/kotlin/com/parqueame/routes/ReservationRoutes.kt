package com.parqueame.routes

import com.parqueame.dto.CreateReservationRequest
import com.parqueame.services.ReservationService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.reservationRoutes(service: ReservationService) {

    route("/reservations") {
        // -------------------- CREATE --------------------
        post {
            val userId = call.request.headers["X-User-Id"]
                ?: return@post call.respond(
                    HttpStatusCode.Unauthorized,
                    mapOf("message" to "X-User-Id requerido")
                )

            val body = call.receive<CreateReservationRequest>()

            runCatching { service.create(userId, body) }
                .onSuccess { call.respond(HttpStatusCode.Created, it) }
                .onFailure { ex ->
                    call.application.environment.log.error("create reservation error", ex)
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("message" to (ex.message ?: "Error"))
                    )
                }
        }

        // -------------------- GET by id --------------------
        get("/{id}") {
            val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            runCatching { service.get(id) }
                .onSuccess { call.respond(it) }
                .onFailure { ex ->
                    call.application.environment.log.warn("reservation not found: $id - ${ex.message}")
                    call.respond(HttpStatusCode.NotFound, mapOf("message" to (ex.message ?: "No encontrado")))
                }
        }

        // -------------------- LIST (todas) --------------------
        get("/by-user/{userId}") {
            val userId = call.parameters["userId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            call.respond(service.listByUser(userId))
        }

        get("/by-parking/{parkingId}") {
            val pid = call.parameters["parkingId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            call.respond(service.listByParking(pid))
        }

        // -------------------- CANCEL --------------------
        patch("/{id}/cancel") {
            val id = call.parameters["id"] ?: return@patch call.respond(HttpStatusCode.BadRequest)
            if (service.cancel(id)) {
                call.respond(HttpStatusCode.OK, mapOf("ok" to true))
            } else {
                call.respond(HttpStatusCode.NotFound, mapOf("message" to "No se pudo cancelar"))
            }
        }

        // ==================== NUEVO: ACTIVAS ====================

        // Activas por usuario
        // GET /reservations/active/by-user/{userId}
        get("/active/by-user/{userId}") {
            val userId = call.parameters["userId"] ?: return@get call.respond(
                HttpStatusCode.BadRequest, mapOf("message" to "Falta parámetro {userId}")
            )
            runCatching { service.listActiveByUser(userId) }
                .onSuccess { call.respond(HttpStatusCode.OK, it) }
                .onFailure { ex ->
                    call.application.environment.log.error("active by user error: $userId", ex)
                    call.respond(HttpStatusCode.BadRequest, mapOf("message" to (ex.message ?: "Error")))
                }
        }

        // Activas por parqueo
        // GET /reservations/active/by-parking/{parkingId}
        get("/active/by-parking/{parkingId}") {
            val parkingId = call.parameters["parkingId"] ?: return@get call.respond(
                HttpStatusCode.BadRequest, mapOf("message" to "Falta parámetro {parkingId}")
            )
            runCatching { service.listActiveByParking(parkingId) }
                .onSuccess { call.respond(HttpStatusCode.OK, it) }
                .onFailure { ex ->
                    call.application.environment.log.error("active by parking error: $parkingId", ex)
                    call.respond(HttpStatusCode.BadRequest, mapOf("message" to (ex.message ?: "Error")))
                }
        }

        // ¿Esta reserva está activa?
        // GET /reservations/{id}/is-active  -> { "active": true|false }
        get("/{id}/is-active") {
            val id = call.parameters["id"] ?: return@get call.respond(
                HttpStatusCode.BadRequest, mapOf("message" to "Falta parámetro {id}")
            )
            val active = runCatching { service.isActiveReservation(id) }.getOrElse { false }
            call.respond(HttpStatusCode.OK, mapOf("active" to active))
        }
    }

    // ==================== Disponibilidad ====================
    // Disponibilidad por parqueo y rango [startMin, endMin)
    get("/parkings/{id}/availability") {
        val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)

        // Alias: startMin | start | startEpochMin   y   endMin | end | endEpochMin
        val startStr = call.request.queryParameters["startMin"]
            ?: call.request.queryParameters["startEpochMin"]
            ?: call.request.queryParameters["start"]

        val endStr = call.request.queryParameters["endMin"]
            ?: call.request.queryParameters["endEpochMin"]
            ?: call.request.queryParameters["end"]

        val start = startStr?.toLongOrNull()
        val end   = endStr?.toLongOrNull()

        if (start == null || end == null) {
            return@get call.respond(
                HttpStatusCode.BadRequest,
                mapOf("message" to "startMin/endMin requeridos (minutos desde epoch). " +
                        "También se aceptan alias start|startEpochMin y end|endEpochMin")
            )
        }

        if (end <= start) {
            return@get call.respond(
                HttpStatusCode.BadRequest,
                mapOf("message" to "endMin debe ser mayor que startMin")
            )
        }

        runCatching { service.availability(id, start, end) }
            .onSuccess { call.respond(it) }
            .onFailure { ex ->
                val msg = ex.message ?: "Error"
                val status = if (msg.contains("Parqueo no encontrado", ignoreCase = true)) {
                    HttpStatusCode.NotFound
                } else {
                    HttpStatusCode.BadRequest
                }
                call.application.environment.log.warn("availability error for $id [$start,$end): $msg", ex)
                call.respond(status, mapOf("message" to msg))
            }
    }
}