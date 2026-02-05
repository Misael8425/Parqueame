//package com.parqueame.routes
//
//import com.parqueame.data.PasswordResetRepository
//import com.parqueame.models.*
//import com.parqueame.services.ResendMailService
//import com.parqueame.util.generateCode
//import com.parqueame.util.hashPassword
//import io.ktor.http.*
//import io.ktor.server.application.*
//import io.ktor.server.request.*
//import io.ktor.server.response.*
//import io.ktor.server.routing.*
//import kotlinx.coroutines.launch
//import org.bson.conversions.Bson
//import org.litote.kmongo.coroutine.CoroutineCollection
//import com.mongodb.client.model.Filters.eq
//import com.mongodb.client.model.Updates.set
//
//fun Route.authRoutes(
//    resetRepo: PasswordResetRepository,
//    mailer: ResendMailService,
//    usersCol: CoroutineCollection<org.bson.Document>, // usar Document evita depender de tu clase User
//    usersCollectionName: String = "users"             // por si quieres loguear
//) {
//    get("/health") { call.respondText("ok") }
//
//    post("/auth/request-reset") {
//        val req = call.receive<EmailRequest>()
//        val email = req.email.trim().lowercase()
//        if (email.isBlank()) {
//            return@post call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Email requerido"))
//        }
//
//        val code = generateCode()
//        resetRepo.save(email, code, ttlMinutes = 10L)
//
//        // Responder rápido (evita timeout en el cliente)
//        call.respond(HttpStatusCode.OK, mapOf("message" to "Código generado"))
//
//        // Enviar correo en background
//        call.application.launch {
//            runCatching { mailer.sendResetCode(email, code) }
//                .onFailure { call.application.log.error("Error enviando reset a $email", it) }
//        }
//    }
//
//    post("/auth/verify-code") {
//        val req = call.receive<CodeVerificationRequest>()
//        val ok = resetRepo.getValid(req.email.trim().lowercase(), req.code.trim()) != null
//        if (!ok) return@post call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Código inválido o expirado"))
//        call.respond(HttpStatusCode.OK, mapOf("message" to "Código válido"))
//    }
//
//    post("/auth/reset-password") {
//        val req = call.receive<ResetPasswordRequest>()
//        val email = req.email.trim().lowercase()
//        val codeDoc = resetRepo.getValid(email, req.code.trim())
//            ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Código inválido o expirado"))
//
//        val newHash = hashPassword(req.newPassword)
//
//        // Actualiza sin tu data class, usando filtros/updates directos
//        val filter: Bson = eq("email", email)
//        val update: Bson = set("passwordHash", newHash)
//        val result = usersCol.updateOne(filter, update)
//
//        if (!result.wasAcknowledged() || result.matchedCount == 0L) {
//            return@post call.respond(HttpStatusCode.NotFound, mapOf("message" to "Usuario no encontrado"))
//        }
//
//        // Consumir el código
//        resetRepo.consume(email, req.code.trim())
//
//        call.respond(HttpStatusCode.OK, mapOf("message" to "Contraseña actualizada"))
//    }
//}
