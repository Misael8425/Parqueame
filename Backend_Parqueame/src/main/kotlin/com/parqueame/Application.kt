package com.parqueame

import com.parqueame.controllers.*
import com.parqueame.routes.registerRatingRoutes
import com.parqueame.services.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.slf4j.event.Level


// NEW: imports para CORS
import io.ktor.server.plugins.cors.routing.*

fun main() {
    // Inicializa base de datos ANTES de arrancar el servidor
    runBlocking {
        DatabaseFactory.init()
    }

    // Inicia el servidor Ktor
    embeddedServer(
        Netty,
        host = "0.0.0.0",
        port = System.getenv("PORT")?.toInt() ?: 8080,
        module = Application::module
    ).start(wait = true)
}

fun Application.module() {
    // NEW: leer ADMIN_USER_ID de variables de entorno
    val adminUserId: String = System.getenv("ADMIN_USER_ID")
        ?: error("ADMIN_USER_ID no está definido en variables de entorno")

    // Plugins
    install(CallLogging) {
        level = Level.INFO
        filter { call -> call.request.path().startsWith("/") }
    }

    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
            explicitNulls = false
        })
    }

    install(StatusPages) {
        exception<Throwable> { call, cause ->
            println("❌ Error en aplicación: ${cause.message}")
            cause.printStackTrace()
            call.respond(HttpStatusCode.InternalServerError, "Error interno del servidor")
        }
    }

    // NEW: habilitar CORS
    install(CORS) {
        allowHost("frontend-production-ab00.up.railway.app", schemes = listOf("https"))
        allowHost("localhost:3000", schemes = listOf("http")) // opcional para desarrollo local

        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch) // ← para /reservations/{id}/cancel
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        allowHeader("X-User-Id")

        allowCredentials = false
        allowNonSimpleContentTypes = true
    }

    // Servicios
    val dgiiService = DgiiService(DatabaseFactory.contribuyentesCollection)

    // 🔹 REGISTRA rutas de reservas FUERA del bloque routing principal
    reservationsWiring()
    qrWiring()

    // Rutas
    routing {
        get("/") {
            call.respondText("🚀 Backend Parqueame activo y funcionando correctamente.", ContentType.Text.Plain)
        }

        get("/health") {                   // <-- ya lo tenías, se conserva
            call.respondText("ok")
        }

        usuarioController()
        cedulaController()
        dgiiController(dgiiService)
        authController()
        stripeController()
        stripeCustomerController()
        recentSearchRoutes(DatabaseFactory)
        parkingController()
        walletController()
        stripePaymentController()
        usuarioPublicController()
        registerRatingRoutes()

        //syncRoutes()

        // Endpoint de prueba para debugging DGII
        get("/test/{rnc}") {
            val rnc = call.parameters["rnc"]
            try {
                val doc = DatabaseFactory.contribuyentesCollection.findOneById(rnc ?: "")
                call.respond(mapOf(
                    "rnc" to rnc,
                    "found" to (doc != null),
                    "doc" to doc?.toString()?.take(200) // Solo primeros 200 caracteres
                ))
            } catch (e: Exception) {
                call.respond(mapOf(
                    "error" to e.message,
                    "type" to e::class.simpleName
                ))
            }
        }

        // NEW: Ruta mínima para probar autorización de backoffice (opcional).
        // Usa header X-User-Id o query ?userId=...
        get("/admin/health") {
            val requesterId = call.request.header("X-User-Id")
                ?: call.request.queryParameters["userId"]

            if (requesterId == adminUserId) {
                call.respond(HttpStatusCode.OK, mapOf("status" to "ok", "role" to "ADMIN"))
            } else {
                call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Acceso denegado"))
            }
        }

        get("/test-sendgrid") {
            call.respondText(SendGridService.testConfiguration())
        }

        get("/send-test-sendgrid/{email}") {
            val email = call.parameters["email"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Email requerido")
            try {
                val success = SendGridService.sendResetCode(email, "TEST123")
                call.respond(mapOf(
                    "status" to if (success) "Email enviado" else "Error enviando email",
                    "email" to email,
                    "success" to success
                ))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }
    }
}