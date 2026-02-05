package com.parqueame.controllers

import com.parqueame.services.PaymentProcessorService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory

fun Route.stripePaymentController() {
    val log = LoggerFactory.getLogger("StripePaymentController")
    val service = PaymentProcessorService()

    /**
     * Crear un pago Stripe y registrar transacción en Mongo
     */
    post("/stripe/create-payment") {
        try {
            val request = call.receive<CreatePaymentRequest>()
            val paymentIntent = service.createPayment(
                userId = request.userId,
                parkingLotId = request.parkingLotId,
                amount = request.amount,
                customerId = request.customerId,
                currency = request.currency
            )

            call.respond(
                HttpStatusCode.OK,
                mapOf(
                    "clientSecret" to paymentIntent.clientSecret,
                    "paymentIntentId" to paymentIntent.id
                )
            )
        } catch (e: Exception) {
            log.error("Error al crear pago", e)
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf("error" to (e.message ?: "Error interno"))
            )
        }
    }

    /**
     * Procesar reembolso
     */
    post("/stripe/refund") {
        try {
            val body = call.receive<RefundRequest>()
            val refund = service.processRefund(body.paymentIntentId, body.reason)
            call.respond(mapOf("refundId" to refund.id, "status" to refund.status))
        } catch (e: Exception) {
            log.error("Error procesando reembolso", e)
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
        }
    }

    /**
     * Webhook Stripe
     * Configura en Stripe Dashboard -> Webhooks -> URL = /stripe/webhook
     */
    post("/stripe/webhook") {
        try {
            val payload = call.receiveText()
            val event = com.stripe.model.Event.GSON.fromJson(payload, com.stripe.model.Event::class.java)
            val type = event.type
            val paymentIntentId = event.dataObjectDeserializer.`object`.orElse(null)?.let {
                it as? com.stripe.model.PaymentIntent
            }?.id

            log.info("🔔 Webhook recibido: $type paymentIntent=$paymentIntentId")
            service.handleWebhookEvent(type, paymentIntentId)
            call.respond(HttpStatusCode.OK)
        } catch (e: Exception) {
            log.error("Error manejando webhook", e)
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
        }
    }
}

@Serializable
data class CreatePaymentRequest(
    val userId: String,
    val parkingLotId: String,
    val amount: Double,
    val customerId: String,
    val currency: String = "DOP"
)

@Serializable
data class RefundRequest(
    val paymentIntentId: String,
    val reason: String? = null
)
