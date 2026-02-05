package com.parqueame.controllers

import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import com.parqueame.DatabaseFactory
import com.parqueame.services.DgiiSyncService

@Serializable
data class ApiError(val ok: Boolean = false, val error: String)

fun Application.syncRoutes() {
    // Lee variables una sola vez al iniciar el servidor
    val adminTok = System.getenv("SYNC_ADMIN_TOKEN") ?: "supertoken"
    val srcUrl   = System.getenv("DGII_SOURCE_URL")
        ?: "https://dgii.gov.do/app/WebApps/Consultas/RNC/RNC_CONTRIBUYENTES.zip"
    val defaultLimit = System.getenv("SYNC_LIMIT")?.toIntOrNull() ?: 1000

    // Reutiliza el cliente sync y el nombre de DB DGII desde DatabaseFactory
    val syncClient = DatabaseFactory.getSyncClient()
    val dgiiDbName = DatabaseFactory.getDgiiDbName()

    // Instancia una sola vez el servicio (singleton en el ciclo de vida de la app)
    val syncService = DgiiSyncService(
        mongoClient = syncClient,
        dbName = dgiiDbName,
        sourceUrl = srcUrl,
        defaultLimit = defaultLimit
    )

    routing {
        post("/admin/tax/sync") {
            val token = call.request.headers["X-Admin-Token"]
            if (token != adminTok) {
                return@post call.respond(HttpStatusCode.Unauthorized, ApiError(error = "No autorizado"))
            }

            val limit = call.request.queryParameters["limit"]?.toIntOrNull()

            try {
                val report = syncService.runOnce(limitOverride = limit)
                call.respond(HttpStatusCode.OK, report)
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, ApiError(error = e.message ?: "Solicitud inválida"))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, ApiError(error = e.message ?: "Error interno"))
            }
        }
    }

    // No cierres syncClient aquí: lo gestiona DatabaseFactory (ciclo de la app)
}
