package com.parqueame.controllers

import com.parqueame.DatabaseFactory
import com.parqueame.dto.CreateParkingLotRequest
import com.parqueame.dto.ParkingCreateResponse
import com.parqueame.dto.toDto
import com.parqueame.models.ErrorResponse
import com.parqueame.models.GeoPoint
import com.parqueame.models.ParkingLot
import com.parqueame.models.ParkingComment
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.bson.types.ObjectId
import org.litote.kmongo.*
import kotlin.math.min

fun Route.parkingController() {

    route("/parkings") {

        // ---------- Crear parqueo ----------
        post {
            try {
                val body = call.receive<CreateParkingLotRequest>()
                if (body.localName.isBlank() ||
                    body.address.isBlank() ||
                    body.capacity <= 0 ||
                    body.priceHour < 0 ||
                    body.daysOfWeek.isEmpty() ||
                    body.schedules.isEmpty()
                ) {
                    call.respond(HttpStatusCode.BadRequest,
                        ErrorResponse("bad_request", "Campos requeridos faltantes o inválidos"))
                    return@post
                }

                fun extractNoAuth(name: String): String? =
                    call.request.headers[name]?.takeIf { it.isNotBlank() }
                        ?: call.request.queryParameters[name]?.takeIf { it.isNotBlank() }

                val userId   = extractNoAuth("X-User-Id")
                val ownerDoc = extractNoAuth("X-Owner-Documento")
                val ownerTipoNorm = extractNoAuth("X-Owner-Tipo")
                    ?.trim()?.uppercase()?.replace("É", "E")

                if (userId.isNullOrBlank() && (ownerDoc.isNullOrBlank() || ownerTipoNorm.isNullOrBlank())) {
                    call.respond(HttpStatusCode.BadRequest,
                        ErrorResponse("bad_request","Falta identidad del dueño (X-User-Id o ownerDocumento+ownerTipo)"))
                    return@post
                }

                val doc = ParkingLot(
                    localName = body.localName.trim(),
                    address = body.address.trim(),
                    capacity = body.capacity,
                    priceHour = body.priceHour,
                    daysOfWeek = body.daysOfWeek,
                    schedules = body.schedules,
                    characteristics = body.characteristics,
                    photos = body.photos,
                    infraDocUrl = body.infraDocUrl,
                    location = if (body.lat != null && body.lng != null) listOf(body.lng, body.lat) else null,
                    status = "pending",
                    solicitudTipo = body.solicitudTipo ?: "Apertura", // 👈 default
                    createdBy = userId,
                    createdByDocumento = ownerDoc,
                    createdByTipoDocumento = ownerTipoNorm,
                    comments = emptyList()
                )

                val res = DatabaseFactory.parkingsCollection.insertOne(doc)
                val id = res.insertedId?.asObjectId()?.value?.toHexString() ?: doc._id.toHexString()
                call.respond(HttpStatusCode.Created, ParkingCreateResponse(id = id))

            } catch (e: Exception) {
                e.printStackTrace()
                call.respond(HttpStatusCode.InternalServerError,
                    ErrorResponse("internal_error", e.message ?: "Error creando parqueo"))
            }
        }

        // ---------- Listar parqueos (filtrado por identidad) ----------
        get {
            try {
                val hUserId = call.request.headers["X-User-Id"]?.takeIf { it.isNotBlank() }
                val qUserId = call.request.queryParameters["createdBy"]?.takeIf { it.isNotBlank() }
                val qDoc    = call.request.queryParameters["ownerDocumento"]?.takeIf { it.isNotBlank() }
                val qTipo   = call.request.queryParameters["ownerTipo"]?.takeIf { it.isNotBlank() }
                    ?.trim()?.uppercase()?.replace("É", "E")

                val col = DatabaseFactory.parkingsCollection

                val list = when {
                    hUserId != null -> col.find(ParkingLot::createdBy eq hUserId).toList()
                    qUserId != null -> col.find(ParkingLot::createdBy eq qUserId).toList()
                    qDoc != null && qTipo != null -> col.find(
                        and(ParkingLot::createdByDocumento eq qDoc, ParkingLot::createdByTipoDocumento eq qTipo)
                    ).toList()
                    else -> {
                        call.respond(HttpStatusCode.BadRequest,
                            ErrorResponse("bad_request", "Falta filtro: X-User-Id o createdBy u ownerDocumento+ownerTipo"))
                        return@get
                    }
                }.map { it.toDto() }

                if (list.isEmpty()) { call.respond(HttpStatusCode.NoContent); return@get }
                call.respond(HttpStatusCode.OK, list)

            } catch (e: Exception) {
                e.printStackTrace()
                call.respond(HttpStatusCode.InternalServerError,
                    ErrorResponse("internal_error", e.message ?: "Error listando parqueos"))
            }
        }

        // ---------- Listar parqueos públicos aprobados ----------
        get("/approved") {
            try {
                val onlyWithLocation = call.request.queryParameters["withLocation"]?.toBoolean() ?: false
                val limit = call.request.queryParameters["limit"]?.toIntOrNull()?.coerceIn(1, 500) ?: 200
                val skip  = call.request.queryParameters["skip"]?.toIntOrNull()?.coerceAtLeast(0) ?: 0

                val col = DatabaseFactory.parkingsCollection

                val filter = if (onlyWithLocation) {
                    and(ParkingLot::status eq "approved", ParkingLot::location ne null)
                } else {
                    ParkingLot::status eq "approved"
                }

                val list = col.find(filter)
                    .sort(descending(ParkingLot::updatedAt))
                    .skip(skip)
                    .limit(limit)
                    .toList()
                    .map { it.toDto() }

                if (list.isEmpty()) { call.respond(HttpStatusCode.NoContent); return@get }
                call.respond(HttpStatusCode.OK, list)
            } catch (e: Exception) {
                e.printStackTrace()
                call.respond(HttpStatusCode.InternalServerError,
                    ErrorResponse("internal_error", e.message ?: "Error listando parqueos aprobados"))
            }
        }

        // ---------- Obtener por id ----------
        get("/{id}") {
            try {
                val id = call.parameters["id"]
                if (id.isNullOrBlank() || !ObjectId.isValid(id)) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("bad_request", "ID inválido"))
                    return@get
                }
                val found = DatabaseFactory.parkingsCollection.findOneById(ObjectId(id))
                if (found == null) {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("not_found", "Parqueo no encontrado"))
                    return@get
                }
                call.respond(HttpStatusCode.OK, found.toDto())
            } catch (e: Exception) {
                e.printStackTrace()
                call.respond(HttpStatusCode.InternalServerError,
                    ErrorResponse("internal_error", e.message ?: "Error obteniendo parqueo"))
            }
        }

        // ---------- Actualizar estado ----------
        put("/{id}/status/{status}") {
            try {
                val id = call.parameters["id"]
                val statusParam = call.parameters["status"]?.lowercase()
                if (id.isNullOrBlank() || !ObjectId.isValid(id)) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("bad_request", "ID inválido")); return@put
                }

                if (statusParam == null || statusParam !in listOf("pending", "approved", "rejected", "inactive")) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("bad_request", "Estado inválido")); return@put
                }

                data class StatusBody(
                    val reason: String? = null,
                    val authorId: String? = null,
                    val authorEmail: String? = null,
                    val solicitudTipo: String? = null // 👈 NUEVO
                )
                val body = runCatching { call.receive<StatusBody>() }.getOrNull()

                val objId = ObjectId(id)
                val current = DatabaseFactory.parkingsCollection.findOneById(objId)
                    ?: run {
                        call.respond(HttpStatusCode.NotFound, ErrorResponse("not_found", "Parqueo no encontrado")); return@put
                    }

                val now = System.currentTimeMillis()

                val newComments =
                    if (statusParam == "rejected" && !body?.reason.isNullOrBlank()) {
                        current.comments + ParkingComment(
                            type = "rejection",
                            text = body!!.reason!!.trim(),
                            authorId = body.authorId,
                            authorEmail = body.authorEmail,
                            createdAt = now
                        )
                    } else if (statusParam == "inactive") {
                        current.comments + ParkingComment(
                            type = "system",
                            text = "Parqueo deshabilitado por el propietario",
                            authorId = body?.authorId,
                            authorEmail = body?.authorEmail,
                            createdAt = now
                        )
                    } else current.comments

                // Deducir tipo si no viene en el body
                val tipoDeducido = body?.solicitudTipo ?: when (statusParam) {
                    "approved"  -> "Activo"
                    "inactive"  -> "Inactivo"
                    "rejected"  -> "Edicion" // puedes separar si quieres "Rechazo"
                    "pending"   -> "Edicion"
                    else        -> "Edicion"
                }

                val updated = current.copy(
                    status = statusParam,
                    solicitudTipo = tipoDeducido, // 👈 guarda tipo
                    updatedAt = now,
                    comments = newComments
                )
                val ok = DatabaseFactory.parkingsCollection.updateOneById(objId, updated).wasAcknowledged()
                if (!ok) {
                    call.respond(HttpStatusCode.InternalServerError, ErrorResponse("internal_error", "No se pudo actualizar")); return@put
                }

                call.respond(HttpStatusCode.OK, updated.toDto())
            } catch (e: Exception) {
                e.printStackTrace()
                call.respond(HttpStatusCode.InternalServerError, ErrorResponse("internal_error", e.message ?: "Error actualizando estado"))
            }
        }

        // =========================
        //    COMENTARIOS (nuevo)
        // =========================

        // Agregar comentario
        post("/{id}/comments") {
            try {
                val id = call.parameters["id"]
                if (id.isNullOrBlank() || !ObjectId.isValid(id)) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("bad_request", "ID inválido")); return@post
                }
                data class CreateCommentBody(
                    val type: String = "note",
                    val text: String,
                    val authorId: String? = null,
                    val authorEmail: String? = null
                )
                val body = call.receive<CreateCommentBody>()
                if (body.text.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("bad_request", "Texto requerido")); return@post
                }

                val objId = ObjectId(id)
                val current = DatabaseFactory.parkingsCollection.findOneById(objId)
                if (current == null) {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("not_found", "Parqueo no encontrado")); return@post
                }

                val now = System.currentTimeMillis()
                val newList = current.comments + ParkingComment(
                    type = if (body.type == "rejection") "rejection" else "note",
                    text = body.text.trim(),
                    authorId = body.authorId,
                    authorEmail = body.authorEmail,
                    createdAt = now
                )

                val updated = current.copy(updatedAt = now, comments = newList)
                val ok = DatabaseFactory.parkingsCollection.updateOneById(objId, updated).wasAcknowledged()
                if (!ok) {
                    call.respond(HttpStatusCode.InternalServerError, ErrorResponse("internal_error", "No se pudo agregar comentario"))
                    return@post
                }

                call.respond(HttpStatusCode.OK, updated.toDto())

            } catch (e: Exception) {
                e.printStackTrace()
                call.respond(HttpStatusCode.InternalServerError,
                    ErrorResponse("internal_error", e.message ?: "Error agregando comentario"))
            }
        }

        // Listar comentarios
        get("/{id}/comments") {
            try {
                val id = call.parameters["id"]
                if (id.isNullOrBlank() || !ObjectId.isValid(id)) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("bad_request", "ID inválido")); return@get
                }
                val found = DatabaseFactory.parkingsCollection.findOneById(ObjectId(id))
                if (found == null) {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("not_found", "Parqueo no encontrado")); return@get
                }
                call.respond(HttpStatusCode.OK, found.toDto().comments)
            } catch (e: Exception) {
                e.printStackTrace()
                call.respond(HttpStatusCode.InternalServerError,
                    ErrorResponse("internal_error", e.message ?: "Error obteniendo comentarios"))
            }
        }

        // ---------- Editar parqueo (PUT) ----------
        put("/{id}") {
            try {
                val id = call.parameters["id"]
                if (id.isNullOrBlank() || !ObjectId.isValid(id)) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("bad_request", "ID inválido"))
                    return@put
                }

                val body = call.receive<CreateParkingLotRequest>()
                if (body.localName.isBlank() ||
                    body.address.isBlank() ||
                    body.capacity <= 0 ||
                    body.priceHour < 0 ||
                    body.daysOfWeek.isEmpty() ||
                    body.schedules.isEmpty()
                ) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse("bad_request", "Campos requeridos faltantes o inválidos")
                    )
                    return@put
                }

                fun extractNoAuth(name: String): String? =
                    call.request.headers[name]?.takeIf { it.isNotBlank() }
                        ?: call.request.queryParameters[name]?.takeIf { it.isNotBlank() }

                val userId   = extractNoAuth("X-User-Id")
                val ownerDoc = extractNoAuth("X-Owner-Documento")
                val ownerTipoNorm = extractNoAuth("X-Owner-Tipo")
                    ?.trim()?.uppercase()?.replace("É", "E")

                if (userId.isNullOrBlank() && (ownerDoc.isNullOrBlank() || ownerTipoNorm.isNullOrBlank())) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse("bad_request","Falta identidad del dueño (X-User-Id o ownerDocumento+ownerTipo)")
                    )
                    return@put
                }

                val objId = ObjectId(id)
                val col = DatabaseFactory.parkingsCollection
                val current = col.findOneById(objId)
                if (current == null) {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("not_found", "Parqueo no encontrado"))
                    return@put
                }

                val sameUser  = !userId.isNullOrBlank() && userId == current.createdBy
                val sameOwner = !ownerDoc.isNullOrBlank() && !ownerTipoNorm.isNullOrBlank() &&
                        ownerDoc == current.createdByDocumento &&
                        ownerTipoNorm == current.createdByTipoDocumento

                if (!sameUser && !sameOwner) {
                    call.respond(HttpStatusCode.Forbidden, ErrorResponse("forbidden", "No autorizado a editar este parqueo"))
                    return@put
                }

                val now = System.currentTimeMillis()

                val newLocation = if (body.lat != null && body.lng != null) {
                    listOf(body.lng, body.lat)  // BD: [lng, lat]
                } else {
                    current.location
                }

                val updated = current.copy(
                    localName = body.localName.trim(),
                    address = body.address.trim(),
                    capacity = body.capacity,
                    priceHour = body.priceHour,
                    daysOfWeek = body.daysOfWeek,
                    schedules = body.schedules,
                    characteristics = body.characteristics,
                    photos = body.photos,
                    infraDocUrl = body.infraDocUrl,
                    location = newLocation,
                    status = "pending",
                    solicitudTipo = body.solicitudTipo ?: "Edicion", // 👈 default en edición
                    updatedAt = now
                )

                val ok = col.updateOneById(objId, updated).wasAcknowledged()
                if (!ok) {
                    call.respond(HttpStatusCode.InternalServerError,
                        ErrorResponse("internal_error", "No se pudo actualizar el parqueo"))
                    return@put
                }

                call.respond(HttpStatusCode.OK, updated.toDto())

            } catch (e: Exception) {
                e.printStackTrace()
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ErrorResponse("internal_error", e.message ?: "Error actualizando parqueo")
                )
            }
        }
    }
}