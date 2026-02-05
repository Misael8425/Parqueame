package com.parqueame.controllers

import com.parqueame.DatabaseFactory
import com.parqueame.models.*
import com.parqueame.repositories.WalletRepository
import com.parqueame.services.WalletService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory

// ✅ SIN PARÁMETROS
fun Route.walletController() {
    val log = LoggerFactory.getLogger("WalletController")

    // Usa las colecciones Coroutine de DatabaseFactory
    val repo = WalletRepository(
        txCol  = DatabaseFactory.transactionsCollection,
        bankCol = DatabaseFactory.bankAccountsCollection,
        wdCol  = DatabaseFactory.withdrawalsCollection
    )
    val service = WalletService(repo)

    route("/wallet") {
        get("/summary") {
            val userId = call.request.header("X-User-Id")
                ?: return@get call.respond(HttpStatusCode.Unauthorized, mapOf("message" to "X-User-Id requerido"))
            call.respond(HttpStatusCode.OK, service.getSummary(userId))
        }

        get("/transactions") {
            val userId = call.request.header("X-User-Id")
                ?: return@get call.respond(HttpStatusCode.Unauthorized, mapOf("message" to "X-User-Id requerido"))
            val start = call.request.queryParameters["startDate"]
            val end = call.request.queryParameters["endDate"]
            call.respond(HttpStatusCode.OK, service.getTransactions(userId, start, end))
        }

        put("/bank-account") {
            val userId = call.request.header("X-User-Id")
                ?: return@put call.respond(HttpStatusCode.Unauthorized, mapOf("message" to "X-User-Id requerido"))
            val req = call.receive<UpdateBankAccountRequest>()
            try {
                call.respond(HttpStatusCode.OK, service.updateBankAccount(userId, req))
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, mapOf("message" to e.message))
            } catch (e: Exception) {
                log.error("update bank account", e)
                call.respond(HttpStatusCode.InternalServerError, mapOf("message" to "Error actualizando cuenta"))
            }
        }

        post("/withdrawals") {
            val userId = call.request.header("X-User-Id")
                ?: return@post call.respond(HttpStatusCode.Unauthorized, mapOf("message" to "X-User-Id requerido"))
            val req = call.receive<CreateWithdrawalRequest>()
            if (req.password.isBlank())
                return@post call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Contraseña requerida"))
            try {
                call.respond(HttpStatusCode.Created, service.createWithdrawal(userId, req.amount))
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, mapOf("message" to e.message))
            } catch (e: Exception) {
                log.error("create withdrawal", e)
                call.respond(HttpStatusCode.InternalServerError, mapOf("message" to "Error creando retiro"))
            }
        }

        get("/withdrawals") {
            val userId = call.request.header("X-User-Id")
                ?: return@get call.respond(HttpStatusCode.Unauthorized, mapOf("message" to "X-User-Id requerido"))
            call.respond(HttpStatusCode.OK, service.listWithdrawals(userId))
        }
    }
}