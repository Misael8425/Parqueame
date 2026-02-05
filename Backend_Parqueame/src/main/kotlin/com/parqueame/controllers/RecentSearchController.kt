package com.parqueame.controllers

import com.parqueame.DatabaseFactory
import com.parqueame.dto.CreateRecentSearchRequest
import com.parqueame.dto.RecentSearchResponse
import com.parqueame.models.RecentSearch
import com.parqueame.util.distanceMeters
import com.parqueame.util.distanceText
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.litote.kmongo.eq

private fun extractUserIdNoAuth(call: ApplicationCall): String? {
    try {
        // 1) Header preferido
        call.request.headers["X-User-Id"]?.let {
            if (it.isNotBlank()) {
                println("✅ X-User-Id encontrado: $it")
                return it
            }
        }

        // 2) Query fallback ?userId=
        call.request.queryParameters["userId"]?.let {
            if (it.isNotBlank()) {
                println("✅ userId query param encontrado: $it")
                return it
            }
        }

        // Debug: mostrar todos los headers disponibles
        println("🔍 Headers disponibles:")
        call.request.headers.entries().forEach { (name, values) ->
            println("  $name: ${values.joinToString(", ")}")
        }

        println("❌ No se encontró X-User-Id ni userId")
        return null

    } catch (e: Exception) {
        println("❌ Error extrayendo userId: ${e.message}")
        return null
    }
}

fun Route.recentSearchRoutes(db: DatabaseFactory) {

    route("/recent-searches") {

        // Crear/guardar una búsqueda reciente
        post {
            val userId = extractUserIdNoAuth(call)
                ?: return@post call.respond(HttpStatusCode.BadRequest, "Falta X-User-Id o userId")

            val body = try {
                call.receive<CreateRecentSearchRequest>()
            } catch (e: Exception) {
                return@post call.respond(HttpStatusCode.BadRequest, "Body inválido: ${e.message}")
            }

            val meters = if (body.originLat != null && body.originLng != null) {
                distanceMeters(body.originLat, body.originLng, body.lat, body.lng)
            } else null

            val doc = RecentSearch(
                userId = userId,
                name = body.name,
                address = body.address,
                lat = body.lat,
                lng = body.lng,
                originLat = body.originLat,
                originLng = body.originLng,
                distanceMeters = meters
            )

            db.recentSearchesCollection.insertOne(doc)
            call.respond(HttpStatusCode.Created, mapOf("id" to doc.id, "message" to "Guardado"))
        }

        // Listar recientes
        get {
            val userId = extractUserIdNoAuth(call)
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Falta X-User-Id o userId")

            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 10

            // Cambio: usar toList() en lugar de toList() directamente
            val list = db.recentSearchesCollection
                .find(RecentSearch::userId eq userId)
                .descendingSort(RecentSearch::createdAt)
                .limit(limit)
                .toList()
                .map {
                    RecentSearchResponse(
                        id = it.id,
                        name = it.name,
                        address = it.address,
                        lat = it.lat,
                        lng = it.lng,
                        distanceText = distanceText(it.distanceMeters),
                        createdAt = it.createdAt
                    )
                }

            call.respond(list)
        }

        // Última
        get("/last") {
            val userId = extractUserIdNoAuth(call)
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Falta X-User-Id o userId")

            // Cambio: usar first() en lugar de firstOrNull()
            val last = db.recentSearchesCollection
                .find(RecentSearch::userId eq userId)
                .descendingSort(RecentSearch::createdAt)
                .first()

            if (last == null) return@get call.respond(HttpStatusCode.NoContent)

            call.respond(
                RecentSearchResponse(
                    id = last.id,
                    name = last.name,
                    address = last.address,
                    lat = last.lat,
                    lng = last.lng,
                    distanceText = distanceText(last.distanceMeters),
                    createdAt = last.createdAt
                )
            )
        }

        // Borrar todas
        delete {
            val userId = extractUserIdNoAuth(call)
                ?: return@delete call.respond(HttpStatusCode.BadRequest, "Falta X-User-Id o userId")

            db.recentSearchesCollection.deleteMany(RecentSearch::userId eq userId)
            call.respond(mapOf("message" to "Eliminadas"))
        }
    }
}